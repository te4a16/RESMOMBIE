package org.example;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.FragmentManager;
import android.widget.Button;
//カメラ機能に必要なインポート
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.util.Log; // Logcatへの出力に必要

/**
 * RESMOMBIEアプリのメインエントリーポイント（アクティビティ）です。
 * Androidアプリのライフサイクル管理とUIの初期化を行います。
 */
public class App extends AppCompatActivity {
    
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    // アプリケーションログ用のタグ
    private static final String TAG = "RESMOMBIE_APP";

    private Button startCameraButton; 
    private boolean cameraLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // activity_main.xml をロード (コンテナが存在するレイアウト)
        setContentView(R.layout.activity_main); 

        //startCameraButton を XML から取得し初期化する
        startCameraButton = findViewById(R.id.start_camera_button);
        
        // 2. カメラ起動処理をボタンクリックイベントに移動 (推奨)
        //    onCreate時に即座にロードするのではなく、ユーザーアクションを待つ方が安全
        startCameraButton.setOnClickListener(v -> {
            if (allPermissionsGranted()) {
                if (!cameraLoaded) {
                    loadCameraFragment();
                    startCameraButton.setEnabled(false);
                }
            } else {
                 // 権限がない場合、再度要求画面を出す
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }
        });

        /* テスト用
        // ログを出力 - デバイス接続後、VS CodeのLogcatビューやターミナルで確認できます。
        Log.i(TAG, "onCreate: RESMOMBIE App is starting...");

        // 画面に表示するTextViewを作成
        TextView textView = new TextView(this);
        textView.setText("Hello RESMOMBIE App on Android! - Connected via ADB/VSCode");
        textView.setTextSize(24);
        
        // TextViewを画面に設定
        setContentView(textView);
        */
    }

    // 権限チェック (以前の回答を参照)
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // ★修正ポイント: 親クラスのメソッド呼び出しを追加
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Toast.makeText(this, "カメラ権限が付与されました。", Toast.LENGTH_SHORT).show();
                loadCameraFragment();
            } else {
                Toast.makeText(this, "カメラ権限がないため、機能を使用できません。", Toast.LENGTH_LONG).show();
                // 権限がない場合はボタンを無効化してもよい
                startCameraButton.setEnabled(false);
            }
        }
    }

    private void loadCameraFragment() {
        // FragmentTransaction を使って CameraFragment をコンテナにロード
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, new CameraFragment());
        transaction.commit();
    }
    
    // その他のライフサイクルメソッド（必要に応じて追加）
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: App is now visible.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: App is going to background.");
    }
}