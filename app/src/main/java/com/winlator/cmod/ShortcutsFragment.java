package com.winlator.cmod;

import static androidx.core.content.ContextCompat.getSystemService;
import static com.winlator.cmod.MainActivity.PACKAGE_NAME;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.R;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contentdialog.ShortcutSettingsDialog;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShortcutsFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private ContainerManager manager;
    private ShortcutSettingsDialog currentDialog;
    private ShortcutsAdapter adapter;
    private SharedPreferences prefs;
    private MenuItem sortItem;
    private final String[] sortTypeText = {"Name", "Container Id", "Path", "Playtime", "Play Count", "Last Play Date"};
    private String searchText = "";
    private Container shortcutContainer;
    
    public int curSortType = 0;
    public int curGridType = 0; // 0: List, 1: Small Grid, 2: Big Grid
    public int curListType = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout frameLayout = (FrameLayout) inflater.inflate(R.layout.shortcuts_fragment, container, false);
        recyclerView = frameLayout.findViewById(R.id.RecyclerView);
        emptyTextView = frameLayout.findViewById(R.id.TVEmptyText);

        prefs = requireContext().getSharedPreferences("ShortcutsPref", Context.MODE_PRIVATE);
        curSortType = prefs.getInt("cur_sort_type", 0);
        curGridType = prefs.getInt("cur_grid_type", 0);
        curListType = prefs.getInt("cur_list_type", 1);

        updateRecyclerLayout();
        return frameLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        manager = new ContainerManager(getContext());
        loadShortcutsList();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.shortcuts);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.shortcuts_menu, menu);
        sortItem = menu.findItem(R.id.sort_shortcuts);
        if (sortItem != null) sortItem.setTitle(sortTypeText[curSortType]);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        SharedPreferences.Editor editor = prefs.edit();

        if (itemId == R.id.add_shortcuts) {
            ArrayList<Container> containers = manager.getContainers();
            showContainerSelectionDialog(containers, selected -> {
                shortcutContainer = selected;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 7777);
            });
            return true;
        } else if (itemId == R.id.sort_by_name) curSortType = 0;
        else if (itemId == R.id.sort_by_con_id) curSortType = 1;
        else if (itemId == R.id.sort_by_path) curSortType = 2;
        else if (itemId == R.id.sort_by_playtime) curSortType = 3;
        else if (itemId == R.id.sort_by_play_count) curSortType = 4;
        else if (itemId == R.id.sort_by_play_date) curSortType = 5;
        else if (itemId == R.id.layout_grid_small) { curGridType = 1; curListType = 0; }
        else if (itemId == R.id.layout_grid_big) { curGridType = 2; curListType = 0; }
        else if (itemId == R.id.layout_list_small) { curGridType = 0; curListType = 1; }
        else if (itemId == R.id.layout_list_big) { curGridType = 0; curListType = 2; }
        else if (itemId == R.id.search_shortcut) {
            showSearchDialog();
            return true;
        } else return super.onOptionsItemSelected(menuItem);

        editor.putInt("cur_sort_type", curSortType);
        editor.putInt("cur_grid_type", curGridType);
        editor.putInt("cur_list_type", curListType);
        editor.apply();

        if (sortItem != null) sortItem.setTitle(sortTypeText[curSortType]);
        updateRecyclerLayout();
        loadShortcutsList();
        return true;
    }

    private void showSearchDialog() {
        final EditText input = new EditText(getContext());
        input.setText(searchText);
        new AlertDialog.Builder(getContext())
                .setTitle("Search Shortcuts")
                .setView(input)
                .setPositiveButton("Search", (d, w) -> {
                    searchText = input.getText().toString();
                    loadShortcutsList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateRecyclerLayout() {
        int orientation = getResources().getConfiguration().orientation;
        if (curGridType > 0) {
            int spanCount = (curGridType == 1) ? (orientation == Configuration.ORIENTATION_PORTRAIT ? 5 : 7) : (orientation == Configuration.ORIENTATION_PORTRAIT ? 3 : 5);
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            if (recyclerView.getItemDecorationCount() == 0) {
                recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
            }
        }
    }

    public void loadShortcutsList() {
        if (manager == null) manager = new ContainerManager(getContext());
        ArrayList<Shortcut> shortcuts = manager.loadShortcuts();
        SharedPreferences playtimePrefs = getContext().getSharedPreferences("playtime_stats", Context.MODE_PRIVATE);

        if (!searchText.isEmpty()) {
            shortcuts.removeIf(s -> !s.name.toLowerCase().contains(searchText.toLowerCase()));
        }

        switch (curSortType) {
            case 0 -> shortcuts.sort(Comparator.comparing(s -> s.name));
            case 1 -> shortcuts.sort(Comparator.comparing(s -> s.container.id));
            case 2 -> shortcuts.sort(Comparator.comparing(s -> s.path));
            case 3 -> shortcuts.sort((s1, s2) -> Long.compare(playtimePrefs.getLong(s2.path + "_playtime", 0), playtimePrefs.getLong(s1.path + "_playtime", 0)));
            case 4 -> shortcuts.sort((s1, s2) -> Integer.compare(playtimePrefs.getInt(s2.path + "_play_count", 0), playtimePrefs.getInt(s1.path + "_play_count", 0)));
            case 5 -> shortcuts.sort((s1, s2) -> Long.compare(playtimePrefs.getLong(s2.path + "_play_date", 0), playtimePrefs.getLong(s1.path + "_play_date", 0)));
        }

        shortcuts.removeIf(shortcut -> shortcut == null || shortcut.file == null || !shortcut.file.exists());

        adapter = new ShortcutsAdapter(shortcuts);
        recyclerView.setAdapter(adapter);
        emptyTextView.setVisibility(shortcuts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1337 && resultCode == Activity.RESULT_OK && data != null) {
            Uri iconUri = data.getData();
            if (iconUri != null && currentDialog != null) currentDialog.onIconSelected(iconUri);
        } else if (requestCode == 7777 && resultCode == Activity.RESULT_OK && data != null) {
            handleManualShortcutAddition(data.getData());
        }
    }

    private void handleManualShortcutAddition(Uri selectedFile) {
        String fileName = queryName(getContext().getContentResolver(), selectedFile);
        if (fileName.toLowerCase().endsWith(".exe")) {
            if (shortcutContainer == null) return;

            String uriPath = selectedFile.getPath();
            String relativePath = "";
            String driveLetter = "D:";
            String linuxDrivePrefix = "/home/xuser/.wine/dosdevices/d:";

            // Detect drive based on Android path
            if (uriPath.contains("Download")) {
                driveLetter = "D:";
                linuxDrivePrefix = "/home/xuser/.wine/dosdevices/d:";
                if (uriPath.contains("Download/")) relativePath = uriPath.split("Download/")[1];
                else relativePath = fileName;
            } else if (uriPath.contains("primary:") || uriPath.contains("/storage/emulated/0")) {
                driveLetter = "F:";
                linuxDrivePrefix = "/home/xuser/.wine/dosdevices/f:";
                if (uriPath.contains("primary:")) relativePath = uriPath.split("primary:")[1];
                else if (uriPath.contains("/0/")) relativePath = uriPath.split("/0/")[1];
                else relativePath = fileName;
            } else if (uriPath.contains("imagefs")) {
                driveLetter = "Z:";
                linuxDrivePrefix = "/home/xuser/.wine/dosdevices/z:";
                relativePath = uriPath.split("imagefs")[1];
            } else {
                relativePath = fileName;
            }

            if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
            
            String windowsPath = relativePath.replace("/", "\\\\\\\\");
            String dirPart = relativePath.contains("/") ? relativePath.substring(0, relativePath.lastIndexOf("/")) : "";
            String linuxDirPath = linuxDrivePrefix + (dirPart.isEmpty() ? "" : "/" + dirPart);

            String nameWoutExe = fileName.substring(0, fileName.length() - 4);
            
            StringBuilder sb = new StringBuilder();
            sb.append("[Desktop Entry]\n");
            sb.append("Name=").append(nameWoutExe).append("\n");
            sb.append("Exec=env WINEPREFIX=\"/data/user/0/").append(getContext().getPackageName()).append("/files/imagefs/home/xuser/.wine\" wine ").append(driveLetter).append("\\\\\\\\").append(windowsPath).append("\n");
            sb.append("Type=Application\n");
            sb.append("StartupNotify=true\n");
            sb.append("Path=").append(linuxDirPath).append("\n");

            File desktopDir = shortcutContainer.getDesktopDir();
            if (!desktopDir.exists()) desktopDir.mkdirs();

            File desktopFile = new File(desktopDir, nameWoutExe + ".desktop");
            
            try (FileWriter writer = new FileWriter(desktopFile)) {
                writer.write(sb.toString());
                writer.flush();
                
                getActivity().runOnUiThread(() -> {
                    loadShortcutsList();
                    AppUtils.showToast(getContext(), "Shortcut created!");
                });
            } catch (IOException e) {
                Log.e("ShortcutsFragment", "Creation failed", e);
                AppUtils.showToast(getContext(), "Failed to create shortcut file.");
            }
        } else {
            AppUtils.showToast(getContext(), "Please select an .exe file!");
        }
    }

    private String queryName(ContentResolver resolver, Uri uri) {
        try (Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e("ShortcutsFragment", "Query name failed", e);
        }
        return "Unknown.exe";
    }

    private class ShortcutsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Shortcut> data;

        public interface OnContainerSelectedListener {
            void onContainerSelected(Container container);
        }

        public ShortcutsAdapter(List<Shortcut> data) { this.data = data; }

        @Override
        public int getItemViewType(int position) { return curGridType > 0 ? 1 : 0; }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(viewType == 1 ? R.layout.shortcut_grid_item : R.layout.shortcut_list_item, parent, false);
            return viewType == 1 ? new GridViewHolder(view) : new ListViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            final Shortcut item = data.get(position);
            if (holder instanceof ListViewHolder vh) {
                if (item.icon != null) vh.imageView.setImageBitmap(item.icon);
                else vh.imageView.setImageResource(R.drawable.icon_shortcut);
                vh.title.setText(item.name);
                vh.subtitle.setText(item.container.getName());
                vh.menuButton.setOnClickListener(v -> showListItemMenu(v, item));
                vh.innerArea.setOnClickListener(v -> runFromShortcut(item));
                applyListSizeConstraints(vh);
            } else if (holder instanceof GridViewHolder vh) {
                if (item.icon != null) vh.imageView.setImageBitmap(item.icon);
                else vh.imageView.setImageResource(R.drawable.icon_shortcut);
                vh.title.setText(item.name);
                vh.subtitle.setText(item.container.getName());
                vh.itemView.setOnClickListener(v -> runFromShortcut(item));
                vh.itemView.setOnLongClickListener(v -> { showListItemMenu(v, item); return true; });
            }
        }

        private void applyListSizeConstraints(ListViewHolder vh) {
            ViewGroup.LayoutParams params = vh.imageView.getLayoutParams();
            if (curListType == 1) { params.width = params.height = 128; vh.title.setTextSize(14); }
            else { params.width = params.height = 192; vh.title.setTextSize(22); }
            vh.imageView.setLayoutParams(params);
        }

        @Override
        public int getItemCount() { return data.size(); }

        private class ListViewHolder extends RecyclerView.ViewHolder {
            private final ImageButton menuButton;
            private final ImageView imageView;
            private final TextView title, subtitle;
            private final View innerArea;
            private ListViewHolder(View view) {
                super(view);
                this.imageView = view.findViewById(R.id.ImageView);
                this.title = view.findViewById(R.id.TVTitle);
                this.subtitle = view.findViewById(R.id.TVSubtitle);
                this.menuButton = view.findViewById(R.id.BTMenu);
                this.innerArea = view.findViewById(R.id.LLInnerArea);
            }
        }

        private class GridViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imageView;
            private final TextView title, subtitle;
            private GridViewHolder(View view) {
                super(view);
                this.imageView = view.findViewById(R.id.ImageView);
                this.title = view.findViewById(R.id.TVTitle);
                this.subtitle = view.findViewById(R.id.TVSubtitle);
            }
        }

        private void showListItemMenu(View anchorView, final Shortcut shortcut) {
            PopupMenu menu = new PopupMenu(getContext(), anchorView);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) menu.setForceShowIcon(true);
            menu.inflate(R.menu.shortcut_popup_menu);
            menu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();
                if (id == R.id.shortcut_settings) showShortcutSettings(shortcut);
                else if (id == R.id.shortcut_remove) {
                    ContentDialog.confirm(getContext(), R.string.do_you_want_to_remove_this_shortcut, () -> {
                        if (shortcut.file.delete()) {
                            File lnk = new File(shortcut.file.getPath().replace(".desktop", ".lnk"));
                            if (lnk.exists()) lnk.delete();
                            disableShortcutOnScreen(requireContext(), shortcut);
                            loadShortcutsList();
                        }
                    });
                } else if (id == R.id.shortcut_clone_to_container) {
                    showContainerSelectionDialog(manager.getContainers(), selected -> {
                        if (shortcut.cloneToContainer(selected)) loadShortcutsList();
                    });
                } else if (id == R.id.shortcut_add_to_home_screen) {
                    if (shortcut.getExtra("uuid").isEmpty()) shortcut.genUUID();
                    addShortcut_to_HomeScreen(shortcut);
                } else if (id == R.id.shortcut_export) exportShortcut(shortcut);
                else if (id == R.id.shortcut_properties) showShortcutProperties(shortcut);
                return true;
            });
            menu.show();
        }
    }

    private void showContainerSelectionDialog(ArrayList<Container> containers, ShortcutsAdapter.OnContainerSelectedListener listener) {
        String[] names = new String[containers.size()];
        for (int i = 0; i < containers.size(); i++) names[i] = containers.get(i).getName();
        new AlertDialog.Builder(getContext()).setTitle("Select Container").setItems(names, (d, w) -> listener.onContainerSelected(containers.get(w))).show();
    }

    private void runFromShortcut(Shortcut shortcut) {
        Activity activity = getActivity();
        if (shortcut.file == null || !shortcut.file.exists()) {
            AppUtils.showToast(getContext(), "Shortcut file is missing!");
            loadShortcutsList();
            return;
        }
        
        if (!XrActivity.isEnabled(getContext())) {
            Intent intent = new Intent(activity, XServerDisplayActivity.class);
            intent.putExtra("container_id", shortcut.container.id);
            intent.putExtra("shortcut_path", shortcut.file.getPath());
            intent.putExtra("shortcut_name", shortcut.name);
            intent.putExtra("disableXinput", shortcut.getExtra("disableXinput", "0"));
            activity.startActivity(intent);
        } else XrActivity.openIntent(activity, shortcut.container.id, shortcut.file.getPath());
    }

    private void exportShortcut(Shortcut shortcut) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String uriString = sharedPreferences.getString("shortcuts_export_path_uri", null);
        File shortcutsDir = (uriString != null) ? new File(FileUtils.getFilePathFromUri(getContext(), Uri.parse(uriString))) : 
                           new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Winlator/Shortcuts");
        
        if (!shortcutsDir.exists() && !shortcutsDir.mkdirs()) return;
        File exportFile = new File(shortcutsDir, shortcut.file.getName());
        try (FileWriter writer = new FileWriter(exportFile)) {
            writer.write("container_id:" + shortcut.container.id + "\n");
            AppUtils.showToast(getContext(), "Exported to " + exportFile.getPath());
        } catch (IOException e) { Log.e("ShortcutsFragment", "Export failed", e); }
    }

    private void exportShortcutToFrontend(Shortcut shortcut) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        String uriString = sp.getString("frontend_export_uri", null);
        File dir = (uriString != null) ? new File(FileUtils.getFilePathFromUri(getContext(), Uri.parse(uriString))) : 
                  new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Winlator/Frontend");
        if (!dir.exists() && !dir.mkdirs()) return;
        
        File exportFile = new File(dir, shortcut.file.getName());
        try (FileWriter writer = new FileWriter(exportFile)) {
            writer.write("container_id:" + shortcut.container.id + "\n");
            AppUtils.showToast(getContext(), "Frontend Shortcut Exported!");
        } catch (IOException e) { Log.e("ShortcutsFragment", "Frontend Export failed", e); }
    }

    private void showShortcutProperties(Shortcut shortcut) {
        SharedPreferences sp = getContext().getSharedPreferences("playtime_stats", Context.MODE_PRIVATE);
        long time = sp.getLong(shortcut.name + "_playtime", 0);
        int count = sp.getInt(shortcut.name + "_play_count", 0);
        
        ContentDialog dialog = new ContentDialog(getContext(), R.layout.shortcut_properties_dialog);
        dialog.setTitle("Properties");
        ((TextView) dialog.findViewById(R.id.play_count)).setText("Played: " + count);
        ((TextView) dialog.findViewById(R.id.playtime)).setText("Playtime: " + (time / 60000) + "m");
        dialog.findViewById(R.id.reset_properties).setOnClickListener(v -> {
            sp.edit().remove(shortcut.name + "_playtime").remove(shortcut.name + "_play_count").apply();
            dialog.dismiss();
            loadShortcutsList();
        });
        dialog.show();
    }

    private ShortcutInfo buildScreenShortCut(String label, String longLabel, int cId, String path, Icon icon, String uuid) {
        Intent intent = new Intent(getActivity(), XServerDisplayActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra("container_id", cId);
        intent.putExtra("shortcut_path", path);
        return new ShortcutInfo.Builder(getActivity(), uuid).setShortLabel(label).setLongLabel(longLabel).setIcon(icon).setIntent(intent).build();
    }

    private void addShortcut_to_HomeScreen(Shortcut shortcut) {
        ShortcutManager sm = getSystemService(requireContext(), ShortcutManager.class);
        if (sm != null && sm.isRequestPinShortcutSupported()) {
            sm.requestPinShortcut(buildScreenShortCut(shortcut.name, shortcut.name, shortcut.container.id, shortcut.file.getPath(), Icon.createWithBitmap(shortcut.icon), shortcut.getExtra("uuid")), null);
        }
    }

    public static void disableShortcutOnScreen(Context context, Shortcut shortcut) {
        ShortcutManager sm = getSystemService(context, ShortcutManager.class);
        try { sm.disableShortcuts(Collections.singletonList(shortcut.getExtra("uuid")), "Unavailable"); } catch (Exception ignored) {}
    }

    public void updateShortcutOnScreen(String shortLabel, String longLabel, int containerId, String shortcutPath, Icon icon, String uuid) {
        ShortcutManager sm = getSystemService(requireContext(), ShortcutManager.class);
        try {
            for (ShortcutInfo info : sm.getPinnedShortcuts()) {
                if (info.getId().equals(uuid)) {
                    sm.updateShortcuts(Collections.singletonList(buildScreenShortCut(shortLabel, longLabel, containerId, shortcutPath, icon, uuid)));
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private void showShortcutSettings(Shortcut shortcut) {
        currentDialog = new ShortcutSettingsDialog(this, shortcut);
        currentDialog.show();
    }
}
