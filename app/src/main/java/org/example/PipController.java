package org.example;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.os.Build;
import android.util.Rational;

public class PipController {

    /**
     * Enter Picture-in-Picture mode (if supported).
     * Call from Activity.onUserLeaveHint() or when user requests.
     */
    public static void enter(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational aspect = new Rational(4, 3); // 好みで変更（PiPウィンドウ比）
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspect)
                    .build();
            activity.enterPictureInPictureMode(params);
        }
    }
}
