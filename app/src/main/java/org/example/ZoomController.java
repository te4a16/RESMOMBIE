package org.example;

import android.util.Log;
import android.view.View;
import androidx.camera.core.Camera;
import androidx.camera.core.ZoomState;
import androidx.lifecycle.LiveData;

/**
 * ZoomController
 * ----------------------------------
 * ・CameraX の zoom を管理
 * ・0.7x 相当の「広角風」表示を実現
 * ----------------------------------
 */
public class ZoomController {

    private static final String TAG = "ZoomController";
    private Camera camera;
    private View previewView;

    public ZoomController(View previewView) {
        this.previewView = previewView;
    }

    /** CameraX の Camera を受け取る */
    public void attachCamera(Camera camera) {
        this.camera = camera;
    }

    /** 通常ズームセット */
    public void setZoom(float zoomRatio) {
        if (camera == null) return;
        camera.getCameraControl().setZoomRatio(zoomRatio);
    }

    /**
     * 0.7x 相当の「広角風」表示
     * 実カメラの0.7xにはならないが、見た目を広げる
     */
    public void applyWideAngleMock() {
        if (previewView == null) return;

        // 普段は 1.0 = 等倍 → 0.7倍風にするには少し縮小して表示
        float scale = 0.70f;

        previewView.setScaleX(scale);
        previewView.setScaleY(scale);

        Log.d(TAG, "applyWideAngleMock: preview scaled to " + scale);
    }

    /** 広角エフェクトを解除（通常等倍表示） */
    public void resetScale() {
        if (previewView == null) return;

        previewView.setScaleX(1.0f);
        previewView.setScaleY(1.0f);
    }
}
