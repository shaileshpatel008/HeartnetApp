package in.dibc.kavitul.drcardio;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

import static in.dibc.kavitul.drcardio.SupportClass.appbt.key;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gauravbhola.ripplepulsebackground.RipplePulseLayout;
import com.gauravk.bubblenavigation.BubbleNavigationLinearView;
import com.gauravk.bubblenavigation.listener.BubbleNavigationChangeListener;
import com.google.android.gms.common.util.ArrayUtils;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.saadahmedsoft.popupdialog.PopupDialog;
import com.saadahmedsoft.popupdialog.Styles;
import com.saadahmedsoft.popupdialog.listener.OnDialogButtonClickListener;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import in.dibc.kavitul.bluetoothmodule.AppBluetoothConnectionThread;
import in.dibc.kavitul.bluetoothmodule.BluetoothPermissions;
import in.dibc.kavitul.bluetoothmodule.ScanConnect;
import in.dibc.kavitul.drcardio.Model.ecgData;
import in.dibc.kavitul.drcardio.SupportClass.LocaleHelper;
import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.settings;
import in.dibc.kavitul.drcardio.utils.PathUtil;
import in.dibc.kavitul.drcardio.utils.PrefHelper;
import in.dibc.kavitul.drcardio.widgets.RoundedImageView;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    String TAG = "TeleECG_Main";
    TextView menu_newecg, menu_csv, menu_hrv, txt_acquire_new_ecg;
    LinearLayout menu_loaddata, menu_reports, menu_settings, menu_help, menu_new_ecg, llAcquireNewECG, llHelp;
    ImageView img_menu_newecg, img_menu_loaddata, img_menu_reports, img_menu_settings, img_menu_help;
    TextView txt_connected_device_name, txt_load_data;
    int device_is_connected = 0;
    private Handler mHandler;
    private String cur_lang;
    private boolean testAck = false;
    int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 4848;

    private static final int PERMISSION_REQUEST_CODE = 200;

    ActivityResultLauncher<Intent> settingActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        setNewECGbuttonText();
                    }
                }
            });
    private boolean waitingForVersionResponse = false;

    BubbleNavigationLinearView bubbleNavigation;

    RipplePulseLayout rippleBackground;

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 98;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_menu);
        initApp();

        //#Bhavesh Change
        isStoragePermissionGranted();



        checkBluetoothPermission();
        requestBluetoothPermission();

        /*if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }*/

        rippleBackground = findViewById(R.id.layout_ripplepulse);
        rippleBackground.startRippleAnimation();

        bubbleNavigation = findViewById(R.id.equal_navigation_bar);
        bubbleNavigation.setNavigationChangeListener(new BubbleNavigationChangeListener() {
            @Override
            public void onNavigationChanged(View view, int position) {
                //navigation changed, do something
                if(position == 1) {
                    Intent intent = new Intent(MainActivity.this, ReportActivity.class);
                    startActivity(intent);
                } else if(position == 2) {
                    Intent intent = new Intent(MainActivity.this, MyAccountsActivity.class);
                    startActivity(intent);
                }

                /*new Handler().postDelayed(
                        new Runnable() {
                            public void run() {
                                bubbleNavigation.setCurrentActiveItem(0);
                            }
                        }, 2000);*/
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        appbt.btHelper.sourceActivity = this;
        setHandler();
        if(!cur_lang.equals(settings.language_code)) {
            recreate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        getFileNames();

        getUserLocation();

        checkLocationPermission();

        bubbleNavigation.setCurrentActiveItem(0);

        /*if (SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestPermission();
            }
        }*/

        //Log.v("PDF GENERATE COUNT :: ", ""+PrefHelper.getInt("PDFGenerateCount", 0));
        //PrefHelper.setInt("PDFGenerateCount", (PrefHelper.getInt("PDFGenerateCount", 0)+1));
        //if(PrefHelper.getInt("PDFGenerateCount", 0) % 6 == 1) {
        //if( ArrayUtils.contains(PathUtil.reviewsContainsCount, PrefHelper.getInt("PDFGenerateCount", 0))) {
        if(PrefHelper.getInt("PDFGenerateCount", 0) == 5) {
//            Log.v("True", ""+PrefHelper.getInt("PDFGenerateCount", 0));
            PrefHelper.setInt("PDFGenerateCount", 0);
            rateAndReviewsDialog();
        } else {
//            Log.v("False", ""+PrefHelper.getInt("PDFGenerateCount", 0));
        }

        if (settings.test_mode.equals("true")) {
            txt_acquire_new_ecg.setText("ACQUIRE TEST ECG");
        } else {
            txt_acquire_new_ecg.setText("ACQUIRE NEW ECG");
        }
    }

    private void requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                startActivityForResult(intent, 2296);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 2296);
            }
        } else {
            //below android 11
            //ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    public void getUserLocation() {
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public  boolean isStoragePermissionGranted() {
        if (SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

//    public  boolean isStoragePermissionGrantedAndroid13() {
//        if (SDK_INT >= Build.VERSION_CODES.M) {
//            if (checkSelfPermission(READ_MEDIA_IMAGES)
//                    == PackageManager.PERMISSION_GRANTED) {
//                Log.v(TAG,"Permission is granted");
//                return true;
//            } else {
//
//                Log.v(TAG,"Permission is revoked");
//                ActivityCompat.requestPermissions(this, new String[]{READ_MEDIA_IMAGES}, 1);
//                return false;
//            }
//        }
//        else { //permission is automatically granted on sdk<23 upon installation
//            Log.v(TAG,"Permission is granted");
//            return true;
//        }
//    }

    /*@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }*/

    private void initApp(){
        if(appbt.pref != null){
            settings.getSettings();
            LocaleHelper.setLocaleLanguage(MainActivity.this);
            cur_lang = settings.language_code;
            setHandler();
            initUIElements();
            if (checkPermission()) {
                checkLastConnectedDevice();
            }

            //showDataBackupWarningDialog();
        } else {
            Intent intent = new Intent(MainActivity.this, StartupActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void showBtError() {
        new AlertDialog.Builder(this)
                .setTitle("Please note :")
                .setMessage("To acquire new ECG reading, you need active bluetooth as well as necessary permissions. You can still use the app to load & view previous data/report.")
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    public boolean checkPermission() {
        BluetoothPermissions btPerm = new BluetoothPermissions(appbt.btHelper);
        if (btPerm.check()) {
            if (appbt.btHelper.sourceActivity == null) {
                registerBluetoothReceivers();
            }
            return true;
        } else {
            showBtError();
            return false;
        }
    }

    private void registerBluetoothReceivers() {
        appbt.btHelper.sourceActivity = this;
        appbt.btHelper.bluetoothReceiver.bthelp = appbt.btHelper;
        IntentFilter filterdiscon = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        appbt.btHelper.sourceActivity.registerReceiver(appbt.btHelper.bluetoothReceiver, filterdiscon);
        IntentFilter filterstate = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        appbt.btHelper.sourceActivity.registerReceiver(appbt.btHelper.bluetoothReceiver, filterstate);
    }

    // start scan connect activity expecting result.
    private void startScanConnect() {
        disconnectFromDevice();
        Intent scanConnect = new Intent(MainActivity.this, ScanConnect.class);  //Redirect to the ScanConnect class to scan and connect to a bluetooth device
        startActivityForResult(scanConnect, 101);
    }

    //When result is received for startActivityForResult
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: req:"+requestCode+"; res:"+resultCode);
        if (requestCode == 101) {
            if(resultCode == RESULT_OK) {
                appbt.btHelper.connectedDevice = data.getParcelableExtra("connectedDevice");
                //Setting the text view of the main activity with the device name i.e. beside the bluetooth icon
                tryConnectTo();
//                connectedToDevice();
            } else {
                Log.d(TAG, "onActivityResult: not ok");
            }
        }
    }

    private void setHandler(){
//        appbt.btHelper.alertConnected();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String packetError = null;
                Intent intent = null;
                Bundle bundle = msg.getData();

                int deviceResponce = bundle.getInt("deviceResponce");
                /* Device Response check  */
//                Log.d(TAG, "handleMessage: msg:"+deviceResponce);
                switch (deviceResponce) {
                    // Custom cases for software only
                    case -4:
                        int ver = 1;
                        try {
                            ver = bundle.getInt("deviceVersion");
//                            Toast.makeText(getApplicationContext(), "received ver " + ver, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d(TAG, "handleMessage: ");
                        }
                        setVersionSettings(ver);
                        break;
                    case -3:
                        // disconnected from device / unable to communicate
                        disconnectFromDevice();
                        break;
                    case -2:
                        try {
                            appbt.conn = appbt.btHelper.connectionThread.getConnectedThread();
                            if (appbt.conn != null) {
                                appbt.conn.btHelper = appbt.btHelper;
                                connectedToDevice();
                            } else {
                                device_is_connected = 0;
                            }
//                            tryConnectTo(appbt.btHelper.connectedDevice.getAddress());
                        } catch (Exception e) {
                            e.printStackTrace();
                            device_is_connected = 0;
                        }

                        break;
                    case -1:
                        String connectionStatus = bundle.getString("connectionStatus");
                        String connectedDeviceName = bundle.getString("deviceName");
                        Log.d(TAG, "handleMessage: "+connectionStatus+" - "+connectedDeviceName);

                        if(connectionStatus.equals("connected")){
                            if(connectedDeviceName != null) {
                                ecgData.patinet_deviceid = connectedDeviceName;
                            }
                            connectedToDevice();
                        } else{
                            device_is_connected = 0;
                            txt_connected_device_name.setClickable(true);
                            txt_connected_device_name.setEnabled(true);
                            txt_connected_device_name.setText(appbt.res.getString(R.string.unable_to_connect_to) + appbt.btHelper.connectedDevice.getName());
                        }
                        break;
                    case 0:

                        break;

                    // Device cases

                    default:

                }
            }
        };
        appbt.btHelper.mHandlersbluetooth = mHandler;
    }

    private void tryConnectTo() {
        disconnectFromDevice();
        if(appbt.btHelper.connectedDevice == null){
            return;
        }
        device_is_connected = 1;
        String txt_msg = appbt.res.getString(R.string.try_connect_to) + appbt.btHelper.connectedDevice.getName();
        txt_connected_device_name.setText(txt_msg);
        txt_connected_device_name.setClickable(false);
        txt_connected_device_name.setEnabled(false);
        new Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        appbt.btHelper.connectionThread = new AppBluetoothConnectionThread(appbt.btHelper.connectedDevice, appbt.btHelper);
                        appbt.btHelper.connectionThread.start();
                    }
                },
                2000);
    }

    public void connectedToDevice() {
        txt_connected_device_name.setClickable(false);
        txt_connected_device_name.setEnabled(false);
        txt_connected_device_name.setText(appbt.res.getString(R.string.connected) + appbt.btHelper.connectedDevice.getName());
        txt_connected_device_name.setTextColor(getResources().getColor(R.color.green_active));
        txt_acquire_new_ecg.setVisibility(View.VISIBLE);
        llAcquireNewECG.setBackground(getResources().getDrawable(R.drawable.rounded_rect_new_ecg));
        rippleBackground.stopRippleAnimation();
        menu_new_ecg.setBackground(getResources().getDrawable(R.drawable.circle_rect_primary));
        settings.saved_address = appbt.btHelper.connectedDevice.getAddress();
        settings.saved_device_name = appbt.btHelper.connectedDevice.getName();
        settings.saveSettings();
        device_is_connected = 2;
        appbt.conn = appbt.btHelper.connectionThread.getConnectedThread();
        if (appbt.conn == null) {
            disconnectFromDevice();
        } else {
            appbt.conn.btHelper = appbt.btHelper;
            checkHwVersion();
        }

        if (settings.test_mode.equals("true")) {
            txt_acquire_new_ecg.setText("ACQUIRE TEST ECG");
        } else {
            txt_acquire_new_ecg.setText("ACQUIRE NEW ECG");
        }
    }

    private void checkHwVersion() {
        waitingForVersionResponse = true;
        ecgData.hwVersion = 1;
        try {
            byte[] cmd = {0x23};
            appbt.conn.write(cmd);
            cmd[0] = 0x53;
            appbt.conn.write(cmd);
            cmd[0] = 0x56;
            appbt.conn.write(cmd);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setVersionSettings();
                }
            }, 2000);
        } catch (Exception e) {
            e.printStackTrace();
            disconnectFromDevice();
        }
    }

    private ArrayList<String> getFileNames() {
        String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/";
        Log.v("Reports File Path :: ", ""+filePath);
        File dir = new File(filePath);
        Log.v("Directory Created :: ", ""+dir.isDirectory());
        ArrayList<String> list = new ArrayList<String>();
        if (dir.isDirectory()) {
            Log.v("Directory Length :: ", ""+dir.listFiles().length);
            if(dir.list() != null) {
                String[] files = dir.list();
                Pattern p = Pattern.compile("^(.*?)\\.pdf$");
                Log.v("Files Length :: ", ""+files.length);
                for (String file : files) {
                    Matcher m = p.matcher(file);
                    if (m.matches()) {
                        list.add(m.group(1));
                    }
                }
            }
        }

        if(list.size() > 0) {
            txt_load_data.setText("LOAD DATA ("+list.size()+")");
        }


        //Collections.reverse(list);
        return list;
    }

    private void testAcquisition() {
        byte[] cmd = {0x54};
        appbt.conn.write(cmd);
        testAck = false;
        appbt.btHelper.mHandlersbluetooth = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        testAck = true;
                        cmd[0] = 0x53;
                        appbt.conn.write(cmd);
                        setVersionSettings();
                        setHandler();
                }
            }
        };
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cmd[0] = 0x53;
                appbt.conn.write(cmd);
                if (!testAck) {
                    disconnectFromDevice();
                } else {
                    setVersionSettings();
                }
                setHandler();
            }
        }, 2000);
    }

    private void setVersionSettings(int ver) {
        ecgData.hwVersion = ver;
        setVersionSettings();
    }

    private void setVersionSettings() {
        waitingForVersionResponse = false;
        if (ecgData.hwVersion < 2) {
            ecgData.hwVersion = 1;
        }

        menu_newecg.setTextColor(Color.RED);
        //Toast.makeText(MainActivity.this, "Device Version " + ecgData.hwVersion, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "setVersionSettings: " + ecgData.hwVersion);
        if (ecgData.hwVersion == 2) {
            appbt.btHelper.LODB = 24;
            settings.val_per_mv = 3495f;
        } else {
            appbt.btHelper.LODB = 16;
            settings.val_per_mv = 54.61f;
        }
    }

    private void disconnectFromDevice() {
        waitingForVersionResponse = false;
        if (appbt.btHelper.connectedThread != null) {
            if (appbt.btHelper.connectedThread.bluetoothSocket != null && appbt.btHelper.connectedThread.bluetoothSocket.isConnected()) {
                appbt.btHelper.connectedThread.disconnect();
            }
            appbt.btHelper.connectedThread.interrupt();
        }
        if(appbt.btHelper.connectionThread != null) {
            if(appbt.btHelper.connectionThread.bluetoothSocket != null && appbt.btHelper.connectionThread.bluetoothSocket.isConnected()) {
                appbt.btHelper.connectionThread.disconnect();
            }
            appbt.btHelper.connectionThread.interrupt();
        }
        appbt.conn = null;
        device_is_connected = 0;
        if(txt_connected_device_name != null){
            txt_connected_device_name.setClickable(true);
            txt_connected_device_name.setEnabled(true);
            txt_connected_device_name.setText(appbt.res.getString(R.string.click_here_to_connect));
        }
        if(menu_newecg != null){
            menu_newecg.setTextColor(appbt.res.getColor(R.color.darkgray));
        }
    }
    public void initUIElements() {
        txt_connected_device_name = findViewById(R.id.txt_connected_device_name);
        menu_newecg = findViewById(R.id.txt_new_ecg);
        txt_acquire_new_ecg = findViewById(R.id.txt_acquire_new_ecg);
        menu_loaddata = findViewById(R.id.menu_loaddata);
        menu_reports = findViewById(R.id.menu_reports);
        menu_settings = findViewById(R.id.menu_settings);
        menu_help = findViewById(R.id.menu_help);
        menu_new_ecg = findViewById(R.id.menu_new_ecg);
        img_menu_newecg = findViewById(R.id.txt_new_ecg_icon);
        img_menu_loaddata = findViewById(R.id.txt_load_data_icon);
        img_menu_reports = findViewById(R.id.txt_reports_icon);
        img_menu_settings = findViewById(R.id.txt_settings_icon);
        img_menu_help = findViewById(R.id.txt_help_icon);
        llAcquireNewECG = findViewById(R.id.llAcquireNewECG);
        llHelp = findViewById(R.id.llHelp);

        txt_load_data = findViewById(R.id.txt_load_data);


        menu_newecg.setTextColor(appbt.res.getColor(R.color.darkgray));
        menu_newecg.setOnClickListener(this);
        menu_loaddata.setOnClickListener(this);
        menu_reports.setOnClickListener(this);
        menu_settings.setOnClickListener(this);
        menu_help.setOnClickListener(this);
        menu_new_ecg.setOnClickListener(this);
        img_menu_newecg.setOnClickListener(this);
        img_menu_loaddata.setOnClickListener(this);
        img_menu_reports.setOnClickListener(this);
        img_menu_settings.setOnClickListener(this);
        img_menu_help.setOnClickListener(this);
        txt_connected_device_name.setOnClickListener(this);
        txt_acquire_new_ecg.setOnClickListener(this);
        llAcquireNewECG.setOnClickListener(this);
        llHelp.setOnClickListener(this);
        setNewECGbuttonText();
    }

    public void checkLastConnectedDevice() {
        if (settings.saved_address != null && !settings.saved_address.equals("")) {
            appbt.btHelper.connectedDevice = appbt.btHelper.bluetoothAdapter.getRemoteDevice(settings.saved_address);
            tryConnectTo();
        } else {
            txt_connected_device_name.setClickable(true);
            txt_connected_device_name.setEnabled(true);
            txt_connected_device_name.setText(appbt.res.getString(R.string.click_here_to_connect));  //Display that we need to click to connect to a bluetooth device beside the bluetooth icon.
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        LinearLayout ll_new_ecg_parent = findViewById(R.id.ll_new_ecg_parent);
//        if(settings.csv){
//            menu_csv.setVisibility(View.VISIBLE);
//            ll_new_ecg_parent.setWeightSum(2);
//        } else {
//            menu_csv.setVisibility(View.GONE);
//            ll_new_ecg_parent.setWeightSum(1);
//        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.txt_connected_device_name:
                if (!checkPermission()) {
                    return;
                }
                if (device_is_connected > 0) {
                    //Alert user to confirm device change
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.alert_change_device_title)
                            .setMessage(R.string.alert_message_change_device)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    startScanConnect();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .create()
                            .show();
                    break;
                } else {
                    startScanConnect();
                }
                break;
            case R.id.menu_new_ecg:
            case R.id.txt_new_ecg:
            case R.id.txt_acquire_new_ecg:
            case R.id.llAcquireNewECG:
            case R.id.txt_new_ecg_icon:
                if (!checkPermission()) {
                    return;
                }
                if (device_is_connected == 1 || waitingForVersionResponse) {
                    Toast.makeText(this, "Please wait trying to connect...", Toast.LENGTH_SHORT).show();
                } else if (device_is_connected == 2) {
                    intent = new Intent(MainActivity.this, NewEcgActivity.class);
                } else {
                    startScanConnect();
                }
                break;
            case R.id.txt_load_data:
            case R.id.txt_load_data_icon:
            case R.id.menu_loaddata:
                intent = new Intent(MainActivity.this, LoadDataActivity.class);
                break;
            case R.id.txt_reports:
            case R.id.txt_reports_icon:
            case R.id.menu_reports:
                intent = new Intent(MainActivity.this, ReportActivity.class);
                break;
            case R.id.txt_settings:
            case R.id.menu_settings:
            case R.id.txt_settings_icon:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.putExtra("From", "Settings88888888");
                settingActivityResultLauncher.launch(intent);
                return;
//                break;
            case R.id.txt_help:
            case R.id.txt_help_icon:
            case R.id.menu_help:
            case R.id.llHelp:
                /*FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
                Bundle params2 = new Bundle();
                params2.putString(FirebaseAnalytics.Param.VALUE, "Clicked On Help Menu from Main Screen");
                mFirebaseAnalytics.logEvent("Main_Activity_Help_Pressed", params2);*/
                //intent = new Intent(MainActivity.this, HelpActivity.class);
                helpDialog();
                break;
        }
        if (intent != null) {
            startActivity(intent);
        }
    }

    private void helpDialog() {
        PopupDialog.getInstance(this)
                .setStyle(Styles.IOS)
                .setHeading("Help")
                .setDescription("Do you want to contact support via phone call or chat using WhatsApp?")
                .setNegativeButtonText("Call")
                .setPositiveButtonText("WhatsApp")
                .setPositiveButtonTextColor(R.color.colorPrimary)
                .setNegativeButtonTextColor(R.color.colorPrimary)
                //.setPositiveButtonBackground(R.color.colorPrimary)
                //.setPopupDialogIcon(R.drawable.btn_icon_help)
                //.setPopupDialogIconTint(R.color.colorPrimary)
                .setCancelable(true)
                .showDialog(new OnDialogButtonClickListener() {
                    @Override
                    public void onPositiveClicked(Dialog dialog) {
                        super.onPositiveClicked(dialog);

                        PackageManager packageManager = getPackageManager();
                        Intent i = new Intent(Intent.ACTION_VIEW);

                        try {
                            String phone = "+919638389333";
                            String url = "https://api.whatsapp.com/send?phone="+ phone +"&text=" + URLEncoder.encode("Hi!", "UTF-8");
                            i.setPackage("com.whatsapp");
                            i.setData(Uri.parse(url));
                            //if (i.resolveActivity(packageManager) != null) {
                            startActivity(i);
                            //}
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onNegativeClicked(Dialog dialog) {
                        super.onNegativeClicked(dialog);
                        Intent intent = new Intent(Intent.ACTION_DIAL);

                        intent.setData(Uri.parse("tel:+919638389333"));
                        startActivity(intent);
                    }
                });
    }

    private void rateAndReviewsDialog() {
        PopupDialog.getInstance(this)
                .setStyle(Styles.IOS)
                .setHeading("Rate & Review")
                .setDescription("Do you want to give review of the app?")
                .setNegativeButtonText("Later")
                .setPositiveButtonText("Rate & Review")
                .setPositiveButtonTextColor(R.color.colorPrimary)
                .setNegativeButtonTextColor(R.color.black)
                //.setPositiveButtonBackground(R.color.colorPrimary)
                //.setPopupDialogIcon(R.drawable.btn_icon_help)
                //.setPopupDialogIconTint(R.color.colorPrimary)
                .setCancelable(true)
                .showDialog(new OnDialogButtonClickListener() {
                    @Override
                    public void onPositiveClicked(Dialog dialog) {
                        super.onPositiveClicked(dialog);

                        openAppRating(MainActivity.this);
                    }

                    @Override
                    public void onNegativeClicked(Dialog dialog) {
                        super.onNegativeClicked(dialog);
                        dialog.dismiss();
                    }
                });
    }

    public void openAppRating(Context context) {
        // you can also use BuildConfig.APPLICATION_ID
        String appId = context.getPackageName();
        Intent rateIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + appId));
        boolean marketFound = false;

        // find all applications able to handle our rateIntent
        final List<ResolveInfo> otherApps = context.getPackageManager()
                .queryIntentActivities(rateIntent, 0);
        for (ResolveInfo otherApp: otherApps) {
            // look for Google Play application
            if (otherApp.activityInfo.applicationInfo.packageName
                    .equals("com.android.vending")) {

                ActivityInfo otherAppActivity = otherApp.activityInfo;
                ComponentName componentName = new ComponentName(
                        otherAppActivity.applicationInfo.packageName,
                        otherAppActivity.name
                );
                // make sure it does NOT open in the stack of your activity
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // task reparenting if needed
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                // if the Google Play was already open in a search result
                //  this make sure it still go to the app page you requested
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                // this make sure only the Google Play app is allowed to
                // intercept the intent
                rateIntent.setComponent(componentName);
                context.startActivity(rateIntent);
                marketFound = true;
                break;

            }
        }

        // if GP not present on device, open web browser
        if (!marketFound) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id="+appId));
            context.startActivity(webIntent);
        }
    }


    private void setNewECGbuttonText() {
        TextView newEcg = findViewById(R.id.txt_new_ecg);
        if (settings.test_mode.equals("true")) {
            newEcg.setText(R.string.test_ecg);
            txt_acquire_new_ecg.setText("ACQUIRE TEST ECG");
        } else {
            newEcg.setText(R.string.new_ecg);
            txt_acquire_new_ecg.setText("ACQUIRE NEW ECG");
        }
    }

    /*public void showDataBackupWarningDialog(){

        if(!settings.shouldShowDataBackupDialog())
            return;


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.msgDataBackupWarning))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.btnIUnderStand), (dialog, id) -> {
                    dialog.dismiss();
                    settings.setDataBackupDialogShown();
                });
        //Creating dialog box
        AlertDialog alert = builder.create();
        alert.setTitle(getString(R.string.app_name));
        alert.show();
    }*/

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        disconnectFromDevice();
        super.onDestroy();
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission")
                        .setMessage("You have to allow location permission to access location")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        //locationManager.requestLocationUpdates(provider, 400, 1, this);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean bluetoothConnectAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean bluetoothScanAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (bluetoothConnectAccepted) {
                        //Toast.makeText(MainActivity.this, "Permission Granted, Now you can access location data and camera.", Snackbar.LENGTH_LONG).show();
                    } else {

                        //Toast.makeText(MainActivity.this, "Permission Denied, You cannot connect with bluetooth to pairing.", Toast.LENGTH_LONG).show();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)) {
                                showMessageOKCancel("You need to allow access to bluetooth connect permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{/*Manifest.permission.ACCESS_FINE_LOCATION,*/ Manifest.permission.BLUETOOTH_CONNECT},
                                                            PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }
                    }

                    if (bluetoothScanAccepted) {
                        //Toast.makeText(MainActivity.this, "Permission Granted, Now you can access location data and camera.", Snackbar.LENGTH_LONG).show();
                    } else {

                        //Toast.makeText(MainActivity.this, "Permission Denied, You cannot connect with bluetooth to pairing.", Toast.LENGTH_LONG).show();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)) {
                                showMessageOKCancel("You need to allow access to bluetooth connect permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{/*Manifest.permission.ACCESS_FINE_LOCATION,*/ Manifest.permission.BLUETOOTH_SCAN},
                                                            PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;

        }
    }

    private boolean checkBluetoothPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN);

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermission() {

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, PERMISSION_REQUEST_CODE);

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new android.app.AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
}
