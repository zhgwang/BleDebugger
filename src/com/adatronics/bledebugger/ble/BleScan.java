package com.adatronics.bledebugger.ble;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.adatronics.bledebugger.R;
import com.adatronics.bledebugger.RefreshableView;
import com.adatronics.bledebugger.R.id;
import com.adatronics.bledebugger.R.layout;
import com.adatronics.bledebugger.RefreshableView.PullToRefreshListener;
import com.adatronics.bledebugger.model.BleDevice;
import com.adatronics.bledebugger.model.BleDeviceLab;


/**
 * @author BojunPan@adatronics
 * 
 *         2014-4-4
 */
public class BleScan extends Activity {
	
	private static final String TAG = "BleScan";
	private BluetoothAdapter mBluetoothAdapter;
	RefreshableView refreshableView;
	
	private boolean mScanning;
	
	private Handler mHandler;
	
	private ArrayList<BleDevice> deviceList;
	private ListView deviceListView;
	private DeviceAdapter deviceAdapter;
	
	private static final int REQUEST_ENABLE_BT = 3;
	private TextView scan_resutls;

	private AsyncTask<String, Integer, Boolean> scanTask;
	
	
	private class DeviceAdapter extends ArrayAdapter<BleDevice> {

		public DeviceAdapter(ArrayList<BleDevice> deviceList) {
			super(getApplicationContext(), 0, deviceList);
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(com.adatronics.bledebugger.R.layout.list_item_scan, null);
			}
			
			BleDevice device = getItem(position);
			
			TextView nameView = (TextView) convertView.findViewById(R.id.device_name);
			nameView.setText(device.getName());
			
			TextView addressView = (TextView) convertView.findViewById(R.id.device_address);
			addressView.setText(device.getAddress());
			
			TextView rssiView = (TextView) convertView.findViewById(R.id.device_rssi);
			rssiView.setText("Rssi: " + device.getRssi());
			
			return convertView;
		}
		
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.blescan);
		
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "The device doesn't support BLE",
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		scan_resutls = (TextView) findViewById(R.id.ble_scan_results);

		BluetoothManager blueManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBluetoothAdapter = blueManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "No Bluetooth device.", Toast.LENGTH_LONG)
					.show();
			finish();
			return;
		}
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		refreshableView = (RefreshableView) findViewById(R.id.refreshable_view);

		deviceListView = (ListView) findViewById(R.id.list_devices);
		
		deviceList = BleDeviceLab.getInstance(getApplicationContext()).getDeviceList();
		deviceAdapter = new DeviceAdapter(deviceList);
		
		deviceListView.setAdapter(deviceAdapter);

		deviceListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				BleDeviceLab.getInstance(getApplicationContext()).setCurrentDevice(position);
				Intent intent = new Intent(getApplicationContext(), BleService.class);
				startActivity(intent);
			}
		});
		
		refreshableView.setOnRefreshListener(new PullToRefreshListener() {
			@Override
			public void onRefresh() {
				deviceScan();
				refreshableView.finishRefreshing();
			}
		}, 0);

	}

	@Override
	public void onResume() {
		super.onResume();
		deviceScan();
		Log.i(TAG, "onResume");
	}

	@Override
	public void onPause() {
		super.onPause();
		if (scanTask != null) {
			scanTask.cancel(true);
		}
		Log.i(TAG, "onPause");
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		Log.i(TAG, "onStop");
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			
			BleDeviceLab mLab = BleDeviceLab.getInstance(getApplicationContext());
			
			if (device.getName() != null) {
				mLab.addDevice(new BleDevice(device, rssi));
			}
						
			final int i = mLab.getSize();
			
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					deviceList = BleDeviceLab.getInstance(getApplicationContext()).getDeviceList();
					deviceAdapter = new DeviceAdapter(deviceList);
					scan_resutls.setText("Found " + i + " Devices");
					deviceListView.setAdapter(deviceAdapter);
				}
			});
		}
	};

	private void deviceScan() {
		try {
			mBluetoothAdapter.startLeScan(mLeScanCallback);
			Thread.sleep(1000);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		mBluetoothAdapter.stopLeScan(mLeScanCallback);
	}
}
