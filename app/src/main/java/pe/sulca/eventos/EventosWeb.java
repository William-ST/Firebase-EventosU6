package pe.sulca.eventos;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import static pe.sulca.eventos.Comun.colorFondo;

/**
 * Created by William_ST on 31/03/18.
 */

public class EventosWeb extends AppCompatActivity {

    private final String TAG = EventosWeb.class.getCanonicalName();

    WebView navegador;
    private String evento;
    private double discount = 0;
    //private ProgressBar barraProgreso;
    ProgressDialog dialogo;
    final InterfazComunicacion miInterfazJava = new InterfazComunicacion(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eventos_web);

        navegador = (WebView) findViewById(R.id.webkit);

        if (comprobarConectividad()) {
            navegador.loadUrl("https://eventos-a93da.firebaseapp.com/index.html");
        } else {
            navegador.loadUrl("file:///android_asset/index.html");
        }

        navegador.getSettings().setJavaScriptEnabled(true);
        navegador.getSettings().setBuiltInZoomControls(false);

        navegador.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(EventosWeb.this).setTitle("Mensaje")
                        .setMessage(message).setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                }).setCancelable(false).create().show();
                return true;
            }
        });

        navegador.addJavascriptInterface(miInterfazJava, "jsInterfazNativa");

        Bundle extras = getIntent().getExtras();
        evento = extras.getString("evento");

        if (evento == null) {
            android.net.Uri url = getIntent().getData();
            Log.d(TAG, url.toString());
            Log.d(TAG, url.getQuery().toString());
            Log.d(TAG, url.getQueryParameterNames().toString());
            evento = url.getQueryParameter("evento");
            discount = Double.parseDouble(url.getQueryParameter("desc"));
            Log.d(TAG, "discount: "+discount);
        }

        navegador.setWebViewClient(new MyWebClient());
    }

    public class MyWebClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            dialogo = new ProgressDialog(EventosWeb.this);
            dialogo.setMessage("Cargando...");
            dialogo.setCancelable(true);
            dialogo.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            dialogo.dismiss();
            navegador.loadUrl("javascript:colorFondo(\""+colorFondo+"\")");
            Log.d(TAG, "call muestraEvento: "+discount);
            navegador.loadUrl("javascript:muestraEvento(\""+evento+"\", "+discount+");");
        }


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (navegador.canGoBack()) {
            navegador.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private boolean comprobarConectividad() {
        // no es necesario valida permisos
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if ((info == null || !info.isConnected() || !info.isAvailable())) {
            Toast.makeText(EventosWeb.this, "Oops! No tienes conexión a internet", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public class InterfazComunicacion {
        Context mContext;

        InterfazComunicacion(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void volver() {
            Log.d(TAG, "calling volver");
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.event_web_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_open:
                navegador.loadUrl("https://us-central1-eventos-a93da.cloudfunctions.net/mostrarEventosHtml?evento="+evento);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
