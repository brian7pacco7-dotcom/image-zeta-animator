package com.example.videozetafixer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.database.Cursor;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE = 1001;

    private EditorView editorView;
    private Button btnOpen, btnWand, btnBg, btnReset, btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(Color.BLACK);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(8, 8, 8, 8);
        bar.setBackgroundColor(Color.rgb(25, 25, 25));

        btnOpen = new Button(this);
        btnOpen.setText("Abrir");

        btnWand = new Button(this);
        btnWand.setText("Varita OFF");

        btnBg = new Button(this);
        btnBg.setText("Fondo B/N");

        btnReset = new Button(this);
        btnReset.setText("Reset");

        btnSave = new Button(this);
        btnSave.setText("Guardar");

        bar.addView(btnOpen, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        bar.addView(btnWand, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        bar.addView(btnBg, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        bar.addView(btnReset, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        bar.addView(btnSave, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        editorView = new EditorView(this);

        main.addView(bar);
        main.addView(editorView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        setContentView(main);

        btnOpen.setOnClickListener(v -> openImage());

        btnWand.setOnClickListener(v -> {
            editorView.toggleWand();
            btnWand.setText(editorView.isWandEnabled() ? "Varita ON" : "Varita OFF");
        });

        btnBg.setOnClickListener(v -> editorView.toggleCheckerBackground());

        btnReset.setOnClickListener(v -> editorView.resetImage());

        btnSave.setOnClickListener(v -> saveImage());
    }

    private void openImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();
                InputStream inputStream = getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (bitmap != null) {
                    editorView.setImage(bitmap);
                    Toast.makeText(this, "Imagen cargada", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No se pudo abrir la imagen", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Toast.makeText(this, "Error al abrir imagen", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImage() {
        try {
            Bitmap result = editorView.getEditedBitmap();

            if (result == null) {
                Toast.makeText(this, "Primero abre una imagen", Toast.LENGTH_SHORT).show();
                return;
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "VideoZeta_" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VideoZeta");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri == null) {
                Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            result.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

            if (outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }

            Toast.makeText(this, "Guardado en Imágenes/VideoZeta", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar imagen", Toast.LENGTH_SHORT).show();
        }
    }

    public static class EditorView extends View {

        private Bitmap originalBitmap;
        private Bitmap editBitmap;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        private boolean wandEnabled = false;
        private boolean checkerMode = true;

        private float scale = 1f;
        private float minScale = 1f;
        private float maxScale = 8f;

        private float offsetX = 0f;
        private float offsetY = 0f;

        private float lastX;
        private float lastY;
        private boolean dragging = false;
        private boolean scaling = false;

        private ScaleGestureDetector scaleDetector;

        private final int tolerance = 38;

        public EditorView(android.content.Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);

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

                    limitImagePosition();
                    invalidate();
                    return true;
                }

                @Override
                public void onScaleEnd(ScaleGestureDetector detector) {
                    scaling = false;
                }
            });
        }

        public void setImage(Bitmap bitmap) {
            originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            editBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            post(() -> {
                fitImageToScreen();
                invalidate();
            });
        }

        public Bitmap getEditedBitmap() {
            return editBitmap;
        }

        public void toggleWand() {
            wandEnabled = !wandEnabled;
        }

        public boolean isWandEnabled() {
            return wandEnabled;
        }

        public void toggleCheckerBackground() {
            checkerMode = !checkerMode;
            invalidate();
        }

        public void resetImage() {
            if (originalBitmap != null) {
                editBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                invalidate();
            }
        }

        private void fitImageToScreen() {
            if (editBitmap == null || getWidth() == 0 || getHeight() == 0) return;

            float viewW = getWidth();
            float viewH = getHeight();

            float imgW = editBitmap.getWidth();
            float imgH = editBitmap.getHeight();

            scale = Math.min(viewW / imgW, viewH / imgH);
            minScale = scale;

            offsetX = (viewW - imgW * scale) / 2f;
            offsetY = (viewH - imgH * scale) / 2f;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            drawBackground(canvas);

            if (editBitmap != null) {
                canvas.save();
                canvas.translate(offsetX, offsetY);
                canvas.scale(scale, scale);
                canvas.drawBitmap(editBitmap, 0, 0, bitmapPaint);
                canvas.restore();
            } else {
                paint.setColor(Color.WHITE);
                paint.setTextSize(42f);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Abre una imagen", getWidth() / 2f, getHeight() / 2f, paint);
            }
        }

        private void drawBackground(Canvas canvas) {
            if (!checkerMode) {
                canvas.drawColor(Color.rgb(35, 35, 35));
                return;
            }

            int size = 36;

            for (int y = 0; y < getHeight(); y += size) {
                for (int x = 0; x < getWidth(); x += size) {
                    boolean white = ((x / size) + (y / size)) % 2 == 0;
                    paint.setColor(white ? Color.WHITE : Color.rgb(35, 35, 35));
                    canvas.drawRect(x, y, x + size, y + size, paint);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (editBitmap == null) return true;

            scaleDetector.onTouchEvent(event);

            int action = event.getActionMasked();

            if (event.getPointerCount() > 1) {
                scaling = true;
                return true;
            }

            float x = event.getX();
            float y = event.getY();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    lastX = x;
                    lastY = y;
                    dragging = true;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (!wandEnabled && dragging && !scaling) {
                        float dx = x - lastX;
                        float dy = y - lastY;

                        offsetX += dx;
                        offsetY += dy;

                        lastX = x;
                        lastY = y;

                        limitImagePosition();
                        invalidate();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (wandEnabled && !scaling) {
                        float moveDistance = Math.abs(x - lastX) + Math.abs(y - lastY);

                        if (moveDistance < 15f) {
                            eraseWithMagicWand(x, y);
                        }
                    }

                    dragging = false;
                    scaling = false;
                    return true;
            }

            return true;
        }

        private void limitImagePosition() {
            if (editBitmap == null) return;

            float imgW = editBitmap.getWidth() * scale;
            float imgH = editBitmap.getHeight() * scale;

            if (imgW <= getWidth()) {
                offsetX = (getWidth() - imgW) / 2f;
            } else {
                if (offsetX > 0) offsetX = 0;
                if (offsetX + imgW < getWidth()) offsetX = getWidth() - imgW;
            }

            if (imgH <= getHeight()) {
                offsetY = (getHeight() - imgH) / 2f;
            } else {
                if (offsetY > 0) offsetY = 0;
                if (offsetY + imgH < getHeight()) offsetY = getHeight() - imgH;
            }
        }

        private void eraseWithMagicWand(float screenX, float screenY) {
            if (editBitmap == null) return;

            int bmpX = (int) ((screenX - offsetX) / scale);
            int bmpY = (int) ((screenY - offsetY) / scale);

            if (bmpX < 0 || bmpY < 0 || bmpX >= editBitmap.getWidth() || bmpY >= editBitmap.getHeight()) {
                return;
            }

            magicEraseSmooth(bmpX, bmpY);
            invalidate();
        }

        private void magicEraseSmooth(int startX, int startY) {
            int width = editBitmap.getWidth();
            int height = editBitmap.getHeight();

            int targetColor = editBitmap.getPixel(startX, startY);

            if (Color.alpha(targetColor) == 0) return;

            boolean[] visited = new boolean[width * height];
            Queue<Point> queue = new LinkedList<>();
            queue.add(new Point(startX, startY));

            int processed = 0;
            int maxProcess = width * height;

            while (!queue.isEmpty() && processed < maxProcess) {
                Point p = queue.poll();

                int x = p.x;
                int y = p.y;

                if (x < 0 || y < 0 || x >= width || y >= height) continue;

                int index = y * width + x;

                if (visited[index]) continue;
                visited[index] = true;

                int currentColor = editBitmap.getPixel(x, y);

                if (isSimilarColor(targetColor, currentColor, tolerance)) {
                    editBitmap.setPixel(x, y, Color.TRANSPARENT);

                    queue.add(new Point(x + 1, y));
                    queue.add(new Point(x - 1, y));
                    queue.add(new Point(x, y + 1));
                    queue.add(new Point(x, y - 1));

                    queue.add(new Point(x + 1, y + 1));
                    queue.add(new Point(x - 1, y - 1));
                    queue.add(new Point(x + 1, y - 1));
                    queue.add(new Point(x - 1, y + 1));
                }

                processed++;
            }

            softenEdges();
        }

        private boolean isSimilarColor(int c1, int c2, int tol) {
            int a2 = Color.alpha(c2);
            if (a2 == 0) return false;

            int r1 = Color.red(c1);
            int g1 = Color.green(c1);
            int b1 = Color.blue(c1);

            int r2 = Color.red(c2);
            int g2 = Color.green(c2);
            int b2 = Color.blue(c2);

            int diff = Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);

            return diff <= tol * 3;
        }

        private void softenEdges() {
            if (editBitmap == null) return;

            int width = editBitmap.getWidth();
            int height = editBitmap.getHeight();

            Bitmap copy = editBitmap.copy(Bitmap.Config.ARGB_8888, true);

            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    int color = copy.getPixel(x, y);

                    if (Color.alpha(color) == 0) continue;

                    boolean nearTransparent =
                            Color.alpha(copy.getPixel(x + 1, y)) == 0 ||
                            Color.alpha(copy.getPixel(x - 1, y)) == 0 ||
                            Color.alpha(copy.getPixel(x, y + 1)) == 0 ||
                            Color.alpha(copy.getPixel(x, y - 1)) == 0;

                    if (nearTransparent) {
                        int r = Color.red(color);
                        int g = Color.green(color);
                        int b = Color.blue(color);

                        editBitmap.setPixel(x, y, Color.argb(120, r, g, b));
                    }
                }
            }
        }
    }
        }
