package com.filantrop.pvnclient.views;

import static com.filantrop.pvnclient.core.common.Constants.SELECTED_SERVER;
import static com.filantrop.pvnclient.utils.ResourcesUtils.getStringResourceByName;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.enums.ConnectionState;
import com.filantrop.pvnclient.services.CustomVpnService;
import com.filantrop.pvnclient.utils.CustomSpinner;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;
import com.filantrop.pvnclient.views.adapter.FptnServerAdapter;
import com.filantrop.pvnclient.vpnclient.exception.ErrorCode;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class HomeActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    @Getter
    private FptnServerViewModel fptnViewModel;

    private TextView connectionTimerLabel;
    private TextView connectionTimer;

    private TextView downloadTextView;
    private TextView uploadTextView;

    private TextView statusTextView;
    private TextView errorTextView;

    private TextView connectedServerTextView;

    private View serverInfoFrame;

    private View homeSpeedFrame;

    private CustomSpinner spinnerServers;

    private View settingsMenuItem;

    private ToggleButton startStopButton;

    //for service binding
    private ServiceConnection connection;
    private CustomVpnService vpnService;

    private BottomNavigationView bottomNavigationView;

    private final ActivityResultLauncher<Intent> intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), activityResult -> {
        if (activityResult != null && activityResult.getResultCode() == RESULT_OK) {
            startService(enrichIntent(getServiceIntent()).setAction(CustomVpnService.ACTION_CONNECT));
        } else {
            Toast.makeText(this, R.string.vpn_permission_warning, Toast.LENGTH_SHORT).show();
            fptnViewModel.errorTextLiveData.postValue(getString(R.string.vpn_permission_warning));
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);

        initializeVariable();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // miss back button
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(TAG, "onServiceConnected: " + name);
                CustomVpnService.LocalBinder localBinder = (CustomVpnService.LocalBinder) service;
                vpnService = localBinder.getService();
                vpnService.fptnViewModel = fptnViewModel;
                vpnService.updateConnectionStateInViewModel();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "onServiceDisconnected: " + name);
                vpnService.fptnViewModel = null;
            }
        };
        bindService(getServiceIntent().setAction("ON_BIND"), connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(connection);
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        spinnerServers = findViewById(R.id.home_server_spinner);
        spinnerServers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.isEnabled()) {
                    Object itemAtPosition = parent.getItemAtPosition(position);
                    if (itemAtPosition instanceof FptnServerDto fptnServerDto) {
                        if (fptnViewModel.selectedServerLiveData.getValue() != fptnServerDto) {
                            fptnViewModel.selectedServerLiveData.postValue(fptnServerDto);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        startStopButton = findViewById(R.id.home_do_connect_button);
        startStopButton.setOnClickListener(this::onClickToStartStop);

        downloadTextView = findViewById(R.id.home_download_speed);

        uploadTextView = findViewById(R.id.home_upload_speed);

        homeSpeedFrame = findViewById(R.id.home_speed_frame);

        connectionTimer = findViewById(R.id.home_connection_timer);
        connectionTimerLabel = findViewById(R.id.home_connection_timer_label);
        statusTextView = findViewById(R.id.home_connection_status);
        errorTextView = findViewById(R.id.home_error_text_view);
        connectedServerTextView = findViewById(R.id.home_connected_server_name);

        serverInfoFrame = findViewById(R.id.home_server_info_frame);

        settingsMenuItem = findViewById(R.id.menuSettings);

        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
        fptnViewModel.serverDtoListLiveData.observe(this, fptnServerDtos -> {
            if (fptnServerDtos != null && !fptnServerDtos.isEmpty()) {
                List<FptnServerDto> fixedServers = new ArrayList<>();
                fixedServers.add(FptnServerDto.AUTO);
                fixedServers.addAll(fptnServerDtos);
                spinnerServers.setAdapter(new FptnServerAdapter(fixedServers, R.layout.home_list_recycler_server_item));

                spinnerServers.performClosedEvent(); // FIX SPINNER BACKGROUND
            }
        });
        fptnViewModel.connectionStateMutableLiveData.observe(this, connectionState -> {
            switch (connectionState) {
                case CONNECTING:
                    connectingStateUiItems();
                    break;
                case CONNECTED:
                    connectedStateUiItems();
                    break;
                case DISCONNECTED:
                    disconnectedStateUiItems();
            }
        });
        fptnViewModel.downloadSpeedAsStringLiveData.observe(this, downloadSpeed -> downloadTextView.setText(downloadSpeed));
        fptnViewModel.uploadSpeedAsStringLiveData.observe(this, uploadSpeed -> uploadTextView.setText(uploadSpeed));
        fptnViewModel.timerTextLiveData.observe(this, text -> connectionTimer.setText(text));
        fptnViewModel.errorTextLiveData.observe(this, errorCodeText -> {
            Log.d(TAG, "ErrorCodeText: " + errorCodeText);
            if (errorCodeText != null && !errorCodeText.isEmpty()) {
                ErrorCode errorCode = ErrorCode.Companion.getErrorCodeByValue(errorCodeText);
                String stringResourceByName = getStringResourceByName(getApplicationContext(), errorCode.getValue());
                Log.e(TAG, "Error as text: " + stringResourceByName);

                if (stringResourceByName != null) {
                    errorTextView.setText(stringResourceByName);

                    Snackbar snackbar = Snackbar.make(findViewById(R.id.layout), stringResourceByName, 8000);
                    if (ErrorCode.Companion.isNeedToOfferRefreshToken(errorCode)) {
                        snackbar.setAction("Refresh token...", v -> {
                            Intent browserIntent = new
                                    Intent(Intent.ACTION_VIEW,
                                    Uri.parse(getString(R.string.telegram_bot_link)));
                            startActivity(browserIntent);
                        });
                    }
                    snackbar.show();
                }
            }
        });

        // set info about selected server
        fptnViewModel.selectedServerLiveData.observe(this, fptnServerDto -> {
            connectedServerTextView.setText(fptnServerDto.getServerInfo());
        });

        // FIXME
        bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuHome);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menuHome) {
                return true;
            } else if (itemId == R.id.menuSettings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.menuShare) {
                bottomNavigationView.setSelectedItemId(R.id.menuHome); // don't change
                final String shareTitle = getString(R.string.share_title);
                final String shareMessage = getString(R.string.share_message);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareIntent, shareTitle));
            }
            return false;
        });
        // hide
        disconnectedStateUiItems();
    }

    private void connectingStateUiItems() {
        spinnerServers.setEnabled(false);
        statusTextView.setText(R.string.connecting);
    }

    private void disconnectedStateUiItems() {
        statusTextView.setText(R.string.disconnected);
        connectionTimer.setText("00:00:00");
        downloadTextView.setText("0 Mb/s");
        uploadTextView.setText("0 Mb/s");
        startStopButton.setChecked(false);
        spinnerServers.setEnabled(true);

        hideView(connectionTimer);
        hideView(connectionTimerLabel);
        hideView(serverInfoFrame);
        hideView(homeSpeedFrame);
        showView(spinnerServers);

        // ENABLE SETTINGS
        settingsMenuItem.setEnabled(true);
    }

    private void connectedStateUiItems() {
        statusTextView.setText(R.string.running);
        fptnViewModel.clearErrorTextMessage();
        startStopButton.setChecked(true);
        spinnerServers.setEnabled(false);
        errorTextView.setText("");

        showView(connectionTimer);
        showView(connectionTimerLabel);
        showView(serverInfoFrame);
        showView(homeSpeedFrame);
        hideView(spinnerServers);

        // DISABLE SETTINGS
        settingsMenuItem.setEnabled(false);
    }

    private void hideView(View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private void showView(View view) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public void onClickToStartStop(View v) {
        if (fptnViewModel.connectionStateMutableLiveData.getValue() == ConnectionState.DISCONNECTED) {
            Intent intent = VpnService.prepare(HomeActivity.this);
            if (intent != null) {
                // Запрос на предоставление приложению возможности запускать впн
                intentActivityResultLauncher.launch(intent);
            } else {
                startService(enrichIntent(getServiceIntent()).setAction(CustomVpnService.ACTION_CONNECT));
            }
        } else if (fptnViewModel.connectionStateMutableLiveData.getValue() == ConnectionState.CONNECTED) {
            startService(getServiceIntent().setAction(CustomVpnService.ACTION_DISCONNECT));
        }
    }

    private Intent getServiceIntent() {
        return new Intent(this, CustomVpnService.class);
    }

    private Intent enrichIntent(Intent intent) {
        FptnServerDto server = fptnViewModel.selectedServerLiveData.getValue();
        intent.putExtra(SELECTED_SERVER, server);
        return intent;
    }

}
