package in.dibc.kavitul.bluetoothmodule;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import java.util.UUID;

@SuppressLint("MissingPermission")
public class AppBluetoothHelper {
    public BluetoothDevice connectedDevice;
    public String connectedDeviceName = "";
    String TAG = "DIBC_Bluetooth_BTHelper";
    public BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public Resources res;
    public Activity sourceActivity;
    public Handler mHandlersbluetooth, mHandlersconnected, mHandlerUpdateUi;
    public Handler mBlutoothstatus;
    public AppBroadcastReceiver bluetoothReceiver;
    public Thread BTReceiverThread;
    public AppBluetoothConnectionThread connectionThread;
    public AppDeviceConnectedThread connectedThread;
    public static UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public BluetoothSocket bluetoothSocket;
    public int SORa, SORb, EOR, LOSB, LODB, CMD_START, CMD_STOP, CMD_ACK, CMD_ERR, CMD_TEST_START;
    public Handler redirectTOSacnActivity;
    Bundle bundle = new Bundle();

    public AppBluetoothHelper(Activity sourceAct) {
        sourceActivity = sourceAct;
        BTReceiverThread = new Thread(new Runnable() {
            public void run() {
                bluetoothReceiver = new AppBroadcastReceiver();
            }
        });
        BTReceiverThread.start();
    }


    public void alertError(final int errCode){
        Log.d(TAG, "alertError: type=bluetooth -- code="+errCode);
        AlertDialog.Builder builder = new AlertDialog.Builder(sourceActivity);
        String[] btErrorMessages = res.getStringArray(R.array.bt_connect_error);
        builder.setMessage(btErrorMessages[errCode])
                .setCancelable(false)
                .setPositiveButton(res.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        switch (errCode) {
                            case 0:
                                // Device does not support bluetooth. use demo mode.
                                break;
                            case 1:
                                // Enable Bluetooth popup
                                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                sourceActivity.startActivityForResult(enableBtIntent, ((short) R.integer.request_enable_bluetooth));
                                break;
                            case 2:
                                // Unable to show permission popup as user denied the permission. show application settings where user can re-enable the permissions
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", sourceActivity.getPackageName(), null);
                                intent.setData(uri);
                                sourceActivity.startActivity(intent);
                                break;
                            case 3:
                                // Disconnected from device
                                Message msg = mHandlersbluetooth.obtainMessage();
                                bundle.putInt("deviceResponce", -3);
                                msg.setData(bundle);
                                mHandlersbluetooth.sendMessage(msg);
                                break;
                        }
                    }
                })
                .setNegativeButton(res.getString(R.string.exit), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sourceActivity.finish();
                    }
                });
        //Creating dialog box
        AlertDialog alert = builder.create();
        String btErrorTitle = res.getString(R.string.bt_error);
        alert.setTitle(btErrorTitle);
        try {
            alert.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // TODO: 1/14/21 check if app is active before alert instead of using try-catch as showing alert window is not allowed while app is inactive
    }
    public void alertConnected() {
        AlertDialog.Builder builder = new AlertDialog.Builder(sourceActivity);
        builder.setMessage(res.getString(R.string.connected) + connectedDeviceName + ". " + res.getString(R.string.tapContinue))
                .setCancelable(false)
                .setPositiveButton(res.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Message msg = mHandlersconnected.obtainMessage();
                        bundle.putString("ConnectedTodevice", "Connected");
                        msg.setData(bundle);
                        mHandlersconnected.sendMessage(msg);

                    }
                });
        //Creating dialog box
        AlertDialog alert = builder.create();
        String btErrorTitle = res.getString(R.string.success);
        alert.setTitle(btErrorTitle);
        alert.show();

    }


//    public String getAppUuid(Context context) {
//        if (APP_UUID == null) {
//            SharedPreferences sharedPrefs = context.getSharedPreferences(
//                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
//            APP_UUID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
//            if (APP_UUID == null) {
//                APP_UUID = UUID.randomUUID().toString();
////                APP_UUID = UUID.fromString("0x1101").toString();
//                SharedPreferences.Editor editor = sharedPrefs.edit();
//                editor.putString(PREF_UNIQUE_ID, APP_UUID);
//                editor.commit();
//            }
//        }
//        Log.d(TAG, "getAppUuid: "+APP_UUID);
//        return APP_UUID;
//    }


}
