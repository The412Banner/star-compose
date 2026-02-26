package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.xserver.PixmapManager;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.XLock;
import com.winlator.cmod.xserver.XServer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ActiveWindowsDialog extends ContentDialog {
    private final XServerDisplayActivity activity;

    public ActiveWindowsDialog(XServerDisplayActivity activity) {
        super(activity, 0x7f0c001e); // layout.content_dialog_active_windows
        this.activity = activity;
        setCancelable(true);
        setTitle(0x7f11001f); // string.active_windows
        setIcon(0x7f08012d); // drawable.ic_windows

        ArrayList<Window> windows = collectActiveWindows();
        loadWindowViews(windows);
    }

    private ArrayList<Window> collectActiveWindows() {
        XServer xServer = activity.getXServer();
        ArrayList<Window> result = new ArrayList<>();
        Set<Long> seenHandles = new HashSet<>();

        try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER)) {
            collectActiveWindows(xServer.windowManager.rootWindow, result, seenHandles);
        }
        return result;
    }

    private void collectActiveWindows(Window window, ArrayList<Window> result, Set<Long> seenHandles) {
        if (window.isRenderable() && window != activity.getXServer().windowManager.rootWindow && !window.isDesktopWindow()) {
            String className = window.getClassName();
            long handle = window.getHandle();

            if ((className == null || !className.equalsIgnoreCase("explorer.exe")) && 
                !seenHandles.contains(handle) && window.getContent() != null) {
                seenHandles.add(handle);
                result.add(window);
            }
        }

        for (Window child : window.getChildren()) {
            collectActiveWindows(child, result, seenHandles);
        }
    }

    private void loadWindowViews(ArrayList<Window> windows) {
        if (windows.isEmpty()) {
            findViewById(0x7f0903a7).setVisibility(View.VISIBLE); // tvEmptyMessage
            return;
        }

        XServer xServer = activity.getXServer();
        LinearLayout llWindowList = findViewById(0x7f0904cd);
        llWindowList.removeAllViews();

        GLRenderer renderer = xServer.getRenderer();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        int previewWidth = (int) UnitUtils.dpToPx(240.0f);
        int previewHeight = (int) UnitUtils.dpToPx(160.0f);

        LinearLayout currentRow = null;
        for (int i = windows.size() - 1; i >= 0; i--) {
            if ((windows.size() - 1 - i) % 2 == 0) {
                currentRow = new LinearLayout(getContext());
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
                llWindowList.addView(currentRow);
            }

            final Window window = windows.get(i);
            View itemView = inflater.inflate(0x7f0c001d, currentRow, false); // layout.active_window_item
            ImageView ivIcon = itemView.findViewById(0x7f0904cc);
            final ImageView ivWindow = itemView.findViewById(0x7f0904cf);
            TextView tvName = itemView.findViewById(0x7f0904ce);
            TextView tvProcess = itemView.findViewById(0x7f0903e0);

            String title = window.getName();
            if (title.isEmpty() && window.getParent() != null) title = window.getParent().getName();
            tvName.setText(title);

            String className = window.getClassName();
            tvProcess.setText(className != null ? className : "");

            PixmapManager pixmapManager = xServer.pixmapManager;
            Bitmap icon = pixmapManager.getIcon(window);
            if (icon != null) ivIcon.setImageBitmap(icon);

            renderer.captureScreenshot(window, previewWidth, previewHeight, new GLRenderer.ScreenshotCallback() {
                @Override
                public void onScreenshotTaken(final Bitmap bitmap) {
                    if (bitmap != null) {
                        ivWindow.post(() -> ivWindow.setImageBitmap(bitmap));
                    }
                }
            });

            itemView.setOnClickListener(v -> {
                WinHandler winHandler = activity.getWinHandler();
                winHandler.bringToFront(window.getClassName(), window.getHandle());
                dismiss();
            });

            currentRow.addView(itemView);
        }
    }
}
