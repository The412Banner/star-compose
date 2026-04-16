package com.winlator.cmod;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.contentdialog.DXVKConfigDialog;
import com.winlator.cmod.contentdialog.ShortcutSettingsDialog;
import com.winlator.cmod.contentdialog.WineD3DConfigDialog;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Static helper methods extracted from ContainerDetailFragment so they can be used
 * by ShortcutSettingsDialog without creating a Fragment instance.
 */
public class ContainerDetailHelper {

    public static void updateGraphicsDriverSpinner(Context context, Spinner spinner) {
        String[] items = context.getResources().getStringArray(R.array.graphics_driver_entries);
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, items));
    }

    public static void setupDXWrapperSpinner(final Spinner sDXWrapper, final View vDXWrapperConfig, boolean isARM64EC) {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String dxwrapper = StringUtils.parseIdentifier(sDXWrapper.getSelectedItem());
                if (dxwrapper.contains("dxvk")) {
                    vDXWrapperConfig.setOnClickListener((v) -> new DXVKConfigDialog(vDXWrapperConfig, isARM64EC).show());
                } else {
                    vDXWrapperConfig.setOnClickListener((v) -> new WineD3DConfigDialog(vDXWrapperConfig).show());
                }
                vDXWrapperConfig.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        sDXWrapper.setOnItemSelectedListener(listener);
        int pos = sDXWrapper.getSelectedItemPosition();
        if (pos >= 0) {
            listener.onItemSelected(sDXWrapper, sDXWrapper.getSelectedView(), pos, sDXWrapper.getSelectedItemId());
        }
    }

    public static void createWinComponentsTabFromShortcut(ShortcutSettingsDialog dialog, View view,
                                                           String wincomponents, boolean isDarkMode) {
        Context context = dialog.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup tabView = view.findViewById(R.id.LLTabWinComponents);
        ViewGroup directxSection = tabView.findViewById(R.id.LLWinComponentsDirectX);
        ViewGroup generalSection = tabView.findViewById(R.id.LLWinComponentsGeneral);

        for (String[] wc : new KeyValueSet(wincomponents)) {
            ViewGroup parent = wc[0].startsWith("direct") ? directxSection : generalSection;
            View itemView = inflater.inflate(R.layout.wincomponent_list_item, parent, false);
            ((TextView) itemView.findViewById(R.id.TextView)).setText(StringUtils.getString(context, wc[0]));
            Spinner spinner = itemView.findViewById(R.id.Spinner);
            spinner.setSelection(Integer.parseInt(wc[1]), false);
            spinner.setTag(wc[0]);
            spinner.setPopupBackgroundResource(isDarkMode
                    ? R.drawable.content_dialog_background_dark
                    : R.drawable.content_dialog_background);
            parent.addView(itemView);
        }

        dialog.onWinComponentsViewsAdded(isDarkMode);
    }

    public static void loadBox64VersionSpinner(Context context, Container container,
                                               ContentsManager manager, Spinner spinner,
                                               boolean isArm64EC) {
        List<String> itemList;
        if (isArm64EC) {
            itemList = new ArrayList<>(Arrays.asList(
                    context.getResources().getStringArray(R.array.wowbox64_version_entries)));
        } else {
            itemList = new ArrayList<>(Arrays.asList(
                    context.getResources().getStringArray(R.array.box64_version_entries)));
        }

        ContentProfile.ContentType boxType = isArm64EC
                ? ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64
                : ContentProfile.ContentType.CONTENT_TYPE_BOX64;
        for (ContentProfile profile : manager.getProfiles(boxType)) {
            String entryName = ContentsManager.getEntryName(profile);
            int dash = entryName.indexOf('-');
            itemList.add(entryName.substring(dash + 1));
        }

        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, itemList));
        if (container != null) {
            AppUtils.setSpinnerSelectionFromValue(spinner, container.getBox64Version());
        } else {
            AppUtils.setSpinnerSelectionFromValue(spinner, isArm64EC ? DefaultVersion.WOWBOX64 : DefaultVersion.BOX64);
        }
    }

    public static String getScreenSize(View view) {
        Spinner sScreenSize = view.findViewById(R.id.SScreenSize);
        String value = sScreenSize.getSelectedItem().toString();
        if (value.equalsIgnoreCase("custom")) {
            String strWidth = ((EditText) view.findViewById(R.id.ETScreenWidth)).getText().toString().trim();
            String strHeight = ((EditText) view.findViewById(R.id.ETScreenHeight)).getText().toString().trim();
            if (strWidth.matches("[0-9]+") && strHeight.matches("[0-9]+")) {
                int w = Integer.parseInt(strWidth);
                int h = Integer.parseInt(strHeight);
                if ((w % 2) == 0 && (h % 2) == 0) return w + "x" + h;
            }
            return Container.DEFAULT_SCREEN_SIZE;
        }
        return StringUtils.parseIdentifier(value);
    }

    public static String getWinComponents(View view) {
        ViewGroup parent = view.findViewById(R.id.LLTabWinComponents);
        ArrayList<View> views = new ArrayList<>();
        AppUtils.findViewsWithClass(parent, Spinner.class, views);
        String[] wincomponents = new String[views.size()];
        for (int i = 0; i < views.size(); i++) {
            Spinner spinner = (Spinner) views.get(i);
            wincomponents[i] = spinner.getTag() + "=" + spinner.getSelectedItemPosition();
        }
        return String.join(",", wincomponents);
    }

    public static void createWinComponentsTab(View view, String wincomponents) {
        Context context = view.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup tabView = view.findViewById(R.id.LLTabWinComponents);
        ViewGroup directxSection = tabView.findViewById(R.id.LLWinComponentsDirectX);
        ViewGroup generalSection = tabView.findViewById(R.id.LLWinComponentsGeneral);

        for (String[] wc : new KeyValueSet(wincomponents)) {
            ViewGroup parent = wc[0].startsWith("direct") ? directxSection : generalSection;
            View itemView = inflater.inflate(R.layout.wincomponent_list_item, parent, false);
            ((TextView) itemView.findViewById(R.id.TextView)).setText(StringUtils.getString(context, wc[0]));
            Spinner spinner = itemView.findViewById(R.id.Spinner);
            spinner.setSelection(Integer.parseInt(wc[1]), false);
            spinner.setTag(wc[0]);
            spinner.setPopupBackgroundResource(R.drawable.content_dialog_background_dark);
            parent.addView(itemView);
        }
    }

    public static void loadScreenSizeSpinner(View view, String selectedValue) {
        final Spinner sScreenSize = view.findViewById(R.id.SScreenSize);
        final LinearLayout llCustomScreenSize = view.findViewById(R.id.LLCustomScreenSize);

        sScreenSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = sScreenSize.getItemAtPosition(position).toString();
                llCustomScreenSize.setVisibility(value.equalsIgnoreCase("custom") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        boolean found = AppUtils.setSpinnerSelectionFromIdentifier(sScreenSize, selectedValue);
        if (!found) {
            AppUtils.setSpinnerSelectionFromValue(sScreenSize, "custom");
            String[] parts = selectedValue.split("x");
            ((EditText) view.findViewById(R.id.ETScreenWidth)).setText(parts[0]);
            ((EditText) view.findViewById(R.id.ETScreenHeight)).setText(parts[1]);
        }
    }
}
