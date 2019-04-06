package de.klimek.scanner.sample;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import de.klimek.scanner.OnDecodedCallback;
import de.klimek.scanner.ScannerView;
import de.klimek.zxingfragment.R;

public class MainActivity extends Activity implements OnDecodedCallback {
    private static final int CAMERA_PERMISSION_REQUEST = 0xabc;
    private ScannerView mScanner;
    private boolean mPermissionGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScanner = findViewById(R.id.scanner);
        mScanner.setOnDecodedCallback(this);

        // get permission
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        mPermissionGranted = cameraPermission == PackageManager.PERMISSION_GRANTED;
        if (!mPermissionGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults.length > 0) {
            mPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPermissionGranted) {
            mScanner.startScanning();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScanner.stopScanning();
    }

    @Override
    public void onDecoded(String decodedData) {
        Toast.makeText(this, decodedData, Toast.LENGTH_SHORT).show();
    }
}
