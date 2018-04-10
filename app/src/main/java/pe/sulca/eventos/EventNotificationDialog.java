package pe.sulca.eventos;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

/**
 * Created by William_ST on 01/04/18.
 */

public class EventNotificationDialog extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (getIntent().hasExtra("idEvent") && getIntent().hasExtra("message")) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Recomendado");
            alertDialog.setMessage(getIntent().getStringExtra("message"));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.event_show_more),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            Intent intent = new Intent(getApplicationContext(), EventoDetalles.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("evento", getIntent().getStringExtra("idEvent"));
                            startActivity(intent);

                            finish();
                        }
                    });
            alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    finish();
                }
            });
            alertDialog.show();
            extras.remove("event");
        }
    }
}