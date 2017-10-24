package com.example.jazz.realtimeobjectdetection.Utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.List;

/**
 * Created by jazz on 24/10/17.
 */

public class BoundingBoxView extends View {

    private static final String TAG = "BoundingBoxView";
    List<PredictionResult> results;

    private final String[] CLASSES = {"__background__",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};

    private final int[] COLOR= {0xFF0080FF, 0xFF00FF80, 0xFFFF0040, 0xFFF9F906, 0xFF00FFBF, 0xFFD24DFF};
    
    public BoundingBoxView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void updateResult(List<PredictionResult> results){
        this.results = results;
        postInvalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        if (results != null) {
            int num = 0;

            float view_height_temp = (float) canvas.getHeight();
            float view_width_temp = (float) canvas.getWidth();
            float view_height = Math.min(view_height_temp, view_width_temp);
            float view_width = Math.max(view_height_temp, view_width_temp);

            String prediction_string = "width: " + Float.toString(view_width) +
                    " height: " + Float.toString(view_height);
            Log.v("BoundingBox", prediction_string);


            Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(9);

            Paint txtPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            txtPaint.setStyle(Paint.Style.STROKE);
            txtPaint.setStrokeWidth(4);
            txtPaint.setTextSize(50);

            for (PredictionResult item : results) {
                StringBuilder sb = new StringBuilder();
                sb.append(item.getLabel())
                        .append("  ").append(String.format("%.2f", item.getConfidence()));
                Log.d(TAG, sb.toString());

                boxPaint.setColor(COLOR[num]);
                txtPaint.setColor(COLOR[num]);

                Log.d(TAG,"left: " + item.getLeft());
                Log.d(TAG,"top: " + item.getTop());
                Log.d(TAG,"right: " + item.getRight());
                Log.d(TAG,"bot: " + item.getBot());

                canvas.drawRect((1 - item.getBot())*view_height, item.getLeft()*view_width, (1-item.getTop())*view_height, item.getRight()*view_width, boxPaint);
                canvas.save();
                canvas.rotate(90, (1 - item.getBot())*view_height, item.getLeft()*view_width);
                canvas.drawText(sb.toString(), (1 - item.getBot())*view_height, item.getLeft()*view_width, txtPaint);
                canvas.restore();

                num++;
            }
        }
    }
}
