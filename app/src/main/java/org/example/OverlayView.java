package org.example;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {
    private final Paint boxPaint;
    private final Paint textPaint;
    private final List<OverlayBox> boxes = new ArrayList<>();
    private float scaleX = 1f;
    private float scaleY = 1f;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);
        boxPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setTextSize(40f);
        textPaint.setAntiAlias(true);
    }

    public void setScale(float sx, float sy) {
        this.scaleX = sx;
        this.scaleY = sy;
    }

    public void setBoxes(List<OverlayBox> newBoxes) {
        boxes.clear();
        if (newBoxes != null) boxes.addAll(newBoxes);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (OverlayBox b : boxes) {
            boxPaint.setColor(b.color);
            textPaint.setColor(b.color);
            RectF r = new RectF(
                    b.rect.left * scaleX,
                    b.rect.top * scaleY,
                    b.rect.right * scaleX,
                    b.rect.bottom * scaleY
            );
            canvas.drawRect(r, boxPaint);
            canvas.drawText(b.label + String.format(" %.2f", b.score), r.left, r.top - 8f, textPaint);
        }
    }

    public static class OverlayBox {
        public final RectF rect;
        public final String label;
        public final float score;
        public final int color;

        public OverlayBox(RectF rect, String label, float score, int color) {
            this.rect = rect;
            this.label = label;
            this.score = score;
            this.color = color;
        }
    }
}
