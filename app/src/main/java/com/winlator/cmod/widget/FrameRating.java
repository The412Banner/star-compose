package com.winlator.cmod.widget;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.winlator.cmod.R;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.StringUtils;

import java.util.HashMap;
import java.util.Locale;

public class FrameRating extends FrameLayout implements Runnable {
    private Context context;
    private long lastTime = 0;
    private int frameCount = 0;
    private float lastFPS = 0;
    private String totalRAM = null;
    private final TextView tvFPS;
    private final TextView tvRenderer;
    private final TextView tvGPU;
    private final TextView tvRAM;
    private final View rowFPS;
    private final View rowCPU;
    private final View rowGPU;
    private final View rowRAM;
    private final View rowRenderer;
    private HashMap graphicsDriverConfig;

    public FrameRating(Context context, HashMap graphicsDriverConfig) {
        this(context, graphicsDriverConfig ,null);
    }

    public FrameRating(Context context, HashMap graphicsDriverConfig, AttributeSet attrs) {
        this(context, graphicsDriverConfig, attrs, 0);
    }

    public FrameRating(Context context, HashMap graphicsDriverConfig, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    // ...
    LayoutInflater.from(context).inflate(R.layout.frame_rating, this, true);

    // YOU MUST INITIALIZE THESE TO AVOID COMPILATION ERRORS:
    tvFPS = findViewById(R.id.TVFPS);
    tvRAM = findViewById(R.id.TVRAM);
    tvRenderer = findViewById(R.id.TVRenderer);
    tvGPU = findViewById(R.id.TVGPU);
    
    // Initialize the row containers (the LinearLayouts in frame_rating.xml)
    rowFPS = (View) tvFPS.getParent(); 
    rowRAM = (View) tvRAM.getParent();
    rowRenderer = (View) tvRenderer.getParent();
    rowGPU = (View) tvGPU.getParent();
    rowCPU = null; // Set to null if not used to satisfy 'final' requirement or remove the variable
}

public void applyConfig(String configString) {
    com.winlator.cmod.core.KeyValueSet config = new com.winlator.cmod.core.KeyValueSet(configString);
    
    // Toggle visibility of the entire ROW so the UI collapses properly
    rowFPS.setVisibility(config.get("showFPS", "0").equals("1") ? View.VISIBLE : View.GONE);
    rowRAM.setVisibility(config.get("showRAM", "0").equals("1") ? View.VISIBLE : View.GONE);
    
    int rendererVisibility = config.get("showRenderer", "0").equals("1") ? View.VISIBLE : View.GONE;
    rowRenderer.setVisibility(rendererVisibility);
    rowGPU.setVisibility(rendererVisibility);
}
    
    private String getTotalRAM() {
        String totalRAM = "";
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        totalRAM = StringUtils.formatBytes(memoryInfo.totalMem);
        return totalRAM;
    }
    
    private String getAvailableRAM() {
        String availableRAM = "";
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long usedMem = memoryInfo.totalMem - memoryInfo.availMem;
        availableRAM = StringUtils.formatBytes(usedMem, false);
        return availableRAM;
    }

    public void setRenderer(String renderer) {
        tvRenderer.setText(renderer);
    }

    public void setGpuName (String gpuName) {
        tvGPU.setText(gpuName);
    }

    public void reset() {
        tvRenderer.setText("OpenGL");
        tvGPU.setText(GPUInformation.getRenderer(graphicsDriverConfig.get("version").toString(), context));
    }

    public void update() {
        if (lastTime == 0) lastTime = SystemClock.elapsedRealtime();
        long time = SystemClock.elapsedRealtime();
        if (time >= lastTime + 500) {
            lastFPS = ((float)(frameCount * 1000) / (time - lastTime));
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
        tvRAM.setText(getAvailableRAM() + " GB Used / " + totalRAM + " Total");
    }

}

