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
import android.graphics.Matrix; // Matrix をインポート

import java.nio.ByteBuffer;

public class YuvToRgbConverter {
    private static final String TAG = "YuvToRgbConverter";

    @OptIn(markerClass = ExperimentalGetImage.class)
    // ★ 変更点: rotationDegrees を引数に追加 ★
    public static Bitmap imageProxyToBitmap(Context context, @NonNull ImageProxy imageProxy, int rotationDegrees) {
        Image image = imageProxy.getImage();
        if (image == null) return null;

        if (image.getFormat() != ImageFormat.YUV_420_888) {
            Log.e(TAG, "Unsupported image format: " + image.getFormat());
            return null;
        }

        try {
            // YUV_420_888 -> NV21 への変換
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            yBuffer.get(nv21, 0, ySize);
            // NV21はVUVUの順であるため、Vプレーンの後にUプレーンをコピーする
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            // NV21 -> Bitmap
            android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                    nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()), 90, out);
            byte[] imageBytes = out.toByteArray();
            Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            // ★ 変更点: 回転補正の適用 ★
            if (bmp != null && rotationDegrees != 0 && rotationDegrees != 360) {
                Matrix matrix = new Matrix();
                // Bitmapの中心を回転軸とする (回転補正後のモデル入力として使用するため)
                matrix.postRotate(rotationDegrees);

                // 回転後の新しいBitmapを生成
                Bitmap rotatedBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                bmp.recycle(); // 元のBitmapは解放

                Log.d(TAG, "Bitmap Rotated by: " + rotationDegrees + " degrees. Size: " + rotatedBmp.getWidth() + "x" + rotatedBmp.getHeight());
                return rotatedBmp;
            }
            return bmp;
        } catch (Exception e) {
            Log.e(TAG, "yuv->rgb failed", e);
            return null;
        }
    }
}