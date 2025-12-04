package org.example;

import android.content.Context;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;
import org.tensorflow.lite.task.vision.detector.ObjectDetector.ObjectDetectorOptions;
import org.tensorflow.lite.task.vision.detector.Detection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DetectorHelper {

    private ObjectDetector detector;
    private boolean initialized = false;

    // CameraFragment が要求するコンストラクタ
    public DetectorHelper(Context context, String modelName) {

        try {
            ObjectDetectorOptions options =
                    ObjectDetectorOptions.builder()
                            .setMaxResults(5)
                            .setScoreThreshold(0.5f)
                            .build();

            detector = ObjectDetector.createFromFileAndOptions(
                    context,
                    modelName,
                    options
            );

            initialized = true;

        } catch (IOException e) {
            Log.e("DetectorHelper", "モデル読み込み失敗: " + modelName, e);
            initialized = false;
        }
    }

    // CameraFragment が呼ぶ
    public boolean isInitialized() {
        return initialized;
    }

    // CameraFragment が要求する戻り値形式
    public static class SimpleDetection {
        public RectF bbox;
        public String label;
        public float score;

        public SimpleDetection(RectF b, String l, float s) {
            bbox = b;
            label = l;
            score = s;
        }
    }

    // 検出処理
    public List<SimpleDetection> detect(TensorImage image) {

        List<SimpleDetection> output = new ArrayList<>();

        if (!initialized || detector == null) {
            Log.e("DetectorHelper", "Detector not initialized");
            return output;
        }

        List<Detection> results = detector.detect(image);

        for (Detection d : results) {
            RectF box = d.getBoundingBox();

            String label = d.getCategories().get(0).getLabel();
            float score  = d.getCategories().get(0).getScore();

            output.add(new SimpleDetection(box, label, score));
        }

        return output;
    }

    // 任意：デバッグ出力
    public void logResults(List<SimpleDetection> results) {
        if (results == null) return;

        for (SimpleDetection d : results) {
            Log.d("DetectorHelper",
                    "検出: " + d.label +
                            " / Score: " + d.score +
                            " / Box: " + d.bbox);
        }
    }
}
