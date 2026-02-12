package com.winlator.cmod.widget;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.winlator.cmod.R;
import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.StringUtils;

import java.util.HashMap;
import java.util.Locale;

public class FrameRating extends FrameLayout implements Runnable {
    private final Context context;
    private long lastTime = 0;
    private int frameCount = 0;
    private float lastFPS = 0;
    private final String totalRAM;
    private final TextView tvFPS;
    private final TextView tvRenderer;
    private final TextView tvGPU;
    private final TextView tvRAM;
    private final View rowFPS;
    private final View rowGPU;
    private final View rowRAM;
    private final View rowRenderer;
    private final HashMap<String, Object> graphicsDriverConfig;

    public FrameRating(Context context, HashMap<String, Object> graphicsDriverConfig) {
        this(context, graphicsDriverConfig, null);
    }

    public FrameRating(Context context, HashMap<String, Object> graphicsDriverConfig, AttributeSet attrs) {
        this(context, graphicsDriverConfig, attrs, 0);
    }

    public FrameRating(Context context, HashMap<String, Object> graphicsDriverConfig, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.graphicsDriverConfig = graphicsDriverConfig;

        LayoutInflater.from(context).inflate(R.layout.frame_rating, this, true);

        // Initialize TextViews
        tvFPS = findViewById(R.id.TVFPS);
        tvRAM = findViewById(R.id.TVRAM);
        tvRenderer = findViewById(R.id.TVRenderer);
        tvGPU = findViewById(R.id.TVGPU);

        // Initialize Row Containers (the LinearLayouts wrap the label + value)
        rowFPS = (View) tvFPS.getParent();
        rowRAM = (View) tvRAM.getParent();
        rowRenderer = (View) tvRenderer.getParent();
        rowGPU = (View) tvGPU.getParent();

        // Pre-calculate total RAM
        this.totalRAM = getTotalRAM();
    }

    /**
     * Updates visibility of elements based on the configuration string from FPSCounterConfigDialog
     */
    public void applyConfig(String configString) {
        KeyValueSet config = new KeyValueSet(configString);

        // "1" = Visible, "0" = Gone
        rowFPS.setVisibility(config.get("showFPS", "1").equals("1") ? View.VISIBLE : View.GONE);
        rowRAM.setVisibility(config.get("showRAM", "1").equals("1") ? View.VISIBLE : View.GONE);

        // Renderer and GPU Name are usually grouped together in the UI
        int rendererVisibility = config.get("showRenderer", "1").equals("1") ? View.VISIBLE : View.GONE;
        rowRenderer.setVisibility(rendererVisibility);
        rowGPU.setVisibility(rendererVisibility);
        
        // Note: CPU Load is not implemented in the layout yet, but if you add it:
        // rowCPU.setVisibility(config.get("showCPULoad", "1").equals("1") ? View.VISIBLE : View.GONE);
    }

    private String getTotalRAM() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return StringUtils.formatBytes(memoryInfo.totalMem);
    }

    private String getAvailableRAM() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long usedMem = memoryInfo.totalMem - memoryInfo.availMem;
        return StringUtils.formatBytes(usedMem, false);
    }

    public void setRenderer(String renderer) {
        tvRenderer.setText(renderer);
    }

    public void setGpuName(String gpuName) {
        tvGPU.setText(gpuName);
    }

    public void reset() {
        tvRenderer.setText("OpenGL");
        Object version = graphicsDriverConfig.get("version");
        tvGPU.setText(GPUInformation.getRenderer(version != null ? version.toString() : "", context));
    }

    public void update() {
        if (lastTime == 0) lastTime = SystemClock.elapsedRealtime();
        long time = SystemClock.elapsedRealtime();
        if (time >= lastTime + 500) {
            lastFPS = ((float) (frameCount * 1000) / (time - lastTime));
            post(this);
            lastTime = time;
            frameCount = 0;
        }
        frameCount++;
    }

    @Override
    public void run() {
        if (getVisibility() == GONE) setVisibility(View.VISIBLE);
        tvFPS.setText(String.format(Locale.ENGLISH, "%.1f", lastFPS));
        tvRAM.setText(getAvailableRAM() + " Used / " + totalRAM);
    }
}
