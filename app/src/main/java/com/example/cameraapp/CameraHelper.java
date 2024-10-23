package com.example.cameraapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CameraHelper {

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private ImageReader imageReader;
    private Size imageDimension;

    private Context context;
    private TextureView textureView;
    private byte[] lastCapturedImage;
    private Handler backgroundHandler;
    private HandlerThread handlerThread;

    private final int CAMERA_REQUEST_CODE = 101;

    public CameraHelper(Context context, TextureView textureView) {
        this.context = context;
        this.textureView = textureView;
        startBackgroundThread();
    }
    // Start the background thread and its handler
    private void startBackgroundThread() {
        handlerThread = new HandlerThread("CameraBackground");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
    }
    // Stop the background thread
    public void stopBackgroundThread() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            startCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    public void startCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(
                    Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(context, "Failed to configure the camera", Toast.LENGTH_SHORT).show();
                        }
                    },
                    new Handler()
            );

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void capturePhoto() {
        try {
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            cameraCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Toast.makeText(context, "Photo Captured", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void openCamera(CameraManager manager) {
        try {
            String cameraId = manager.getCameraIdList()[0]; // Rear camera
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            imageDimension = new Size(1920, 1080); // Default size
            int maxImage =5;
            imageReader = ImageReader.newInstance(imageDimension.getWidth(), imageDimension.getHeight(), ImageFormat.JPEG, maxImage);

            // Set the image reader listener
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    lastCapturedImage = new byte[buffer.remaining()];
                    buffer.get(lastCapturedImage);

                    // Save the image with a unique filename
                    String filename = generateUniqueFilename(); // Generate a unique filename
                    StorageHelper.saveImageToStorage(context,lastCapturedImage,filename);
                    image.close();
                }
            }, null);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((MainActivity) context, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void captureBurstPhoto(int numPhoto) throws CameraAccessException {
        //create a list of capture requests for burst mode
        try {
            List<CaptureRequest> captureRequestList = new ArrayList<>();
            for (int i = 0; i < numPhoto; i++) {
                CaptureRequest.Builder burstBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                burstBuilder.addTarget(imageReader.getSurface());
                burstBuilder.set(CaptureRequest.CONTROL_MODE,CameraMetadata.CONTROL_MODE_AUTO);
                captureRequestList.add(burstBuilder.build());
            }
            //capture the burst photo

            cameraCaptureSession.captureBurst(captureRequestList, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                    Log.d("Burst", "Burst capture sequence completed.");
                }

            }, backgroundHandler);
        }
        catch (CameraAccessException e){
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
    private int imageCounter = 0; // Counter for burst images

    public String generateUniqueFilename() {
        TimeZone timeZoneInd = TimeZone.getTimeZone("Asia/Kolkata");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        dateFormat.setTimeZone(timeZoneInd);
        String timeStamp = dateFormat.format(new Date());
        return "IMG_" + timeStamp + "_" + (imageCounter++) + ".jpg"; // Append counter for uniqueness
    }

    public byte[] getLastCapturedImage() {
        return lastCapturedImage;
    }

    public void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        stopBackgroundThread(); // Stop the background thread
    }
}
