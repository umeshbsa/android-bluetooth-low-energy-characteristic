package com.app.bluetoothlowengery.activity;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.app.bluetoothlowengery.R;
import com.app.bluetoothlowengery.utils.BLEService;
import com.app.bluetoothlowengery.utils.BLEUUIDConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BLEControlActivity extends AppCompatActivity {

    private final static String TAG = BLEControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private BLEService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private TextView mDeviceAddressTv;
    private TextView mConnectionState;
    private TextView mDataField;
    private ExpandableListView mGattServicesList;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.

        mDeviceAddressTv = ((TextView) findViewById(R.id.device_address));
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mGattServicesList = (ExpandableListView) findViewById(R.id.elistview_all_service_characteristic);

        mDeviceAddressTv.setText(mDeviceAddress);

        // Start bind service
        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        findViewById(R.id.btn_read_characteristic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothLeService.readCharacteristic("e54eaa57-371b-476c-99a3-74d267e3edae");
            }
        });

        findViewById(R.id.btn_write_characteristic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothLeService.writeConnectionForProtection("e54eaa55-371b-476c-99a3-74d267e3edae");
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;

        String unknownServiceString = "Unknown service";

        String unknownCharaString = "Unknown characteristic";

        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();

        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            String serviceNameFromUUID = BLEUUIDConstant.lookup(uuid, unknownServiceString);

            //  Log.i("DDDDDDDDD", "SERVICE Name : " + serviceNameFromUUID);
            //  Log.i("DDDDDDDDD", "SERVICE UUID : " + uuid);
            currentServiceData.put(LIST_NAME, serviceNameFromUUID);
            currentServiceData.put(LIST_UUID, uuid);

            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();

            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);

                charas.add(gattCharacteristic);

                HashMap<String, String> currentCharaData = new HashMap<String, String>();

                uuid = gattCharacteristic.getUuid().toString();

                String charNameFromUUID = BLEUUIDConstant.lookup(uuid, unknownCharaString);

                //   Log.i("DDDDDDDDD", "Char Name : " + charNameFromUUID);
                // Log.i("DDDDDDDDD", "Char UUID : " + uuid);

                currentCharaData.put(LIST_NAME, charNameFromUUID);
                currentCharaData.put(LIST_UUID, uuid);

                gattCharacteristicGroupData.add(currentCharaData);

                List<BluetoothGattDescriptor> descs = gattCharacteristic.getDescriptors();
                for (BluetoothGattDescriptor desc : descs) {
                    // Log.i("DDDDDDDDD", "Desc Name : " + charNameFromUUID);
                    //Log.i("DDDDDDDDD", "Desc UUID : " + uuid);
                }
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_BLE_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_BLE_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_BLE_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_BLE_DATA_AVAILABLE);
        return intentFilter;
    }


    private void clearUI() {
        mDataField.setText("No data");
        Toast.makeText(this, "Service disconnected", Toast.LENGTH_LONG).show();
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BLEService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_BLE_CONNECTED: connected to a GATT server.
    // ACTION_BLE_DISCONNECTED: disconnected from a GATT server.
    // ACTION_BLE_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_BLE_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEService.ACTION_BLE_CONNECTED.equals(action)) {

                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();

            } else if (BLEService.ACTION_BLE_DISCONNECTED.equals(action)) {

                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();

            } else if (BLEService.ACTION_BLE_SERVICES_DISCOVERED.equals(action)) {

                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());

            } else if (BLEService.ACTION_BLE_DATA_AVAILABLE.equals(action)) {

                displayData(intent.getStringExtra(BLEService.EXTRA_BLE_DATA));

            }
        }
    };

}
