package in.dibc.kavitul.bluetoothmodule;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.animation.LinearInterpolator;

import static in.dibc.kavitul.bluetoothmodule.ScanConnect.btHelper;

@SuppressLint("MissingPermission")
public class AppBroadcastReceiver extends BroadcastReceiver {
    String TAG = "DIBC_Bluetooth_BroadcastReceiver";
    public AppBluetoothHelper bthelp = btHelper;
    boolean connStatus = false;
    AppBroadcastReceiver() {

    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: "+action);
        BluetoothDevice device;

        switch (action){
            case BluetoothDevice.ACTION_FOUND:
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothDevices.addToSearchedDevices(device);
                Log.d(TAG, "onReceive: Found " + device.getName());
                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "onReceive: connected to " + device.getName());
                bthelp.alertConnected();
                connStatus = true;
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                if (connStatus) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "onReceive: disconnected from " + device.getName());
                    bthelp.alertError(3);
                    connStatus = false;
                }
                break;
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "onReceive: bond state for " + device.getName() + " changed to " + device.getBondState());
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    new ScanConnect().connectToDevice(device);
                }
                break;
            case BluetoothDevice.ACTION_PAIRING_REQUEST:
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "onReceive: pairing to "+device.getName());

                break;
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_OFF){
                    bthelp.alertError(1);
                }

                break;
            case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                Log.d(TAG, "onReceive: discovery started");
                startButtonAnimation();
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                Log.d(TAG, "onReceive: discovery stopped");
                //stopButtonAnimation();
                startButtonAnimation();
                break;
            default:

        }

    }

    private void startButtonAnimation(){

        Runnable repeatAnimation = new Runnable() {
            @Override
            public void run() {
                ScanConnect.searchDevicesFAB.animate().rotationBy(360).withEndAction(this).setDuration(2000).setInterpolator(new LinearInterpolator()).start();
            }
        };

        ScanConnect.searchDevicesFAB.animate()
                .rotationBy(90)
                .withLayer()
                .setDuration(1000)
                .withEndAction(repeatAnimation)
                .setInterpolator(new LinearInterpolator())
                .start();
    }
    private void stopButtonAnimation(){
        ScanConnect.searchDevicesFAB.animate()
                .rotationBy(90)
                .withLayer()
                .setDuration(1000)
                .setInterpolator(new LinearInterpolator())
                .start();
    }
}
