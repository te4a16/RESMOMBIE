package org.example;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * カメラ映像をアプリ内に表示する Fragment
 * -----------------------------------------
 * ・XML の miniPreview に映像を表示
 * ・CameraX の初期化
 * ・PIP モードのときの UI 調整
 * -----------------------------------------
 */
public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";

    private PreviewView miniPreview; // カメラ映像を出すビュー
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture; // CameraX provider

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // fragment_camera.xml を読み込む
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // XML 内の PreviewView を取得
        miniPreview = view.findViewById(R.id.pipPreview);

        // CameraX の Provider を取得
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                // CameraX provider が取れたらカメラ開始
                ProcessCameraProvider provider = cameraProviderFuture.get();

                // 別ファイルに移したカメラ起動コード
                CameraController.startCamera(
                        miniPreview,
                        provider,
                        this  // Fragment のライフサイクルに合わせる
                );

            } catch (Exception e) {
                Log.e(TAG, "Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /** PIP になったら常に miniPreview は visible にしておく */
    public void onEnterPipMode() {
        if (miniPreview != null) miniPreview.setVisibility(View.VISIBLE);
    }

    /** PIP から戻っても同じく可視状態にする */
    public void onExitPipMode() {
        if (miniPreview != null) miniPreview.setVisibility(View.VISIBLE);
    }
}
