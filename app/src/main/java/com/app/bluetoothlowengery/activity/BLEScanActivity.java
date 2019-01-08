package com.app.bluetoothlowengery.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1000;


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

        if (checkLocationPermission()) {
            scanLeDevice(!isScanning);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_ENABLE_BT) {
                scanLeDevice(!isScanning);
            }
        }
    }


    /**
     * All device scan list and set into ble adapter to show with list view.
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


    private void scanLeDevice(final boolean enable) {

        if (mBluetoothAdapter.isEnabled()) {
            if (enable) {
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        Toast.makeText(BLEScanActivity.this, "Scan all devices in 2 sec.", Toast.LENGTH_LONG).show();
                    }
                }, 2000);

                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        } else {
            Toast.makeText(BLEScanActivity.this, "Bluetooth is not enable", Toast.LENGTH_LONG).show();
        }
    }


    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(BLEScanActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        scanLeDevice(!isScanning);
                    }

                } else {
                    Toast.makeText(BLEScanActivity.this, getString(R.string.text_location_permission), Toast.LENGTH_LONG).show();
                }
                return;
            }

        }
    }
}
