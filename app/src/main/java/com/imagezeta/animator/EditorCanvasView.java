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
    private final Paint checkerA;
    private final Paint checkerB;

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
    private final float maxScale = 45f;
    private float moveX = 0f;
    private float moveY = 0f;

    private float lastPanX;
    private float lastPanY;
    private boolean panning = false;

    private float lastX;
    private float lastY;
    private boolean drawing = false;

    public EditorCanvasView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        checkerA = new Paint();
        checkerA.setColor(Color.rgb(220, 220, 220));

        checkerB = new Paint();
        checkerB.setColor(Color.rgb(245, 245, 245));

        hardErase = new Paint(Paint.ANTI_ALIAS_FLAG);
        hardErase.setStyle(Paint.Style.STROKE);
        hardErase.setStrokeCap(Paint.Cap.ROUND);
        hardErase.setStrokeJoin(Paint.Join.ROUND);
        hardErase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        softErase = new Paint(Paint.ANTI_ALIAS_FLAG);
        softErase.setStyle(Paint.Style.STROKE);
        softErase.setStrokeCap(Paint.Cap.ROUND);
        softErase.setStrokeJoin(Paint.Join.ROUND);
        softErase.setAlpha(130);
        softErase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        restorePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        restorePaint.setStyle(Paint.Style.STROKE);
        restorePaint.setStrokeCap(Paint.Cap.ROUND);
        restorePaint.setStrokeJoin(Paint.Join.ROUND);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (work == null) return false;

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                float oldScale = scale;
                scale *= detector.getScaleFactor();
                scale = Math.max(minScale, Math.min(scale, minScale * maxScale));

                float factor = scale / oldScale;
                moveX = focusX - factor * (focusX - moveX);
                moveY = focusY - factor * (focusY - moveY);

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

        post(this::resetView);
        invalidate();
    }

    public void setTool(int newTool) {
        tool = newTool;
    }

    public void setBrushSize(int size) {
        brushSize = Math.max(1, size);
    }

    public void setTolerance(int value) {
        tolerance = Math.max(1, value);
    }

    public void setEdgeSoftness(int value) {
        edgeSoftness = Math.max(0, Math.min(100, value));
        int alpha = 80 + (edgeSoftness * 140 / 100);
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

        moveX = (getWidth() - work.getWidth() * scale) / 2f;
        moveY = (getHeight() - work.getHeight() * scale) / 2f;

        notifyZoom();
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

        drawBackground(canvas);

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
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawColor(Color.rgb(72, 72, 72));

        if (work == null) return;

        buildMatrix();

        canvas.save();
        canvas.concat(matrix);

        int size = Math.max(10, work.getWidth() / 40);

        for (int y = 0; y < work.getHeight(); y += size) {
            for (int x = 0; x < work.getWidth(); x += size) {
                boolean alt = ((x / size) + (y / size)) % 2 == 0;
                canvas.drawRect(x, y, x + size, y + size, alt ? checkerA : checkerB);
            }
        }

        canvas.restore();
    }

    private void buildMatrix() {
        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(moveX, moveY);
        matrix.invert(inverse);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (work == null) return true;

        getParent().requestDisallowInterceptTouchEvent(true);
        scaleDetector.onTouchEvent(event);

        if (event.getPointerCount() >= 2) {
            handleTwoFinger(event);
            return true;
        }

        if (!scaleDetector.isInProgress()) {
            handleOneFinger(event);
        }

        return true;
    }

    private void handleTwoFinger(MotionEvent event) {
        int action = event.getActionMasked();

        float cx = (event.getX(0) + event.getX(1)) / 2f;
        float cy = (event.getY(0) + event.getY(1)) / 2f;

        if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_DOWN) {
            panning = true;
            lastPanX = cx;
            lastPanY = cy;
        } else if (action == MotionEvent.ACTION_MOVE && panning) {
            moveX += cx - lastPanX;
            moveY += cy - lastPanY;

            lastPanX = cx;
            lastPanY = cy;

            limitMove();
            notifyZoom();
            invalidate();
        } else if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            panning = false;
        }
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

        int[] pixels = new int[w * h];
        work.getPixels(pixels, 0, w, 0, 0, w, h);

        int startIndex = startY * w + startX;
        int startColor = pixels[startIndex];

        int sr = Color.red(startColor);
        int sg = Color.green(startColor);
        int sb = Color.blue(startColor);
        int sa = Color.alpha(startColor);

        boolean[] seen = new boolean[w * h];
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        queue.add(startIndex);
        seen[startIndex] = true;

        int limit = tolerance * 3;
        int maxPixels = w * h;
        int touched = 0;

        while (!queue.isEmpty()) {
            int index = queue.removeFirst();
            int p = pixels[index];

            int diff = Math.abs(Color.red(p) - sr)
                    + Math.abs(Color.green(p) - sg)
                    + Math.abs(Color.blue(p) - sb)
                    + Math.abs(Color.alpha(p) - sa);

            if (diff > limit) continue;

            pixels[index] = Color.TRANSPARENT;
            touched++;

            if (!holeRecognition && touched > maxPixels / 2) break;

            int x = index % w;
            int y = index / w;

            add(queue, seen, x + 1, y, w, h);
            add(queue, seen, x - 1, y, w, h);
            add(queue, seen, x, y + 1, w, h);
            add(queue, seen, x, y - 1, w, h);
        }

        work.setPixels(pixels, 0, w, 0, 0, w, h);
        workCanvas = new Canvas(work);
        invalidate();
    }

    private void add(ArrayDeque<Integer> queue, boolean[] seen, int x, int y, int w, int h) {
        if (x < 0 || y < 0 || x >= w || y >= h) return;

        int index = y * w + x;

        if (!seen[index]) {
            seen[index] = true;
            queue.add(index);
        }
    }

    private void limitMove() {
        if (work == null) return;

        float imgW = work.getWidth() * scale;
        float imgH = work.getHeight() * scale;

        if (imgW <= getWidth()) {
            moveX = (getWidth() - imgW) / 2f;
        } else {
            float minX = getWidth() - imgW;
            moveX = Math.max(minX, Math.min(0, moveX));
        }

        if (imgH <= getHeight()) {
            moveY = (getHeight() - imgH) / 2f;
        } else {
            float minY = getHeight() - imgH;
            moveY = Math.max(minY, Math.min(0, moveY));
        }
    }

    private float sp(int value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
  }
