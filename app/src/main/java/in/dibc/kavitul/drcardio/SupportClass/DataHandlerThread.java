package in.dibc.kavitul.drcardio.SupportClass;

import static in.dibc.kavitul.drcardio.SupportClass.appbt.btHelper;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Date;

import in.dibc.kavitul.drcardio.Model.ecgData;

public class DataHandlerThread extends HandlerThread {
    private static final String TAG = "DataHandlerThread";
    public static final int ECG_DATA = 1;
    long lastStopAttempt = new Date().getTime();
//    ExecutorService pool = Executors.newFixedThreadPool(10 * ecgData.sampleRatePerSec);

    public DataHandlerThread() {
        super("DataHandlerThread", Process.THREAD_PRIORITY_VIDEO);
        if (settings.actualGain == null) {
            settings.getSettings();
        }
    }

    private static void sendMessage(int status) {
        Bundle bundle = new Bundle();
        Message msg = btHelper.mHandlerUpdateUi.obtainMessage();
        bundle.putInt("status", status);
        msg.setData(bundle);
        btHelper.mHandlerUpdateUi.sendMessage(msg);
    }

    @Override
    protected void onLooperPrepared() {
        appbt.btHelper.mHandlersbluetooth = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ECG_DATA:
                        if (!ecgData.isReading) {
                            if(lastStopAttempt+100 < new Date().getTime()) {
                                DataHandlerThread.sendMessage(4);
                                Log.d(TAG, "receiving data after sending stop command. trying to stop again.");
                                lastStopAttempt = new Date().getTime();
                            }
                            break;

                        }
//                        Log.d(TAG, "handleMessage: rawCount"+ecgData.rawDataCount+ " count"+msg.arg1);
                        Bundle bundle = msg.getData();
                        byte packetType = bundle.getByte("packetType");
                        byte[] statusBytes = bundle.getByteArray("statusBytes");
                        char[] dataChar = bundle.getCharArray("dataChar");
                        /* parse Response to get 1 point for each of 12 channels  */
                        float[] channels = parseData.charArrayToChannels(dataChar, settings.test_mode.equals("true"));


                        // Set base line
                        if (ecgData.sampleDataCount < ecgData.sampleDataLength) {
                            for (int ch = 0; ch < ecgData.NO_OF_CHANNELS; ch++) {
                                ecgData.sample_data[ch][ecgData.sampleDataCount] = channels[ch] / Float.parseFloat(settings.actualGain);
                                if (ecgData.sampleDataCount + 1 == ecgData.sampleDataLength) {
                                    ecgData.dcShift[ch] = ecgData.calcAvg(ecgData.sample_data[ch]);
                                }
                            }
                            ecgData.sampleDataCount++;
                            break;
                        }
                        if (ecgData.rawDataCount < ecgData.raw_data.length) {
                            ecgData.raw_data[ecgData.rawDataCount] = channels;
                            addEntry(ecgData.rawDataCount, channels);
                            ecgData.rawDataCount++;
                        }
                        break;
                    case 9:
                        // write failed.
                        Log.d(TAG, "handleMessage: write failed.");
                        DataHandlerThread.sendMessage(9);
                }
            }
        };
    }

    public void addEntry(int x, float[] channels) {
        if (x > ecgData.filtered_data.length) {
            return;
        }
        if(x%ecgData.sampleRatePerSec==500){
            Bundle bundle = new Bundle();
            Message msg = btHelper.mHandlerUpdateUi.obtainMessage();
            bundle.putInt("status", 10);
            bundle.putInt("sec", x/ecgData.sampleRatePerSec);
            msg.setData(bundle);
            btHelper.mHandlerUpdateUi.sendMessage(msg);
        }
//        Log.d(TAG, "addEntry: "+channels[0]);
        for (int chi = 0; chi < ecgData.NO_OF_CHANNELS; chi++) {
            int ch = ecgData.lead_arrange[chi];
            float currVal = channels[ch] / Float.parseFloat(settings.actualGain);
            int finalChi = chi;
            prepareEntryPerChannel(finalChi, x, currVal);
        }

        if (ecgData.rawDataCount % ecgData.downSamplingRate == 0) {
            ecgData.displayDataCount++;
        }

    }


    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.YELLOW);
        set.setLineWidth(1f);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
//        set.setValueTextSize(9f);
//        set.setFillAlpha(65);
//        set.setFillColor(Color.YELLOW);
        return set;
    }

    private void prepareEntryPerChannel(int chi, int x, float currVal) {
        int ch = ecgData.lead_arrange[chi];

        ecgData.dc_corrected_data[x][ch] = currVal - ecgData.dcShift[ch];
        ecgData.filtered_data[x][ch] = ecgFilter.filter(ch, x, ecgData.dc_corrected_data[x][ch]);
        float hpf_val = ecgFilter.highPassFilter(ch, x, ecgData.filtered_data[x][ch]);
        if (ecgData.rawDataCount % ecgData.downSamplingRate == 0) {
//                ecgData.sample_data[ch][ecgData.sampleDataCount%ecgData.sampleDataLength] = (short) hpf_val;
//                if(ecgData.sampleDataCount>ecgData.sampleDataLength) {
//                    ecgData.dcAdjust[ch] = ecgData.calcAvg(ecgData.sample_data[ch]);
//                }
//                float scaledValue = ecgData.graph_scale * (hpf_val-ecgData.dcAdjust[ch]);
            float scaledValue = ecgData.graph_scale * hpf_val;
            try {
                addEntryPerChannel(chi, scaledValue, ecgData.displayDataCount);
            } catch (Exception e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
    }

    private void addEntryPerChannel(int ch, float y, int x) {
//        Log.d(TAG, "addEntryPerChannel: ch:"+ch+" x:"+x+" y:"+y);
        LineData data = ecgData.chart[ch].getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            /* // set.addEntry(...); // can be called as well */
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            try {
                data.addEntry(new Entry(set.getEntryCount(), y), 0);
                data.notifyDataChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
            ecgData.chart[ch].notifyDataSetChanged();

            ecgData.chart[ch].setVisibleXRange(ecgData.display_data_range_x - 1, ecgData.display_data_range_x);

            if (x > ecgData.display_data_range_x) {
                int x_pos = (int) (ecgData.display_data_range_x * (1 + Math.ceil(x / ecgData.display_data_range_x)));
                ecgData.chart[ch].moveViewToX(x_pos);
            } else {
                ecgData.chart[ch].moveViewToX(ecgData.display_data_range_x);
            }
        }
    }

}