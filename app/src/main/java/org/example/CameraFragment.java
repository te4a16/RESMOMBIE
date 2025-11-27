package org.example;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

import java.util.Arrays;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

public class CameraFragment extends Fragment {

    private PreviewView previewView;    // 画面に表示するメインカメラプレビュー
    private PreviewView pipPreview;     // PIP用のプレビュー（今回は使用しない）

    private ZoomController zoomController;  // ズーム制御を行うクラス

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // レイアウトを読み込む
        View root = inflater.inflate(R.layout.fragment_camera, container, false);

        previewView = root.findViewById(R.id.previewView);
        pipPreview = root.findViewById(R.id.pipPreview);

        // PIPは現在使わないため非表示にしておく
        pipPreview.setVisibility(View.GONE);

        // ズームコントローラを初期化
        zoomController = new ZoomController(previewView);

        // 端末に搭載されているカメラを全て調べてログに出力
        // 端末が超広角レンズを公開しているかどうかを確認するため
        logAllCameraInfo();

        // カメラプレビューの開始
        startCamera();

        return root;
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private void startCamera() {

        // CameraX のプロバイダを取得
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {

            try {
                ProcessCameraProvider provider = cameraProviderFuture.get();

                // カメラを起動し、PreviewView に映像を表示する
                Camera camera = CameraController.startCamera(
                        previewView,
                        pipPreview,
                        provider,
                        getViewLifecycleOwner()
                );

                if (camera != null) {

                    // ズームコントローラに実際のカメラを渡す
                    zoomController.attachCamera(camera);

                    // デフォルトズームを標準倍率に設定
                    // AQUOS sense6 では超広角が公開されていないため、
                    // linearZoom 0.0 は標準カメラの1.0倍表示になる
                    camera.getCameraControl().setLinearZoom(0.0f);
                    Log.d("CameraFragment", "Applied UltraWide (0.7x) via linearZoom=0.0f");
                }

            } catch (Exception e) {
                Log.e("CameraFragment", "Camera start failed", e);
            }

        }, ContextCompat.getMainExecutor(requireContext()));
    }


    // MainExecutor を安全に取得する
    private Executor getExecutorSafe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return requireActivity().getMainExecutor();
        } else {
            return ContextCompat.getMainExecutor(requireContext());
        }
    }

    // PIPモードに入る時の画面エフェクト
    public void onEnterPipMode() {
        if (previewView != null) previewView.setAlpha(0.85f);
    }

    // PIPモードから戻った時の画面エフェクト
    public void onExitPipMode() {
        if (previewView != null) previewView.setAlpha(1.0f);
    }


    // ============================================================
    // カメラの基本情報をすべてログに出力する
    // 端末が背面複数カメラを公開しているか確認するために使用する
    // ============================================================
    private void logAllCameraInfo() {
        try {
            CameraManager manager =
                    (CameraManager) requireContext().getSystemService(Context.CAMERA_SERVICE);

            // 利用可能なカメラIDをすべて取得
            for (String id : manager.getCameraIdList()) {

                // 各カメラの特性を取得
                CameraCharacteristics c = manager.getCameraCharacteristics(id);

                Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                float[] focals = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                float[] apertures = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
                int[] capabilities = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                int hwLevel = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

                // まとめてログ出力
                Log.d("CAMERA_INFO", "---- Camera ID: " + id + " ----");
                Log.d("CAMERA_INFO", "Facing: " + lensFacingToString(lensFacing));
                Log.d("CAMERA_INFO", "Focal Lengths: " + Arrays.toString(focals));
                Log.d("CAMERA_INFO", "Apertures: " + Arrays.toString(apertures));
                Log.d("CAMERA_INFO", "Capabilities: " + Arrays.toString(capabilities));
                Log.d("CAMERA_INFO", "Hardware Level: " + hwLevelToString(hwLevel));
            }

        } catch (Exception e) {
            Log.e("CAMERA_INFO", "Camera info error", e);
        }
    }

    // レンズの向きを文字列に変換する
    private String lensFacingToString(Integer facing) {
        if (facing == null) return "null";
        switch (facing) {
            case CameraCharacteristics.LENS_FACING_BACK:
                return "BACK";
            case CameraCharacteristics.LENS_FACING_FRONT:
                return "FRONT";
            case CameraCharacteristics.LENS_FACING_EXTERNAL:
                return "EXTERNAL";
            default:
                return "UNKNOWN";
        }
    }

    // ハードウェアレベルを読みやすい文字列に変換する
    private String hwLevelToString(int level) {
        switch (level) {
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                return "FULL";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                return "LEGACY";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                return "LIMITED";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                return "LEVEL_3";
            default:
                return "UNKNOWN";
        }
    }

}
