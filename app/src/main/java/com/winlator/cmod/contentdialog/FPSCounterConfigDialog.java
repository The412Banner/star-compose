package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import com.winlator.cmod.R;
import com.winlator.cmod.core.KeyValueSet;
import java.util.HashMap;

public class FPSCounterConfigDialog extends ContentDialog {
    private final CheckBox cbShowFPS;
    private final CheckBox cbShowCPULoad;
    private final CheckBox cbShowGPULoad;
    private final CheckBox cbShowRAM;
    private final CheckBox cbShowRenderer;
    private final CheckBox cbShowBatteryTemp;
    private final CheckBox cbShowBatteryVoltage;
    private final SeekBar sbHUDScale;
    private final TextView tvHUDScaleValue;
    private final SeekBar sbHUDTransparency; // New
    private final TextView tvHUDTransparencyValue; // New

    public FPSCounterConfigDialog(Context context, String configString) {
        super(context, R.layout.fps_counter_config_dialog);
        setTitle("FPS Counter Settings");

        cbShowFPS = findViewById(R.id.CBShowFPS);
        cbShowCPULoad = findViewById(R.id.CBShowCPULoad);
        cbShowGPULoad = findViewById(R.id.CBShowGPULoad);
        cbShowRAM = findViewById(R.id.CBShowRAM);
        cbShowRenderer = findViewById(R.id.CBShowRenderer);
        cbShowBatteryTemp = findViewById(R.id.CBShowBatteryTemp);
        cbShowBatteryVoltage = findViewById(R.id.CBShowBatteryVoltage);
        sbHUDScale = findViewById(R.id.SBHUDScale);
        tvHUDScaleValue = findViewById(R.id.TVHUDScaleValue);
        sbHUDTransparency = findViewById(R.id.SBHUDTransparency); // New
        tvHUDTransparencyValue = findViewById(R.id.TVHUDTransparencyValue); // New

        // Parse and set initial state
        HashMap<String, String> config = parseConfig(configString);
        cbShowFPS.setChecked(config.getOrDefault("showFPS", "1").equals("1"));
        cbShowCPULoad.setChecked(config.getOrDefault("showCPULoad", "0").equals("1"));
        cbShowGPULoad.setChecked(config.getOrDefault("showGPULoad", "0").equals("1"));
        cbShowRAM.setChecked(config.getOrDefault("showRAM", "0").equals("1"));
        cbShowRenderer.setChecked(config.getOrDefault("showRenderer", "0").equals("1"));
        cbShowBatteryTemp.setChecked(config.getOrDefault("showBatteryTemp", "0").equals("1"));
        cbShowBatteryVoltage.setChecked(config.getOrDefault("showBatteryVoltage", "0").equals("1"));

        // Initialize HUD Scale (Range 50-150, default 100)
        int initialScale = Integer.parseInt(config.getOrDefault("hudScale", "100"));
        sbHUDScale.setProgress(initialScale);
        tvHUDScaleValue.setText(initialScale + "%");

        sbHUDScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int finalValue = Math.max(50, progress);
                tvHUDScaleValue.setText(finalValue + "%");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() < 50) seekBar.setProgress(50);
            }
        });

        // Initialize HUD Transparency (Range 0-50, default 0)
        int initialTrans = Integer.parseInt(config.getOrDefault("hudTransparency", "0"));
        sbHUDTransparency.setMax(50);
        sbHUDTransparency.setProgress(initialTrans);
        tvHUDTransparencyValue.setText(String.valueOf(initialTrans));

        sbHUDTransparency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvHUDTransparencyValue.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public static void show(Context context, View anchorView) {
        String currentTag = anchorView.getTag() != null ? anchorView.getTag().toString() : "";
        FPSCounterConfigDialog dialog = new FPSCounterConfigDialog(context, currentTag);
        
        dialog.setOnConfirmCallback(() -> {
            HashMap<String, String> config = new HashMap<>();
            config.put("showFPS", dialog.cbShowFPS.isChecked() ? "1" : "0");
            config.put("showCPULoad", dialog.cbShowCPULoad.isChecked() ? "1" : "0");
            config.put("showGPULoad", dialog.cbShowGPULoad.isChecked() ? "1" : "0");
            config.put("showRAM", dialog.cbShowRAM.isChecked() ? "1" : "0");
            config.put("showRenderer", dialog.cbShowRenderer.isChecked() ? "1" : "0");
            config.put("showBatteryTemp", dialog.cbShowBatteryTemp.isChecked() ? "1" : "0");
            config.put("showBatteryVoltage", dialog.cbShowBatteryVoltage.isChecked() ? "1" : "0");
            
            // Save Scale and Transparency values
            config.put("hudScale", String.valueOf(Math.max(50, dialog.sbHUDScale.getProgress())));
            config.put("hudTransparency", String.valueOf(dialog.sbHUDTransparency.getProgress()));
            
            anchorView.setTag(toConfigString(config));
        });
        dialog.show();
    }

    private static HashMap<String, String> parseConfig(String configString) {
        HashMap<String, String> config = new HashMap<>();
        if (configString == null || configString.isEmpty()) return config;
        KeyValueSet kv = new KeyValueSet(configString);
        for (String[] entry : kv) config.put(entry[0], entry[1]);
        return config;
    }

    private static String toConfigString(HashMap<String, String> config) {
        KeyValueSet kv = new KeyValueSet();
        for (String key : config.keySet()) kv.put(key, config.get(key));
        return kv.toString();
    }
}
