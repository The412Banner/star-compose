package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.widget.CheckBox;
import com.winlator.cmod.R;
import com.winlator.cmod.core.KeyValueSet;

public class FPSCounterConfigDialog extends ContentDialog {
    private final CheckBox cbShowFPS;
    private final CheckBox cbShowCPULoad;
    private final CheckBox cbShowGPULoad;
    private final CheckBox cbShowRAM;
    private final CheckBox cbShowRenderer;
    private String configString;

    public FPSCounterConfigDialog(Context context, String initialConfig) {
        super(context, R.layout.fps_counter_config_dialog);
        this.configString = initialConfig;
        setTitle("FPS Counter Settings");

        cbShowFPS = findViewById(R.id.CBShowFPS);
        cbShowCPULoad = findViewById(R.id.CBShowCPULoad);
        cbShowGPULoad = findViewById(R.id.CBShowGPULoad);
        cbShowRAM = findViewById(R.id.CBShowRAM);
        cbShowRenderer = findViewById(R.id.CBShowRenderer);

        // Load current values
        KeyValueSet config = new KeyValueSet(initialConfig);
        cbShowFPS.setChecked(config.get("showFPS").equals("1"));
        cbShowCPULoad.setChecked(config.get("showCPULoad").equals("1"));
        cbShowGPULoad.setChecked(config.get("showGPULoad").equals("1"));
        cbShowRAM.setChecked(config.get("showRAM").equals("1"));
        cbShowRenderer.setChecked(config.get("showRenderer").equals("1"));

        // When "Confirm" is clicked, we must bake the UI state into the configString
        setOnConfirmCallback(() -> {
            KeyValueSet newConfig = new KeyValueSet();
            newConfig.put("showFPS", cbShowFPS.isChecked() ? "1" : "0");
            newConfig.put("showCPULoad", cbShowCPULoad.isChecked() ? "1" : "0");
            newConfig.put("showGPULoad", cbShowGPULoad.isChecked() ? "1" : "0");
            newConfig.put("showRAM", cbShowRAM.isChecked() ? "1" : "0");
            newConfig.put("showRenderer", cbShowRenderer.isChecked() ? "1" : "0");
            
            // CRITICAL: Update the member variable so getConfigString() returns the NEW data
            this.configString = newConfig.toString();
        });
    }

    public String getConfigString() {
        return configString;
    }
}
