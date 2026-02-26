package com.winlator.cmod.contentdialog;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
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

        // Fetch windows and then populate views
        ArrayList<Window> windows = collectActiveWindows();
        loadWindowViews(windows);
    }

    private ArrayList<Window> collectActiveWindows() {
        XServer xServer = activity.getXServer();
        ArrayList<Window> result = new ArrayList<>();

        // Lock the XServer to safely iterate the window tree
        try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
            Window root = xServer.windowManager.rootWindow;
            // Iterate children of root to find top-level app windows
            for (Window child : root.getChildren()) {
                if (child.attributes.isMapped() && !isDesktopOrTaskbar(child)) {
                    result.add(child);
                }
            }
        }
        return result;
    }

    private boolean isDesktopOrTaskbar(Window window) {
        String className = window.getClassName();
        if (className == null) return false;
        return className.equalsIgnoreCase("Progman") || 
               className.equalsIgnoreCase("Shell_TrayWnd") ||
               className.equalsIgnoreCase("explorer.exe");
    }

    private void loadWindowViews(ArrayList<Window> windows) {
        LinearLayout llWindowList = findViewById(R.id.llWindowList);
        TextView tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        if (windows.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyMessage.setVisibility(View.GONE);
        llWindowList.removeAllViews();

        XServer xServer = activity.getXServer();
        GLRenderer renderer = xServer.getRenderer();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        
        int previewWidth = (int) UnitUtils.dpToPx(240.0f);
        int previewHeight = (int) UnitUtils.dpToPx(160.0f);

        // Grid-like layout: 2 items per row
        LinearLayout currentRow = null;
        for (int i = 0; i < windows.size(); i++) {
            if (i % 2 == 0) {
                currentRow = new LinearLayout(getContext());
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                currentRow.setLayoutParams(rowParams);
                llWindowList.addView(currentRow);
            }

            final Window window = windows.get(i);
            View itemView = inflater.inflate(R.layout.active_window_item, currentRow, false);
            
            // Adjust layout weight for equal distribution in the row
            LinearLayout.LayoutParams itemParams = (LinearLayout.LayoutParams) itemView.getLayoutParams();
            itemParams.weight = 1;
            itemParams.width = 0;
            itemView.setLayoutParams(itemParams);

            ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
            final ImageView ivWindow = itemView.findViewById(R.id.ivWindow);
            TextView tvName = itemView.findViewById(R.id.tvName);
            TextView tvProcess = itemView.findViewById(R.id.tvProcess);

            // Set Title
            String title = window.getName();
            if (title == null || title.isEmpty()) title = "Unnamed Window";
            tvName.setText(title);

            // Set Process/Class Name
            String className = window.getClassName();
            tvProcess.setText(className != null ? className : "Unknown");

            // Set Icon
            try (XLock lock = xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
                PixmapManager pixmapManager = xServer.pixmapManager;
                Bitmap icon = pixmapManager.getWindowIcon(window);
                if (icon != null) ivIcon.setImageBitmap(icon);
            }

            // Capture Screenshot for Preview
            renderer.captureScreenshot(window, previewWidth, previewHeight, (bitmap) -> {
                if (bitmap != null) {
                    ivWindow.post(() -> ivWindow.setImageBitmap(bitmap));
                }
            });

            // Action: Bring to front and dismiss
            itemView.setOnClickListener(v -> {
                WinHandler winHandler = activity.getWinHandler();
                winHandler.bringToFront(window.getClassName(), window.getHandle());
                dismiss();
            });

            currentRow.addView(itemView);
        }
    }
}
