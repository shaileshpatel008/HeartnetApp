package in.dibc.kavitul.drcardio;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.dibc.kavitul.bluetoothmodule.BluetoothPermissions;
import in.dibc.kavitul.drcardio.Model.ECGDataModel;
import in.dibc.kavitul.drcardio.Model.ecgData;
import in.dibc.kavitul.drcardio.SupportClass.DataHandlerThread;
import in.dibc.kavitul.drcardio.SupportClass.ECGAnalysis;
import in.dibc.kavitul.drcardio.SupportClass.PdfGenerator;
import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.ecgFilter;
import in.dibc.kavitul.drcardio.SupportClass.mFile;
import in.dibc.kavitul.drcardio.SupportClass.settings;
import in.dibc.kavitul.drcardio.utils.GPSTracker;
import in.dibc.kavitul.drcardio.utils.PathUtil;
import in.dibc.kavitul.drcardio.utils.PrefHelper;
import in.dibc.kavitul.drcardio.widgets.VolleyMultipartRequest;

public class NewEcgActivity extends AppCompatActivity implements View.OnClickListener {
    String TAG = "NewECGActivity";
    ImageButton new_ecg_start_stop;
    LinearLayout fab_generate_report;
    TextView timer;
    LinearLayout graph_container;
    Toolbar toolbar;

    Handler mHandler;
    SeekBar seekbar;
    Boolean[] lead_status = new Boolean[10];

    int min_saved_seconds = 20;
    static int PATIENT_FORM_REQUEST_CODE = 10001;
    static boolean openPatientFormToGenerateReport = false;
    // TODO: 24-10-2020 : Maybe static isn't necessary
    static boolean[] checkedItems = new boolean[12];
    private int report_x_position = 0;
    CountDownTimer autoStopTimer, autoStartTimer;
    //    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
//        1,
//        12,
//        5,
//        TimeUnit.SECONDS,
//        new DataHandlerThread()
//    );
    private final DataHandlerThread dataHandlerThread = new DataHandlerThread();
    private LinearLayout ll_loading;
    // TODO: 2/2/21 try to use threadpool or thread factory for multithreading

    String upload_URL = "https://drcadio.soluspottechnolabs.com/api/upload-pdf"; //https://rainbowline.soluspottechnolabs.com/api/uploadCardioPdf";
    RequestQueue rQueue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_new_ecg);
        init();
        setSupportActionBar(toolbar);
        setUiHandler();
        setDataHandlerThread();
        ecgData.resetEcgData();
        checkFromLoadData();
        ecgFilter.setFilter(settings.filter);
        if (settings.test_mode.equals("true")) {
            showTestModePopup();
        }

        // check if GPS enabled
        GPSTracker gpsTracker = new GPSTracker(this);
        if (gpsTracker.getIsGPSTrackingEnabled())
        {
            PathUtil.userLatitude = gpsTracker.getLatitude();
            PathUtil.userLongitude = gpsTracker.getLongitude();
            PathUtil.userAddress = gpsTracker.getAddressLine(NewEcgActivity.this);
            Log.v("Latitude :::: ", ""+PathUtil.userLatitude);
            Log.v("Longitude :::: ", ""+PathUtil.userLongitude);
            Log.v("User Address :::: ", ""+PathUtil.userAddress);
        }
        else
        {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gpsTracker.showSettingsAlert();
        }
    }

    private void showTestModePopup() {
        new AlertDialog.Builder(NewEcgActivity.this)
                .setTitle(R.string.alert_message_test_mode_title)
                .setMessage(R.string.alert_message_test_mode)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .create()
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int delay = (getIntent().getExtras() != null) ? 5000 : 1000;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SubMenu sub_menu_filter, sub_menu_gain;
                boolean itemSelected = false;
                if (appbt.pref.getString("settings_test_mode", "").equals("false")) {
                    sub_menu_filter = menu.addSubMenu(1, 0, 0, appbt.res.getString(R.string.filter));
                    sub_menu_filter.clearHeader();
                    //SubMenu Group ID : Filter = 0, Gain = 1, Patient Data = 2
                    for (int i = 0; i < settings.filter_values.size(); i++) {
                        MenuItem subMenu = sub_menu_filter.add(1, i, i, settings.filter_values.get(i));
                        if (settings.filter.equals(settings.filter_actual_values.get(i))) {
                            subMenu.setChecked(true);
                        }
                    }
                    sub_menu_filter.setGroupCheckable(1, true, true);
                }

                sub_menu_gain = menu.addSubMenu(9, 0, 1, appbt.res.getString(R.string.gain));
                sub_menu_gain.clearHeader();
                for (int i = 0; i < settings.gain_values.size(); i++) {
                    MenuItem subMenu = sub_menu_gain.add(2, i, i, settings.gain_values.get(i));
                    if (settings.gain.equals(settings.gain_values.get(i))) {
                        subMenu.setChecked(true);
                    }
                }
                sub_menu_gain.setGroupCheckable(2, true, true);

                Menu menu_patient_data = menu.addSubMenu(3, 0, 2, "Patient Data");
                //Menu menu_graphs = menu.addSubMenu(4, 0, 3, "Graphs To Display");

            }
        }, 500);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: "+item.getTitle().toString());
        switch (item.getGroupId()) {
            case 0:
                if (item.getTitle().toString().equals("ECG Trend")) {
                    onBackPressed();
                }
                break;
            case 1:
                stopReading();
                if (!item.getTitle().toString().equals("Filter")) {
                    Log.d(TAG, "onOptionsItemSelected: " + item.getTitle().toString());
                    String selected_filter = item.getTitle().toString();
                    int filterIndex = settings.filter_values.indexOf(selected_filter);
                    filterIndex = filterIndex < 0 ? 0 : filterIndex;
                    settings.filter = settings.filter_actual_values.get(filterIndex);
                    ecgFilter.setFilter(settings.filter);
                    item.setChecked(true);
                    filterData();
                }
                break;
            case 2:
                stopReading();
                if(!item.getTitle().toString().equals("Gain")) {
                    setGain(item.getTitle().toString());
                    item.setChecked(true);
                    loopThroughFilteredData();
                }
                break;
            case 3:
                openPatientFormToGenerateReport = false;
                openPatientForm();
                break;
            case 4:
                openGraphAlertDialogBox();
                break;

            default:
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDisclaimerMessage() {
        Toast.makeText(getApplicationContext(), R.string.graph_display_disclaimer, Toast.LENGTH_LONG).show();
    }

    public void setDataHandlerThread() {
        dataHandlerThread.start();
    }

    private void openGraphAlertDialogBox() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(NewEcgActivity.this);
        alertDialog.setTitle("Select which graphs to display");
        alertDialog.setMultiChoiceItems(ecgData.lead_name, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                checkedItems[which]=isChecked;
            }
        })
        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                showHideGraphs();
            }
        })
        .setNegativeButton(android.R.string.no, null);
        AlertDialog alert = alertDialog.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    public void showHideGraphs(){
        int sum = 0;
        for(int i = 0; i<ecgData.NO_OF_CHANNELS; i++){
            if(checkedItems[i]){
                sum++;
                ecgData.chart[i].setVisibility(View.VISIBLE);
            }else{
                ecgData.chart[i].setVisibility(View.GONE);
            }
        }
        graph_container.setWeightSum(sum);
    }

    public void checkFromLoadData(){
        Bundle extras = getIntent().getExtras();
        //short[][] raw_data_from_file = new short[sampleCount][ecgData.NO_OF_CHANNELS];
        if(extras != null) {
            ll_loading.setVisibility(View.VISIBLE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //load data from file
                    ecgData.date_time = extras.getString("file_name", "");
                    ecgData.parseInfo();
                    showPlayPauseButton(false);
                    showGenerateReportButton(true);
                }
            }, 500);

        }else{
            //New ECG Activity
            setGain(settings.gain);

            showPlayPauseButton(true);
            showGenerateReportButton(false);
            ecgData.resetEcgData();
        }
    }

    public void showPlayPauseButton(Boolean show){
        if(show){
            new_ecg_start_stop.setVisibility(View.VISIBLE);
        }else{
            new_ecg_start_stop.setVisibility(View.GONE);
        }
    }

    public void showGenerateReportButton(Boolean show){
        if(show){
            fab_generate_report.setVisibility(View.VISIBLE);
        }else{
            fab_generate_report.setVisibility(View.GONE);
        }
    }

    public void init() {
        if(appbt.btHelper == null){
            finish();
        }
        appbt.btHelper.sourceActivity = this;
        Arrays.fill(checkedItems, true);
        loadUIElements();
    }

    private void loadUIElements() {
        graph_container = findViewById(R.id.graph_container);
        timer = findViewById(R.id.timer);
        seekbar = findViewById(R.id.seekbar);
        new_ecg_start_stop = findViewById(R.id.new_ecg_start_stop);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            new_ecg_start_stop.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        }
        new_ecg_start_stop.setOnClickListener(this);
        fab_generate_report = findViewById(R.id.fab_generate_report);
        fab_generate_report.setOnClickListener(this);
        ll_loading = findViewById(R.id.loading);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        graph_container.setWeightSum(12);
        seekbar.setVisibility(View.GONE);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setChartXPosition(progress);
//                progress *= ecgData.downSamplingRate;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        initLineCharts();
    }
    private void notifyPreparingToRead(){
        autoStartTimer = new CountDownTimer(ecgData.sampleDataSec * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                String val = String.valueOf(millisUntilFinished / 1000);
                timer.setText("Calibrating. Please wait.. " + val);
            }

            public void onFinish() {
                timer.setText("");
                setAutoStop(settings.max_read_seconds);
            }
        };
        autoStartTimer.start();
    }

    private String getDurationString(long seconds) {

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        return /*twoDigitString(hours) + " : " + */twoDigitString(minutes) + " : " + twoDigitString(seconds);
    }

    private String twoDigitString(long number) {

        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }

    private void setAutoStop(int sec) {
        autoStopTimer = new CountDownTimer((sec + 1) * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                long secUntilFinished = millisUntilFinished / 1000;
                String val = String.valueOf(secUntilFinished);
                if (min_saved_seconds <= ecgData.rawDataCount / ecgData.sampleRatePerSec) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        new_ecg_start_stop.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                    }
                }
//                timer.setText("Stop in " + val);
                //timer.setText(String.valueOf(sec - secUntilFinished));
                timer.setText(""+getDurationString(sec - secUntilFinished));

            }

            public void onFinish() {
                stopReading();
            }
        };
        autoStopTimer.start();
    }

    private void setChartXPosition(int x_position) {
        report_x_position = x_position*ecgData.downSamplingRate;
        for(int i=0; i<ecgData.NO_OF_CHANNELS; i++){
            ecgData.chart[i].moveViewToX(x_position);
        }
    }

    private void setGain(String gain) {
        if (gain == null) {
            gain = settings.gain_values.get(0);
        }
        settings.gain = gain;
        int gainIndex = settings.gain_values.indexOf(gain);
        gainIndex = gainIndex < 0 ? 0 : gainIndex;
        settings.actualGain = settings.gain_actual_values.get(gainIndex);
        ecgData.graph_scale = Float.parseFloat(gain);
        int gain_cmd = appbt.btHelper.res.getInteger(appbt.btHelper.res.getIdentifier("CMD_GAIN_" + settings.actualGain, "integer", appbt.packageName));
        sendCommand(gain_cmd);
    }

    /* init all 12 charts to the ecgData.chart[] array identified by id sequentially from 1-12 with prefix line_chart_ */
    private void initLineCharts(){
        for(int ch=0; ch<ecgData.NO_OF_CHANNELS; ch++){
            int id = appbt.res.getIdentifier("line_chart_" + ch, "id", appbt.packageName);
            ecgData.chart[ch] = findViewById(id);
            ecgData.chart[ch].setBackgroundColor(Color.BLACK);
            LineData data = new LineData();
            data.setValueTextColor(Color.WHITE);
            ecgData.chart[ch].setViewPortOffsets(0f, 0f, 0f, 0f);
            //ecgData.chart[i].setScaleX(1f);
            //ecgData.chart[i].setScaleMinima(2f,1f);
            // add empty data
            ecgData.chart[ch].setData(data);
            ecgData.chart[ch].setTouchEnabled(false);
//            ecgData.chart[i].setDragEnabled(false);
            ecgData.chart[ch].setAutoScaleMinMaxEnabled(false);
//            ecgData.chart[i].setScaleYEnabled(false);
            ecgData.chart[ch].getDescription().setText(ecgData.lead_name[ch]);
            ecgData.chart[ch].getDescription().setTextColor(Color.GREEN);
            ecgData.chart[ch].getDescription().setTextSize(appbt.res.getDimension(R.dimen._14sdp));
            ecgData.chart[ch].getDescription().setPosition(15f, 75f);
            ecgData.chart[ch].getDescription().setTextAlign(Paint.Align.LEFT);
            ecgData.chart[ch].setLogEnabled(false); // SP 888888888
            modifyLineChartsUI(ecgData.chart[ch]);
        }
    }

    private void modifyLineChartsUI(LineChart chart){
        // get the legend DataSet(only possible after setting data)
        Legend l = chart.getLegend();

        // modify the legend ...
        l.setEnabled(false);
        l.setForm(Legend.LegendForm.LINE);
//        l.setTypeface(mTfLight);
        l.setTextColor(Color.YELLOW);

        XAxis xa = chart.getXAxis();
//        xa.setTypeface(mTfLight);
//        xa.setGranularityEnabled(true);
//        xa.setGranularity(1f);
        xa.setTextColor(Color.YELLOW);
        xa.setDrawGridLines(false);
        xa.setDrawLabels(false);
        xa.setEnabled(true);
//        xa.setCenterAxisLabels(true);

//        xa.setAxisMaximum(2500f);
//        xa.setAxisMinimum(1f);
        YAxis leftAxis = chart.getAxisLeft();
//        leftAxis.setTypeface(mTfLight);
        leftAxis.setTextColor(Color.YELLOW);
        leftAxis.setAxisMaximum((float) (1.5 * settings.val_per_mv));
        leftAxis.setAxisMinimum((float) (-1.5 * settings.val_per_mv));
        leftAxis.setDrawLabels(false);
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawZeroLine(true);
        leftAxis.setEnabled(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }


    @Override
    public void onBackPressed() {
        if (ecgData.isReading) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(NewEcgActivity.this);
            alertDialog.setTitle("Are you sure you want to stop reading and return to main screen?");
            alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    stopReading();
                    finish();
                }
            })
                    .setNegativeButton(android.R.string.no, null);
            AlertDialog alert = alertDialog.create();
            alert.setCanceledOnTouchOutside(false);
            alert.show();
        } else {
            finish();
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.new_ecg_start_stop:
                if (ecgData.isReading) {
                    stopReading();
                } else {
                    startReading();
                    CancelAutoStartStop();
                    notifyPreparingToRead();
                }

                break;

            case R.id.fab_generate_report: // on generate report button click

//                new AlertDialog.Builder(this)
//                        .setTitle("Generate report?")
//                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                if (
//                                        ecgData.patient_name != null && !ecgData.patient_name.equals("") &&
//                                                ecgData.patient_name != null && !ecgData.patient_name.equals("")
//                                ) {
//                                    ll_loading.setVisibility(View.VISIBLE);
//                                    new Handler().postDelayed(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            generateReport();
//                                        }
//                                    }, 500);
//                                } else {
//                                    openPatientFormToGenerateReport = true;
//                                    openPatientForm();
//                                }
//                            }
//                        }).setNegativeButton(R.string.cancel, null)
//                        .create()
//                        .show();

                if (
                        ecgData.patient_name != null && !ecgData.patient_name.equals("") &&
                                ecgData.patient_name != null && !ecgData.patient_name.equals("")
                ) {
                    ll_loading.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            generateReport();
                        }
                    }, 500);
                } else {
                    openPatientFormToGenerateReport = true;
                    openPatientForm();
                }

                break;
        }

    }


    private void startReading() {
        BluetoothPermissions btPerm = new BluetoothPermissions(appbt.btHelper);
        if (!btPerm.check()) {
            return;
        }
        if (appbt.conn == null || !appbt.conn.isAlive()) {
            Toast.makeText(getApplicationContext(), "Error communicating to the device. Please go back and connect again.", Toast.LENGTH_LONG).show();
//            finish(); // SP 88888888
//            appbt.btHelper.connectedThread.disconnect(); // SP 88888888
            return;
        }
        hideSeekBar();
        showGenerateReportButton(false);

        ll_loading.setVisibility(View.VISIBLE);

        //Resets any previous data in chart
        ecgData.date_time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SS").format(new Date());
        resetChartData();
        clearCharts();
        initLineCharts();
        ecgData.initDataWithLength(false);

        new Handler().postDelayed(() -> {
            ll_loading.setVisibility(View.GONE);
            ecgData.isReading = true;
            new_ecg_start_stop.setImageResource(R.drawable.icon_stop);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                new_ecg_start_stop.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
            }
            if (settings.test_mode.equals("true")) {
                sendCommand(appbt.btHelper.CMD_TEST_START);
            } else {
                sendCommand(appbt.btHelper.CMD_START);
            }
            showDisclaimerMessage();
        }, 500);

    }


    private void stopReading() {
        CancelAutoStartStop();
        new_ecg_start_stop.setImageResource(R.drawable.icon_start);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            new_ecg_start_stop.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        }
        sendCommand(appbt.btHelper.CMD_STOP);
        ecgData.isReading = false;
        if (min_saved_seconds <= ecgData.rawDataCount / ecgData.sampleRatePerSec) {
                showGenerateReportButton(true);
                showSeekBar();
                Toast.makeText(getApplicationContext(), "Select clean data for Report", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Need to acquire for at least " + min_saved_seconds + " seconds!", Toast.LENGTH_SHORT).show();
        }

    }

    private void enableChartInteraction(int ch, boolean enable) {
//        ecgData.chart[ch].setTouchEnabled(enable);
//        ecgData.chart[ch].setPinchZoom(enable);
//        ecgData.chart[ch].setDragEnabled(enable);
    }

    private void CancelAutoStartStop(){
        if(autoStopTimer != null) {
            autoStopTimer.cancel();
        }
        if(autoStartTimer != null){
            autoStartTimer.cancel();
        }
        timer.setText("");
    }
    private int getVisibleChannels(){
        int visibleChannels = 0;
        for(int i = 0; i<ecgData.NO_OF_CHANNELS; i++){
            if(checkedItems[i]) {
                visibleChannels++;
            }
        }
        return visibleChannels;
    }
    private void showSeekBar(){
        graph_container.setWeightSum(getVisibleChannels() + 0.25f);


        int seekBarMax = ecgData.displayDataCount - ecgData.display_data_range_x;
//        int seekBarStep = ecgData.sampleRatePerSec/ecgData.downSamplingRate;
        if (seekBarMax > 1) {
            seekbar.setMax(seekBarMax);
        } else {
            seekbar.setMax(1);
        }
//        seekbar.incrementProgressBy(seekBarStep);
        int seekbarPos = seekBarMax-2*ecgData.sampleRatePerSec / ecgData.downSamplingRate;
        if(seekbarPos < 0){ seekbarPos = 0; }
        seekbar.setProgress(seekbarPos);
        seekbar.setVisibility(View.VISIBLE);


        Log.d(TAG, "debugReportRange: rawLen:" + ecgData.raw_data.length);
        Log.d(TAG, "debugReportRange: rawCount:" + ecgData.rawDataCount);
        Log.d(TAG, "debugReportRange: FiltLen:" + ecgData.filtered_data.length);
        Log.d(TAG, "debugReportRange: dispCount:" + ecgData.displayDataCount);
        Log.d(TAG, "debugReportRange: disRange:" + ecgData.display_data_range_x);
        Log.d(TAG, "debugReportRange: seekMax:" + seekBarMax);
    }

    private void hideSeekBar() {
        graph_container.setWeightSum(getVisibleChannels());
        seekbar.setVisibility(View.GONE);
    }

    private void resetChartData() {
        for (int ch = 0; ch < ecgData.NO_OF_CHANNELS; ch++) {
            ecgData.chart[ch].clearValues();
            enableChartInteraction(ch, false);
        }
    }

    private void clearCharts() {
        for (int ch = 0; ch < ecgData.NO_OF_CHANNELS; ch++) {
            ecgData.chart[ch].clear();
        }
    }

    private void generateReport() {
        final Boolean[] isGenerated = {false};
        int pos = report_x_position;
        if (ecgData.rawDataCount - report_x_position < 6500) {
            pos = ecgData.rawDataCount - 6500;
        }
        final int posF = pos;

        ECGAnalysis ecgAnalysis = new ECGAnalysis(ecgData.filtered_data, pos, Integer.parseInt(settings.actualGain));
        if (!settings.test_mode.equals("true")) {
            new Handler().postDelayed(new Runnable() {
                int analysisError = 0;

                @Override
                public void run() {
                    try {
                        analysisError = ecgAnalysis.analyse();
                    } catch (Exception e) {
                        e.printStackTrace();
                        FirebaseCrashlytics.getInstance().recordException(e);
                        int analysisError = 3;
                    }
                    switch (analysisError) {
                        case 0:

                            break;
                        case 1:
                            Toast.makeText(NewEcgActivity.this, "Data not ready for ECG analysis", Toast.LENGTH_LONG).show();
                            break;
                        case 2:
                            Toast.makeText(NewEcgActivity.this, "Unable to detect peaks for ECG Analysis", Toast.LENGTH_LONG).show();
                            break;
                        case 3:
                            Toast.makeText(NewEcgActivity.this, "Unknown error in ECG Analysis", Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            }, 500);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    PdfGenerator pdf = new PdfGenerator(posF, ecgAnalysis.analysisArray);
                    isGenerated[0] = pdf.generatePDF(NewEcgActivity.this);

                    ll_loading.setVisibility(View.GONE);
                    if (isGenerated[0]) {
                        displayAlertToView();

                        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(NewEcgActivity.this);
                        Bundle params2 = new Bundle();
                        params2.putString(FirebaseAnalytics.Param.VALUE, "Report Generated from : "+ecgData.patinet_deviceid+" Device");
                        mFirebaseAnalytics.logEvent("ECG_Report_Generated", params2);

//                        String appendFileName = ecgData.patient_name.length() < 12 ? ecgData.patient_name : ecgData.patient_name.substring(0, 11);
//                        String filepath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + appendFileName + "_" + ecgData.date_time + ".pdf";
//                        File file = new File(filepath);//here schedule1.pdf is the pdf file name which is keep in assets folder.
//
//                        //String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + appendFileName + "_" + ecgData.date_time + ".pdf";
//                        Intent intent = new Intent(NewEcgActivity.this, PDFViewerActivity.class);
//                        intent.putExtra("PDFPath", filepath);
//                        intent.putExtra("FileName", appendFileName + "_" + ecgData.date_time + ".pdf");
//                        startActivity(intent);
//                        finish();

                        // File UPLOAD
                        String appendFileName = ecgData.patient_name.length() < 12 ? ecgData.patient_name : ecgData.patient_name.substring(0, 11);
                        String filepath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + appendFileName + "_" + ecgData.date_time + ".pdf";
                        File file = new File(filepath);
                        Log.v("Upload PDF File Name : ", appendFileName + "_" + ecgData.date_time + ".pdf");
                        Log.v("Upload Path :: ", filepath);

                        if(PathUtil.INTERNET_STATUS) {
                            uploadPDF(appendFileName + "_" + ecgData.date_time + ".pdf", Uri.fromFile(file), ecgAnalysis.analysisArray);
                        } else {

                            try {
                                JSONObject jDeviceInfo = new JSONObject();
                                try {
//                        jDeviceInfo.put("OS", System.getProperty("os.version"));
                                    jDeviceInfo.put("API Level", android.os.Build.VERSION.SDK_INT);
                                    jDeviceInfo.put("Device", android.os.Build.DEVICE);
                                    jDeviceInfo.put("Model", android.os.Build.MODEL );
                                    jDeviceInfo.put("FullDeviceInfo", Build.BRAND+" "+android.os.Build.MODEL);
                                    Log.v("Device Informations :: ", jDeviceInfo.toString());
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }

                                JSONObject jPatientInfo = new JSONObject();
                                try {
                                    jPatientInfo.put("Id", ecgData.patient_id);
                                    jPatientInfo.put("Name", ecgData.patient_name);
                                    jPatientInfo.put("Age", ecgData.patient_age);
                                    jPatientInfo.put("Comments", ecgData.patient_comments);
                                    jPatientInfo.put("Gender", ecgData.patient_sex);
                                    jPatientInfo.put("DeviceID", ecgData.patinet_deviceid);
                                    Log.v("Patient Informations :: ", jPatientInfo.toString());
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
//                                Map<String, String> params = new HashMap<>();
//                                params.put("device_id", ecgData.patinet_deviceid);
//                                params.put("latitude", ""+PathUtil.userLatitude);
//                                params.put("longitude", ""+PathUtil.userLongitude);
//                                params.put("device_info", jDeviceInfo.toString());
//                                params.put("patient_info", jDeviceInfo.toString());
//                                params.put("address", ""+PathUtil.userAddress);

                                float[][] analysis = ecgAnalysis.analysisArray;
                                DecimalFormat df = new DecimalFormat();
                                df.setMaximumFractionDigits(2);

                                int rr = 0;
                                if((int) analysis[1][0] > 0){
                                    rr = Math.round((60000 / analysis[1][0]));
                                }

                                ArrayList<ECGDataModel> listECGDataModel = new ArrayList<>();
                                if(PrefHelper.getECGDataListResponse("ECGDataList") != null) {
                                    listECGDataModel = PrefHelper.getECGDataListResponse("ECGDataList");
                                    ECGDataModel ecgDataModel = new ECGDataModel();
                                    ecgDataModel.setAddress(""+PathUtil.userAddress);
                                    ecgDataModel.setDevice_id(ecgData.patinet_deviceid);
                                    ecgDataModel.setDevice_info(jDeviceInfo.toString());
                                    ecgDataModel.setLatitude(""+PathUtil.userLatitude);
                                    ecgDataModel.setLongitude(""+PathUtil.userLongitude);
                                    ecgDataModel.setPatient_info(jDeviceInfo.toString());
                                    ecgDataModel.setPdf_name(appendFileName + "_" + ecgData.date_time + ".pdf");
                                    ecgDataModel.setPdf_file_path(filepath);
                                    ecgDataModel.setHr((int) analysis[1][0] +"");
                                    ecgDataModel.setR(df.format(analysis[1][1]) +"");
                                    ecgDataModel.setRr(rr +"");
                                    ecgDataModel.setPr(Math.round(analysis[1][7]*1000) +"");
                                    ecgDataModel.setQrs(Math.round(analysis[1][6]*1000) +"");
                                    ecgDataModel.setQt(Math.round(analysis[1][6]*1000) +"");
                                    ecgDataModel.setQtc(Math.round((analysis[1][8]/analysis[1][11])*1000) +"");
                                    ecgDataModel.setQt_by_qtc(df.format(analysis[1][11])+"");
                                    listECGDataModel.add(ecgDataModel);
                                    PrefHelper.setECGDataListResponse("ECGDataList", listECGDataModel);
                                } else {
                                    ECGDataModel ecgDataModel = new ECGDataModel();
                                    ecgDataModel.setAddress(""+PathUtil.userAddress);
                                    ecgDataModel.setDevice_id(ecgData.patinet_deviceid);
                                    ecgDataModel.setDevice_info(jDeviceInfo.toString());
                                    ecgDataModel.setLatitude(""+PathUtil.userLatitude);
                                    ecgDataModel.setLongitude(""+PathUtil.userLongitude);
                                    ecgDataModel.setPatient_info(jDeviceInfo.toString());
                                    ecgDataModel.setPdf_name(appendFileName + "_" + ecgData.date_time + ".pdf");
                                    ecgDataModel.setPdf_file_path(filepath);
                                    ecgDataModel.setHr((int) analysis[1][0] +"");
                                    ecgDataModel.setR(df.format(analysis[1][1]) +"");
                                    ecgDataModel.setRr(rr +"");
                                    ecgDataModel.setPr(Math.round(analysis[1][7]*1000) +"");
                                    ecgDataModel.setQrs(Math.round(analysis[1][6]*1000) +"");
                                    ecgDataModel.setQt(Math.round(analysis[1][6]*1000) +"");
                                    ecgDataModel.setQtc(Math.round((analysis[1][8]/analysis[1][11])*1000) +"");
                                    ecgDataModel.setQt_by_qtc(df.format(analysis[1][11])+"");
                                    listECGDataModel.add(ecgDataModel);
                                    PrefHelper.setECGDataListResponse("ECGDataList", listECGDataModel);
                                }


                                openGeneratedPDF();

//                                Intent intent = new Intent(NewEcgActivity.this, MainActivity.class);
//                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//                                startActivity(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        new AlertDialog.Builder(NewEcgActivity.this)
                                .setTitle("Unable to generate report")
                                //.setMessage(R.string.alert_message_change_device)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                    }
                                })
                                .create()
                                .show();
                    }
                }
            }, 1500);
        }


        new Thread(() -> mFile.saveDatFile()).run();
        new Thread(() -> {
            if(settings.csv) {
                mFile.saveDataCsvFile(posF, getApplicationContext());
            }
        }).run();

    }


    private void displayAlertToView() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_generated_pdf)
                //.setMessage(R.string.alert_message_change_device)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        openGeneratedPDF();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .create()
                .show();
    }

    private void openGeneratedPDF() {
        String appendFileName = ecgData.patient_name.length() < 12 ? ecgData.patient_name : ecgData.patient_name.substring(0, 11);
        String filepath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + appendFileName + "_" + ecgData.date_time + ".pdf";
        File file = new File(filepath);//here schedule1.pdf is the pdf file name which is keep in assets folder.

        //#Bhavesh Change
        /*String fileName = appendFileName + "_" + ecgData.date_time + ".pdf";
        File pdfFile = new File(mFile.getDirFile(this,mFile.DIR_REPORT), fileName);*/
        String mimeType = "application/pdf";
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = FileProvider.getUriForFile(getBaseContext(), getApplicationContext().getPackageName() + ".provider", file);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open PDF report using.."));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), "Unable to open report. make sure your phone has a PDF viewer installed. Report file: "+ file, Toast.LENGTH_LONG).show();
        }

        //String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + appendFileName + "_" + ecgData.date_time + ".pdf";
//        Intent intent = new Intent(NewEcgActivity.this, PDFViewerActivity.class);
//        intent.putExtra("PDFPath", filepath);
//        intent.putExtra("FileName", appendFileName + "_" + ecgData.date_time + ".pdf");
//        startActivity(intent);
    }

    public void openPatientForm(){
        Intent intent = new Intent(NewEcgActivity.this, PatientData.class);
        startActivityForResult(intent, PATIENT_FORM_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PATIENT_FORM_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                if(openPatientFormToGenerateReport){
                    ll_loading.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> generateReport(), 500);
                }
            }

        }
    }

    private void sendCommand(int command){
        byte[] commandBytes = new byte[1];
        commandBytes[0] = (byte) command;
        sendCommand(commandBytes);
    }
    private void sendCommand(byte[] command){
        if (appbt.conn == null) {
            try {
                appbt.conn = appbt.btHelper.connectionThread.getConnectedThread();
                appbt.conn.btHelper = appbt.btHelper;
                appbt.conn.setPriority(Thread.MAX_PRIORITY);
                //Logger.d(TAG, "Debug", "Login Command Pass Successfully", "Login Command sent");
            } catch (Exception e) {
                e.printStackTrace();
                //Logger.e(TAG, "Error", "Login Command Pass UnSuccessfully", "Login Command Failed to Write Packet");
            }
        }
        try {
            appbt.conn.write(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("HandlerLeak")
    private void setUiHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            int status = bundle.getInt("status");
            short[] channels;
            switch (status){
                case 0:

                    break;
                case 1:
                    Log.d(TAG, "handleMessage: received 1");

                    break;
                case 2:
                    Toast.makeText(getApplicationContext(), "File Version not compatible with the current version of the application", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    ll_loading.setVisibility(View.GONE);
                    showDisclaimerMessage();
                    for (int i = 0; i < ecgData.NO_OF_CHANNELS; i++) {
                        modifyLineChartsUI(ecgData.chart[i]);
                    }
                    loopThroughFilteredData();
                    break;
                case 4: // Stop Reading
                    sendCommand(appbt.btHelper.CMD_STOP);
                    break;
                case 9: // comm error - write failed.
                    Toast.makeText(getApplicationContext(), "Error communicating to device", Toast.LENGTH_LONG).show();
                    finish();
//                    appbt.btHelper.connectedThread.disconnect(); // SP 88888888
                    break;
                case 10:
                    int s = bundle.getInt("sec");
                    timer.setText("s");
                    break;
                default:
                    break;
            }
        }


        };

        appbt.btHelper.mHandlerUpdateUi = mHandler;
    }
    private void filterData(){
        // TODO: 1/20/21 check if sufficient data is available for processing
        resetChartData();
        ecgData.filtered_data = new float[ecgData.rawDataCount][ecgData.NO_OF_CHANNELS];
        for (int ch = 0; ch < ecgData.NO_OF_CHANNELS; ch++) {
            for (int x = 0; x < ecgData.rawDataCount; x++) {
                ecgData.filtered_data[x][ch] = ecgFilter.filter(ch, x, ecgData.dc_corrected_data[x][ch]);
            }
        }
        loopThroughFilteredData();
    }

//    private void scaleData(){
//        resetChartData();
//        ArrayList<Entry>[] ecgValues = new ArrayList[12];
//        for (int ch = 0; ch < ecgData.NO_OF_CHANNELS; ch++) {
//            ecgValues[ch] = new ArrayList<Entry>();
//            ecgData.displayDataCount = 0;
//            for (int x = 0; x < ecgData.rawDataCount; x++) {
//                if(x %ecgData.downSamplingRate == 0) {
//                    float scaledValue = ecgData.graph_scale * ecgData.filtered_data[x][ch];
//                    ecgValues[ch].add(new Entry((float) ecgData.displayDataCount, scaledValue));
//                    ecgData.displayDataCount++;
//                }
//            }
//            setValues(ch, ecgValues[ch]);
//            notifyDataChange(ch);
//            ecgData.chart[ch].setVisibleXRangeMaximum(ecgData.display_data_range_x);
//        }
//    }

    private void loopThroughFilteredData() {
        resetChartData();

        ArrayList<Entry>[] ecgValues = new ArrayList[12];
        for(int chi = 0; chi< ecgData.NO_OF_CHANNELS; chi++){
            int ch = ecgData.lead_arrange[chi];
            ecgValues[ch] = new ArrayList<Entry>();
            ecgData.displayDataCount = 0;
            for (int x = 0; x < ecgData.rawDataCount; x++){
                float hpf_val = ecgFilter.highPassFilter(ch, x, ecgData.filtered_data[x][ch]);
                if (x % ecgData.downSamplingRate == 0) {
                    float scaledValue = ecgData.graph_scale * hpf_val;
                    ecgValues[ch].add(new Entry((float) ecgData.displayDataCount, scaledValue));
                    ecgData.displayDataCount++;
                }
            }
            setValues(chi, ecgValues[ch]);
            notifyDataChange(chi);
            ecgData.chart[chi].setVisibleXRangeMaximum(ecgData.display_data_range_x);
        }
        showSeekBar();
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.YELLOW);
        set.setLineWidth(1f);
        set.setFillAlpha(65);
        set.setFillColor(Color.YELLOW);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        //set.setValueTextSize(9f);
        set.setDrawCircles(false);
        return set;
    }
    private void setValues(int ch, List ecgValues){
        LineData data = ecgData.chart[ch].getData();
        if (data != null) {
            int x = data.getEntryCount();
//            Log.d(TAG, "addEntryPerChannel: "+ch+" "+x+" : "+y);
            LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            set.setValues(ecgValues);
        }
    }
    private void notifyDataChange(int ch){
        LineData data = ecgData.chart[ch].getData();
        data.notifyDataChanged();
        ecgData.chart[ch].notifyDataSetChanged();
    }
    @Override
    protected void onDestroy() {
        for (int i = 0; i < ecgData.NO_OF_CHANNELS; i++) {
            ecgData.chart[i].clear();
        }
        super.onDestroy();
        dataHandlerThread.quit();
    }

    private void uploadPDF(final String pdfname, Uri pdffile, float[][] analysisArray){

        ProgressDialog pd = new ProgressDialog(NewEcgActivity.this);
        pd.setMessage("Please wait.. Syncing with cloud..");
        pd.setCancelable(false);
        pd.show();

        InputStream iStream = null;
        try {

            iStream = getContentResolver().openInputStream(pdffile);
            final byte[] inputData = getBytes(iStream);

            VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(Request.Method.POST, upload_URL,
                    new Response.Listener<NetworkResponse>() {
                        @Override
                        public void onResponse(NetworkResponse response) {
                            Log.d("ressssssoo",new String(response.data));
                            String resultResponse = new String(response.data);
                            rQueue.getCache().clear();
                            try {
                                JSONObject result = new JSONObject(resultResponse);
                                String status = result.getString("status");
//                                String message = result.getString("message");
                                Log.v("Response :: ", result.toString());

                                pd.dismiss();

                                if (status.equals("success")) {

//                                    String appendFileName = ecgData.patient_name.length() < 12 ? ecgData.patient_name : ecgData.patient_name.substring(0, 11);
//                                    String filepath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + appendFileName + "_" + ecgData.date_time + ".pdf";
//
//                                    Intent intent = new Intent(NewEcgActivity.this, PDFViewerActivity.class);
//                                    intent.putExtra("PDFPath", filepath);
//                                    intent.putExtra("FileName", appendFileName + "_" + ecgData.date_time + ".pdf");
//                                    startActivity(intent);
//                                    finish();

                                    Intent intent = new Intent(NewEcgActivity.this, PDFViewerActivity.class);
                                    intent.putExtra("PDFPath", result.getJSONObject("success_data").getString("document_path")/*.replaceAll("", "%20")*/);
                                    intent.putExtra("FileName", result.getJSONObject("success_data").getString("pdf"));
                                    startActivity(intent);
                                    finish();
                                } else {
                                    pd.dismiss();
                                    //Toast.makeText(NewEcgActivity.this, result.getString("error_data").toString(), Toast.LENGTH_SHORT).show();
                                    new AlertDialog.Builder(NewEcgActivity.this)
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setTitle("Upload error")
                                            .setMessage(result.getString("error_data").toString())
                                            .setPositiveButton("Ok", new DialogInterface.OnClickListener()
                                            {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }

                                            })
                                            //.setNegativeButton("No", null)
                                            .show();
                                }
                            } catch (JSONException e) {
                                pd.dismiss();
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            //Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                            pd.dismiss();
                            new AlertDialog.Builder(NewEcgActivity.this)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setTitle("Upload error")
                                    .setMessage(error.getMessage())
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }

                                    })
                                    //.setNegativeButton("No", null)
                                    .show();
                        }
                    }) {

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    String token = PrefHelper.getString("Token", "");
                    Log.v("Token :: ", token.toString());
                    Map<String, String> params = new HashMap<String, String>();
//                    params.put("Content-Type", "multipart/form-data; charset=UTF-8");
                    params.put("Authorization", "Bearer "+token);
                    Log.v("Authorization :: ", params.toString());
                    return params;
                }

                /*
                 * If you want to add more parameters with the image
                 * you can do it here
                 * here we have only one parameter with the image
                 * which is tags
                 * */
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
//                    System.getProperty("os.version"); // OS version
//                    android.os.Build.VERSION.SDK      // API Level
//                    android.os.Build.DEVICE           // Device
//                    android.os.Build.MODEL            // Model
//                    android.os.Build.PRODUCT          // Product

                    JSONObject jDeviceInfo = new JSONObject();
                    try {
//                        jDeviceInfo.put("OS", System.getProperty("os.version"));
                        jDeviceInfo.put("API Level", android.os.Build.VERSION.SDK_INT);
                        jDeviceInfo.put("Device", android.os.Build.DEVICE);
                        jDeviceInfo.put("Model", android.os.Build.MODEL );
                        jDeviceInfo.put("FullDeviceInfo", Build.BRAND+" "+android.os.Build.MODEL);
                        Log.v("Device Informations :: ", jDeviceInfo.toString());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    JSONObject jPatientInfo = new JSONObject();
                    try {
//                        jDeviceInfo.put("OS", System.getProperty("os.version"));
                        jPatientInfo.put("Id", ecgData.patient_id);
                        jPatientInfo.put("Name", ecgData.patient_name);
                        jPatientInfo.put("Age", ecgData.patient_age);
                        jPatientInfo.put("Comments", ecgData.patient_comments);
                        jPatientInfo.put("Gender", ecgData.patient_sex);
                        jPatientInfo.put("DeviceID", ecgData.patinet_deviceid);
//                        Log.v("Patient Informations :: ", jPatientInfo.toString());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    float[][] analysis = analysisArray;
                    DecimalFormat df = new DecimalFormat();
                    df.setMaximumFractionDigits(2);

                    int rr = 0;
                    if((int) analysis[1][0] > 0){
                        rr = Math.round((60000 / analysis[1][0]));
                    }

                    Map<String, String> params = new HashMap<>();
                    params.put("device_id", ecgData.patinet_deviceid);
                    params.put("latitude", ""+PathUtil.userLatitude);
                    params.put("longitude", ""+PathUtil.userLongitude);
                    params.put("device_info", jDeviceInfo.toString());
                    params.put("patient_info", jPatientInfo.toString());
                    params.put("address", ""+PathUtil.userAddress);
                    params.put("hr", (int) analysis[1][0]+"");
                    params.put("r", df.format(analysis[1][1])+"");
                    params.put("rr", rr +"");
                    params.put("pr", Math.round(analysis[1][7]*1000)+"");
                    params.put("qrs", Math.round(analysis[1][6]*1000)+"");
                    params.put("qt", Math.round(analysis[1][8]*1000)+"");
                    params.put("qtc", Math.round((analysis[1][8]/analysis[1][11])*1000)+"");
                    params.put("qt_by_qtc", df.format(analysis[1][11])+"");

                    Log.v("Request :: ", params.toString());
                    return params;
                }

                /*
                 *pass files using below method
                 * */
                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();

                    params.put("document_path", new DataPart(pdfname ,inputData));
                    Log.v("Document Path :: ", pdfname);

                    return params;
                }
            };


            volleyMultipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                    400000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            rQueue = Volley.newRequestQueue(NewEcgActivity.this);
            rQueue.add(volleyMultipartRequest);

        } catch (FileNotFoundException e) {
            pd.dismiss();
            e.printStackTrace();
        } catch (IOException e) {
            pd.dismiss();
            e.printStackTrace();
        }
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}
