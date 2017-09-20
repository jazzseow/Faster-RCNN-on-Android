package com.example.jazz.objectdetection.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.jazz.objectdetection.Utils.ImageFilePath;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private ImageView mImageView;
    private static final int REQUEST_IMAGE_CAPTURE = 0, SELECT_FILE = 1;
    private Uri imageUri, camUri = null, galUri = null;


    static {
        System.loadLibrary("caffe-jni");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.image);
    }


    public void openDialog(View view) {
        final CharSequence[] items = {"Take Photo", "Choose from Library",
                "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    cameraIntent(dialog);
                } else if (items[item].equals("Choose from Library")) {
                    galleryIntent(dialog);
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }


        });
        builder.show();
    }

    public void runNetwork(View view){
        int id = view.getId();

        if (id == R.id.faster_rcnn){
            if (galUri != null){
                Intent intent = new Intent(this, ObjectDetectActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("Path", ImageFilePath.getPath(this, galUri));
                bundle.putInt("Model", 0);
                intent.putExtras(bundle);
                startActivity(intent);
            }
            else if (camUri != null){
                Intent intent = new Intent(this, ObjectDetectActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("Path", getRealPathFromURI(camUri));
                bundle.putInt("Model", 0);
                intent.putExtras(bundle);
                startActivity(intent);
            }
            else{
                Toast.makeText(this,"Please select an Image", Toast.LENGTH_LONG).show();
            }
        }
        else if (id == R.id.ssd){
            if (galUri != null){
                Intent intent = new Intent(this, ObjectDetectActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("Path", ImageFilePath.getPath(this, galUri));
                bundle.putInt("Model", 1);
                intent.putExtras(bundle);
                startActivity(intent);
            }
            else if (camUri != null){
                Intent intent = new Intent(this, ObjectDetectActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("Path", getRealPathFromURI(camUri));
                bundle.putInt("Model", 1);
                intent.putExtras(bundle);
                startActivity(intent);
            }
            else{
                Toast.makeText(this,"Please select an Image", Toast.LENGTH_LONG).show();
            }
        }
    }


    private void cameraIntent(DialogInterface dialog) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        dialog.dismiss();
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, setImageUri());
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    private void galleryIntent(DialogInterface dialog) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        dialog.dismiss();
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                try {
                    onCameraResult(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onCameraResult(Intent data) throws IOException {
        Bitmap thumbnail = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        mImageView.setImageBitmap(thumbnail);

        // CALL THIS METHOD TO GET THE URI FROM THE BITMAP
        camUri = getImageUri(getApplicationContext(), thumbnail);


    }

    private void onSelectFromGalleryResult(Intent data) {
        Bitmap bm = null;
        galUri = data.getData();
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), galUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mImageView.setImageBitmap(bm);
    }

    public String getRealPathFromURI(Uri uri) {
        String result;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public Uri setImageUri() {
        File file = new File(Environment.getExternalStorageDirectory() + "/ObjectDetection/test_images", new Date().getTime() + ".jpg");
        this.imageUri = Uri.fromFile(file);
        return imageUri;
    }

}
