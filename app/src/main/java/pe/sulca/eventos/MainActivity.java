package pe.sulca.eventos;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import io.fabric.sdk.android.Fabric;

import static pe.sulca.eventos.Comun.mFirebaseAnalytics;
import static pe.sulca.eventos.Comun.mostrarDialogo;
import static pe.sulca.eventos.Comun.storage;
import static pe.sulca.eventos.Comun.storageRef;
import static pe.sulca.eventos.EventosFirestore.EVENTOS;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getCanonicalName();
    private AdaptadorEventos adaptador;
    private CollectionReference eventCollectionReference;

    java.io.File ficheroFoto;

    private static Uri uriFichero;
    static final int REQUEST_PICK_ACCOUNT = 10, REQUEST_TAKE_PHOTO = 11, REQUEST_AUTHORIZATION = 12;
    private Boolean noAutoriza = false;
    static String nombreCuenta = null;
    static GoogleAccountCredential credencial = null;
    static Drive servicio = null;
    private String idShareFolder = "1_BaZuoiV8QQMbJohr53WiGZky_Ym5XZA";
    private Handler handler;
    private ProgressDialog dialogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());

        eventCollectionReference = FirebaseFirestore.getInstance().collection(EVENTOS);

        Query query = eventCollectionReference.limit(50);
        FirestoreRecyclerOptions<Evento> opciones = new FirestoreRecyclerOptions
                .Builder<Evento>().setQuery(query, Evento.class).build();
        adaptador = new AdaptadorEventos(opciones);
        final RecyclerView recyclerView = findViewById(R.id.reciclerViewEventos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adaptador);


        final SharedPreferences preferencias = getApplicationContext().getSharedPreferences("Temas", Context.MODE_PRIVATE);
        if (preferencias.getBoolean("Inicializado", false) == false) {
            final SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                    "Temas", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("Inicializado", true);
            editor.commit();
            FirebaseMessaging.getInstance().subscribeToTopic("Todos");
        }

        adaptador.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = recyclerView.getChildAdapterPosition(view);
                Evento currentItem = adaptador.getItem(position);
                String idEvento = adaptador.getSnapshots().getSnapshot(position).getId();
                Context context = getAppContext();
                Intent intent = new Intent(context, EventoDetalles.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("evento", idEvento);
                context.startActivity(intent);
            }
        });

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReferenceFromUrl("gs://eventos-a93da.appspot.com");
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        String[] PERMISOS = {
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.GET_ACCOUNTS,
                android.Manifest.permission.CAMERA
        };
        ActivityCompat.requestPermissions(this, PERMISOS, 1);

        credencial = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE));
        credencial.setSelectedAccountName("sulca.03.1995@gmail.com");//para no solicitar validación
        servicio = obtenerServicioDrive(credencial);

        Log.d(TAG, "token: " + FirebaseInstanceId.getInstance().getToken());
        Fabric.with(this, new Crashlytics());

        Crashlytics.log(Log.VERBOSE, "screen", "MainActivity");
        Crashlytics.setString("LOGIN_STATUS", "logged_out");

        /*
        only test
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String[] items = null;
                Log.d(TAG, items[1]);//crash
            }
        }, 20000);
        */
    }

    private static MainActivity current;

    public static MainActivity getCurrentContext() {
        return current;
    }

    @Override
    public void onStart() {
        super.onStart();
        current = this;
        adaptador.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adaptador.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.keySet().size() > 4) {
            String evento = "";
            evento = "Evento: " + extras.getString("evento") + "\n";
            evento = evento + "Día: " + extras.getString("dia") + "\n";
            evento = evento + "Ciudad: " + extras.getString("ciudad") + "\n";
            evento = evento + "Comentario: " + extras.getString("comentario");
            mostrarDialogo(getApplicationContext(), evento);
            for (String key : extras.keySet()) {
                getIntent().removeExtra(key);
            }
            extras = null;
        }
        /*
        Bundle extras = getIntent().getExtras();
        if (getIntent().hasExtra("body")) {
            mostrarDialogo(this, extras.getString("body"));
            extras.remove("body");
        }
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Crashlytics.log(Log.VERBOSE, "onOptionsItemSelected", "onOptionsItemSelected");
        int id = item.getItemId();
        if (id == R.id.action_temas) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "suscripciones");

            mFirebaseAnalytics.logEvent("menus", bundle);

            Intent intent = new Intent(getBaseContext(), Temas.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_send_event) {
            startActivity(new Intent(getBaseContext(), SendEventActivity.class));
        } else if (id == R.id.action_share_photo) {
            callTakePhoto();
        } else if (id == R.id.action_error) {
            Crashlytics.log(Log.VERBOSE, "onOptionsItemSelected", "action_error");
            Crashlytics.getInstance().crash();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static Context getAppContext() {
        return MainActivity.getCurrentContext();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (!(grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(MainActivity.this, "Has denegado algún permiso de la aplicación.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case 2:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(MainActivity.this, "Permiso denegado para acceder a la camara", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    private Drive obtenerServicioDrive(GoogleAccountCredential credencial) {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credencial).build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    guardarFicheroEnDrive();
                }
                break;
        }
    }

    private java.io.File crearFicheroImagen() throws IOException {
        String tiempo = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String nombreFichero = "JPEG_" + tiempo + "_";
        java.io.File dirAlmacenaje = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        java.io.File ficheroImagen = java.io.File.createTempFile(nombreFichero, ".jpg", dirAlmacenaje);
        return ficheroImagen;
    }

    private void callTakePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                ficheroFoto = crearFicheroImagen();
                if (ficheroFoto != null) {
                    Uri fichero = FileProvider.getUriForFile(
                            MainActivity.this,
                            BuildConfig.APPLICATION_ID + ".provider",
                            ficheroFoto);
                    uriFichero = Uri.parse("content://" + ficheroFoto.getAbsolutePath());
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fichero);
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }
            } catch (IOException ex) {
                return;
            }
        }
    }

    private void mostrarCarga(final Context context, final String mensaje) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                dialogo = new ProgressDialog(context);
                dialogo.setMessage(mensaje);
                dialogo.setCancelable(false);
                dialogo.show();
            }
        });
    }

    private void mostrarMensaje(final Context context, final String mensaje) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void ocultarCarga(final Context context) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                dialogo.dismiss();
            }
        });
    }

    private void guardarFicheroEnDrive() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mostrarCarga(MainActivity.this, "Subiendo imagen carpeta compartida...");
                    java.io.File ficheroJava = new java.io.File(uriFichero.getPath());
                    FileContent contenido = new FileContent("image/jpeg", ficheroJava);
                    File ficheroDrive = new File();
                    ficheroDrive.setName(ficheroJava.getName());
                    ficheroDrive.setMimeType("image/jpeg");
                    ficheroDrive.setParents(Collections.singletonList(idShareFolder));

                    File ficheroSubido = servicio.files().create(ficheroDrive, contenido).setFields("id").execute();
                    if (ficheroSubido.getId() != null) {
                        mostrarMensaje(MainActivity.this, "¡Foto subida!");
                    }
                } catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                } catch (final IOException e) {
                    mostrarMensaje(MainActivity.this, "Error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    ocultarCarga(MainActivity.this);
                }
            }
        });
        t.start();
    }

}
