package pe.sulca.eventos;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;

import com.facebook.share.widget.ShareDialog;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by William_ST on 28/03/18.
 */

public class Comun {

    static final String URL_SERVIDOR = "http://cursoandroid.hol.es/notificaciones/";
    static String ID_PROYECTO = "eventos-a93da";
    static String SERVER_KEY_FCM = "AAAAMIq_IAc:APA91bH3SZgcpnwqwmZWGONo-DFPY-Ze-nHj_5FavkMO6PDoVGxqgmL3w1RmT2IBB-Jik3v-2qXeN440-vUpJqkfPDoF7RQTNtvzMTUX-jFnKndxZ5TL0lZoy3tJooK2mpWztN8HkyxG";
    String idRegistro = "";
    static FirebaseStorage storage;
    static StorageReference storageRef;
    static FirebaseAnalytics mFirebaseAnalytics;
    static FirebaseRemoteConfig mFirebaseRemoteConfig;
    static String colorFondo;
    static Boolean acercaDe;
    static ShareDialog shareDialog;

    static void mostrarDialogo(final Context context, final String mensaje) {
        Intent intent = new Intent(context, Dialogo.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("mensaje", mensaje);
        context.startActivity(intent);
    }

    static void mostrarDialogEvent(final Context context, final String idEvent, final String message) {
        Intent intent = new Intent(context, EventNotificationDialog.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("idEvent", idEvent);
        intent.putExtra("message", message);
        context.startActivity(intent);
    }

    public static class registrarDispositivoEnServidorWebTask extends AsyncTask<Void, Void, String> {

        String response = "error";
        Context contexto;
        String idRegistroTarea = "";

        public void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... arg0) {
            try {
                Uri.Builder constructorParametros = new Uri.Builder().appendQueryParameter("iddevice", idRegistroTarea).appendQueryParameter("idapp", ID_PROYECTO);
                String parametros = constructorParametros.build().getEncodedQuery();
                String url = URL_SERVIDOR + "registrar.php";
                URL direccion = new URL(url);
                HttpURLConnection conexion = (HttpURLConnection)
                        direccion.openConnection();
                conexion.setRequestMethod("POST");
                conexion.setRequestProperty("Accept-Language", "UTF-8");
                conexion.setDoOutput(true);
                OutputStreamWriter outputStreamWriter = new
                        OutputStreamWriter(conexion.getOutputStream());
                outputStreamWriter.write(parametros.toString());
                outputStreamWriter.flush();
                int respuesta = conexion.getResponseCode();
                if (respuesta == 200) {
                    response = "ok";
                } else {
                    response = "error";
                }
            } catch (IOException e) {
                response = "error";
            }
            return response;
        }

        public void onPostExecute(String res) {
            Log.d(Comun.class.getCanonicalName(), "res: " + res);
        }
    }

    public static void guardarIdRegistro(Context context, String idRegistro) {
        registrarDispositivoEnServidorWebTask tarea =
                new registrarDispositivoEnServidorWebTask();
        tarea.contexto = context;
        tarea.idRegistroTarea = idRegistro;
        tarea.execute();
    }

    public static void eliminarIdRegistro(Context context) {
        desregistrarDispositivoEnServidorWebTask tarea = new desregistrarDispositivoEnServidorWebTask();
        tarea.contexto = context;
        tarea.idRegistroTarea = FirebaseInstanceId.getInstance().getToken();
        tarea.execute();
    }

    public static class desregistrarDispositivoEnServidorWebTask
            extends AsyncTask<Void, Void, String> {
        String response = "error";
        Context contexto;
        String idRegistroTarea;

        public void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... arg0) {
            try {
                Uri.Builder constructorParametros = new Uri.Builder().appendQueryParameter("iddevice", idRegistroTarea).appendQueryParameter("idapp", ID_PROYECTO);
                String parametros =
                        constructorParametros.build().getEncodedQuery();
                String url = URL_SERVIDOR + "desregistrar.php";
                URL direccion = new URL(url);
                HttpURLConnection conexion = (HttpURLConnection)
                        direccion.openConnection();
                conexion.setRequestMethod("POST");
                conexion.setRequestProperty("Accept-Language", "UTF-8");
                conexion.setDoOutput(true);
                OutputStreamWriter outputStreamWriter = new
                        OutputStreamWriter(conexion.getOutputStream());
                outputStreamWriter.write(parametros.toString());
                outputStreamWriter.flush();
                int respuesta = conexion.getResponseCode();
                if (respuesta == 200) {
                    response = "ok";
                } else {
                    response = "error";
                }
            } catch (IOException e) {
                response = "error";
            }
            return response;
        }

        public void onPostExecute(String res) {
            Log.d(Comun.class.getCanonicalName(), "res: " + res);
        }
    }

    public static StorageReference getStorageReference() {
        return storageRef;
    }
}
