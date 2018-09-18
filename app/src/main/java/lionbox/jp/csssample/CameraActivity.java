package lionbox.jp.csssample;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import lionbox.jp.csssample.utils.Utilities;

import static lionbox.jp.csssample.Constants.ACTION_FORWARD;
import static lionbox.jp.csssample.Constants.ACTION_SERVER;
import static lionbox.jp.csssample.Constants.CAMERA_DIR;
import static lionbox.jp.csssample.Constants.F_STATUS_CAMERA_OPEN;
import static lionbox.jp.csssample.Constants.F_STATUS_SHUTTER;
import static lionbox.jp.csssample.Constants.F_STATUS_STANDBY;
import static lionbox.jp.csssample.Constants.F_STATUS_UPLOAD_OK;
import static lionbox.jp.csssample.Constants.PARAM_FILE_PATH;
import static lionbox.jp.csssample.Constants.PARAM_STATUS;
import static lionbox.jp.csssample.Constants.PARAM_TOKEN;
import static lionbox.jp.csssample.Constants.S_STATUS_UPLOAD;

public class CameraActivity extends AppCompatActivity {

    /**
     * local broadcast manager
     */
    private LocalBroadcastManager mBroadcastReceiver;

    /**
     * CentralReceiver
     */
    private CentralReceiver mCentralReceiver;

    private TextureView mTextureView;

    private CameraManager mCameraManager;
    /**
     * request code
     */
    static final int REQUEST_CODE = 1;

    /**
     * camera device
     */
    private CameraDevice mCameraDevice;

    private Surface mPreviewSurface;

    /**
     * point
     */
    private Point mPoint;

    /**
     * capture session
     */
    private CameraCaptureSession mCaptureSession = null;

    /**
     * ImageReader
     */
    private ImageReader mImageReader;

    /**
     * camera id
     */
    private String CameraId;

    public CameraActivity() {
        //mCaptureCallback = new CaptureCallback();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    // パーミッションが必要であることを明示するアプリケーション独自のUIを表示
                }
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA
                }, REQUEST_CODE);
                return;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                initialize();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * initialize
     */
    private void initialize() throws CameraAccessException {

        try {
            Display display = getWindowManager().getDefaultDisplay();
            mPoint = new Point();
            display.getSize(mPoint);

            mTextureView = findViewById(R.id.camera2_texture);

            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

            // camera open
            mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            mImageReader = ImageReader.newInstance(mPoint.x, mPoint.y, ImageFormat.JPEG, 3);
            mImageReader.setOnImageAvailableListener(mTakePictureAvailableListener, null);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

                String[] idList = mCameraManager.getCameraIdList();
                for (String id : idList) {
                    CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                    Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        CameraId = id;
                        mCameraManager.openCamera(CameraId, mStateCallback, null);
                        break;
                    }
                }
            }

            if (mBroadcastReceiver == null) {
                mBroadcastReceiver = LocalBroadcastManager.getInstance(getApplicationContext());
                mCentralReceiver = new CentralReceiver();
                // レシーバのフィルタをインスタンス化
                final IntentFilter filter = new IntentFilter();
                // フィルタのアクション名を設定する（文字列の内容は任意）
                filter.addAction(ACTION_FORWARD);
                // 登録
                mBroadcastReceiver.registerReceiver(mCentralReceiver, filter);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * createCameraPreviewSession
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(mPoint.x, mPoint.y);
            mPreviewSurface = new Surface(texture);

            mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface,
                    mImageReader.getSurface()),
                    mSessionCallback,
                    null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    createCameraPreviewSession();
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
            };

    CameraDevice.StateCallback mStateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraDevice = camera;
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {

                }
            };

    CameraCaptureSession.StateCallback mSessionCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCaptureSession = session;
                    try {
                        CaptureRequest.Builder captureBuilder =
                                mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        captureBuilder.addTarget(mPreviewSurface);
                        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        mCaptureSession.setRepeatingRequest(captureBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            };

    ImageReader.OnImageAvailableListener mTakePictureAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    File dirPath = new File(getCacheDir().getPath() + "/" + CAMERA_DIR);
                    if (!dirPath.exists()) {
                        if (!dirPath.mkdirs()) {
                            Log.d("TAG", "----error");
                        }
                    }

                    ByteBuffer buffer = reader.acquireLatestImage().getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);

                    // 画像の書き込み
                    OutputStream output = null;
                    File fileName = new File(dirPath.getPath() + "/" + "camera.jpeg");
                    try {
                        output = new FileOutputStream(fileName);
                        output.write(bytes);
                    } catch (Exception e) {
                        // 例外処理
                        e.printStackTrace();
                    }
                    Log.d("TAG", "---file[" + fileName.getPath() + "][" + fileName.exists());

                    // ImageReaderのクローズ
                    reader.close();

                    mCaptureSession.close();
                    mCameraDevice.close();

                    // file upload 依頼
                    Intent intent = new Intent();
                    intent.setAction(ACTION_SERVER);
                    intent.putExtra(PARAM_STATUS, S_STATUS_UPLOAD);
                    intent.putExtra(PARAM_FILE_PATH, fileName.getPath());
                    mBroadcastReceiver.sendBroadcast(intent);
                }
            };

    /**
     * onShutter button
     * @param view
     */
    public void onShutterButton(View view) {
        onShutter();
    }

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * onShutter
     */
    public void onShutter() {
        try {
            CameraManager cameraManager =
                    (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            int sensorOrientation = 0;
            try {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(CameraId);
                Integer value = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                if (value != null) {
                    sensorOrientation = value;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360;


            CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientation);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), new CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }

                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                    super.onCaptureProgressed(session, request, partialResult);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                }

                @Override
                public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                    super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                }

                @Override
                public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
                    super.onCaptureSequenceAborted(session, sequenceId);
                }

                @Override
                public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
                    super.onCaptureBufferLost(session, request, target, frameNumber);
                }
            }, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * local broadcast receiver
     */
    private class CentralReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (ACTION_FORWARD.equals(intent.getAction())) {
                    int status = intent.getIntExtra(PARAM_STATUS, 0);
                    switch (status) {
                        case F_STATUS_STANDBY: // スタンバイOK
                            break;
                        case F_STATUS_CAMERA_OPEN: // カメラOPEN
                            break;
                        case F_STATUS_SHUTTER: // カメラシャッター
                            onShutter();
                            break;
                        case F_STATUS_UPLOAD_OK: // UPLOAD OK
                            finish();
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
