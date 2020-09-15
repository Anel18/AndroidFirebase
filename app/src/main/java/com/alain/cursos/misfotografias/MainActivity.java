package com.alain.cursos.misfotografias;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/* *
 * Inventario
 * Created by Alain Nicolás Tello on 07/09/2018 at 05:34pm
 * All rights reserved 2018.
 * Course Specialize in Firebase for Android 2018 with MVP
 * More info: https://www.udemy.com/especialidad-en-firebase-para-android-con-mvp-profesional/
 */

public class MainActivity extends AppCompatActivity {
    //variables  y constantes para un buen flujo y buenas practicas
    private static final int RC_GALLERY=21;
    private static final int RC_CAMERA=22;

    private static final int RP_CAMERA=121;
    private static final int RP_STORAGE=122;

    private  static final String IMAGE_DIRECTORY="/MyPhotoApp";
    private static final String MY_PHOTO="my_photo";

    private static final String PATH_PROFILE="profile";
    private static final String PATH_PHOTO_URL="photoUrl";

    @BindView(R.id.imgPhoto)
    AppCompatImageView imgPhoto;
    @BindView(R.id.btnDelete)
    ImageButton btnDelete;
    @BindView(R.id.container)
    ConstraintLayout container;
    private TextView mTextMessage;
    private StorageReference mStorageReference;
    private DatabaseReference mDatabaseReference;

    //donde almmacenara la ruta que esta tomando la camera
    private String mCurrentPhotoPath;
    //variable que contiene la ruta de la imagen en la galeria independiente
    private Uri mPhotoSelectedUrl;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_gallery:
                    mTextMessage.setText(R.string.main_label_gallery);
                    fromGallery();
                    return true;
                case R.id.navigation_camera:
                    mTextMessage.setText(R.string.main_label_camera);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mTextMessage = findViewById(R.id.message);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        configFirebase();
    }

    private void configFirebase() {
        mStorageReference= FirebaseStorage.getInstance().getReference();
        FirebaseDatabase database=FirebaseDatabase.getInstance();
        mDatabaseReference= database.getReference().child(PATH_PROFILE).child(PATH_PHOTO_URL);
    }


    private void fromGallery() {
        Intent intent=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent,RC_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==RESULT_OK){
            switch (requestCode){
                case RC_GALLERY:
                    if (data!=null){
                        mPhotoSelectedUrl=data.getData();

                        try {
                            Bitmap bitmap=MediaStore.Images.Media.getBitmap(this.getContentResolver(),mPhotoSelectedUrl);
                            imgPhoto.setImageBitmap(bitmap);
                            btnDelete.setVisibility(View.GONE);
                            mTextMessage.setText(R.string.main_message_question_upload);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case RC_CAMERA:
                    break;
            }
        }
    }

    @OnClick(R.id.btnUpload)
    public void onViewClicked() {
        StorageReference profileReference=mStorageReference.child(PATH_PROFILE);

        StorageReference photoReference=profileReference.child(MY_PHOTO);
        photoReference.putFile(mPhotoSelectedUrl).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Snackbar.make(container, R.string.main_message_upload_success,Snackbar.LENGTH_LONG).show();
              //  Uri downloadUri=taskSnapshot.getDownloadUri();
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        savePhotoUrl(uri);
                        btnDelete.setVisibility(View.VISIBLE);
                        mTextMessage.setText(R.string.main_message_done);
                    }
                });


            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(container,R.string.main_message_upload_error,Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void savePhotoUrl(Uri downloadUri) {
        mDatabaseReference.setValue(downloadUri.toString());
    }
}
