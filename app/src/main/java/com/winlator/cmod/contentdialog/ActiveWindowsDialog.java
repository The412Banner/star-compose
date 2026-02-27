package com.winlator.cmod.contentdialog;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.winlator.cmod.R;
import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.xserver.PixmapManager;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.XLock;
import com.winlator.cmod.xserver.XServer;

import java.util.ArrayList;

public class ActiveWindowsDialog extends ContentDialog {
    private final XServerDisplayActivity activity;

    public ActiveWindowsDialog(XServerDisplayActivity activity) {
        super(activity, R.layout.active_windows_dialog);
        this.activity = activity;
        setCancelable(true);
        
        setTitle(activity.getString(R.string.active_windows));
        setIcon(R.drawable.icon_active_windows);

        // Populate the list immediately
        refreshWindowList();
    }

    private void refreshWindowList() {
        XServer xServer = activity.getXServer();
        ArrayList<Window> activeWindows = new ArrayList<>();

        // Use a single lock for the entire collection process to prevent UI hanging
        try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
            collectVisibleWindows(xServer.windowManager.rootWindow, activeWindows);
        }

        loadWindowViews(activeWindows);
    }

    private void collectVisibleWindows(Window parent, ArrayList<Window> result) {
        if (parent == null) return;

        for (Window child : parent.getChildren()) {
            // Check if the window is actually 'Mapped' (Visible to the user)
            // and has actual content to display
            if (child.attributes.isMapped() && child.getContent() != null) {
                String className = child.getClassName();
                
                // Filter out system components that shouldn't be in a 'Task Switcher'
                boolean isSystem = className != null && (
                    className.equalsIgnoreCase("Shell_TrayWnd") || 
                    className.equalsIgnoreCase("Progman") ||
                    className.equalsIgnoreCase("Explorer.EXE")
                );

                if (!isSystem) {
                    result.add(child);
                }
            }
            // Recurse to find child windows (some apps wrap their main view in a container)
            collectVisibleWindows(child, result);
        }
    }

    private void loadWindowViews(ArrayList<Window> windows) {
        LinearLayout llWindowList = findViewById(R.id.llWindowList);
        TextView tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        llWindowList.removeAllViews();

        if (windows.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyMessage.setVisibility(View.GONE);

        XServer xServer = activity.getXServer();
        GLRenderer renderer = xServer.getRenderer();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        
        int previewWidth = (int) UnitUtils.dpToPx(240.0f);
        int previewHeight = (int) UnitUtils.dpToPx(160.0f);

        for (final Window window : windows) {
            View itemView = inflater.inflate(R.layout.active_window_item, llWindowList, false);
            
            ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
            final ImageView ivWindow = itemView.findViewById(R.id.ivWindow);
            TextView tvName = itemView.findViewById(R.id.tvName);
            TextView tvProcess = itemView.findViewById(R.id.tvProcess);

            // Logic to get a readable name
            String name = window.getName();
            String className = window.getClassName();
            
            tvName.setText((name != null && !name.isEmpty()) ? name : 
                          (className != null ? className : "Application"));
            tvProcess.setText(className != null ? className : "Unknown Process");

            // Set Icon
            try (XLock lock = xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
                Bitmap icon = xServer.pixmapManager.getWindowIcon(window);
                if (icon != null) ivIcon.setImageBitmap(icon);
            }

            // Preview Screenshot
            renderer.captureScreenshot(window, previewWidth, previewHeight, (bitmap) -> {
                if (bitmap != null) {
                    ivWindow.post(() -> ivWindow.setImageBitmap(bitmap));
                }
            });

            // Interaction
            itemView.setOnClickListener(v -> {
                WinHandler winHandler = activity.getWinHandler();
                // We use className and Handle to tell the Windows side which one to focus
                winHandler.bringToFront(window.getClassName(), window.getHandle());
                dismiss();
            });

            llWindowList.addView(itemView);
        }
    }
}
