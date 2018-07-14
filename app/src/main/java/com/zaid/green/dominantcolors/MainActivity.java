package com.zaid.green.dominantcolors;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback, FrameRenderer.OnFrameRenderedListener {

    // Constants
    private final int CAMERA_PERMISSION_CODE = 1;
    private final int CHECK_FRAME_EVERY_MILIS = 1000;

    // Objects
    private android.hardware.Camera mCamera;
    private CameraPreview mPreview;
    private SurfaceHolder surfaceHolder;
    private FrameRenderer frameRenderer;

    // Variables
    private long lastFrameCheckTimestamp = 0;

    // UI Views
    private FrameLayout preview;
    private FrameLayout bottomFrame;
    private TextView rgbTv;
    private TextView rgbTv2;
    private TextView rgbTv3;
    private TextView rgbTv4;
    private TextView rgbTv5;
    private TextView percentageTv;
    private TextView percentageTv2;
    private TextView percentageTv3;
    private TextView percentageTv4;
    private TextView percentageTv5;
    private CardView colorCardView;
    private CardView colorCardView2;
    private CardView colorCardView3;
    private CardView colorCardView4;
    private CardView colorCardView5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        frameRenderer = new FrameRenderer(this);
        frameRenderer.setOnRenderListener(this);
        initViews();
        // Getting permission for using the camera, opens if it has
        getCameraPermissionAndInit();
    }

    // https://developer.android.com/guide/topics/media/camera
    // I learned about the initialization of the camera from the android developers site
    private void initCamera() {
        if(mCamera == null) {
            // Create an instance of Camera
            mCamera = getCameraInstance();

            // Set camera display orientation
            // TODO handle changing camera orientation to fit device orientation.
            mCamera.setDisplayOrientation(90);
            mPreview = new CameraPreview(this, mCamera);
            surfaceHolder = mPreview.getmHolder();
            preview.addView(mPreview);

            try {
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();

            } catch (Exception e) {
                e.printStackTrace();
            }
            mCamera.setPreviewCallback(this);
        }
    }

    public void stopCamera() {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    //  https://blog.xamarin.com/requesting-runtime-permissions-in-android-marshmallow/
    //  Some information about handling camera permissions was taken for here
    //  The function checks if it has camera permission and calls init camera only if it has.
    private void getCameraPermissionAndInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show();
                initCamera();
            }
            else requestCameraPermission();
        } else {
            //TODO Handle for version lower then 23
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            stopCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCamera != null) {
            stopCamera();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCamera != null) {
            stopCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            // checks if there is permission and if it has opens camera
            getCameraPermissionAndInit();
        }
    }

    public void initViews() {
        preview = findViewById(R.id.camera_frame_layout);
        rgbTv = findViewById(R.id.rgbTv);
        rgbTv2 = findViewById(R.id.rgbTv2);
        rgbTv3 = findViewById(R.id.rgbTv3);
        rgbTv4 = findViewById(R.id.rgbTv4);
        rgbTv5 = findViewById(R.id.rgbTv5);
        bottomFrame = findViewById(R.id.colors_frame_layout);
        percentageTv = findViewById(R.id.colorPercentageTv);
        percentageTv2 = findViewById(R.id.colorPercentageTv2);
        percentageTv3 = findViewById(R.id.colorPercentageTv3);
        percentageTv4 = findViewById(R.id.colorPercentageTv4);
        percentageTv5 = findViewById(R.id.colorPercentageTv5);
        colorCardView = findViewById(R.id.colorDataCv);
        colorCardView2 = findViewById(R.id.colorDataCv2);
        colorCardView3 = findViewById(R.id.colorDataCv3);
        colorCardView4 = findViewById(R.id.colorDataCv4);
        colorCardView5 = findViewById(R.id.colorDataCv5);
    }

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)\
        }
        return c; // returns null if camera is unavailable
    }

    //  https://blog.xamarin.com/requesting-runtime-permissions-in-android-marshmallow/
    //  Information about handling camera permissions was taken for here
    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {

            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because the app requires camera")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    // The function is being called when a user decides to give a permission or not
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        long timestamp = new Date().getTime();
        // The function is being called for every frame but we check for every 1000 ms.
        if (timestamp - lastFrameCheckTimestamp > CHECK_FRAME_EVERY_MILIS) {
            lastFrameCheckTimestamp = timestamp;
            frameRenderer.renderFrameAsync(data, camera);
        }
    }

    @Override
    public void onRenderedMostUsedColors(ArrayList<FrameRenderer.ColorUsage> mostUsedColors) {

        // Calculating the percentage of a color according to the screen size.
        double percentage = mostUsedColors.get(0).getColorPercentage();
        double percentage2 = mostUsedColors.get(1).getColorPercentage();
        double percentage3 = mostUsedColors.get(2).getColorPercentage();
        double percentage4 = mostUsedColors.get(3).getColorPercentage();
        double percentage5 = mostUsedColors.get(4).getColorPercentage();


        percentageTv.setText(new DecimalFormat("##.####").format(percentage) + "%");
        percentageTv2.setText(new DecimalFormat("##.####").format(percentage2) + "%");
        percentageTv3.setText(new DecimalFormat("##.####").format(percentage3) + "%");
        percentageTv4.setText(new DecimalFormat("##.####").format(percentage4) + "%");
        percentageTv5.setText(new DecimalFormat("##.####").format(percentage5) + "%");

        String usedEntry = mostUsedColors.get(0).getColorHex();
        String usedEntry2 = mostUsedColors.get(1).getColorHex();
        String usedEntry3 = mostUsedColors.get(2).getColorHex();
        String usedEntry4 = mostUsedColors.get(3).getColorHex();
        String usedEntry5 = mostUsedColors.get(4).getColorHex();

        colorCardView.setBackgroundColor(Integer.parseInt(usedEntry));
        colorCardView2.setBackgroundColor(Integer.parseInt(usedEntry2));
        colorCardView3.setBackgroundColor(Integer.parseInt(usedEntry3));
        colorCardView4.setBackgroundColor(Integer.parseInt(usedEntry4));
        colorCardView5.setBackgroundColor(Integer.parseInt(usedEntry5));

        rgbTv.setText(convertColorHexToRgb(Integer.parseInt(usedEntry)));
        rgbTv2.setText(convertColorHexToRgb(Integer.parseInt(usedEntry2)));
        rgbTv3.setText(convertColorHexToRgb(Integer.parseInt(usedEntry3)));
        rgbTv4.setText(convertColorHexToRgb(Integer.parseInt(usedEntry4)));
        rgbTv5.setText(convertColorHexToRgb(Integer.parseInt(usedEntry5)));

    }

    // https://stackoverflow.com/questions/17183587/convert-integer-color-value-to-rgb
    // Information about conversion was taken from here
    public String convertColorHexToRgb(int colorHex) {
        int red = Color.red(colorHex);
        int green = Color.green(colorHex);
        int blue = Color.blue(colorHex);
        return "R:" + red + "G:" + green + "B:" + blue;
    }
}