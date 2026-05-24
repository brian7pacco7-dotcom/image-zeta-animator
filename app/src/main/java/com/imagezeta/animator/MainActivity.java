package com.imagezeta.animator;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE = 1001;

    private EditorCanvasView editorView;

    private LinearLayout configPanel;
    private boolean configVisible = true;

    private Button btnUndo;
    private Button btnRedo;
    private Button btnConfig;
    private Button btnArea;
    private Button btnWand;
    private Button btnOpen;
    private Button btnEraseArea;
    private Button btnReset;
    private Button btnSave;

    private TextView txtTolerance;
    private TextView txtSoftness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildLayout();
    }

    private void buildLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(55, 55, 55));

        editorView = new EditorCanvasView(this);
        root.addView(editorView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(8), dp(8), dp(8), dp(8));
        topBar.setBackgroundColor(Color.rgb(70, 70, 70));

        btnUndo = roundButton("↶");
        btnRedo = roundButton("↷");
        btnWand = roundButton("✦");
        btnArea = roundButton("□");
        btnConfig = roundButton("☝");

        topBar.addView(btnUndo);
        topBar.addView(btnRedo);

        SpaceView space = new SpaceView(this);
        topBar.addView(space, new LinearLayout.LayoutParams(0, 1, 1));

        topBar.addView(btnWand);
        topBar.addView(btnArea);
        topBar.addView(btnConfig);

        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(72)
        );
        topParams.gravity = Gravity.TOP;
        root.addView(topBar, topParams);

        configPanel = buildConfigPanel();

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                dp(230),
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        panelParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        panelParams.setMargins(0, dp(70), dp(14), dp(70));
        root.addView(configPanel, panelParams);

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(dp(4), dp(4), dp(4), dp(4));
        bottomBar.setBackgroundColor(Color.rgb(70, 70, 70));

        btnOpen = smallBottomButton("Abrir");
        btnEraseArea = smallBottomButton("Borrar\nÁrea");
        btnReset = smallBottomButton("Restaurar");
        btnSave = smallBottomButton("Guardar");

        bottomBar.addView(btnOpen);
        bottomBar.addView(btnEraseArea);
        bottomBar.addView(btnReset);
        bottomBar.addView(btnSave);

        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(70)
        );
        bottomParams.gravity = Gravity.BOTTOM;
        root.addView(bottomBar, bottomParams);

        setContentView(root);

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorView.undo();
            }
        });

        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorView.redo();
            }
        });

        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                configVisible = !configVisible;
                configPanel.setVisibility(configVisible ? View.VISIBLE : View.GONE);
            }
        });

        btnWand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorView.toggleWand();

                if (editorView.isWandEnabled()) {
                    editorView.setLassoEnabled(false);
                    Toast.makeText(MainActivity.this, "Varita mágica: toca un color", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Mover, zoom y girar con dedos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorView.toggleLasso();

                if (editorView.isLassoEnabled()) {
                    editorView.setWandEnabled(false);
                    Toast.makeText(MainActivity.this, "Dibuja selección libre", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Selección apagada", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImage();
            }
        });

        btnEraseArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean ok = editorView.applyLassoErase();
                Toast.makeText(MainActivity.this, ok ? "Área borrada" : "Primero dibuja un área", Toast.LENGTH_SHORT).show();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorView.resetImage();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImage();
            }
        });
    }

    private LinearLayout buildConfigPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(220, 20, 20, 20));
        bg.setCornerRadius(dp(18));
        panel.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("Herramienta");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setGravity(Gravity.LEFT);
        panel.addView(title);

        txtTolerance = new TextView(this);
        txtTolerance.setText("Tolerancia: 35");
        txtTolerance.setTextColor(Color.WHITE);
        txtTolerance.setTextSize(15f);
        txtTolerance.setPadding(0, dp(16), 0, 0);
        panel.addView(txtTolerance);

        SeekBar seekTolerance = new SeekBar(this);
        seekTolerance.setMax(145);
        seekTolerance.setProgress(30);
        panel.addView(seekTolerance);

        txtSoftness = new TextView(this);
        txtSoftness.setText("Suavidad: 2");
        txtSoftness.setTextColor(Color.WHITE);
        txtSoftness.setTextSize(15f);
        txtSoftness.setPadding(0, dp(16), 0, 0);
        panel.addView(txtSoftness);

        SeekBar seekSoftness = new SeekBar(this);
        seekSoftness.setMax(20);
        seekSoftness.setProgress(2);
        panel.addView(seekSoftness);

        TextView help = new TextView(this);
        help.setText("Varita: toca un color.\nÁrea: dibuja selección.\nGirar: usa dos dedos.");
        help.setTextColor(Color.LTGRAY);
        help.setTextSize(13f);
        help.setPadding(0, dp(16), 0, 0);
        panel.addView(help);

        seekTolerance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = 5 + progress;
                editorView.setMagicTolerance(value);
                txtTolerance.setText("Tolerancia: " + value);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekSoftness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editorView.setEdgeSoftness(progress);
                txtSoftness.setText("Suavidad: " + progress);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        return panel;
    }

    private Button roundButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(22f);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.rgb(55, 55, 55));
        bg.setStroke(dp(1), Color.rgb(120, 120, 120));
        b.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(58), dp(58));
        params.setMargins(dp(6), 0, dp(6), 0);
        b.setLayoutParams(params);

        return b;
    }

    private Button smallBottomButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(11f);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        b.setLayoutParams(params);

        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
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
                Bitmap bitmap = decodeBitmapFromUri(uri, 2200);

                if (bitmap == null) {
                    Toast.makeText(this, "No se pudo abrir imagen", Toast.LENGTH_SHORT).show();
                    return;
                }

                editorView.setBitmap(bitmap);
                Toast.makeText(this, "Imagen cargada", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(this, "Error al abrir", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap decodeBitmapFromUri(Uri uri, int maxSize) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;

            InputStream input1 = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(input1, null, bounds);
            if (input1 != null) input1.close();

            int sample = 1;
            while ((bounds.outWidth / sample) > maxSize || (bounds.outHeight / sample) > maxSize) {
                sample *= 2;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            InputStream input2 = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input2, null, options);
            if (input2 != null) input2.close();

            return bitmap;

        } catch (Exception e) {
            return null;
        }
    }

    private void saveImage() {
        try {
            Bitmap bitmap = editorView.getEditedBitmap();

            if (bitmap == null) {
                Toast.makeText(this, "Primero abre una imagen", Toast.LENGTH_SHORT).show();
                return;
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "ImageZeta_" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ImageZeta");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri == null) {
                Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream outputStream = getContentResolver().openOutputStream(uri);

            if (outputStream == null) {
                Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show();
                return;
            }

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            Toast.makeText(this, "Guardado en Imágenes/ImageZeta", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
        }
    }

    public static class SpaceView extends View {
        public SpaceView(android.content.Context context) {
            super(context);
        }
    }
                                   }
