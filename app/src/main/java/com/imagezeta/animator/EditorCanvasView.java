package com.imagezeta.animator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayDeque;

public class EditorCanvasView extends View {

    public static final int TOOL_MOVE = 0;
    public static final int TOOL_ERASE_SOFT = 1;
    public static final int TOOL_ERASE_HARD = 2;
    public static final int TOOL_RESTORE = 3;
    public static final int TOOL_MAGIC = 4;
    public static final int TOOL_RECT = 5;

    public interface ZoomListener {
        void onZoomChanged(float percent);
    }

    private ZoomListener zoomListener;

    private Bitmap original;
    private Bitmap work;
    private Canvas workCanvas;

    private final Paint imagePaint;
    private final Paint hardErase;
    private final Paint softErase;
    private final Paint restorePaint;
    private final Paint checkerDark;
    private final Paint checkerLight;
    private final Paint rectPaint;

    private final Matrix matrix = new Matrix();
    private final Matrix inverse = new Matrix();

    private final ScaleGestureDetector scaleDetector;

    private final ArrayDeque<Bitmap> undoStack = new ArrayDeque<>();
    private final ArrayDeque<Bitmap> redoStack = new ArrayDeque<>();

    private int tool = TOOL_ERASE_SOFT;
    private int tolerance = 60;
    private int edgeSoftness = 35;
    private boolean holeRecognition = true;

    private float brushSize = 35f;

    private float scale = 1f;
    private float minScale = 1f;
    private final float maxZoomFactor = 80f;
    private float moveX = 0f;
    private float moveY = 0f;
    private float rotationDegrees = 0f;

    private float lastPanX;
    private float lastPanY;
    private float lastFingerAngle;
    private boolean panning = false;

    private float lastX;
    private float lastY;
    private boolean drawing = false;

    private boolean suppressSingleAfterMulti = false;

    private boolean hasRectLimit = false;
    private boolean drawingRect = false;
    private float rectStartX;
    private float rectStartY;
    private final RectF limitRect = new RectF();

    public EditorCanvasView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        checkerDark = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkerDark.setColor(Color.rgb(38, 38, 38));

        checkerLight = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkerLight.setColor(Color.rgb(245, 245, 245));

        hardErase = new Paint(Paint.ANTI_ALIAS_FLAG);
        hardErase.setStyle(Paint.Style.STROKE);
        hardErase.setStrokeCap(Paint.Cap.ROUND);
        hardErase.setStrokeJoin(Paint.Join.ROUND);
        hardErase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        softErase = new Paint(Paint.ANTI_ALIAS_FLAG);
        softErase.setStyle(Paint.Style.STROKE);
        softErase.setStrokeCap(Paint.Cap.ROUND);
        softErase.setStrokeJoin(Paint.Join.ROUND);
        softErase.setAlpha(150);
        softErase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        restorePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        restorePaint.setStyle(Paint.Style.STROKE);
        restorePaint.setStrokeCap(Paint.Cap.ROUND);
        restorePaint.setStrokeJoin(Paint.Join.ROUND);

        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(4f);
        rectPaint.setColor(Color.WHITE);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (work == null) return false;

                suppressSingleAfterMulti = true;
                drawing = false;
                drawingRect = false;

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                float[] fixedBitmapPoint = toBitmapPoint(focusX, focusY);

                float oldScale = scale;
                scale *= detector.getScaleFactor();
                scale = Math.max(minScale, Math.min(scale, minScale * maxZoomFactor));

                if (oldScale != scale) {
                    buildMatrix();
                    float[] fixedScreenPoint = new float[]{fixedBitmapPoint[0], fixedBitmapPoint[1]};
                    matrix.mapPoints(fixedScreenPoint);
                    moveX += focusX - fixedScreenPoint[0];
                    moveY += focusY - fixedScreenPoint[1];
                }

                limitMove();
                notifyZoom();
                invalidate();
                return true;
            }
        });
    }

    public void setZoomListener(ZoomListener listener) {
        zoomListener = listener;
    }

    public boolean hasImage() {
        return work != null;
    }

    public Bitmap getOutputBitmap() {
        return work;
    }

    public void setImage(Bitmap bitmap) {
        original = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        work = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        workCanvas = new Canvas(work);

        restorePaint.setShader(new BitmapShader(original, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

        undoStack.clear();
        redoStack.clear();
        hasRectLimit = false;
        limitRect.setEmpty();

        post(this::resetView);
        invalidate();
    }

    public void setTool(int newTool) {
        tool = newTool;

        if (tool == TOOL_RECT) {
            Toast.makeText(getContext(), "Dibuja un cuadro para limitar la varita.", Toast.LENGTH_SHORT).show();
        }
    }

    public void setBrushSize(int size) {
        brushSize = Math.max(1, size);
    }

    public void setTolerance(int value) {
        tolerance = Math.max(1, Math.min(180, value));
    }

    public void setEdgeSoftness(int value) {
        edgeSoftness = Math.max(0, Math.min(100, value));
        int alpha = 100 + (edgeSoftness * 120 / 100);
        softErase.setAlpha(alpha);
    }

    public void setHoleRecognitionEnabled(boolean enabled) {
        holeRecognition = enabled;
    }

    public void resetView() {
        if (work == null || getWidth() <= 0 || getHeight() <= 0) return;

        float sx = getWidth() / (float) work.getWidth();
        float sy = getHeight() / (float) work.getHeight();

        minScale = Math.min(sx, sy) * 0.92f;
        scale = minScale;
        moveX = 0f;
        moveY = 0f;
        rotationDegrees = 0f;

        notifyZoom();
        invalidate();
    }

    public void clearRectLimit() {
        hasRectLimit = false;
        limitRect.setEmpty();
        invalidate();
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            Toast.makeText(getContext(), "No hay cambios para deshacer.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (work != null) {
            redoStack.addLast(work.copy(Bitmap.Config.ARGB_8888, true));
        }

        work = undoStack.removeLast();
        workCanvas = new Canvas(work);
        restorePaint.setShader(new BitmapShader(original, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        invalidate();
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            Toast.makeText(getContext(), "No hay cambios para rehacer.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (work != null) {
            undoStack.addLast(work.copy(Bitmap.Config.ARGB_8888, true));
        }

        work = redoStack.removeLast();
        workCanvas = new Canvas(work);
        restorePaint.setShader(new BitmapShader(original, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        invalidate();
    }

    private void saveUndo() {
        if (work == null) return;

        if (undoStack.size() >= 12) {
            undoStack.removeFirst();
        }

        undoStack.addLast(work.copy(Bitmap.Config.ARGB_8888, true));
        redoStack.clear();
    }

    private void notifyZoom() {
        if (zoomListener != null && minScale > 0) {
            zoomListener.onZoomChanged((scale / minScale) * 100f);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawTransparentBackground(canvas);

        if (work == null) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE);
            p.setTextSize(sp(18));
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Abre una imagen", getWidth() / 2f, getHeight() / 2f, p);
            return;
        }

        buildMatrix();
        canvas.drawBitmap(work, matrix, imagePaint);
        drawLimitRect(canvas);
    }

    private void drawTransparentBackground(Canvas canvas) {
        int size = Math.max(10, (int) (14 * getResources().getDisplayMetrics().density));

        for (int y = 0; y < getHeight(); y += size) {
            for (int x = 0; x < getWidth(); x += size) {
                boolean alt = ((x / size) + (y / size)) % 2 == 0;
                canvas.drawRect(x, y, x + size, y + size, alt ? checkerLight : checkerDark);
            }
        }
    }

    private void drawLimitRect(Canvas canvas) {
        if (!hasRectLimit || work == null) return;

        RectF screenRect = new RectF(limitRect);
        buildMatrix();
        matrix.mapRect(screenRect);

        canvas.drawRect(screenRect, rectPaint);
    }

    private void buildMatrix() {
        matrix.reset();

        if (work != null) {
            matrix.postTranslate(-work.getWidth() / 2f, -work.getHeight() / 2f);
            matrix.postScale(scale, scale);
            matrix.postRotate(rotationDegrees);
            matrix.postTranslate(getWidth() / 2f + moveX, getHeight() / 2f + moveY);
        }

        matrix.invert(inverse);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (work == null) return true;

        getParent().requestDisallowInterceptTouchEvent(true);
        scaleDetector.onTouchEvent(event);

        if (event.getPointerCount() >= 2) {
            suppressSingleAfterMulti = true;
            drawing = false;
            drawingRect = false;
            handleTwoFinger(event);
            return true;
        }

        int action = event.getActionMasked();

        if (suppressSingleAfterMulti) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                suppressSingleAfterMulti = false;
                panning = false;
            }
            return true;
        }

        if (!scaleDetector.isInProgress()) {
            handleOneFinger(event);
        }

        return true;
    }

    private void handleTwoFinger(MotionEvent event) {
        int action = event.getActionMasked();

        if (event.getPointerCount() < 2) {
            panning = false;
            return;
        }

        float cx = (event.getX(0) + event.getX(1)) / 2f;
        float cy = (event.getY(0) + event.getY(1)) / 2f;
        float angle = getTwoFingerAngle(event);

        if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_DOWN) {
            panning = true;
            lastPanX = cx;
            lastPanY = cy;
            lastFingerAngle = angle;
        } else if (action == MotionEvent.ACTION_MOVE && panning) {
            float[] fixedBitmapPoint = toBitmapPoint(lastPanX, lastPanY);
            float angleDelta = normalizeAngle(angle - lastFingerAngle);

            rotationDegrees = normalizeRotation(rotationDegrees + angleDelta);
            moveX += cx - lastPanX;
            moveY += cy - lastPanY;

            buildMatrix();
            float[] fixedScreenPoint = new float[]{fixedBitmapPoint[0], fixedBitmapPoint[1]};
            matrix.mapPoints(fixedScreenPoint);
            moveX += cx - fixedScreenPoint[0];
            moveY += cy - fixedScreenPoint[1];

            lastPanX = cx;
            lastPanY = cy;
            lastFingerAngle = angle;

            limitMove();
            notifyZoom();
            invalidate();
        } else if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            panning = false;
        }
    }

    private float getTwoFingerAngle(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    private float normalizeAngle(float value) {
        while (value > 180f) value -= 360f;
        while (value < -180f) value += 360f;
        return value;
    }

    private float normalizeRotation(float value) {
        while (value >= 360f) value -= 360f;
        while (value <= -360f) value += 360f;
        return value;
    }

    private void handleOneFinger(MotionEvent event) {
        if (tool == TOOL_MOVE) return;

        float[] point = toBitmapPoint(event.getX(), event.getY());
        float x = point[0];
        float y = point[1];

        if (x < 0 || y < 0 || x >= work.getWidth() || y >= work.getHeight()) {
            return;
        }

        int action = event.getActionMasked();

        if (tool == TOOL_RECT) {
            handleRectTool(action, x, y);
            return;
        }

        if (tool == TOOL_MAGIC) {
            if (action == MotionEvent.ACTION_DOWN) {
                saveUndo();
                magicErase((int) x, (int) y);
            }
            return;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            saveUndo();
            drawing = true;
            lastX = x;
            lastY = y;
            drawStroke(x, y, x + 0.1f, y + 0.1f);
        } else if (action == MotionEvent.ACTION_MOVE && drawing) {
            drawStroke(lastX, lastY, x, y);
            lastX = x;
            lastY = y;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            drawing = false;
        }
    }

    private void handleRectTool(int action, float x, float y) {
        if (action == MotionEvent.ACTION_DOWN) {
            rectStartX = x;
            rectStartY = y;
            drawingRect = true;
            hasRectLimit = true;
            limitRect.set(x, y, x, y);
            invalidate();
        } else if (action == MotionEvent.ACTION_MOVE && drawingRect) {
            limitRect.set(
                    Math.min(rectStartX, x),
                    Math.min(rectStartY, y),
                    Math.max(rectStartX, x),
                    Math.max(rectStartY, y)
            );
            invalidate();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            drawingRect = false;

            if (limitRect.width() < 5 || limitRect.height() < 5) {
                hasRectLimit = false;
                limitRect.setEmpty();
            }

            invalidate();
        }
    }

    private float[] toBitmapPoint(float sx, float sy) {
        buildMatrix();
        float[] p = new float[]{sx, sy};
        inverse.mapPoints(p);
        return p;
    }

    private void drawStroke(float x1, float y1, float x2, float y2) {
        if (workCanvas == null) return;

        float radius = brushSize * (work.getWidth() / 1080f);
        radius = Math.max(1.5f, radius);

        float strokeWidth = radius * 2f;

        if (tool == TOOL_ERASE_HARD) {
            hardErase.setStrokeWidth(strokeWidth);
            workCanvas.drawLine(x1, y1, x2, y2, hardErase);
        } else if (tool == TOOL_ERASE_SOFT) {
            softErase.setStrokeWidth(strokeWidth);
            workCanvas.drawLine(x1, y1, x2, y2, softErase);
        } else if (tool == TOOL_RESTORE) {
            restorePaint.setStrokeWidth(strokeWidth);
            workCanvas.drawLine(x1, y1, x2, y2, restorePaint);
        }

        invalidate();
    }

    private void magicErase(int startX, int startY) {
        int w = work.getWidth();
        int h = work.getHeight();

        if (hasRectLimit && !limitRect.contains(startX, startY)) {
            Toast.makeText(getContext(), "Toca dentro del cuadro.", Toast.LENGTH_SHORT).show();
            return;
        }

        int[] pixels = new int[w * h];
        work.getPixels(pixels, 0, w, 0, 0, w, h);

        int startIndex = startY * w + startX;
        int startColor = pixels[startIndex];

        if (Color.alpha(startColor) <= 8) {
            Toast.makeText(getContext(), "Esa zona ya está transparente.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean[] selected = new boolean[w * h];
        boolean[] seen = new boolean[w * h];
        int[] queue = new int[w * h];

        int head = 0;
        int tail = 0;
        int selectedCount = 0;

        double hardLimit = 12.0 + (tolerance * 1.45);
        double localLimit = 10.0 + (tolerance * 0.90);

        queue[tail++] = startIndex;
        seen[startIndex] = true;

        while (head < tail) {
            int index = queue[head++];
            int x = index % w;
            int y = index / w;

            if (hasRectLimit && !limitRect.contains(x, y)) continue;

            int currentColor = pixels[index];
            if (!isMagicSelectable(currentColor, startColor, hardLimit)) continue;

            selected[index] = true;
            selectedCount++;

            tail = addMagicCandidate(queue, tail, seen, pixels, index, startColor, x + 1, y, w, h, hardLimit, localLimit);
            tail = addMagicCandidate(queue, tail, seen, pixels, index, startColor, x - 1, y, w, h, hardLimit, localLimit);
            tail = addMagicCandidate(queue, tail, seen, pixels, index, startColor, x, y + 1, w, h, hardLimit, localLimit);
            tail = addMagicCandidate(queue, tail, seen, pixels, index, startColor, x, y - 1, w, h, hardLimit, localLimit);

            if (holeRecognition) {
                tail = addMagicCandidate(queue, tail, seen, pixels, index, startColor, x + 1, y + 1, w, h, hardLimit * 0.86, localLimit * 0.82);
                tail = addMagicCandidate(queue, tail, seen, pixels, index, startColor, x - 1, y - 1, w, h, hardLimit * 0.86, localLimit * 0.82);
                tail = addMagicCandidate(queue, tail, seen, pixels, index, startColor, x + 1, y - 1, w, h, hardLimit * 0.86, localLimit * 0.82);
                tail = addMagicCandidate(queue, tail, seen, pixels, index, startColor, x - 1, y + 1, w, h, hardLimit * 0.86, localLimit * 0.82);
            }
        }

        if (selectedCount == 0) {
            Toast.makeText(getContext(), "No se encontró una zona para borrar.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (holeRecognition) {
            closeTinySelectionGaps(selected, pixels, w, h, startColor, hardLimit);
        }

        applyMagicSelection(pixels, selected, w, h);
        work.setPixels(pixels, 0, w, 0, 0, w, h);
        workCanvas = new Canvas(work);
        invalidate();
    }

    private int addMagicCandidate(
            int[] queue,
            int tail,
            boolean[] seen,
            int[] pixels,
            int parentIndex,
            int startColor,
            int x,
            int y,
            int w,
            int h,
            double hardLimit,
            double localLimit
    ) {
        if (x < 0 || y < 0 || x >= w || y >= h) return tail;

        int index = y * w + x;
        if (seen[index]) return tail;
        if (hasRectLimit && !limitRect.contains(x, y)) return tail;

        int candidate = pixels[index];
        if (!isMagicSelectable(candidate, startColor, hardLimit)) return tail;

        double localDiff = colorDistance(candidate, pixels[parentIndex]);
        double startDiff = colorDistance(candidate, startColor);

        if (localDiff <= localLimit || startDiff <= hardLimit * 0.70) {
            seen[index] = true;
            queue[tail++] = index;
        }

        return tail;
    }

    private boolean isMagicSelectable(int color, int startColor, double hardLimit) {
        int alpha = Color.alpha(color);
        if (alpha <= 8) return false;

        double diff = colorDistance(color, startColor);
        if (diff > hardLimit) return false;

        double startLuma = luma(startColor);
        double currentLuma = luma(color);

        // Protección para no comerse líneas negras del personaje cuando se toca fondo claro/gris.
        if (startLuma > 95 && currentLuma < 45 && diff > 35) {
            return false;
        }

        return true;
    }

    private double colorDistance(int a, int b) {
        int dr = Color.red(a) - Color.red(b);
        int dg = Color.green(a) - Color.green(b);
        int db = Color.blue(a) - Color.blue(b);
        int da = Color.alpha(a) - Color.alpha(b);

        return Math.sqrt((dr * dr * 0.30) + (dg * dg * 0.59) + (db * db * 0.11)) + Math.abs(da) * 0.25;
    }

    private double luma(int color) {
        return (Color.red(color) * 0.299) + (Color.green(color) * 0.587) + (Color.blue(color) * 0.114);
    }

    private void closeTinySelectionGaps(boolean[] selected, int[] pixels, int w, int h, int startColor, double hardLimit) {
        boolean[] addMask = new boolean[selected.length];

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int index = y * w + x;
                if (selected[index]) continue;
                if (hasRectLimit && !limitRect.contains(x, y)) continue;
                if (!isMagicSelectable(pixels[index], startColor, hardLimit * 1.10)) continue;

                int count = 0;
                for (int yy = -1; yy <= 1; yy++) {
                    for (int xx = -1; xx <= 1; xx++) {
                        if (xx == 0 && yy == 0) continue;
                        if (selected[(y + yy) * w + (x + xx)]) count++;
                    }
                }

                if (count >= 7) {
                    addMask[index] = true;
                }
            }
        }

        for (int i = 0; i < selected.length; i++) {
            if (addMask[i]) selected[i] = true;
        }
    }

    private void applyMagicSelection(int[] pixels, boolean[] selected, int w, int h) {
        int[] originalPixels = pixels.clone();

        for (int i = 0; i < pixels.length; i++) {
            if (selected[i]) {
                pixels[i] = Color.TRANSPARENT;
            }
        }

        if (edgeSoftness <= 0) return;

        int radius = Math.max(1, 1 + edgeSoftness / 18);
        float strength = 0.45f + (edgeSoftness / 100f) * 0.42f;

        for (int y = radius; y < h - radius; y++) {
            for (int x = radius; x < w - radius; x++) {
                int index = y * w + x;

                if (selected[index]) continue;
                if (hasRectLimit && !limitRect.contains(x, y)) continue;

                float strongestCoverage = 0f;

                for (int yy = -radius; yy <= radius; yy++) {
                    for (int xx = -radius; xx <= radius; xx++) {
                        if (xx == 0 && yy == 0) continue;

                        int ni = (y + yy) * w + (x + xx);
                        if (!selected[ni]) continue;

                        float distance = (float) Math.sqrt((xx * xx) + (yy * yy));
                        if (distance > radius) continue;

                        float coverage = 1f - (distance / (radius + 0.001f));
                        if (coverage > strongestCoverage) strongestCoverage = coverage;
                    }
                }

                if (strongestCoverage > 0f) {
                    int p = originalPixels[index];
                    int alpha = Color.alpha(p);
                    int newAlpha = (int) (alpha * (1f - strongestCoverage * strength));

                    // Evita destruir las líneas oscuras principales del dibujo.
                    if (luma(p) < 50 && alpha > 170) {
                        newAlpha = Math.max(newAlpha, 165);
                    }

                    newAlpha = Math.max(0, Math.min(255, newAlpha));
                    pixels[index] = Color.argb(newAlpha, Color.red(p), Color.green(p), Color.blue(p));
                }
            }
        }
    }

    private void limitMove() {
        if (work == null || getWidth() <= 0 || getHeight() <= 0) return;

        buildMatrix();
        RectF bounds = new RectF(0, 0, work.getWidth(), work.getHeight());
        matrix.mapRect(bounds);

        if (bounds.width() <= getWidth()) {
            moveX += getWidth() / 2f - bounds.centerX();
        } else {
            if (bounds.left > 0) moveX -= bounds.left;
            if (bounds.right < getWidth()) moveX += getWidth() - bounds.right;
        }

        if (bounds.height() <= getHeight()) {
            moveY += getHeight() / 2f - bounds.centerY();
        } else {
            if (bounds.top > 0) moveY -= bounds.top;
            if (bounds.bottom < getHeight()) moveY += getHeight() - bounds.bottom;
        }
    }

    private float sp(int value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
