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

    // ★ PIP（ミニ画面）で使うプレビューだけ
    private PreviewView miniPreview;

    // CameraX のカメラプロバイダ（非同期で取得される）
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // fragment_camera.xml を画面に表示する
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // ★ レイアウト内の小さいカメラ枠（PIPでも使われる部分）
        miniPreview = view.findViewById(R.id.pipPreview);

        // ★ CameraX のカメラ provider を非同期で取得
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        // Provider が取得できたらカメラを起動する
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // カメラのプレビュー用意
                bindCameraUseCases(cameraProvider);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed.", e);
                Toast.makeText(getContext(), "Camera initialization failed.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * ★ カメラ機能を miniPreview に紐付ける処理
     *
     * CameraX は UseCase(カメラ機能) を bind する形で動く。
     * 今回は Preview（映像を表示する機能）だけ使う。
     */
    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {

        // ★ ミニプレビュー画面に映像を流すための Preview UseCase
        Preview previewMini = new Preview.Builder().build();

        // miniPreview（PreviewView）に映像を送る
        previewMini.setSurfaceProvider(miniPreview.getSurfaceProvider());

        // ★ 利用するカメラ（今回は背面カメラ固定）
        CameraSelector cameraSelector =
                new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

        // 既存の UseCase をすべて解除（毎回クリーンな状態にする）
        cameraProvider.unbindAll();

        // ★ カメラを起動し、プレビューを miniPreview に出す
        try {
            cameraProvider.bindToLifecycle(
                    (LifecycleOwner) this, // Fragment のライフサイクルに従う
                    cameraSelector,
                    previewMini
            );
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    // ★ PIP に入った時の処理（ミニプレビューは常に表示しておく）
    public void onEnterPipMode() {
        if (miniPreview != null) miniPreview.setVisibility(View.VISIBLE);
    }

    // ★ PIP から戻った時の処理（ミニプレビューはそのまま表示）
    public void onExitPipMode() {
        if (miniPreview != null) miniPreview.setVisibility(View.VISIBLE);
    }
}
