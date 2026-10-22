package in.dibc.kavitul.drcardio;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

import in.dibc.kavitul.bluetoothmodule.AppBluetoothHelper;
import in.dibc.kavitul.bluetoothmodule.BluetoothPermissions;
import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.mFile;
import in.dibc.kavitul.drcardio.SupportClass.mLog;
import in.dibc.kavitul.drcardio.utils.PathUtil;
import in.dibc.kavitul.drcardio.utils.PrefHelper;
import in.dibc.kavitul.drcardio.walkthroughscreen.IntroductionActivity;

public class StartupActivity extends AppCompatActivity {
    String TAG = "TeleECG_SplashScreen";
    String check_initial_val_for_pref = null;

    private final int SPLASH_TIME_OUT = 2000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_splashscreen);
        initApp();
    }

    //Initializes all the variables of appbt and then goes on to the next activity i.e. main activity or menu activity
    private void initApp() {
        PathUtil.TOKEN = PrefHelper.getString("AppToken", "");
        appbt.btHelper = new AppBluetoothHelper(this);
        appbt.res = getResources();
        appbt.packageName = getPackageName();
        appbt.btHelper.res = getResources();
        appbt.btHelper.SORa = appbt.btHelper.res.getInteger(R.integer.SORa);
        appbt.btHelper.SORb = appbt.btHelper.res.getInteger(R.integer.SORb);
        appbt.btHelper.EOR = appbt.btHelper.res.getInteger(R.integer.EOR);
        appbt.btHelper.LOSB = appbt.btHelper.res.getInteger(R.integer.status_bytes);
        appbt.btHelper.LODB = appbt.btHelper.res.getInteger(R.integer.data_bytes);
        appbt.btHelper.CMD_START = appbt.btHelper.res.getInteger(R.integer.CMD_START);
        appbt.btHelper.CMD_TEST_START = appbt.btHelper.res.getInteger(R.integer.CMD_TEST_START);
        appbt.btHelper.CMD_STOP = appbt.btHelper.res.getInteger(R.integer.CMD_STOP);
        appbt.btHelper.CMD_ACK = appbt.btHelper.res.getInteger(R.integer.CMD_ACK);
        appbt.btHelper.CMD_ERR = appbt.btHelper.res.getInteger(R.integer.CMD_ERR);
        appbt.pref = getApplicationContext().getSharedPreferences(appbt.res.getString(R.string.shared_pref_title), 0);  // 0 - for private mode
        checkCreateStorageFolders();
        mLog.logLevels = appbt.res.getStringArray(R.array.log_level);
        mLog.d("SplashScreen", "initApp", "Start Splash", "Splash successful");
        goToNextScreen();

    }


    private void showNextScreenButton(){
        Button btnContinue = findViewById(R.id.continueButton);
        btnContinue.setVisibility(View.VISIBLE);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToNextScreen();
            }
        });
    }
    private void goToNextScreen() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if( PrefHelper.getBoolean("isUserLoggedIn", false)) {
                    Intent intent = new Intent(StartupActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(StartupActivity.this, SignInActivity.class);
                    startActivity(intent);
                    finish();
                }

//                Intent intent = new Intent(StartupActivity.this, MainActivity.class);
//                startActivity(intent);
//                finish();
            }
        }, SPLASH_TIME_OUT);
        /*BluetoothPermissions btPerm = new BluetoothPermissions(appbt.btHelper);
        if (btPerm.checkPerm()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(StartupActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, SPLASH_TIME_OUT);
        } else {
            showNextScreenButton();
        }*/
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

    }

    public void checkCreateStorageFolders(){
        String baseDir = appbt.btHelper.res.getString(R.string.storage_folder);
        appbt.storageSubDirs = appbt.btHelper.res.getStringArray(R.array.storage_sub_folders);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            appbt.storageBaseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/"+ baseDir;
//        } else {
//            appbt.storageBaseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"+ baseDir;
//        }

        //#Bhavesh Change
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            appbt.storageBaseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/"+ baseDir;
        }else{
            appbt.storageBaseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"+ baseDir;
        }
        //appbt.storageBaseDir = mFile.getStorageParentDir(this).getAbsolutePath();

        File dir = new File(appbt.storageBaseDir);
        if(!dir.exists()){
            dir.mkdir();
        }
        for (int i = 0; i < appbt.storageSubDirs.length; i++){
            File sd = new File(appbt.storageBaseDir+"/"+appbt.storageSubDirs[i]);
            if(!sd.exists()){
                sd.mkdir();
            }
        }
    }
}
