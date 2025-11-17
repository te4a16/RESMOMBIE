package org.example;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import android.widget.Button;
// カメラ機能
import android.content.pm.PackageManager;

import android.app.PictureInPictureParams; // PIP（ピクチャインピクチャ）
import android.content.res.Configuration;
import android.view.View;

import android.util.Log;


/**
 * RESMOMBIEアプリのメイン画面（Activity）
 * - UI 初期化
 * - ボタン処理
 * - カメラ権限の要求
 * - CameraFragment の起動
 * - PIP の状態管理
 */
public class App extends AppCompatActivity {

    // カメラ権限要求のための定数
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    // Logcat 用タグ
    private static final String TAG = "RESMOMBIE_APP";

    // カメラ開始ボタン
    private Button startCameraButton;
    private boolean cameraLoaded = false; // Fragment が重複起動しないようにするフラグ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 画面のレイアウト（activity_main.xml）をセット
        setContentView(R.layout.activity_main);

        // XML からボタンを取得
        startCameraButton = findViewById(R.id.start_camera_button);

        /**
         * カメラ起動ボタンのクリック処理
         * - 権限があれば CameraFragment を表示
         * - 権限がなければ要求する
         */
        startCameraButton.setOnClickListener(v -> {
            if (allPermissionsGranted()) {
                // Fragment がまだ読み込まれていない場合のみ起動
                if (!cameraLoaded) {
                    loadCameraFragment();
                    startCameraButton.setEnabled(false); // ボタン押せなくする
                }
            } else {
                // カメラ権限を要求
                ActivityCompat.requestPermissions(
                        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                );
            }
        });

        Log.i(TAG, "onCreate: App started.");
    }

    /**
     * カメラ権限が許可されているか確認する
     */
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            // 許可されていなければ false
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 権限要求ダイアログの結果を受け取る
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // カメラ権限の結果処理
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Toast.makeText(this, "カメラ権限が付与されました。", Toast.LENGTH_SHORT).show();
                loadCameraFragment();
            } else {
                Toast.makeText(this,
                        "カメラ権限がないため、機能を使用できません。",
                        Toast.LENGTH_LONG).show();
                startCameraButton.setEnabled(false); // 使えないためボタン無効化
            }
        }
    }

    /**
     * CameraFragment を画面に表示する
     */
    private void loadCameraFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // camera_small_window（XML の FrameLayout）に CameraFragment を入れる
        transaction.replace(R.id.camera_small_window, new CameraFragment());
        transaction.commit();

        cameraLoaded = true;
    }

    // ログ用（任意）
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: App visible.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: App background.");
    }

    /**
     * ホームボタン押下時などに呼ばれる
     * → PIP へ自動で移行する
     */
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        enterPipMode();
    }

    /**
     * Picture-in-Picture モードに入る
     */
    private void enterPipMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            // PIP のパラメータ（今回はサイズの指定なし）
            PictureInPictureParams params =
                    new PictureInPictureParams.Builder().build();

            // PIP に移行
            enterPictureInPictureMode(params);
        }
    }

    /**
     * PIP の出入りで UI を調整
     */
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                              @NonNull Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

        // 現在表示中の CameraFragment を取得
        CameraFragment fragment = (CameraFragment) getSupportFragmentManager()
                .findFragmentById(R.id.camera_small_window);

        // Fragment 側に PIP 状態を通知
        if (fragment != null) {
            if (isInPictureInPictureMode) {
                fragment.onEnterPipMode(); // PIP 入った時
            } else {
                fragment.onExitPipMode();  // PIP 退出した時
            }
        }

        /**
         * ボタン表示/非表示
         * - 通常: 表示
         * - PIP: 非表示（画面が小さすぎるため）
         */
        if (startCameraButton != null) {
            startCameraButton.setVisibility(
                    isInPictureInPictureMode ? View.GONE : View.VISIBLE
            );
        }
    }
}
