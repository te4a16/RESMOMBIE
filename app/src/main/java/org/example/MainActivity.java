package org.example;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import android.util.Log;

/**
 * RESMOMBIEアプリのメインエントリーポイント（アクティビティ）。
 * 純粋なナビゲーションホストとして機能し、HomeViewModelからの遷移指示を実行します。
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "RESMOMBIE_APP_HOST";
    private HomeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_main.xml をロード（コンテナのみを含む）
        setContentView(R.layout.activity_main); 

        // 1. ViewModelの初期化 (Activityスコープ)
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // 2. ナビゲーションホストの初期化 (初回起動時にHomeFragmentをロード)
        if (savedInstanceState == null) {
            loadHomeFragment();
        }
        
        // 3. ViewModelのLiveDataを監視 (Observe)
        observeViewModel();
    }

    private void loadHomeFragment() {
         FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
         // HomeFragmentをコンテナに配置
         transaction.replace(R.id.container, new HomeFragment());
         transaction.commit();
    }
    
    /**
     * HomeViewModelのナビゲーションイベントを監視し、Fragment遷移を実行します。
     */
    private void observeViewModel() {
        viewModel.navigationEvent.observe(this, event -> {
            if (event != null) {
                handleNavigation(event);
            }
        });
    }

    private void handleNavigation(HomeViewModel.NavigationEvent event) {
        switch (event) {
            case TO_CAMERA_FRAGMENT:
                loadCameraFragment();
                Toast.makeText(this, "AI検出のためにカメラを起動します。", Toast.LENGTH_SHORT).show();
                break;
            // 他の遷移先があればここに追加
        }
    }

    // Fragmentのロード処理 (UI操作なのでViewに残す)
    private void loadCameraFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, new CameraFragment());
        // 戻るボタンでホームに戻れるようにバックスタックに追加
        transaction.addToBackStack("camera_screen"); 
        transaction.commit();
    }
}