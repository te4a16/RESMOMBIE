package org.example;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

/**
 * CameraX の起動や UseCase のバインドだけを担当
 * Fragment のコードをスッキリさせるために分離した
 */
public class CameraController {

    private static final String TAG = "CameraController";

    /**
     * CameraX を PreviewView にバインドする
     */
    public static void startCamera(
            PreviewView previewView,
            @NonNull ProcessCameraProvider provider,
            LifecycleOwner owner
    ) {

        // カメラ映像を表示するための Preview UseCase
        Preview preview = new Preview.Builder().build();

        // PreviewView に映像を表示する
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // 背面カメラを選択
        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // 過去の UseCase を解除（リセット）
        provider.unbindAll();

        // カメラを起動
        try {
            provider.bindToLifecycle(owner, selector, preview);

        } catch (Exception e) {
            Log.e(TAG, "Camera bind failed", e);
        }
    }
}
