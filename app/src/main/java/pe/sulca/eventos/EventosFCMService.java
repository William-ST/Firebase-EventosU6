package pe.sulca.eventos;

import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import static pe.sulca.eventos.Comun.mostrarDialogEvent;
import static pe.sulca.eventos.Comun.mostrarDialogo;

/**
 * Created by William_ST on 28/03/18.
 */

public class EventosFCMService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().containsKey("evento") && (remoteMessage.getData().get("dia") == null || remoteMessage.getData().get("ciudad") == null)) {
                //this condition evento is idEvent
                mostrarDialogEvent(getApplicationContext(), remoteMessage.getData().get("evento"), getString(R.string.event_message));
            } else {
                String evento = "";
                evento = "Evento: " + remoteMessage.getData().get("evento") + "\n";
                evento = evento + "Día: " + remoteMessage.getData().get("dia") + "\n";
                evento = evento + "Ciudad: " + remoteMessage.getData().get("ciudad") + "\n";
                evento = evento + "Comentario: "
                        + remoteMessage.getData().get("comentario");
                mostrarDialogo(getApplicationContext(), evento);
            }
        } else {
            if (remoteMessage.getNotification() != null) {
                mostrarDialogo(getApplicationContext(), remoteMessage.getNotification().getBody());
            }
        }

        /*
        if (remoteMessage.getNotification() != null) {
            mostrarDialogo(getApplicationContext(), remoteMessage.getNotification().getBody());
        }
        */
    }
}
