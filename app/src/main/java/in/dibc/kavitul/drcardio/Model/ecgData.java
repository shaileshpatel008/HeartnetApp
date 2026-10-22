package in.dibc.kavitul.drcardio.Model;

import com.github.mikephil.charting.charts.LineChart;

import java.text.SimpleDateFormat;
import java.util.Date;

import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.mFile;
import in.dibc.kavitul.drcardio.SupportClass.settings;

public class ecgData {
    private static final String TAG = "ecgData";
    public final static int NO_OF_CHANNELS = 12;
    public static int sampleRatePerSec = 500;  /* sample rate = 500 samples per sec */
    public static int samplesForDcShiftCalc = 500;
    public static int downSamplingRate = 10; /* one point to consider every x points while displaying graph. */
    public static short[][] downSamplingAverage = new short[NO_OF_CHANNELS][downSamplingRate];
    public static int sampleDataSec = 3;
    public static int sampleDataLength = sampleDataSec * sampleRatePerSec;
    public static int sampleDataCount = 0;
    public static float[][] sample_data = new float[NO_OF_CHANNELS][sampleDataLength];
    public static float[] dcShift = new float[NO_OF_CHANNELS];
    public static float[] dcAdjust = new float[NO_OF_CHANNELS];
    public static float[][] raw_data;
    public static float[][] dc_corrected_data;
    public static float[][] filtered_data;


    public static float[] pre_IIR_X = new float[NO_OF_CHANNELS];
    public static float[] pre_IIR_Y = new float[NO_OF_CHANNELS];

    public static String fileVersion;
    public static int hwVersion;
    public static int rawDataCount = 0, displayDataCount = 0, start_point = 200,
            display_data_range_x = 13 * sampleRatePerSec / downSamplingRate;
    public static String date_time = "";
    public static String patient_id, patient_name, patient_age, patient_sex, patient_medications, patient_bloodpressure, patient_dob = "", patient_height, patient_weight, patient_comments, patient_photo, patinet_deviceid, patient_signature_photo;
    public static final String APP_FILE_VERSION = "1";
    // TODO: 10-01-2021 Add gain and filter
    public static String separator = "\t";
    static String[] patientFields;

    public static int[] lead_arrange = {1, 2, 8, 9, 10, 11, 7, 3, 5, 4, 6, 0};
//    public static int[] lead_arrange = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    public static String[] lead_name = {"I      ", "II     ", "III    ", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"};

    public static LineChart[] chart = new LineChart[ecgData.NO_OF_CHANNELS];
    public static Boolean isReading = false;

    public static float graph_scale = 1;
    public static String deviceName;

    public static void parseInfo(){
        if (date_time == null || date_time.equals("")) {
            return;
        }
        mFile.readDatFile(date_time);
    }

    public static void resetEcgData() {
        patient_id = "";
        patient_name = "";
        patient_age = "";
        patient_sex = "";
        patient_medications = "";
        patient_bloodpressure = "";
        patient_dob = "";
        patient_height = "";
        patient_weight = "";
        patient_comments = "";

        initDataWithLength(false);
        ecgData.date_time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SS").format(new Date());
        ecgData.deviceName = appbt.btHelper.connectedDeviceName;
    }
    public static void initDataWithLength(boolean auto) {
        int len = auto ? settings.auto_read_seconds * ecgData.sampleRatePerSec : settings.max_read_seconds * ecgData.sampleRatePerSec;
        ecgData.displayDataCount = 0;
        ecgData.sampleDataCount = 0;
        ecgData.rawDataCount = 0;
        ecgData.raw_data = new float[len][NO_OF_CHANNELS];
        ecgData.dc_corrected_data = new float[len][NO_OF_CHANNELS];
        ecgData.filtered_data = new float[len][NO_OF_CHANNELS];
        ecgData.sample_data = new float[NO_OF_CHANNELS][sampleDataLength];
        downSamplingAverage = new short[NO_OF_CHANNELS][downSamplingRate];
        sample_data = new float[NO_OF_CHANNELS][sampleDataLength];
        dcShift = new float[NO_OF_CHANNELS];
        pre_IIR_X = new float[NO_OF_CHANNELS];
        pre_IIR_Y = new float[NO_OF_CHANNELS];
    }
    public static String patientInfoToString() {
        String patient_info_string = "";
        patient_info_string = patient_id + separator
                + patient_name + separator
                + patient_age + separator
                + patient_sex + separator;
//        ecgData ecg_data = new ecgData();
//        for(int i=0; i<patientFields.length; i++){
//            //Field f1;
//            try {
//                patient_info_string += ecgData.class.getDeclaredField(patientFields[i]).get(ecg_data).toString();
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            } catch (NoSuchFieldException e) {
//                e.printStackTrace();
//            }
//            patient_info_string += separator;
//        }
        return patient_info_string;
    }
    public static boolean stringToPatientInfo(String patient_data) {
        boolean parseResult = true;
        if (patient_data == null || patient_data.equals("")) {
            parseResult = false;
        } else {
            String[] arrPatientData = patient_data.split(separator, 4);

            patient_id = arrPatientData[0].trim();
            patient_name = arrPatientData[1].trim();
            patient_age = arrPatientData[2].trim();
            patient_sex = arrPatientData[3].trim();

//            for(int i=0; i<patientFields.length; i++) {
//                try {
//                    ecgData.class.getDeclaredField(patientFields[i]).set(patientFields[i], arrPatientData[i]);
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                    parseResult = false;
//                } catch (NoSuchFieldException e) {
//                    e.printStackTrace();
//                    parseResult = false;
//                }
//            }
        }
        return parseResult;
    }

    public static String dcShiftToString() {
        String dcShiftString = String.valueOf(ecgData.dcShift[0]);
        for (int i = 1; i < NO_OF_CHANNELS; i++) {
            dcShiftString += separator + ecgData.dcShift[i];
        }
        return dcShiftString;
    }

    public static boolean stringToDcShift(String dcShiftString) {
        boolean parseResult = true;
        if (dcShiftString == null || dcShiftString.equals("")) {
            parseResult = false;
        } else {
            String[] arrDcShift = dcShiftString.split(separator, NO_OF_CHANNELS);
            for (int i = 0; i < NO_OF_CHANNELS; i++) {
                try {
                    ecgData.dcShift[i] = Float.parseFloat(arrDcShift[i]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    parseResult = false;
                }
            }
        }
        return parseResult;
    }

    public static float calcAvg(float[] channel) {
        double total = 0;
        for (int i = 0; i < channel.length; i++) {
            total += channel[i];
        }
        short avg = (short) (total / channel.length);
        return avg;
    }

}
