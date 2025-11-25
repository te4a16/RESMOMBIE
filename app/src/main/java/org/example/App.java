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
 * ==============================================
 * App.java（実質 MainActivity）
 * ----------------------------------------------
 * ・メイン画面の UI（ボタンなど）を管理
 * ・カメラ権限のチェックとリクエスト
 * ・CameraFragment のロード
 * ・PIP（ピクチャーインピクチャ）制御
 * ==============================================
 */
public class App extends AppCompatActivity {

    private Button startCameraButton;     // 「カメラ開始」ボタン
    private boolean cameraLoaded = false; // CameraFragment が二重挿入されるのを防ぐフラグ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // メイン画面のレイアウトをセット
        setContentView(R.layout.activity_main);

        // UI 初期化
        startCameraButton = findViewById(R.id.start_camera_button);

        // 「カメラ開始」ボタン押下時
        startCameraButton.setOnClickListener(v -> {

            // ---- カメラ権限チェック ----
            if (CameraPermissionHelper.hasPermissions(this)) {

                // Fragment の二重挿入防止
                if (!cameraLoaded) {
                    loadCameraFragment();
                    startCameraButton.setEnabled(false);
                }

            } else {
                // 権限要求
                CameraPermissionHelper.requestPermissions(this);
            }
        });
    }

    /**
     * --------------------------------------------------------------
     * 権限ダイアログの結果を受け取る
     * CameraPermissionHelper に判定は委譲している
     * --------------------------------------------------------------
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        // lint エラー回避のため必ず super を呼ぶ
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

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
     * --------------------------------------------------------------
     * CameraFragment を画面に追加する
     * --------------------------------------------------------------
     */
    private void loadCameraFragment() {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        // FrameLayout（camera_small_window）に Fragment を配置
        ft.replace(R.id.camera_small_window, new CameraFragment());

        ft.commit();

        cameraLoaded = true;
    }

    /**
     * --------------------------------------------------------------
     * ホームボタンなどで Activity がバックグラウンドに行く直前
     * → 自動で PIP に切り替える
     * --------------------------------------------------------------
     */
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        PipController.enter(this);
    }

    /**
     * --------------------------------------------------------------
     * PIP の状態変化に応じて UI を切り替える
     * --------------------------------------------------------------
     */
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                              @NonNull Configuration newConfig) {

        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

        // 今表示されている CameraFragment を取得
        CameraFragment fragment = (CameraFragment)
                getSupportFragmentManager().findFragmentById(R.id.camera_small_window);

        if (fragment != null) {
            if (isInPictureInPictureMode) fragment.onEnterPipMode();
            else fragment.onExitPipMode();
        }

        // PIP 中はボタンを隠す
        startCameraButton.setVisibility(
                isInPictureInPictureMode ? View.GONE : View.VISIBLE
        );
    }
}
