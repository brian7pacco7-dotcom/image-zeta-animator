package com.imagezeta.animator;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private EditorCanvasView editor;
    private TextView zoomText;
    private TextView toolText;
    private TextView sizeText;
    private TextView toleranceText;
    private TextView softnessText;

    private Button moveButton;
    private Button softButton;
    private Button hardButton;
    private Button restoreButton;
    private Button magicButton;
    private Button zoneButton;
    private Button clearZoneButton;
    private Button backgroundButton;

    private int currentTool = EditorCanvasView.TOOL_ERASE_SOFT;
    private boolean magicEnabled = false;

    private final ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        loadImage(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissionIfNeeded();

        FrameLayout root = new FrameLayout(this);

        editor = new EditorCanvasView(this);
        root.addView(editor, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8), dp(8), dp(8), dp(8));
        panel.setBackgroundColor(0xCC111111);

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        panelParams.gravity = Gravity.BOTTOM;
        root.addView(panel, panelParams);

        zoomText = makeLabel("Zoom: 100%");
        toolText = makeLabel("Herramienta: Borrador suave");

        panel.addView(zoomText);
        panel.addView(toolText);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        scroll.addView(buttons);
        panel.addView(scroll);

        Button openButton = makeButton("Abrir");
        openButton.setOnClickListener(v -> openImagePicker());
        buttons.addView(openButton);

        moveButton = makeButton("Mover");
        moveButton.setOnClickListener(v -> setTool(EditorCanvasView.TOOL_MOVE));
        buttons.addView(moveButton);

        softButton = makeButton("Borrar suave");
        softButton.setOnClickListener(v -> setTool(EditorCanvasView.TOOL_ERASE_SOFT));
        buttons.addView(softButton);

        hardButton = makeButton("Borrar duro");
        hardButton.setOnClickListener(v -> setTool(EditorCanvasView.TOOL_ERASE_HARD));
        buttons.addView(hardButton);

        restoreButton = makeButton("Restaurar");
        restoreButton.setOnClickListener(v -> setTool(EditorCanvasView.TOOL_RESTORE));
        buttons.addView(restoreButton);

        magicButton = makeButton("Varita OFF");
        magicButton.setOnClickListener(v -> toggleMagic());
        buttons.addView(magicButton);

        zoneButton = makeButton("Zona");
        zoneButton.setOnClickListener(v -> setTool(EditorCanvasView.TOOL_RECT));
        buttons.addView(zoneButton);

        clearZoneButton = makeButton("Limpiar zona");
        clearZoneButton.setOnClickListener(v -> editor.clearMagicMask());
        buttons.addView(clearZoneButton);

        backgroundButton = makeButton("Fondo");
        backgroundButton.setOnClickListener(v -> {
            String name = editor.cycleTransparencyBackground();
            Toast.makeText(this, name, Toast.LENGTH_SHORT).show();
        });
        buttons.addView(backgroundButton);

        Button undoButton = makeButton("Deshacer");
        undoButton.setOnClickListener(v -> editor.undo());
        buttons.addView(undoButton);

        Button redoButton = makeButton("Rehacer");
        redoButton.setOnClickListener(v -> editor.redo());
        buttons.addView(redoButton);

        Button resetButton = makeButton("Reset vista");
        resetButton.setOnClickListener(v -> editor.resetView());
        buttons.addView(resetButton);

        Button saveButton = makeButton("Guardar PNG");
        saveButton.setOnClickListener(v -> saveImage());
        buttons.addView(saveButton);

        sizeText = makeLabel("Tamaño: 35");
        panel.addView(sizeText);

        SeekBar sizeSeek = new SeekBar(this);
        sizeSeek.setMax(150);
        sizeSeek.setProgress(35);
        sizeSeek.setOnSeekBarChangeListener(new SimpleSeekBar() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = Math.max(1, progress);
                sizeText.setText("Tamaño: " + value);
                editor.setBrushSize(value);
            }
        });
        panel.addView(sizeSeek);

        toleranceText = makeLabel("Tolerancia varita: 60");
        panel.addView(toleranceText);

        SeekBar toleranceSeek = new SeekBar(this);
        toleranceSeek.setMax(180);
        toleranceSeek.setProgress(60);
        toleranceSeek.setOnSeekBarChangeListener(new SimpleSeekBar() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = Math.max(1, progress);
                toleranceText.setText("Tolerancia varita: " + value);
                editor.setTolerance(value);
            }
        });
        panel.addView(toleranceSeek);

        softnessText = makeLabel("Suavidad borde: 35");
        panel.addView(softnessText);

        SeekBar softnessSeek = new SeekBar(this);
        softnessSeek.setMax(100);
        softnessSeek.setProgress(35);
        softnessSeek.setOnSeekBarChangeListener(new SimpleSeekBar() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                softnessText.setText("Suavidad borde: " + progress);
                editor.setEdgeSoftness(progress);
            }
        });
        panel.addView(softnessSeek);

        Switch holesSwitch = new Switch(this);
        holesSwitch.setText("Huecos / continuidad diagonal");
        holesSwitch.setTextColor(0xFFFFFFFF);
        holesSwitch.setChecked(true);
        holesSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                editor.setHoleRecognitionEnabled(isChecked)
        );
        panel.addView(holesSwitch);

        editor.setZoomListener(percent ->
                zoomText.setText("Zoom: " + Math.round(percent) + "%")
        );

        setContentView(root);
        updateButtonStates();
    }

    private void toggleMagic() {
        magicEnabled = !magicEnabled;

        if (magicEnabled) {
            setTool(EditorCanvasView.TOOL_MAGIC);
        } else {
            setTool(EditorCanvasView.TOOL_MOVE);
        }

        updateButtonStates();
    }

    private void setTool(int tool) {
        currentTool = tool;
        editor.setTool(tool);

        if (tool == EditorCanvasView.TOOL_MAGIC) {
            magicEnabled = true;
            toolText.setText("Herramienta: Varita ON");
        } else if (tool == EditorCanvasView.TOOL_MOVE) {
            magicEnabled = false;
            toolText.setText("Herramienta: Mover / Zoom normal");
        } else if (tool == EditorCanvasView.TOOL_ERASE_SOFT) {
            magicEnabled = false;
            toolText.setText("Herramienta: Borrador suave");
        } else if (tool == EditorCanvasView.TOOL_ERASE_HARD) {
            magicEnabled = false;
            toolText.setText("Herramienta: Borrador duro");
        } else if (tool == EditorCanvasView.TOOL_RESTORE) {
            magicEnabled = false;
            toolText.setText("Herramienta: Restaurar");
        } else if (tool == EditorCanvasView.TOOL_RECT) {
            magicEnabled = false;
            toolText.setText("Herramienta: Dibujar zona de varita");
        }

        updateButtonStates();
    }

    private void updateButtonStates() {
        if (magicButton != null) {
            magicButton.setText(magicEnabled ? "Varita ON" : "Varita OFF");
        }

        setButtonSelected(moveButton, currentTool == EditorCanvasView.TOOL_MOVE);
        setButtonSelected(softButton, currentTool == EditorCanvasView.TOOL_ERASE_SOFT);
        setButtonSelected(hardButton, currentTool == EditorCanvasView.TOOL_ERASE_HARD);
        setButtonSelected(restoreButton, currentTool == EditorCanvasView.TOOL_RESTORE);
        setButtonSelected(magicButton, currentTool == EditorCanvasView.TOOL_MAGIC);
        setButtonSelected(zoneButton, currentTool == EditorCanvasView.TOOL_RECT);
    }

    private void setButtonSelected(Button button, boolean selected) {
        if (button == null) return;
        button.setAlpha(selected ? 1f : 0.72f);
    }

    private TextView makeLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(14);
        label.setPadding(dp(4), dp(2), dp(4), dp(2));
        return label;
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setPadding(dp(8), dp(4), dp(8), dp(4));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        button.setLayoutParams(params);

        return button;
    }

    private void openImagePicker() {
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );
        imagePicker.launch(intent);
    }

    private void loadImage(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(input);

            if (bitmap == null) {
                Toast.makeText(this, "No se pudo abrir la imagen.", Toast.LENGTH_SHORT).show();
                return;
            }

            editor.setImage(bitmap.copy(Bitmap.Config.ARGB_8888, true));
            setTool(EditorCanvasView.TOOL_ERASE_SOFT);

        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveImage() {
        if (!editor.hasImage()) {
            Toast.makeText(this, "Primero abre una imagen.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap output = editor.getOutputBitmap();
            String fileName = "image_zeta_" + System.currentTimeMillis() + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ImageZeta");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            Uri uri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            );

            if (uri == null) {
                throw new Exception("No se pudo crear archivo.");
            }

            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) {
                    throw new Exception("No se pudo escribir archivo.");
                }

                output.compress(Bitmap.CompressFormat.PNG, 100, out);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
            }

            Toast.makeText(this, "Guardado en Pictures/ImageZeta", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void requestPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        10
                );
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private abstract static class SimpleSeekBar implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
                                                 }
