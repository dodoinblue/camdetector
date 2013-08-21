
package com.example.org.charles.android.camdetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static android.view.SurfaceHolder.Callback;

public class MainActivity extends Activity {

    private Camera mCamera;
    private SurfaceView mLiveView;
    private SurfaceHolder mLiveViewHolder;
    private boolean mLoaded = false;
    private String PATH = Environment.getExternalStorageDirectory() + "/detector.jpg";

    private int mWidth;
    private int mHeight;

    private Callback mSurfaceHolderCallback = new Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            log("surfaceCreated");
            mLoaded = true;
            initLiveView();
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            log("surfaceChanged");
            mHeight = mCamera.getParameters().getPreviewSize().height;
            mWidth = mCamera.getParameters().getPreviewSize().width;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            log("surfaceDestroyed");
            mLoaded = false;
        }
    };

    Camera.AutoFocusMoveCallback afMoveCallback = new Camera.AutoFocusMoveCallback() {
        @Override
        public void onAutoFocusMoving(boolean b, Camera camera) {
//            log("onAutoFocusMoving: " + b);
        }
    };
    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {

            log("onPreviewFrame");
            log("saving frame");
            log("Frame size: WxH" + mWidth + "x" + mHeight);
            try {
                final YuvImage image = new YuvImage(bytes, ImageFormat.NV21, mWidth, mHeight,
                        null);
                File file = new File(PATH);
                FileOutputStream stream = new FileOutputStream(file);
                image.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 90, stream);
                stream.close();

                Bitmap picture = BitmapFactory.decodeFile(PATH);
                if (picture != null) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    log("picture size (WxH): " + picture.getWidth() + "x" + picture.getHeight());
                    Bitmap rotatedBitmap = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(),
                            picture.getHeight(), matrix, true);
                    saveBitmap(rotatedBitmap);
                    log("rotatedBitmap size (WxH): " + rotatedBitmap.getWidth() + "x" +
                            rotatedBitmap.getHeight());
                    int pixel = picture.getPixel(640, 360);
                    int alpha = Color.alpha(pixel);
                    int red = Color.red(pixel);
                    int blue = Color.blue(pixel);
                    int green = Color.green(pixel);
                    String color = String.format("#%02X%02X%02X%02X", alpha, red, green, blue);
//                    showMessage(color);
//                    mColorView.setBackgroundColor(pixel);
//                    mColorText.setText("Color is: " + color);
                    showDialog("Color is: " + color);
                } else {
                    log("pic is null");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private Button mDetect;
    private TextView mColorText;
//    private View mColorView;
    private Button mBtnFabric;
    private Button mBtnLiquid;

    private AlertDialog mDialog;
    private TextView mMaterial;

    private void showDialog(final String[] items){
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Make your selection");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                log(items[item] + "is pressed");
                mMaterial.setText(items[item]);
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private void showDialog(final String result){
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Material Detectd!");
        builder.setMessage(result);
        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mDialog.dismiss();
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if (view == mBtnFabric) {
                String[] list = {"Cotton", "Wool", "Huaxian"};
                showDialog(list);
            } else if (view == mBtnLiquid) {
                String[] list = {"Wine", "Milk"};
                showDialog(list);
            }
        }
    };

    private void showMessage(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void saveBitmap(Bitmap img) {
        File file = new File(PATH);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
            img.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mLiveView = (SurfaceView) findViewById(R.id.live_view);
        mLiveViewHolder = mLiveView.getHolder();

        mDetect = (Button) findViewById(R.id.detect);
        mColorText = (TextView) findViewById(R.id.color_text);
//        mColorView = findViewById(R.id.color_view);

        mDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                log("detecting...");
                mCamera.setOneShotPreviewCallback(previewCallback);
            }
        });

        mBtnFabric = (Button) findViewById(R.id.fabric);
        mBtnFabric.setOnClickListener(mOnClickListener);

        mBtnLiquid = (Button) findViewById(R.id.liquid);
        mBtnLiquid.setOnClickListener(mOnClickListener);

        mMaterial = (TextView) findViewById(R.id.material);
    }

    public void initLiveView() {
        if (mCamera == null) {
            log("initPreview called when mCamera is not available. (before onResume)");
            return;
        }
        requestParameters();
        try {
            mCamera.setPreviewDisplay(mLiveViewHolder);
        } catch (Exception e) {
            log(e.getMessage());
        }
        mCamera.startPreview();
    }

    public void requestParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureSize(1920, 1080);
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width / size.height == 1280 / 720) {
                log("Aspect Ratio match! Setting Preview size to : " + size.width + "x" + size
                        .height);
                parameters.setPreviewSize(size.width, size.height);
                break;
            }
        }
        mCamera.setParameters(parameters);
    }

    private void log(String s) {
        Log.i("Charles_TAG", s);
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume");
        mLiveView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mLiveViewHolder.addCallback(mSurfaceHolderCallback);
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);
        mCamera.setAutoFocusMoveCallback(afMoveCallback);
//        mCamera.setPreviewCallback(previewCallback);
        if (mLoaded) {
            log("starting preview");
            initLiveView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause");
        mLiveViewHolder.removeCallback(mSurfaceHolderCallback);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }
}
