package com.admin.keyboard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button enableButton = findViewById(R.id.btn_enable);
        Button switchButton = findViewById(R.id.btn_switch);
        TextView statusText = findViewById(R.id.tv_status);
        RadioGroup layoutGroup = findViewById(R.id.layout_group);

        SharedPreferences preferences = getSharedPreferences(
                KeyboardService.PREFS_NAME, MODE_PRIVATE);
        String layout = preferences.getString(
                KeyboardService.PREF_LAYOUT, KeyboardService.LAYOUT_TKL);
        layoutGroup.check(KeyboardService.LAYOUT_T9.equals(layout)
                ? R.id.layout_t9 : R.id.layout_tkl);
        layoutGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String selected = checkedId == R.id.layout_t9
                        ? KeyboardService.LAYOUT_T9 : KeyboardService.LAYOUT_TKL;
                preferences.edit().putString(KeyboardService.PREF_LAYOUT, selected).apply();
            }
        });

        enableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });

        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showInputMethodPicker();
                }
            }
        });

        updateStatus(statusText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView statusText = findViewById(R.id.tv_status);
        updateStatus(statusText);
    }

    private void updateStatus(TextView statusText) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            boolean isEnabled = false;
            String[] enabledMethods = null;
            try {
                enabledMethods = Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ENABLED_INPUT_METHODS).split(":");
            } catch (Exception e) {
                enabledMethods = new String[0];
            }

            String myIME = getPackageName() + "/.KeyboardService";
            for (String method : enabledMethods) {
                if (method.equals(myIME)) {
                    isEnabled = true;
                    break;
                }
            }

            if (isEnabled) {
                statusText.setText("Status: ENABLED ✓");
                statusText.setTextColor(0xFF4CAF50);
            } else {
                statusText.setText("Status: NOT ENABLED");
                statusText.setTextColor(0xFFF44336);
            }
        }
    }
}
