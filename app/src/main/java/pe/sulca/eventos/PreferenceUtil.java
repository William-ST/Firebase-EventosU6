package pe.sulca.eventos;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by William_ST on 14/04/18.
 */

public class PreferenceUtil {

    public final static String ARG_FIRST_TIME = "firstTime";
    public final static int RC_FIRST_TIME = 0, RC_YES = 1, RC_NOT = 2;

    public static int getReportCrashesStatus(Context context) {
        SharedPreferences preferencias = context.getSharedPreferences("user", Context.MODE_PRIVATE);
        return preferencias.getInt(ARG_FIRST_TIME, RC_FIRST_TIME);
    }

    public static void setReportCrashesStatus(Context context, int status) {
        SharedPreferences preferencias = context.getSharedPreferences("user", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferencias.edit();
        editor.putInt(ARG_FIRST_TIME, status);
        editor.apply();
    }

}
