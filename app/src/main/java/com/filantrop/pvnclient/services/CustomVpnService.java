package com.filantrop.pvnclient.services;

import static com.filantrop.pvnclient.core.common.Constants.SELECTED_SERVER;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.core.common.Constants;
import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.enums.ConnectionState;
import com.filantrop.pvnclient.enums.HandlerMessageTypes;
import com.filantrop.pvnclient.repository.FptnServerRepository;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;
import com.filantrop.pvnclient.views.HomeActivity;
import com.filantrop.pvnclient.views.speedtest.SpeedTestService;
import com.filantrop.pvnclient.vpnclient.exception.ErrorCode;
import com.filantrop.pvnclient.vpnclient.exception.PVNClientException;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.Setter;

public class CustomVpnService extends VpnService implements Handler.Callback {
    private static final String TAG = CustomVpnService.class.getName();

    public static final String ACTION_CONNECT = "com.example.android.fptn.START";
    public static final String ACTION_DISCONNECT = "com.example.android.fptn.STOP";

    //Handler - очередь обрабатываемых в потоке сообщений.
    @Getter
    private Handler mHandler;

    @Setter
    private FptnServerViewModel fptnViewModel;

    // Подключающийся поток. Ссылка обнуляется если подключение успешно.
    // Видимо, чтобы потушить поток с неудавшимся соединением
    private final AtomicReference<CustomVpnConnection> mConnectingThread = new AtomicReference<>();

    // Удачно подключившийся поток становится mConnection
    private final AtomicReference<Pair<CustomVpnConnection, ParcelFileDescriptor>> mConnection = new AtomicReference<>();

    private final AtomicInteger mNextConnectionId = new AtomicInteger(1);

    // Pending Intent for launch MainActivity when notification tapped
    private PendingIntent launchMainActivityPendingIntent;

    // Pending Intent to disconnect from notification
    private PendingIntent disconnectPendingIntent;

    // Server for finding the best server (and check availability)
    private SpeedTestService speedTestService;

    private FptnServerDto selectedServer;

    private FptnServerRepository fptnServerRepository;

    // Binder given to clients.
    private final IBinder binder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public CustomVpnService getService() {
            return CustomVpnService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "CustomVpnService.onCreate: " + Thread.currentThread().getId());
        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // pending intent for open MainActivity on tap
        launchMainActivityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, HomeActivity.class),
                PendingIntent.FLAG_IMMUTABLE);

        // pending intent for disconnect button in notification
        disconnectPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, CustomVpnService.class)
                        .setAction(CustomVpnService.ACTION_DISCONNECT),
                PendingIntent.FLAG_IMMUTABLE);

        try {
            speedTestService = new SpeedTestService();
        } catch (PVNClientException e) {
            Log.e(TAG, "onCreate(): " + e.getMessage(), e);
            throw new RuntimeException(e);
        }

        fptnServerRepository = new FptnServerRepository(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "CustomVpnService.onStartCommand: " + intent);
        if (ACTION_DISCONNECT.equals(intent.getAction())) {
            disconnect();
            return START_NOT_STICKY;
        } else {
            // Достаем параметры для подключения из intent
            selectedServer = (FptnServerDto) intent.getSerializableExtra(SELECTED_SERVER);
            if (FptnServerDto.AUTO.equals(selectedServer)) {
                try {
                    List<FptnServerDto> fptnServerDtos = fptnServerRepository.getAllServersListFuture().get();
                    Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getConnectionStateMutableLiveData().postValue(ConnectionState.CONNECTING));
                    selectedServer = speedTestService.findFastestServer(fptnServerDtos);
                    Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getSelectedServerLiveData().postValue(selectedServer));
                } catch (ExecutionException | InterruptedException | PVNClientException e) {
                    Log.e(TAG, "onStartCommand: findFastestServer error! ", e);
                    Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getConnectionStateMutableLiveData().postValue(ConnectionState.DISCONNECTED));
                    Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getErrorTextLiveData().postValue(e.getMessage()));
                    selectedServer = null;
                }
            }

            if (selectedServer == null) {
                Log.e(TAG, "onStartCommand: selectedServer is null");
                Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getErrorTextLiveData().postValue(ErrorCode.ALL_SERVERS_UNREACHABLE.getValue()));
                return START_NOT_STICKY;
            }

            connect(selectedServer.getHost(), selectedServer.getPort(), selectedServer.getUsername(), selectedServer.getPassword());
            return START_REDELIVER_INTENT;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        disconnect();
    }


    public void updateConnectionStateInViewModel() {
        Optional.ofNullable(fptnViewModel).ifPresent(model -> {
            if (mConnection.get() != null) {
                model.getConnectionStateMutableLiveData().postValue(ConnectionState.CONNECTED);
                //Передаем на UI актуальные параметры подключения
                model.startTimer(mConnection.get().first.getConnectionTime());
                // connected server
                model.getSelectedServerLiveData().setValue(selectedServer);
            } else if (mConnection.get() == null && mConnectingThread.get() != null) {
                model.getConnectionStateMutableLiveData().postValue(ConnectionState.CONNECTING);
            } else if (mConnection.get() == null && mConnectingThread.get() == null) {
                model.getConnectionStateMutableLiveData().postValue(ConnectionState.DISCONNECTED);
            }
        });
    }

    @Override
    public boolean handleMessage(@NonNull Message message) {
        Log.d(TAG, "handleMessage: " + message);

        HandlerMessageTypes type = Arrays.stream(HandlerMessageTypes.values()).filter(t -> t.getValue() == message.what).findFirst().orElse(HandlerMessageTypes.UNKNOWN);
        switch (type) {
            case SPEED_INFO:
                Optional.ofNullable(fptnViewModel).ifPresent(model -> {
                    Pair<String, String> downloadSpeedUploadSpeed = (Pair<String, String>) message.obj;
                    String downloadSpeed = downloadSpeedUploadSpeed.first;
                    String uploadSpeed = downloadSpeedUploadSpeed.second;
                    model.getDownloadSpeedAsStringLiveData().postValue(downloadSpeed);
                    model.getUploadSpeedAsStringLiveData().postValue(uploadSpeed);
                    updateNotificationWithMessage("Connected to " + selectedServer.getServerInfo(), "Download: " + downloadSpeed + " Upload: " + uploadSpeed);
                });
                break;
            case CONNECTION_STATE:
                Optional.ofNullable(fptnViewModel).ifPresent(model -> {
                    Pair<ConnectionState, Instant> connectionStateInstantPair = (Pair<ConnectionState, Instant>) message.obj;
                    Optional<ConnectionState> connectionState = Arrays.stream(ConnectionState.values()).filter(t -> t == connectionStateInstantPair.first).findFirst();
                    connectionState.ifPresent(state -> {
                        model.getConnectionStateMutableLiveData().postValue(state);
                        if (ConnectionState.CONNECTED.equals(state)) {
                            model.startTimer(connectionStateInstantPair.second);
                            updateNotificationWithMessage("Connected to " + selectedServer.getServerInfo(), "");
                        } else if (ConnectionState.DISCONNECTED.equals(state)) {
                            model.stopTimer();
                            disconnect();
                        }
                    });
                });
                break;
            case ERROR:
                Optional.ofNullable(fptnViewModel).ifPresent(model -> {
                    model.getErrorTextLiveData().postValue((String) message.obj);
                });
                break;
            default:
                Log.e(TAG, "unexpected message: " + message);
        }
        return true;
    }

    private void connect(String server, int port, String username, String password) {
        // Moving VPNService to foreground to give it higher priority in system
        startForegroundWithNotification("Connecting to " + selectedServer.getServerInfo());

        CustomVpnConnection connection = new CustomVpnConnection(
                this, mNextConnectionId.getAndIncrement(), server, port, username, password);
        setConnectingConnection(connection);

        // Handler to mark as connected once onEstablish is called.
        connection.setConfigureIntent(launchMainActivityPendingIntent);
        connection.setOnEstablishListener(tunInterface -> {
            // Если удалось подключиться, обнуляем подключающийся поток и
            // сохраняем пару подключившийся поток/tunInterface
            mConnectingThread.compareAndSet(connection, null);
            setEstablishedConnection(new Pair<>(connection, tunInterface));
        });
        connection.start();
    }

    private void setConnectingConnection(final Thread thread) {
        final Thread oldThread = mConnectingThread.getAndSet((CustomVpnConnection) thread);
        if (oldThread != null) {
            oldThread.interrupt();
        }
    }

    private void setEstablishedConnection(final Pair<CustomVpnConnection, ParcelFileDescriptor> establishedConnection) {
        final Pair<CustomVpnConnection, ParcelFileDescriptor> oldEstablishedConnection = mConnection.getAndSet(establishedConnection);
        if (oldEstablishedConnection != null) {
            try {
                // прерываем старый поток
                oldEstablishedConnection.first.interrupt();
                // закрываем старый интерфейс
                oldEstablishedConnection.second.close();
            } catch (IOException e) {
                Log.e(TAG, "Closing VPN interface", e);
            }
        }
    }

    private void disconnect() {
        // прерываем/обнуляем подключающийся поток (если есть)
        setConnectingConnection(null);
        // прерываем/обнуляем установленное соединение
        setEstablishedConnection(null);
        // выводим сервис из переднего плана
        stopForeground(true);
    }

    private void startForegroundWithNotification(String title) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel notificationChannel = notificationManager.getNotificationChannel(Constants.MAIN_NOTIFICATION_CHANNEL_ID);
        if (notificationChannel == null) {
            notificationManager.createNotificationChannelGroup(
                    new NotificationChannelGroup(Constants.MAIN_NOTIFICATION_CHANNEL_GROUP_ID, getString(R.string.notification_group_name)));

            NotificationChannel newNotificationChannel = new NotificationChannel(
                    Constants.MAIN_NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            newNotificationChannel.setGroup(Constants.MAIN_NOTIFICATION_CHANNEL_GROUP_ID);
            notificationManager.createNotificationChannel(
                    newNotificationChannel
            );
        }

        Notification notification = createNotification(title, "");
        startForeground(Constants.MAIN_CONNECTED_NOTIFICATION_ID, notification);
    }

    private void updateNotificationWithMessage(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        Notification notification = createNotification(title, message);
        notificationManager.notify(Constants.MAIN_CONNECTED_NOTIFICATION_ID, notification);
    }

    private Notification createNotification(String title, String message) {
        // In Api level 24 an above, there is no icon in design!!!
        Notification.Action actionDisconnect = new Notification.Action.Builder(null, getString(R.string.disconnect_action), disconnectPendingIntent)
                .build();
        Notification.Builder builder = new Notification.Builder(this, Constants.MAIN_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.vpn_icon)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
            //.setOngoing(true) // user can't close notification (works only when screen locked)
        }
        builder.setContentTitle(title)
                .setContentText(message)
                .setOnlyAlertOnce(true) // so when data is updated don't make sound and alert in android 8.0+
                //.setAutoCancel(false) // for not remove notification after press it
                .addAction(actionDisconnect)
                .setContentIntent(launchMainActivityPendingIntent);
        return builder.build();
    }
}
