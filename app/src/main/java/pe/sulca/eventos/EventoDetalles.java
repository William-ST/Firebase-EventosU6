package pe.sulca.eventos;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pe.sulca.eventos.Comun.acercaDe;
import static pe.sulca.eventos.Comun.getStorageReference;
import static pe.sulca.eventos.Comun.mFirebaseAnalytics;
import static pe.sulca.eventos.Comun.mostrarDialogo;
import static pe.sulca.eventos.Comun.shareDialog;
import static pe.sulca.eventos.Comun.storageRef;

import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.services.StatusesService;

import retrofit2.Call;

/**
 * Created by William_ST on 29/03/18.
 */

public class EventoDetalles extends AppCompatActivity {

    private String TAG = EventoDetalles.class.getCanonicalName();
    private ProgressDialog progresoSubida;
    Boolean subiendoDatos = false;

    TextView txtEvento, txtFecha, txtCiudad;
    ImageView imgImagen;
    String evento;
    CollectionReference registros;

    EditText etComment;
    Button btnSendCommentFacebook, btnSendCommentTwitter;

    final int SOLICITUD_SUBIR_PUTDATA = 0;
    final int SOLICITUD_SUBIR_PUTSTREAM = 1;
    final int SOLICITUD_SUBIR_PUTFILE = 2;
    final int SOLICITUD_SELECCION_STREAM = 100;
    final int SOLICITUD_SELECCION_PUTFILE = 101;
    final int SOLICITUD_FOTOGRAFIAS_DRIVE = 102;

    static UploadTask uploadTask = null;
    StorageReference imagenRef;
    EventFirestore eventFirestore;

    Trace mTrace;

    @Override
    @AddTrace(name = "onCreateTrace", enabled = true)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.evento_detalles);

        txtEvento = findViewById(R.id.txtEvento);
        txtFecha = findViewById(R.id.txtFecha);
        txtCiudad = findViewById(R.id.txtCiudad);
        imgImagen = findViewById(R.id.imgImagen);
        etComment = findViewById(R.id.et_comment);
        btnSendCommentFacebook = findViewById(R.id.btn_send_comment_facebook);
        btnSendCommentTwitter = findViewById(R.id.btn_send_comment_twitter);

        Bundle extras = getIntent().getExtras();
        evento = extras.getString("evento");

        if (evento == null) {
            android.net.Uri url = getIntent().getData();
            evento = url.getQueryParameter("evento");
        }

        if (!TextUtils.isEmpty(evento)) {
            registros = FirebaseFirestore.getInstance().collection("eventos");
            registros.document(evento).get().addOnCompleteListener(
                    new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                eventFirestore = new EventFirestore();
                                eventFirestore.setCiudad(task.getResult().get("ciudad").toString());
                                eventFirestore.setEvento(task.getResult().get("evento").toString());
                                eventFirestore.setFecha(task.getResult().get("fecha").toString());
                                eventFirestore.setImagen(task.getResult().get("imagen").toString());

                                txtEvento.setText(eventFirestore.getEvento());
                                txtCiudad.setText(eventFirestore.getCiudad());
                                txtFecha.setText(eventFirestore.getFecha());
                                new DownloadImageTask((ImageView) imgImagen).execute(eventFirestore.getImagen());
                            }
                        }
                    });

            mFirebaseAnalytics.setUserProperty("evento_detalle", evento);
        }

        /*
        if (evento == null) {
            Log.d(TAG, "evento empty!");
            evento = "";
        } else {
            Log.d(TAG, "evento: "+evento);
        }
        */

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        mTrace = FirebasePerformance.getInstance().newTrace("trace_EventoDetalles");
        //mTrace.start();
        btnSendCommentFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AccessToken.getCurrentAccessToken() != null) {
                    final String comment = etComment.getText().toString();
                    if (!TextUtils.isEmpty(comment)) {
                        Bundle params = new Bundle();
                        params.putString("message", comment);
                    /* make the API call */
                        new GraphRequest(
                                AccessToken.getCurrentAccessToken(),
                                "/me/comments",
                                params,
                                HttpMethod.POST,
                                new GraphRequest.Callback() {
                                    public void onCompleted(GraphResponse response) {
                                    /* handle the result */
                                        if (response.getError() == null) {
                                            etComment.setText("");
                                            Toast.makeText(getApplicationContext(), "¡Comentario realizado!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "¡Ha ocurrido un error!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                        ).executeAsync();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "AccessToken.getCurrentAccessToken() == null", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSendCommentTwitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AuthenticateUtil.isLoginTwitter()) {
                    final String comment = etComment.getText().toString();
                    if (!TextUtils.isEmpty(comment)) {
                        // TODO : getApiClient reivew!
                        StatusesService statusesService = AuthenticateUtil.getApiClientTwitter().getStatusesService();
                        Call<Tweet> call = statusesService.update(comment, null,
                                null, null, null, null,
                                null, null, null);
                        call.enqueue(new Callback<Tweet>() {
                            @Override
                            public void success(Result<Tweet> result) {
                                Toast.makeText(getApplicationContext(), "Tweet publicado: " + result.response.message(), Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void failure(TwitterException e) {
                                Toast.makeText(getApplicationContext(), "No se pudo publicar el tweet: " +e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        });

        if (AccessToken.getCurrentAccessToken() != null) {
            findViewById(R.id.ll_public).setVisibility(View.VISIBLE);
            btnSendCommentFacebook.setVisibility(View.VISIBLE);
            btnSendCommentTwitter.setVisibility(View.GONE);
        } else if (AuthenticateUtil.isLoginTwitter()) {
            findViewById(R.id.ll_public).setVisibility(View.VISIBLE);
            btnSendCommentFacebook.setVisibility(View.GONE);
            btnSendCommentTwitter.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTrace.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTrace.stop();
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mImagen = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mImagen = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mImagen;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        if (acercaDe != null && !acercaDe) {
            menu.removeItem(R.id.action_acercaDe);
        }

        if (!ShareDialog.canShow(ShareLinkContent.class)) {
            menu.removeItem(R.id.action_recommend_fb);
        }

        if (AccessToken.getCurrentAccessToken() == null) {
            menu.removeItem(R.id.action_post_image_fb);
            if (!ShareDialog.canShow(SharePhotoContent.class)) {
                menu.removeItem(R.id.action_post_image_share_dialog);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View vista = (View) findViewById(android.R.id.content);
        Bundle bundle = new Bundle();
        int id = item.getItemId();
        switch (id) {
            case R.id.action_putData:
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "subir_imagen");
                mFirebaseAnalytics.logEvent("menus", bundle);
                subirAFirebaseStorage(SOLICITUD_SUBIR_PUTDATA, null);
                break;
            case R.id.action_streamData:
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "subir_stream");
                mFirebaseAnalytics.logEvent("menus", bundle);
                seleccionarFotografiaDispositivo(vista, SOLICITUD_SELECCION_STREAM);
                break;
            case R.id.action_putFile:
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "subir_fichero");
                mFirebaseAnalytics.logEvent("menus", bundle);
                seleccionarFotografiaDispositivo(vista, SOLICITUD_SELECCION_PUTFILE);
                break;
            case R.id.action_getFile:
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "descargar_fichero");
                mFirebaseAnalytics.logEvent("menus", bundle);
                descargarDeFirebaseStorage(evento);
                break;
            case R.id.action_fotografiasDrive:
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "fotografias_drive");
                mFirebaseAnalytics.logEvent("menus", bundle);
                Intent intent = new Intent(getBaseContext(), FotografiasDrive.class);
                intent.putExtra("evento", evento);
                startActivity(intent);
                break;
            case R.id.action_acercaDe:
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "acerca_de");
                mFirebaseAnalytics.logEvent("menus", bundle);
                Intent intentWeb = new Intent(getBaseContext(), EventosWeb.class);
                intentWeb.putExtra("evento", evento);
                startActivity(intentWeb);
                break;
            case R.id.action_delete:
                storageRef.child(evento).delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                deleteImageDatabase(evento);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EventoDetalles.this, "Ha ocurrido un error", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case R.id.action_post_image_fb:
                if (AccessToken.getCurrentAccessToken() != null) {
                    Bundle params = new Bundle();
                    imgImagen.setDrawingCacheEnabled(true);
                    imgImagen.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    imgImagen.layout(0, 0, imgImagen.getMeasuredWidth(), imgImagen.getMeasuredHeight());
                    imgImagen.buildDrawingCache();
                    Bitmap bitmap = imgImagen.getDrawingCache();
                    //imgImagen.setDrawingCacheEnabled(false);

                    if (bitmap == null) Log.e(TAG, "bitmap == null");

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] data = baos.toByteArray();

                    params.putByteArray("multipart/form-data", data);

                    params.putString("caption", "Nueva foto - " + evento);
                    /* make the API call */
                    new GraphRequest(
                            AccessToken.getCurrentAccessToken(),
                            "/me/photos",
                            params,
                            HttpMethod.POST,
                            new GraphRequest.Callback() {
                                public void onCompleted(GraphResponse response) {
                                    /* handle the result */
                                    Log.e("responseImagedata---", response.toString());
                                    if (response.getError() == null) {
                                        Toast.makeText(getApplicationContext(), "¡Foto publicada en facebook!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "¡Ha ocurrido un error!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                    ).executeAsync();
                }

                break;
            case R.id.action_recommend_fb:
                ShareLinkContent content = new ShareLinkContent.Builder()
                        .setContentUrl(Uri.parse("https://us-central1-eventos-a93da.cloudfunctions.net/mostrarEventosHtml?evento=" + evento))
                        .setContentDescription(evento + ", ¡vamos de viaje!").build();
                shareDialog.show(content);
                break;
            case R.id.action_post_image_share_dialog:
                imgImagen.setDrawingCacheEnabled(true);
                imgImagen.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                imgImagen.layout(0, 0, imgImagen.getMeasuredWidth(), imgImagen.getMeasuredHeight());
                imgImagen.buildDrawingCache();
                SharePhoto photo = new SharePhoto.Builder()
                        .setBitmap(imgImagen.getDrawingCache()).build();
                //imgImagen.setDrawingCacheEnabled(false);
                SharePhotoContent contentPhoto = new SharePhotoContent.Builder()
                        .addPhoto(photo).build();
                shareDialog.show(contentPhoto);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteImageDatabase(String event) {
        eventFirestore.setImagen("");
        registros.document(event).set(eventFirestore);
        Toast.makeText(EventoDetalles.this, "Imagen eliminada", Toast.LENGTH_SHORT).show();
    }

    public void seleccionarFotografiaDispositivo(View v, Integer solicitud) {
        Intent seleccionFotografiaIntent = new Intent(Intent.ACTION_PICK);
        seleccionFotografiaIntent.setType("image/*");
        startActivityForResult(seleccionFotografiaIntent, solicitud);
    }

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent data) {
        Uri ficheroSeleccionado;
        Cursor cursor;
        String rutaImagen;
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case SOLICITUD_SELECCION_STREAM:
                    ficheroSeleccionado = data.getData();
                    String[] proyeccionStream = {MediaStore.Images.Media.DATA};
                    cursor = getContentResolver().query(ficheroSeleccionado,
                            proyeccionStream, null, null, null);
                    rutaImagen = cursor.getString(cursor.getColumnIndex(proyeccionStream[0]));
                    cursor.close();
                    subirAFirebaseStorage(SOLICITUD_SUBIR_PUTSTREAM, rutaImagen);
                    break;
                case SOLICITUD_SELECCION_PUTFILE:
                    ficheroSeleccionado = data.getData();
                    String[] proyeccionFile = {MediaStore.Images.Media.DATA};
                    cursor = getContentResolver().query(ficheroSeleccionado, proyeccionFile, null, null, null);
                    cursor.moveToFirst();
                    rutaImagen = cursor.getString(
                            cursor.getColumnIndex(proyeccionFile[0]));
                    cursor.close();
                    subirAFirebaseStorage(SOLICITUD_SUBIR_PUTFILE, rutaImagen);
                    break;
            }
        }
    }

    public void subirAFirebaseStorage(Integer opcion, String ficheroDispositivo) {
        final ProgressDialog progresoSubida = new ProgressDialog(EventoDetalles.this);
        progresoSubida.setTitle("Subiendo...");
        progresoSubida.setMessage("Espere...");
        progresoSubida.setCancelable(true);
        progresoSubida.setCanceledOnTouchOutside(false);
        progresoSubida.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                uploadTask.cancel();
            }
        });

        String fichero = evento;
        imagenRef = getStorageReference().child(fichero);
        try {
            switch (opcion) {
                case SOLICITUD_SUBIR_PUTDATA:
                    imgImagen.setDrawingCacheEnabled(true);
                    imgImagen.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    imgImagen.layout(0, 0, imgImagen.getMeasuredWidth(), imgImagen.getMeasuredHeight());
                    imgImagen.buildDrawingCache();
                    Bitmap bitmap = imgImagen.getDrawingCache();
                    //imgImagen.setDrawingCacheEnabled(false);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] data = baos.toByteArray();
                    uploadTask = imagenRef.putBytes(data);
                    break;
                case SOLICITUD_SUBIR_PUTSTREAM:
                    InputStream stream = new FileInputStream(new File(ficheroDispositivo));
                    uploadTask = imagenRef.putStream(stream);
                    break;
                case SOLICITUD_SUBIR_PUTFILE:
                    Uri file = Uri.fromFile(new File(ficheroDispositivo));
                    uploadTask = imagenRef.putFile(file);
                    break;
            }

            uploadTask
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            subiendoDatos = false;
                            mostrarDialogo(getApplicationContext(), "Ha ocurrido un error al subir la imagen o el usuario ha cancelado la subida.");
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Map<String, Object> datos = new HashMap<>();
                            datos.put("imagen", taskSnapshot.getDownloadUrl().toString());
                            FirebaseFirestore.getInstance().collection("eventos")
                                    .document(evento).set(datos, SetOptions.merge());
                            new DownloadImageTask(imgImagen).execute(taskSnapshot.getDownloadUrl().toString());
                            progresoSubida.dismiss();

                            subiendoDatos = false;
                            mostrarDialogo(getApplicationContext(), "Imagen subida correctamente.");
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                            if (!subiendoDatos) {
                                progresoSubida.show();
                                subiendoDatos = true;
                            } else {
                                if (taskSnapshot.getTotalByteCount() > 0)
                                    progresoSubida.setMessage("Espere... " + String.valueOf(100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()) + "%");
                            }

                        }
                    })
                    .addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                            subiendoDatos = false;
                            mostrarDialogo(getApplicationContext(), "La subida ha sido pausada.");
                        }
                    });

        } catch (IOException e) {
            mostrarDialogo(getApplicationContext(), e.toString());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (imagenRef != null) {
            outState.putString("EXTRA_STORAGE_REFERENCE_KEY", imagenRef.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        final String stringRef = savedInstanceState.getString("EXTRA_STORAGE_REFERENCE_KEY");

        if (stringRef == null) {
            return;
        }

        imagenRef = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);
        List<UploadTask> tasks = imagenRef.getActiveUploadTasks();
        for (UploadTask task : tasks) {
            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    upload_error(exception);
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    upload_exito(taskSnapshot);
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    upload_progreso(taskSnapshot);
                }
            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                    upload_pausa(taskSnapshot);
                }
            });
        }
    }

    private void upload_error(Exception exception) {
        subiendoDatos = false;
        mostrarDialogo(getApplicationContext(),
                "Ha ocurrido un error al subir la imagen o el usuario ha cancelado la subida.");
    }

    private void upload_exito(UploadTask.TaskSnapshot taskSnapshot) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("imagen", taskSnapshot.getDownloadUrl().toString());
        FirebaseFirestore.getInstance().collection("eventos").document(evento).set(datos);
        new DownloadImageTask((ImageView) imgImagen)
                .execute(taskSnapshot.getDownloadUrl().toString());
        progresoSubida.dismiss();
        subiendoDatos = false;
        mostrarDialogo(getApplicationContext(), "Imagen subida correctamente.");
    }

    private void upload_progreso(UploadTask.TaskSnapshot taskSnapshot) {
        if (!subiendoDatos) {
            progresoSubida = new ProgressDialog(EventoDetalles.this);
            progresoSubida.setTitle("Subiendo...");
            progresoSubida.setMessage("Espere...");
            progresoSubida.setCancelable(true);
            progresoSubida.setCanceledOnTouchOutside(false);
            progresoSubida.setButton(DialogInterface.BUTTON_NEGATIVE,
                    "Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            uploadTask.cancel();
                        }
                    });
            progresoSubida.show();
            subiendoDatos = true;
        } else {
            if (taskSnapshot.getTotalByteCount() > 0)
                progresoSubida.setMessage("Espere... " + String.valueOf(100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()) + "%");
        }
    }

    private void upload_pausa(UploadTask.TaskSnapshot taskSnapshot) {
        subiendoDatos = false;
        mostrarDialogo(getApplicationContext(), "La subida ha sido pausada.");
    }

    public void descargarDeFirebaseStorage(String fichero) {
        StorageReference referenciaFichero =
                getStorageReference().child(fichero);
        File rootPath = new File(Environment.getExternalStorageDirectory(), "Eventos");
        if (!rootPath.exists()) {
            rootPath.mkdirs();
        }
        final File localFile = new File(rootPath, evento + ".jpg");
        referenciaFichero.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                mostrarDialogo(getApplicationContext(), "Fichero descargado con éxito: " + localFile.toString());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                mostrarDialogo(getApplicationContext(),
                        "Error al descargar el fichero.");
            }
        });
    }


}
