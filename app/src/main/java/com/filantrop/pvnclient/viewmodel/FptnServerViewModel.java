package com.filantrop.pvnclient.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.enums.ConnectionState;
import com.filantrop.pvnclient.repository.FptnServerRepository;
import com.filantrop.pvnclient.utils.DataRateCalculator;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

public class FptnServerViewModel extends AndroidViewModel {
    private final static String TAG = FptnServerViewModel.class.getName();

    private final FptnServerRepository fptnServerRepository;

    public final MutableLiveData<ConnectionState> connectionStateMutableLiveData = new MutableLiveData<>(ConnectionState.DISCONNECTED);
    public final MutableLiveData<String> downloadSpeedAsStringLiveData = new MutableLiveData<>(new DataRateCalculator(1000).getFormatString());
    public final MutableLiveData<String> uploadSpeedAsStringLiveData = new MutableLiveData<>(new DataRateCalculator(1000).getFormatString());
    public final MutableLiveData<String> timerTextLiveData = new MutableLiveData<>("00:00:00");
    public final MutableLiveData<String> errorTextLiveData = new MutableLiveData<>("");
    public final LiveData<List<FptnServerDto>> serverDtoListLiveData;
    public final MutableLiveData<FptnServerDto> selectedServerLiveData = new MutableLiveData<>(FptnServerDto.AUTO);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFuture;

    public FptnServerViewModel(@NonNull Application application) {
        super(application);

        fptnServerRepository = new FptnServerRepository(getApplication());
        serverDtoListLiveData = fptnServerRepository.getAllServersLiveData();
    }

    public ListenableFuture<List<FptnServerDto>> getAllServers() {
        return fptnServerRepository.getAllServersListFuture();
    }

    public int getSelectedServerPosition() {
        FptnServerDto selectedServer = selectedServerLiveData.getValue();
        if (FptnServerDto.AUTO.equals(serverDtoListLiveData.getValue()) || selectedServer == null) {
            return 0;
        }
        List<FptnServerDto> fptnServerDtos = serverDtoListLiveData.getValue();
        if (fptnServerDtos != null && !fptnServerDtos.isEmpty()) {
            for (int i = 0; i < fptnServerDtos.size(); i++) {
                if (selectedServer.equals(fptnServerDtos.get(i))) {
                    // first element in spinner adapter is AUTO
                    return i + 1;
                }
            }
        }
        return 0;
    }

    public void deleteAll() {
        fptnServerRepository.deleteAll();
    }

    public boolean parseAndSaveFptnLink(String url) {
        // removes all whitespaces and non-visible characters (e.g., tab, \n) and prefixes fptn://  fptn:
        final String preparedUrl = url.replaceAll("\\s+","")
                .replace("fptn://", "")
                .replace("fptn:", "");
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(preparedUrl);
            String jsonString = new String(decodedBytes);
            JSONObject jsonObject = new JSONObject(jsonString);
            String username = jsonObject.getString("username");
            String password = jsonObject.getString("password");

            List<FptnServerDto> serverDtoList = new ArrayList<>();
            JSONArray serversArray = jsonObject.getJSONArray("servers");
            for (int i = 0; i < serversArray.length(); i++) {
                JSONObject serverObject = serversArray.getJSONObject(i);
                String name = serverObject.getString("name");
                String host = serverObject.getString("host");
                int port = serverObject.getInt("port");

                serverDtoList.add(new FptnServerDto(name, username, password, host, port));
                Log.i(TAG, "=== SERVER: " + username + " " + password + " " + host + ":" + port);
            }
            if (!serverDtoList.isEmpty()) {
                deleteAll(); // delete old
                fptnServerRepository.insertAll(serverDtoList);
                return true;
            }
            Log.e(TAG, "Servers from fptnLink is empty!");
        } catch (JSONException e) {
            Log.e(TAG, "Can't parse fptnLink!", e);
        } catch (Exception e) {
            Log.e(TAG, "Undefined error:", e);
        }
        return false;
    }

    public void clearErrorTextMessage() {
        errorTextLiveData.setValue("");
    }


    public void startTimer(Instant connectedFrom) {
        Log.d(TAG, "FptnServerViewModel.startTimer: " + connectedFrom);
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        scheduledFuture = scheduler.scheduleWithFixedDelay(() -> {
            Instant now = Instant.now();
            long durationInSeconds = Duration.between(connectedFrom, now).getSeconds();

            long hours = durationInSeconds / 3600;
            long minutes = (durationInSeconds % 3600) / 60;
            long seconds = durationInSeconds % 60;

            // Format and display the time as HH:MM:SS
            String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
            Log.d(TAG, "FptnServerViewModel.time: " + time);
            timerTextLiveData.postValue(time);
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void stopTimer() {
        Log.d(TAG, "FptnServerViewModel.stopTimer()");
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        timerTextLiveData.postValue("00:00:00");
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        stopTimer();
        scheduler.shutdown();
    }
}
