package com.example.cameraapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class StorageHelper {
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    public static void saveImageToStorage(Context context, byte[] imageBytes, String filename) {
        executorService.execute(() ->{
        // Create a directory in external storage
        File imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CameraApp");

        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }

        // Create the image file with the specified filename
        File imageFile = new File(imageDir, filename);

        // Convert byte array to Bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        // Write image bytes to the file
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            fos.write(imageBytes);
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "Image saved to " + imageFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            bitmap.recycle(); // Free up memory
        }
    });
    }


}
