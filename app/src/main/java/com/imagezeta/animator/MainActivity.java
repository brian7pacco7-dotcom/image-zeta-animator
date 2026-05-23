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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PICK_EXTRACT = 100;
    private static final int PICK_ANIMATE = 200;

    private final int BLUE = Color.rgb(20, 105, 245);
    private final int DARK = Color.rgb(12, 24, 54);
    private final int TEXT = Color.rgb(55, 65, 85);
    private final int SOFT = Color.rgb(246, 249, 255);
    private final int PURPLE = Color.rgb(130, 70, 255);

    private LinearLayout root;
    private ExtractView extractView;
    private MotionView motionView;
    private TextView sizeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
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
        t.setIncludeFontPadding(true);
        return t;
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private GradientDrawable border(int color, int stroke, int radius) {
        GradientDrawable g = bg(color, radius);
        g.setStroke(dp(1), stroke);
        return g;
    }

    private Button button(String s, int color, int textColor) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(textColor);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(bg(color, 16));
        return b;
    }

    private void page() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(18));
        root.setBackgroundColor(SOFT);

        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(scroll);
    }

    private View space(int w) {
        TextView s = new TextView(this);
        s.setWidth(dp(w));
        return s;
    }

    private TextView chip(String s) {
        TextView c = text(s, 12, BLUE, Typeface.BOLD);
        c.setGravity(Gravity.CENTER);
        c.setBackground(border(Color.WHITE, Color.rgb(210, 225, 255), 14));
        return c;
    }

    private void showHome() {
        page();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top);

        TextView menu = text("☰", 28, DARK, Typeface.BOLD);
        top.addView(menu);

        TextView title = text("  Image Zeta Animator", 20, DARK, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView pro = text("PRO", 13, Color.WHITE, Typeface.BOLD);
        pro.setGravity(Gravity.CENTER);
        pro.setBackground(bg(BLUE, 20));
        top.addView(pro, new LinearLayout.LayoutParams(dp(58), dp(34)));

        TextView sub = text("Prepara y da vida a tus imágenes con calidad profesional.", 15, Color.rgb(90, 100, 125), Typeface.NORMAL);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(20), 0, dp(12));
        root.addView(sub);

        root.addView(homeCard("Extraer fondo Pro+", "Quita el fondo sin perder tamaño ni calidad.", "PNG", BLUE, v -> showExtract()));
        root.addView(homeCard("Animar imagen o fondo", "Crea movimiento fácil con puntos y anclas.", "▶", PURPLE, v -> showAnimate()));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));
        root.addView(row);

        row.addView(smallCard("Proyectos recientes", "Continúa donde lo dejaste.", "▣"), new LinearLayout.LayoutParams(0, dp(120), 1));
        row.addView(space(10));
        row.addView(smallCard("Sin marca de agua", "Exporta limpio y profesional.", "✓"), new LinearLayout.LayoutParams(0, dp(120), 1));

        root.addView(smallCard("100% calidad original", "Sin compresión. Tamaño y calidad intactos.", "HD"), new LinearLayout.LayoutParams(-1, dp(110)));

        TextView nav = text("Inicio        Proyectos        Ajustes", 13, BLUE, Typeface.BOLD);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, dp(18), 0, 0);
        root.addView(nav);
    }

    private LinearLayout homeCard(String title, String desc, String icon, int color, View.OnClickListener click) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(border(Color.WHITE, Color.rgb(225, 232, 245), 24));
        card.setOnClickListener(click);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(150));
        lp.setMargins(0, dp(8), 0, dp(10));
        card.setLayoutParams(lp);

        TextView ic = text(icon, 23, Color.WHITE, Typeface.BOLD);
        ic.setGravity(Gravity.CENTER);
        ic.setBackground(bg(color, 20));
        card.addView(ic, new LinearLayout.LayoutParams(dp(86), dp(86)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(16), 0, dp(8), 0);
        info.addView(text(title, 19, color, Typeface.BOLD));

        TextView d = text(desc, 14, TEXT, Typeface.NORMAL);
        d.setPadding(0, dp(6), 0, 0);
        info.addView(d);

        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        TextView arrow = text("➜", 24, Color.WHITE, Typeface.BOLD);
        arrow.setGravity(Gravity.CENTER);
        arrow.setBackground(bg(color, 28));
        card.addView(arrow, new LinearLayout.LayoutParams(dp(46), dp(46)));

        return card;
    }

    private LinearLayout smallCard(String title, String desc, String icon) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.setBackground(border(Color.WHITE, Color.rgb(225, 232, 245), 18));

        c.addView(text(icon, 18, BLUE, Typeface.BOLD));

        TextView t = text(title, 15, DARK, Typeface.BOLD);
        t.setPadding(0, dp(8), 0, 0);
        c.addView(t);

        TextView d = text(desc, 12, Color.rgb(95, 105, 130), Typeface.NORMAL);
        d.setPadding(0, dp(5), 0, 0);
        c.addView(d);

        return c;
    }

    private void showExtract() {
        page();

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(bar);

        TextView back = text("‹", 34, DARK, Typeface.BOLD);
        back.setOnClickListener(v -> showHome());
        bar.addView(back, new LinearLayout.LayoutParams(dp(42), -2));

        TextView title = text("Extraer fondo", 20, DARK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView help = text("?", 18, DARK, Typeface.BOLD);
        help.setGravity(Gravity.CENTER);
        help.setBackground(border(Color.WHITE, Color.rgb(220, 230, 245), 30));
        bar.addView(help, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, dp(14), 0, dp(12));
        root.addView(chips);

        sizeText = chip("Tamaño original: abre imagen");
        chips.addView(sizeText, new LinearLayout.LayoutParams(0, dp(42), 1));
        chips.addView(space(8));
        chips.addView(chip("Calidad: original"), new LinearLayout.LayoutParams(0, dp(42), 1));

        extractView = new ExtractView();
        extractView.setBackground(border(Color.WHITE, Color.rgb(220, 230, 245), 18));
        root.addView(extractView, new LinearLayout.LayoutParams(-1, dp(410)));

        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setPadding(0, dp(14), 0, dp(8));
        root.addView(modes);

        modes.addView(button("Selección automática", Color.WHITE, BLUE), new LinearLayout.LayoutParams(0, dp(50), 1));
        modes.addView(space(8));
        modes.addView(button("Corrección manual", Color.WHITE, DARK), new LinearLayout.LayoutParams(0, dp(50), 1));

        addToolRow("Auto", "Punto", "Borde");
        addToolRow("Borrador suave", "Borrador duro", "Precisión");
        addToolRow("Restaurar", "Zoom", "");

        Button open = button("Abrir imagen", Color.WHITE, BLUE);
        open.setOnClickListener(v -> pickImage(PICK_EXTRACT));
        root.addView(open, new LinearLayout.LayoutParams(-1, dp(52)));

        Button export = button("Exportar PNG transparente", BLUE, Color.WHITE);
        LinearLayout.LayoutParams exp = new LinearLayout.LayoutParams(-1, dp(58));
        exp.setMargins(0, dp(10), 0, dp(8));
        root.addView(export, exp);
        export.setOnClickListener(v -> savePng());

        TextView ok = text("✓ Sin pérdida de resolución", 13, Color.rgb(20, 150, 85), Typeface.BOLD);
        ok.setGravity(Gravity.CENTER);
        root.addView(ok);
    }

    private void addToolRow(String a, String b, String c) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));
        root.addView(row);

        row.addView(tool(a), new LinearLayout.LayoutParams(0, dp(70), 1));
        row.addView(space(8));
        row.addView(tool(b), new LinearLayout.LayoutParams(0, dp(70), 1));

        if (!c.isEmpty()) {
            row.addView(space(8));
            row.addView(tool(c), new LinearLayout.LayoutParams(0, dp(70), 1));
        }
    }

    private Button tool(String name) {
        Button b = button(name, Color.WHITE, DARK);
        b.setTextSize(11);

        b.setOnClickListener(v -> {
            if (extractView == null) return;

            if (name.equals("Auto")) extractView.autoRemove();
            else if (name.equals("Punto")) extractView.setTool(true, 22);
            else if (name.equals("Borde")) extractView.setTool(true, 12);
            else if (name.equals("Borrador suave")) extractView.setTool(true, 54);
            else if (name.equals("Borrador duro")) extractView.setTool(true, 30);
            else if (name.equals("Precisión")) extractView.setTool(true, 9);
            else if (name.equals("Restaurar")) extractView.setTool(false, 35);
            else if (name.equals("Zoom")) extractView.changeZoom();
        });

        return b;
    }

    private void showAnimate() {
        page();

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(bar);

        TextView back = text("‹", 34, DARK, Typeface.BOLD);
        back.setOnClickListener(v -> showHome());
        bar.addView(back, new LinearLayout.LayoutParams(dp(42), -2));

        TextView title = text("Animar", 20, DARK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView menu = text("⋮", 28, DARK, Typeface.BOLD);
        menu.setGravity(Gravity.CENTER);
        bar.addView(menu, new LinearLayout.LayoutParams(dp(42), -2));

        LinearLayout tools = new LinearLayout(this);
        tools.setPadding(0, dp(12), 0, dp(10));
        root.addView(tools);

        tools.addView(animLabel("Movimiento"), new LinearLayout.LayoutParams(0, dp(58), 1));
        tools.addView(animLabel("Puntos"), new LinearLayout.LayoutParams(0, dp(58), 1));
        tools.addView(animLabel("Ancla"), new LinearLayout.LayoutParams(0, dp(58), 1));
        tools.addView(animLabel("Velocidad"), new LinearLayout.LayoutParams(0, dp(58), 1));
        tools.addView(animLabel("Máscara"), new LinearLayout.LayoutParams(0, dp(58), 1));

        motionView = new MotionView();
        motionView.setBackground(bg(Color.WHITE, 18));
        root.addView(motionView, new LinearLayout.LayoutParams(-1, dp(430)));

        Button open = button("Abrir fondo o imagen", Color.WHITE, BLUE);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, dp(52));
        op.setMargins(0, dp(12), 0, dp(8));
        root.addView(open, op);
        open.setOnClickListener(v -> pickImage(PICK_ANIMATE));

        TextView timeline = text("▶   00:03 / 00:10      ━━━━━●━━━━", 14, DARK, Typeface.BOLD);
        timeline.setGravity(Gravity.CENTER);
        timeline.setBackground(border(Color.WHITE, Color.rgb(220, 230, 245), 16));
        root.addView(timeline, new LinearLayout.LayoutParams(-1, dp(55)));

        TextView speed = text("Velocidad global     ━━━━━●━━     1.00x", 13, TEXT, Typeface.NORMAL);
        speed.setPadding(0, dp(14), 0, dp(8));
        root.addView(speed);

        LinearLayout exp = new LinearLayout(this);
        exp.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(exp);

        Button export = button("Exportar video", BLUE, Color.WHITE);
        export.setOnClickListener(v -> Toast.makeText(this, "Exportación MP4/GIF se agregará en la siguiente versión.", Toast.LENGTH_LONG).show());
        exp.addView(export, new LinearLayout.LayoutParams(0, dp(58), 1));
        exp.addView(space(8));
        exp.addView(button("MP4", Color.WHITE, BLUE), new LinearLayout.LayoutParams(dp(70), dp(58)));
        exp.addView(space(8));
        exp.addView(button("GIF", Color.WHITE, DARK), new LinearLayout.LayoutParams(dp(70), dp(58)));
    }

    private TextView animLabel(String s) {
        TextView v = text(s, 10, BLUE, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setBackground(border(Color.WHITE, Color.rgb(220, 230, 245), 14));
        return v;
    }

    private void pickImage(int request) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i, request);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        if (result != RESULT_OK || data == null || data.getData() == null) return;

        try {
            Bitmap bitmap = loadBitmap(data.getData());

            if (bitmap == null) {
                Toast.makeText(this, "No se pudo abrir la imagen.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (request == PICK_EXTRACT && extractView != null) {
                extractView.setBitmap(bitmap);
                if (sizeText != null) {
                    sizeText.setText("Tamaño original: " + bitmap.getWidth() + " × " + bitmap.getHeight());
                }
            }

            if (request == PICK_ANIMATE && motionView != null) {
                motionView.setBitmap(bitmap);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        if (extractView == null || extractView.getBitmap() == null) {
            Toast.makeText(this, "Primero abre una imagen.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap outputBitmap = extractView.getBitmap();
            String fileName = "ImageZeta_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Image Zeta Animator");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("No se pudo crear el archivo PNG.");

            OutputStream output = getContentResolver().openOutputStream(uri);
            outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            if (output != null) output.close();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
            }

            Toast.makeText(this, "PNG guardado sin cambiar tamaño original.", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar PNG: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public class ExtractView extends View {

        private Bitmap original;
        private Bitmap work;
        private Canvas workCanvas;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix matrix = new Matrix();
        private final Matrix inverse = new Matrix();

        private boolean eraser = true;
        private float brush = 30f;
        private float zoom = 1f;

        public ExtractView() {
            super(MainActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            clearPaint.setStyle(Paint.Style.FILL);
            clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }

        public void setBitmap(Bitmap bitmap) {
            original = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            work = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            workCanvas = new Canvas(work);
            zoom = 1f;
            invalidate();
    
