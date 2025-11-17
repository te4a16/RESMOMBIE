package org.example;

// Android 基本
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

// AndroidX / CameraX
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class CameraFragment extends Fragment {

    // Logcat 用のタグ
    private static final String TAG = "CameraFragment";

    // カメラ映像を表示する2つのプレビューView（大画面とミニ）
    private PreviewView previewView;   // 全画面用
    private PreviewView miniPreview;   // 左上ミニ画面用

    // CameraX のカメラプロバイダー
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;


    /**
     * Fragment の UI レイアウトを生成する（XML を inflate）
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // fragment_camera.xml を読み込む
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }


    /**
     * onCreateView の後で実行される。
     * ここで XML 内の previewView を取得して CameraX を開始する準備を行う。
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // XML の PreviewView を取得
        previewView = view.findViewById(R.id.previewView);   // 大画面
        miniPreview = view.findViewById(R.id.miniPreview);   // 左上ミニ画面

        // CameraX のプロバイダーを非同期で取得
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        // カメラ準備ができたら実行する処理
        cameraProviderFuture.addListener(() -> {
            try {
                // CameraProvider を取得
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // カメラを PreviewView にバインドする
                bindCameraUseCases(cameraProvider);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed.", e);
                Toast.makeText(getContext(), "Camera initialization failed.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));  // UIスレッドで実行
    }


    /**
     * CameraX のユースケース（Preview）を画面にバインドする。
     * 大画面 preview + 左上ミニ preview の 2 つを同時にバインドする。
     */
    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        // -------------------------
        // ① 大プレビュー用 Preview
        // -------------------------
        Preview previewBig = new Preview.Builder().build();
        previewBig.setSurfaceProvider(previewView.getSurfaceProvider());
    
        // -------------------------
        // ② ミニプレビュー用 Preview
        // -------------------------
        Preview previewMini = new Preview.Builder().build();
        previewMini.setSurfaceProvider(miniPreview.getSurfaceProvider());
    
        // -------------------------
        // ③ カメラ選択（背面）
        // -------------------------
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
    
        // -------------------------
        // ④ 既存のユースケース解除 → 新しく2つバインド
        // -------------------------
        cameraProvider.unbindAll();
    
        try {
            cameraProvider.bindToLifecycle(
                    (LifecycleOwner) this,
                    cameraSelector,
                    previewBig,
                    previewMini  // ←ミニビューもバインド
            );
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }
}
    
