package com.example.cameraapp;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class StorageHelper {

    public static void saveImageToStorage(Context context, byte[] imageBytes) {
        // Create a directory in external storage
        File imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CameraApp");

        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }

        // Create a unique image file name
        TimeZone timeZoneInd = TimeZone.getTimeZone("Asia/Kolkata");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        dateFormat.setTimeZone(timeZoneInd);
        String timeStamp = dateFormat.format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".jpg";
        File imageFile = new File(imageDir, imageFileName);

        // Write image bytes to the file
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            fos.write(imageBytes);
            Toast.makeText(context, "Image saved to " + imageFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
