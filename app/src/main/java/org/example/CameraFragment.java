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

import java.util.Arrays;



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

        // CameraX のプロバイダを取得
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {

            try {
                ProcessCameraProvider provider = cameraProviderFuture.get();

                // CameraController.startCamera は Preview のみを bind しているので、
                // ここでは ImageAnalysis を追加して bind し直す

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
                        .setTargetResolution(new android.util.Size(320, 320)) // 小さめで高速化
                        .build();

                imageAnalysis.setAnalyzer(analysisExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        if (!detectorHelper.isInitialized()) {
                            imageProxy.close();
                            return;
                        }
                    
                        // ImageProxy -> Bitmap -> TensorImage の簡易変換
                        Bitmap bmp = YuvToRgbConverter.imageProxyToBitmap(requireContext(), imageProxy);
                        if (bmp != null) {
                            TensorImage tImage = TensorImage.fromBitmap(bmp);
                            List<DetectorHelper.SimpleDetection> results = detectorHelper.detect(tImage);
                    
                            // --- ★ 座標変換のための Matrix を作成する ★ ---
                            
                            // ImageProxyの座標系をPreviewViewの座標系に合わせるための変換行列を取得
                            // これにより、回転やクロップ処理が自動で考慮される
                            Matrix matrix = new Matrix();
                            
                            // 1. ImageAnalysisの入力サイズを取得
                            RectF srcRect = new RectF(0, 0, imageProxy.getWidth(), imageProxy.getHeight());
                            
                            // 2. PreviewViewの表示エリアを取得
                            RectF dstRect = new RectF(0, 0, previewView.getWidth(), previewView.getHeight());
                    
                            // 3. 変換（ImageAnalysisの向きに回転した後、PreviewViewの表示方法に合わせて拡大縮小）
                            // CameraX の PreviewView.getOutputTransform() のロジックに近い処理
                            matrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.FILL);
                            
                            // ImageProxy の回転（ImageAnalysisのターゲット回転）を考慮
                            // これは ImageAnalysis のターゲット回転を設定していない場合、通常は 0 で問題ないことが多いですが、
                            // 厳密には ImageProxy.getImageInfo().getRotationDegrees() の考慮が必要です。
                            // 今回は単純な RectToRect で試します。
                            
                            // --- ★ 座標変換 Matrix 作成 完了 ★ ---
                            
                            List<OverlayView.OverlayBox> boxes = new ArrayList<>();
                            
                            // OverlayView の setScale はここでは不要（Matrixで座標変換するため）
                            // overlayView.setScale(1f, 1f); // もしあれば 1.0 に戻すか、この行を無視する
                    
                            for (DetectorHelper.SimpleDetection d : results) {
                                
                                // 検出結果の RectF (モデル座標系) をコピー
                                RectF originalBBox = d.bbox; 
                                RectF transformedBBox = new RectF(originalBBox); 
                                
                                // Matrixを使って座標をPreviewViewピクセル座標に変換
                                matrix.mapRect(transformedBBox); // 変換を適用！
                    
                                int color = 0xFFFF0000; // 赤
                                
                                // 変換済みの座標を渡す
                                boxes.add(new OverlayView.OverlayBox(transformedBBox, d.label, d.score, color));
                            }
                            
                            // Matrixで変換したピクセル座標を渡すため、OverlayViewのスケールは 1.0 に戻す
                            // (念のため、OverlayView の setScale を 1.0 に設定するか、削除してください)
                            overlayView.setScale(1f, 1f);
                            overlayView.setBoxes(boxes);
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
