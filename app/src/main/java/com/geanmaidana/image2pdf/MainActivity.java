package com.geanmaidana.image2pdf;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.Manifest;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGES_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int MAX_IMAGES = 10;
    private ArrayList<Uri> imageUris = new ArrayList<>();

    private ImageAdapter adapter;
    private PDFExport pdfExport;

    InterstitialAd mInterstitialAd;
    InterstitialAdLoadCallback adLoadCallback;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        // Banner ad
        AdView mAdViewRecBanner = findViewById(R.id.adViewRecBanner);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdViewRecBanner.loadAd(adRequest);

        AdView mAdViewLargeBanner = findViewById(R.id.adViewLargeBanner);
        mAdViewLargeBanner.loadAd(adRequest);


        // Interstitial ad
        // Test adUnitID: "ca-app-pub-3940256099942544/1033173712"
        // Real ID "ca-app-pub-xxxxxxxxxxxxxxxxxxxxxxxxxx"
        adLoadCallback = new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Load a new ad when the current ad is closed
                        InterstitialAd.load(MainActivity.this, "ca-app-pub-3940256099942544/1033173712", adRequest, adLoadCallback);
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitialAd = null;
            }
        };

        // Now you can use adLoadCallback here
        InterstitialAd.load(MainActivity.this, "ca-app-pub-3940256099942544/1033173712", adRequest, adLoadCallback);


        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ImageAdapter(this, imageUris);
        recyclerView.setAdapter(adapter);




        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }





        Button btnUpload = findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TextView userIntroMsg = findViewById(R.id.userIntroMsg);
                userIntroMsg.setVisibility(View.GONE);
                mAdViewRecBanner.setVisibility(View.GONE);
                mAdViewLargeBanner.setVisibility(View.GONE);
                uploadImages();
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(MainActivity.this);
                } else {
                    Log.d("TAG", "The interstitial ad wasn't ready yet.");
                }

            }
        });

        Button btnExport = findViewById(R.id.btnExport);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        pdfExport = new PDFExport(MainActivity.this, imageUris, progressBar);
        btnExport.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {

                if (mInterstitialAd != null) {
                    mInterstitialAd.show(MainActivity.this);
                } else {
                    Log.d("TAG", "The interstitial ad wasn't ready yet.");
                }
                if(imageUris.isEmpty())
                {
                    Toast.makeText(MainActivity.this, "Please upload at least one image before exporting", Toast.LENGTH_SHORT).show();
                    return;
                }
                pdfExport.exportToPDF();
            }
        });

        ImageReorderCallback callback = new ImageReorderCallback(imageUris, adapter, recyclerView);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void uploadImages() {
        if (imageUris.size() >= MAX_IMAGES) {
            Toast.makeText(this, "You can only select up to " + MAX_IMAGES + " images", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            int count = data.getClipData() != null ? data.getClipData().getItemCount() : 1;
            int newTotal = imageUris.size() + count;

            if (newTotal > MAX_IMAGES) {
                Toast.makeText(this, "You can only select up to " + MAX_IMAGES + " images", Toast.LENGTH_SHORT).show();
            } else {
                if (data.getClipData() != null) {
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        imageUris.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    Uri imageUri = data.getData();
                    imageUris.add(imageUri);
                }

                adapter.notifyDataSetChanged();
            }

        }
    }

    // Reads EXIF data so images taken on a camera aren't orientated side
    // ways in the PDF
    Bitmap correctOrientation(Bitmap bitmap, Uri imageUri) throws IOException {
        ExifInterface exif = new ExifInterface(getContentResolver().openInputStream((imageUri)));
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270);
                break;
            default:
                return bitmap;
        }

        try {
            Bitmap orientatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return orientatedBitmap;
        }
        catch(OutOfMemoryError e){
            e.printStackTrace();
            return bitmap;
        }

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. The app needs storage access to export to PDF.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
