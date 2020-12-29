package com.example.musicmanagement.filedemo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.musicmanagement.R;
import com.example.musicmanagement.util.Constants;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

public class FileDemoActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_IMAGE = 1;
    private static final int PERMISSION_READ_WRITE_EXTERNAL_STORAGE = 2;

    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;
    private StorageReference mStorageReferenceImages;
    private Uri mUri;
    private ImageView mImageView;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_demo);

        Toolbar toolbar = findViewById(R.id.toolbar);
        mImageView = findViewById(R.id.imageView);
        setSupportActionBar(toolbar);

        // Create an instance of Firebase Storage
        mFirebaseStorage = FirebaseStorage.getInstance();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_PICK_IMAGE) {
                String filePath = FileUtil.getPath(this, data.getData());
                mUri = Uri.fromFile(new File(filePath));
                uploadFile(mUri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_READ_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            }
        }
    }

    private void showProgressDialog(String title, String message) {
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.setMessage(message);
        else
            mProgressDialog = ProgressDialog.show(this, title, message, true, false);
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void showHorizontalProgressDialog(String title, String body) {

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.setTitle(title);
            mProgressDialog.setMessage(body);
        } else {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setTitle(title);
            mProgressDialog.setMessage(body);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setProgress(0);
            mProgressDialog.setMax(100);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }
    }

    public void updateProgress(int progress) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.setProgress(progress);
        }
    }

    /**
     * Step 1: Create a Storage
     *
     */
    public void onCreateReferenceClick(View view) {
        mStorageReference = mFirebaseStorage.getReferenceFromUrl(Constants.FIREBASE_STORAGE_URL);
        showToast("Reference Created Successfully.");
        findViewById(R.id.button_step_2).setEnabled(true);
    }

    /**
     * Step 2: Create a directory named "Images"
     *
     */
    public void onCreateDirectoryClick(View view) {
        mStorageReferenceImages = mStorageReference.child("images");
        showToast("Directory 'images' created Successfully.");
        findViewById(R.id.button_step_3).setEnabled(true);
    }

    /**
     * Step 3: Upload an Image File and display it on ImageView
     *
     */
    public void onUploadFileClick(View view) {
        if (ContextCompat.checkSelfPermission(FileDemoActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(FileDemoActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(FileDemoActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_READ_WRITE_EXTERNAL_STORAGE);
        else {
            pickImage();
        }
    }

    /**
     * Step 4: Download an Image File and display it on ImageView
     *
     */
    public void onDownloadFileClick(View view) {
        downloadFile(mUri);
    }

    /**
     * Step 5: Delete am Image File and remove Image from ImageView
     *
     */
    public void onDeleteFileClick(View view) {
        deleteFile(mUri);
    }

    // service firebase.storage {
//   match /b/fir-articles.appspot.com/o {
//     match /{allPaths=**} {
//       allow read, write: if request.auth != null;
//     }
//   }
// }

    private void showAlertDialog(Context ctx, String title, String body, DialogInterface.OnClickListener okListener) {

        if (okListener == null) {
            okListener = (dialog, which) -> dialog.cancel();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx).setMessage(body).setPositiveButton("OK", okListener).setCancelable(false);

        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }

        builder.show();
    }

    private void uploadFile(Uri uri) {
        mImageView.setImageResource(R.drawable.placeholder_image);

        StorageReference uploadStorageReference = mStorageReferenceImages.child(uri.getLastPathSegment());
        final UploadTask uploadTask = uploadStorageReference.putFile(uri);
        showHorizontalProgressDialog("Uploading", "Please wait...");
        uploadTask
                .addOnSuccessListener(taskSnapshot -> {
                    hideProgressDialog();
                    Uri downloadUrl = taskSnapshot.getUploadSessionUri();
                    Log.d("MainActivity", downloadUrl.toString());
                    showAlertDialog(FileDemoActivity.this, "Upload Complete", downloadUrl.toString(), (dialogInterface, i) -> {
                        findViewById(R.id.button_step_3).setEnabled(false);
                        findViewById(R.id.button_step_4).setEnabled(true);
                    });

                    Glide.with(FileDemoActivity.this)
                            .load(downloadUrl)
                            .into(mImageView);
                })
                .addOnFailureListener(exception -> {
                    exception.printStackTrace();
                    // Handle unsuccessful uploads
                    hideProgressDialog();
                })
                .addOnProgressListener(FileDemoActivity.this, taskSnapshot -> {
                    int progress = (int) (100 * (float) taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    Log.i("Progress", progress + "");
                    updateProgress(progress);
                });
    }

    private void downloadFile(Uri uri) {
        mImageView.setImageResource(R.drawable.placeholder_image);
        final StorageReference storageReferenceImage = mStorageReferenceImages.child(uri.getLastPathSegment());
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Firebase Storage");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MainActivity", "failed to create Firebase Storage directory");
            }
        }

        final File localFile = new File(mediaStorageDir, uri.getLastPathSegment());
        try {
            localFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        showHorizontalProgressDialog("Downloading", "Please wait...");
        storageReferenceImage.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
            hideProgressDialog();
            showAlertDialog(FileDemoActivity.this, "Download Complete", localFile.getAbsolutePath(), (dialogInterface, i) -> {
                findViewById(R.id.button_step_4).setEnabled(false);
                findViewById(R.id.button_step_5).setEnabled(true);
            });

            Glide.with(FileDemoActivity.this)
                    .load(localFile)
                    .into(mImageView);
        }).addOnFailureListener(exception -> {
            // Handle any errors
            hideProgressDialog();
            exception.printStackTrace();
        }).addOnProgressListener(taskSnapshot -> {
            int progress = (int) (100 * (float) taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
            Log.i("Progress", progress + "");
            updateProgress(progress);
        });
    }

    private void deleteFile(Uri uri) {
        showProgressDialog("Deleting", "Please wait...");
        StorageReference storageReferenceImage = mStorageReferenceImages.child(uri.getLastPathSegment());
        storageReferenceImage.delete().addOnSuccessListener(aVoid -> {
            hideProgressDialog();
            showAlertDialog(FileDemoActivity.this, "Success", "File deleted successfully.", (dialogInterface, i) -> {
                mImageView.setImageResource(R.drawable.placeholder_image);
                findViewById(R.id.button_step_3).setEnabled(true);
                findViewById(R.id.button_step_4).setEnabled(false);
                findViewById(R.id.button_step_5).setEnabled(false);
            });
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "Firebase Storage");
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("MainActivity", "failed to create Firebase Storage directory");
                }
            }
            deleteFiles(mediaStorageDir);
        }).addOnFailureListener(exception -> {
            hideProgressDialog();
            exception.printStackTrace();
        });
    }

    private void deleteFiles(File directory) {
        if (directory.isDirectory())
            for (File child : directory.listFiles())
                child.delete();
    }
}
