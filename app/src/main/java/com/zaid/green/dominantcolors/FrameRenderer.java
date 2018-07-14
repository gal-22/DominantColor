package com.zaid.green.dominantcolors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.support.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class FrameRenderer {

    // Constants
    private final int MISTAKE_OFFSET = 20;

    // Variables
    private Context context;

    // Listeners
    @Nullable
    private OnFrameRenderedListener onFrameRenderedListener;

    // Constructors

    public FrameRenderer(Context context) {
        this.context = context;
    }
    // https://blog.mindorks.com/android-core-looper-handler-and-handlerthread-bd54d69fe91a
    // Some information about running threads in background was taken from here
    public void renderFrameAsync(final byte[] data, final Camera camera) {
        new Thread(new Runnable() {
            public void run() {
                final ArrayList<ColorUsage> mostUsedColors = renderFrame(data, camera);

                if (onFrameRenderedListener != null) {
                    new android.os.Handler(context.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            onFrameRenderedListener.onRenderedMostUsedColors(mostUsedColors);
                        }
                    });
                }
            }
        }).start();
    }

    private ArrayList<ColorUsage> renderFrame(byte[] data, Camera camera) {
        // Getting frame's width and height.
        int widthFrame = camera.getParameters().getPreviewSize().width;
        int heightFrame = camera.getParameters().getPreviewSize().height;

        // Creating bitmap from data and camera parameters
        Bitmap bitmap = createBitmapFromFrame(data, camera.getParameters(), widthFrame, heightFrame);

        HashMap<String, Integer> pixelValues = findColorUsage(bitmap, widthFrame, heightFrame);
        bitmap.recycle();

        ArrayList<HashMap.Entry<String, Integer>> mostUsedColorsEntries = findTopColors(pixelValues);
        ArrayList<ColorUsage> mostUsedColors = new ArrayList<>();
        for (HashMap.Entry<String, Integer> entry : mostUsedColorsEntries) {
            // The percentage was calculated from MISTAKE_OFFSET^2 of the frame size
            double percentage = calculatePercentage(entry.getValue(), (widthFrame * heightFrame) / (MISTAKE_OFFSET * MISTAKE_OFFSET));
            ColorUsage colorUsage = new ColorUsage(entry.getKey(), percentage);
            mostUsedColors.add(colorUsage);
        }
        return mostUsedColors;
    }

    private double calculatePercentage(int of, double from) {
        return (100 * of) / from;
    }

    // https://stackoverflow.com/questions/25505973/how-do-i-convert-raw-camera-data-into-a-bitmap-on-android
    // Some information about how to convert the frame to data was taken from here
    private Bitmap createBitmapFromFrame(byte[] frameData, Camera.Parameters parameters, int widthFrame, int heightFrame) {
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


    private HashMap<String, Integer> findColorUsage(Bitmap bitmap, int frameWidth, int frameHeight) {
        HashMap<String, Integer> pixelValues = new HashMap<>();
        for (int w = 0; w < frameWidth; w += MISTAKE_OFFSET) {
            for (int h = 0; h < frameHeight; h += MISTAKE_OFFSET) {
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
    private ArrayList<HashMap.Entry<String, Integer>> findTopColors(HashMap<String, Integer> sortedPixels) {
        ArrayList<HashMap.Entry<String, Integer>> mostUsedColors = new ArrayList<>();
        int minPos = 0;
        int minValue = 0;
        for (HashMap.Entry<String, Integer> entry : sortedPixels.entrySet()) {
            if (mostUsedColors.size() < 5) mostUsedColors.add(entry);
            else if (mostUsedColors.size() == 5) {
                minPos = findMinPos(mostUsedColors);
                minValue = mostUsedColors.get(minPos).getValue();
            } else if (entry.getValue() > minValue) {
                mostUsedColors.add(minPos, entry);
                minPos = findMinPos(mostUsedColors);
                minValue = mostUsedColors.get(minPos).getValue();
            }
        }
        return mostUsedColors;
    }

    private int findMinPos(ArrayList<HashMap.Entry<String, Integer>> arrayList) {
        int minValue = arrayList.get(0).getValue();
        int minPos = 0;
        for (int i = 1; i < arrayList.size(); i++) {
            if (arrayList.get(i).getValue() < minValue) {
                minValue = arrayList.get(i).getValue();
                minPos = i;
            }
        }
        return minPos;
    }

    public void setOnRenderListener(@Nullable OnFrameRenderedListener listener) {
        onFrameRenderedListener = listener;
    }

    public class ColorUsage {
        private String colorHex;
        private double colorPercentage;

        public ColorUsage(String colorHex, double colorPercentage) {
            this.colorHex = colorHex;
            this.colorPercentage = colorPercentage;
        }

        public String getColorHex() {
            return colorHex;
        }

        public void setColorHex(String colorHex) {
            this.colorHex = colorHex;
        }

        public double getColorPercentage() {
            return colorPercentage;
        }

        public void setColorPercentage(double colorPercentage) {
            this.colorPercentage = colorPercentage;
        }
    }

    public interface OnFrameRenderedListener {
        void onRenderedMostUsedColors(ArrayList<ColorUsage> mostUsedColors);
    }
}
