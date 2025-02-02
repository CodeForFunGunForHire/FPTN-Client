package com.filantrop.pvnclient.services;

import android.app.PendingIntent;
import android.net.IpPrefix;
import android.net.VpnService;
import android.os.Build;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;

import com.filantrop.pvnclient.enums.ConnectionState;
import com.filantrop.pvnclient.enums.HandlerMessageTypes;
import com.filantrop.pvnclient.services.websocket.CustomWebSocketListener;
import com.filantrop.pvnclient.services.websocket.OkHttpClientWrapper;
import com.filantrop.pvnclient.services.websocket.WebSocketMessageCallback;
import com.filantrop.pvnclient.utils.DataRateCalculator;
import com.filantrop.pvnclient.utils.IPUtils;
import com.filantrop.pvnclient.vpnclient.exception.ErrorCode;
import com.filantrop.pvnclient.vpnclient.exception.PVNClientException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import lombok.Getter;
import lombok.Setter;

public class CustomVpnConnection extends Thread {

    public interface OnEstablishListener {
        void onEstablish(ParcelFileDescriptor tunInterface);
    }

    /**
     * Maximum packet size is constrained by the MTU
     */
    private static final int MAX_PACKET_SIZE = 65536;

    private final CustomVpnService service;

    @Getter
    private final int connectionId;

    private final String serverHost;

    private OkHttpClientWrapper okHttpClientWrapper;

    private PendingIntent mConfigureIntent;

    public OnEstablishListener onEstablishListener;

    public Instant connectionTime;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final DataRateCalculator downloadRate = new DataRateCalculator(1000);
    private final DataRateCalculator uploadRate = new DataRateCalculator(1000);

    public CustomVpnConnection(final CustomVpnService service, final int connectionId,
                               final String serverHost, final int serverPort,
                               final String username, final String password) {
        this.service = service;
        this.connectionId = connectionId;
        this.serverHost = serverHost;
        try {
            this.okHttpClientWrapper = new OkHttpClientWrapper(username, password, serverHost, serverPort);
        } catch (PVNClientException ex) {
            sendErrorMessageToUI(ex.getMessage());
        }
    }

    /**
     * Optionally, set an intent to configure the VPN. This is {@code null} by default.
     */
    public void setConfigureIntent(PendingIntent intent) {
        mConfigureIntent = intent;
    }

    @Override
    public void run() {
        ParcelFileDescriptor vpnInterface = null;
        try {
            sendConnectionStateToUI(ConnectionState.CONNECTING);

            final String token = okHttpClientWrapper.getAuthToken();
            if (token == null) {
                throw new PVNClientException(ErrorCode.AUTHENTICATION_ERROR.getValue());
            }

            VpnService.Builder builder = service.new Builder();
            builder.addAddress("10.10.0.1", 32);
            builder.addRoute("172.20.0.1", 32);

            final String dnsServer = okHttpClientWrapper.getDnsServerIPv4();
            builder.addDnsServer(dnsServer);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                builder.excludeRoute(new IpPrefix(InetAddress.getByName(serverHost), 32));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("10.10.0.0"), 16));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("172.16.0.0"), 12));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("192.168.0.0"), 16));
                builder.addRoute("0.0.0.0", 0);
            } else {
                IPAddress rootSubnet = new IPAddressString("0.0.0.0/0").getAddress();
                List<IPAddress> subnetsToExclude = Stream.of(
                                serverHost + "/32",
                                "10.10.0.0/16",
                                "172.16.0.0/12",
                                "192.168.0.0/16")
                        .map(sub -> new IPAddressString(sub).getAddress()).collect(Collectors.toList());

                List<IPAddress> subnetsToInclude = new ArrayList<>();
                IPUtils.exclude(rootSubnet, subnetsToExclude, subnetsToInclude);
                for (IPAddress ipAddress : subnetsToInclude) {
                    String hostIp = ipAddress.getLower().toAddressString().getHostAddress().toString();
                    Integer networkPrefixLength = ipAddress.getLower().toAddressString().getNetworkPrefixLength();
                    Log.d(getTag(), "subnetsToInclude.ipAddress: " + hostIp + "/" + networkPrefixLength);
                    builder.addRoute(hostIp, networkPrefixLength != null ? networkPrefixLength : 32);
                }
            }

            builder.setSession(serverHost).setConfigureIntent(mConfigureIntent);

            synchronized (service) {
                vpnInterface = builder.establish();
            }
            if (vpnInterface == null) {
                throw new PVNClientException(ErrorCode.VPN_INTERFACE_ERROR.getValue());
            } else {
                if (onEstablishListener != null) {
                    onEstablishListener.onEstablish(vpnInterface);
                }
            }
            Log.i(getTag(), "New interface: " + vpnInterface);

            scheduler.scheduleWithFixedDelay(() -> {
                // Get download and upload speeds
                String downloadSpeed = downloadRate.getFormatString();
                String uploadSpeed = uploadRate.getFormatString();
                sendSpeedInfoToUI(downloadSpeed, uploadSpeed);
            }, 1, 1, TimeUnit.SECONDS); // Start after 1 second, repeat every 1 second

            // Packets received need to be written to this output stream.
            FileOutputStream outputStream = new FileOutputStream(vpnInterface.getFileDescriptor());
            WebSocketMessageCallback callback = data -> {
                try {
                    downloadRate.update(data.length);
                    outputStream.write(data);
                } catch (Exception e) {
                    Log.w(getTag(), "onMessageReceived: " + new String(data));
                }
            };
            okHttpClientWrapper.startWebSocket(new CustomWebSocketListener(callback));
            connectionTime = Instant.now();
            sendConnectionStateToUI(ConnectionState.CONNECTED);

            // Packets to be sent are queued in this input stream.
            // todo: вынести в отдельный поток? чтобы не блокировать этот
            FileInputStream inputStream = new FileInputStream(vpnInterface.getFileDescriptor());
            ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
            while (!isInterrupted()) {
                try {
                    int length = inputStream.read(buffer.array());
                    if (length > 0) {
                        uploadRate.update(length);
                        okHttpClientWrapper.send(buffer, length);
                    }
                } catch (Exception e) {
                    Log.d(getTag(), "Error reading data from VPN interface: " + e.getMessage());
                }
            }
        } catch (PVNClientException | IOException e) {
            sendErrorMessageToUI(e.getMessage());
        } finally {
            sendConnectionStateToUI(ConnectionState.DISCONNECTED);
            if (vpnInterface != null) {
                try {
                    vpnInterface.close();
                } catch (IOException e) {
                    Log.e(getTag(), "Unable to close interface", e);
                }
            }
            okHttpClientWrapper.stopWebSocket();
            scheduler.shutdown();
        }
    }

    private void sendErrorMessageToUI(String msg) {
        service.mHandler.sendMessage(Message.obtain(null, HandlerMessageTypes.ERROR.value, 0, 0, msg));
    }

    private void sendSpeedInfoToUI(String downloadSpeed, String uploadSpeed) {
        service.mHandler.sendMessage(Message.obtain(null, HandlerMessageTypes.SPEED_INFO.value, 0, 0, Pair.create(downloadSpeed, uploadSpeed)));
    }

    private void sendConnectionStateToUI(ConnectionState connectionState) {
        service.mHandler.sendMessage(Message.obtain(null, HandlerMessageTypes.CONNECTION_STATE.value, 0, 0, Pair.create(connectionState, Instant.now())));
    }

    private String getTag() {
        return this.getClass().getCanonicalName() + "[" + connectionId + "]";
    }

}
