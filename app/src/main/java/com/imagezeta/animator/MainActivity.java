package com.imagezeta.animator;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE = 77;

    private final int BLUE = Color.rgb(20, 105, 245);
    private final int DARK = Color.rgb(12, 24, 54);
    private final int BG = Color.rgb(246, 249, 255);

    private CutEditorView editor;
    private TextView sizeInfo;
    private TextView modeInfo;
    private TextView brushInfo;
    private TextView toleranceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String s, int sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, style);
        return t;
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private GradientDrawable card() {
        GradientDrawable g = bg(Color.WHITE, 16);
        g.setStroke(dp(1), Color.rgb(220, 230, 245));
        return g;
    }

    private Button btn(String s, int color, int textColor) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTextColor(textColor);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(bg(color, 14));
        return b;
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.setBackgroundColor(BG);
        setContentView(root);

        TextView title = text("Image Zeta Background Remover", 18, DARK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(34)));

        sizeInfo = text("Tamaño original: abre imagen", 12, BLUE, Typeface.BOLD);
        sizeInfo.setGravity(Gravity.CENTER);
        sizeInfo.setBackground(card());
        root.addView(sizeInfo, new LinearLayout.LayoutParams(-1, dp(38)));

        editor = new CutEditorView(this);
        editor.setBackground(card());
        root.addView(editor, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        modeInfo = text("Modo: borrador suave | Zoom y mover: 2 dedos", 12, DARK, Typeface.BOLD);
        modeInfo.setGravity(Gravity.CENTER);
        modeInfo.setPadding(0, dp(5), 0, dp(5));
        root.addView(modeInfo, new LinearLayout.LayoutParams(-1, dp(36)));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(row1, new LinearLayout.LayoutParams(-1, dp(52)));

        Button open = btn("Abrir", Color.WHITE, BLUE);
        open.setOnClickListener(v -> openImage());
        row1.addView(open, new LinearLayout.LayoutParams(0, -1, 1));

        Button magic = btn("Varita", Color.WHITE, DARK);
        magic.setOnClickListener(v -> {
            editor.setMode(CutEditorView.MODE_MAGIC);
            modeInfo.setText("Modo: varita | toca el fondo para borrar zona similar");
        });
        row1.addView(magic, new LinearLayout.LayoutParams(0, -1, 1));

        Button soft = btn("Suave", Color.WHITE, DARK);
        soft.setOnClickListener(v -> {
            editor.setMode(CutEditorView.MODE_ERASE_SOFT);
            modeInfo.setText("Modo: borrador suave");
        });
        row1.addView(soft, new LinearLayout.LayoutParams(0, -1, 1));

        Button hard = btn("Duro", Color.WHITE, DARK);
        hard.setOnClickListener(v -> {
            editor.setMode(CutEditorView.MODE_ERASE_HARD);
            modeInfo.setText("Modo: borrador duro");
        });
        row1.addView(hard, new LinearLayout.LayoutParams(0, -1, 1));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(6), 0, 0);
        root.addView(row2, new LinearLayout.LayoutParams(-1, dp(58)));

        Button restore = btn("Restaurar", Color.WHITE, DARK);
        restore.setOnClickListener(v -> {
            editor.setMode(CutEditorView.MODE_RESTORE);
            modeInfo.setText("Modo: restaurar partes borradas");
        });
        row2.addView(restore, new LinearLayout.LayoutParams(0, -1, 1));

        Button undo = btn("Deshacer", Color.WHITE, DARK);
        undo.setOnClickListener(v -> editor.undo());
        row2.addView(undo, new LinearLayout.LayoutParams(0, -1, 1));

        Button fit = btn("Ajustar", Color.WHITE, DARK);
        fit.setOnClickListener(v -> editor.resetView());
        row2.addView(fit, new LinearLayout.LayoutParams(0, -1, 1));

        Button export = btn("Exportar PNG", BLUE, Color.WHITE);
        export.setOnClickListener(v -> savePng());
        row2.addView(export, new LinearLayout.LayoutParams(0, -1, 1));

        brushInfo = text("Pincel: 35", 12, DARK, Typeface.BOLD);
        root.addView(brushInfo, new LinearLayout.LayoutParams(-1, dp(28)));

        SeekBar brush = new SeekBar(this);
        brush.setMax(120);
        brush.setProgress(35);
        brush.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int value = Math.max(5, progress);
                editor.setBrushSize(value);
                brushInfo.setText("Pincel: " + value);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(brush, new LinearLayout.LayoutParams(-1, dp(44)));

        toleranceInfo = text("Tolerancia varita: 45", 12, DARK, Typeface.BOLD);
        root.addView(toleranceInfo, new LinearLayout.LayoutParams(-1, dp(28)));

        SeekBar tolerance = new SeekBar(this);
        tolerance.setMax(120);
        tolerance.setProgress(45);
        tolerance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int value = Math.max(5, progress);
                editor.setTolerance(value);
                toleranceInfo.setText("Tolerancia varita: " + value);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(tolerance, new LinearLayout.LayoutParams(-1, dp(44)));
    }

    private void openImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        if (request != PICK_IMAGE || result != RESULT_OK || data == null || data.getData() == null) return;

        try {
            Bitmap bitmap = loadBitmap(data.getData());
            if (bitmap == null) {
                toast("No se pudo abrir la imagen.");
                return;
            }

            editor.setImage(bitmap);
            sizeInfo.setText("Tamaño original: " + bitmap.getWidth() + " × " + bitmap.getHeight());
            toast("Imagen cargada.");
        } catch (Exception e) {
            toast("Error al abrir imagen.");
        }
    }

    private Bitmap loadBitmap(Uri uri) throws Exception {
        InputStream input = getContentResolver().openInputStream(uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
        if (input != null) input.close();
        if (bitmap == null) return null;
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    private void savePng() {
        if (!editor.hasImage()) {
            toast("Primero abre una imagen.");
            return;
        }

        try {
            Bitmap outputBitmap = editor.getOutputBitmap();

            String fileName = "ImageZeta_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) +
                    ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Image Zeta Background Remover");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                toast("No se pudo crear PNG.");
                return;
            }

            OutputStream output = getContentResolver().openOutputStream(uri);
            outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            if (output != null) output.close();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
            }

            toast("PNG guardado sin cambiar tamaño.");
        } catch (Exception e) {
            toast("Error al guardar PNG.");
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
                               }
