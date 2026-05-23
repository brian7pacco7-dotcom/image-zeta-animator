package com.imagezeta.animator;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.Intent;
import android.content.ContentValues;
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
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ScaleGestureDetector;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PICK_EXTRACT_IMAGE = 101;
    private static final int PICK_ANIMATE_IMAGE = 102;

    private final int BLUE = Color.rgb(20, 105, 245);
    private final int DARK = Color.rgb(12, 24, 54);
    private final int TEXT = Color.rgb(30, 41, 59);
    private final int LIGHT = Color.rgb(245, 248, 255);
    private final int PURPLE = Color.rgb(130, 70, 255);

    private LinearLayout root;
    private ExtractEditorView extractEditor;
    private AnimationPreviewView animationPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(value);
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

    private GradientDrawable strokeBg(int color, int strokeColor, int radius, int strokeDp) {
        GradientDrawable g = bg(color, radius);
        g.setStroke(dp(strokeDp), strokeColor);
        return g;
    }

    private Button button(String label, int color, int textColor) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(textColor);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(bg(color, 16));
        b.setPadding(dp(10), dp(8), dp(10), dp(8));
        return b;
    }

    private TextView chip(String value) {
        TextView c = text(value, 12, BLUE, Typeface.BOLD);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(12), dp(8), dp(12), dp(8));
        c.setBackground(strokeBg(Color.WHITE, Color.rgb(210, 225, 255), 14, 1));
        return c;
    }

    private LinearLayout verticalRoot() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout base = new LinearLayout(this);
        base.setOrientation(LinearLayout.VERTICAL);
        base.setPadding(dp(16), dp(18), dp(16), dp(18));
        base.setBackgroundColor(Color.rgb(248, 250, 255));

        scroll.addView(base, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(scroll);
        root = base;
        return base;
    }

    private void showHome() {
        LinearLayout base = verticalRoot();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        base.addView(top, new LinearLayout.LayoutParams(-1, -2));

        TextView menu = text("☰", 28, DARK, Typeface.BOLD);
        top.addView(menu);

        TextView title = text("  Image Zeta Animator", 20, DARK, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView crown = text("♛", 24, Color.rgb(255, 170, 0), Typeface.BOLD);
        top.addView(crown);

        TextView sub = text("Prepara y da vida a tus imágenes con calidad profesional.", 15, Color.rgb(95, 105, 130), Typeface.NORMAL);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(18), 0, dp(16));
        base.addView(sub);

        LinearLayout card1 = homeBigCard("Extraer fondo Pro+", "Quita el fondo sin perder tamaño ni calidad.", "PNG", BLUE);
        card1.setOnClickListener(v -> showExtract());
        base.addView(card1);

        LinearLayout card2 = homeBigCard("Animar imagen o fondo", "Crea movimiento fácil con puntos, flechas y anclas.", "▶", PURPLE);
        card2.setOnClickListener(v -> showAnimate());
        base.addView(card2);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));
        base.addView(row);

        row.addView(smallCard("Proyectos recientes", "Continúa donde lo dejaste.", "▣"), new LinearLayout.LayoutParams(0, dp(120), 1));
        Space(row, 10);
        row.addView(smallCard("Sin marca de agua", "Exporta limpio y profesional.", "✓"), new LinearLayout.LayoutParams(0, dp(120), 1));

        LinearLayout quality = smallCard("100% calidad original", "Sin compresión. Tamaño y calidad intactos.", "HD");
        base.addView(quality, new LinearLayout.LayoutParams(-1, dp(105)));

        TextView footer = text("Inicio        Proyectos        Ajustes", 13, BLUE, Typeface.BOLD);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(16), 0, 0);
        base.addView(footer);
    }

    private LinearLayout homeBigCard(String title, String desc, String icon, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(strokeBg(Color.WHITE, Color.rgb(225, 232, 245), 24, 1));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(150));
        lp.setMargins(0, dp(8), 0, dp(10));
        card.setLayoutParams(lp);

        TextView ic = text(icon, 24, Color.WHITE, Typeface.BOLD);
        ic.setGravity(Gravity.CENTER);
        ic.setBackground(bg(color, 18));
        card.addView(ic, new LinearLayout.LayoutParams(dp(86), dp(86)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(16), 0, dp(8), 0);

        texts.addView(text(title, 20, color, Typeface.BOLD));
        TextView d = text(desc, 14, TEXT, Typeface.NORMAL);
        d.setPadding(0, dp(6), 0, 0);
        texts.addView(d);

        card.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView arrow = text("➜", 24, Color.WHITE, Typeface.BOLD);
        arrow.setGravity(Gravity.CENTER);
        arrow.setBackground(bg(color, 40));
        card.addView(arrow, new LinearLayout.LayoutParams(dp(46), dp(46)));

        return card;
    }

    private LinearLayout smallCard(String title, String desc, String icon) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.setBackground(strokeBg(Color.WHITE, Color.rgb(225, 232, 245), 18, 1));
        TextView ic = text(icon, 18, BLUE, Typeface.BOLD);
        c.addView(ic);
        TextView tt = text(title, 15, DARK, Typeface.BOLD);
        tt.setPadding(0, dp(8), 0, 0);
        c.addView(tt);
        TextView dd = text(desc, 12, Color.rgb(95, 105, 130), Typeface.NORMAL);
        dd.setPadding(0, dp(5), 0, 0);
        c.addView(dd);
        return c;
    }

    private void Space(LinearLayout row, int w) {
        TextView s = new TextView(this);
        row.addView(s, new LinearLayout.LayoutParams(dp(w), 1));
    }

    private void showExtract() {
        LinearLayout base = verticalRoot();

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        base.addView(bar);

        TextView back = text("‹", 34, DARK, Typeface.BOLD);
        back.setOnClickListener(v -> showHome());
        bar.addView(back, new LinearLayout.LayoutParams(dp(45), -2));

        TextView title = text("Extraer fondo", 20, DARK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView help = text("?", 18, DARK, Typeface.BOLD);
        help.setGravity(Gravity.CENTER);
        help.setBackground(strokeBg(Color.WHITE, Color.rgb(220, 230, 245), 30, 1));
        bar.addView(help, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, dp(14), 0, dp(12));
        base.addView(chips);

        TextView sizeChip = chip("Tamaño original: esperando imagen");
        chips.addView(sizeChip, new LinearLayout.LayoutParams(0, -2, 1));
        Space(chips, 8);
        chips.addView(chip("Calidad: original"), new LinearLayout.LayoutParams(0, -2, 1));

        extractEditor = new ExtractEditorView(this, sizeChip);
        extractEditor.setBackground(strokeBg(Color.WHITE, Color.rgb(220, 230, 245), 18, 1));
        base.addView(extractEditor, new LinearLayout.LayoutParams(-1, dp(410)));

        LinearLayout mode = new LinearLayout(this);
        mode.setOrientation(LinearLayout.HORIZONTAL);
        mode.setPadding(0, dp(14), 0, dp(8));
        base.addView(mode);

        Button automatic = button("Selección automática", Color.WHITE, BLUE);
        Button manual = button("Corrección manual", Color.WHITE, DARK);
        mode.addView(automatic, new LinearLayout.LayoutParams(0, dp(48), 1));
        Space(mode, 8);
        mode.addView(manual, new LinearLayout.LayoutParams(0, dp(48), 1));

        LinearLayout tools1 = new LinearLayout(this);
        tools1.setOrientation(LinearLayout.HORIZONTAL);
        base.addView(tools1);

        tools1.addView(tool("Auto", "✦", () -> extractEditor.autoRemoveBackground()), new LinearLayout.LayoutParams(0, dp(82), 1));
        Space(tools1, 8);
        tools1.addView(tool("Punto", "•", () -> extractEditor.setEraseMode(true, 34)), new LinearLayout.LayoutParams(0, dp(82), 1));
        Space(tools1, 8);
        tools1.addView(tool("Borde", "□", () -> extractEditor.setEraseMode(true, 18)), new LinearLayout.LayoutParams(0, dp(82), 1));

        LinearLayout tools2 = new LinearLayout(this);
        tools2.setOrientation(LinearLayout.HORIZONTAL);
        tools2.setPadding(0, dp(8), 0, 0);
        base.addView(tools2);

        tools2.addView(tool("Borrador suave", "⌁", () -> extractEditor.setEraseMode(true, 55)), new LinearLayout.LayoutParams(0, dp(82), 1));
        Space(tools2, 8);
        tools2.addView(tool("Borrador duro", "◆", () -> extractEditor.setEraseMode(true, 28)), new LinearLayout.LayoutParams(0, dp(82), 1));
        Space(tools2, 8);
        tools2.addView(tool("Precisión", "⌖", () -> extractEditor.setEraseMode(true, 10)), new LinearLayout.LayoutParams(0, dp(82), 1));

        LinearLayout tools3 = new LinearLayout(this);
        tools3.setOrientation(LinearLayout.HORIZONTAL);
        tools3.setPadding(0, dp(8), 0, dp(12));
        base.addView(tools3);

        tools3.addView(tool("Restaurar", "↶", () -> extractEditor.setEraseMode(false, 34)), new LinearLayout.LayoutParams(0, dp(72), 1));
        Space(tools3, 8);
        tools3.addView(tool("Zoom", "⌕", () -> extractEditor.toggleZoomMode()), new LinearLayout.LayoutParams(0, dp(72), 1));

        Button open = button("Abrir imagen", Color.WHITE, BLUE);
        open.setOnClickListener(v -> pickImage(PICK_EXTRACT_IMAGE));
        base.addView(open, new LinearLayout.LayoutParams(-1, dp(52)));

        Button export = button("Exportar PNG transparente", BLUE, Color.WHITE);
        LinearLayout.LayoutParams exlp = new LinearLayout.LayoutParams(-1, dp(58));
        exlp.setMargins(0, dp(10), 0, dp(8));
        base.addView(export, exlp);
        export.setOnClickListener(v -> savePng());

        TextView ok = text("✓ Sin pérdida de resolución", 13, Color.rgb(20, 150, 85), Typeface.BOLD);
        ok.setGravity(Gravity.CENTER);
        base.addView(ok);
    }

    private View tool(String label, String icon, final Runnable action) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(4), dp(6), dp(4), dp(6));
        t.setBackground(strokeBg(Color.WHITE, Color.rgb(220, 230, 245), 16, 1));

        TextView ic = text(icon, 24, BLUE, Typeface.BOLD);
        ic.setGravity(Gravity.CENTER);
        t.addView(ic);

        TextView l = text(label, 11, DARK, Typeface.BOLD);
        l.setGravity(Gravity.CENTER);
        t.addView(l);

        t.setOnClickListener(v -> action.run());
        return t;
    }

    private void showAnimate() {
        LinearLayout base = verticalRoot();

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        base.addView(bar);

        TextView back = text("‹", 34, DARK, Typeface.BOLD);
        back.setOnClickListener(v -> showHome());
        bar.addView(back, new LinearLayout.LayoutParams(dp(45), -2));

        TextView title = text("Animar", 20, DARK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView menu = text("⋮", 26, DARK, Typeface.BOLD);
        menu.setGravity(Gravity.CENTER);
        bar.addView(menu, new LinearLayout.LayoutParams(dp(42), -2));

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setPadding(0, dp(12), 0, dp(10));
        base.addView(tools);

        tools.addView(animTool("Movimiento"), new LinearLayout.LayoutParams(0, dp(58), 1));
        tools.addView(animTool("Puntos"), new LinearLayout.LayoutParams(0, dp(58), 1));
        tools.addView(animTool("Ancla"), new LinearLayout.LayoutParams(0, dp(58), 1));
        tools.addView(animTool("Velocidad"), new LinearLayout.LayoutParams(0, dp(58), 1));
        tools.addView(animTool("Máscara"), new LinearLayout.LayoutParams(0, dp(58), 1));

        animationPreview = new AnimationPreviewView(this);
        animationPreview.setBackgroundColor(Color.WHITE);
        base.addView(animationPreview, new LinearLayout.LayoutParams(-1, dp(430)));

        Button open = button("Abrir fondo o imagen", Color.WHITE, BLUE);
        open.setOnClickListener(v -> pickImage(PICK_ANIMATE_IMAGE));
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(-1, dp(52));
        olp.setMargins(0, dp(12), 0, dp(8));
        base.addView(open, olp);

        TextView timeline = text("▶   00:03 / 00:10     ━━━━━●━━━━", 14, DARK, Typeface.BOLD);
        timeline.setGravity(Gravity.CENTER);
        timeline.setPadding(0, dp(8), 0, dp(8));
        timeline.setBackground(strokeBg(Color.WHITE, Color.rgb(220, 230, 245), 16, 1));
        base.addView(timeline, new LinearLayout.LayoutParams(-1, dp(55)));

        TextView speed = text("Velocidad global    ━━━━━●━━    1.00x", 13, TEXT, Typeface.NORMAL);
        speed.setPadding(0, dp(14), 0, dp(8));
        base.addView(speed);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        base.addView(row);

        Button export = button("Exportar video", BLUE, Color.WHITE);
        export.setOnClickListener(v -> Toast.makeText(this, "Motor MP4/GIF se agregará en el siguiente bloque.", Toast.LENGTH_LONG).show());
        row.addView(export, new LinearLayout.LayoutParams(0, dp(58), 1));

        Space(row, 8);
        Button mp4 = button("MP4", Color.WHITE, BLUE);
        row.addView(mp4, new LinearLayout.LayoutParams(dp(70), dp(58)));

        Space(row, 8);
        Button gif = button("GIF", Color.WHITE, DARK);
        row.addView(gif, new LinearLayout.LayoutParams(dp(70), dp(58)));
    }

    private TextView animTool(String label) {
        TextView v = text(label, 10, BLUE, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setBackground(strokeBg(Color.WHITE, Color.rgb(220, 230, 245), 14, 1));
        return v;
    }

    private void pickImage(int request) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        try {
            Uri uri = data.getData();
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            Bitmap bitmap = loadBitmap(uri);
            if (bitmap == null) {
                Toast.makeText(this, "No se pudo abrir la imagen.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (requestCode == PICK_EXTRACT_IMAGE && extractEditor != null) {
                extractEditor.setBitmap(bitmap);
            }

            if (requestCode == PICK_ANIMATE_IMAGE && animationPreview != null) {
                animationPreview.setBitmap(bitmap);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap loadBitmap(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap b = BitmapFactory.decodeStream(is, null, options);
        if (is != null) is.close();

        if (b == null) return null;

        if (b.getConfig() != Bitmap.Config.ARGB_8888) {
            b = b.copy(Bitmap.Config.ARGB_8888, true);
        } else if (!b.isMutable()) {
            b = b.copy(Bitmap.Config.ARGB_8888, true);
        }

        return b;
    }

    private void savePng() {
        if (extractEditor == null || extractEditor.getBitmap() == null) {
            Toast.makeText(this, "Primero abre una imagen.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap outBitmap = extractEditor.getBitmap();
            String name = "ImageZeta_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Image Zeta Animator");
               
