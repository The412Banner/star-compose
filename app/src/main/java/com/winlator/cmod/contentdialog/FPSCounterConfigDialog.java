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

    // We now pass the String config instead of the Container object
    public FPSCounterConfigDialog(Context context, String initialConfig) {
        super(context, R.layout.fps_counter_config_dialog);
        this.configString = initialConfig;
        setTitle("FPS Counter Settings");

        cbShowFPS = findViewById(R.id.CBShowFPS);
        cbShowCPULoad = findViewById(R.id.CBShowCPULoad);
        cbShowGPULoad = findViewById(R.id.CBShowGPULoad);
        cbShowRAM = findViewById(R.id.CBShowRAM);
        cbShowRenderer = findViewById(R.id.CBShowRenderer);

        // Load values from the provided string
        KeyValueSet config = new KeyValueSet(initialConfig);
        cbShowFPS.setChecked(config.get("showFPS").equals("1"));
        cbShowCPULoad.setChecked(config.get("showCPULoad").equals("1"));
        cbShowGPULoad.setChecked(config.get("showGPULoad").equals("1"));
        cbShowRAM.setChecked(config.get("showRAM").equals("1"));
        cbShowRenderer.setChecked(config.get("showRenderer").equals("1"));

        setOnConfirmCallback(() -> {
            // Update the local configString when OK is pressed
            KeyValueSet newConfig = new KeyValueSet();
            newConfig.put("showFPS", cbShowFPS.isChecked() ? "1" : "0");
            newConfig.put("showCPULoad", cbShowCPULoad.isChecked() ? "1" : "0");
            newConfig.put("showGPULoad", cbShowGPULoad.isChecked() ? "1" : "0");
            newConfig.put("showRAM", cbShowRAM.isChecked() ? "1" : "0");
            newConfig.put("showRenderer", cbShowRenderer.isChecked() ? "1" : "0");
            this.configString = newConfig.toString();
        });
    }

    // Getter to retrieve the modified string back in the Fragment
    public String getConfigString() {
        return configString;
    }
}
            config.put("showGPULoad", cbShowGPULoad.isChecked() ? "1" : "0");
            config.put("showRAM", cbShowRAM.isChecked() ? "1" : "0");
            config.put("showRenderer", cbShowRenderer.isChecked() ? "1" : "0");
            container.setFPSCounterConfig(config.toString());
        });
    }
}
