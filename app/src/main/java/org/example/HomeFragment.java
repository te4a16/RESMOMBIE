package org.example;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.content.pm.PackageManager;
import android.util.Log;

// ホーム画面のUIとユーザー入力の責務を持つ View 層
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private HomeViewModel viewModel;
    private Button startDetectionButton; 
    
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    private static final int REQUEST_CODE_PERMISSIONS = 10;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // fragment_home.xml をインフレート（AI検出ボタンを含むレイアウト）
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. ViewModelの初期化 (Activityスコープを共有)
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // 2. UIコンポーネントの初期化
        startDetectionButton = view.findViewById(R.id.startDetectionButton); 

        // 3. ユーザーアクションをViewModelへ伝達
        startDetectionButton.setOnClickListener(v -> {
            boolean granted = allPermissionsGranted();

            // ViewModelにアクションと現在の権限状態を伝える
            viewModel.onStartCameraClicked(granted);

            // 権限がない場合は、Viewの責務として権限リクエストUIを表示
            if (!granted) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }
        });
        
        // 4. UIの状態監視
        observeViewModel();
    }

    private void observeViewModel() {
        // 必要に応じて、権限状態が変更された際のボタンの有効/無効化などをここで行う
        viewModel.cameraPermissionGranted.observe(getViewLifecycleOwner(), granted -> {
            if (!granted.booleanValue()) {
                // 権限がない場合のボタンの無効化など
                // startDetectionButton.setEnabled(false); 
            }
        });
    }

    // 権限チェックはFragment内に残す
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    // Fragmentでの権限結果の受け取り
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean granted = allPermissionsGranted();
            
            // 権限リクエストの結果をViewModelに渡し、状態を更新させる
            viewModel.onPermissionResult(granted);
            
            if (!granted) {
                Toast.makeText(requireContext(), "カメラ権限がないため、機能を使用できません。", Toast.LENGTH_LONG).show();
                // startDetectionButton.setEnabled(false);
            }
        }
    }
}