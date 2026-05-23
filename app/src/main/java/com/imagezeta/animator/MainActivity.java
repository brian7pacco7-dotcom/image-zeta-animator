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

    private static final int PICK_CUT = 10;
    private static final int PICK_ANIM = 20;

    private final int BLUE = Color.rgb(20, 105, 245);
    private final int DARK = Color.rgb(12, 24, 54);
    private final int PURPLE = Color.rgb(130, 70, 255);
    private final int BG = Color.rgb(246, 249, 255);
    private final int TEXT = Color.rgb(55, 65, 85);

    private LinearLayout root;
    private CutView cutView;
    private AnimView animView;
    private TextView sizeChip;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        showHome();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView txt(String s, int sp, int color, int style) {
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

    private GradientDrawable border(int color, int radius) {
        GradientDrawable g = bg(color, radius);
        g.setStroke(dp(1), Color.rgb(220, 230, 245));
        return g;
    }

    private Button btn(String s, int color, int txtColor) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(txtColor);
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
        root.setBackgroundColor(BG);

        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(scroll);
    }

    private View space(int w) {
        TextView v = new TextView(this);
        v.setWidth(dp(w));
        return v;
    }

    private TextView chip(String s) {
        TextView c = txt(s, 12, BLUE, Typeface.BOLD);
        c.setGravity(Gravity.CENTER);
        c.setBackground(border(Color.WHITE, 14));
        return c;
    }

    private void showHome() {
        page();

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top);

        top.addView(txt("☰", 28, DARK, Typeface.BOLD));
        top.addView(txt("  Image Zeta Animator", 20, DARK, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, -2, 1));

        TextView pro = txt("PRO", 13, Color.WHITE, Typeface.BOLD);
        pro.setGravity(Gravity.CENTER);
        pro.setBackground(bg(BLUE, 20));
        top.addView(pro, new LinearLayout.LayoutParams(dp(58), dp(34)));

        TextView sub = txt("Prepara y da vida a tus imágenes con calidad profesional.", 15, Color.rgb(90, 100, 125), Typeface.NORMAL);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(20), 0, dp(14));
        root.addView(sub);

        root.addView(bigCard("Extraer fondo Pro+", "Quita el fondo sin perder tamaño ni calidad.", "PNG", BLUE, v -> showCut()));
        root.addView(bigCard("Animar imagen o fondo", "Crea movimiento fácil con puntos y anclas.", "▶", PURPLE, v -> showAnim()));

        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(8), 0, dp(8));
        root.addView(row);

        row.addView(smallCard("Proyectos recientes", "Continúa donde lo dejaste.", "▣"),
                new LinearLayout.LayoutParams(0, dp(120), 1));
        row.addView(space(10));
        row.addView(smallCard("Sin marca de agua", "Exporta limpio y profesional.", "✓"),
                new LinearLayout.LayoutParams(0, dp(120), 1));

        root.addView(smallCard("100% calidad original", "Sin compresión. Tamaño y calidad intactos.", "HD"),
                new LinearLayout.LayoutParams(-1, dp(110)));

        TextView nav = txt("Inicio        Proyectos        Ajustes", 13, BLUE, Typeface.BOLD);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, dp(18), 0, 0);
        root.addView(nav);
    }

    private LinearLayout bigCard(String title, String desc, String icon, int color, View.OnClickListener click) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(border(Color.WHITE, 24));
        card.setOnClickListener(click);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(150));
        lp.setMargins(0, dp(8), 0, dp(10));
        card.setLayoutParams(lp);

        TextView ic = txt(icon, 22, Color.WHITE, Typeface.BOLD);
        ic.setGravity(Gravity.CENTER);
        ic.setBackground(bg(color, 20));
        card.addView(ic, new LinearLayout.LayoutParams(dp(86), dp(86)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(16), 0, dp(8), 0);
        info.addView(txt(title, 19, color, Typeface.BOLD));

        TextView d = txt(desc, 14, TEXT, Typeface.NORMAL);
        d.setPadding(0, dp(6), 0, 0);
        info.addView(d);

        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        TextView ar = txt("➜", 24, Color.WHITE, Typeface.BOLD);
        ar.setGravity(Gravity.CENTER);
        ar.setBackground(bg(color, 28));
        card.addView(ar, new LinearLayout.LayoutParams(dp(46), dp(46)));

        return card;
    }

    private LinearLayout smallCard(String title, String desc, String icon) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.setBackground(border(Color.WHITE, 18));
        c.addView(txt(icon, 18, BLUE, Typeface.BOLD));

        TextView t = txt(title, 15, DARK, Typeface.BOLD);
        t.setPadding(0, dp(8), 0, 0);
        c.addView(t);

        TextView d = txt(desc, 12, Color.rgb(95, 105, 130), Typeface.NORMAL);
        d.setPadding(0, dp(5), 0, 0);
        c.addView(d);

        return c;
    }

    private void showCut() {
        page();

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(bar);

        TextView back = txt("‹", 34, DARK, Typeface.BOLD);
        back.setOnClickListener(v -> showHome());
        bar.addView(back, new LinearLayout.LayoutParams(dp(42), -2));

        TextView title = txt("Extraer fondo", 20, DARK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(txt("?", 20, DARK, Typeface.BOLD), new LinearLayout.LayoutParams(dp(36), -2));

        LinearLayout chips = new LinearLayout(this);
        chips.setPadding(0, dp(14), 0, dp(12));
        root.addView(chips);

        sizeChip = chip("Tamaño original: abre imagen");
        chips.addView(sizeChip, new LinearLayout.LayoutParams(0, dp(42), 1));
        chips.addView(space(8));
        chips.addView(chip("Calidad: original"), new LinearLayout.LayoutParams(0, dp(42), 1));

        cutView = new CutView();
        cutView.setBackground(border(Color.WHITE, 18));
        root.addView(cutView, new LinearLayout.LayoutParams(-1, dp(410)));

        LinearLayout mode = new LinearLayout(this);
        mode.setPadding(0, dp(14), 0, dp(8));
        root.addView(mode);
        mode.addView(btn("Selección automática", Color.WHITE, BLUE), new LinearLayout.LayoutParams(0, dp(50), 1));
        mode.addView(space(8));
        mode.addView(btn("Corrección manual", Color.WHITE, DARK), new LinearLayout.LayoutParams(0, dp(50), 1));

        addToolRow("Auto", "Punto", "Borde");
        addToolRow("Borrador suave", "Borrador duro", "Precisión");
        addToolRow("Restaurar", "Zoom", "");

        Button open = btn("Abrir imagen", Color.WHITE, BLUE);
        open.setOnClickListener(v -> pickImage(PICK_CUT));
        root.addView(open, new LinearLayout.LayoutParams(-1, dp(52)));

        Button export = btn("Exportar PNG transparente", BLUE, Color.WHITE);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, dp(58));
        ep.setMargins(0, dp(10), 0, dp(8));
        root.addView(export, ep);
        export.setOnClickListener(v -> savePng());

        TextView ok = txt("✓ Sin pérdida de resolución", 13, Color.rgb(20, 150, 85), Typeface.BOLD);
        ok.setGravity(Gravity.CENTER);
        root.addView(ok);
    }

    private void addToolRow(String a, String b, String c) {
        LinearLayout row = new LinearLayout(this);
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
        Button b = btn(name, Color.WHITE, DARK);
        b.setTextSize(11);

        b.setOnClickListener(v -> {
            if (cutView == null) return;

            if (name.equals("Auto")) cutView.autoRemove();
            else if (name.equals("Punto")) cutView.setTool(true, 22);
            else if (name.equals("Borde")) cutView.setTool(true, 12);
            else if (name.equals("Borrador suave")) cutView.setTool(true, 54);
            else if (name.equals("Borrador duro")) cutView.setTool(true, 30);
            else if (name.equals("Precisión")) cutView.setTool(true, 9);
            else if (name.equals("Restaurar")) cutView.setTool(false, 35);
            else if (name.equals("Zoom")) cutView.changeZoom();
        });

        return b;
    }

    private void showAnim() {
        page();

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(bar);

        TextView back = txt("‹", 34, DARK, Typeface.BOLD);
        back.setOnClickListener(v -> showHome());
        bar.addView(back, new LinearLayout.LayoutParams(dp(42), -2));

        TextView title = txt("Animar", 20, DARK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(txt("⋮", 28, DARK, Typeface.BOLD), new LinearLayout.LayoutParams(dp(42), -2));

        LinearLayout tools = new LinearLayout(this);
        tools.setPadding(0, dp(12), 0, dp(10));
        root.addView(tools);

        tools.addView(label("Movimiento"), new LinearLayout.LayoutParams(0, dp(58), 1));
        tools.addView(label("Puntos"), new LinearLayout.LayoutParams(0, dp(58), 1));
        tools.addView(label("Ancla"), new LinearLayout.LayoutParams(0, dp(58), 1));
        tools.addView(label("Velocidad"), new LinearLayout.LayoutParams(0, dp(58), 1));
        tools.addView(label("Máscara"), new LinearLayout.LayoutParams(0, dp(58), 1));

        animView = new AnimView();
        animView.setBackground(bg(Color.WHITE, 18));
        root.addView(animView, new LinearLayout.LayoutParams(-1, dp(430)));

        Button open = btn("Abrir fondo o imagen", Color.WHITE, BLUE);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, dp(52));
        op.setMargins(0, dp(12), 0, dp(8));
        root.addView(open, op);
        open.setOnClickListener(v -> pickImage(PICK_ANIM));

        TextView timeline = txt("▶   00:03 / 00:10      ━━━━━●━━━━", 14, DARK, Typeface.BOLD);
        timeline.setGravity(Gravity.CENTER);
        timeline.setBackground(border(Color.WHITE, 16));
        root.addView(timeline, new LinearLayout.LayoutParams(-1, dp(55)));

        TextView speed = txt("Velocidad global     ━━━━━●━━     1.00x", 13, TEXT, Typeface.NORMAL);
        speed.setPadding(0, dp(14), 0, dp(8));
        root.addView(speed);

        LinearLayout exp = new LinearLayout(this);
        root.addView(exp);

        Button export = btn("Exportar video", BLUE, Color.WHITE);
        export.setOnClickListener(v -> Toast.makeText(this, "Exportación MP4/GIF se agregará en la siguiente versión.", Toast.LENGTH_LONG).show());
        exp.addView(export, new LinearLayout.LayoutParams(0, dp(58), 1));
        exp.addView(space(8));
        exp.addView(btn("MP4", Color.WHITE, BLUE), new LinearLayout.LayoutParams(dp(70), dp(58)));
        exp.addView(space(8));
        exp.addView(btn("GIF", Color.WHITE, DARK), new LinearLayout.LayoutParams(dp(70), dp(58)));
    }

    private TextView label(String s) {
        TextView v = txt(s, 10, BLUE, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setBackground(border(Color.WHITE, 14));
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
            if (bitmap == null) return;

            if (request == PICK_CUT && cutView != null) {
                cutView.setBitmap(bitmap);
                sizeChip.setText("Tamaño original: " + bitmap.getWidth() + " × " + bitmap.getHeight());
            }

            if (request == PICK_ANIM && animView != null) {
                animView.setBitmap(bitmap);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap loadBitmap(Uri uri) throws Exception {
        InputStream input = getContentResolver().openInputStream(uri);
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, opt);
        if (input != null) input.close();

        if (bitmap == null) return null;
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    private void savePng() {
        if (cutView == null || cutView.getBitmap() == null) {
            Toast.makeText(this, "Primero abre una imagen.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap out = cutView.getBitmap();
            String fileName = "ImageZeta_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Image Zeta Animator");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("No se pudo crear PNG.");

            OutputStream os = getContentResolver().openOutputStream(uri);
            out.compress(Bitmap.CompressFormat.PNG, 100, os);
            if (os != null) os.close();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
            }

            Toast.makeText(this, "PNG guardado sin cambiar tamaño original.", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public class CutView extends View {
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

        public CutView() {
            super(MainActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            clearPaint.setStyle(Paint.Style.FILL);
            clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }

        public void setBitmap(Bitmap b) {
            original = b.copy(Bitmap.Config.ARGB_8888, true);
            work = b.copy(Bitmap.Config.ARGB_8888, true);
            workCanvas = new Canvas(work);
            zoom = 1f;
            invalidate();
            Toast.makeText(MainActivity.this, "Imagen cargada en tamaño original.", Toast.LENGTH_SHORT).show();
        }

        public Bitmap getBitmap() {
            return work;
        }

        public void setTool(boolean erase, int size) {
            eraser = erase;
            brush = size;
            Toast.makeText(MainActivity.this, erase ? "Borrador activo" : "Restaurar activo", Toast.LENGTH_SHORT).show();
        }

        public void changeZoom() {
            zoom = zoom == 1f ? 2f : zoom == 2f ? 3f : 1f;
            Toast.makeText(MainActivity.this, "Zoom: " + zoom + "x", Toast.LENGTH_SHORT).show();
            invalidate();
        }

        public void autoRemove() {
            if (work == null) {
                Toast.makeText(MainActivity.this, "Primero abre una imagen.", Toast.LENGTH_SHORT).show();
                return;
            }

            int w = work.getWidth();
            int h = work.getHeight();

            int c1 = work.getPixel(0, 0);
            int c2 = work.getPixel(w - 1, 0);
            int c3 = work.getPixel(0, h - 1);
            int c4 = work.getPixel(w - 1, h - 1);

            int r = (Color.red(c1) + Color.red(c2) + Color.red(c3) + Color.red(c4)) / 4;
            int g = (Color.green(c1) + Color.green(c2) + Color.green(c3) + Color.gree
