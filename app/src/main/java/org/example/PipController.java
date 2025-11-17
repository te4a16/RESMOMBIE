package org.example;

import android.app.Activity;
import android.app.PictureInPictureParams;

/**
 * Picture-in-Picture に入る処理をまとめたクラス
 * App.java をスッキリさせるために分離
 */
public class PipController {

    /** PIP モードに入る */
    public static void enter(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            PictureInPictureParams params =
                    new PictureInPictureParams.Builder().build();

            activity.enterPictureInPictureMode(params);
        }
    }
}
