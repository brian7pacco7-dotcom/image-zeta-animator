package com.imagezeta.animator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayDeque;

public class CutEditorView extends View {

    public static final int MODE_ERASE_SOFT = 1;
    public static final int MODE_ERASE_HARD = 2;
    public static final int MODE_RESTORE = 3;

    private Bitmap originalBitmap;
    private Bitmap workBitmap;
    private Canvas workCanvas;

    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint hardErasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint softErasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkerA = new Paint();
    private final Paint checkerB = new Paint();

    private final Matrix viewMatrix = new Matrix();
    private final Matrix inverseMatrix = new Matrix();

    private final ArrayDeque<Bitmap> undoStack = new ArrayDeque<>();

    private int mode = MODE_ERASE_SOFT;
    private float brushSize = 35f;

    private float scale = 1f;
    private float minScale = 1f;
    private float translateX = 0f;
    private float translateY = 0f;

    private float lastPanX = 0f;
    private float lastPanY = 0f;
    private boolean panning = false;

    private ScaleGestureDetector scaleDetector;

    public CutEditorView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        checkerA.setColor(Color.rgb(235, 238, 245));
        checkerB.setColor(Color.WHITE);

        hardErasePaint.setStyle(Paint.Style.FILL);
        hardErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        softErasePaint.setStyle(Paint.Style.FILL);
        softErasePaint.setAlpha(95);
        softErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (workBitmap == null) return false;

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                float oldScale = scale;

                scale *= detector.getScaleFactor();
                scale = Math.max(minScale, Math.min(scale, minScale * 8f));

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

    public void setImage(Bitmap bitmap) {
        originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        workBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        workCanvas = new Canvas(workBitmap);

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

    public Bitmap getOutputBitmap() {
        return workBitmap;
    }

    public void resetView() {
        if (workBitmap == null || getWidth() == 0 || getHeight() == 0) {
            scale = 1f;
            translateX = 0f;
            translateY = 0f;
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

        if (undoStack.size() >= 8) {
            undoStack.removeFirst();
        }

        undoStack.addLast(workBitmap.copy(Bitmap.Config.ARGB_8888, true));
    }

    public void autoRemoveBackground() {
        if (workBitmap == null) return;

        saveUndo();

        int w = workBitmap.getWidth();
        int h = workBitmap.getHeight();

        int c1 = workBitmap.getPixel(0, 0);
        int c2 = workBitmap.getPixel(w - 1, 0);
        int c3 = workBitmap.getPixel(0, h - 1);
        int c4 = workBitmap.getPixel(w - 1, h - 1);

        int r = (Color.red(c1) + Color.red(c2) + Color.red(c3) + Color.red(c4)) / 4;
        int g = (Color.green(c1) + Color.green(c2) + Color.green(c3) + Color.green(c4)) / 4;
        int b = (Color.blue(c1) + Color.blue(c2) + Color.blue(c3) + Color.blue(c4)) / 4;

        int[] pixels = new int[w * h];
        workBitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];

            int dr = Color.red(p) - r;
            int dg = Color.green(p) - g;
            int db = Color.blue(p) - b;

            int distance = Math.abs(dr) + Math.abs(dg) + Math.abs(db);

            if (distance < 145) {
                pixels[i] = Color.TRANSPARENT;
            }
        }

        workBitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        workCanvas = new Canvas(workBitmap);
        invalidate();

        Toast.makeText(getContext(), "Fondo quitado. Corrige bordes manualmente.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawChecker(canvas);

        if (workBitmap == null) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.rgb(95, 105, 130));
            p.setTextSize(sp(16));
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Abre una imagen para editar", getWidth() / 2f, getHeight() / 2f, p);
            return;
        }

        buildMatrix();
        canvas.drawBitmap(workBitmap, viewMatrix, imagePaint);

        drawBrushPreview(canvas);
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

    private void drawBrushPreview(Canvas canvas) {
        if (workBitmap == null) return;

        Paint preview = new Paint(Paint.ANTI_ALIAS_FLAG);
        preview.setStyle(Paint.Style.STROKE);
        preview.setStrokeWidth(dp(2));

        if (mode == MODE_RESTORE) {
            preview.setColor(Color.rgb(20, 180, 100));
        } else {
            preview.setColor(Color.rgb(20, 105, 245));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (workBitmap == null) return true;

        scaleDetector.onTouchEvent(event);

        int pointers = event.getPointerCount();

        if (pointers >= 2) {
            handlePan(event);
            return true;
        }

        if (pointers == 1 && !scaleDetector.isInProgress()) {
            handleBrush(event);
            return true;
        }

        return true;
    }

    private void handlePan(MotionEvent event) {
        float cx = (event.getX(0) + event.getX(1)) / 2f;
        float cy = (event.getY(0) + event.getY(1)) / 2f;

        int action = event.getActionMasked();

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

    private void handleBrush(MotionEvent event) {
        float[] point = new float[]{event.getX(), event.getY()};

        buildMatrix();
        inverseMatrix.mapPoints(point);

        float x = point[0];
        float y = point[1];

        if (x < 0 || y < 0 || x >= workBitmap.getWidth() || y >= workBitmap.getHeight()) {
            return;
        }

        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            saveUndo();
            drawAt(x, y);
        } else if (action == MotionEvent.ACTION_MOVE) {
            drawAt(x, y);
        }
    }

    private void drawAt(float x, float y) {
        if (workCanvas == null) return;

        float radius = brushSize * (workBitmap.getWidth() / 1080f);
        radius = Math.max(4f, radius);

        if (mode == MODE_ERASE_HARD) {
            workCanvas.drawCircle(x, y, radius, hardErasePaint);
        } else if (mode == MODE_ERASE_SOFT) {
            workCanvas.drawCircle(x, y, radius, softErasePaint);
        } else if (mode == MODE_RESTORE && originalBitmap != null) {
            restoreCircle(x, y, radius);
        }

        invalidate();
    }

    private void restoreCircle(float x, float y, float radius) {
        Path path = new Path();
        path.addCircle(x, y, radius, Path.Direction.CW);

        workCanvas.save();
        workCanvas.clipPath(path);
        workCanvas.drawBitmap(originalBitmap, 0, 0, imagePaint);
        workCanvas.restore();
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
