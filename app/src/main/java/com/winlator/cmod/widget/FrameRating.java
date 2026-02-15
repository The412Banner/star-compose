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
    private final HashMap<String, ?> graphicsDriverConfig;

    public FrameRating(Context context, HashMap<String, ?> graphicsDriverConfig) {
        this(context, graphicsDriverConfig, null);
    }

    public FrameRating(Context context, HashMap<String, ?> graphicsDriverConfig, AttributeSet attrs) {
        this(context, graphicsDriverConfig, attrs, 0);
    }

    public FrameRating(Context context, HashMap<String, ?> graphicsDriverConfig, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.graphicsDriverConfig = graphicsDriverConfig;

        LayoutInflater.from(context).inflate(R.layout.frame_rating, this, true);

        tvFPS = findViewById(R.id.TVFPS);
        tvRAM = findViewById(R.id.TVRAM);
        tvRenderer = findViewById(R.id.TVRenderer);
        tvGPU = findViewById(R.id.TVGPU);

        rowFPS = findViewById(R.id.RowFPS);
        rowRAM = findViewById(R.id.RowRAM);
        rowRenderer = findViewById(R.id.RowRenderer);
        rowGPU = findViewById(R.id.RowGPU);

        this.totalRAM = getTotalRAM();
    }

    /**
     * Applies the visibility settings from the Container's FPS counter config string.
     */
    public void applyConfig(String configString) {
        if (configString == null || configString.isEmpty()) return;
        KeyValueSet config = new KeyValueSet(configString);

        // Map config keys to View visibility
        if (rowFPS != null) rowFPS.setVisibility(config.get("showFPS", "1").equals("1") ? VISIBLE : GONE);
        if (rowRAM != null) rowRAM.setVisibility(config.get("showRAM", "0").equals("1") ? VISIBLE : GONE);
        
        // Handling CPU Load row (if present in your XML)
        View rowCPULoad = findViewById(R.id.RowCPULoad);
        if (rowCPULoad != null) rowCPULoad.setVisibility(config.get("showCPULoad", "0").equals("1") ? VISIBLE : GONE);

        // Renderer and GPU info usually go together in the HUD
        int rendererVis = config.get("showRenderer", "0").equals("1") ? VISIBLE : GONE;
        if (rowRenderer != null) rowRenderer.setVisibility(rendererVis);
        if (rowGPU != null) rowGPU.setVisibility(rendererVis);
        
        // Logic to hide the whole widget if all rows are hidden
        updateParentVisibility();
    }

    private void updateParentVisibility() {
        boolean anyVisible = (rowFPS != null && rowFPS.getVisibility() == VISIBLE) ||
                             (rowRAM != null && rowRAM.getVisibility() == VISIBLE) ||
                             (rowRenderer != null && rowRenderer.getVisibility() == VISIBLE);
        setVisibility(anyVisible ? VISIBLE : GONE);
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
        if (tvRenderer != null) tvRenderer.setText(renderer);
    }

    public void setGpuName(String gpuName) {
        if (tvGPU != null) tvGPU.setText(gpuName);
    }

    public void reset() {
        if (tvRenderer != null) tvRenderer.setText("OpenGL");
        Object version = graphicsDriverConfig.get("version");
        if (tvGPU != null) tvGPU.setText(GPUInformation.getRenderer(version != null ? version.toString() : "", context));
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
        // Only show if updateParentVisibility hasn't hidden us
        tvFPS.setText(String.format(Locale.ENGLISH, "%.1f", lastFPS));
        tvRAM.setText(getAvailableRAM() + " Used / " + totalRAM);
    }
}
