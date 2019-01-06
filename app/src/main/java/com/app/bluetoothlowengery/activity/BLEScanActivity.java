package com.app.bluetoothlowengery.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.app.bluetoothlowengery.R;
import com.app.bluetoothlowengery.adapter.BLEScanAdapter;

/**
 * Scan of all BLE devices
 */
public class BLEScanActivity extends AppCompatActivity {

    Handler mHandler = new Handler();
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BLEScanAdapter mBLEAdapter;
    private ListView mScanListView;
    private boolean isScanning;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_scan);
        checkBLEOnDevice();
        initBLEAdapter();
    }

    /**
     * Chaeck device is support of bluetooth or not
     */
    private void checkBLEOnDevice() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private void initBLEAdapter() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mScanListView = (ListView) findViewById(R.id.list_view_scan_device);

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        mBLEAdapter = new BLEScanAdapter(this);
        mScanListView.setAdapter(mBLEAdapter);

        mScanListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mBLEAdapter.getDevice(position);
                if (isScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    isScanning = false;
                }
                if (device == null) {
                    Toast.makeText(BLEScanActivity.this, "Device can not be null", Toast.LENGTH_LONG).show();
                } else {
                    final Intent intent = new Intent(BLEScanActivity.this, BLEControlActivity.class);
                    intent.putExtra(BLEControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                    intent.putExtra(BLEControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                    startActivity(intent);
                }
            }
        });

        scanBLEDevices();
    }

    private void scanBLEDevices() {
        isScanning = true;
        // Stops scanning after 2 second
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                Toast.makeText(BLEScanActivity.this, "Scan Stop in 2 sec.", Toast.LENGTH_LONG).show();
            }
        }, 2000);

        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    /**
     * All device scan list and set inot ble adapter to show with list view.
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBLEAdapter.addDevice(device);
                    mBLEAdapter.notifyDataSetChanged();
                }
            });
        }
    };
}
