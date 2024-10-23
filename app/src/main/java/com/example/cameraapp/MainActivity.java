package com.example.cameraapp;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.util.Log;
import android.view.TextureView;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private TextureView textureView;
    private Button btnCapture;
    private Button burstCapture;
    private ImageView previewImage;
    private CameraHelper cameraHelper;

    //private final byte[] lastImage = cameraHelper.getLastCapturedImage();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.textureView);
        btnCapture = findViewById(R.id.btnCapture);
        burstCapture = findViewById(R.id.btnBurstCapture);
        previewImage = findViewById(R.id.previewImage);

        cameraHelper = new CameraHelper(this, textureView);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
                cameraHelper.openCamera(manager);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
        // single capture photo on button click
        btnCapture.setOnClickListener(v -> {
            cameraHelper.capturePhoto();
           // byte[] lastImage = cameraHelper.getLastCapturedImage();
            //if (lastImage != null) {
             //   StorageHelper.saveImageToStorage(MainActivity.this, lastImage);
              //  ImageHelper.showCapturedImage(MainActivity.this, lastImage, previewImage);
            //}
        });

        //burst capture photo
        burstCapture.setOnClickListener(v->{
            try {
                cameraHelper.captureBurstPhoto(5);
//                byte[] burstImage = cameraHelper.getLastCapturedImage();
//                if(burstImage!=null){
//                    StorageHelper.saveImageToStorage(MainActivity.this, burstImage);
//                    ImageHelper.showCapturedImage(MainActivity.this, burstImage, previewImage);
//                }
                Log.d("burst","burst mode testing");
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }

        });

        // Show full image on preview click
        previewImage.setOnClickListener(v -> {
            byte[] lastImage = cameraHelper.getLastCapturedImage();
            ImageHelper.showFullImage(MainActivity.this, lastImage);
        });

    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Retrieve the last captured image from CameraHelper
        byte[] lastImage = cameraHelper.getLastCapturedImage();


        // Reload the last captured image if available
        if (lastImage != null) {
            ImageHelper.showCapturedImage(this, lastImage, previewImage);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraHelper.closeCamera();
    }
}

