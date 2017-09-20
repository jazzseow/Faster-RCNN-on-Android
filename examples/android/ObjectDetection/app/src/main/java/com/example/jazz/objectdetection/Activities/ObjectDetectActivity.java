package com.example.jazz.objectdetection.Activities;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jazz.objectdetection.Utils.CaffeInterface;
import com.example.jazz.objectdetection.Utils.PredictionResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ObjectDetectActivity extends AppCompatActivity {
    private final static String TAG = "ObjectDetectActivity";
    private ImageView imageView;
    private ListView listView;
    private TextView textView;
    private CaffeInterface caffeInterface;
    private double timeTaken;
    private String imgPath;
    private int model;

    private final int[] COLOR= {0xFF0080FF, 0xFF00FF80, 0xFFFF0040, 0xFFF9F906, 0xFF00FFBF, 0xFFD24DFF};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_object_detect);
        imageView = (ImageView) findViewById(R.id.detect_image);
        listView = (ListView) findViewById(R.id.listview_result);
        textView = (TextView) findViewById(R.id.text_time);

        imgPath = getIntent().getExtras().getString("Path");
        model = getIntent().getIntExtra("Model", 0);

        new LoadModel().execute(model);

        if (!new File(imgPath).exists()) {
            Toast.makeText(this, "No file path", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
        DetectTask task = new DetectTask();
        task.execute(imgPath);
    }

    private class LoadModel extends AsyncTask<Integer,Void, Void>{
        @Override
        protected Void doInBackground(Integer... integers) {
            final int model = integers[0];
            caffeInterface = new CaffeInterface();
            if (model == 0){
                File modelFile = new File(Environment.getExternalStorageDirectory(), "ObjectDetection/models/net1.protobin");
                File weightFile = new File(Environment.getExternalStorageDirectory(), "ObjectDetection/models/weight1.caffemodel");
                Log.d(TAG, "onCreate: modelFile:" + modelFile.getPath());
                Log.d(TAG, "onCreate: weightFIle:" + weightFile.getPath());

                if (!caffeInterface.loadModel(modelFile.getPath(), weightFile.getPath())){
                    Log.d(TAG, "Cannot load model");
                    return null;
                }
            }
            else if (model == 1){
                File modelFile = new File(Environment.getExternalStorageDirectory(), "ObjectDetection/models/net2.protobin");
                File weightFile = new File(Environment.getExternalStorageDirectory(), "ObjectDetection/models/weight2.caffemodel");
                Log.d(TAG, "onCreate: modelFile:" + modelFile.getPath());
                Log.d(TAG, "onCreate: weightFIle:" + weightFile.getPath());

                if (!caffeInterface.loadModel(modelFile.getPath(), weightFile.getPath())){
                    Log.d(TAG, "Cannot load model");
                    return null;
                }
            }
            return null;
        }
    }

    // ==========================================================
    // Tasks inner class
    // ==========================================================
    private class DetectTask extends AsyncTask<String, Void, List<PredictionResult>> {
        private ProgressDialog mmDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (model == 0){
                mmDialog = ProgressDialog.show(ObjectDetectActivity.this, getString(R.string.dialog_wait), "Testing with Faster RCNN", true);
            }
            else if (model == 1){
                mmDialog = ProgressDialog.show(ObjectDetectActivity.this, getString(R.string.dialog_wait), "Testing with SSD", true);
            }
        }

        @Override
        protected List<PredictionResult> doInBackground(String... strings) {
            final String filePath = strings[0];
            long startTime;
            long endTime;
            Log.d(TAG, "DetectTask filePath:" + filePath);

            float[] mean = {102.9801f, 115.9465f, 122.7717f};

            startTime = System.currentTimeMillis();
            Log.d(TAG, "Start objDetect");
            List<PredictionResult> rets = caffeInterface.detectImage(filePath, mean, model);
            Log.d(TAG, "end objDetect");
            endTime = System.currentTimeMillis();
            timeTaken = (double) (endTime - startTime) / 1000;

            return rets;

        }

        @Override
        protected void onPostExecute(List<PredictionResult> rets) {
            super.onPostExecute(rets);
            if (mmDialog != null) {
                mmDialog.dismiss();
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(imgPath, options);

            imageView.setImageBitmap(bitmap);

            final List<String> lists = new ArrayList<String>();
            if (rets != null) {
                int num = 0;
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(mutableBitmap);

                for (PredictionResult item : rets) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(item.getLabel())
                            .append(", Prob: ").append(item.getConfidence())
                            .append("\n[")
                            .append(item.getLeft()).append(", ")
                            .append(item.getTop()).append(", ")
                            .append(item.getRight()).append(", ")
                            .append(item.getBot())
                            .append(']');
                    Log.d(TAG, sb.toString());
                    lists.add(sb.toString());

                    paint.setColor(COLOR[num]);
                    num++;
                    canvas.drawRect(item.getLeft(), item.getTop(), item.getRight(), item.getBot(), paint);
                }

                imageView.setImageBitmap(mutableBitmap);

            }else {
                imageView.setImageBitmap(bitmap);
                lists.add("No object Recognised");
            }

            textView.setText("Time taken: " + timeTaken + "s");
            ArrayAdapter<String> mResultAdapter = new ArrayAdapter<String>(ObjectDetectActivity.this, R.layout.list_item, R.id.item, lists){

                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

                    LayoutInflater inflater = getLayoutInflater();
                    View row = convertView;

                    row = inflater.inflate(R.layout.list_item, parent, false);
                    TextView textView = (TextView) row.findViewById(R.id.item);

                    textView.setText(lists.get(position));

                    row.setBackgroundColor(COLOR[position]);
                    return row;
                };
            };

            listView.setAdapter(mResultAdapter);
        }
    }
}
