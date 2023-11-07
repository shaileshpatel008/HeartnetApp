
package in.dibc.kavitul.drcardio.SupportClass;

import android.annotation.SuppressLint;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class mLog {
    private static String TAG = "CLog";
    //    YYYYmmdd_HHmmss_LogLevel_Activity_Action/Function_data_Result
    private static String SEPARATOR = ",";
    public static int logLevel = 2;
    public static String[] logLevels;
    // TODO: 29-09-2020 : Create a field under advanced settings to change log level in settings screen. 

    public static void writeLog(int level, String activity, String action, String data, String result){
        String ll = String.valueOf(level);
        Log.d(TAG, "writeLog: "+logLevels);
        if(logLevels != null && logLevels.length > level){
            ll = logLevels[level];
        }
        @SuppressLint("SimpleDateFormat") String fileLogData = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + SEPARATOR + ll + SEPARATOR + activity + SEPARATOR + action + SEPARATOR +
                data + SEPARATOR + result;
        if (logLevel <= level) {
            mFile.writeLog(fileLogData);
        }
    }

    public static void v(String activity, String action, String data, String result) {
        writeLog(Log.VERBOSE, activity, action, data, result);
        Log.v(activity, action + SEPARATOR + data + SEPARATOR + result);
    }

    public static void d(String activity, String action, String data, String result) {
        writeLog(Log.DEBUG, activity, action, data, result);
        Log.d(activity, action + SEPARATOR + data + SEPARATOR + result);
    }

    public static void i(String activity, String action, String data, String result) {
        writeLog(Log.INFO, activity, action, data, result);
        Log.i(activity, action + SEPARATOR + data + SEPARATOR + result);
    }

    public static void w(String activity, String action, String data, String result) {
        writeLog(Log.WARN, activity, action, data, result);
        Log.w(activity, action + SEPARATOR + data + SEPARATOR + result);
    }

    public static void e(String activity, String action, String data, String result) {
        writeLog(Log.ERROR, activity, action, data, result);
        Log.e(activity, action + SEPARATOR + data + SEPARATOR + result);
    }
}

