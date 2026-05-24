package com.imagezeta.animator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.InputStream;
import java.util.ArrayDeque;

public class EditorCanvasView extends View {

    private Bitmap originalBitmap;
    private Bitmap workBitmap;
    private Canvas workCanvas;

    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint erasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean wandEnabled = false;
    private boolean checkerBackground = true;

    private float scale = 1f;
    private float minScale = 1f;
    private float maxScale = 8f;

    private float offsetX = 0f;
    private float offsetY = 0f;

    private float lastX = 0f;
    private float lastY = 0f;

    private float downX = 0f;
    private float downY = 0f;

    private boolean isDragging = false;
    private boolean isScaling = false;
    private boolean hasMovedWhileWand = false;

    private int tolerance = 28;
    private float eraserSizeScreen = 42f;

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

        erasePaint.setAntiAlias(true);
        erasePaint.setStyle(Paint.Style.STROKE);
        erasePaint.setStrokeCap(Paint.Cap.ROUND);
        erasePaint.setStrokeJoin(Paint.Join.ROUND);
        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        erasePaint.setMaskFilter(new BlurMaskFilter(1.2f, BlurMaskFilter.Blur.NORMAL));

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isScaling = true;
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
                isScaling = false;
            }
        });
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null) return;

        Bitmap fixed = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        originalBitmap = fixed.copy(Bitmap.Config.ARGB_8888, true);
        workBitmap = fixed.copy(Bitmap.Config.ARGB_8888, true);
        workCanvas = new Canvas(workBitmap);

        post(new Runnable() {
            @Override
            public void run() {
                fitImageToScreen();
                invalidate();
            }
        });
    }

    public void setImage(Bitmap bitmap) {
        setBitmap(bitmap);
    }

    public void setImageBitmap(Bitmap bitmap) {
        setBitmap(bitmap);
    }

    public void loadImageFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap != null) {
                setBitmap(bitmap);
            }
        } catch (Exception ignored) {
        }
    }

    public Bitmap getBitmap() {
        return workBitmap;
    }

    public Bitmap getEditedBitmap() {
        return workBitmap;
    }

    public Bitmap exportBitmap() {
        return workBitmap;
    }

    public void resetImage() {
        if (originalBitmap == null) return;

        workBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        workCanvas = new Canvas(workBitmap);
        invalidate();
    }

    public void reset() {
        resetImage();
    }

    public void toggleWand() {
        wandEnabled = !wandEnabled;
        invalidate();
    }

    public void toggleMagicWand() {
        toggleWand();
    }

    public void setWandEnabled(boolean enabled) {
        wandEnabled = enabled;
        invalidate();
    }

    public void setMagicWandEnabled(boolean enabled) {
        setWandEnabled(enabled);
    }

    public boolean isWandEnabled() {
        return wandEnabled;
    }

    public boolean isMagicWandEnabled() {
        return wandEnabled;
    }

    public void toggleCheckerBackground() {
        checkerBackground = !checkerBackground;
        invalidate();
    }

    public void setCheckerBackground(boolean enabled) {
        checkerBackground = enabled;
        invalidate();
    }

    public void setCheckerBackgroundEnabled(boolean enabled) {
        setCheckerBackground(enabled);
    }

    public boolean isCheckerBackgroundEnabled() {
        return checkerBackground;
    }

    public void setTolerance(int value) {
        if (value < 5) value = 5;
        if (value > 100) value = 100;
        tolerance = value;
    }

    public int getTolerance() {
        return tolerance;
    }

    public void setEraserSize(float size) {
        if (size < 10f) size = 10f;
        if (size > 160f) size = 160f;
        eraserSizeScreen = size;
    }

    public float getEraserSize() {
        return eraserSizeScreen;
    }

    public void zoomIn() {
        scale *= 1.2f;
        if (scale > maxScale) scale = maxScale;
        limitPosition();
        invalidate();
    }

    public void zoomOut() {
        scale /= 1.2f;
        if (scale < minScale) scale = minScale;
        limitPosition();
        invalidate();
    }

    public void fitImageToScreen() {
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
            canvas.restore();
        } else {
            bgPaint.setColor(Color.WHITE);
            bgPaint.setTextSize(42f);
            bgPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Abre una imagen", getWidth() / 2f, getHeight() / 2f, bgPaint);
        }
    }

    private void drawBackground(Canvas canvas) {
        if (!checkerBackground) {
            canvas.drawColor(Color.rgb(30, 30, 30));
            return;
        }

        int size = 38;

        for (int y = 0; y < getHeight(); y += size) {
            for (int x = 0; x < getWidth(); x += size) {
                boolean isWhite = ((x / size) + (y / size)) % 2 == 0;
                bgPaint.setColor(isWhite ? Color.WHITE : Color.rgb(32, 32, 32));
                canvas.drawRect(x, y, x + size, y + size, bgPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (workBitmap == null) return true;

        scaleDetector.onTouchEvent(event);

        int action = event.getActionMasked();

        if (event.getPointerCount() > 1) {
            isScaling = true;
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        switch (action) {

            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                lastX = x;
                lastY = y;
                isDragging = true;
                hasMovedWhileWand = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isScaling) return true;

                float dx = x - lastX;
                float dy = y - lastY;

                if (wandEnabled) {
                    float distance = Math.abs(x - downX) + Math.abs(y - downY);

                    if (distance > 8f) {
                        hasMovedWhileWand = true;
                        eraseLineOnBitmap(lastX, lastY, x, y);
                    }

                    lastX = x;
                    lastY = y;
                    invalidate();
                    return true;
                }

                if (isDragging) {
                    offsetX += dx;
                    offsetY += dy;

                    lastX = x;
                    lastY = y;

                    limitPosition();
                    invalidate();
                }

                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (wandEnabled && !isScaling) {
                    float totalMove = Math.abs(x - downX) + Math.abs(y - downY);

                    if (!hasMovedWhileWand && totalMove < 15f) {
                        eraseSimilarArea(x, y);
                    }
                }

                isDragging = false;
                isScaling = false;
                hasMovedWhileWand = false;
                invalidate();
                return true;
        }

        return true;
    }

    private void eraseLineOnBitmap(float screenX1, float screenY1, float screenX2, float screenY2) {
        if (workBitmap == null || workCanvas == null) return;

        float x1 = screenToBitmapX(screenX1);
        float y1 = screenToBitmapY(screenY1);
        float x2 = screenToBitmapX(screenX2);
        float y2 = screenToBitmapY(screenY2);

        if (!isInsideBitmap(x1, y1) && !isInsideBitmap(x2, y2)) return;

        float stroke = eraserSizeScreen / scale;
        if (stroke < 4f) stroke = 4f;
        if (stroke > 120f) stroke = 120f;

        erasePaint.setStrokeWidth(stroke);

        workCanvas.drawLine(x1, y1, x2, y2, erasePaint);
        workCanvas.drawCircle(x2, y2, stroke / 2f, erasePaint);
    }

    private void eraseSimilarArea(float screenX, float screenY) {
        if (workBitmap == null) return;

        int startX = (int) screenToBitmapX(screenX);
        int startY = (int) screenToBitmapY(screenY);

        if (startX < 0 || startY < 0 || startX >= workBitmap.getWidth() || startY >= workBitmap.getHeight()) {
            return;
        }

        floodErase(startX, startY);
        softenEdges();
        invalidate();
    }

    private void floodErase(int startX, int startY) {
        int width = workBitmap.getWidth();
        int height = workBitmap.getHeight();

        int targetColor = workBitmap.getPixel(startX, startY);

        if (Color.alpha(targetColor) == 0) return;

        int[] pixels = new int[width * height];
        workBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        boolean[] visited = new boolean[width * height];

        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startX, startY));

        while (!queue.isEmpty()) {
            Point p = queue.removeFirst();

            int x = p.x;
            int y = p.y;

            if (x < 0 || y < 0 || x >= width || y >= height) continue;

            int index = y * width + x;

            if (visited[index]) continue;
            visited[index] = true;

            int currentColor = pixels[index];

            if (!isSimilarColor(targetColor, currentColor)) continue;

            pixels[index] = Color.TRANSPARENT;

            queue.add(new Point(x + 1, y));
            queue.add(new Point(x - 1, y));
            queue.add(new Point(x, y + 1));
            queue.add(new Point(x, y - 1));

            queue.add(new Point(x + 1, y + 1));
            queue.add(new Point(x - 1, y - 1));
            queue.add(new Point(x + 1, y - 1));
            queue.add(new Point(x - 1, y + 1));
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

        return diffR <= tolerance && diffG <= tolerance && diffB <= tolerance;
    }

    private void softenEdges() {
        if (workBitmap == null) return;

        int width = workBitmap.getWidth();
        int height = workBitmap.getHeight();

        Bitmap copy = workBitmap.copy(Bitmap.Config.ARGB_8888, true);

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int color = copy.getPixel(x, y);

                if (Color.alpha(color) == 0) continue;

                boolean nearTransparent =
                        Color.alpha(copy.getPixel(x + 1, y)) == 0 ||
                        Color.alpha(copy.getPixel(x - 1, y)) == 0 ||
                        Color.alpha(copy.getPixel(x, y + 1)) == 0 ||
                        Color.alpha(copy.getPixel(x, y - 1)) == 0 ||
                        Color.alpha(copy.getPixel(x + 1, y + 1)) == 0 ||
                        Color.alpha(copy.getPixel(x - 1, y - 1)) == 0 ||
                        Color.alpha(copy.getPixel(x + 1, y - 1)) == 0 ||
                        Color.alpha(copy.getPixel(x - 1, y + 1)) == 0;

                if (nearTransparent) {
                    int r = Color.red(color);
                    int g = Color.green(color);
                    int b = Color.blue(color);
                    workBitmap.setPixel(x, y, Color.argb(150, r, g, b));
                }
            }
        }

        workCanvas = new Canvas(workBitmap);
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
