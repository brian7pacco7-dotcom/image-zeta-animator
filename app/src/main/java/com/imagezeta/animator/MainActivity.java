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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE = 100;

    private final int BLUE = Color.rgb(20, 105, 245);
    private final int DARK = Color.rgb(12, 24, 54);
    private final int BG = Color.rgb(246, 249, 255);
    private final int TEXT = Color.rgb(55, 65, 85);

    private LinearLayout root;
    private CutEditorView editor;
    private TextView sizeInfo;
    private TextView brushInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showEditor();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(value);
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

    private GradientDrawable cardBg() {
        GradientDrawable g = bg(Color.WHITE, 20);
        g.setStroke(dp(1), Color.rgb(220, 230, 245));
        return g;
    }

    private Button button(String label, int color, int textColor) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(textColor);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(bg(color, 16));
        return b;
    }

    private TextView chip(String value) {
        TextView c = text(value, 12, BLUE, Typeface.BOLD);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(10), dp(8), dp(10), dp(8));
        c.setBackground(cardBg());
        return c;
    }

    private void showEditor() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(18), dp(14), dp(18));
        root.setBackgroundColor(BG);

        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(scroll);

        TextView title = text("Image Zeta Background Remover", 21, DARK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = text("Extrae fondo, corrige bordes y exporta PNG sin perder tamaño.", 14, TEXT, Typeface.NORMAL);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(6), 0, dp(14));
        root.addView(subtitle);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, 0, 0, dp(10));
        root.addView(chips);

        sizeInfo = chip("Tamaño original: abre imagen");
        chips.addView(sizeInfo, new LinearLayout.LayoutParams(0, dp(46), 1));

        TextView quality = chip("Calidad: original");
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(0, dp(46), 1);
        qlp.setMargins(dp(8), 0, 0, 0);
        chips.addView(quality, qlp);

        editor = new CutEditorView(this);
        editor.setBackground(cardBg());
        root.addView(editor, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(470)
        ));

        TextView help = text("Zoom: 2 dedos. Mover: 2 dedos. Borrar/restaurar: 1 dedo.", 12, TEXT, Typeface.BOLD);
        help.setGravity(Gravity.CENTER);
        help.setPadding(0, dp(8), 0, dp(8));
        root.addView(help);

        LinearLayout mainActions = new LinearLayout(this);
        mainActions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(mainActions);

        Button open = button("Abrir imagen", Color.WHITE, BLUE);
        open.setOnClickListener(v -> openImage());
        mainActions.addView(open, new LinearLayout.LayoutParams(0, dp(54), 1));

        Button auto = button("Auto quitar fondo", BLUE, Color.WHITE);
        auto.setOnClickListener(v -> {
            if (editor.hasImage()) {
                editor.autoRemoveBackground();
            } else {
                toast("Primero abre una imagen.");
            }
        });

        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(0, dp(54), 1);
        alp.setMargins(dp(8), 0, 0, 0);
        mainActions.addView(auto, alp);

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setPadding(0, dp(10), 0, 0);
        root.addView(tools);

        Button eraseSoft = button("Borrador suave", Color.WHITE, DARK);
        eraseSoft.setOnClickListener(v -> {
            editor.setMode(CutEditorView.MODE_ERASE_SOFT);
            toast("Borrador suave activo.");
        });
        tools.addView(eraseSoft, new LinearLayout.LayoutParams(0, dp(58), 1));

        Button eraseHard = button("Borrador duro", Color.WHITE, DARK);
        eraseHard.setOnClickListener(v -> {
            editor.setMode(CutEditorView.MODE_ERASE_HARD);
            toast("Borrador duro activo.");
        });

        LinearLayout.LayoutParams ehlp = new LinearLayout.LayoutParams(0, dp(58), 1);
        ehlp.setMargins(dp(8), 0, 0, 0);
        tools.addView(eraseHard, ehlp);

        Button restore = button("Restaurar", Color.WHITE, DARK);
        restore.setOnClickListener(v -> {
            editor.setMode(CutEditorView.MODE_RESTORE);
            toast("Restaurar activo.");
        });

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0, dp(58), 1);
        rlp.setMargins(dp(8), 0, 0, 0);
        tools.addView(restore, rlp);

        LinearLayout tools2 = new LinearLayout(this);
        tools2.setOrientation(LinearLayout.HORIZONTAL);
        tools2.setPadding(0, dp(10), 0, 0);
        root.addView(tools2);

        Button undo = button("Deshacer", Color.WHITE, DARK);
        undo.setOnClickListener(v -> editor.undo());
        tools2.addView(undo, new LinearLayout.LayoutParams(0, dp(54), 1));

        Button fit = button("Ajustar vista", Color.WHITE, DARK);
        fit.setOnClickListener(v -> editor.resetView());

        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, dp(54), 1);
        flp.setMargins(dp(8), 0, 0, 0);
        tools2.addView(fit, flp);

        brushInfo = text("Tamaño de pincel: 35", 13, DARK, Typeface.BOLD);
        brushInfo.setPadding(0, dp(14), 0, dp(4));
        root.addView(brushInfo);

        SeekBar brushBar = new SeekBar(this);
        brushBar.setMax(100);
        brushBar.setProgress(35);
        brushBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int size = Math.max(5, progress);
                editor.setBrushSize(size);
                brushInfo.setText("Tamaño de pincel: " + size);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        root.addView(brushBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));

        Button export = button("Exportar PNG transparente", BLUE, Color.WHITE);
        LinearLayout.LayoutParams xlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(62)
        );
        xlp.setMargins(0, dp(12), 0, 0);
        root.addView(export, xlp);
        export.setOnClickListener(v -> savePng());
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

        if (request != PICK_IMAGE || result != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        try {
            Bitmap bitmap = loadBitmap(data.getData());

            if (bitmap == null) {
                toast("No se pudo abrir la imagen.");
                return;
            }

            editor.setImage(bitmap);
            sizeInfo.setText("Tamaño original: " + bitmap.getWidth() + " × " + bitmap.getHeight());
            toast("Imagen cargada sin cambiar tamaño.");

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
                values.put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Image Zeta Background Remover"
                );
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri == null) {
                toast("No se pudo crear el PNG.");
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

            toast("PNG guardado sin perder tamaño original.");

        } catch (Exception e) {
            toast("Error al guardar PNG.");
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
                              }
