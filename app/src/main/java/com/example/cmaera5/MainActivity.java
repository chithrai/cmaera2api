package com.example.cmaera5;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompatSideChannelService;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AndroidCameraApi";
    private Button takePictureButton,nextactivity;
    private TextureView textureView;
    private static  final int REQUEST_CAMERA_PERMISSION_RESULT=0;
    private static  final int REQUEST_WRITE_STORAGE_PERMISSION_RESULT=1;
    private static final int STATE_PREVIEW=0;
    private static final int STATE_WAIT_LOCK=1;
    private int mCaptureState = STATE_PREVIEW;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final long LOCK_FOCUS_DELAY_ON_FOCUSED = 5000;
    private static final long LOCK_FOCUS_DELAY_ON_UNFOCUSED = 1000;

    private Integer mLastAfState = null;
    private Handler mUiHandler = new Handler(); // UI handler

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private CameraCaptureSession mPreviewCaptureSession;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {


        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process(result);
            Log.d("on capture complete","true");
        }
    };

    private void process(TotalCaptureResult captureResult)
    {
        Log.d("process","called");
        Log.d("mCaptureState", String.valueOf(mCaptureState));
        switch (mCaptureState)
        {

            case STATE_PREVIEW:
                break;
            case STATE_WAIT_LOCK:
                mCaptureState = STATE_PREVIEW;
//                startStillCaptureRequest();
                Log.d("STATE_WAIT_LOCK", String.valueOf(STATE_WAIT_LOCK));

                Integer afstate = captureResult.get(CaptureResult.CONTROL_AF_STATE);

                Log.d("afstate", String.valueOf(afstate));
                Log.d("CONTROL_AF_STATE", String.valueOf(CaptureResult.CONTROL_AF_STATE));
                Log.d("CONTROL_AF_STATE_FOCUSED_LOCKED", String.valueOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED));
                Log.d("CONTROL_AF_STATE_NOT_FOCUSED_LOCKED", String.valueOf(CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED));
                Log.d("CONTROL_AF_STATE_PASSIVE_FOCUSED", String.valueOf(CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED));
                if (afstate == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                  //  restartFocus();
                }
                if (afstate == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED )
                {
                    Log.d("Auto focus locked","true");
                    Toast.makeText(MainActivity.this, "Auto Focus Locked", Toast.LENGTH_SHORT).show();
                    startStillCaptureRequest();
                }

                break;
        }
    }

    private static  class  CompareSiebyArea implements Comparator<Size>{
        @Override
        public int compare(Size size, Size t1) {
            return Long.signum((long) size.getWidth() * size.getHeight() / (long) t1.getWidth() * t1.getHeight());
        }
    }

    final private  TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            setupCamera(i,i1);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };
    private CameraDevice mCameraDevice;
    final private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int i) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private String mCameraId;
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private Size mPreviewSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private int mTotalRotation;



    private Size mImageSize;
    private ImageReader mImageReader;
    private File mImageFolder;
    private String mImageFilename;

    private final ImageReader.OnImageAvailableListener monImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(new  InmageSaver(reader.acquireNextImage()));
        }
    };

    private class InmageSaver implements  Runnable
    {
        private final Image mImage;
        public InmageSaver(Image image)
        {
            mImage = image;
        }
        @Override
        public void run() {
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes =  new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(mImageFilename);
                fileOutputStream.write(bytes);
                String strBase64= Base64.encodeToString(bytes, Base64.DEFAULT);

                awsinsert(strBase64);
            } catch (FileNotFoundException | NullPointerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                mImage.close();
                if (fileOutputStream != null)
                {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void awsinsert(String mstrBase64)
    {
                                            JSONObject js = new JSONObject();
                                try {
                                    js.put("imagebinary",String.valueOf(mstrBase64));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                String url = "https://uzob2ah004.execute-api.ap-south-1.amazonaws.com/dev/uploadweightdetailsimage";
                                JsonObjectRequest jsonObjReq = new JsonObjectRequest(
                                        Request.Method.POST, url, js,
                                        new Response.Listener<JSONObject>() {
                                            @Override
                                            public void onResponse(JSONObject response) {
                                                Log.d("Volley response", response.toString());
                                            }
                                        }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        VolleyLog.d("Volley Error", "Error: " + error.getMessage());
                                    }
                                }) {

                                    /**
                                     * Passing some request headers
                                     */
                                    @Override
                                    public Map<String, String> getHeaders() throws AuthFailureError {
                                        HashMap<String, String> headers = new HashMap<String, String>();
                                        headers.put("Content-Type", "application/json; charset=utf-8");
                                        return headers;
                                    }

                                };

                                // Adding request to request queue
                                Volley.newRequestQueue(MainActivity.this).add(jsonObjReq);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkWriteStoragePermission();
        createImageFolder();
        Log.d("Focus Supported", String.valueOf(isAutoFocusSupported()));
        textureView = findViewById(R.id.texture);
        takePictureButton = findViewById(R.id.btn_takepicture);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lockFocus();
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if(textureView.isAvailable())
        {
            setupCamera(textureView.getWidth(), textureView.getHeight());
            connectCamera();
        }
        else
        {
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "Will not run Without Camera", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_WRITE_STORAGE_PERMISSION_RESULT)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "cant save without permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();

    }

    private void setupCamera(int width, int height)
    {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList())
            {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation = sensorDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation)
                {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }

                mPreviewSize = chooseOptionalSize(map.getOutputSizes(SurfaceTexture.class), 1280, 720);
                mImageSize = chooseOptionalSize(map.getOutputSizes(ImageFormat.JPEG), 1280, 720);
                mImageReader = ImageReader.newInstance(1280, 720, ImageFormat.JPEG,2);
                mImageReader.setOnImageAvailableListener(monImageAvailableListener,mBackgroundHandler);
                mCameraId = cameraId;
                return;

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void connectCamera()
    {
        CameraManager cameraManager =(CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                {
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                }
                else
                {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                    {
                        Toast.makeText(this, "App required Camera Access", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            }
            else
            {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }

        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }

    }

    private void startPreview()
    {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(1280, 720);
        Surface previewSurface = new Surface(surfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface,mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mPreviewCaptureSession = session;
                    try {
                        session.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "unable to setup camera", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startStillCaptureRequest()
    {

        Log.d("stillcapturerequest","true");
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_QUALITY,(byte)100 );
            CameraCaptureSession.CaptureCallback stillCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    try {
                        createImageFileName();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void closeCamera()
        {
            if (mCameraDevice != null)
            {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        private  void startBackgroundThread()
        {
            mBackgroundHandlerThread = new HandlerThread("camera2api");
            mBackgroundHandlerThread.start();
            mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());

        }

        private  void stopBackgroundThread()
        {
            mBackgroundHandlerThread.quitSafely();
            try {
                mBackgroundHandlerThread.join();
                mBackgroundHandlerThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        private static int sensorDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation)
        {
            int sesorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            deviceOrientation = ORIENTATIONS.get(deviceOrientation);
            return (sesorOrientation + deviceOrientation + 360) %360;
        }

        private static Size chooseOptionalSize(Size[] choices, int width, int height)
        {
            List<Size> bigEnough = new ArrayList<Size>();
            for (Size option : choices)
            {
                if ((option.getHeight() == option.getHeight() * height / width)
                        && (option.getWidth() >= width
                        && option.getHeight() >= height)) {
                    bigEnough.add(option);
                    }
            }
            if (bigEnough.size() > 0)
            {
                return Collections.min(bigEnough, new CompareSiebyArea());
            }
            else
            {
                return  choices[0];
            }
        }

        private void createImageFolder()
        {
            File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            mImageFolder = new File(imageFile,"Camera2Image");
            if (!mImageFolder.exists())
            {
                mImageFolder.mkdirs();
            }
        }

        private File createImageFileName() throws IOException {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String prepend = "IMG_"+timestamp+"_";

            File imageFile = File.createTempFile(prepend,".jpg",mImageFolder);
                mImageFilename = imageFile.getAbsolutePath();
                return imageFile;

        }

        private void checkWriteStoragePermission()
        {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    {
                        Toast.makeText(this, "App required storage Access", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_PERMISSION_RESULT);
                }
            }
        }
    private void restartFocus(){
        mCaptureState = STATE_PREVIEW;
        try {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(),mPreviewCaptureCallback, mBackgroundHandler);

            lockFocus();

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
        private void lockFocus()
        {

            try {
                mCaptureState = STATE_WAIT_LOCK;
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

    private boolean isAutoFocusSupported() {
       return getPackageManager().hasSystemFeature("android.hardware.camera.autofocus");

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean isHardwareLevelSupported(int requiredLevel) {
        boolean res = false;
        if (mCameraId == null)
            return res;
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(mCameraId);

            int deviceLevel = cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            switch (deviceLevel) {
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                    Log.d(TAG, "Camera support level: INFO_SUPPORTED_HARDWARE_LEVEL_3");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                    Log.d(TAG, "Camera support level: INFO_SUPPORTED_HARDWARE_LEVEL_FULL");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                    Log.d(TAG, "Camera support level: INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                    Log.d(TAG, "Camera support level: INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED");
                    break;
                default:
                    Log.d(TAG, "Unknown INFO_SUPPORTED_HARDWARE_LEVEL: " + deviceLevel);
                    break;
            }


            if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                res = requiredLevel == deviceLevel;
            } else {
                // deviceLevel is not LEGACY, can use numerical sort
                res = requiredLevel <= deviceLevel;
            }

        } catch (Exception e) {
            Log.e(TAG, "isHardwareLevelSupported Error", e);
        }
        return res;
    }

    private float getMinimumFocusDistance() {
        if (mCameraId == null)
            return 0;

        Float minimumLens = null;
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics c = manager.getCameraCharacteristics(mCameraId);
            minimumLens = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        } catch (Exception e) {
            Log.e(TAG, "isHardwareLevelSupported Error", e);
        }
        if (minimumLens != null)
            return minimumLens;
        return 0;
    }
}