package com.filantrop.pvnclient.views;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;
import com.filantrop.pvnclient.views.callback.DBFutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    private FptnServerViewModel fptnViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        initializeVariable();
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);

        ListenableFuture<List<FptnServerDto>> allServersListFuture = fptnViewModel.getAllServers();
        Futures.addCallback(allServersListFuture, (DBFutureCallback<List<FptnServerDto>>) result -> {
            if (result != null && !result.isEmpty()) { // miss login
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        }, this.getMainExecutor());

        // Show HTML
        String html = "<div style=\"text-align:center;\">Use the Telegram <a href=\"https://t.me/fptn_bot\">bot</a> to get your key.</div>";
        TextView label = findViewById(R.id.fptn_login_html_label);
        label.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
        label.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void onLogin(View v) {
        final EditText linkInput = findViewById(R.id.fptn_login_link_input);
        final String fptnLink = linkInput.getText().toString();
        if (fptnViewModel.parseAndSaveFptnLink(fptnLink)) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(getApplicationContext(), "Invalid link format or saving failed", Toast.LENGTH_SHORT).show();
        }
    }
}
