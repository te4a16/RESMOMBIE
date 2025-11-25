package org.example;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.List;

@ExperimentalCamera2Interop
public class CameraController {

    private static final String TAG = "CameraController";

    public static void startCamera(
            @NonNull PreviewView previewView,
            @NonNull PreviewView pipPreview,   // ← 今は使用しない（後でPIPモードの時だけ使う）
            @NonNull ProcessCameraProvider provider,
            @NonNull LifecycleOwner owner
    ) {
        Context context = previewView.getContext();

        // ★ 超広角レンズ（焦点距離が最短のもの）を取得
        String ultraWideCameraId = findUltraWideCameraId(context);
        Log.d(TAG, "UltraWide camera id = " + ultraWideCameraId);

        CameraSelector selector;

        if (ultraWideCameraId != null) {
            // ★ CameraId を直接指定できる CameraSelector
            selector = new CameraSelector.Builder()
                    .addCameraFilter(cameraInfos -> {
                        List<CameraInfo> out = new ArrayList<>();
                        for (CameraInfo info : cameraInfos) {
                            String id = Camera2CameraInfo.from(info).getCameraId();
                            if (ultraWideCameraId.equals(id)) {
                                out.add(info);
                            }
                        }
                        return out;
                    })
                    .build();
        } else {
            // 超広角が無い場合 → 背面広角1x
            selector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();
        }

        // プレビューは1つだけ（←二重プレビューを防ぐため）
        Preview previewMain = new Preview.Builder().build();
        previewMain.setSurfaceProvider(previewView.getSurfaceProvider());

        provider.unbindAll();
        try {
            provider.bindToLifecycle(owner, selector, previewMain);
        } catch (Exception e) {
            Log.e(TAG, "binding failed", e);
        }
    }

    private static String findUltraWideCameraId(Context context) {
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

            String bestId = null;
            float shortest = Float.MAX_VALUE;

            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics ch = manager.getCameraCharacteristics(id);

                Integer facing = ch.get(CameraCharacteristics.LENS_FACING);
                if (facing == null || facing != CameraCharacteristics.LENS_FACING_BACK) continue;

                float[] focals = ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                if (focals == null || focals.length == 0) continue;

                float f = focals[0];
                if (f < shortest) {
                    shortest = f;
                    bestId = id;
                }
            }
            return bestId;

        } catch (Exception e) {
            Log.e(TAG, "find error", e);
            return null;
        }
    }
}
