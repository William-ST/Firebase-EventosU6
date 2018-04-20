package pe.sulca.eventos;

import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterCore;

/**
 * Created by William_ST on 20/04/18.
 */

public class AuthenticateUtil {

    public static TwitterApiClient getApiClientTwitter() {
        return TwitterCore.getInstance().getApiClient();
    }

    public static boolean isLoginTwitter() {
        if (TwitterCore.getInstance().getSessionManager() != null && TwitterCore.getInstance().getSessionManager().getActiveSession() != null) {
            return true;
        }
        return false;
    }

}
