package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import com.winlator.cmod.R;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.core.KeyValueSet;

public class FPSCounterConfigDialog extends ContentDialog {
    private final CheckBox cbShowFPS;
    private final CheckBox cbShowCPULoad;
    private final CheckBox cbShowGPULoad;
    private final CheckBox cbShowRAM;
    private final CheckBox cbShowRenderer;

    public FPSCounterConfigDialog(Context context, Container container) {
        super(context, R.layout.fps_counter_config_dialog);
        setTitle("FPS Counter Settings");

        cbShowFPS = findViewById(R.id.CBShowFPS);
        cbShowCPULoad = findViewById(R.id.CBShowCPULoad);
        cbShowGPULoad = findViewById(R.id.CBShowGPULoad);
        cbShowRAM = findViewById(R.id.CBShowRAM);
        cbShowRenderer = findViewById(R.id.CBShowRenderer);

        KeyValueSet config = new KeyValueSet(container.getFPSCounterConfig());
        cbShowFPS.setChecked(config.get("showFPS").equals("1"));
        cbShowCPULoad.setChecked(config.get("showCPULoad").equals("1"));
        cbShowGPULoad.setChecked(config.get("showGPULoad").equals("1"));
        cbShowRAM.setChecked(config.get("showRAM").equals("1"));
        cbShowRenderer.setChecked(config.get("showRenderer").equals("1"));

        setOnConfirmCallback(() -> {
            config.put("showFPS", cbShowFPS.isChecked() ? "1" : "0");
            config.put("showCPULoad", cbShowCPULoad.isChecked() ? "1" : "0");
            config.put("showGPULoad", cbShowGPULoad.isChecked() ? "1" : "0");
            config.put("showRAM", cbShowRAM.isChecked() ? "1" : "0");
            config.put("showRenderer", cbShowRenderer.isChecked() ? "1" : "0");
            container.setFPSCounterConfig(config.toString());
        });
    }
}
