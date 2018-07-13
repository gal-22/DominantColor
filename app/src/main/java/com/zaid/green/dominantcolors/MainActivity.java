package com.zaid.green.dominantcolors;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.YuvImage;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback {

    // Constants
    private final int CAMERA_PERMISSION_CODE = 1;

    // Objects
    private android.hardware.Camera mCamera;
    private CameraPreview mPreview;
    private SurfaceHolder surfaceHolder;

    // Variables
    private long lastFrameCheckTimestamp = 0;

    // UI Views
    private FrameLayout preview;
    private FrameLayout bottomFrame;
    private TextView percentageTv;
    private TextView percentageTv2;
    private CardView colorCardView;
    private CardView colorCardView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initCamera();
    }

    private void initCamera() {
        // Getting permission for using the camera
        getCameraPermission();

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

    private void getCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show();
            else requestCameraPermission();
        } else {
            //TODO Handle for version lower then 23
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            mCamera = getCameraInstance();
            mPreview = new CameraPreview(this, mCamera);
            mCamera.setDisplayOrientation(90);
            surfaceHolder = mPreview.getmHolder();
            preview.addView(mPreview);
            try {
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void initViews() {
        preview = findViewById(R.id.camera_frame_layout);
        bottomFrame = findViewById(R.id.colors_frame_layout);
        percentageTv = findViewById(R.id.colorPercentage);
        colorCardView = findViewById(R.id.colorDataCv);
        percentageTv2 = findViewById(R.id.percentageTv2);
        colorCardView2 = findViewById(R.id.colorDataCv2);

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

    public Bitmap createBitmapFromFrame(byte[] frameData, Camera.Parameters parameters, int widthFrame, int heightFrame) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(frameData, parameters.getPreviewFormat(), widthFrame, heightFrame, null);
        yuvImage.compressToJpeg(new Rect(0, 0, widthFrame, heightFrame), 90, out);
        byte[] imageBytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        try {
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public void renderImageFrame(Bitmap bitmap, int frameWidth, int frameHeight) {
        HashMap<String, Integer> pixelValues = findColorUsage(bitmap, frameWidth, frameHeight);
        ArrayList<HashMap.Entry> mostUsedColorsEntries;
        mostUsedColorsEntries = findTopColors(pixelValues);


        // Calculating the percentage of a color according to the screen size.
        double percentage = 100 * (pixelValues.get(mostUsedColorsEntries.get(0).getKey() + "") / (double) (frameHeight * frameWidth));
        double percentage2 = 100 * (pixelValues.get(mostUsedColorsEntries.get(1).getKey() + "") / (double) (frameHeight * frameWidth));

        percentageTv.setText(percentage + "%");
        percentageTv2.setText(percentage2 + "%");

        String usedEntry = (String) mostUsedColorsEntries.get(0).getKey();
        String usedEntry2 = (String) mostUsedColorsEntries.get(1).getKey();

        colorCardView.setBackgroundColor(Integer.parseInt(usedEntry));
        colorCardView2.setBackgroundColor(Integer.parseInt(usedEntry2));
    }


    public HashMap<String, Integer> findColorUsage(Bitmap bitmap, int frameWidth, int frameHeight) {
        HashMap<String, Integer> pixelValues = new HashMap<>();
        for (int w = 0; w < frameWidth; w++) {
            for (int h = 0; h < frameHeight; h++) {
                int pixel = bitmap.getPixel(w, h);
                int redValue = Color.red(pixel);
                int blueValue = Color.blue(pixel);
                int greenValue = Color.green(pixel);
                int color = Color.rgb(redValue, greenValue, blueValue);
                if (pixelValues.get(color + "") == null) {
                    pixelValues.put(color + "", 1);
                } else {
                    int value = pixelValues.get(color + "") + 1;
                    pixelValues.put(color + "", value);
                }
            }
        }
        return pixelValues;
    }


    // receives Hashtable with all pixels recurrences returns top 5 most used colors
    public ArrayList<HashMap.Entry> findTopColors(HashMap<String, Integer> sortedPixels) {
        ArrayList<HashMap.Entry> mostUsedColors = new ArrayList<>();
        int minPos = 0;
        int minValue = 0;
        for (HashMap.Entry<String, Integer> entry : sortedPixels.entrySet()) {
            if (mostUsedColors.size() < 5) mostUsedColors.add(entry);
            else if (mostUsedColors.size() == 5) {
                minPos = findMinPos(mostUsedColors);
                minValue = (int) mostUsedColors.get(minPos).getValue();
            } else if (entry.getValue() > minValue) {
                mostUsedColors.add(minPos, entry);
                minPos = findMinPos(mostUsedColors);
                minValue = (int) mostUsedColors.get(minPos).getValue();
            }
        }
        return mostUsedColors;
    }

    public int findMinPos(ArrayList<HashMap.Entry> arrayList) {
        int minValue = (int) arrayList.get(0).getValue();
        int minPos = 0;
        for (int i = 1; i < arrayList.size(); i++) {
            if ((int) arrayList.get(i).getValue() < minValue) {
                minValue = (int) arrayList.get(i).getValue();
                minPos = i;
            }
        }
        return minPos;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        long timestamp = new Date().getTime();
        // The function is being called for every frame but we check for every 100 ms.
        if (timestamp - lastFrameCheckTimestamp > 100) {
            lastFrameCheckTimestamp = timestamp;
            // Getting frame's width and height.
            int widthFrame = camera.getParameters().getPreviewSize().width;
            int heightFrame = camera.getParameters().getPreviewSize().height;

            // Creating bitmap from data and camera parameters
            Bitmap bitmap = createBitmapFromFrame(data, camera.getParameters(), widthFrame, heightFrame);

            // Rendering the image from bitmap
            renderImageFrame(bitmap, widthFrame, heightFrame);
        }
    }
}

