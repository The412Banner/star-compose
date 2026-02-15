package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import com.winlator.cmod.R;
import com.winlator.cmod.core.KeyValueSet;
import java.util.HashMap;

public class FPSCounterConfigDialog extends ContentDialog {
    private final CheckBox cbShowFPS;
    private final CheckBox cbShowCPULoad;
    private final CheckBox cbShowGPULoad;
    private final CheckBox cbShowRAM;
    private final CheckBox cbShowRenderer;

    public FPSCounterConfigDialog(Context context, String configString) {
        super(context, R.layout.fps_counter_config_dialog);
        setTitle("FPS Counter Settings");

        cbShowFPS = findViewById(R.id.CBShowFPS);
        cbShowCPULoad = findViewById(R.id.CBShowCPULoad);
        cbShowGPULoad = findViewById(R.id.CBShowGPULoad);
        cbShowRAM = findViewById(R.id.CBShowRAM);
        cbShowRenderer = findViewById(R.id.CBShowRenderer);

        // Parse and set initial state
        HashMap<String, String> config = parseConfig(configString);
        cbShowFPS.setChecked(config.getOrDefault("showFPS", "1").equals("1"));
        cbShowCPULoad.setChecked(config.getOrDefault("showCPULoad", "0").equals("1"));
        cbShowGPULoad.setChecked(config.getOrDefault("showGPULoad", "0").equals("1"));
        cbShowRAM.setChecked(config.getOrDefault("showRAM", "0").equals("1"));
        cbShowRenderer.setChecked(config.getOrDefault("showRenderer", "0").equals("1"));
    }

    /**
     * Integrated show method that handles tag management automatically.
     */
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
            
            // Save the serialized string back to the view's tag
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
