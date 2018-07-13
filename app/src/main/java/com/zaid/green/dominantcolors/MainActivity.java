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
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    private android.hardware.Camera mCamera;
    private CameraPreview mPreview;
    private SurfaceHolder surfaceHolder;
    private Thread thread;
    // variables
    private int CAMERA_PERMISSION_CODE = 1;
    private int frameHeight;
    private int frameWidth;
    private byte[] cameraData;
    // UI Views
    private FrameLayout preview;
    private FrameLayout bottomFrame;
    private TextView percentageTv;
    private TextView percentageTv2;
    private CardView colorCardView;
    private CardView colorCardView2;

    @RequiresApi(api = Build.VERSION_CODES.M) // TODO check on that
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        // Create an instance of Camera
        mCamera = getCameraInstance();
        //getting parameters
        Camera.Parameters parameters = mCamera.getParameters();
        frameWidth = parameters.getPreviewSize().width;
        frameHeight = parameters.getPreviewSize().height;
        cameraData = new byte[frameHeight * frameWidth];
        mCamera.setDisplayOrientation(90);
        mPreview = new CameraPreview(this, mCamera);
        surfaceHolder = mPreview.getmHolder();
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show();
        else requestCameraPermission();
        preview.addView(mPreview);

        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }
        mCamera.setPreviewCallback(previewCallback);
        updateColors();
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
        thread.interrupt();
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

    public void updateColors() {

        thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(10000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                previewCallback.onPreviewFrame(cameraData, mCamera);
                            }

                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                thread.interrupt();
            }
        };
        thread.start();
    }


    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // Do something with the frame
            Camera.Parameters parameters = camera.getParameters();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            YuvImage yuvImage = new YuvImage(data, parameters.getPreviewFormat(), parameters.getPreviewSize().width, parameters.getPreviewSize().height, null);
            yuvImage.compressToJpeg(new Rect(0, 0, parameters.getPreviewSize().width, parameters.getPreviewSize().height), 90, out);
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
            renderImageFrame(bitmap);
        }
    };

    public void renderImageFrame(Bitmap bitmap) {
        HashMap<String, Integer> pixelValues = findColorUsage(bitmap);
        ArrayList<HashMap.Entry> mostUsedColorsEntries;
        mostUsedColorsEntries = findTopColors(pixelValues);
        Log.i("fafa" , pixelValues.get(mostUsedColorsEntries.get(1).getKey() + "") + "");
        Log.i("kaka" , (frameHeight * frameWidth) + "");
        Log.i("allShit",   (pixelValues.get(mostUsedColorsEntries.get(0).getKey() + "") / (frameHeight * frameWidth)) + "");
        double percentage =  100 *  (pixelValues.get(mostUsedColorsEntries.get(0).getKey() + "") / (double) (frameHeight * frameWidth));
        double percentage2 = 100 * (pixelValues.get(mostUsedColorsEntries.get(1).getKey() + "") / (double) (frameHeight * frameWidth));
        percentageTv.setText(percentage + "%");
        percentageTv2.setText(percentage2 + "%");
        String usedEntry = (String) mostUsedColorsEntries.get(0).getKey();
        String usedEntry2 = (String) mostUsedColorsEntries.get(1).getKey();
        colorCardView.setBackgroundColor(Integer.parseInt(usedEntry));
        colorCardView2.setBackgroundColor(Integer.parseInt(usedEntry2));
    }


    public HashMap<String, Integer> findColorUsage(Bitmap bitmap) {
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
        for(HashMap.Entry<String, Integer> entry : sortedPixels.entrySet()) {
            if(mostUsedColors.size() < 5) mostUsedColors.add(entry);
            else if (mostUsedColors.size() == 5) {
                 minPos = findMinPos(mostUsedColors);
                 minValue = (int) mostUsedColors.get(minPos).getValue();
            }
            else if(entry.getValue() > minValue) {
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
        for(int i = 1; i < arrayList.size(); i ++) {
            if((int) arrayList.get(i).getValue() < minValue) {
                minValue = (int) arrayList.get(i).getValue();
                minPos = i;
            }
        }
        return  minPos;
    }
}

