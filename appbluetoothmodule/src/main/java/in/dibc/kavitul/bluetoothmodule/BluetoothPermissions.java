package in.dibc.kavitul.bluetoothmodule;

import android.Manifest;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import java.util.ArrayList;
import java.util.List;

public class BluetoothPermissions {
    String TAG = "DIBC_Bluetooth_Permissions";
    public int checkCount = 0;
    public String[] permissionsRequired = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            //#Bhavesh Change
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    public List<String> listPermissionsToBeGranted = new ArrayList<>();

    public boolean permRationale = false;
    private final AppBluetoothHelper btHelper;

    public BluetoothPermissions(AppBluetoothHelper bth) {
        btHelper = bth;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsRequired = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
//                    Manifest.permission.ACCESS_COARSE_LOCATION,
//                    Manifest.permission.ACCESS_FINE_LOCATION,
                    //#Bhavesh Change
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
            };
        }
    }
    public boolean checkBluetooth(){
        return btHelper.bluetoothAdapter != null;
    }
    public boolean checkBluetoothEnabled(){
        return btHelper.bluetoothAdapter.isEnabled();
    }
    public boolean checkPermissions(){
        Log.d(TAG, "checkPermissions: ");
        for (String perm: permissionsRequired){
            if(PermissionChecker.checkSelfPermission(btHelper.sourceActivity, perm) != PermissionChecker.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(btHelper.sourceActivity,
                        perm)) {
                    permRationale = true;
                }
                listPermissionsToBeGranted.add(perm);
            }
            Log.d(TAG, "checkBluetoothPermissions: perm - "+perm+" - "+ContextCompat.checkSelfPermission(btHelper.sourceActivity, perm));
        }
        return listPermissionsToBeGranted.isEmpty();
    }
    public boolean requestPermissions(){
        Log.d(TAG, "checkBluetoothPermissions: requesting - "+listPermissionsToBeGranted.size()+" permissions");
        if(!listPermissionsToBeGranted.isEmpty()){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(btHelper.sourceActivity,
                        listPermissionsToBeGranted.toArray(new String[listPermissionsToBeGranted.size()]),
                        ((short) R.integer.request_permission_code));
                return false;
            }
        }
        return true;
    }

    public boolean check() {
        if (checkBt()) {
            return checkPerm();
        }
        return false;
    }

    public boolean checkBt() {
        if (!checkBluetooth()) {
            btHelper.alertError(0);
            return false;
        }
        if (!checkBluetoothEnabled()) {
            btHelper.alertError(1);
            return false;
        }
        return true;
    }

    public boolean checkPerm() {
        if (!checkPermissions()) {
            if (!listPermissionsToBeGranted.isEmpty()) {
                if (permRationale || checkCount > 3) {
                    btHelper.alertError(2);
                } else {
                    requestPermissions();
                    checkCount++;
                }
                return false;
            }
        }
        return true;
    }

}
