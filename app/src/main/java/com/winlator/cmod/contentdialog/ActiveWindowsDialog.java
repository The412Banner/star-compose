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

        // Fetch windows using recursive logic and then populate views
        ArrayList<Window> windows = collectActiveWindows();
        loadWindowViews(windows);
    }

    private ArrayList<Window> collectActiveWindows() {
        XServer xServer = activity.getXServer();
        ArrayList<Window> result = new ArrayList<>();

        // Lock the XServer to safely iterate the window tree
        try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
            Window root = xServer.windowManager.rootWindow;
            findApplicationWindows(root, result);
        }
        return result;
    }

    private void findApplicationWindows(Window parent, ArrayList<Window> result) {
        for (Window child : parent.getChildren()) {
            // Check if the window is visible (mapped) and not a system component
            if (child.attributes.isMapped() && !isDesktopOrTaskbar(child)) {
                String name = child.getName();
                
                // FIX: If the parent doesn't have a name, check children for a name
                if (name == null || name.isEmpty()) {
                    for (Window grandChild : child.getChildren()) {
                        String childName = grandChild.getName();
                        if (childName != null && !childName.isEmpty()) {
                            name = childName;
                            break;
                        }
                    }
                }

                // If we found a name (either on the window or its child), treat it as an app window
                if (name != null && !name.isEmpty()) {
                    result.add(child);
                    // Continue to next sibling to avoid listing internal sub-windows
                    continue; 
                }
            }
            // Recurse deeper if no application window was identified at this level
            findApplicationWindows(child, result);
        }
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
            
            LinearLayout.LayoutParams itemParams = (LinearLayout.LayoutParams) itemView.getLayoutParams();
            itemParams.weight = 1;
            itemParams.width = 0;
            itemView.setLayoutParams(itemParams);

            ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
            final ImageView ivWindow = itemView.findViewById(R.id.ivWindow);
            TextView tvName = itemView.findViewById(R.id.tvName);
            TextView tvProcess = itemView.findViewById(R.id.tvProcess);

            // Set Title (with fallback if still empty)
            String title = window.getName();
            if (title == null || title.isEmpty()) {
                // Final fallback check of children for the UI display
                for (Window child : window.getChildren()) {
                    if (child.getName() != null && !child.getName().isEmpty()) {
                        title = child.getName();
                        break;
                    }
                }
            }
            tvName.setText((title == null || title.isEmpty()) ? "Unnamed Window" : title);

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
