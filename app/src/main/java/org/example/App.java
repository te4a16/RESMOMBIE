package org.example;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton; // 【重要】ImageButtonをインポートに追加
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

// AlertFragmentのimportは不要（削除済みのため）

public class App extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String TAG = "RESMOMBIE_APP";

    private Button startCameraButton;
    // 【修正】AlertボタンもXML側でImageButtonの可能性があるため、型をImageButtonに統一
    private ImageButton alertButton; 
    
    // 【修正】settingsButtonとcloseButtonの型をImageButtonに変更 (WrongViewCastエラーの解消)
    private ImageButton settingsButton; 
    private ImageButton closeButton; 

    private boolean cameraLoaded = false;
    private boolean isCameraRequest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // アプリ起動時の最初の画面を設定
        setContentView(R.layout.activity_main); 

        // 1. メイン画面のボタン初期化とロジック設定
        initializeActivityMainControls();
        
        // ログ
        Log.i(TAG, "onCreate: RESMOMBIE App is starting...");
    }

    // activity_mainのコントロールを初期化するメソッド
    private void initializeActivityMainControls() {
        // 【修正】startCameraButtonは引き続きButton型として仮定
        startCameraButton = findViewById(R.id.start_camera_button);
        
        // 【修正】alertButtonの初期化を再開し、ImageButton型で処理
        // R.id.start_alert_button がXMLに存在することを確認してください。
        alertButton = findViewById(R.id.start_alert_button); 
        
        // 【修正】settingsButtonをImageButton型で初期化
        settingsButton = findViewById(R.id.settings_button);

        // --- 2. カメラ起動処理 ---
        if (startCameraButton != null) {
            startCameraButton.setOnClickListener(v -> {
                if (allPermissionsGranted()) {
                    if (!cameraLoaded) {
                        loadCameraFragment();
                        startCameraButton.setEnabled(false);
                    }
                } else {
                    isCameraRequest = true;
                    ActivityCompat.requestPermissions(this, getRequiredPermissions(), REQUEST_CODE_PERMISSIONS);
                }
            });
        }
        
        // --- 3. アラート機能の呼び出し ---
        if (alertButton != null) {
            alertButton.setOnClickListener(v -> {
                Toast.makeText(this, "警告通知をトリガーしました（ただし通知権限が必要です）", Toast.LENGTH_SHORT).show();
            });
        }
        
        // --- 4. 設定ページへの遷移処理 ---
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                // 設定ページへ遷移
                setContentView(R.layout.setting_page);
                // 遷移先の画面のボタン初期化処理を呼び出す
                initializeSettingPageControls();
            });
        }
    }

    // setting_pageのコントロールを初期化するメソッド
    private void initializeSettingPageControls() {
        // 【修正】closeButtonをImageButton型で初期化
        closeButton = findViewById(R.id.close_button);

        // --- 5. メイン画面へ戻る処理（close_button） ---
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                // メイン画面へ戻る
                setContentView(R.layout.activity_main);
                // メイン画面のボタン初期化処理を呼び出す
                initializeActivityMainControls();
            });
        }
    }

    // --- 権限関連のヘルパーメソッド ---
    // (省略: 変更なし)
    private String[] getRequiredPermissions() {
        // ... 中略 ...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            return new String[]{
                Manifest.permission.CAMERA
            };
        }
    }

    // (省略: 変更なし)
    private boolean allPermissionsGranted() {
        // ... 中略 ...
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // ... 中略 ...
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Toast.makeText(this, "必要な権限が付与されました。", Toast.LENGTH_SHORT).show();
                if (isCameraRequest && !cameraLoaded) {
                    loadCameraFragment();
                    startCameraButton.setEnabled(false);
                }
            } else {
                Toast.makeText(this, "権限が不足しているため機能が制限されます。", Toast.LENGTH_LONG).show();
            }
            isCameraRequest = false;
        }
    }

    // (省略: 変更なし)
    private void loadCameraFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, new CameraFragment()); 
        transaction.commit();
        cameraLoaded = true;
    }

    // (省略: 変更なし)
    @Override
    protected void onResume() {
        super.onResume();
    }

    // (省略: 変更なし)
    @Override
    protected void onPause() {
        super.onPause();
    }
}
/*package org.example;

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
*/