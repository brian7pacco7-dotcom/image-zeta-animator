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
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE = 1001;

    private EditorCanvasView editorView;

    private Button btnOpen;
    private Button btnWand;
    private Button btnBg;
    private Button btnReset;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildLayout();
    }

    private void buildLayout() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.BLACK);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER);
        topBar.setPadding(6, 6, 6, 6);
        topBar.setBackgroundColor(Color.rgb(40, 40, 40));

        btnOpen = new Button(this);
        btnOpen.setText("Abrir");

        btnWand = new Button(this);
        btnWand.setText("Varita OFF");

        btnBg = new Button(this);
        btnBg.setText("Fondo");

        btnReset = new Button(this);
        btnReset.setText("Reset");

        btnSave = new Button(this);
        btnSave.setText("Guardar");

        topBar.addView(btnOpen, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        topBar.addView(btnWand, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        topBar.addView(btnBg, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        topBar.addView(btnReset, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        topBar.addView(btnSave, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        editorView = new EditorCanvasView(this);

        mainLayout.addView(topBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        mainLayout.addView(editorView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        setContentView(mainLayout);

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
                    btnWand.setText("Varita ON");
                    Toast.makeText(MainActivity.this, "Varita activada", Toast.LENGTH_SHORT).show();
                } else {
                    btnWand.setText("Varita OFF");
                    Toast.makeText(MainActivity.this, "Varita desactivada", Toast.LENGTH_SHORT).show();
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

                InputStream inputStream = getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (inputStream != null) {
                    inputStream.close();
                }

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
