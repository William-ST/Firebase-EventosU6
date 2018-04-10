package pe.sulca.eventos;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import static pe.sulca.eventos.Comun.guardarIdRegistro;

/**
 * Created by William_ST on 29/03/18.
 */

public class EventosFCMInstanceIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        String idPush;
        idPush = FirebaseInstanceId.getInstance().getToken();
        guardarIdRegistro(getApplicationContext(), idPush);
    }

}
