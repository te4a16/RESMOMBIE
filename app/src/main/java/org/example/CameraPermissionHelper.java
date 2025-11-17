package org.example;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CameraPermissionHelper {

    // 要求する権限
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA
    };

    // リクエストコード
    public static final int REQUEST_CODE_PERMISSIONS = 10;

    /** カメラ権限があるか？ */
    public static boolean hasPermissions(Activity activity) {
        return ContextCompat.checkSelfPermission(
                activity, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /** 権限要求ダイアログを表示 */
    public static void requestPermissions(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
        );
    }

    /** 権限が許可されたかを grantResults から判断する */
    public static boolean isPermissionGranted(int[] grantResults) {
        if (grantResults.length == 0) return false;
        return grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }
}
