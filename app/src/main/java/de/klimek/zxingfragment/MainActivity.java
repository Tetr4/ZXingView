package de.klimek.zxingfragment;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import de.klimek.zxingfragment.Decoder.OnDecodedCallback;

public class MainActivity extends Activity implements OnDecodedCallback {

    private ZxingFragment mZxingFragment;
    private static final int CAMERA_PERMISSION_REQUEST = 0xabc;
    private boolean mPermissionGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getFragmentManager();
        mZxingFragment = (ZxingFragment) fragmentManager.findFragmentById(R.id.zxing_fragment);
        mZxingFragment.setOnDecodedCallback(this);

        // get permission
        mPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (!mPermissionGranted) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            mPermissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPermissionGranted) {
            mZxingFragment.startScanning();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mZxingFragment.stopScanning();
    }

    @Override
    public void onDecoded(String decodedData) {
        Toast.makeText(this, decodedData, Toast.LENGTH_SHORT).show();
    }

}
