package org.example;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

    private static final String TAG = "CameraFragment";
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    // 1. FragmentのViewを生成
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // fragment_camera.xml をインフレート
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    // 2. Viewが作成された後にPreviewViewを取得
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        previewView = view.findViewById(R.id.previewView);

        // カメラプロバイダーを取得し、カメラ機能を初期化
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed.", e);
                Toast.makeText(getContext(), "Camera initialization failed.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    // 3. カメラのユースケース（Previewなど）をライフサイクルにバインド
    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        // プレビューのセットアップ
        Preview preview = new Preview.Builder()
                .build();

        // プレビューをPreviewViewに接続
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // 背面カメラを選択
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // 既存のユースケースをアンバインドしてから、新しいユースケースをバインド
        cameraProvider.unbindAll();
        
        try {
            // プレビューをFragmentのライフサイクルにバインド
            cameraProvider.bindToLifecycle(
                    (LifecycleOwner) this,
                    cameraSelector,
                    preview
            );
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }
}