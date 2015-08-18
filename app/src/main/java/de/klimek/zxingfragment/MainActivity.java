package de.klimek.zxingfragment;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.widget.Toast;
import de.klimek.zxingfragment.Decoder.OnDecodedCallback;

public class MainActivity extends Activity implements OnDecodedCallback {

	private ZxingFragment mZxingFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FragmentManager fragmentManager = getFragmentManager();
		mZxingFragment = (ZxingFragment) fragmentManager.findFragmentById(R.id.zxing_fragment);
		mZxingFragment.setOnDecodedCallback(this);
	}

	@Override
	protected void onResume() {
		mZxingFragment.startScanning();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		mZxingFragment.stopScanning();
		super.onPause();
	}
	
	@Override
	public void onDecoded(String decodedData) {
		Toast.makeText(this, decodedData, Toast.LENGTH_SHORT).show();
	}

}
