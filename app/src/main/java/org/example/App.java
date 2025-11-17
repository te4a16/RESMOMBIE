package org.example;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.widget.Button;
import android.view.View;

import android.content.res.Configuration;

/**
 * アプリのメイン Activity
 * -----------------------------------------
 * ・ボタンなどの UI の管理
 * ・カメラ権限の確認と要求
 * ・CameraFragment の起動
 * ・PIP（ピクチャーインピクチャ）状態の管理
 * -----------------------------------------
 */
public class App extends AppCompatActivity {

    private Button startCameraButton; // 「カメラ開始」ボタン
    private boolean cameraLoaded = false; // CameraFragment を二重起動しないためのフラグ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // メイン画面のレイアウトを読み込む
        setContentView(R.layout.activity_main);

        // XML からボタンを取得
        startCameraButton = findViewById(R.id.start_camera_button);

        // ボタンクリック時の処理
        startCameraButton.setOnClickListener(v -> {

            // カメラ権限があるか？
            if (CameraPermissionHelper.hasPermissions(this)) {

                if (!cameraLoaded) {
                    loadCameraFragment();     // 権限OK → カメラ画面を表示
                    startCameraButton.setEnabled(false); // ボタンは1回だけ押せる
                }

            } else {
                // 権限が無ければ要求する
                CameraPermissionHelper.requestPermissions(this);
            }
        });
    }

    /**
     * 権限要求ダイアログの結果を受け取る処理
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        // ★ Lint 対応：必ず super を呼ぶ
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // CameraPermissionHelper に処理を任せる
        if (CameraPermissionHelper.isPermissionGranted(grantResults)) {

            Toast.makeText(this, "カメラ権限が付与されました。", Toast.LENGTH_SHORT).show();
            loadCameraFragment();

        } else {

            Toast.makeText(this,
                    "カメラ権限がないため、機能を使用できません。",
                    Toast.LENGTH_LONG).show();

            startCameraButton.setEnabled(false);
        }
    }

    /**
     * CameraFragment（カメラ表示用）を画面に配置する
     */
    private void loadCameraFragment() {

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // camera_small_window（FrameLayout）に Fragment を入れる
        transaction.replace(R.id.camera_small_window, new CameraFragment());

        transaction.commit();

        cameraLoaded = true;
    }

    /**
     * アプリがホームに戻る直前に呼ばれる
     * → 自動で PIP モードへ
     */
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        PipController.enter(this);
    }

    /**
     * PIP の ON/OFF で UI 表示を切り替える
     */
    @Override
public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                          @NonNull Configuration newConfig) {

    // ★ Lint 対応：必ず super を呼ぶ
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

    // フラグメントを取得
    CameraFragment fragment = (CameraFragment)
            getSupportFragmentManager().findFragmentById(R.id.camera_small_window);

    if (fragment != null) {
        if (isInPictureInPictureMode) fragment.onEnterPipMode();
        else fragment.onExitPipMode();
    }

    // ボタンは PIP のとき非表示にする
    startCameraButton.setVisibility(
            isInPictureInPictureMode ? View.GONE : View.VISIBLE
    );
}

}
