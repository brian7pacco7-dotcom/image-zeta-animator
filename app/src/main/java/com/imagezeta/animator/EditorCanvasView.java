package com.imagezeta.animator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
    private final Paint lassoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lassoFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean wandEnabled = false;
    private boolean lassoEnabled = false;
    private boolean checkerBackground = true;
    private boolean lassoReady = false;
    private boolean processingMagic = false;

    private Path lassoPath = new Path();

    private float scale = 1f;
    private float minScale = 1f;
    private float maxScale = 8f;

    private float offsetX = 0f;
    private float offsetY = 0f;

    private float lastX = 0f;
    private float lastY = 0f;
    private float downX = 0f;
    private float downY = 0f;

    private boolean dragging = false;
    private boolean scaling = false;

    private int magicTolerance = 35;
    private int edgeSoftness = 2;

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

        lassoPaint.setColor(Color.CYAN);
        lassoPaint.setStyle(Paint.Style.STROKE);
        lassoPaint.setStrokeWidth(4f);
        lassoPaint.setStrokeCap(Paint.Cap.ROUND);
        lassoPaint.setStrokeJoin(Paint.Join.ROUND);

        lassoFillPaint.setColor(Color.argb(60, 0, 255, 255));
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
        fitImageToScreen();
        invalidate();
    }

    public void rotateRight() {
        if (workBitmap == null || originalBitmap == null) return;

        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        workBitmap = Bitmap.createBitmap(
                workBitmap,
                0, 0,
                workBitmap.getWidth(),
                workBitmap.getHeight(),
                matrix,
                true
        );

        originalBitmap = Bitmap.createBitmap(
                originalBitmap,
                0, 0,
                originalBitmap.getWidth(),
                originalBitmap.getHeight(),
                matrix,
                true
        );

        workCanvas = new Canvas(workBitmap);
        clearLasso();
        fitImageToScreen();
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

    public void setMagicTolerance(int tolerance) {
        if (tolerance < 5) tolerance = 5;
        if (tolerance > 100) tolerance = 100;
        magicTolerance = tolerance;
    }

    public void setEdgeSoftness(int softness) {
        if (softness < 0) softness = 0;
        if (softness > 10) softness = 10;
        edgeSoftness = softness;
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

            if (processingMagic) {
                drawProcessing(canvas);
            }
        } else {
            bgPaint.setColor(Color.WHITE);
            bgPaint.setTextSize(38f);
            bgPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Pulsa ABRIR", getWidth() / 2f, getHeight() / 2f, bgPaint);
        }
    }

    private void drawProcessing(Canvas canvas) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.argb(170, 0, 0, 0));
        canvas.drawRect(0, 0, getWidth(), getHeight(), p);

        p.setColor(Color.WHITE);
        p.setTextSize(34f);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Procesando varita...", getWidth() / 2f, getHeight() / 2f, p);
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
        if (workBitmap == null || processingMagic) return true;

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
                downX = sx;
                downY = sy;
                lastX = sx;
                lastY = sy;
                dragging = true;

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

                if (!wandEnabled && dragging) {
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

                if (wandEnabled && !scaling) {
                    float move = Math.abs(sx - downX) + Math.abs(sy - downY);

                    if (move < 25f) {
                        applyMagicWand(sx, sy);
                    }
                }

                dragging = false;
                scaling = false;
                invalidate();
                return true;
        }

        return true;
    }

    private void applyMagicWand(float screenX, float screenY) {
        if (workBitmap == null) return;

        final int startX = (int) screenToBitmapX(screenX);
        final int startY = (int) screenToBitmapY(screenY);

        if (startX < 0 || startY < 0 || startX >= workBitmap.getWidth() || startY >= workBitmap.getHeight()) {
            return;
        }

        processingMagic = true;
        invalidate();

        new Thread(new Runnable() {
            @Override
            public void run() {
                magicErasePixels(startX, startY);
                processingMagic = false;
                postInvalidate();
            }
        }).start();
    }

    private void magicErasePixels(int startX, int startY) {
        if (workBitmap == null) return;

        int width = workBitmap.getWidth();
        int height = workBitmap.getHeight();
        int total = width * height;

        int[] pixels = new int[total];
        workBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int startIndex = startY * width + startX;
        int targetColor = pixels[startIndex];

        if (Color.alpha(targetColor) == 0) return;

        int[] maskPixels = null;

        if (lassoReady) {
            Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas maskCanvas = new Canvas(mask);

            Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            maskPaint.setColor(Color.WHITE);
            maskPaint.setStyle(Paint.Style.FILL);

            maskCanvas.drawPath(lassoPath, maskPaint);

            maskPixels = new int[total];
            mask.getPixels(maskPixels, 0, width, 0, 0, width, height);

            if (Color.alpha(maskPixels[startIndex]) == 0) {
                mask.recycle();
                return;
            }

            mask.recycle();
        }

        boolean[] visited = new boolean[total];
        boolean[] erased = new boolean[total];

        int[] queue = new int[total];
        int head = 0;
        int tail = 0;

        queue[tail++] = startIndex;
        visited[startIndex] = true;

        while (head < tail) {
            int index = queue[head++];

            if (maskPixels != null && Color.alpha(maskPixels[index]) == 0) {
                continue;
            }

            int currentColor = pixels[index];

            if (!isSimilarColor(targetColor, currentColor)) {
                continue;
            }

            pixels[index] = Color.TRANSPARENT;
            erased[index] = true;

            int x = index % width;
            int y = index / width;

            int n;

            if (x > 0) {
                n = index - 1;
                if (!visited[n]) {
                    visited[n] = true;
                    queue[tail++] = n;
                }
            }

            if (x < width - 1) {
                n = index + 1;
                if (!visited[n]) {
                    visited[n] = true;
                    queue[tail++] = n;
                }
            }

            if (y > 0) {
                n = index - width;
                if (!visited[n]) {
                    visited[n] = true;
                    queue[tail++] = n;
                }
            }

            if (y < height - 1) {
                n = index + width;
                if (!visited[n]) {
                    visited[n] = true;
                    queue[tail++] = n;
                }
            }
        }

        if (edgeSoftness > 0) {
            softenMagicEdges(pixels, erased, width, height);
        }

        workBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        workCanvas = new Canvas(workBitmap);
    }

    private boolean isSimilarColor(int target, int current) {
        if (Color.alpha(current) == 0) return false;

        int r1 = Color.red(target);
        int g1 = Color.green(target);
        int b1 = Color.blue(target);

        int r2 = Color.red(current);
        int g2 = Color.green(current);
        int b2 = Color.blue(current);

        int diffR = Math.abs(r1 - r2);
        int diffG = Math.abs(g1 - g2);
        int diffB = Math.abs(b1 - b2);

        return diffR <= magicTolerance &&
               diffG <= magicTolerance &&
               diffB <= magicTolerance;
    }

    private void softenMagicEdges(int[] pixels, boolean[] erased, int width, int height) {
        int radius = edgeSoftness;
        if (radius < 1) radius = 1;
        if (radius > 5) radius = 5;

        int[] copy = pixels.clone();

        for (int y = radius; y < height - radius; y++) {
            for (int x = radius; x < width - radius; x++) {
                int index = y * width + x;

                if (Color.alpha(copy[index]) == 0) continue;

                boolean nearErased = false;

                for (int dy = -radius; dy <= radius && !nearErased; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int ni = (y + dy) * width + (x + dx);
                        if (erased[ni]) {
                            nearErased = true;
                            break;
                        }
                    }
                }

                if (nearErased) {
                    int c = copy[index];
                    int r = Color.red(c);
                    int g = Color.green(c);
                    int b = Color.blue(c);
                    pixels[index] = Color.argb(120, r, g, b);
                }
            }
        }
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
