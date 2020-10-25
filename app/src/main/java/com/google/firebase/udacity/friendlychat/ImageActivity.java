package com.google.firebase.udacity.friendlychat;


import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.soundcloud.android.crop.Crop;

import java.io.File;

public class ImageActivity extends AppCompatActivity {

    private ImageButton btnCaptureImage;
    private Button btnDetectImage;
    private String chosen;
    private boolean quantised=true;
    // for permission requests
    public static final int REQUEST_PERMISSION = 300;
    // request code for permission requests to the os for image
    public static final int REQUEST_IMAGE = 100;
    // will hold uri of image obtained from camera
    private Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);
        bottomNavigationView.setSelectedItemId(R.id.image_activity);

        bottomNavigationView.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.chat_activity:
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                        return;

                    case R.id.developer_activity:
                        startActivity(new Intent(getApplicationContext(), DeveloperActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                        return;

                    case R.id.image_activity:
                }
            }
        });

        // request permission to use the camera on the user's phone
        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.CAMERA}, REQUEST_PERMISSION);
        }

        // request permission to write data (aka images) to the user's external storage of their phone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }

        // request permission to read data (aka images) from the user's external storage of their phone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }

        btnCaptureImage = findViewById(R.id.btn_capture);
        btnCaptureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chosen = "inception_quant.tflite";
                openCameraIntent();
            }
        });
    }
    private void openCameraIntent(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New_Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From_Camera");
        //tell camera where to store the resulting picture
        imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    // checks that the user has allowed all the required permission of read and write and camera. If not, notify the user
    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(getApplicationContext(),"This application needs read, write, and camera permissions to run. Application now closing.",Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // if the camera activity is finished, obtained the uri, crop it to make it square, and send it to 'Classify' activity
        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK) {
            try {
                Uri source_uri = imageUri;
                Uri dest_uri = Uri.fromFile(new File(getCacheDir(), "cropped"));
                // need to crop it to square image as CNN's always required square input
                Crop.of(source_uri, dest_uri).asSquare().start(ImageActivity.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // if cropping activity is finished, get the resulting cropped image uri and send it to 'Classify' activity
        if(requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK){
            imageUri = Crop.getOutput(data);
            Intent i = new Intent(ImageActivity.this, Classify.class);
            // put image data in extras to send
            i.putExtra("resID_uri", imageUri);
            // put filename in extras
            i.putExtra("chosen", chosen);
            // put model type in extras
            i.putExtra("quant", quantised);
            // send other required data
            startActivity(i);
        }

    }
}