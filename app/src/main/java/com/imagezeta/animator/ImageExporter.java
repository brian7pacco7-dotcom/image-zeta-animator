package com.imagezeta.animator;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageExporter {

    public interface ExportCallback {
        void onResult(boolean success);
    }

    public static void savePng(
            Context context,
            Bitmap bitmap,
            String folderName,
            ExportCallback callback
    ) {
        if (context == null || bitmap == null) {
            if (callback != null) callback.onResult(false);
            return;
        }

        try {
            String fileName = "ImageZeta_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) +
                    ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/" + folderName
                );
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            Uri uri = context.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            );

            if (uri == null) {
                if (callback != null) callback.onResult(false);
                return;
            }

            OutputStream output = context.getContentResolver().openOutputStream(uri);

            if (output == null) {
                if (callback != null) callback.onResult(false);
                return;
            }

            boolean saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            output.flush();
            output.close();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                context.getContentResolver().update(uri, values, null, null);
            }

            if (callback != null) callback.onResult(saved);

        } catch (Exception e) {
            if (callback != null) callback.onResult(false);
        }
    }
        }
