package com.example.jazz.objectdetection.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Created by jazz on 3/9/17.
 */

public class CaffeInterface {

    private static String TAG = "CaffeInterface";

    private final String[] Classes = {"__background__",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};

    class CaffeImage{
        byte[] pixels;
        int channels;
        float[] im_info = new float[3];
        int[] ori_img_info = new int[2];
    }
    
    public List<PredictionResult> detectImage(String fileName, float[] mean, int model) {
        float[][] result = null;
        if (model == 0) {
            Log.i(TAG,"Faster RCNN");
            CaffeImage image = readImageFasterRCNN(fileName);
            result = fasterRCNN(image.pixels, image.channels, mean, image.im_info, image.ori_img_info);
        }
        else if (model == 1){
            Log.i(TAG,"SSD");
            CaffeImage image = readImageSSD(fileName);
            result = ssd(image.pixels, mean, image.ori_img_info);
        }
        else if (model == 2){
            Log.i(TAG,"MNSSD");
            CaffeImage image = readImageSSD(fileName);
            result = mnssd(image.pixels, mean, image.ori_img_info);
        }

        List <PredictionResult> rets = new ArrayList<PredictionResult>();
        if (result == null){
            return null;
        }
        for (int i = 0; i < result.length; i++){
            PredictionResult pr = new PredictionResult(result[i][0], result[i][1], result[i][2], result[i][3], result[i][4], Classes[(int)result[i][5]]);
            rets.add(pr);
        }

        return rets;
    }

    private CaffeImage readImageFasterRCNN(String fileName) {
        //Log.i(TAG, "readImage: reading: " + fileName);
        // Read image file to bitmap (in ARGB format)
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inPremultiplied = false;
        Bitmap bitmap = BitmapFactory.decodeFile(fileName, options);

        // Scale image down if one side is larger than 500
        CaffeImage image = new CaffeImage();

        final int  max_input_side = 500;
		final int  min_input_side = 300;

        image.ori_img_info[0] = bitmap.getHeight();
        image.ori_img_info[1] = bitmap.getWidth();


        int max_side = max(image.ori_img_info[0], image.ori_img_info[1]);
        int min_side = min(image.ori_img_info[0], image.ori_img_info[1]);

        //Log.i(TAG, "max side: " + max_side + "   min side: " + min_side);

        float max_side_scale = (float)max_side / max_input_side;
        float min_side_scale = (float)min_side / min_input_side;
        float max_scale = max(max_side_scale, min_side_scale);

        //Log.i(TAG, "max side scale: " + max_side_scale + "   min side scale: " + min_side_scale + "   max scale: "+ max_scale);
        image.im_info[2] = (float)1.0;

        if (max_scale > 1){
            image.im_info[2] = (float)1 / max_scale;
        }

        //Log.i(TAG, "scale:" + image.im_info[2]);

        image.im_info[0] = image.ori_img_info[0] * image.im_info[2];
        image.im_info[1] = image.ori_img_info[1] * image.im_info[2];

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int)image.im_info[1], (int)image.im_info[0], true);

        // Copy bitmap pixels to buffer
        ByteBuffer argb_buf = ByteBuffer.allocate(scaledBitmap.getByteCount());
        scaledBitmap.copyPixelsToBuffer(argb_buf);

        image.channels = 4;
        //Log.i(TAG,"Original Image Size: " + image.ori_img_info[1] + "x" + image.ori_img_info[0]);
        //Log.i(TAG, "readImage: image CxWxH(im_info)  : " + image.channels + "x" + image.im_info[1] + "x" + image.im_info[0]);
        Log.i(TAG, "readImage: image CxWxH(scaled img): " + image.channels + "x" + scaledBitmap.getWidth() + "x" + scaledBitmap.getHeight());
        // Get the underlying array containing the ARGB pixels
        image.pixels = argb_buf.array();
        Log.d(TAG, "readImage: bitmap(0,0)="
                + Integer.toHexString(bitmap.getPixel(0, 0))
                + ", rgba[0,0]="
                + Integer.toHexString((image.pixels[0] << 24 & 0xff000000) | (image.pixels[1] << 16 & 0xff0000)
                | (image.pixels[2] << 8 & 0xff00) | (image.pixels[3] & 0xff) ));
        return image;
    }

    private CaffeImage readImageSSD(String fileName){
        //Log.i(TAG, "readImage: reading: " + fileName);
        // Read image file to bitmap (in ARGB format)
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inPremultiplied = false;
        Bitmap bitmap = BitmapFactory.decodeFile(fileName, options);

        // Scale image to 300 x 300
        CaffeImage image = new CaffeImage();

        image.ori_img_info[0] = bitmap.getHeight();
        image.ori_img_info[1] = bitmap.getWidth();

        Log.i(TAG,"imgInfo[0]: "+ image.ori_img_info[0] + "   imgInfo[1]: "+ image.ori_img_info[1]);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);

        // Copy bitmap pixels to buffer
        ByteBuffer argb_buf = ByteBuffer.allocate(scaledBitmap.getByteCount());
        scaledBitmap.copyPixelsToBuffer(argb_buf);

        image.channels = 4;

        Log.i(TAG, "readImage: image CxWxH(scaled img): " + image.channels + "x" + scaledBitmap.getWidth() + "x" + scaledBitmap.getHeight());
        // Get the underlying array containing the ARGB pixels
        image.pixels = argb_buf.array();
        Log.d(TAG, "readImage: bitmap(0,0)="
                + Integer.toHexString(bitmap.getPixel(0, 0))
                + ", rgba[0,0]="
                + Integer.toHexString((image.pixels[0] << 24 & 0xff000000) | (image.pixels[1] << 16 & 0xff0000)
                | (image.pixels[2] << 8 & 0xff00) | (image.pixels[3] & 0xff) ));
        return image;
    }

    public native boolean loadModel(String modelPath, String weightPath);

    private native float[][] fasterRCNN(byte[] bitmap, int channels, float[]mean, float[] im_info, int[] ori_img_info);

    private native float[][] ssd(byte[] bitmap, float[]mean, int[] ori_img_info);

    private native float[][] mnssd(byte[] bitmap, float[]mean, int[] ori_img_info);
    
}
