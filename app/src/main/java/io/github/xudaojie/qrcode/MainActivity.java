package io.github.xudaojie.qrcode;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.HashMap;

import io.github.xudaojie.qrcode.common.ActionUtils;
import io.github.xudaojie.qrcode.common.QrUtils;

import static io.github.xudaojie.qrcode.common.QrUtils.getYUV420sp;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_QR_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button qrCodeBtn = (Button) findViewById(R.id.qrcode_btn);
        Button qrCodeReadBtn = (Button) findViewById(R.id.qrcode_read_btn);
        qrCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, CaptureActivity.class);
                MainActivity.this.startActivityForResult(i, REQUEST_QR_CODE);
            }
        });
        qrCodeReadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActionUtils.startActivityForGallery(MainActivity.this);
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK
                && requestCode == REQUEST_QR_CODE
                && data != null) {
            String result = data.getStringExtra("result");
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
        } else if (resultCode == RESULT_OK
                && requestCode == ActionUtils.PHOTO_REQUEST_GALLERY
                && data != null) {
            Uri inputUri = data.getData();

            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(inputUri, proj, null, null, null);
            if (cursor.moveToFirst()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                decode(path);
            }
        }
    }

    private void decode(final String path) {
        new Thread() {
            @Override
            public void run() {
                super.run();

                Bitmap bitmap = QrUtils.decodeSampledBitmapFromFile(path, 256, 256);

//                Bitmap bitmap = getDecodeAbleBitmap(R.drawable.qrcode_zh);
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] pixels = new int[width * height];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
//                RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                PlanarYUVLuminanceSource source1 = new PlanarYUVLuminanceSource(getYUV420sp(width, height, bitmap), width, height, 0, 0, width, height, false);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source1));
//                BinaryBitmap binaryBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source1));
                HashMap<DecodeHintType, Object> hints = new HashMap<>();

                hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");

                try {
                    Result result = new MultiFormatReader().decode(binaryBitmap, hints);
//                    Result result = new QRCodeReader().decode(binaryBitmap, hints);
                    Log.d("MainActivity", result.getText());
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

}
