package com.example.jazz.realtimeobjectdetection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.jazz.realtimeobjectdetection.Utils.BoundingBoxView;
import com.example.jazz.realtimeobjectdetection.Utils.PredictionResult;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE;

public class CameraActivity extends AppCompatActivity{

    private static final String TAG = "Camera Activity";
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private TextureView textureView;
    private BoundingBoxView boxView;
    int  deviceHeight,deviceWidth;
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private Handler mBackgroundHandler, caffeHandler;
    private HandlerThread mBackgroundThread, caffeThread;
    private boolean processing = false;
    private Image image = null;
    private CaffeImageListener caffeImageListener = new CaffeImageListener();

    private final String[] CLASSES = {"__background__",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};


    static {
        System.loadLibrary("caffe-jni");
    }


    private class SetUpNeuralNetwork extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void[] v) {
            File modelFile = new File(Environment.getExternalStorageDirectory(), "ObjectDetection/models/MNSSD_net.protobin");
            File weightFile = new File(Environment.getExternalStorageDirectory(), "ObjectDetection/models/MNSSD_weight.caffemodel");
            Log.d(TAG, "onCreate: modelFile:" + modelFile.getPath());
            Log.d(TAG, "onCreate: weightFIle:" + weightFile.getPath());

            if (!loadModel(modelFile.getPath(), weightFile.getPath())){
                Log.d(TAG, "Cannot load model");
            }

            return null;
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Log.d(TAG, "HELLO");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
            return;
        }

        new SetUpNeuralNetwork().execute();

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.activity_camera);
        getSupportActionBar().hide();

        textureView = (TextureView) findViewById(R.id.cameraView);
        textureView.setSystemUiVisibility(SYSTEM_UI_FLAG_IMMERSIVE);
        final GestureDetector gestureDetector = new GestureDetector(this.getApplicationContext(),
                new GestureDetector.SimpleOnGestureListener(){
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        super.onLongPress(e);

                    }

                    @Override
                    public boolean onDoubleTapEvent(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }
                });

        textureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        boxView = (BoundingBoxView) findViewById(R.id.boxView);

    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }


    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.e(TAG, "cameracallback : CAMERA DC");
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
                final Activity activity = CameraActivity.this;
                if (null != activity) {
                    activity.finish();
                }
            }
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "cameracallback : CAMERA ERROR");
            if (error == CameraDevice.StateCallback.ERROR_CAMERA_SERVICE){
                Log.e(TAG, "cameracallback : CAMERA ERROR1");
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
                final Activity activity = CameraActivity.this;
                if (null != activity) {
                    activity.finish();
                }
            }

        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        caffeThread = new HandlerThread("Caffe Thread");
        caffeThread.start();
        caffeHandler = new Handler(caffeThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        caffeThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;

            caffeThread.join();
            caffeThread = null;
            caffeHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

//    private class RunNN extends AsyncTask<Void, Void , List <PredictionResult>>{
//
//        private Image.Plane[] planes;
//        private byte[] pixels;
//        private float[] mean;
//
//
//        public RunNN(Image.Plane[] planes, float[]mean){
//            super();
//            this.planes = planes;
//            this.mean = mean;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            ByteBuffer buffer = planes[0].getBuffer();
//            buffer.rewind();
//            byte[] data = new byte[buffer.capacity()];
//            buffer.get(data);
//            Bitmap bitmap  = BitmapFactory.decodeByteArray(data, 0, data.length);
//
//            Matrix matrix = new Matrix();
//            matrix.postRotate(90);
//
//            bitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap .getWidth(), bitmap .getHeight(), matrix, true);
//
//            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
//
//            // Copy bitmap pixels to buffer
//            ByteBuffer argb_buf = ByteBuffer.allocate(scaledBitmap.getByteCount());
//            scaledBitmap.copyPixelsToBuffer(argb_buf);
//
//            pixels = argb_buf.array();
//        }
//
//        @Override
//        protected List<PredictionResult> doInBackground(Void... voids) {
//            float[][] result = null;
//            Log.d(TAG, mean.toString());
//            result = mnssd(pixels, mean);
//
//            final List <PredictionResult> rets = new ArrayList<PredictionResult>();
//            if (result != null){
//                for (int i = 0; i < result.length; i++) {
//                    PredictionResult pr = new PredictionResult(result[i][0], result[i][1], result[i][2], result[i][3], result[i][4], CLASSES[(int) result[i][5]]);
//                    rets.add(pr);
//                }
//            }
//            return rets;
//        }
//
//        @Override
//        protected void onPostExecute(List<PredictionResult> predictionResults) {
//            boxView.updateResult(predictionResults);
//            processing = false;
//        }
//    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Log.d(TAG, "w1: " + imageDimension.getWidth() + "  h1: " + imageDimension.getHeight());
            Surface surface = new Surface(texture);
            int width = 1920;
            int height = 1080;
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
//            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
//                @Override
//                public void onImageAvailable(ImageReader reader) {
//                try {
//
//                    image = reader.acquireNextImage();
//                    if (processing) {
//                        image.close();
//                        return;
//                    }
//
//                    processing = true;
//
//                    float[] mean = new float[] {127.5f, 127.5f, 127.5f};
//                    new RunNN(image.getPlanes(), mean).execute();
//
//
//                } finally {
//                    if (image != null) {
//                        image.close();
//                    }
//                }
//                }
//            };
            reader.setOnImageAvailableListener(caffeImageListener, mBackgroundHandler);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(reader.getSurface());

            cameraDevice.createCaptureSession(Arrays.asList(surface, reader.getSurface()), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == cameraDevice) {
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CameraActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        caffeImageListener.init(caffeHandler);
    }


    private void openCamera() {
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d(TAG,"manager: " + manager.getCameraIdList().length);
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[7];
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if(null == cameraDevice) {
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(CameraActivity.this, "You can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }


    public class CaffeImageListener implements ImageReader.OnImageAvailableListener{
        private Handler handler;

        CaffeImageListener(){
        }

        public void init(Handler handler){
            this.handler = handler;
        }

        @Override
        public void onImageAvailable(ImageReader reader) {
            try {
                image = reader.acquireNextImage();
                if (processing) {
                    image.close();
                    return;
                }

                processing = true;
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                buffer.rewind();
                byte[] data = new byte[buffer.capacity()];
                buffer.get(data);
                Bitmap bitmap  = BitmapFactory.decodeByteArray(data, 0, data.length);
//
//                Matrix matrix = new Matrix();
//                matrix.postRotate(90);
//
//                bitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap .getWidth(), bitmap .getHeight(), matrix, true);

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);

                // Copy bitmap pixels to buffer
                ByteBuffer argb_buf = ByteBuffer.allocate(scaledBitmap.getByteCount());
                scaledBitmap.copyPixelsToBuffer(argb_buf);

                final byte[] pixels = argb_buf.array();
                final float[] mean = new float[] {127.5f, 127.5f, 127.5f};
////                new RunNN(image.getPlanes(), mean).execute();
//
                handler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            float[][] result = null;
                            result = mnssd(pixels, mean);

                            final List <PredictionResult> rets = new ArrayList<>();
                            if (result != null){
                                for (int i = 0; i < result.length; i++) {
                                    PredictionResult pr = new PredictionResult(result[i][0], result[i][1], result[i][2], result[i][3], result[i][4], CLASSES[(int) result[i][5]]);
                                    rets.add(pr);
                                }
                            }

                            boxView.updateResult(rets);
                            processing = false;
                        }
                    }
                );


            } finally {
                if (image != null) {
                    image.close();
                }
            }

        }
    }
    public native boolean loadModel(String modelPath, String weightPath);

    private native float[][] mnssd(byte[] pixels, float[]mean);
}
