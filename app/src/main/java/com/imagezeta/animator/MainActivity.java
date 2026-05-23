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

    private final int DARK_BG = Color.rgb(48, 48, 48);
    private final int PANEL_BG = Color.argb(225, 20, 20, 20);
    private final int TOOL_BG = Color.argb(145, 70, 70, 70);
    private final int BLUE = Color.rgb(20, 105, 245);
    private final int WHITE = Color.WHITE;

    private FrameLayout root;
    private EditorCanvasView editor;

    private LinearLayout rightPanel;
    private Button panelToggle;

    private TextView zoomText;
    private TextView brushValue;
    private TextView toleranceValue;
    private TextView softnessValue;

    private Button softBtn;
    private Button hardBtn;
    private Button restoreBtn;
    private Button magicBtn;

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
            zoomText.postDelayed(hideZoomRunnable, 850);
        }));

        addTopBar();
        addRightPanel();
        addPanelToggle();
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
        magic.setOnClickListener(v -> selectTool(EditorCanvasView.TOOL_MAGIC));
        top.addView(magic, new LinearLayout.LayoutParams(dp(48), dp(48)));

        Button select = iconButton("□");
        select.setOnClickListener(v -> toast("Modo cuadro se agregará en el siguiente paso."));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(48), dp(48));
        slp.setMargins(dp(8), 0, 0, 0);
        top.addView(select, slp);

        Button hand = iconButton("☝");
        hand.setOnClickListener(v -> {
            editor.setTool(EditorCanvasView.TOOL_MOVE);
            resetToolButtons();
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

        Button export = iconButton("⇧");
        export.setOnClickListener(v -> savePng());
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(dp(48), dp(48));
        elp.setMargins(dp(8), 0, 0, 0);
        top.addView(export, elp);

        zoomText = label("100%", 18, WHITE, Typeface.BOLD);
        zoomText.setBackground(roundBg(Color.argb(215, 0, 0, 0), 8));
        zoomText.setVisibility(View.GONE);

        FrameLayout.LayoutParams zlp = new FrameLayout.LayoutParams(dp(150), dp(56));
        zlp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        zlp.topMargin = dp(12);
        root.addView(zoomText, zlp);
    }

    private void addRightPanel() {
        rightPanel = new LinearLayout(this);
        rightPanel.setOrientation(LinearLayout.VERTICAL);
        rightPanel.setPadding(dp(8), dp(10), dp(8), dp(10));
        rightPanel.setBackground(roundBg(PANEL_BG, 12));

        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(dp(152), ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        plp.rightMargin = dp(8);
        root.addView(rightPanel, plp);

        TextView title = label("Herramienta", 15, WHITE, Typeface.BOLD);
        title.setGravity(Gravity.LEFT);
        rightPanel.addView(title, new LinearLayout.LayoutParams(-1, dp(32)));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        rightPanel.addView(row1, new LinearLayout.LayoutParams(-1, dp(58)));

        softBtn = toolButton("Suave");
        softBtn.setOnClickListener(v -> selectTool(EditorCanvasView.TOOL_ERASE_SOFT));
        row1.addView(softBtn, new LinearLayout.LayoutParams(0, -1, 1));

        hardBtn = toolButton("Duro");
        hardBtn.setOnClickListener(v -> selectTool(EditorCanvasView.TOOL_ERASE_HARD));
        LinearLayout.LayoutParams hardLp = new LinearLayout.LayoutParams(0, -1, 1);
        hardLp.setMargins(dp(6), 0, 0, 0);
        row1.addView(hardBtn, hardLp);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams row2Lp = new LinearLayout.LayoutParams(-1, dp(58));
        row2Lp.setMargins(0, dp(6), 0, dp(8));
        rightPanel.addView(row2, row2Lp);

        restoreBtn = toolButton("Restaurar");
        restoreBtn.setOnClickListener(v -> selectTool(EditorCanvasView.TOOL_RESTORE));
        row2.addView(restoreBtn, new LinearLayout.LayoutParams(0, -1, 1));

        magicBtn = toolButton("Varita");
        magicBtn.setOnClickListener(v -> selectTool(EditorCanvasView.TOOL_MAGIC));
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(0, -1, 1);
        wlp.setMargins(dp(6), 0, 0, 0);
        row2.addView(magicBtn, wlp);

        addPanelDivider(rightPanel);

        brushValue = label("Tamaño: 35", 13, WHITE, Typeface.BOLD);
        brushValue.setGravity(Gravity.LEFT);
        rightPanel.addView(brushValue, new LinearLayout.LayoutParams(-1, dp(30)));

        SeekBar brush = new SeekBar(this);
        brush.setMax(180);
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
        rightPanel.addView(brush, new LinearLayout.LayoutParams(-1, dp(42)));

        toleranceValue = label("Tolerancia: 60", 13, WHITE, Typeface.BOLD);
        toleranceValue.setGravity(Gravity.LEFT);
        rightPanel.addView(toleranceValue, new LinearLayout.LayoutParams(-1, dp(30)));

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
        rightPanel.addView(tolerance, new LinearLayout.LayoutParams(-1, dp(42)));

        softnessValue = label("Suavidad: 35%", 13, WHITE, Typeface.BOLD);
        softnessValue.setGravity(Gravity.LEFT);
        rightPanel.addView(softnessValue, new LinearLayout.LayoutParams(-1, dp(30)));

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
        rightPanel.addView(softness, new LinearLayout.LayoutParams(-1, dp(42)));

        addPanelDivider(rightPanel);

        LinearLayout holesRow = new LinearLayout(this);
        holesRow.setOrientation(LinearLayout.HORIZONTAL);
        holesRow.setGravity(Gravity.CENTER_VERTICAL);
        rightPanel.addView(holesRow, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView holesText = label("Huecos", 13, WHITE, Typeface.BOLD);
        holesText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        holesRow.addView(holesText, new LinearLayout.LayoutParams(0, -1, 1));

        Switch holesSwitch = new Switch(this);
        holesSwitch.setChecked(true);
        holesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> editor.setHoleRecognitionEnabled(isChecked));
        holesRow.addView(holesSwitch, new LinearLayout.LayoutParams(dp(58), -1));

        LinearLayout rowBottom = new LinearLayout(this);
        rowBottom.setOrientation(LinearLayout.HORIZONTAL);
        rightPanel.addView(rowBottom, new LinearLayout.LayoutParams(-1, dp(44)));

        Button reset = toolButton("Reset");
        reset.setOnClickListener(v -> editor.resetView());
        rowBottom.addView(reset, new LinearLayout.LayoutParams(0, -1, 1));

        Button apply = toolButton("Aplicar");
        apply.setOnClickListener(v -> toast("Aplicado."));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(0, -1, 1);
        alp.setMargins(dp(6), 0, 0, 0);
        rowBottom.addView(apply, alp);

        selectTool(EditorCanvasView.TOOL_ERASE_SOFT);
    }

    private void addPanelToggle() {
        panelToggle = iconButton("›");
        panelToggle.setTextSize(22);
        panelToggle.setOnClickListener(v -> togglePanel());

        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(dp(42), dp(42));
        tlp.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        tlp.rightMargin = dp(164);
        root.addView(panelToggle, tlp);
    }

    private void togglePanel() {
        if (rightPanel.getVisibility() == View.VISIBLE) {
            rightPanel.setVisibility(View.GONE);
            panelToggle.setText("‹");

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) panelToggle.getLayoutParams();
            lp.rightMargin = dp(8);
            panelToggle.setLayoutParams(lp);
        } else {
            rightPanel.setVisibility(View.VISIBLE);
            panelToggle.setText("›");

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) panelToggle.getLayoutParams();
            lp.rightMargin = dp(164);
            panelToggle.setLayoutParams(lp);
        }
    }

    private void selectTool(int tool) {
        editor.setTool(tool);
        resetToolButtons();

        if (tool == EditorCanvasView.TOOL_ERASE_SOFT) {
            activateButton(softBtn);
            toast("Borrador suave.");
        } else if (tool == EditorCanvasView.TOOL_ERASE_HARD) {
            activateButton(hardBtn);
            toast("Borrador duro.");
        } else if (tool == EditorCanvasView.TOOL_RESTORE) {
            activateButton(restoreBtn);
            toast("Restaurar activo.");
        } else if (tool == EditorCanvasView.TOOL_MAGIC) {
            activateButton(magicBtn);
            toast("Varita mágica activa.");
        }
    }

    private void activateButton(Button button) {
        if (button != null) {
            button.setBackground(roundBg(Color.argb(220, 20, 105, 245), 12));
        }
    }

    private void resetToolButtons() {
        Button[] buttons = new Button[]{softBtn, hardBtn, restoreBtn, magicBtn};
        for (Button b : buttons) {
            if (b != null) {
                b.setBackground(roundBg(Color.argb(120, 60, 60, 60), 12));
            }
        }
    }

    private void addPanelDivider(LinearLayout panel) {
        View line = new View(this);
        line.setBackgroundColor(Color.argb(80, 255, 255, 255));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.setMargins(0, dp(8), 0, dp(8));
        panel.addView(line, lp);
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
