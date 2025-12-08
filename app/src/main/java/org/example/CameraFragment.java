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
import android.graphics.Color;


public class CameraFragment extends Fragment {


    private PreviewView previewView;// 画面に表示するメインカメラプレビュー
    private PreviewView pipPreview;// PIP用のプレビュー（今回は使用しない）
    private ZoomController zoomController;// ズーム制御を行うクラス
    private OverlayView overlayView;

    private DetectorHelper detectorHelper;
    private Executor analysisExecutor;
    
    // UIに描画する検出ボックスの最大数
    private static final int MAX_UI_BOXES = 5; 
    
    // person 以外の検出ボックスの色
    private static final int COLOR_DEFAULT = Color.YELLOW; 
    // person の検出ボックスの色
    private static final int COLOR_PERSON = Color.RED; 
    
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
        logAllCameraInfo();

        // Detector の初期化（assets のモデル名を渡す）
        // DetectorHelperのコンストラクタで setScoreThreshold(0.03f) などが設定されている前提
        detectorHelper = new DetectorHelper(requireContext(), "efficientdet_lite0.tflite"); // モデル名は適宜修正してください

        if (!detectorHelper.isInitialized()) {
            // ユーザーにエラーを通知
            Log.e("CameraFragment", "DetectorHelperの初期化に失敗しました。カメラは起動しません。");
            Toast.makeText(requireContext(), "モデルの読み込みに失敗しました。検出機能は無効です。", Toast.LENGTH_LONG).show();
        }

        analysisExecutor = Executors.newSingleThreadExecutor();
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

                // セレクタは CameraController と同様の選定を使いたいので簡潔に BACK を指定
                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                Preview previewMain = new Preview.Builder().build();
                previewMain.setSurfaceProvider(previewView.getSurfaceProvider());

                // ImageAnalysis を追加（解析解像度は小さめにしてパフォーマンス確保）
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageRotationEnabled(true)
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .setTargetResolution(new android.util.Size(320, 320)) // EfficientDet-Lite0の標準サイズ
                        .build();

                imageAnalysis.setAnalyzer(analysisExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    @OptIn(markerClass = ExperimentalGetImage.class) 
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        if (!detectorHelper.isInitialized()) {
                            imageProxy.close();
                            return;
                        }

                        Bitmap bmp = YuvToRgbConverter.imageProxyToBitmap(requireContext(), imageProxy);
                        if (bmp != null) { 
                            TensorImage tImage = TensorImage.fromBitmap(bmp);
                            List<DetectorHelper.SimpleDetection> results = detectorHelper.detect(tImage);

                            // 検出結果の座標を PreviewView 座標系に変換
                            onImageAnalyze(results, imageProxy);

                        } else {
                            overlayView.setBoxes(null);
                        }
                        imageProxy.close();
                    }
                });

                provider.unbindAll();
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
    
    /**
     * TFLiteの検出結果をOverlayViewに描画するための座標変換を行う
     */
    private void onImageAnalyze(List<DetectorHelper.SimpleDetection> results, @NonNull ImageProxy imageProxy) {
        
        // --- ★ 座標変換 Matrix の手動作成 ★ ---
        Matrix matrix = new Matrix();

        // 1. 画像の回転後の幅と高さを計算
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        int rotatedWidth = (rotationDegrees == 90 || rotationDegrees == 270) 
                             ? imageProxy.getHeight() : imageProxy.getWidth();
        int rotatedHeight = (rotationDegrees == 90 || rotationDegrees == 270) 
                              ? imageProxy.getWidth() : imageProxy.getHeight();

        // **★ 修正箇所１：X軸反転処理を最初に適用 ★**
        // 背面カメラだが、表示が反転している（ミラーリング）現象に対応するため、X軸方向に -1.0 倍する
        final float FLIP_SCALE = -1.0f; 
        final float NORM_SCALE = 1.0f; 
        matrix.preScale(FLIP_SCALE, NORM_SCALE, rotatedWidth / 2f, rotatedHeight / 2f);
        
        // 2. 画像の回転補正を適用
        matrix.postRotate(rotationDegrees, rotatedWidth / 2f, rotatedHeight / 2f);

        // 3. プレビュー表示サイズに合わせたスケーリングと移動を適用
        int previewWidth = previewView.getWidth();
        int previewHeight = previewView.getHeight();

        // スケーリングファクタを計算
        float scaleX = (float) previewWidth / rotatedWidth;
        float scaleY = (float) previewHeight / rotatedHeight;

        // PreviewView.ScaleType.FIT_CENTER に合わせる
        float scaleFactor = Math.min(scaleX, scaleY); 

        // スケーリングを適用
        matrix.postScale(scaleFactor, scaleFactor);
        
        // 4. アスペクト比維持によるオフセット（中央寄せ）の計算
        float dx = (previewWidth - rotatedWidth * scaleFactor) / 2f;
        float dy = (previewHeight - rotatedHeight * scaleFactor) / 2f;

        matrix.postTranslate(dx, dy); // オフセットを適用

        // --- 座標変換 Matrix 作成 完了 ---
        
        // OverlayViewの描画スケールをリセット (Matrixで変換済みのため)
        overlayView.setScale(1f, 1f);

        List<OverlayView.OverlayBox> boxes = new ArrayList<>();
        
        // **★ 修正箇所２：表示するボックスの最大数を制限する ★**
        // 連続で検出されるノイズ対策として、表示数を制限
        for (int i = 0; i < results.size() && i < MAX_UI_BOXES; i++) {
            DetectorHelper.SimpleDetection d = results.get(i);
            
            RectF originalBBox = d.bbox; 
            RectF transformedBBox = new RectF(originalBBox); 
            
            // Matrixを使って座標をPreviewViewピクセル座標に変換
            matrix.mapRect(transformedBBox); 
            
            // ラベルに応じて色を設定
            int color = d.label.equalsIgnoreCase("person") ? COLOR_PERSON : COLOR_DEFAULT;
            
            boxes.add(new OverlayView.OverlayBox(transformedBBox, d.label, d.score, color));
        }
        
        overlayView.setBoxes(boxes);
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
                // int[] capabilities = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES); // この行はコメントアウトまたは削除しても動作に影響なし
                int hwLevel = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

                // まとめてログ出力
                Log.d("CAMERA_INFO", "---- Camera ID: " + id + " ----");
                Log.d("CAMERA_INFO", "Facing: " + lensFacingToString(lensFacing));
                Log.d("CAMERA_INFO", "Focal Lengths: " + Arrays.toString(focals));
                Log.d("CAMERA_INFO", "Apertures: " + Arrays.toString(apertures));
                // Log.d("CAMERA_INFO", "Capabilities: " + Arrays.toString(capabilities));
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