package in.dibc.kavitul.drcardio.SupportClass;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;

import in.dibc.kavitul.drcardio.R;

public class settings {
    public static String test_mode, threeByteData, auto_start, auto_save, gain, actualGain, filter, report = "Sequential", doctor_name, doctor_address, doctor_contactno, doctor_website, doctor_email, saved_address, saved_device_name, language, log, language_code;
    // TODO: 1/15/21 create dropdown selection for default report type;
    public static int auto_start_seconds, auto_read_seconds, max_read_seconds = 300;
    public static boolean csv, debug_mode = false;
    public static String[] reportTypes = appbt.res.getStringArray(R.array.generate_report);
    public static boolean[] reports = new boolean[reportTypes.length];
    public static float val_per_mv = 54.61f; // 54.61 in data corresponds to 1mV in firmware v1

    public static ArrayList<String> filter_values = new ArrayList(Arrays.asList(appbt.res.getStringArray(R.array.spinner_filter)));
    public static ArrayList<String> filter_actual_values = new ArrayList(Arrays.asList(appbt.res.getStringArray(R.array.spinner_actual_filter)));
    public static ArrayList<String> gain_values = new ArrayList(Arrays.asList(appbt.res.getStringArray(R.array.spinner_gain)));
    public static ArrayList<String> gain_actual_values = new ArrayList(Arrays.asList(appbt.res.getStringArray(R.array.spinner_actual_gain)));
    public static ArrayList<String> report_values = new ArrayList(Arrays.asList(appbt.res.getStringArray(R.array.generate_report)));


    public settings(){
        getSettings();
    }

    public static void getSettings(){
        //Other Parameters Default Settings
        test_mode = appbt.pref.getString("settings_test_mode", "false");
        csv = appbt.pref.getBoolean("csv", true);
        threeByteData = appbt.pref.getString("settings_threeByteData", "");
        auto_start = appbt.pref.getString("settings_auto_start", "");
        auto_start_seconds = appbt.pref.getInt("settings_auto_start_seconds", 10);
        auto_read_seconds = appbt.pref.getInt("settings_auto_read_seconds", 10);
        auto_save = appbt.pref.getString("settings_auto_save", "");
        gain = appbt.pref.getString("settings_gain", "");
        actualGain = appbt.pref.getString("settings_actualgain", "");

        filter = appbt.pref.getString("settings_filter", "");
        //Doctor Default Settings
        doctor_name = appbt.pref.getString("settings_doctor_name", "");
        doctor_address = appbt.pref.getString("settings_doctor_address", "");
        doctor_contactno = appbt.pref.getString("settings_doctor_contactno", "");
        doctor_website = appbt.pref.getString("settings_doctor_website", "");
        doctor_email = appbt.pref.getString("settings_doctor_email", "");
        //Saved device settings
        saved_address = appbt.pref.getString("saved_device_address", "");  //Get saved bluetooth device address if available
        saved_device_name = appbt.pref.getString("saved_device_name", "");  //Get saved bluetooth device address if available
        //Advanced Settings
        language = appbt.pref.getString("settings_language", "");
        log = appbt.pref.getString("settings_log", "");
        language_code = appbt.pref.getString("settings_language_code", "");
        String reportStr = appbt.pref.getString("settings_reports", "");
        reports = csvToBoolArr(reportStr);
        if (reports.length != settings.reportTypes.length) {
            setFirstReportsOn();
        }
        checkDefaultSettings();
    }

    private static void setFirstReportsOn() {
        reports = new boolean[reportTypes.length];
        for (int i = 0; i < reports.length; i++) {
            reports[i] = false;
        }
        reports[0] = true;
    }

    private static void setAllReportsOn() {
        reports = new boolean[reportTypes.length];
        for (int i = 0; i < reports.length; i++) {
            reports[i] = true;
        }
    }

    private static void checkDefaultSettings() {
        //Get any value of the settings activity, in this case we take the value of the doctor's name to check.
        if (gain.isEmpty() || gain == null || actualGain.isEmpty() || actualGain == null) {
            setDefaultSettings(); // no indicates that there are no default values for settings
        }
    }


    private static void setDefaultSettings(){
        test_mode = appbt.res.getString(R.string.settings_default_test_mode);
        csv = true;
        threeByteData = appbt.res.getString(R.string.settings_default_threeByteData);
        auto_start = appbt.res.getString(R.string.settings_default_auto_start);
        auto_start_seconds = appbt.res.getInteger(R.integer.settings_default_auto_start_seconds);
        auto_read_seconds = appbt.res.getInteger(R.integer.settings_default_auto_read_seconds);
        filter = appbt.res.getString(R.string.settings_default_filter);
        gain = appbt.res.getString(R.string.settings_default_gain);
        actualGain = "6";
        auto_save = appbt.res.getString(R.string.settings_default_auto_save);
        doctor_name = appbt.res.getString(R.string.settings_default_doctor_name);
        doctor_address = appbt.res.getString(R.string.settings_default_doctor_address);
        doctor_contactno = appbt.res.getString(R.string.settings_default_doctor_contactno);
        doctor_website = appbt.res.getString(R.string.settings_default_doctor_website);
        doctor_email = appbt.res.getString(R.string.settings_default_doctor_email);
        saved_address = "";
        saved_device_name = "";
        language = appbt.res.getString(R.string.settings_default_language);
        log = appbt.res.getString(R.string.settings_default_log);
        language_code = appbt.res.getString(R.string.settings_default_language_code);
        setFirstReportsOn();
        saveSettings();
    }



    public static void saveSettings(){
        SharedPreferences.Editor edit = appbt.pref.edit();
        edit.putString("settings_test_mode", test_mode);
        edit.putBoolean("csv", csv);
        edit.putString("settings_threeByteData", threeByteData);
        edit.putString("settings_auto_start", auto_start);
        edit.putInt("settings_auto_start_seconds",auto_start_seconds);
        edit.putInt("settings_auto_read_seconds", auto_read_seconds);
        edit.putString("settings_filter", filter);
        edit.putString("settings_gain", gain);
        edit.putString("settings_actualgain", actualGain);
        edit.putString("settings_auto_save", auto_save);
        edit.putString("settings_doctor_name", doctor_name);
        edit.putString("settings_doctor_address", doctor_address);
        edit.putString("settings_doctor_contactno", doctor_contactno);
        edit.putString("settings_doctor_website", doctor_website);
        edit.putString("settings_doctor_email", doctor_email);
        edit.putString("saved_device_address", saved_address);
        edit.putString("saved_device_name", saved_device_name);
        edit.putString("settings_language", language);
        edit.putString("settings_log", log);
        edit.putString("settings_language_code", language_code);
        edit.putString("settings_reports", boolArrToCsv(reports));
        edit.apply();
    }

    public static String boolArrToCsv(boolean[] b){
        String[] s = new String[b.length];
        for (int i = 0; i < b.length; i++) {
            s[i] = Boolean.toString(b[i]);
        }
        return TextUtils.join(",", s);
    }
    public static boolean[] csvToBoolArr(String s){
        String[] sa = s.split(",");
        boolean[] b = new boolean[sa.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = Boolean.valueOf(sa[i]);
        }
        return b;
    }
    public static void setDataBackupDialogShown(){
        SharedPreferences.Editor edit = appbt.pref.edit();
        edit.putBoolean("isDataBackupDialogShown", false);
        edit.apply();
    }
    public static boolean shouldShowDataBackupDialog(){
        return appbt.pref.getBoolean("isDataBackupDialogShown", true);
    }

}
