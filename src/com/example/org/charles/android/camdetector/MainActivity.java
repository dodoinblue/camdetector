
package com.example.org.charles.android.camdetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextPaint;
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
//                    Matrix matrix = new Matrix();
//                    matrix.postRotate(90);
                    log("picture size (WxH): " + picture.getWidth() + "x" + picture.getHeight());
                    String color = getCenterColor(picture);
                    showResult(color);
                } else {
                    log("pic is null");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private String getCenterColor(Bitmap picture) {
        int w = picture.getWidth();
        int h = picture.getHeight();
        Bitmap subset = Bitmap.createBitmap(picture, w/2-24, h/2-24, 50, 50, null, true);
        int[] pixels = new int[2500];
        saveBitmap(subset);
        subset.getPixels(pixels, 0, 50, 0, 0, 50, 50);
        int average = averageColor(pixels);
        log("pixel color: " + average);
        int alpha = Color.alpha(average);
        int red = Color.red(average);
        int blue = Color.blue(average);
        int green = Color.green(average);
        String color = String.format("#%02X%02X%02X%02X", alpha, red, green, blue);
        log("Color: " + color);
        return color;
    }

    private int averageColor(int[] pixels) {
        int i = 0;
        int r = 0;
        int g = 0;
        int b = 0;
        for(int pixel : pixels) {
            i++;
            r = r + Color.red(pixel);
            g = g + Color.green(pixel);
            b = b + Color.blue(pixel);
//            log(i + " : " + pixel);
        }
        log("Count: " + i);
        return Color.rgb((int) r / i, (int) g / i, (int) b / i);
    }

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

    private void showResult(final String color){
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        log("color int: " + Color.parseColor(color));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Material Detectd!");

        String result = generateResult(getYUV_Y(color));
        builder.setMessage("Reflectivity value is " + getYUV_Y(color) + "\n" + result);
        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mDialog.dismiss();
            }
        });
        ShapeDrawable icon = new ShapeDrawable(new RectShape());
        icon.setIntrinsicWidth(50);
        icon.setIntrinsicHeight(50);
        icon.setColorFilter(Color.parseColor(color), PorterDuff.Mode.ADD);
        builder.setIcon(icon);
        mDialog = builder.create();
        mDialog.show();
    }

    private String generateResult(float yuv_y) {
        String result = "";
        log("material = " + mMaterial.getText());
        if(mMaterial.getText().equals("Cotton")) {
            log("parsing cotton value");
            result = parseCotton(yuv_y);
        } else if (mMaterial.getText().equals("Woolen")) {
            log("parsing woolen value");
            result = parseWoolen(yuv_y);
        }
        return result;
    }

    private String parseWoolen(float yuv_y) {
        if( yuv_y > 40.0f && yuv_y < 60.0f) {
            return "Less than 50% woolen";
        } else if( yuv_y > 60.1f && yuv_y < 70.0f) {
            return "About 60% woolen";
        } else if( yuv_y > 70.1f && yuv_y < 80.0f) {
            return "About 70% woolen";
        } else if( yuv_y > 80.1f && yuv_y < 90.0f) {
            return "About 80% woolen";
        } else if( yuv_y > 90.1f && yuv_y < 110.0f) {
            return "About 90% woolen";
        }

        return "Unknown material";
    }

    private String parseCotton(float yuv_y) {
        if (yuv_y > 50.0f && yuv_y < 80.0f) {
            return "Blended fabric";
        } else if (yuv_y > 100.0f) {
            return "Pure cotton";
        }
        return "Unknown material";
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if (view == mBtnFabric) {
                String[] list = {"Cotton", "Woolen"};
                showDialog(list);
            } else if (view == mBtnLiquid) {
                String[] list = {"Wine", "Milk"};
                showDialog(list);
            }
        }
    };

    private float getYUV_Y(int r, int g, int b) {
        float result;
        // Y=0.3R+0.59G+0.11B
        result = 0.3f * r + 0.59f * g + 0.11f * b;
        return result;
    }

    private float getYUV_Y(String color) {
        int c = Color.parseColor(color);
        int r = Color.red(c);
        int g = Color.green(c);
        int b = Color.blue(c);
        return getYUV_Y(r, g, b);
    }

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
//        mColorText = (TextView) findViewById(R.id.color_text);
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
        TextPaint tp1 = mMaterial.getPaint();
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
            if (size.width * 100 / size.height == 1280 * 100 / 720) {
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
