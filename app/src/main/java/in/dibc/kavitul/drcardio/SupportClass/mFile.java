package in.dibc.kavitul.drcardio.SupportClass;

import static in.dibc.kavitul.drcardio.SupportClass.appbt.btHelper;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import baseline_remove1.Baseline_remove1;
import in.dibc.kavitul.drcardio.Model.ecgData;

public class mFile {
    static SimpleDateFormat s2 = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
    static String newLine = "\r\n";
    private static final String TAG = "mFile";
    //#Bhavesh Change
    public static final String DIR_DR_CARDIO = "Dr.Cardio";
    public static final String DIR_REPORT = "Reports";
    public static final String DIR_DATA = "Data";

    public static void writeLog(String data) {
        if(!settings.debug_mode && data.indexOf("Error") < 0){
            return;
        }
        String logFileName = s2.format(new Date()) + ".log";

        String filePath = appbt.storageBaseDir + "/Logs/";

        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(filePath + logFileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        try {
            FileOutputStream fileOutputStream  = new FileOutputStream(file, true);
            OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
            writer.append(data);
            writer.append(newLine);
            writer.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void saveDatFile(){
        try {
            String appendFileName = ecgData.patient_name.length() < 20 ? ecgData.patient_name : ecgData.patient_name.substring(0, 20);
//            File myFile = new File(appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_DATA] + "/" + ecgData.date_time.replaceAll("-","").replaceAll("_","") + "_" + appendFileName + ".dat");
            File myFile = new File(appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_DATA] + "/" + appendFileName + "_" + ecgData.date_time + ".dat");
            RandomAccessFile raf = new RandomAccessFile(myFile, "rw");
            //Writing File info on the first line
            //Writing sample count and down sampling rate in the second line with tab as a separator
            Log.d("mFile: write dat", ecgData.APP_FILE_VERSION + " " + ecgData.rawDataCount + " " + ecgData.downSamplingRate);
            raf.writeUTF(ecgData.APP_FILE_VERSION);
            raf.writeInt(ecgData.hwVersion);
            raf.writeUTF(ecgData.deviceName);
            raf.writeUTF(ecgData.date_time);
            raf.writeInt(ecgData.rawDataCount);
            raf.writeInt(ecgData.downSamplingRate);
            raf.writeUTF(ecgData.dcShiftToString());
            raf.writeUTF(settings.test_mode);
            raf.writeUTF(ecgData.patientInfoToString());
            raf.writeUTF(ecgData.patient_comments);
            raf.writeUTF(settings.gain);
            raf.writeUTF(settings.actualGain);

            //Writing raw data on packet by packet till the sample count i.e. number of packets
            for (int j = 0; j < ecgData.rawDataCount; j++) {
                for (int k = 0; k < 12; k++) {
                    raf.writeFloat(ecgData.raw_data[j][k]);
                }
            }
            raf.close();
            Log.d(TAG, "saveDatFile: complete");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "saveDatFile: error ", e);
        }
    }


    public static void readDatFile(String file_name) {
        ecgData.resetEcgData();
        try {
            File myFile = new File(appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_DATA] + "/" + file_name + ".dat");
            RandomAccessFile raf = new RandomAccessFile(myFile, "r");

            ecgData.fileVersion = raf.readUTF();
            ecgData.hwVersion = raf.readInt();
            if (ecgData.hwVersion == 2) {
                appbt.btHelper.LODB = 24;
                settings.val_per_mv = 3495f;
            } else {
                appbt.btHelper.LODB = 16;
                settings.val_per_mv = 54.61f;
            }
            ecgData.deviceName = raf.readUTF();

            if (!ecgData.fileVersion.equals(ecgData.APP_FILE_VERSION)) {
                sendMessage(2);
                return;
            }

            ecgData.date_time = raf.readUTF();

            int dataCount = raf.readInt();
//             int downSamplingRate = raf.readInt();
            ecgData.downSamplingRate = raf.readInt();
            ecgData.downSamplingAverage = new short[ecgData.NO_OF_CHANNELS][ecgData.downSamplingRate];
            String dcShiftData = raf.readUTF();
            ecgData.stringToDcShift(dcShiftData);
            settings.test_mode = raf.readUTF();
            String patient_data = raf.readUTF();
            ecgData.stringToPatientInfo(patient_data);
            ecgData.patient_comments = raf.readUTF();
            settings.gain = raf.readUTF();
            ecgData.graph_scale = Float.parseFloat(settings.gain);
            settings.actualGain = raf.readUTF();
            float[][] rawData = new float[dataCount][ecgData.NO_OF_CHANNELS];
            ecgFilter.setFilter(settings.filter);
            ArrayList<Entry>[] ecgValues = new ArrayList[12];
            for (int ch = 0; ch < ecgData.NO_OF_CHANNELS; ch++) {
                ecgValues[ch] = new ArrayList<Entry>();
            }
            int zeroDatas = 0;
            ecgData.rawDataCount = 0;
            for (int x = 0; x < dataCount; x++)//reads the ecg data and saves in the raw_data array
            {
                // TODO: 2/1/21 remove zero data check as its a temporary fix
                if(zeroDatas>10){
                    break;
                } else {
                    for (int ch = 0; ch < ecgData.NO_OF_CHANNELS; ch++) {
                        rawData[x][ch] = raf.readFloat();
                    }
                    ecgData.rawDataCount++;

                    if (rawData[x][1] == 0 && rawData[x][2] == 0) {
                        zeroDatas++;
                    }
                }
            }
            raf.close();
            ecgData.raw_data = new float[ecgData.rawDataCount][ecgData.NO_OF_CHANNELS];
            ecgData.dc_corrected_data = new float[ecgData.rawDataCount][ecgData.NO_OF_CHANNELS];
            ecgData.filtered_data = new float[ecgData.rawDataCount][ecgData.NO_OF_CHANNELS];


            float[][] sample_data = new float[ecgData.NO_OF_CHANNELS][ecgData.sampleDataLength];
            for (int ch = 0; ch < ecgData.NO_OF_CHANNELS; ch++) {
                for(int x = 0; x<ecgData.rawDataCount; x++)//reads the ecg data and saves in the raw_data array
                {
                    ecgData.raw_data[x][ch] = rawData[x][ch];
                    float currVal = ecgData.raw_data[x][ch] / Float.parseFloat(settings.actualGain);
                    ecgData.dc_corrected_data[x][ch] = currVal - ecgData.dcShift[ch];
                    ecgData.filtered_data[x][ch] = ecgFilter.filter(ch, x, ecgData.dc_corrected_data[x][ch]);
                }
            }
            sendMessage(3);

        } catch (Exception e) {
            sendMessage(2);
            e.printStackTrace();
            Log.d(TAG, "readDatFile: error ", e);
        }
    }

    private static void sendMessage(int status){
        Bundle bundle = new Bundle();
        Message msg = btHelper.mHandlerUpdateUi.obtainMessage();
        bundle.putInt("status", status);
        msg.setData(bundle);
        btHelper.mHandlerUpdateUi.sendMessage(msg);
    }

    public static String makeCompatible(String file_name){
        //here we make the file compatible with the current version
        try{
            File myFile = new File(appbt.storageBaseDir +"/"+ appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_OLD_DATA] + "/" + file_name + ".dat");
            RandomAccessFile raf = new RandomAccessFile(myFile,"r");

            String title, sdf1, schno, sname, sdob, sage, sgen, sht, swt, smedi, sBP;
            int iGain, filter_state;

            ecgData.date_time = file_name;
            ecgData.fileVersion = "1";
            ecgData.downSamplingRate = 5;
            ecgData.rawDataCount = 6500;
            //Convert here and then send data to raw_ecg data
            title = raf.readUTF();//title
            sdf1=raf.readUTF();//date
            schno=raf.readUTF();//chssno
            ecgData.patient_name=raf.readUTF();//name
            ecgData.patient_dob=raf.readUTF();//dob//
            ecgData.patient_age=raf.readUTF();//age//
            ecgData.patient_sex=raf.readUTF();//gende
            ecgData.patient_height=raf.readUTF();//height
            ecgData.patient_weight=raf.readUTF();//weight
            ecgData.patient_medications=raf.readUTF();//medi
            ecgData.patient_bloodpressure=raf.readUTF();//bp
            ecgData.patient_comments="NA";//comments
            iGain=raf.readInt();//gain
            filter_state=raf.readInt();//filter

            raf.seek(1023);
            ecgData.raw_data = new float[ecgData.rawDataCount][12];

            for(int j=0;j<ecgData.NO_OF_CHANNELS;j++)
            {
                for(int k = 0; k< ecgData.rawDataCount; k++) {
                    ecgData.raw_data[k][j] = (short) raf.readInt();
                }
            }
            raf.close();

            saveDatFile();
            return file_name;

        }catch (Exception e) {
            sendMessage(2);
            e.printStackTrace();
        }

        return "";

    }


    public static void saveDataCsvFile(int start_position, Context appContext){
        try {
            String appendFileName = ecgData.patient_name.length() < 20 ? ecgData.patient_name : ecgData.patient_name.substring(0, 20);
//            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String filename = appendFileName + "_" + ecgData.date_time + ".csv";

            File file = new File(appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_CSV] + "/" + filename);
//            File file = new File(downloads + "/" + filename);
            Log.d("mFile: write dat", ecgData.APP_FILE_VERSION + " " + ecgData.rawDataCount + " " + ecgData.downSamplingRate);
            FileWriter fw = new FileWriter(file);

            int dataLength = 6500;
            Baseline_remove1 baseLineRemove = new Baseline_remove1();
            float[][] ecgDataArray = new float[12][5000];

            for (int k = 0; k < 12; k++) {
                fw.append(String.valueOf(ecgData.lead_name[k]));
                fw.append(',');
            }
            fw.append('\n');
            if(ecgData.rawDataCount < dataLength){
                return;
            } else if(start_position+dataLength>ecgData.rawDataCount){
                start_position = ecgData.rawDataCount - dataLength;
            }

            for (int ch = 0; ch < 12; ch++) {
                float[] chData = new float[dataLength];
                double[] filteredData = new double[dataLength];
                for (int i = 0; i < dataLength; i++) {
                    filteredData[i] = ecgData.filtered_data[i + start_position][ch];
                }
                double[] baselineCorrectedData = baseLineRemove.Remove(filteredData);
                for (int j = 0; j < 5000; j++) {
                    ecgDataArray[ch][j]= (float) baselineCorrectedData[j+250];
                }
            }
            for (int j = 0; j < 5000; j++) {
                for (int k = 0; k < 12; k++) {
                    int ch = ecgData.lead_arrange[k];
                    float val = ecgDataArray[ch][j] / settings.val_per_mv;
                    fw.append(String.valueOf(val));
                    fw.append(',');
                }
                fw.append('\n');
            }
            fw.close();
            Log.d(TAG, "saveCsvFile: complete");

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "saveDatFile: error ", e);
        }
    }

    public static File getStorageParentDir(Context context){
        return new File(context.getExternalFilesDir(""),mFile.DIR_DR_CARDIO);
    }
    public static File getDirFile(Context context, String dirName){
        return new File(context.getExternalFilesDir(""),mFile.DIR_DR_CARDIO + File.separator + dirName);
    }

}
