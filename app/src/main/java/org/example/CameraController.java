package org.example;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
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

/**
 * CameraX を使ってメイン画面用プレビューと
 * PIP（または小窓表示）用プレビューの両方を同時に起動するクラス。
 *
 * また Camera2 API を使って「焦点距離が最も短い背面カメラ（＝超広角）」を
 * 自動で選ぶようにしている。
 */
@OptIn(markerClass = ExperimentalCamera2Interop.class)
public class CameraController {

    private static final String TAG = "CameraController";

    /**
     * **カメラを開始して、2つの PreviewView に同時出力する**
     *
     * @param previewView  メイン画面（大画面）側の PreviewView
     * @param pipPreview   小窓 or PIP 用の PreviewView
     * @param provider     CameraX の CameraProvider
     * @param owner        ライフサイクル所有者（Activity / Fragment）
     */
    public static void startCamera(
            @NonNull PreviewView previewView,
            @NonNull PreviewView pipPreview,
            @NonNull ProcessCameraProvider provider,
            @NonNull LifecycleOwner owner
    ) {
        Context context = previewView.getContext();

        // Camera2 API を使い、背面カメラの中で「最短焦点距離＝超広角」を探す
        String ultraWideId = findUltraWideCameraId(context);
        Log.d(TAG, "Detected UltraWide = " + ultraWideId);

        // CameraX の CameraSelector を Camera2 の cameraId に合わせて作成
        CameraSelector selector = buildSelectorForCameraId(ultraWideId);

        // 2つの Preview（メイン用 + 小窓／PIP用）
        Preview previewMain = new Preview.Builder().build();
        Preview previewPip = new Preview.Builder().build();

        // Preview をそれぞれの SurfaceProvider に接続
        previewMain.setSurfaceProvider(previewView.getSurfaceProvider());
        previewPip.setSurfaceProvider(pipPreview.getSurfaceProvider());

        // 既存バインド解除 → 新しい2つの Preview を bind
        provider.unbindAll();
        try {
            provider.bindToLifecycle(owner, selector, previewMain, previewPip);
        } catch (Exception e) {
            Log.e(TAG, "bind error", e);
        }
    }

    /**
     * **Camera2 API を使い、最も焦点距離の短い背面カメラ（＝超広角）を探す**
     *
     * 焦点距離(focalLength) が短いほど画角が広くなるため
     * AQUOS など複数レンズ搭載のスマホでは超広角の判定に使える。
     *
     * @return 見つかったカメラID（null の場合は標準カメラを使う）
     */
    private static String findUltraWideCameraId(Context context) {
        Log.d(TAG, "findUltraWideCameraId() called");
        try {
            CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

            String bestId = null;
            float shortest = Float.MAX_VALUE;

            for (String id : cm.getCameraIdList()) {

    Log.d(TAG, "LoopCheck: checking cameraId = " + id);

    CameraCharacteristics ch = cm.getCameraCharacteristics(id);

    // 背面カメラのみ対象
    Integer facing = ch.get(CameraCharacteristics.LENS_FACING);
    Log.d(TAG, "LoopCheck:   facing = " + facing);

    if (facing == null || facing != CameraCharacteristics.LENS_FACING_BACK) {
        Log.d(TAG, "LoopCheck:   → skip (not back camera)");
        continue;
    }

    // 焦点距離の配列を取得
    float[] focals = ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
    Log.d(TAG, "LoopCheck:   focal lengths = " + Arrays.toString(focals));

    if (focals == null || focals.length == 0) {
        Log.d(TAG, "LoopCheck:   → skip (no focal lengths)");
        continue;
    }

    float f = focals[0];
    Log.d(TAG, "LoopCheck:   → candidate focal = " + f);

    if (f < shortest) {
        shortest = f;
        bestId = id;
        Log.d(TAG, "LoopCheck:   → update bestId = " + bestId);
    }
}



            Log.d(TAG, "UltraWideCandidate = " + bestId + " f=" + shortest);
            return bestId;

        } catch (Exception e) {
            Log.e(TAG, "find error", e);
            return null;
        }
    }

    /**
     * **Camera2 の cameraId を指定して CameraX の CameraSelector を作成する**
     *
     * CameraX は直接 cameraId を選べないため
     * CameraFilter を使って CameraInfo から特定のIDだけ抽出する。
     */
    private static CameraSelector buildSelectorForCameraId(String targetId) {

        // 超広角が見つからなければ通常の背面カメラ
        if (targetId == null) {
            return new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();
        }

        // CameraFilter を使って CameraX の CameraInfo から
        //   「Camera2CameraInfo の cameraId が一致するものだけ」選ぶ
        return new CameraSelector.Builder()
                .addCameraFilter(cameraInfos -> {
                    List<CameraInfo> result = new ArrayList<>();

                    for (CameraInfo info : cameraInfos) {
                        String id = Camera2CameraInfo.from(info).getCameraId();
                        if (id.equals(targetId)) {
                            result.add(info);
                        }
                    }

                    return result;
                })
                .build();
    }
}
