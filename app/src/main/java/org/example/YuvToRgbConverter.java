package org.example;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ExperimentalGetImage;
import androidx.annotation.OptIn;


import java.nio.ByteBuffer;

public class YuvToRgbConverter {
    private static final String TAG = "YuvToRgbConverter";

    // ここでは簡易的に ImageProxy を Bitmap に変換する方法を記載。
    // 高速かつ正確な方法は RenderScript / ScriptIntrinsicYuvToRGB (deprecated)、
    // もしくは OpenGL を使った変換ですが、簡易実装を示します。

    @OptIn(markerClass = ExperimentalGetImage.class)
    public static Bitmap imageProxyToBitmap(Context context, @NonNull ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) return null;

        if (image.getFormat() == ImageFormat.YUV_420_888) {
            // 最も単純な方法は ImageProxy を Bitmap に変換する既存ユーティリティを使うこと。
            // ここでは AndroidX Camera 用の公式 Convert を使わずに、簡潔な方法を提示。
            try {
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer yBuffer = planes[0].getBuffer();
                ByteBuffer uBuffer = planes[1].getBuffer();
                ByteBuffer vBuffer = planes[2].getBuffer();

                int ySize = yBuffer.remaining();
                int uSize = uBuffer.remaining();
                int vSize = vBuffer.remaining();

                byte[] nv21 = new byte[ySize + uSize + vSize];

                yBuffer.get(nv21, 0, ySize);
                vBuffer.get(nv21, ySize, vSize); // V
                uBuffer.get(nv21, ySize + vSize, uSize); // U

                // NV21 -> Bitmap へ変換（ここでは API の BitmapFactory を使わないため、
                // 実際には YuvImage + compressToJpeg を使うのが簡単）
                android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                        nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()), 90, out);
                byte[] imageBytes = out.toByteArray();
                Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                return bmp;
            } catch (Exception e) {
                Log.e(TAG, "yuv->rgb failed", e);
                return null;
            }
        } else {
            Log.e(TAG, "Unsupported image format: " + image.getFormat());
            return null;
        }
    }
}
