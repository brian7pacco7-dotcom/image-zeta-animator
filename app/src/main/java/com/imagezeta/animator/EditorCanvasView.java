package com.imagezeta.animator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class EditorCanvasView extends View {

    private Bitmap originalBitmap;
    private Bitmap workBitmap;
    private Canvas workCanvas;

    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint erasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lassoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lassoFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean wandEnabled = false;
    private boolean lassoEnabled = false;
    private boolean checkerBackground = true;

    private boolean lassoReady = false;
    private Path lassoPath = new Path();

    private float scale = 1f;
    private float minScale = 1f;
    private float maxScale = 8f;

    private float offsetX = 0f;
    private float offsetY = 0f;

    private float lastX = 0f;
    private float lastY = 0f;

    private boolean dragging = false;
    private boolean scaling = false;
    private boolean erasedOnMove = false;

    private float eraserSize = 45f;
    private int edgeSoftness = 3;

    private ScaleGestureDetector scaleDetector;

    public EditorCanvasView(Context context) {
        super(context);
        init(context);
    }

    public EditorCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EditorCanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        erasePaint.setStyle(Paint.Style.STROKE);
        erasePaint.setStrokeCap(Paint.Cap.ROUND);
        erasePaint.setStrokeJoin(Paint.Join.ROUND);
        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        updateEraserPaint();

        lassoPaint.setColor(Color.CYAN);
        lassoPaint.setStyle(Paint.Style.STROKE);
        lassoPaint.setStrokeWidth(3f);
        lassoPaint.setStrokeCap(Paint.Cap.ROUND);
        lassoPaint.setStrokeJoin(Paint.Join.ROUND);

        lassoFillPaint.setColor(Color.argb(45, 0, 255, 255));
        lassoFillPaint.setStyle(Paint.Style.FILL);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                scaling = true;
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float oldScale = scale;

                scale *= detector.getScaleFactor();

                if (scale < minScale) scale = minScale;
                if (scale > maxScale) scale = maxScale;

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                offsetX = focusX - ((focusX - offsetX) * scale / oldScale);
                offsetY = focusY - ((focusY - offsetY) * scale / oldScale);

                limitPosition();
                invalidate();
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                scaling = false;
            }
        });
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null) return;

        Bitmap fixed = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        originalBitmap = fixed.copy(Bitmap.Config.ARGB_8888, true);
        workBitmap = fixed.copy(Bitmap.Config.ARGB_8888, true);
        workCanvas = new Canvas(workBitmap);

        clearLasso();

        post(new Runnable() {
            @Override
            public void run() {
                fitImageToScreen();
                invalidate();
            }
        });
    }

    public Bitmap getEditedBitmap() {
        return workBitmap;
    }

    public void resetImage() {
        if (originalBitmap == null) return;

        workBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        workCanvas = new Canvas(workBitmap);

        clearLasso();
        invalidate();
    }

    public void toggleWand() {
        wandEnabled = !wandEnabled;

        if (wandEnabled) {
            lassoEnabled = false;
        }

        invalidate();
    }

    public void setWandEnabled(boolean enabled) {
        wandEnabled = enabled;

        if (enabled) {
            lassoEnabled = false;
        }

        invalidate();
    }

    public boolean isWandEnabled() {
        return wandEnabled;
    }

    public void toggleLasso() {
        lassoEnabled = !lassoEnabled;

        if (lassoEnabled) {
            wandEnabled = false;
            clearLasso();
        }

        invalidate();
    }

    public void setLassoEnabled(boolean enabled) {
        lassoEnabled = enabled;

        if (enabled) {
            wandEnabled = false;
            clearLasso();
        }

        invalidate();
    }

    public boolean isLassoEnabled() {
        return lassoEnabled;
    }

    public void toggleCheckerBackground() {
        checkerBackground = !checkerBackground;
        invalidate();
    }

    public void setEraserSize(float size) {
        if (size < 10f) size = 10f;
        if (size > 160f) size = 160f;

        eraserSize = size;
        updateEraserPaint();
        invalidate();
    }

    public void setEdgeSoftness(int softness) {
        if (softness < 0) softness = 0;
        if (softness > 20) softness = 20;

        edgeSoftness = softness;
        updateEraserPaint();
        invalidate();
    }

    private void updateEraserPaint() {
        erasePaint.setStrokeWidth(eraserSize / Math.max(scale, 0.1f));

        if (edgeSoftness <= 0) {
            erasePaint.setMaskFilter(null);
        } else {
            erasePaint.setMaskFilter(new BlurMaskFilter(edgeSoftness, BlurMaskFilter.Blur.NORMAL));
        }
    }

    private void fitImageToScreen() {
        if (workBitmap == null || getWidth() == 0 || getHeight() == 0) return;

        float viewW = getWidth();
        float viewH = getHeight();

        float imgW = workBitmap.getWidth();
        float imgH = workBitmap.getHeight();

        scale = Math.min(viewW / imgW, viewH / imgH);
        minScale = scale;

        offsetX = (viewW - imgW * scale) / 2f;
        offsetY = (viewH - imgH * scale) / 2f;

        limitPosition();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        fitImageToScreen();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackground(canvas);

        if (workBitmap != null) {
            canvas.save();
            canvas.translate(offsetX, offsetY);
            canvas.scale(scale, scale);

            canvas.drawBitmap(workBitmap, 0, 0, imagePaint);

            if (lassoReady || lassoEnabled) {
                canvas.drawPath(lassoPath, lassoFillPaint);
                canvas.drawPath(lassoPath, lassoPaint);
            }

            canvas.restore();
        } else {
            bgPaint.setColor(Color.WHITE);
            bgPaint.setTextSize(38f);
            bgPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Pulsa ABRIR", getWidth() / 2f, getHeight() / 2f, bgPaint);
        }
    }

    private void drawBackground(Canvas canvas) {
        if (!checkerBackground) {
            canvas.drawColor(Color.rgb(35, 35, 35));
            return;
        }

        int size = 24;

        for (int y = 0; y < getHeight(); y += size) {
            for (int x = 0; x < getWidth(); x += size) {
                boolean white = ((x / size) + (y / size)) % 2 == 0;
                bgPaint.setColor(white ? Color.WHITE : Color.rgb(55, 55, 55));
                canvas.drawRect(x, y, x + size, y + size, bgPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (workBitmap == null) return true;

        if (!wandEnabled && !lassoEnabled) {
            scaleDetector.onTouchEvent(event);
        }

        int action = event.getActionMasked();

        if (event.getPointerCount() > 1) {
            scaling = true;
            return true;
        }

        float sx = event.getX();
        float sy = event.getY();

        switch (action) {

            case MotionEvent.ACTION_DOWN:
                lastX = sx;
                lastY = sy;
                dragging = true;
                erasedOnMove = false;

                if (lassoEnabled) {
                    float bx = screenToBitmapX(sx);
                    float by = screenToBitmapY(sy);

                    if (isInsideBitmap(bx, by)) {
                        lassoPath.reset();
                        lassoPath.moveTo(bx, by);
                        lassoReady = true;
                    }
                }

                return true;

            case MotionEvent.ACTION_MOVE:
                if (scaling) return true;

                if (lassoEnabled) {
                    float bx = screenToBitmapX(sx);
                    float by = screenToBitmapY(sy);

                    if (isInsideBitmap(bx, by)) {
                        lassoPath.lineTo(bx, by);
                        lassoReady = true;
                    }

                    invalidate();
                    return true;
                }

                if (wandEnabled) {
                    eraseLine(lastX, lastY, sx, sy);
                    erasedOnMove = true;

                    lastX = sx;
                    lastY = sy;

                    invalidate();
                    return true;
                }

                if (dragging) {
                    float dx = sx - lastX;
                    float dy = sy - lastY;

                    offsetX += dx;
                    offsetY += dy;

                    lastX = sx;
                    lastY = sy;

                    limitPosition();
                    invalidate();
                }

                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (lassoEnabled && lassoReady) {
                    lassoPath.close();
                }

                if (wandEnabled && !scaling && !erasedOnMove) {
                    eraseCircle(sx, sy);
                }

                dragging = false;
                scaling = false;
                erasedOnMove = false;

                invalidate();
                return true;
        }

        return true;
    }

    public boolean applyLassoErase() {
        if (workBitmap == null || workCanvas == null || !lassoReady) {
            return false;
        }

        Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setStyle(Paint.Style.FILL);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        if (edgeSoftness > 0) {
            clearPaint.setMaskFilter(new BlurMaskFilter(edgeSoftness, BlurMaskFilter.Blur.NORMAL));
        }

        workCanvas.drawPath(lassoPath, clearPaint);

        clearLasso();
        invalidate();

        return true;
    }

    private void clearLasso() {
        lassoPath.reset();
        lassoReady = false;
    }

    private void eraseLine(float sx1, float sy1, float sx2, float sy2) {
        if (workBitmap == null || workCanvas == null) return;

        float x1 = screenToBitmapX(sx1);
        float y1 = screenToBitmapY(sy1);
        float x2 = screenToBitmapX(sx2);
        float y2 = screenToBitmapY(sy2);

        if (!isInsideBitmap(x1, y1) && !isInsideBitmap(x2, y2)) return;

        updateEraserPaint();

        workCanvas.drawLine(x1, y1, x2, y2, erasePaint);
    }

    private void eraseCircle(float sx, float sy) {
        if (workBitmap == null || workCanvas == null) return;

        float x = screenToBitmapX(sx);
        float y = screenToBitmapY(sy);

        if (!isInsideBitmap(x, y)) return;

        updateEraserPaint();

        Paint circlePaint = new Paint(erasePaint);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        if (edgeSoftness <= 0) {
            circlePaint.setMaskFilter(null);
        } else {
            circlePaint.setMaskFilter(new BlurMaskFilter(edgeSoftness, BlurMaskFilter.Blur.NORMAL));
        }

        float radius = (eraserSize / Math.max(scale, 0.1f)) / 2f;
        workCanvas.drawCircle(x, y, radius, circlePaint);
    }

    private float screenToBitmapX(float screenX) {
        return (screenX - offsetX) / scale;
    }

    private float screenToBitmapY(float screenY) {
        return (screenY - offsetY) / scale;
    }

    private boolean isInsideBitmap(float x, float y) {
        if (workBitmap == null) return false;
        return x >= 0 && y >= 0 && x < workBitmap.getWidth() && y < workBitmap.getHeight();
    }

    private void limitPosition() {
        if (workBitmap == null) return;

        float imageW = workBitmap.getWidth() * scale;
        float imageH = workBitmap.getHeight() * scale;

        if (imageW <= getWidth()) {
            offsetX = (getWidth() - imageW) / 2f;
        } else {
            if (offsetX > 0) offsetX = 0;
            if (offsetX + imageW < getWidth()) offsetX = getWidth() - imageW;
        }

        if (imageH <= getHeight()) {
            offsetY = (getHeight() - imageH) / 2f;
        } else {
            if (offsetY > 0) offsetY = 0;
            if (offsetY + imageH < getHeight()) offsetY = getHeight() - imageH;
        }
    }
            }
