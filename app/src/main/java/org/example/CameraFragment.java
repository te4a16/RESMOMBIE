package org.example;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;

/**
 * カメラ表示を行う Fragment
 * ------------------------------------
 * ・カメラ起動
 * ・プレビュー表示
 * ・PIP（ピクチャーインピクチャ）に入った/戻った時の画面操作
 * ------------------------------------
 */
public class CameraFragment extends Fragment {

    private PreviewView previewView; // メインプレビュー
    private PreviewView pipPreview;  // PIP用（今は非表示で使わない）

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_camera, container, false);

        previewView = root.findViewById(R.id.previewView);
        pipPreview = root.findViewById(R.id.pipPreview);

        // PIP用プレビューは非表示にして二重描画を防止
        pipPreview.setVisibility(View.GONE);

        startCamera();

        return root;
    }

    /**
     * CameraX を開始
     */
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
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
        }, ContextCompat.getMainExecutor(requireContext())); // ★ ココを修正
    }

    /**
     * Android API の安全な Executor を取得
     * API28以上：Activity.getMainExecutor()
     * API21〜27：ContextCompat.getMainExecutor()
     */
    private Executor getExecutorSafe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return requireActivity().getMainExecutor();
        } else {
            return ContextCompat.getMainExecutor(requireContext());
        }
    }

    /**
     * PIP に入った瞬間（小さい画面になる）
     */
    public void onEnterPipMode() {
        if (previewView != null) previewView.setAlpha(0.85f);
    }

    /**
     * PIP から戻った瞬間（通常画面）
     */
    public void onExitPipMode() {
        if (previewView != null) previewView.setAlpha(1.0f);
    }
}
