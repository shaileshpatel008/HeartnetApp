package in.dibc.kavitul.drcardio.SupportClass;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import in.dibc.kavitul.bluetoothmodule.AppBluetoothHelper;
import in.dibc.kavitul.bluetoothmodule.AppDeviceConnectedThread;

@SuppressLint("MissingPermission")
public class appbt {
    public static AppBluetoothHelper btHelper;
    public static AppDeviceConnectedThread conn;
    public static Resources res;
    public static String packageName;
    //public static DeviceCommand devCmd;
    public static int lastCMD;
    private Handler mHandler;
    public static byte[] key = new byte[4];
    public static byte[] ipg_internal_status = new byte[4];
    public static SharedPreferences pref;
    public static String storageBaseDir;
    public static String[] storageSubDirs;
    public static int STORAGE_SUB_DIR_DATA = 0;
    public static int STORAGE_SUB_DIR_REPORT = 1;
    public static int STORAGE_SUB_DIR_CSV = 2;
    public static int STORAGE_SUB_DIR_LOG = 3;
    public static int STORAGE_SUB_DIR_OLD_DATA = 4;
    //public static ecgData ecg_data;
    /*
     *   Simple Keys for app functionality [Data Persistence]
     * */
    public static String KEY_LOGLEVEL = "logLevel";
    public static String Defulatlanguage;



    public appbt() {
        super();
        connectSocket();

    }

    public Boolean connectSocket() {
        try {
            btHelper.bluetoothSocket.connect();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void init() {
    }

    @SuppressLint("SimpleDateFormat")
    public static String getTodaysDate() {
        String s = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String datefolder = "Log_" + s;
        return datefolder;
    }

}
