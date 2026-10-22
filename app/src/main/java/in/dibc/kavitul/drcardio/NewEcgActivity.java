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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.EditText;
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
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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

import in.dibc.kavitul.drcardio.Model.PatientDetails;
import in.dibc.kavitul.drcardio.SupportClass.ECGFileGenerator;
import in.dibc.kavitul.drcardio.SupportClass.ECGReportGenerator;
import in.dibc.kavitul.drcardio.apis.RetrofitClient;
import in.dibc.kavitul.drcardio.apis.request.AuthRequest;
import in.dibc.kavitul.drcardio.apis.request.DownloadRequest;
import in.dibc.kavitul.drcardio.apis.response.DownloadResponse;
import in.dibc.kavitul.drcardio.apis.response.DownloadResponseData;
import in.dibc.kavitul.drcardio.apis.response.GetScanResponseModel;
import in.dibc.kavitul.drcardio.apis.response.UploadResponse;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
import in.dibc.kavitul.drcardio.apis.ApiService;
import in.dibc.kavitul.drcardio.apis.response.PatientDetailsResponse;
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

    private ApiService apiService;
    String PatientID = "";
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_new_ecg);
        init();
        setSupportActionBar(toolbar);
        setUiHandler();
        setDataHandlerThread();
        ecgData.resetEcgData();
//        checkFromLoadData();
        ecgFilter.setFilter(settings.filter);
        if (settings.test_mode.equals("true")) {
            showTestModePopup();
        }

//        callDownloadReportsAPI("0000000999-11-04-2025-10-13-00");

//        String PatientID = "";
//        if(getIntent() != null) {
//            PatientID = getIntent().getStringExtra("PatientID");
//            Log.v("", "Patient ID :::: "+PatientID);
//            apiService = RetrofitClient.getApiService();
//            getPatientDetails(PatientID);
//        }

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
//        Toast.makeText(getApplicationContext(), R.string.graph_display_disclaimer, Toast.LENGTH_LONG).show();
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
            ecgData.chart[ch].getDescription().setTextSize(appbt.res.getDimension(com.intuit.sdp.R.dimen._14sdp));
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
            alertDialog.setPositiveButton(in.dibc.kavitul.bluetoothmodule.R.string.yes, new DialogInterface.OnClickListener() {
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
//                            generateReport();
//                            String PatientID = "";
                            if(getIntent() != null) {
                                PatientID = getIntent().getStringExtra("PatientID");
//                                Log.v("", "Patient ID :::: "+PatientID);
                                apiService = RetrofitClient.getApiService();
                                getPatientDetails(PatientID);
                            }
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
//        Log.v("ecgData.sample_data :: ", "ecgData.sample_data :: "+ecgData.sample_data[0].length);
//        Log.v("ecgData.raw_data :: ", "ecgData.raw_data :: "+ecgData.raw_data.length);
//        Log.v("ecgData.dc_corrected_data :: ", "ecgData.dc_corrected_data :: "+ecgData.dc_corrected_data.length);
//        Log.v("ecgData.filtered_data :: ", "ecgData.filtered_data :: "+ecgData.filtered_data.length);
//        String jsonData = convertDataDataToJson(ecgData.sample_data, "sample_data");
//        Log.d("ECG_JSON sample_data", jsonData);
//        String jsonData1 = convertDataDataToJson(ecgData.raw_data, "raw_data");
//        Log.d("ECG_JSON raw_data", jsonData1);
//        String jsonData2 = convertDataDataToJson(ecgData.dc_corrected_data, "dc_corrected_data");
//        Log.d("ECG_JSON dc_corrected_data", jsonData2);
//        String jsonData4 = convertDataDataToJson(ecgData.filtered_data, "filtered_data");
//        Log.d("ECG_JSON filtered_data", jsonData4);
        if (min_saved_seconds <= ecgData.rawDataCount / ecgData.sampleRatePerSec) {
            showGenerateReportButton(true);
            showSeekBar();
            Toast.makeText(getApplicationContext(), "Select clean data for Report", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Need to acquire for at least " + min_saved_seconds + " seconds!", Toast.LENGTH_SHORT).show();
        }

    }

    public static String convertDataDataToJson(float[][] ecgData, String keyName) {
        try {
            JSONArray leadsArray = new JSONArray();

            for (float[] lead : ecgData) {
                JSONArray leadArray = new JSONArray();
                for (float sample : lead) {
                    leadArray.put(sample);
                }
                leadsArray.put(leadArray);
            }

            JSONObject json = new JSONObject();
            json.put(keyName, leadsArray);

            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
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

    public void uploadECGFiles(String uploadFilePath) {
        // Create .tmp file
//        File tmpFile = PathUtil.createTempFile(this);
//
//        String tmpFilePath = "";
//        if (tmpFile != null) {
//            tmpFilePath = tmpFile.getAbsolutePath();
//            Log.d("FilePath", "TMP File Path: " + tmpFilePath);
//        } else {
//            Log.e("FilePath", "Failed to create TMP file");
//        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");
        progressDialog.setCancelable(false); // Prevents dismissal on touch outside
        progressDialog.show();

        // Initialize Retrofit
        ApiService apiService = RetrofitClient.getApiService();

        // File paths (replace with actual file paths)
        File ecgFile = new File(/*"/storage/emulated/0/Download/sample.ecg"*/uploadFilePath);
        File tmpFile = new File(/*"/storage/emulated/0/Download/sample.ecg"*/uploadFilePath.replaceAll(".ecg", ".tmp"));
//        File tmpFileNew = new File(tmpFilePath);

        if (!ecgFile.exists()) Log.e("UPLOAD_ERROR", "ECG file not found!");
        if (!tmpFile.exists()) Log.e("UPLOAD_ERROR", "TMP file not found!");

        // Convert Files to RequestBody
        RequestBody requestFile1 = RequestBody.create(MediaType.parse("application/octet-stream"), ecgFile);
        RequestBody requestFile2 = RequestBody.create(MediaType.parse("application/octet-stream"), tmpFile);

        // Wrap into MultipartBody.Part
        MultipartBody.Part body1 = MultipartBody.Part.createFormData("files", ecgFile.getName(), requestFile1);
        MultipartBody.Part body2 = MultipartBody.Part.createFormData("files", tmpFile.getName(), requestFile2);

        // Add both parts to a list
        List<MultipartBody.Part> fileParts = new ArrayList<>();
        fileParts.add(body1);
        fileParts.add(body2);

        // Log Request Data
        Log.d("UploadAPI", "Uploading Files ECG : " + ecgFile.getName() + ", " + ecgFile.getName());
        Log.d("UploadAPI", "Uploading Files TMP : " + ecgFile.getName() + ", " + tmpFile.getName());
        Log.v("Token ::", "Token ::"+PathUtil.TOKEN);

        // Make API Call
        Call<UploadResponse> call = apiService.uploadECGFiles(PathUtil.TOKEN,/*body1, body2*/fileParts);
        call.enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                progressDialog.dismiss();

                if (response.code() == 401) {
                    handleTokenExpired();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    Log.d("UploadAPI", "Success: " + response.body().toString());
//                    callDownloadReportsAPI(extractScanID(ecgFile.getName()));
//                    Toast.makeText(NewEcgActivity.this, "Report uploaded successfully", Toast.LENGTH_LONG);
                    showAndRedirect("Report uploaded successfully!", 3000);
                } else {
                    // Log error response body
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e("UploadAPI", "Failed: " + errorBody);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("UploadAPI", "Error parsing errorBody");
                    }
                }
            }

            @Override
            public void onFailure(Call<UploadResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("UploadAPI", "API Call Failed: " + t.getMessage());

                ArrayList<String> listECGDataModel = new ArrayList<>();
                if(PrefHelper.getECGDataListResponse("ECGDataList") != null) {
                    listECGDataModel = PrefHelper.getECGDataListResponse("ECGDataList");
                    listECGDataModel.add(uploadFilePath);
                    PrefHelper.setECGDataListResponse("ECGDataList", listECGDataModel);
                } else {
                    listECGDataModel.add(uploadFilePath);
                    PrefHelper.setECGDataListResponse("ECGDataList", listECGDataModel);
                }
            }
        });
    }

    public void showAndRedirect(String message, int duration) {
        // Create a TextView for the message
        TextView textView = new TextView(NewEcgActivity.this);
        textView.setText(message);
        textView.setTextSize(18);
        textView.setTextColor(Color.BLACK);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(40, 40, 40, 40);

        // Create the AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(NewEcgActivity.this)
                .setView(textView) // Set the TextView as the dialog view
                .setCancelable(false) // Prevent manual dismissal
                .create();

        // Show the dialog
        dialog.show();

        // Auto dismiss after the given duration and redirect
        new Handler().postDelayed(() -> {
            dialog.dismiss(); // Dismiss dialog
            finish();
        }, duration);
    }

    public String extractScanID(String filename) {
        if (filename.contains("_")) {
            return filename.substring(0, filename.indexOf("_"));
        }
        return filename; // If no underscore is found, return original filename
    }


    private void callDownloadReportsAPI(String ScanID) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Downloading report...");
        progressDialog.setCancelable(false); // Prevents dismissal on touch outside
        progressDialog.show();

        ApiService apiService = RetrofitClient.getApiService();

        Log.v("Token ::", "Token ::"+PathUtil.TOKEN);

//        DownloadRequest downloadRequest = new DownloadRequest(ScanID);

        Call<DownloadResponse> call = apiService.downloadReport(PathUtil.TOKEN, ScanID);
        call.enqueue(new Callback<DownloadResponse>() {
            @Override
            public void onResponse(Call<DownloadResponse> call, Response<DownloadResponse> response) {
                progressDialog.dismiss();
                if (response.code() == 401) {
                    handleTokenExpired();
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("GET_SUCCESS", "Response: " + response.body().getResponse());

                    //Log.d(TAG, "Raw Response String: " + patientResponse.getResponse());

                    // Convert String to JSON Array
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().getResponse());
                        if(jsonArray.length() > 0) {
                            String reportAnalysis = jsonArray.getJSONObject(0).getString("ReportDetails");
                            Log.v("ReportDetails :: ", "ReportDetails :: "+reportAnalysis);
                            String UniqueStudyId = jsonArray.getJSONObject(0).getString("UniqueStudyId");
                            Log.v("UniqueStudyId :: ", "UniqueStudyId :: "+UniqueStudyId);
                            String cardiologistName = jsonArray.getJSONObject(0).getString("Cardiologist");
                            Log.v("CardiologistName :: ", "CardiologistName :: "+cardiologistName);
                            String cardiologistSignatureURL = jsonArray.getJSONObject(0).getString("CardiologistSignatureURL");
                            Log.v("CardiologistSignatureURL :: ", "CardiologistSignatureURL :: "+cardiologistSignatureURL);
                            callGetReportsDetailsAPI(UniqueStudyId, reportAnalysis, cardiologistName, cardiologistSignatureURL);
                        }
                    } catch (Exception e) {
                        progressDialog.dismiss();
                        Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
                    }
                } else {
                    Log.e("GET_ERROR", "Error: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<DownloadResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("GET_FAILURE", "Failed: " + t.getMessage());
            }
        });
    }

    private void callGetReportsDetailsAPI(String studyUniqueId, String reportAnalysis, String cardiologistName, String cardiologistSignatureURL) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching report details...");
        progressDialog.setCancelable(false); // Prevents dismissal on touch outside
        progressDialog.show();

        ApiService apiService = RetrofitClient.getApiService();

        Log.v("Token ::", "Token ::"+PathUtil.TOKEN);

//        DownloadRequest downloadRequest = new DownloadRequest(ScanID);

        Call<GetScanResponseModel> call = apiService.getReportDetails(PathUtil.TOKEN, studyUniqueId);
        call.enqueue(new Callback<GetScanResponseModel>() {
            @Override
            public void onResponse(Call<GetScanResponseModel> call, Response<GetScanResponseModel> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {

                    Log.d(
                            "GET_SUCCESS",
                            "Response: " + response.body().getResponse()
                    );

                    try {

                        DownloadResponseData responseData =
                                response.body().getResponse();

                        if (responseData != null) {

                            String reportImageURL =
                                    responseData.getImageUrl();

                            Log.v(
                                    "ReportImageURL GIF :: ",
                                    "ReportImageURL GIF :: " + reportImageURL
                            );

                            if (reportImageURL != null &&
                                    !reportImageURL.isEmpty()) {

                                new ECGReportGenerator.ECGReportGeneratorTask(
                                        NewEcgActivity.this,
                                        reportImageURL,
                                        reportAnalysis,
                                        "",
                                        cardiologistName,
                                        cardiologistSignatureURL
                                ).execute();

                            } else {

                                progressDialog.dismiss();

                                Log.e(
                                        "API_ERROR",
                                        "ImageUrl is empty or null"
                                );
                            }

                        } else {

                            progressDialog.dismiss();

                            Log.e(
                                    "API_ERROR",
                                    "Response object is null"
                            );
                        }

                    } catch (Exception e) {

                        progressDialog.dismiss();

                        Log.e(
                                "API_ERROR",
                                "Error: " + e.getMessage(),
                                e
                        );
                    }

                } else {

                    progressDialog.dismiss();

                    Log.e(
                            "GET_ERROR",
                            "Error: " + response.errorBody()
                    );
                }
            }

            @Override
            public void onFailure(Call<GetScanResponseModel> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("GET_FAILURE", "Failed: " + t.getMessage());
            }
        });
    }

    // ✅ Common Method
    private void handleTokenExpired() {

        Toast.makeText(NewEcgActivity.this,
                "Session expired. Please login again.",
                Toast.LENGTH_LONG).show();

        // Clear Session
        PrefHelper.setBoolean("isUserLoggedIn", false);
        PrefHelper.setString("AppToken", "");
        PathUtil.TOKEN = "";

        // Redirect Login
        Intent intent =
                new Intent(NewEcgActivity.this, SignInActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);

        finish();
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
                        float[][] analysis = ecgAnalysis.analysisArray;
                        DecimalFormat df = new DecimalFormat();
                        df.setMaximumFractionDigits(2);

                        int rr = 0;
                        if((int) analysis[1][0] > 0){
                            rr = Math.round((60000 / analysis[1][0]));
                        }

//                        short[][] raw_data_from_file = new short[sampleCount][ecgData.NO_OF_CHANNELS];

                        for (int ch = 0; ch < ecgData.NO_OF_CHANNELS; ch++) {
                            for(int x = 0; x<ecgData.rawDataCount; x++)//reads the ecg data and saves in the raw_data array
                            {
//                                ecgData.raw_data[x][ch] = rawData[x][ch];
                                float currVal = ecgData.raw_data[x][ch] / Float.parseFloat(settings.actualGain);
                                ecgData.dc_corrected_data[x][ch] = currVal - ecgData.dcShift[ch];
                                ecgData.filtered_data[x][ch] = ecgFilter.filter(ch, x, ecgData.dc_corrected_data[x][ch]);
                            }
                        }

//                        final int posF = pos;
//                        printEcgData(convertFloatToShort(ecgData.sample_data));
                        String uploadFilePath = ECGFileGenerator.generateECGFile(
                                PrefHelper.getString("PhysicianName", ""),
                                Integer.parseInt(PatientID),
                                16,
                                (int) analysis[1][0],
                                Math.round(analysis[1][6]*1000),
                                Math.round(analysis[1][7]*1000),
                                Math.round(analysis[1][8]*1000),
                                0,
                                0,
                                0,
                                posF
                        );

                        Log.v("", "Upload ECG File Path :::: "+uploadFilePath);

                        uploadECGFiles(uploadFilePath);
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

    public short[][] convertFloatToShort(float[][] floatArray) {
        int rows = floatArray.length;
        int cols = floatArray[0].length; // Assuming all rows have the same length

        short[][] shortArray = new short[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                shortArray[i][j] = (short) Math.round(floatArray[i][j]); // Convert float to short
                if (shortArray[i][j] > 0) {
                    shortArray[i][j] += 2048;
                } else if (shortArray[i][j] < 0) {
                    shortArray[i][j] -= 2048;
                }
            }
        }

        return shortArray;
    }

    public void printEcgData(short[][] ecgData) {
        for (int i = 0; i < ecgData.length; i++) { // Loop through leads (12)
            StringBuilder leadData = new StringBuilder("Lead " + (i + 1) + ": ");
            for (int j = 0; j < Math.min(ecgData[i].length, 10); j++) { // Print first 10 samples
                leadData.append(ecgData[i][j]).append(", ");
            }
            System.out.println(leadData.toString());
        }
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
        /*String mimeType = "application/pdf";
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = FileProvider.getUriForFile(getBaseContext(), getApplicationContext().getPackageName() + ".provider", file);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open PDF report using.."));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), "Unable to open report. make sure your phone has a PDF viewer installed. Report file: "+ file, Toast.LENGTH_LONG).show();
        }*/

        String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + appendFileName + "_" + ecgData.date_time + ".pdf";

        Intent intent8 = new Intent("callOfflineReports");    //action: "msg"
        intent8.setPackage(getPackageName());
        getApplicationContext().sendBroadcast(intent8);


        Intent intent = new Intent(NewEcgActivity.this, PDFViewerActivity.class);
        intent.putExtra("PDFPath", filepath);
        intent.putExtra("FileName", appendFileName + "_" + ecgData.date_time + ".pdf");
        startActivity(intent);
        finish();
    }

    public void openPatientForm(){
//        Intent intent = new Intent(NewEcgActivity.this, PatientData.class);
//        startActivityForResult(intent, PATIENT_FORM_REQUEST_CODE);

//        String PatientID = "";
        if(getIntent() != null) {
            PatientID = getIntent().getStringExtra("PatientID");
//            Log.v("", "Patient ID :::: "+PatientID);
            apiService = RetrofitClient.getApiService();
            getPatientDetails(PatientID);
        }


    }

    private void getPatientDetails(String patientId) {
        Call<PatientDetailsResponse> call = apiService.getPatientDetails(patientId);

        call.enqueue(new Callback<PatientDetailsResponse>() {
            @Override
            public void onResponse(Call<PatientDetailsResponse> call, Response<PatientDetailsResponse> response) {

                if (response.code() == 401) {
                    handleTokenExpired();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    String jsonString = response.body().getResponse(); // Get response string
//                    Log.d(TAG, "Raw Response String: " + jsonString);

                    // Convert JSON String to Object using Gson
                    try {
                        Gson gson = new Gson();
                        PatientDetails patientDetails = gson.fromJson(jsonString, PatientDetails.class);

//                        Log.d("Patient", "ID: " + patientDetails.getPatientId());
                        Log.d("Patient", "Name: " + patientDetails.getFirstName() + " " + patientDetails.getLastName());
                        Log.d("Patient", "DOB: " + patientDetails.getDateOfBirth());
                        Log.d("Patient", "Mobile: " + patientDetails.getMobile1());
                        Log.d("Patient", "Gender: " + patientDetails.getGender());

//                        Toast.makeText(NewEcgActivity.this, "Patient: " + patientDetails.getFirstName(), Toast.LENGTH_LONG).show();

//                        ecgData.patient_id = ""+patientDetails.getPatientId();
                        ecgData.patient_name = patientDetails.getFirstName()+" "+patientDetails.getLastName();
                        ecgData.patient_age = patientDetails.getDateOfBirth();
                        ecgData.patient_comments = "";
                        ecgData.patient_photo = "";
                        ecgData.patient_sex = patientDetails.getGender();

                        ll_loading.setVisibility(View.VISIBLE);
                        new Handler().postDelayed(() -> generateReport(), 500);
                    } catch (JsonSyntaxException e) {
                        Log.e("API_ERROR", "JSON Parsing Error: " + e.getMessage());
                    }
                } else {
                    Toast.makeText(NewEcgActivity.this, "Failed to get patient details!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PatientDetailsResponse> call, Throwable t) {
                Log.e("API_ERROR", "API Call Failed", t);
                Toast.makeText(NewEcgActivity.this, "API Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
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

//    private void uploadPDF(final String pdfname, Uri pdffile, float[][] analysisArray){
//
//        ProgressDialog pd = new ProgressDialog(NewEcgActivity.this);
//        pd.setMessage("Please wait.. Syncing with cloud..");
//        pd.setCancelable(false);
//        pd.show();
//
//        InputStream iStream = null;
//        try {
//
//            iStream = getContentResolver().openInputStream(pdffile);
//            final byte[] inputData = getBytes(iStream);
//
//            VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(Request.Method.POST, upload_URL,
//                    new Response.Listener<NetworkResponse>() {
//                        @Override
//                        public void onResponse(NetworkResponse response) {
//                            Log.d("ressssssoo",new String(response.data));
//                            String resultResponse = new String(response.data);
//                            rQueue.getCache().clear();
//                            try {
//                                JSONObject result = new JSONObject(resultResponse);
//                                String status = result.getString("status");
////                                String message = result.getString("message");
//                                Log.v("Response :: ", result.toString());
//
//                                pd.dismiss();
//
//                                if (status.equals("success")) {
//
////                                    String appendFileName = ecgData.patient_name.length() < 12 ? ecgData.patient_name : ecgData.patient_name.substring(0, 11);
////                                    String filepath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + appendFileName + "_" + ecgData.date_time + ".pdf";
////
////                                    Intent intent = new Intent(NewEcgActivity.this, PDFViewerActivity.class);
////                                    intent.putExtra("PDFPath", filepath);
////                                    intent.putExtra("FileName", appendFileName + "_" + ecgData.date_time + ".pdf");
////                                    startActivity(intent);
////                                    finish();
//
////                                    Intent intent = new Intent(NewEcgActivity.this, PDFViewerActivity.class);
////                                    intent.putExtra("PDFPath", result.getJSONObject("success_data").getString("document_path")/*.replaceAll("", "%20")*/);
////                                    intent.putExtra("FileName", result.getJSONObject("success_data").getString("pdf"));
////                                    startActivity(intent);
////                                    finish();
//                                } else {
//                                    pd.dismiss();
//                                    //Toast.makeText(NewEcgActivity.this, result.getString("error_data").toString(), Toast.LENGTH_SHORT).show();
//                                    new AlertDialog.Builder(NewEcgActivity.this)
//                                            .setIcon(android.R.drawable.ic_dialog_alert)
//                                            .setTitle("Upload error")
//                                            .setMessage(result.getString("error_data").toString())
//                                            .setPositiveButton("Ok", new DialogInterface.OnClickListener()
//                                            {
//                                                @Override
//                                                public void onClick(DialogInterface dialog, int which) {
//                                                    dialog.dismiss();
//                                                }
//
//                                            })
//                                            //.setNegativeButton("No", null)
//                                            .show();
//                                }
//                            } catch (JSONException e) {
//                                pd.dismiss();
//                                e.printStackTrace();
//                            }
//                        }
//                    },
//                    new Response.ErrorListener() {
//                        @Override
//                        public void onErrorResponse(VolleyError error) {
//                            //Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
//                            pd.dismiss();
//                            new AlertDialog.Builder(NewEcgActivity.this)
//                                    .setIcon(android.R.drawable.ic_dialog_alert)
//                                    .setTitle("Upload error")
//                                    .setMessage(error.getMessage())
//                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener()
//                                    {
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            dialog.dismiss();
//                                        }
//
//                                    })
//                                    //.setNegativeButton("No", null)
//                                    .show();
//                        }
//                    }) {
//
//                @Override
//                public Map<String, String> getHeaders() throws AuthFailureError {
//                    String token = PrefHelper.getString("Token", "");
//                    Log.v("Token :: ", token.toString());
//                    Map<String, String> params = new HashMap<String, String>();
////                    params.put("Content-Type", "multipart/form-data; charset=UTF-8");
//                    params.put("Authorization", "Bearer "+token);
//                    Log.v("Authorization :: ", params.toString());
//                    return params;
//                }
//
//                /*
//                 * If you want to add more parameters with the image
//                 * you can do it here
//                 * here we have only one parameter with the image
//                 * which is tags
//                 * */
//                @Override
//                protected Map<String, String> getParams() throws AuthFailureError {
////                    System.getProperty("os.version"); // OS version
////                    android.os.Build.VERSION.SDK      // API Level
////                    android.os.Build.DEVICE           // Device
////                    android.os.Build.MODEL            // Model
////                    android.os.Build.PRODUCT          // Product
//
//                    JSONObject jDeviceInfo = new JSONObject();
//                    try {
////                        jDeviceInfo.put("OS", System.getProperty("os.version"));
//                        jDeviceInfo.put("API Level", android.os.Build.VERSION.SDK_INT);
//                        jDeviceInfo.put("Device", android.os.Build.DEVICE);
//                        jDeviceInfo.put("Model", android.os.Build.MODEL );
//                        jDeviceInfo.put("FullDeviceInfo", Build.BRAND+" "+android.os.Build.MODEL);
//                        Log.v("Device Informations :: ", jDeviceInfo.toString());
//                    } catch (JSONException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                    JSONObject jPatientInfo = new JSONObject();
//                    try {
////                        jDeviceInfo.put("OS", System.getProperty("os.version"));
//                        jPatientInfo.put("Id", ecgData.patient_id);
//                        jPatientInfo.put("Name", ecgData.patient_name);
//                        jPatientInfo.put("Age", ecgData.patient_age);
//                        jPatientInfo.put("Comments", ecgData.patient_comments);
//                        jPatientInfo.put("Gender", ecgData.patient_sex);
//                        jPatientInfo.put("DeviceID", ecgData.patinet_deviceid);
////                        Log.v("Patient Informations :: ", jPatientInfo.toString());
//                    } catch (JSONException e) {
//                        throw new RuntimeException(e);
//                    }
//                    float[][] analysis = analysisArray;
//                    DecimalFormat df = new DecimalFormat();
//                    df.setMaximumFractionDigits(2);
//
//                    int rr = 0;
//                    if((int) analysis[1][0] > 0){
//                        rr = Math.round((60000 / analysis[1][0]));
//                    }
//
//                    Map<String, String> params = new HashMap<>();
//                    params.put("device_id", ecgData.patinet_deviceid);
//                    params.put("latitude", ""+PathUtil.userLatitude);
//                    params.put("longitude", ""+PathUtil.userLongitude);
//                    params.put("device_info", jDeviceInfo.toString());
//                    params.put("patient_info", jPatientInfo.toString());
//                    params.put("address", ""+PathUtil.userAddress);
//                    params.put("hr", (int) analysis[1][0]+"");
//                    params.put("r", df.format(analysis[1][1])+"");
//                    params.put("rr", rr +"");
//                    params.put("pr", Math.round(analysis[1][7]*1000)+"");
//                    params.put("qrs", Math.round(analysis[1][6]*1000)+"");
//                    params.put("qt", Math.round(analysis[1][8]*1000)+"");
//                    params.put("qtc", Math.round((analysis[1][8]/analysis[1][11])*1000)+"");
//                    params.put("qt_by_qtc", df.format(analysis[1][11])+"");
//
//                    Log.v("Request :: ", params.toString());
//                    return params;
//                }
//
//                /*
//                 *pass files using below method
//                 * */
//                @Override
//                protected Map<String, DataPart> getByteData() {
//                    Map<String, DataPart> params = new HashMap<>();
//
//                    params.put("document_path", new DataPart(pdfname ,inputData));
//                    Log.v("Document Path :: ", pdfname);
//
//                    return params;
//                }
//            };
//
//
//            volleyMultipartRequest.setRetryPolicy(new DefaultRetryPolicy(
//                    400000,
//                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//            rQueue = Volley.newRequestQueue(NewEcgActivity.this);
//            rQueue.add(volleyMultipartRequest);
//
//        } catch (FileNotFoundException e) {
//            pd.dismiss();
//            e.printStackTrace();
//        } catch (IOException e) {
//            pd.dismiss();
//            e.printStackTrace();
//        }
//    }

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
