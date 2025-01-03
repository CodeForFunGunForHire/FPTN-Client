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
import com.filantrop.pvnclient.services.exception.PVNClientException;
import com.filantrop.pvnclient.services.websocket.CustomWebSocketListener;
import com.filantrop.pvnclient.services.websocket.OkHttpClientWrapper;
import com.filantrop.pvnclient.services.websocket.WebSocketMessageCallback;
import com.filantrop.pvnclient.utils.DataRateCalculator;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private final OkHttpClientWrapper okHttpClientWrapper;

    private PendingIntent mConfigureIntent;

    @Setter
    private OnEstablishListener onEstablishListener;

    @Getter
    private Instant connectionTime;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final DataRateCalculator downloadRate = new DataRateCalculator(1000);
    private final DataRateCalculator uploadRate = new DataRateCalculator(1000);

    public CustomVpnConnection(final CustomVpnService service, final int connectionId,
                               final String serverHost, final int serverPort,
                               final String username, final String password) {
        this.service = service;
        this.connectionId = connectionId;
        this.serverHost = serverHost;
        this.okHttpClientWrapper = new OkHttpClientWrapper(username, password, serverHost, serverPort);
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

            String token = okHttpClientWrapper.getAuthToken();
            if (token == null) {
                // todo: подумать над тем чтобы хранить тексты ошибок в strings.xml и с разной локализацией!!!
                throw PVNClientException.fromMessage("Can't get authToken!");
            }

            VpnService.Builder builder = service.new Builder();
            builder.addAddress("10.10.0.1", 32);
            builder.addRoute("172.20.0.1", 32);
            builder.addDnsServer("172.20.0.1"); // FIXME! String dnsServer = okHttpClientWrapper.getDNSServer(serverName, serverPort);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                builder.addRoute("0.0.0.0", 0);
                builder.excludeRoute(new IpPrefix(InetAddress.getByName(serverHost), 32));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("10.10.0.0"), 16));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("172.16.0.0"), 12));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("192.168.0.0"), 16));
            } else {
                builder.addRoute("224.0.0.0", 3);
                builder.addRoute("208.0.0.0", 4);
                builder.addRoute("200.0.0.0", 5);
                builder.addRoute("196.0.0.0", 6);
                builder.addRoute("194.0.0.0", 7);
                builder.addRoute("193.0.0.0", 8);
                builder.addRoute("192.0.0.0", 9);
                builder.addRoute("192.192.0.0", 10);
                builder.addRoute("192.128.0.0", 11);
                builder.addRoute("192.176.0.0", 12);
                builder.addRoute("192.160.0.0", 13);
                builder.addRoute("192.172.0.0", 14);
                builder.addRoute("192.170.0.0", 15);
                builder.addRoute("192.169.0.0", 16);
                builder.addRoute("128.0.0.0", 3);
                builder.addRoute("176.0.0.0", 4);
                builder.addRoute("160.0.0.0", 5);
                builder.addRoute("168.0.0.0", 6);
                builder.addRoute("174.0.0.0", 7);
                builder.addRoute("173.0.0.0", 8);
                builder.addRoute("172.128.0.0", 9);
                builder.addRoute("172.64.0.0", 10);
                builder.addRoute("172.32.0.0", 11);
                builder.addRoute("172.0.0.0", 12);
                builder.addRoute("64.0.0.0", 2);
                builder.addRoute("32.0.0.0", 3);
                builder.addRoute("16.0.0.0", 4);
                //builder.addRoute("0.0.0.0", 5);
                builder.addRoute("12.0.0.0", 6);
                builder.addRoute("8.0.0.0", 7);
                builder.addRoute("11.0.0.0", 8);
            }
            builder.setSession(serverHost).setConfigureIntent(mConfigureIntent);

            synchronized (service) {
                vpnInterface = builder.establish();
            }
            if (vpnInterface == null) {
                throw PVNClientException.fromMessage("Can't get vpn interface");
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
        service.getMHandler().sendMessage(Message.obtain(null, HandlerMessageTypes.ERROR.getValue(), 0, 0, msg));
    }

    private void sendSpeedInfoToUI(String downloadSpeed, String uploadSpeed) {
        service.getMHandler().sendMessage(Message.obtain(null, HandlerMessageTypes.SPEED_INFO.getValue(), 0, 0, Pair.create(downloadSpeed, uploadSpeed)));
    }

    private void sendConnectionStateToUI(ConnectionState connectionState) {
        service.getMHandler().sendMessage(Message.obtain(null, HandlerMessageTypes.CONNECTION_STATE.getValue(), 0, 0, Pair.create(connectionState, Instant.now())));
    }

    private String getTag() {
        return this.getClass().getCanonicalName() + "[" + connectionId + "]";
    }

}
