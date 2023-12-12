package com.geanmaidana.image2pdf;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class PDFExport {
    private MainActivity mainActivity;
    private ArrayList<Uri> imageUris;
    private ProgressBar progressBar;

    public PDFExport(MainActivity mainActivity, ArrayList<Uri> imageUris, ProgressBar
                     progressBar) {
        this.mainActivity = mainActivity;
        this.imageUris = imageUris;
        this.progressBar = progressBar;
    }

    public void exportToPDF() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                PdfDocument pdfDocument = new PdfDocument();

                for(int i = 0; i < imageUris.size(); i++) {
                    Uri imageUri = imageUris.get(i);
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(mainActivity.getContentResolver(), imageUri);
                        bitmap = mainActivity.correctOrientation(bitmap, imageUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mainActivity, "Error reading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }

                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), i + 1).create();
                    PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                    Canvas canvas = page.getCanvas();
                    Paint paint = new Paint();
                    canvas.drawPaint(paint);

                    paint.setColor(Color.WHITE);
                    canvas.drawBitmap(bitmap, 0, 0, null);

                    pdfDocument.finishPage(page);

                    // Recycle the bitmap to free up memory
                    bitmap.recycle();
                }

                String pdfName = "Image2PDF.pdf";
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, pdfName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = mainActivity.getContentResolver().insert(MediaStore.Files.getContentUri("external"), contentValues);
                try (OutputStream outputStream = mainActivity.getContentResolver().openOutputStream(uri)) {
                    pdfDocument.writeTo(outputStream);
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mainActivity, "PDF saved to " + uri.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mainActivity, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }

                pdfDocument.close();
            }
        }).start();
    }
}


