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
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE = 1001;

    private EditorCanvasView editorView;

    private Button btnOpen;
    private Button btnWand;
    private Button btnArea;
    private Button btnApplyArea;
    private Button btnBg;
    private Button btnReset;
    private Button btnSave;

    private TextView txtSize;
    private TextView txtSoftness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildLayout();
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout sidePanel = new LinearLayout(this);
        sidePanel.setOrientation(LinearLayout.VERTICAL);
        sidePanel.setGravity(Gravity.CENTER_HORIZONTAL);
        sidePanel.setPadding(6, 8, 6, 8);
        sidePanel.setBackgroundColor(Color.rgb(25, 25, 25));

        btnOpen = new Button(this);
        btnOpen.setText("ABRIR");

        btnWand = new Button(this);
        btnWand.setText("VARITA\nOFF");

        btnArea = new Button(this);
        btnArea.setText("ÁREA\nOFF");

        btnApplyArea = new Button(this);
        btnApplyArea.setText("BORRAR\nÁREA");

        btnBg = new Button(this);
        btnBg.setText("FONDO");

        btnReset = new Button(this);
        btnReset.setText("RESET");

        btnSave = new Button(this);
        btnSave.setText("GUARDAR");

        TextView title = new TextView(this);
        title.setText("CONFIG");
        title.setTextColor(Color.WHITE);
        title.setTextSize(13f);
        title.setGravity(Gravity.CENTER);

        txtSize = new TextView(this);
        txtSize.setTextColor(Color.WHITE);
        txtSize.setTextSize(11f);
        txtSize.setGravity(Gravity.CENTER);
        txtSize.setText("Tamaño\n45");

        SeekBar seekSize = new SeekBar(this);
        seekSize.setMax(140);
        seekSize.setProgress(35);

        txtSoftness = new TextView(this);
        txtSoftness.setTextColor(Color.WHITE);
        txtSoftness.setTextSize(11f);
        txtSoftness.setGravity(Gravity.CENTER);
        txtSoftness.setText("Suavidad\n3");

        SeekBar seekSoftness = new SeekBar(this);
        seekSoftness.setMax(20);
        seekSoftness.setProgress(3);

        addPanelView(sidePanel, btnOpen);
        addPanelView(sidePanel, btnWand);
        addPanelView(sidePanel, btnArea);
        addPanelView(sidePanel, btnApplyArea);
        addPanelView(sidePanel, btnBg);
        addPanelView(sidePanel, btnReset);
        addPanelView(sidePanel, btnSave);
        addPanelView(sidePanel, title);
        addPanelView(sidePanel, txtSize);
        addPanelView(sidePanel, seekSize);
        addPanelView(sidePanel, txtSoftness);
        addPanelView(sidePanel, seekSoftness);

        editorView = new EditorCanvasView(this);

        root.addView(sidePanel, new LinearLayout.LayoutParams(
                dpToPx(115),
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        root.addView(editorView, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        ));

        setContentView(root);

        seekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float size = 10f + progress;
                editorView.setEraserSize(size);
                txtSize.setText("Tamaño\n" + (int) size);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekSoftness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editorView.setEdgeSoftness(progress);
                txtSoftness.setText("Suavidad\n" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImage();
            }
        });

        btnWand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorView.toggleWand();

                if (editorView.isWandEnabled()) {
                    editorView.setLassoEnabled(false);
                    btnWand.setText("VARITA\nON");
                    btnArea.setText("ÁREA\nOFF");
                    Toast.makeText(MainActivity.this, "Dibuja sobre la imagen para borrar", Toast.LENGTH_SHORT).show();
                } else {
                    btnWand.setText("VARITA\nOFF");
                    Toast.makeText(MainActivity.this, "Modo mover/zoom activado", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorView.toggleLasso();

                if (editorView.isLassoEnabled()) {
                    editorView.setWandEnabled(false);
                    btnArea.setText("ÁREA\nON");
                    btnWand.setText("VARITA\nOFF");
                    Toast.makeText(MainActivity.this, "Dibuja una forma cerrada", Toast.LENGTH_SHORT).show();
                } else {
                    btnArea.setText("ÁREA\nOFF");
                }
            }
        });

        btnApplyArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean ok = editorView.applyLassoErase();

                if (ok) {
                    Toast.makeText(MainActivity.this, "Área borrada", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Primero dibuja un área", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorView.toggleCheckerBackground();
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

    private void addPanelView(LinearLayout panel, View view) {
        panel.addView(view, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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

                if (uri == null) {
                    Toast.makeText(this, "Imagen no válida", Toast.LENGTH_SHORT).show();
                    return;
                }

                Bitmap bitmap = decodeBitmapFromUri(uri, 2200);

                if (bitmap == null) {
                    Toast.makeText(this, "No se pudo abrir la imagen", Toast.LENGTH_SHORT).show();
                    return;
                }

                editorView.setBitmap(bitmap);
                Toast.makeText(this, "Imagen cargada", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(this, "Error al abrir imagen", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap decodeBitmapFromUri(Uri uri, int maxSize) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;

            InputStream input1 = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(input1, null, bounds);

            if (input1 != null) {
                input1.close();
            }

            int width = bounds.outWidth;
            int height = bounds.outHeight;

            int sample = 1;

            while ((width / sample) > maxSize || (height / sample) > maxSize) {
                sample *= 2;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            InputStream input2 = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input2, null, options);

            if (input2 != null) {
                input2.close();
            }

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
                Toast.makeText(this, "No se pudo crear el archivo", Toast.LENGTH_SHORT).show();
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
            }
