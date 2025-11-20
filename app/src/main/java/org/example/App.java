package org.example;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

/**
 * RESMOMBIEアプリのメインエントリーポイント（アクティビティ）です。
 * UIとアプリケーションのライフサイクルを管理し、CameraFragmentおよびAlertFragmentをホストします。
 * 各機能のロジックは対応するFragmentに委譲されています。
 */
public class App extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    // カメラは実行時権限が必要です。
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private static final String TAG = "RESMOMBIE_APP";

    private Button startCameraButton;
    private Button alertButton; 
    private boolean cameraLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // activity_main.xml をロード
        setContentView(R.layout.activity_main); 

        // 1. ボタンの初期化
        startCameraButton = findViewById(R.id.start_camera_button);
        alertButton = findViewById(R.id.alert_button); 

        // 2. カメラ起動処理（CameraFragmentの呼び出し）
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
        
        // 3. アラート機能の呼び出し（AlertFragmentの表示）
        alertButton.setOnClickListener(v -> {
            // AlertFragmentをDialogFragmentとして表示
            new AlertFragment().show(getSupportFragmentManager(), "buzzer_alert_dialog");
        });
        
        Log.i(TAG, "onCreate: RESMOMBIE App is starting...");
    }
    
    // --- カメラ機能関連のヘルパーメソッド ---

    /**
     * すべての必須権限（CAMERA）が付与されているかチェックします。
     */
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Toast.makeText(this, "カメラ権限が付与されました。", Toast.LENGTH_SHORT).show();
                loadCameraFragment();
            } else {
                Toast.makeText(this, "カメラ権限がないため、機能を使用できません。", Toast.LENGTH_LONG).show();
                startCameraButton.setEnabled(false);
            }
        }
    }

    /**
     * CameraFragmentをコンテナビューにロードします。
     */
    private void loadCameraFragment() {
        // FragmentTransaction を使って CameraFragment をコンテナにロード
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, new CameraFragment());
        transaction.commit();
        cameraLoaded = true; // カメラがロードされたフラグを設定
    }
    
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