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
import android.view.View;

import java.util.ArrayList;

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

    private final Path lassoPath = new Path();

    private float scale = 1f;
    private float minScale = 0.2f;
    private float maxScale = 10f;
    private float rotation = 0f;

    private float offsetX = 0f;
    private float offsetY = 0f;

    private float lastX = 0f;
    private float lastY = 0f;
    private float downX = 0f;
    private float downY = 0f;

    private boolean dragging = false;
    private boolean multiTouch = false;

    private float startDistance = 0f;
    private float startAngle = 0f;
    private float startScale = 1f;
    private float startRotation = 0f;
    private float focusBitmapX = 0f;
    private float focusBitmapY = 0f;

    private int magicTolerance = 35;
    private int edgeSoftness = 2;

    private final ArrayList<Bitmap> undoStack = new ArrayList<>();
    private final ArrayList<Bitmap> redoStack = new ArrayList<>();
    private static final int MAX_HISTORY = 12;

    public EditorCanvasView(Context context) {
        super(context);
        init();
    }

    public EditorCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditorCanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        lassoPaint.setColor(Color.CYAN);
        lassoPaint.setStyle(Paint.Style.STROKE);
        lassoPaint.setStrokeWidth(4f);
        lassoPaint.setStrokeCap(Paint.Cap.ROUND);
        lassoPaint.setStrokeJoin(Paint.Join.ROUND);

        lassoFillPaint.setColor(Color.argb(55, 0, 255, 255));
        lassoFillPaint.setStyle(Paint.Style.FILL);
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null) return;

        Bitmap fixed = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        originalBitmap = fixed.copy(Bitmap.Config.ARGB_8888, true);
        workBitmap = fixed.copy(Bitmap.Config.ARGB_8888, true);
        workCanvas = new Canvas(workBitmap);

        undoStack.clear();
        redoStack.clear();
        clearLasso();

        scale = 1f;
        rotation = 0f;
        offsetX = 0f;
        offsetY = 0f;

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

        saveState();

        workBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        workCanvas = new Canvas(workBitmap);

        clearLasso();

        rotation = 0f;
        fitImageToScreen();
        invalidate();
    }

    public void undo() {
        if (undoStack.isEmpty() || workBitmap == null) return;

        redoStack.add(workBitmap.copy(Bitmap.Config.ARGB_8888, true));

        Bitmap previous = undoStack.remove(undoStack.size() - 1);
        workBitmap = previous.copy(Bitmap.Config.ARGB_8888, true);
        workCanvas = new Canvas(workBitmap);

        clearLasso();
        invalidate();
    }

    public void redo() {
        if (redoStack.isEmpty() || workBitmap == null) return;

        undoStack.add(workBitmap.copy(Bitmap.Config.ARGB_8888, true));

        Bitmap next = redoStack.remove(redoStack.size() - 1);
        workBitmap = next.copy(Bitmap.Config.ARGB_8888, true);
        workCanvas = new Canvas(workBitmap);

        clearLasso();
        invalidate();
    }

    private void saveState() {
        if (workBitmap == null) return;

        undoStack.add(workBitmap.copy(Bitmap.Config.ARGB_8888, true));

        if (undoStack.size() > MAX_HISTORY) {
            Bitmap old = undoStack.remove(0);
            if (old != null && !old.isRecycled()) {
                old.recycle();
            }
        }

        redoStack.clear();
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
        if (tolerance > 150) tolerance = 150;

        magicTolerance = tolerance;
    }

    public void setEdgeSoftness(int softness) {
        if (softness < 0) softness = 0;
        if (softness > 20) softness = 20;

        edgeSoftness = softness;
    }

    private void fitImageToScreen() {
        if (workBitmap == null || getWidth() == 0 || getHeight() == 0) return;

        float viewW = getWidth();
        float viewH = getHeight();

        float imgW = workBitmap.getWidth();
        float imgH = workBitmap.getHeight();

        scale = Math.min(viewW / imgW, viewH / imgH);
        minScale = scale * 0.5f;

        offsetX = (viewW - imgW * scale) / 2f;
        offsetY = (viewH - imgH * scale) / 2f;
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
            canvas.rotate(rotation);
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
            canvas.drawText("Pulsa Abrir", getWidth() / 2f, getHeight() / 2f, bgPaint);
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

    private void drawProcessing(Canvas canvas) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setColor(Color.argb(170, 0, 0, 0));
        canvas.drawRect(0, 0, getWidth(), getHeight(), p);

        p.setColor(Color.WHITE);
        p.setTextSize(34f);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Procesando varita...", getWidth() / 2f, getHeight() / 2f, p);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (workBitmap == null || processingMagic) return true;

        int action = event.getActionMasked();

        if (event.getPointerCount() >= 2) {
            handleMultiTouch(event, action);
            return true;
        }

        if (multiTouch && event.getPointerCount() < 2) {
            multiTouch = false;
            dragging = false;
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
                    float[] p = screenToBitmapPoint(sx, sy);

                    if (isInsideBitmap(p[0], p[1])) {
                        lassoPath.reset();
                        lassoPath.moveTo(p[0], p[1]);
                        lassoReady = true;
                    }
                }

                return true;

            case MotionEvent.ACTION_MOVE:
                if (lassoEnabled) {
                    float[] p = screenToBitmapPoint(sx, sy);

                    if (isInsideBitmap(p[0], p[1])) {
                        lassoPath.lineTo(p[0], p[1]);
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

                    invalidate();
                }

                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (lassoEnabled && lassoReady) {
                    lassoPath.close();
                }

                if (wandEnabled) {
                    float move = Math.abs(sx - downX) + Math.abs(sy - downY);

                    if (move < 25f) {
                        applyMagicWand(sx, sy);
                    }
                }

                dragging = false;
                invalidate();
                return true;
        }

        return true;
    }

    private void handleMultiTouch(MotionEvent event, int action) {
        if (event.getPointerCount() < 2) return;

        if (action == MotionEvent.ACTION_POINTER_DOWN || !multiTouch) {
            multiTouch = true;

            startDistance = getDistance(event);
            startAngle = getAngle(event);
            startScale = scale;
            startRotation = rotation;

            float focusX = (event.getX(0) + event.getX(1)) / 2f;
            float focusY = (event.getY(0) + event.getY(1)) / 2f;

            float[] p = screenToBitmapPoint(focusX, focusY);
            focusBitmapX = p[0];
            focusBitmapY = p[1];

            return;
        }

        if (action == MotionEvent.ACTION_MOVE && multiTouch) {
            float currentDistance = getDistance(event);

            if (startDistance > 10f) {
                scale = startScale * (currentDistance / startDistance);
            }

            if (scale < minScale) scale = minScale;
            if (scale > maxScale) scale = maxScale;

            float currentAngle = getAngle(event);
            rotation = startRotation + (currentAngle - startAngle);

            float focusX = (event.getX(0) + event.getX(1)) / 2f;
            float focusY = (event.getY(0) + event.getY(1)) / 2f;

            float[] transformed = bitmapToScreenWithoutOffset(focusBitmapX, focusBitmapY);

            offsetX = focusX - transformed[0];
            offsetY = focusY - transformed[1];

            invalidate();
        }
    }

    private float getDistance(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);

        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float getAngle(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);

        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    private float[] bitmapToScreenWithoutOffset(float bx, float by) {
        double rad = Math.toRadians(rotation);

        float sx = bx * scale;
        float sy = by * scale;

        float rx = (float) (sx * Math.cos(rad) - sy * Math.sin(rad));
        float ry = (float) (sx * Math.sin(rad) + sy * Math.cos(rad));

        return new float[]{rx, ry};
    }

    private float[] screenToBitmapPoint(float screenX, float screenY) {
        float x = screenX - offsetX;
        float y = screenY - offsetY;

        double rad = Math.toRadians(-rotation);

        float rx = (float) (x * Math.cos(rad) - y * Math.sin(rad));
        float ry = (float) (x * Math.sin(rad) + y * Math.cos(rad));

        rx = rx / scale;
        ry = ry / scale;

        return new float[]{rx, ry};
    }

    private void applyMagicWand(float screenX, float screenY) {
        if (workBitmap == null) return;

        float[] p = screenToBitmapPoint(screenX, screenY);

        final int startX = (int) p[0];
        final int startY = (int) p[1];

        if (startX < 0 || startY < 0 || startX >= workBitmap.getWidth() || startY >= workBitmap.getHeight()) {
            return;
        }

        saveState();

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
        if (radius > 6) radius = 6;

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

        saveState();

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

    private boolean isInsideBitmap(float x, float y) {
        if (workBitmap == null) return false;

        return x >= 0 &&
                y >= 0 &&
                x < workBitmap.getWidth() &&
                y < workBitmap.getHeight();
    }
}
