package org.example;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

// ViewModel層: カメラの初期化ロジックと状態管理を担う
public class CameraViewModel extends ViewModel {

    private static final String TAG = "CameraViewModel";
    
    /**
     * View (CameraFragment) から呼び出され、カメラの初期化とバインド処理を開始します。
     * 複雑なCameraXの操作をカプセル化し、View層から分離します。
     */
    public void startCamera(
        @NonNull Context context, 
        @NonNull LifecycleOwner lifecycleOwner, 
        @NonNull PreviewView previewView) {

        // 1. ProcessCameraProviderのインスタンスを取得
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(context);

        // 2. 非同期処理としてカメラプロバイダーの準備を待つ
        cameraProviderFuture.addListener(() -> {
            try {
                // プロバイダーが利用可能になったら取得
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // カメラのユースケースをライフサイクルにバインド
                bindCameraUseCases(cameraProvider, lifecycleOwner, previewView, context);
            } catch (ExecutionException | InterruptedException e) {
                // 初期化失敗時のエラーログ
                Log.e(TAG, "Camera initialization failed in ViewModel.", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    /**
     * カメラのユースケース（プレビュー）をFragmentのライフサイクルにバインドします。
     */
    private void bindCameraUseCases(
        @NonNull ProcessCameraProvider cameraProvider, 
        @NonNull LifecycleOwner lifecycleOwner, 
        @NonNull PreviewView previewView,
        @NonNull Context context) {
        
        // 1. プレビューのセットアップ
        Preview preview = new Preview.Builder()
            .build();

        // プレビューの出力をPreviewViewのサーフェスに接続
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // 2. カメラの選択（背面カメラ）
        CameraSelector cameraSelector = new CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build();

        // 3. 既存のユースケースをアンバインド（リソース解放の確認）
        cameraProvider.unbindAll();
        
        try {
            // 4. プレビューをFragmentのライフサイクルにバインド
            // Fragmentが破棄されると、CameraXが自動的にリソースを解放します。
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview
            );
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed in ViewModel", e);
        }
    }
    
    /**
     * ViewModelが破棄される際に呼び出されます。
     * bindToLifecycleを使用しているため、通常は明示的なリソース解放は不要ですが、ログを出力します。
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel is cleared. Camera resources should be released.");
    }
}