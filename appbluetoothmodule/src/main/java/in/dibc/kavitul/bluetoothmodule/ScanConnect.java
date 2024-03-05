package in.dibc.kavitul.bluetoothmodule;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

@SuppressLint("MissingPermission")
public class ScanConnect extends AppCompatActivity {

    protected static final int CONNECTED= 0;
    CountDownTimer mCountDownTimer;
    private ProgressDialog PP_Progress;
    public static AppBluetoothHelper btHelper;
    String TAG = "Bluetooth_ScanConnect";
    private BluetoothPermissions btPerm;
    public BluetoothDevices bluetoothDevices;
    public Thread bluetoothDevicesThread;
    public static ImageView searchDevicesFAB;
    private ListView deviceListView;
    private ImageView header_left_btn;
    public static ArrayList<BluetoothDevice> deviceArrayList;
    public static DeviceListArrayAdaptor deviceListAdapter;
    private AlertDialog alert;
    public static boolean connecting = false;
    private TextView connectedButton;
    Context context;
    BluetoothDevice device;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan_connect);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = this;
        cancelBluetoothConnection();
//        Intent intent = getIntent();
        checkLocation();
        initBluetoothScan();

        header_left_btn = findViewById(R.id.header_left_btn);
        header_left_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void checkLocation() {
        if (android.os.Build.VERSION.SDK_INT < 30) {
            return;
        }
        LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(context)
                    .setMessage("Scanning device requires location to be turned on.")
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .show();
        }
    }

    protected void initBluetoothScan() {
        btHelper = new AppBluetoothHelper(this);
        setHandler();
        btPerm = new BluetoothPermissions(btHelper);
        btHelper.res = getResources();
        btPerm.check();

        bluetoothDevices = new BluetoothDevices(btHelper);
        bluetoothDevicesThread = new Thread(bluetoothDevices);

//        btHelper.APP_UUID = btHelper.getAppUuid(this);


        if (!bluetoothDevicesThread.isAlive()) {
            try {
                bluetoothDevicesThread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        bluetoothDevices.getPairedDevices();
        bluetoothDevices.scanAvailableBluetoothDevices(deviceListView);

        searchDevicesFAB = findViewById(R.id.fab_scan_list);
        searchDevicesFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                deviceListView.setVisibility(View.VISIBLE);
//                connectedButton.setVisibility(View.GONE);
                btPerm.check();
                checkLocation();
                connecting = false;
                if (!bluetoothDevicesThread.isAlive()) {
                    try {
                        bluetoothDevicesThread.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                bluetoothDevices.getPairedDevices();
                bluetoothDevices.scanAvailableBluetoothDevices(view);
                Log.d(TAG, "onClick: search");

            }
        });

        deviceListView = findViewById(R.id.bluetoothDevicesList);
        deviceArrayList = new ArrayList<BluetoothDevice>();


        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {


                if(btHelper.bluetoothAdapter.isDiscovering()){
                    btHelper.bluetoothAdapter.cancelDiscovery();
                }
                if(connecting){
                    Snackbar.make(view, R.string.already_connecting, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    Log.d(TAG, "onItemClick: clicked item " + position);
                    device = deviceArrayList.get(position);
                    Snackbar.make(view, btHelper.res.getString(R.string.connecting, device.getName()), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    connecting = true;
//                    connectToDevice(device);
//                    progressbarinit();
                    btHelper.connectedDevice = device;
                    returnToSourceActivity();


                }
            }
        });

        deviceListAdapter = new DeviceListArrayAdaptor(this, R.layout.bluetooth_device_list_item, deviceArrayList);
        deviceListView.setAdapter(deviceListAdapter );
        connectedButton = findViewById(R.id.btn_deviceConnected);
        connectedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connecting = false;
                deviceListView.setVisibility(View.VISIBLE);
                if (btHelper.connectedDeviceName == "") {
                    Snackbar.make(view, btHelper.res.getString(R.string.selectDevice), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    returnToSourceActivity();
                }
            }
        });
    }


    //    Handler mHandler = new Handler() {
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case CONNECTED:
//                    try {
//                        Log.d(TAG, "connectedToDevice: "+btHelper.connectedDeviceName);
//                        connecting = false;
//                        try {
//
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//            }
//        }
//    };


    private void setHandler() {
        btHelper.mHandlersconnected = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                String ConnectedDevice = bundle.getString("ConnectedTodevice");

                if (ConnectedDevice == "Connected") {
                    returnToSourceActivity();
                }

            }

        };
    }
    public static void updateDeviceList(){
        ScanConnect.deviceListAdapter.clear();
        ScanConnect.deviceArrayList.clear();
        for(BluetoothDevice bt : BluetoothDevices.searchedDevices){
            deviceArrayList.add(bt);
            Log.d("DIBC_", "updateDeviceList: add "+bt.getName());
        }
        deviceListAdapter.notifyDataSetChanged();
    }



    public void connectToDevice(BluetoothDevice device){
        Log.d(TAG, "onItemClick: connecting to "+device.getName());
        //unregisterReceiver(btHelper.bluetoothReceiver);
        IntentFilter filterconn = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        btHelper.sourceActivity.registerReceiver(btHelper.bluetoothReceiver, filterconn);
        IntentFilter filterdiscon = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        btHelper.sourceActivity.registerReceiver(btHelper.bluetoothReceiver, filterdiscon);
        IntentFilter filterchange = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        btHelper.sourceActivity.registerReceiver(btHelper.bluetoothReceiver, filterchange);
        IntentFilter filterpair = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            filterpair = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
            btHelper.sourceActivity.registerReceiver(btHelper.bluetoothReceiver, filterpair);
        }
        btHelper.connectionThread = new AppBluetoothConnectionThread(device, btHelper);
        btHelper.connectionThread.start();
    }
    public void cancelBluetoothConnection(){
        connecting = false;
        try {
            if(btHelper.connectionThread.bluetoothSocket.isConnected()){
                btHelper.connectionThread.bluetoothSocket.close();
            }
            btHelper.connectionThread.interrupt();
            btHelper.connectionThread.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            bluetoothDevicesThread.interrupt();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: code " + requestCode);
        Log.d(TAG, "onRequestPermissionsResult: "+grantResults);
        Log.d(TAG, "onRequestPermissionsResult: "+permissions);
//        initBluetoothScan();
    }
    public void returnToSourceActivity(){
        Intent intent=new Intent();
        intent.putExtra("connectedDevice",btHelper.connectedDevice);
        setResult(RESULT_OK,intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelBluetoothConnection();
        try {
            unregisterReceiver(btHelper.bluetoothReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        checkLocation();
    }

    public void progressbarinit() {
        PP_Progress = ProgressDialog.show(ScanConnect.this, null, null, true);
        PP_Progress.setContentView(R.layout.progressbar_dialog_module);
        PP_Progress.setTitle("Please Wait.." + device.getName());
        PP_Progress.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));


        mCountDownTimer = new CountDownTimer(5000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                Log.v("Log_tag", "Tick of Progress" + millisUntilFinished);
                showProgressbar();
            }

            @Override
            public void onFinish() {
//                hideProgressbar();
            }

        };
        mCountDownTimer.start();
    }

    public void showProgressbar() {
        PP_Progress.show();

    }

    public void hideProgressbar() {
        PP_Progress.dismiss();

    }
}
