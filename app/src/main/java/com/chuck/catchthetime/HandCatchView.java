package com.chuck.catchthetime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public class HandCatchView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ThemeConfig theme;

    public HandCatchView(Context context) {
        super(context);
        theme = ThemeConfig.load(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void refreshTheme() {
        theme = ThemeConfig.load(getContext());
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w * 0.60f;
        float cy = h * 0.33f;
        float r = Math.min(w, h) * 0.205f;

        // A very soft halo gives the mark depth without turning it into an illustration.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(theme.accent);
        paint.setAlpha(30);
        canvas.drawCircle(cx, cy, r * 1.42f, paint);
        paint.setAlpha(255);

        // Clock: hollow ring, three restrained hour marks, two hands.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(3f, w * 0.035f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(theme.accent);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setStrokeWidth(Math.max(2f, w * 0.022f));
        canvas.drawLine(cx, cy, cx, cy - r * 0.49f, paint);
        canvas.drawLine(cx, cy, cx + r * 0.42f, cy + r * 0.22f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(theme.accent);
        canvas.drawCircle(cx, cy - r * 0.72f, Math.max(1.8f, w * 0.018f), paint);
        canvas.drawCircle(cx + r * 0.72f, cy, Math.max(1.8f, w * 0.018f), paint);
        canvas.drawCircle(cx - r * 0.72f, cy, Math.max(1.8f, w * 0.018f), paint);

        // Cupped hand silhouette. One clean form reads better than articulated fingers at icon scale.
        Path hand = new Path();
        hand.moveTo(w * 0.08f, h * 0.73f);
        hand.cubicTo(w * 0.18f, h * 0.62f, w * 0.29f, h * 0.60f, w * 0.39f, h * 0.68f);
        hand.lineTo(w * 0.52f, h * 0.77f);
        hand.cubicTo(w * 0.57f, h * 0.81f, w * 0.64f, h * 0.81f, w * 0.70f, h * 0.77f);
        hand.lineTo(w * 0.86f, h * 0.66f);
        hand.cubicTo(w * 0.92f, h * 0.62f, w * 0.99f, h * 0.68f, w * 0.96f, h * 0.75f);
        hand.cubicTo(w * 0.95f, h * 0.78f, w * 0.92f, h * 0.81f, w * 0.88f, h * 0.84f);
        hand.lineTo(w * 0.69f, h * 0.97f);
        hand.cubicTo(w * 0.64f, h * 1.00f, w * 0.57f, h * 1.01f, w * 0.50f, h * 1.01f);
        hand.lineTo(w * 0.29f, h * 1.01f);
        hand.cubicTo(w * 0.20f, h * 1.01f, w * 0.14f, h * 0.95f, w * 0.09f, h * 0.87f);
        hand.lineTo(w * 0.045f, h * 0.80f);
        hand.cubicTo(w * 0.015f, h * 0.75f, w * 0.035f, h * 0.71f, w * 0.08f, h * 0.73f);
        hand.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(theme.text);
        paint.setShadowLayer(Math.max(2f, w * 0.02f), 0, Math.max(1f, h * 0.012f), 0x33000000);
        canvas.drawPath(hand, paint);
        paint.clearShadowLayer();

        // Tiny catch arc connects the hand and clock without adding visual clutter.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, w * 0.018f));
        paint.setColor(theme.accent);
        paint.setAlpha(190);
        RectF arc = new RectF(cx - r * 1.27f, cy - r * 1.27f, cx + r * 1.27f, cy + r * 1.27f);
        canvas.drawArc(arc, 196, 66, false, paint);
        paint.setAlpha(255);
    }
}
