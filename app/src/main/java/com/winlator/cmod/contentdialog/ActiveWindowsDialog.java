package com.winlator.cmod.contentdialog;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.winlator.cmod.R;
import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.winhandler.WinHandler;
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

        // Safe Title and Icon handling (prevents silent aborts if resources are missing)
        try {
            setTitle(activity.getString(R.string.active_windows));
        } catch (Exception e) {
            setTitle("Active Windows");
        }

        try {
            // Using the raw integer from original smali as fallback, or standard ID
            setIcon(0x7f08012d); 
        } catch (Exception e) {
            // Ignore if icon fails to load
        }

        refreshWindowList();
    }

    private void refreshWindowList() {
        XServer xServer = activity.getXServer();
        ArrayList<Window> activeWindows = new ArrayList<>();
        ArrayList<Bitmap> activeIcons = new ArrayList<>();

        // Lock both managers ONCE to prevent deadlocks and concurrent modification
        try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER)) {
            findAppWindows(xServer.windowManager.rootWindow, activeWindows);
            
            // Gather icons while we hold the lock
            for (Window w : activeWindows) {
                activeIcons.add(xServer.pixmapManager.getWindowIcon(w));
            }
        }

        loadWindowViews(activeWindows, activeIcons);
    }

    private void findAppWindows(Window parent, ArrayList<Window> result) {
        if (parent == null) return;

        for (Window child : parent.getChildren()) {
            if (child.attributes.isMapped()) {
                String className = child.getClassName();
                boolean isSystem = false;
                
                if (className != null) {
                    String cls = className.toLowerCase();
                    if (cls.contains("progman") || cls.contains("shell_traywnd") || cls.equals("explorer.exe")) {
                        isSystem = true;
                    }
                }

                String title = child.getName();
                boolean hasTitle = title != null && !title.isEmpty();
                boolean hasClass = className != null && !className.isEmpty();

                // If it's visible, not a taskbar/desktop, and has identity
                if (!isSystem && (hasTitle || hasClass)) {
                    // Check to avoid adding the invisible full-screen root desktop fallback
                    if (!isDesktopWindowFallback(child)) {
                        result.add(child);
                        continue; // Stop searching this branch (prevents listing internal buttons/toolbars)
                    }
                }
            }
            // Recurse into children
            findAppWindows(child, result);
        }
    }

    private boolean isDesktopWindowFallback(Window window) {
        XServer xServer = activity.getXServer();
        if (window.getWidth() >= xServer.screenInfo.width && window.getHeight() >= xServer.screenInfo.height) {
            if (window.getParent() == xServer.windowManager.rootWindow) {
                String title = window.getName();
                return title == null || title.isEmpty() || title.equalsIgnoreCase("Default - Wine desktop");
            }
        }
        return false;
    }

    private void loadWindowViews(ArrayList<Window> windows, ArrayList<Bitmap> icons) {
        LinearLayout llWindowList = findViewById(R.id.llWindowList);
        TextView tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        llWindowList.removeAllViews();

        if (windows.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyMessage.setVisibility(View.GONE);

        GLRenderer renderer = activity.getXServer().getRenderer();
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
            final Bitmap icon = icons.get(i);
            
            View itemView = inflater.inflate(R.layout.active_window_item, currentRow, false);
            
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            itemView.setLayoutParams(itemParams);

            ImageView ivIcon = itemView.findViewById(R.id.ivIcon);
            final ImageView ivWindow = itemView.findViewById(R.id.ivWindow);
            TextView tvName = itemView.findViewById(R.id.tvName);
            TextView tvProcess = itemView.findViewById(R.id.tvProcess);

            String className = window.getClassName();
            String title = window.getName();
            
            if (title == null || title.isEmpty()) title = className;
            if (title == null || title.isEmpty()) title = "Unnamed Window";

            tvName.setText(title);
            tvProcess.setText(className != null && !className.isEmpty() ? className : "Application");

            if (icon != null) {
                ivIcon.setImageBitmap(icon);
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

            // Add dummy view if it's the last item and odd, so it doesn't stretch awkwardly
            if (i == windows.size() - 1 && i % 2 == 0) {
                View dummy = new View(getContext());
                dummy.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1.0f));
                currentRow.addView(dummy);
            }
        }
    }
}
