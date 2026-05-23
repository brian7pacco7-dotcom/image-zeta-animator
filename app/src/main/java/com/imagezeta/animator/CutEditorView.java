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

public class CutEditorView extends View {

    public static final int MODE_ERASE_SOFT = 1;
    public static final int MODE_ERASE_HARD = 2;
    public static final int MODE_RESTORE = 3;
    public static final int MODE_MAGIC = 4;

    private Bitmap originalBitmap;
    private Bitmap workBitmap;
    private Canvas workCanvas;

    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint checkerA = new Paint();
    private final Paint checkerB = new Paint();

    private Paint eraseHardPaint;
    private Paint eraseSoftPaint;
    private Paint restorePaint;

    private final Matrix viewMatrix = new Matrix();
    private final Matrix inverseMatrix = new Matrix();

    private final ArrayDeque<Bitmap> undoStack = new ArrayDeque<>();

    private ScaleGestureDetector scaleDetector;

    private int mode = MODE_ERASE_SOFT;
    private int tolerance = 45;
    private float brushSize = 35f;

    private float scale = 1f;
    private float minScale = 1f;
    private float translateX = 0f;
    private float translateY = 0f;

    private float lastPanX = 0f;
    private float lastPanY = 0f;
    private boolean panning = false;

    private float lastBitmapX = 0f;
    private float lastBitmapY = 0f;
    private boolean drawing = false;

    public CutEditorView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        checkerA.setColor(Color.rgb(235, 238, 245));
        checkerB.setColor(Color.WHITE);

        eraseHardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eraseHardPaint.setStyle(Paint.Style.STROKE);
        eraseHardPaint.setStrokeCap(Paint.Cap.ROUND);
        eraseHardPaint.setStrokeJoin(Paint.Join.ROUND);
        eraseHardPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        eraseSoftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eraseSoftPaint.setStyle(Paint.Style.STROKE);
        eraseSoftPaint.setStrokeCap(Paint.Cap.ROUND);
        eraseSoftPaint.setStrokeJoin(Paint.Join.ROUND);
        eraseSoftPaint.setAlpha(120);
        eraseSoftPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        restorePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        restorePaint.setStyle(Paint.Style.STROKE);
        restorePaint.setStrokeCap(Paint.Cap.ROUND);
        restorePaint.setStrokeJoin(Paint.Join.ROUND);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (workBitmap == null) return false;

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                float oldScale = scale;

                scale *= detector.getScaleFactor();
                scale = Math.max(minScale, Math.min(scale, minScale * 12f));

                float factor = scale / oldScale;
                translateX = focusX - factor * (focusX - translateX);
                translateY = focusY - factor * (focusY - translateY);

                limitPan();
                invalidate();
                return true;
            }
        });
    }

    public boolean hasImage() {
        return workBitmap != null;
    }

    public Bitmap getOutputBitmap() {
        return workBitmap;
    }

    public void setImage(Bitmap bitmap) {
        originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        workBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        workCanvas = new Canvas(workBitmap);

        restorePaint.setShader(new BitmapShader(originalBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

        undoStack.clear();
        resetView();
        invalidate();
    }

    public void setMode(int newMode) {
        mode = newMode;
    }

    public void setBrushSize(int size) {
        brushSize = Math.max(5, size);
    }

    public void setTolerance(int value) {
        tolerance = Math.max(5, value);
    }

    public void resetView() {
        if (workBitmap == null || getWidth() == 0 || getHeight() == 0) {
            invalidate();
            return;
        }

        float sx = getWidth() / (float) workBitmap.getWidth();
        float sy = getHeight() / (float) workBitmap.getHeight();

        minScale = Math.min(sx, sy);
        scale = minScale;
        translateX = (getWidth() - workBitmap.getWidth() * scale) / 2f;
        translateY = (getHeight() - workBitmap.getHeight() * scale) / 2f;

        invalidate();
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            Toast.makeText(getContext(), "No hay cambios para deshacer.", Toast.LENGTH_SHORT).show();
            return;
        }

        workBitmap = undoStack.removeLast();
        workCanvas = new Canvas(workBitmap);
        invalidate();
    }

    private void saveUndo() {
        if (workBitmap == null) return;

        if (undoStack.size() >= 10) {
            undoStack.removeFirst();
        }

        undoStack.addLast(workBitmap.copy(Bitmap.Config.ARGB_8888, true));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        resetView();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawChecker(canvas);

        if (workBitmap == null) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.rgb(80, 90, 110));
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(sp(16));
            canvas.drawText("Abre una imagen", getWidth() / 2f, getHeight() / 2f, p);
            return;
        }

        buildMatrix();
        canvas.drawBitmap(workBitmap, viewMatrix, imagePaint);
    }

    private void drawChecker(Canvas canvas) {
        int size = dp(18);

        for (int y = 0; y < getHeight(); y += size) {
            for (int x = 0; x < getWidth(); x += size) {
                boolean alt = ((x / size) + (y / size)) % 2 == 0;
                canvas.drawRect(x, y, x + size, y + size, alt ? checkerA : checkerB);
            }
        }
    }

    private void buildMatrix() {
        viewMatrix.reset();
        viewMatrix.postScale(scale, scale);
        viewMatrix.postTranslate(translateX, translateY);
        viewMatrix.invert(inverseMatrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (workBitmap == null) return true;

        getParent().requestDisallowInterceptTouchEvent(true);
        scaleDetector.onTouchEvent(event);

        if (event.getPointerCount() >= 2) {
            handlePan(event);
            return true;
        }

        if (!scaleDetector.isInProgress()) {
            handleOneFinger(event);
        }

        return true;
    }

    private void handlePan(MotionEvent event) {
        int action = event.getActionMasked();

        float cx = (event.getX(0) + event.getX(1)) / 2f;
        float cy = (event.getY(0) + event.getY(1)) / 2f;

        if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_DOWN) {
            lastPanX = cx;
            lastPanY = cy;
            panning = true;
        } else if (action == MotionEvent.ACTION_MOVE && panning) {
            translateX += cx - lastPanX;
            translateY += cy - lastPanY;

            lastPanX = cx;
            lastPanY = cy;

            limitPan();
            invalidate();
        } else if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            panning = false;
        }
    }

    private void handleOneFinger(MotionEvent event) {
        float[] point = screenToBitmap(event.getX(), event.getY());

        float x = point[0];
        float y = point[1];

        if (x < 0 || y < 0 || x >= workBitmap.getWidth() || y >= workBitmap.getHeight()) {
            return;
        }

        int action = event.getActionMasked();

        if (mode == MODE_MAGIC) {
            if (action == MotionEvent.ACTION_DOWN) {
                saveUndo();
                magicErase((int) x, (int) y);
            }
            return;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            saveUndo();
            lastBitmapX = x;
            lastBitmapY = y;
            drawing = true;
            drawStroke(x, y, x + 0.1f, y + 0.1f);
        } else if (action == MotionEvent.ACTION_MOVE && drawing) {
            drawStroke(lastBitmapX, lastBitmapY, x, y);
            lastBitmapX = x;
            lastBitmapY = y;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            drawing = false;
        }
    }

    private float[] screenToBitmap(float sx, float sy) {
        buildMatrix();
        float[] point = new float[]{sx, sy};
        inverseMatrix.mapPoints(point);
        return point;
    }

    private void drawStroke(float x1, float y1, float x2, float y2) {
        if (workCanvas == null) return;

        float radius = brushSize * (workBitmap.getWidth() / 1080f);
        radius = Math.max(4f, radius);
        float strokeWidth = radius * 2f;

        if (mode == MODE_ERASE_HARD) {
            eraseHardPaint.setStrokeWidth(strokeWidth);
            workCanvas.drawLine(x1, y1, x2, y2, eraseHardPaint);
        } else if (mode == MODE_ERASE_SOFT) {
            eraseSoftPaint.setStrokeWidth(strokeWidth);
            workCanvas.drawLine(x1, y1, x2, y2, eraseSoftPaint);
        } else if (mode == MODE_RESTORE) {
            restorePaint.setStrokeWidth(strokeWidth);
            workCanvas.drawLine(x1, y1, x2, y2, restorePaint);
        }

        invalidate();
    }

    private void magicErase(int startX, int startY) {
        int w = workBitmap.getWidth();
        int h = workBitmap.getHeight();

        if (startX < 0 || startY < 0 || startX >= w || startY >= h) return;

        int[] pixels = new int[w * h];
        workBitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        int startColor = pixels[startY * w + startX];
        int sr = Color.red(startColor);
        int sg = Color.green(startColor);
        int sb = Color.blue(startColor);
        int sa = Color.alpha(startColor);

        boolean[] visited = new boolean[w * h];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(startY * w + startX);
        visited[startY * w + startX] = true;

        int limit = tolerance * 3;

        while (!queue.isEmpty()) {
            int index = queue.removeFirst();

            int p = pixels[index];
            int diff = Math.abs(Color.red(p) - sr)
                    + Math.abs(Color.green(p) - sg)
                    + Math.abs(Color.blue(p) - sb)
                    + Math.abs(Color.alpha(p) - sa);

            if (diff > limit) continue;

            pixels[index] = Color.TRANSPARENT;

            int x = index % w;
            int y = index / w;

            addNeighbor(queue, visited, x + 1, y, w, h);
            addNeighbor(queue, visited, x - 1, y, w, h);
            addNeighbor(queue, visited, x, y + 1, w, h);
            addNeighbor(queue, visited, x, y - 1, w, h);
        }

        workBitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        workCanvas = new Canvas(workBitmap);
        invalidate();
    }

    private void addNeighbor(ArrayDeque<Integer> queue, boolean[] visited, int x, int y, int w, int h) {
        if (x < 0 || y < 0 || x >= w || y >= h) return;

        int index = y * w + x;

        if (!visited[index]) {
            visited[index] = true;
            queue.add(index);
        }
    }

    private void limitPan() {
        if (workBitmap == null) return;

        float imgW = workBitmap.getWidth() * scale;
        float imgH = workBitmap.getHeight() * scale;

        if (imgW <= getWidth()) {
            translateX = (getWidth() - imgW) / 2f;
        } else {
            float minX = getWidth() - imgW;
            translateX = Math.max(minX, Math.min(0, translateX));
        }

        if (imgH <= getHeight()) {
            translateY = (getHeight() - imgH) / 2f;
        } else {
            float minY = getHeight() - imgH;
            translateY = Math.max(minY, Math.min(0, translateY));
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private float sp(int value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
                            }
