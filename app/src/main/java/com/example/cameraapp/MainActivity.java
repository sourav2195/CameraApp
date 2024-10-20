package com.example.cameraapp;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private TextureView textureView;
    private Button btnCapture;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader imageReader;
    private ImageView previewImage;

    private static final int CAMERA_REQUEST_CODE = 101;
    private Size imageDimension;
    private byte[] lastCapturedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.textureView);
        btnCapture = findViewById(R.id.btnCapture);
        previewImage = findViewById(R.id.previewImage);

        textureView.setSurfaceTextureListener(textureListener);
        btnCapture.setOnClickListener(v -> capturePhoto());

        // Check for camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }

        previewImage.setOnClickListener(v -> showFullImage());

    }

    private void saveImagetoStore(byte[] bytes) {
        //create a file in external storage directory
        File imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"CameraApp");

        //create the directory if not exists
        if(!imageDir.exists())
        {
            imageDir.mkdirs();
        }
        //create a file name for the image
        TimeZone timeZoneInd = TimeZone.getTimeZone("Asia/Kolkata");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        dateFormat.setTimeZone(timeZoneInd);
        String timeStamp = dateFormat.format(new Date());
        String imageFileName = "IMG"+timeStamp+".jpg";
        File imageFile = new File(imageDir,imageFileName );

        try (FileOutputStream fos = new FileOutputStream(imageFile)){
            fos.write(bytes);
            Toast.makeText(this, "Image saved to "+imageFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();

        }
        catch  (IOException e) {
            throw new RuntimeException(e);
        }

    }
    private void showCaptureImage(byte[] imageBytes){
        //convert byte array to bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length);
        //set the bitmap in image view
        previewImage.setImageBitmap(bitmap);

    }

    private void showFullImage(){
        //check if there is a image or not
        if(lastCapturedImage == null);
        {
            Toast.makeText(this, "No Image captured yet", Toast.LENGTH_SHORT).show();
        }
        //create a dialog box to show the image in full screen
        Dialog dialog = new Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.full_screen_image);

        //find the Image view inside the dialog
        ImageView fullImageView = dialog.findViewById(R.id.fullImage);

        //convert byte array to bitmap and set it in dialog's image view
        Bitmap bitmap = BitmapFactory.decodeByteArray(lastCapturedImage,0,lastCapturedImage.length);
        fullImageView.setImageBitmap(bitmap);

        //show dialog
        dialog.show();

    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return false; }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            startCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0]; // Rear camera
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            imageDimension = new Size(1920, 1080); // default size
            imageReader = ImageReader.newInstance(imageDimension.getWidth(), imageDimension.getHeight(), ImageFormat.JPEG, 1);

            //initialize the imageReader and set the listner
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    //acquir the latest image from the ImageReader
                    Image image = reader.acquireLatestImage();

                    if (image != null)
                    {
                        //convert the image to byte array
                        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(bytes);

                        //save the image to storage
                        saveImagetoStore(bytes);
                        //close the image after processing


                        //show the captured image in the imageview in main thread
                        runOnUiThread(()->showCaptureImage(bytes));
                        image.close();
                    }

                }
            },null);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) return;
                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) return;
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void capturePhoto() {
        try {
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            cameraCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Toast.makeText(MainActivity.this, "Photo Captured", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}
