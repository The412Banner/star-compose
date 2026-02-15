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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Locale;

public class FrameRating extends FrameLayout implements Runnable {
    private final Context context;
    private long lastTime = 0;
    private int frameCount = 0;
    private float lastFPS = 0;
    private float cpuTemp = 0; // Changed from cpuLoad to cpuTemp
    private int gpuLoad = 0;
    private final String totalRAM;

    private final TextView tvFPS;
    private final TextView tvRenderer;
    private final TextView tvGPU;
    private final TextView tvRAM;
    private final TextView tvCPUTemp; // Renamed from tvCPULoad
    private final TextView tvGPULoad;

    private final View rowFPS;
    private final View rowGPU;
    private final View rowRAM;
    private final View rowRenderer;
    private final View rowCPUTemp; // Renamed from rowCPULoad
    private final View rowGPULoad;

    private final HashMap<String, ?> graphicsDriverConfig;

    // Common paths for CPU temperature on various Android devices
    private static final String[] THERMAL_PATHS = {
        "/sys/class/thermal/thermal_zone0/temp",
        "/sys/class/thermal/thermal_zone1/temp",
        "/sys/devices/virtual/thermal/thermal_zone0/temp",
        "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp"
    };

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

        // Bind TextViews
        tvFPS = findViewById(R.id.TVFPS);
        tvRAM = findViewById(R.id.TVRAM);
        tvRenderer = findViewById(R.id.TVRenderer);
        tvGPU = findViewById(R.id.TVGPU);
        tvCPUTemp = findViewById(R.id.TVCPULoad); // Keeping ID for XML compatibility
        tvGPULoad = findViewById(R.id.TVGPULoad);

        // Bind Rows
        rowFPS = findViewById(R.id.RowFPS);
        rowRAM = findViewById(R.id.RowRAM);
        rowRenderer = findViewById(R.id.RowRenderer);
        rowGPU = findViewById(R.id.RowGPU);
        rowCPUTemp = findViewById(R.id.RowCPULoad); // Keeping ID for XML compatibility
        rowGPULoad = findViewById(R.id.RowGPULoad);

        this.totalRAM = getTotalRAM();
    }

    public void applyConfig(String configString) {
        if (configString == null || configString.isEmpty()) return;
        KeyValueSet config = new KeyValueSet(configString);

        if (rowFPS != null) rowFPS.setVisibility(config.get("showFPS", "1").equals("1") ? VISIBLE : GONE);
        if (rowRAM != null) rowRAM.setVisibility(config.get("showRAM", "0").equals("1") ? VISIBLE : GONE);
        if (rowCPUTemp != null) rowCPUTemp.setVisibility(config.get("showCPULoad", "0").equals("1") ? VISIBLE : GONE);
        if (rowGPULoad != null) rowGPULoad.setVisibility(config.get("showGPULoad", "0").equals("1") ? VISIBLE : GONE);

        int rendererVis = config.get("showRenderer", "0").equals("1") ? VISIBLE : GONE;
        if (rowRenderer != null) rowRenderer.setVisibility(rendererVis);
        if (rowGPU != null) rowGPU.setVisibility(rendererVis);
        
        updateParentVisibility();
    }

    private void updateParentVisibility() {
        boolean anyVisible = (rowFPS != null && rowFPS.getVisibility() == VISIBLE) ||
                             (rowRAM != null && rowRAM.getVisibility() == VISIBLE) ||
                             (rowRenderer != null && rowRenderer.getVisibility() == VISIBLE) ||
                             (rowGPU != null && rowGPU.getVisibility() == VISIBLE) ||
                             (rowCPUTemp != null && rowCPUTemp.getVisibility() == VISIBLE) ||
                             (rowGPULoad != null && rowGPULoad.getVisibility() == VISIBLE);
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

    /**
     * Scans system thermal zones to find CPU temperature.
     */
    private float getCPUTemperature() {
        for (String path : THERMAL_PATHS) {
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String line = reader.readLine();
                if (line != null) {
                    float temp = Float.parseFloat(line.trim());
                    // Convert millidegrees (standard on most Androids) to Celsius
                    return temp > 1000 ? temp / 1000.0f : temp;
                }
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private int calculateGPULoad() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/sys/class/kgsl/kgsl-3d0/gpubusy"));
            String line = reader.readLine();
            reader.close();
            if (line != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    long busy = Long.parseLong(parts[0]);
                    long total = Long.parseLong(parts[1]);
                    if (total != 0) return (int) ((busy * 100) / total);
                }
            }
        } catch (Exception e) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/sys/class/misc/mali0/device/utilisation"));
                String line = reader.readLine();
                reader.close();
                if (line != null) return Integer.parseInt(line.trim());
            } catch (Exception e2) {}
        }
        return 0;
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
            
            // REPLACED: Temperature instead of Load
            cpuTemp = getCPUTemperature();
            gpuLoad = calculateGPULoad();
            
            post(this); 
            lastTime = time;
            frameCount = 0;
        }
        frameCount++;
    }

    @Override
    public void run() {
        if (tvFPS != null) tvFPS.setText(String.format(Locale.ENGLISH, "%.1f", lastFPS));
        if (tvRAM != null) tvRAM.setText(getAvailableRAM() + " Used / " + totalRAM);
        
        // Display as Temperature (e.g., 45.5°C)
        if (tvCPUTemp != null) tvCPUTemp.setText(String.format(Locale.ENGLISH, "%.1f°C", cpuTemp));
        
        if (tvGPULoad != null) tvGPULoad.setText(gpuLoad + "%");
    }
}
