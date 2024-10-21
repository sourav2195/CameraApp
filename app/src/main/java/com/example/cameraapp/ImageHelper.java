package com.example.cameraapp;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class ImageHelper {

    public static void showCapturedImage(Context context, byte[] imageBytes, ImageView imageView) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        // Get the device orientation
        int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay()
                .getRotation();

        // Rotate the bitmap based on the orientation
        Bitmap rotatedBitmap = rotateBitmap(bitmap, rotation);

        // Set the rotated bitmap to the ImageView
        imageView.setImageBitmap(rotatedBitmap);
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int rotation) {
        Matrix matrix = new Matrix();
        switch (rotation) {
            case Surface.ROTATION_0:
                // No rotation needed
                break;
            case Surface.ROTATION_90:
                matrix.postRotate(90);
                break;
            case Surface.ROTATION_180:
                matrix.postRotate(180);
                break;
            case Surface.ROTATION_270:
                matrix.postRotate(270);
                break;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }


    public static void showFullImage(Context context, byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            Toast.makeText(context, "No Image captured yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a dialog to show the image in full-screen
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.full_screen_image);

        ImageView fullImageView = dialog.findViewById(R.id.fullImage);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (bitmap != null) {
            fullImageView.setImageBitmap(bitmap);
            dialog.show();
        } else {
            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }
}
