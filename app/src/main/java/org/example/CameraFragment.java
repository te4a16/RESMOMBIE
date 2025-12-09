package org.example;

import android.graphics.Bitmap;
import android.graphics.RectF;
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

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.support.image.TensorImage;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.CameraSelector;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.widget.Toast;
import android.graphics.Matrix;
import androidx.camera.view.TransformExperimental;
import androidx.camera.core.ExperimentalGetImage;
import java.util.Arrays;
import android.util.Log;



public class CameraFragment extends Fragment {


    private PreviewView previewView;    // 画面に表示するメインカメラプレビュー
    private PreviewView pipPreview;     // PIP用のプレビュー（今回は使用しない）
    private ZoomController zoomController;  // ズーム制御を行うクラス
    private OverlayView overlayView;

    private DetectorHelper detectorHelper;
    private Executor analysisExecutor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // レイアウトを読み込む
        View root = inflater.inflate(R.layout.fragment_camera, container, false);

        previewView = root.findViewById(R.id.previewView);
        pipPreview = root.findViewById(R.id.pipPreview);
        overlayView = root.findViewById(R.id.overlay);

        // PIPは現在使わないため非表示にしておく
        pipPreview.setVisibility(View.GONE);

        // ズームコントローラを初期化
        zoomController = new ZoomController(previewView);

        // 端末に搭載されているカメラを全て調べてログに出力
        // 端末が超広角レンズを公開しているかどうかを確認するため
        logAllCameraInfo();

        // Detector の初期化（assets のモデル名を渡す）
        detectorHelper = new DetectorHelper(requireContext(), "1.tflite");

        if (!detectorHelper.isInitialized()) {
            // ユーザーにエラーを通知
            Log.e("CameraFragment", "DetectorHelperの初期化に失敗しました。カメラは起動しません。");
            // モデル初期化失敗をユーザーに通知し、カメラ解析をスキップする
            Toast.makeText(requireContext(), "モデルの読み込みに失敗しました。検出機能は無効です。", Toast.LENGTH_LONG).show();
            // startCamera() を呼び出すが、ImageAnalysis は isInitialized() でチェックされるため安全
        }

        analysisExecutor = Executors.newSingleThreadExecutor();
        // カメラプレビューの開始
        startCamera();

        return root;
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {

            try {
                ProcessCameraProvider provider = cameraProviderFuture.get();

                // 🔴 修正: 'selector' 変数の定義
                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // 🔴 修正: 'previewMain' 変数の定義
                Preview previewMain = new Preview.Builder().build();
                previewMain.setSurfaceProvider(previewView.getSurfaceProvider());

                // ImageAnalysis を追加（解析解像度は小さめにしてパフォーマンス確保）
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageRotationEnabled(true)
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .setTargetResolution(new android.util.Size(320, 320))
                        .build();

                imageAnalysis.setAnalyzer(analysisExecutor, new ImageAnalysis.Analyzer() {
                    // ... (analyze メソッド全体は変更なし)
                    @Override
                    @OptIn(markerClass = ExperimentalGetImage.class)
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        if (!detectorHelper.isInitialized()) {
                            imageProxy.close();
                            return;
                        }

                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        Bitmap bmp = YuvToRgbConverter.imageProxyToBitmap(requireContext(), imageProxy, rotationDegrees);

                        if (bmp != null) {
                            TensorImage tImage = TensorImage.fromBitmap(bmp);
                            List<DetectorHelper.SimpleDetection> results = detectorHelper.detect(tImage);

                            // --- ★ 座標変換 Matrix の最終修正 (fillStart対応版) ★ ---
                            final int MODEL_INPUT_SIZE = 320;
                            int previewWidth = previewView.getWidth();
                            int previewHeight = previewView.getHeight();
                            Matrix matrix = new Matrix();

                            float scaleX = (float) previewWidth / MODEL_INPUT_SIZE;
                            float scaleY = (float) previewHeight / MODEL_INPUT_SIZE;

                            // fillStart (クロップあり、画面を覆う) に合わせるため Math.max を使用
                            float scaleFactor = Math.max(scaleX, scaleY);

                            matrix.postScale(scaleFactor, scaleFactor);
                            // fillStart は左上寄せなので、中央寄せのための postTranslate は削除する
                            // --- 座標変換 Matrix 作成 完了 ---

                            // overlayView.setScale(1f, 1f); // setScale があれば削除またはコメントアウト

                            List<OverlayView.OverlayBox> boxes = new ArrayList<>();

                            for (DetectorHelper.SimpleDetection d : results) {
                                // 検出結果の RectF を Matrix で変換
                                RectF transformedBBox = new RectF(d.bbox);
                                matrix.mapRect(transformedBBox);
                                boxes.add(new OverlayView.OverlayBox(transformedBBox, d.label, d.score, 0xFFFF0000));
                            }

                            overlayView.setBoxes(boxes);

                        } else {
                            overlayView.setBoxes(null);
                        }
                        imageProxy.close();
                    }
                });

                provider.unbindAll();
                // 🔴 修正: bindToLifecycle の呼び出し (selector, previewMain を使用)
                Camera camera = provider.bindToLifecycle(getViewLifecycleOwner(), selector, previewMain, imageAnalysis);

                if (camera != null) {
                    zoomController.attachCamera(camera);
                    camera.getCameraControl().setLinearZoom(0.0f);
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
