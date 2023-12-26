package in.dibc.kavitul.drcardio.SupportClass;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import in.dibc.kavitul.drcardio.StartupActivity;

public class App extends Application {
    private static final String TAG = "App";

    public static SharedPreferences myPref;
    private static App mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable e) {
                        Log.e(TAG, "uncaughtException: "+thread.getName(), e);
                        StackTraceElement[] stackTrace = e.getStackTrace();
                        String log = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+"Error: Unhandled exception occured \n";
                        log += e.getMessage()+" "+e.getCause()+ "\n";
                        for (int i = 0; i < stackTrace.length; i++) {
                            log += stackTrace[i].getClassName()+"::"+stackTrace[i].getMethodName()+": ("+stackTrace[i].getFileName()+":"+stackTrace[i].getLineNumber()+") \n";
                        }
                        mFile.writeLog(log);

                        Toast.makeText(getApplicationContext(), "Error occured. Restarting app.", Toast.LENGTH_LONG).show();
                        restart(getApplicationContext());
                    }
                });
        myPref = PreferenceManager.getDefaultSharedPreferences(App.mInstance);
    }
    public static void restart(Context context){
        Intent mainIntent = new Intent(context, StartupActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.getApplicationContext().startActivity(mainIntent);
        System.exit(0);
    }

    public static synchronized App getInstance() {
        return mInstance;
    }
}
