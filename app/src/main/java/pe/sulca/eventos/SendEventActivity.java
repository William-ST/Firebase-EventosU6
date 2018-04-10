package pe.sulca.eventos;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import static pe.sulca.eventos.Comun.ID_PROYECTO;
import static pe.sulca.eventos.Comun.SERVER_KEY_FCM;
import static pe.sulca.eventos.Comun.URL_SERVIDOR;

public class SendEventActivity extends AppCompatActivity {

    private String TAG = SendEventActivity.class.getCanonicalName();

    EditText etMessage;
    Button btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_event);

        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String message = etMessage.getText().toString();
                if (!TextUtils.isEmpty(message)) {
                    new registrarDispositivoEnServidorWebTask().execute(message);
                }
            }
        });

    }

    public class registrarDispositivoEnServidorWebTask extends AsyncTask<String, Void, String> {

        ProgressDialog dialogo;
        String response = "error";

        public void onPreExecute() {
            super.onPreExecute();
            dialogo = new ProgressDialog(SendEventActivity.this);
            dialogo.setMessage("Cargando...");
            dialogo.setCancelable(false);
            dialogo.show();
        }

        @Override
        protected String doInBackground(String... arg0) {
            try {
                Uri.Builder constructorParametros = new Uri.Builder()
                        .appendQueryParameter("apiKey", SERVER_KEY_FCM)
                        .appendQueryParameter("idapp", ID_PROYECTO)
                        .appendQueryParameter("mensaje", arg0[0]);

                String parametros = constructorParametros.build().getEncodedQuery();
                Log.d(TAG, "parametros: "+parametros);
                String url = URL_SERVIDOR + "notificar.php";
                Log.d(TAG, "url: "+URL_SERVIDOR + "notificar.php");
                URL direccion = new URL(url);
                HttpURLConnection conexion = (HttpURLConnection)
                        direccion.openConnection();
                conexion.setRequestMethod("POST");
                conexion.setRequestProperty("Accept-Language", "UTF-8");
                conexion.setDoOutput(true);
                OutputStreamWriter outputStreamWriter = new
                        OutputStreamWriter(conexion.getOutputStream());
                outputStreamWriter.write(URLDecoder.decode(parametros, "UTF-8"));
                outputStreamWriter.flush();
                int respuesta = conexion.getResponseCode();
                Log.d(TAG, conexion.getResponseMessage());
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
            dialogo.dismiss();
            Log.d(TAG, "res: " + res);
        }
    }

}
