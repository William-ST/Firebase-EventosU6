package pe.sulca.eventos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by William_ST on 29/03/18.
 */

public class ReceptorInicio extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, EventosFCMService.class));
    }

}
