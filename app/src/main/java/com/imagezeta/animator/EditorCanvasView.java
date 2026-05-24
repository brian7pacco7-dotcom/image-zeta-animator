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
    private final Paint maskDrawPaint;
    private final Paint maskPreviewPaint;

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

    private Bitmap limitMask;
    private Canvas limitMaskCanvas;
    private boolean hasMaskLimit = false;
    private boolean drawingMask = false;
    private float maskLastX;
    private float maskLastY;

    private boolean pendingMagicTap = false;
    private float magicTapX;
    private float magicTapY;
    private float magicDownScreenX;
    private float magicDownScreenY;

    private int transparencyMode = 0;

    public EditorCanvasView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        checkerDark = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkerDark.setColor(Color.rgb(150, 150, 150));

        checkerLight = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkerLight.setColor(Color.rgb(225, 225, 225));

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

        maskDrawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskDrawPaint.setStyle(Paint.Style.STROKE);
        maskDrawPaint.setStrokeCap(Paint.Cap.ROUND);
        maskDrawPaint.setStrokeJoin(Paint.Join.ROUND);
        maskDrawPaint.setColor(Color.WHITE);

        maskPreviewPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        maskPreviewPaint.setAlpha(95);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (work == null) return false;

                suppressSingleAfterMulti = true;
                drawing = false;
                drawingMask = false;
                pendingMagicTap = false;

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

        limitMask = Bitmap.createBitmap(work.getWidth(), work.getHeight(), Bitmap.Config.ARGB_8888);
        limitMask.eraseColor(Color.TRANSPARENT);
        limitMaskCanvas = new Canvas(limitMask);

        undoStack.clear();
        redoStack.clear();
        hasMaskLimit = false;
        drawingMask = false;
        pendingMagicTap = false;

        post(this::resetView);
        invalidate();
    }

    public void setTool(int newTool) {
        tool = newTool;

        if (tool == TOOL_RECT) {
            Toast.makeText(getContext(), "Dibuja con el dedo la zona donde trabajará la varita.", Toast.LENGTH_SHORT).show();
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
        clearMagicMask();
    }

    public void clearMagicMask() {
        hasMaskLimit = false;
        drawingMask = false;
        pendingMagicTap = false;
        if (limitMask != null) {
            limitMask.eraseColor(Color.TRANSPARENT);
            limitMaskCanvas = new Canvas(limitMask);
        }
        invalidate();
    }

    public String cycleTransparencyBackground() {
        transparencyMode = (transparencyMode + 1) % 3;
        invalidate();

        if (transparencyMode == 1) return "Fondo blanco";
        if (transparencyMode == 2) return "Fondo negro";
        return "Cuadros de transparencia";
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
        drawMagicMask(canvas);
    }

    private void drawTransparentBackground(Canvas canvas) {
        if (transparencyMode == 1) {
            canvas.drawColor(Color.WHITE);
            return;
        }

        if (transparencyMode == 2) {
            canvas.drawColor(Color.BLACK);
            return;
        }

        canvas.drawColor(Color.WHITE);

        if (work == null) return;

        buildMatrix();
        canvas.save();
        canvas.concat(matrix);
        canvas.clipRect(0, 0, work.getWidth(), work.getHeight());

        final float density = getResources().getDisplayMetrics().density;
        int size = Math.max(6, (int) (8 * density));

        Paint light = checkerLight;
        Paint dark = checkerDark;
        light.setColor(Color.rgb(232, 232, 232));
        dark.setColor(Color.rgb(178, 178, 178));

        for (int y = 0; y < work.getHeight(); y += size) {
            for (int x = 0; x < work.getWidth(); x += size) {
                boolean alt = ((x / size) + (y / size)) % 2 == 0;
                canvas.drawRect(x, y, x + size, y + size, alt ? light : dark);
            }
        }

        canvas.restore();
    }

    private void drawMagicMask(Canvas canvas) {
        if (!hasMaskLimit || limitMask == null || work == null) return;

        buildMatrix();
        canvas.drawBitmap(limitMask, matrix, maskPreviewPaint);
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
            drawingMask = false;
            pendingMagicTap = false;
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
        int action = event.getActionMasked();

        if (tool == TOOL_MOVE) {
            handleMoveTool(event);
            return;
        }

        float[] point = toBitmapPoint(event.getX(), event.getY());
        float x = point[0];
        float y = point[1];

        if (x < 0 || y < 0 || x >= work.getWidth() || y >= work.getHeight()) {
            pendingMagicTap = false;
            return;
        }

        if (tool == TOOL_RECT) {
            handleMaskTool(action, x, y);
            return;
        }

        if (tool == TOOL_MAGIC) {
            handleMagicTouch(event, action, x, y);
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

    private void handleMoveTool(MotionEvent event) {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            panning = true;
            lastPanX = event.getX();
            lastPanY = event.getY();
        } else if (action == MotionEvent.ACTION_MOVE && panning) {
            float dx = event.getX() - lastPanX;
            float dy = event.getY() - lastPanY;
            moveX += dx;
            moveY += dy;
            lastPanX = event.getX();
            lastPanY = event.getY();
            limitMove();
            invalidate();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            panning = false;
        }
    }

    private void handleMagicTouch(MotionEvent event, int action, float x, float y) {
        if (action == MotionEvent.ACTION_DOWN) {
            pendingMagicTap = true;
            magicTapX = x;
            magicTapY = y;
            magicDownScreenX = event.getX();
            magicDownScreenY = event.getY();
            return;
        }

        if (action == MotionEvent.ACTION_MOVE && pendingMagicTap) {
            float dx = event.getX() - magicDownScreenX;
            float dy = event.getY() - magicDownScreenY;
            float cancelDistance = Math.max(10f, 10f * getResources().getDisplayMetrics().density);

            if ((dx * dx) + (dy * dy) > cancelDistance * cancelDistance) {
                pendingMagicTap = false;
            }
            return;
        }

        if (action == MotionEvent.ACTION_UP) {
            if (pendingMagicTap) {
                saveUndo();
                magicErase((int) magicTapX, (int) magicTapY);
            }
            pendingMagicTap = false;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            pendingMagicTap = false;
        }
    }

    private void handleMaskTool(int action, float x, float y) {
        if (limitMaskCanvas == null) return;

        float radius = brushSize * (work.getWidth() / 1080f);
        radius = Math.max(8f, radius * 1.3f);
        maskDrawPaint.setStrokeWidth(radius * 2f);

        if (action == MotionEvent.ACTION_DOWN) {
            drawingMask = true;
            hasMaskLimit = true;
            maskLastX = x;
            maskLastY = y;
            limitMaskCanvas.drawLine(x, y, x + 0.1f, y + 0.1f, maskDrawPaint);
            invalidate();
        } else if (action == MotionEvent.ACTION_MOVE && drawingMask) {
            limitMaskCanvas.drawLine(maskLastX, maskLastY, x, y, maskDrawPaint);
            maskLastX = x;
            maskLastY = y;
            invalidate();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            drawingMask = false;
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
            workCanvas.drawLine(x1, y1, x2, y2, s
