package org.example;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import android.annotation.SuppressLint;

import com.google.common.util.concurrent.ListenableFuture;

public class CameraFragment extends Fragment {

    private PreviewView previewView;    // メイン用プレビュー
    private PreviewView pipPreview;     // 左上ミニプレビュー

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_camera, container, false);

        previewView = root.findViewById(R.id.previewView);
        pipPreview = root.findViewById(R.id.pipPreview);

        // カメラ開始
        startCamera();

        // 黒枠削除（画面いっぱい）
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        pipPreview.setScaleType(PreviewView.ScaleType.FILL_CENTER);

        return root;
    }

    @SuppressLint("NewApi")
private void startCamera() {
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext());

    cameraProviderFuture.addListener(() -> {
        try {
            ProcessCameraProvider provider = cameraProviderFuture.get();

            CameraController.startCamera(
                    previewView,
                    pipPreview,
                    provider,
                    getViewLifecycleOwner()
            );

        } catch (Exception e) {
            Log.e("CameraFragment", "Camera start failed", e);
        }
    }, requireActivity().getMainExecutor());
}


    // PIP に入ったとき
    public void onEnterPipMode() {
        if (previewView != null) previewView.setAlpha(0.8f);
        if (pipPreview != null) pipPreview.setAlpha(0.8f);
    }

    // PIP から戻ったとき
    public void onExitPipMode() {
        if (previewView != null) previewView.setAlpha(1.0f);
        if (pipPreview != null) pipPreview.setAlpha(1.0f);
    }
}
