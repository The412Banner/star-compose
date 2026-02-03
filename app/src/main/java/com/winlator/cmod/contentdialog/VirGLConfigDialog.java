package com.winlator.contentdialog;

/* Decompiled from Winlator 10 Final
 * https://github.com/brunodev85/winlator/releases/tag/v10.0.0
 */

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.winlator.R;
import com.winlator.core.AppUtils;
import com.winlator.core.EnvVars;
import com.winlator.core.KeyValueSet;

import java.util.ArrayList;

public class VirGLConfigDialog extends ContentDialog {

  private final Context context;

  public VirGLConfigDialog(View anchor) {
    super(anchor.getContext(), R.layout.virgl_config_dialog);
    context = anchor.getContext();
    setIcon(R.drawable.icon_settings);
    setTitle("VirGL " + context.getString(R.string.configuration));

    final Spinner SOpenglVersion = findViewById(R.id.SOpenglVersion);
    final CheckBox CBdisableVertexArrayBGRA = findViewById(R.id.CBdisableVertexArrayBGRA);
    final CheckBox CBdisableKHRdebug = findViewById(R.id.CBdisableKHRdebug);
    final CheckBox CBdisableTextureSRGBdecode = findViewById(R.id.CBdisableTextureSRGBdecode);
    final CheckBox CBuseOldVirGL = findViewById(R.id.CBuseOldVirGL);

    KeyValueSet config = new KeyValueSet(anchor.getTag().toString());
    AppUtils.setSpinnerSelectionFromIdentifier(SOpenglVersion, config.get("glVersion", "3.1"));
    CBdisableVertexArrayBGRA.setChecked(config.getBoolean("disableVertexArrayBGRA", true));
    CBdisableKHRdebug.setChecked(config.getBoolean("disableKHRdebug", false));
    CBdisableTextureSRGBdecode.setChecked(config.getBoolean("disableTextureSRGBdecode", true));
    CBuseOldVirGL.setChecked(config.getBoolean("useOldVirGL", false));

    setOnConfirmCallback(() -> {
      config.put("glVersion", SOpenglVersion.getSelectedItem().toString());
      config.put("disableVertexArrayBGRA", CBdisableVertexArrayBGRA.isChecked());
      config.put("disableKHRdebug", CBdisableKHRdebug.isChecked());
      config.put("disableTextureSRGBdecode", CBdisableTextureSRGBdecode.isChecked());
      config.put("useOldVirGL", CBuseOldVirGL.isChecked());
      anchor.setTag(config.toString());
    });
  }
  
  public static void setEnvVars(KeyValueSet paramKeyValueSet, EnvVars paramEnvVars) {
    ArrayList<String> arrayList = new ArrayList();
    if (paramKeyValueSet.getBoolean("disableKHRdebug", true))
      arrayList.add("GL_KHR_debug");
    if (paramKeyValueSet.getBoolean("disableVertexArrayBGRA", true))
      arrayList.add("GL_EXT_vertex_array_bgra");
    if (paramKeyValueSet.getBoolean("disableTextureSRGBdecode", true))
      arrayList.add("GL_EXT_texture_sRGB_decode");

    String str = "";
    for (String str1 : arrayList) {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(str);
      if (!str.isEmpty()) {
        str = " ";
      } else {
        str = "";
      } 
      stringBuilder.append(str);
      stringBuilder.append("-");
      stringBuilder.append(str1);
      str = stringBuilder.toString();
    } 
    if (!str.isEmpty())
      paramEnvVars.put("MESA_EXTENSION_OVERRIDE", str);

    float mesaGLVersion = Float.parseFloat(paramKeyValueSet.get("glVersion", "3.1"));

    if (paramKeyValueSet.getBoolean("useOldVirGL", false)) {
      if (mesaGLVersion <= 2.1)
        paramEnvVars.put("MESA_GLSL_VERSION_OVERRIDE", "120");
      else if (mesaGLVersion > 2.1 && mesaGLVersion <= 3.3)
        paramEnvVars.put("MESA_GLSL_VERSION_OVERRIDE", "330");
    }

    paramEnvVars.put("MESA_GL_VERSION_OVERRIDE", mesaGLVersion);

  }
}
