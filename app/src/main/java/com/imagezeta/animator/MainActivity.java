package com.imagezeta.animator;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE = 100;

    private final int DARK_BG = Color.rgb(42, 42, 42);
    private final int PANEL_BG = Color.argb(220, 25, 25, 25);
    private final int TOOL_BG = Color.argb(140, 70, 70, 70);
    private final int BLUE = Color.rgb(20, 105, 245);
    private final int WHITE = Color.WHITE;

    private FrameLayout root;
    private EditorCanvasView editor;

    private TextView zoomText;
    private TextView brushValue;
    private TextView toleranceValue;
    private TextView softnessValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildEditorScreen();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable roundBg(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private GradientDrawable circleBg(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        g.setStroke(dp(1), Color.argb(100, 255, 255, 255));
        return g;
    }

    private TextView label(String text, int sp, int color, int style) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT, style);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private Button iconButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(20);
        b.setTextColor(WHITE);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(circleBg(TOOL_BG));
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    private Button toolButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(11);
        b.setTextColor(WHITE);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(roundBg(Color.argb(120, 60, 60, 60), 12));
        return b;
    }

    private void buildEditorScreen() {
        root = new FrameLayout(this);
        root.setBackgroundColor(DARK_BG);
        setContentView(root);

        editor = new EditorCanvasView(this);
        editor.setBackgroundColor(DARK_BG);

        root.addView(editor, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        editor.setZoomListener(percent -> runOnUiThread(() -> {
            zoomText.setText(String.format(Locale.US, "%.1f%%", percent));
            zoomText.setVisibility(View.VISIBLE);
            zoomText.removeCallbacks(hideZoomRunnable);
            zoomText.postDelayed(hideZoomRunnable, 900);
        }));

        addTopBar();
        addRightPanel();
        addBottomBars();
    }

    private final Runnable hideZoomRunnable = new Runnable() {
        @Override
        public void run() {
            if (zoomText != null) {
                zoomText.setVisibility(View.GONE);
            }
        }
    };

    private void addTopBar() {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), dp(8), dp(8), dp(4));

        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
        );
        topLp.gravity = Gravity.TOP;
        root.addView(top, topLp);

        Button undo = iconButton("↶");
        undo.setOnClickListener(v -> editor.undo());
        top.addView(undo, new LinearLayout.LayoutParams(dp(48), dp(48)));

        Button redo = iconButton("↷");
        redo.setOnClickListener(v -> editor.redo());
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(dp(48), dp(48));
        rlp.setMargins(dp(8), 0, 0, 0);
        top.addView(redo, rlp);

        TextView spacer = new TextView(this);
        top.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        Button magic = iconButton("✦");
        magic.setOnClickListener(v -> {
            editor.setTool(EditorCanvasView.TOOL_MAGIC);
            toast("Varita mágica activa.");
        });
        top.addView(magic, new LinearLayout.LayoutParams(dp(48), dp(48)));

        Button select = iconButton("□");
        select.setOnClickListener(v -> toast("Selección manual se agregará después."));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(48), dp(48));
        slp.setMargins(dp(8), 0, 0, 0);
        top.addView(select, slp);

        Button hand = iconButton("☝");
        hand.setOnClickListener(v -> {
            editor.setTool(EditorCanvasView.TOOL_MOVE);
            toast("Mover/zoom activo.");
        });
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(48), dp(48));
        hlp.setMargins(dp(8), 0, 0, 0);
        top.addView(hand, hlp);

        Button importImg = iconButton("▣");
        importImg.setOnClickListener(v -> openImage());
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(48), dp(48));
        ilp.setMargins(dp(8), 0, 0, 0);
        top.addView(importImg, ilp);

        zoomText = label("100%", 18, WHITE, Typeface.BOLD);
        zoomText.setBackground(roundBg(Color.argb(210, 0, 0, 0), 8));
        zoomText.setVisibility(View.GONE);

        FrameLayout.LayoutParams zlp = new FrameLayout.LayoutParams(dp(150), dp(56));
        zlp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        zlp.topMargin = dp(12);
        root.addView(zoomText, zlp);
    }

    private void addRightPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8), dp(10), dp(8), dp(10));
        panel.setBackground(roundBg(PANEL_BG, 12));

        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(dp(150), ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        plp.rightMargin = dp(8);
        root.addView(panel, plp);

        TextView title = label("Herramienta", 15, WHITE, Typeface.BOLD);
        title.setGravity(Gravity.LEFT);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(32)));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        panel.addView(row1, new LinearLayout.LayoutParams(-1, dp(58)));

        Button soft = toolButton("Suave");
        soft.setOnClickListener(v -> {
            editor.setTool(EditorCanvasView.TOOL_ERASE_SOFT);
            toast("Borrador suave.");
        });
        row1.addView(soft, new LinearLayout.LayoutParams(0, -1, 1));

        Button hard = toolButton("Duro");
        hard.setOnClickListener(v -> {
            editor.setTool(EditorCanvasView.TOOL_ERASE_HARD);
            toast("Borrador duro.");
        });
        LinearLayout.LayoutParams hardLp = new LinearLayout.LayoutParams(0, -1, 1);
        hardLp.setMargins(dp(6), 0, 0, 0);
        row1.addView(hard, hardLp);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams row2Lp = new LinearLayout.LayoutParams(-1, dp(58));
        row2Lp.setMargins(0, dp(6), 0, dp(8));
        panel.addView(row2, row2Lp);

        Button restore = toolButton("Restaurar");
        restore.setBackground(roundBg(Color.argb(190, 20, 105, 245), 12));
        restore.setOnClickListener(v -> {
            editor.setTool(EditorCanvasView.TOOL_RESTORE);
            toast("Restaurar activo.");
        });
        row2.addView(restore, new LinearLayout.LayoutParams(0, -1, 1));

        Button wand = toolButton("Varita");
        wand.setOnClickListener(v -> {
            editor.setTool(EditorCanvasView.TOOL_MAGIC);
            toast("Varita mágica activa.");
        });
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(0, -1, 1);
        wlp.setMargins(dp(6), 0, 0, 0);
        row2.addView(wand, wlp);

        addPanelDivider(panel);

        brushValue = label("Tamaño: 35", 13, WHITE, Typeface.BOLD);
        brushValue.setGravity(Gravity.LEFT);
        panel.addView(brushValue, new LinearLayout.LayoutParams(-1, dp(30)));

        SeekBar brush = new SeekBar(this);
        brush.setMax(160);
        brush.setProgress(35);
        brush.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int value = Math.max(1, progress);
                editor.setBrushSize(value);
                brushValue.setText("Tamaño: " + value);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        panel.addView(brush, new LinearLayout.LayoutParams(-1, dp(42)));

        toleranceValue = label("Tolerancia: 60", 13, WHITE, Typeface.BOLD);
        toleranceValue.setGravity(Gravity.LEFT);
        panel.addView(toleranceValue, new LinearLayout.LayoutParams(-1, dp(30)));

        SeekBar tolerance = new SeekBar(this);
        tolerance.setMax(180);
        tolerance.setProgress(60);
        tolerance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int value = Math.max(5, progress);
                editor.setTolerance(value);
                toleranceValue.setText("Tolerancia: " + value);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        panel.addView(tolerance, new LinearLayout.LayoutParams(-1, dp(42)));

        softnessValue = label("Suavidad: 35%", 13, WHITE, Typeface.BOLD);
        softnessValue.setGravity(Gravity.LEFT);
        panel.addView(softnessValue, new LinearLayout.LayoutParams(-1, dp(30)));

        SeekBar softness = new SeekBar(this);
        softness.setMax(100);
        softness.setProgress(35);
        softness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                editor.setEdgeSoftness(progress);
                softnessValue.setText("Suavidad: " + progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        panel.addView(softness, new LinearLayout.LayoutParams(-1, dp(42)));

        addPanelDivider(panel);

        LinearLayout holesRow = new LinearLayout(this);
        holesRow.setOrientation(LinearLayout.HORIZONTAL);
        holesRow.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(holesRow, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView holesText = label("Huecos", 13, WHITE, Typeface.BOLD);
        holesText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        holesRow.addView(holesText, new LinearLayout.LayoutParams(0, -1, 1));

        Switch holesSwitch = new Switch(this);
        holesSwitch.setChecked(true);
        holesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> editor.setHoleRecognitionEnabled(isChecked));
        holesRow.addView(holesSwitch, new LinearLayout.LayoutParams(dp(58), -1));

        LinearLayout rowBottom = new LinearLayout(this);
        rowBottom.setOrientation(LinearLayout.HORIZONTAL);
        panel.addView(rowBottom, new LinearLayout.LayoutParams(-1, dp(44)));

        Button reset = toolButton("Reset");
        reset.setOnClickListener(v -> editor.resetView());
        rowBottom.addView(reset, new LinearLayout.LayoutParams(0, -1, 1));

        Button apply = toolButton("Aplicar");
        apply.setOnClickListener(v -> toast("Herramienta aplicada."));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(0, -1, 1);
        alp.setMargins(dp(6), 0, 0, 0);
        rowBottom.addView(apply, alp);
    }

    private void addPanelDivider(LinearLayout panel) {
        View line = new View(this);
        line.setBackgroundColor(Color.argb(80, 255, 255, 255));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.setMargins(0, dp(8), 0, dp(8));
        panel.addView(line, lp);
    }

    private void addBottomBars() {
        LinearLayout sliders = new LinearLayout(this);
        sliders.setOrientation(LinearLayout.VERTICAL);
        sliders.setPadding(dp(110), dp(4), dp(86), dp(0));
        sliders.setBackgroundColor(Color.argb(110, 0, 0, 0));

        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(-1, dp(82));
        slp.gravity = Gravity.BOTTOM;
        slp.bottomMargin = dp(62);
        root.addView(sliders, slp);

        TextView sizeLine = label("3.0px     −   ━━━━━━━━━━━━━━━   +", 16, WHITE, Typeface.BOLD);
        sliders.addView(sizeLine, new LinearLayout.LayoutParams(-1, dp(36)));

        TextView opacityLine = label("100       −   ░░░░░░░░████████   +", 16, WHITE, Typeface.BOLD);
        sliders.addView(opacityLine, new LinearLayout.LayoutParams(-1, dp(36)));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(dp(4), dp(4), dp(4), dp(4));
        bottom.setBackgroundColor(Color.argb(210, 65, 65, 65));

        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(-1, dp(62));
        blp.gravity = Gravity.BOTTOM;
        root.addView(bottom, blp);

        bottom.addView(bottomTool("Abrir", "▰", v -> openImage()), new LinearLayout.LayoutParams(0, -1, 1));
        bottom.addView(bottomTool("Borrador", "▰", v -> {
            editor.setTool(EditorCanvasView.TOOL_ERASE_SOFT);
            toast("Borrador activo.");
        }), new LinearLayout.LayoutParams(0, -1, 1));
        bottom.addView(bottomTool("Restaurar", "✎", v -> {
            editor.setTool(EditorCanvasView.TOOL_RESTORE);
            toast("Restaurar activo.");
        }), new LinearLayout.LayoutParams(0, -1, 1));
        bottom.addView(bottomTool("Tamaño", "3.0", v -> toast("Ajusta el tamaño en el panel.")), new LinearLayout.LayoutParams(0, -1, 1));
        bottom.addView(bottomTool("Varita", "✦", v -> {
            editor.setTool(EditorCanvasView.TOOL_MAGIC);
            toast("Varita mágica.");
        }), new LinearLayout.LayoutParams(0, -1, 1));
        bottom.addView(bottomTool("Guardar", "↓", v -> savePng()), new LinearLayout.LayoutParams(0, -1, 1));
        bottom.addView(bottomTool("Exportar", "⇧", v -> savePng()), new LinearLayout.LayoutParams(0, -1, 1));
    }

    private LinearLayout bottomTool(String label, String icon, View.OnClickListener listener) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setOnClickListener(listener);

        TextView i = this.label(icon, 20, WHITE, Typeface.BOLD);
        TextView l = this.label(label, 10, WHITE, Typeface.BOLD);

        box.addView(i, new LinearLayout.LayoutParams(-1, 30));
        box.addView(l, new LinearLayout.LayoutParams(-1, 22));

        return box;
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
            toast("Imagen cargada en tamaño original.");

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

        ImageExporter.savePng(
                this,
                editor.getOutputBitmap(),
                "Image Zeta Background Remover",
                success -> {
                    if (success) {
                        toast("PNG exportado sin cambiar tamaño.");
                    } else {
                        toast("No se pudo exportar PNG.");
                    }
                }
        );
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    }
