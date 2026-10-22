package in.dibc.kavitul.drcardio.SupportClass;

import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import baseline_remove1.Baseline_remove1;
import in.dibc.kavitul.drcardio.Model.ecgData;
import in.dibc.kavitul.drcardio.utils.PrefHelper;

public class ECGFileGenerator {

    public static String generateECGFile(
            String physicianName, int patientId, int apkVersion, int heartRate, int qrsInterval, int prInterval,
            int qtInterval, int pAxis, int qrsAxis, int tAxis, int start_position) {

        try {
            // Define the file name
            String deviceId = PrefHelper.getString("AmplifierID", "");
            String dateTime = getCurrentTimestamp();
            String fileName = deviceId + "-" + dateTime + "_" + physicianName + ".ecg";
            String fileNameTmp = deviceId + "-" + dateTime + "_" + physicianName + ".tmp";
            Log.v("Upload :: ", "patientID :: "+patientId);
//            Log.v("Upload :: ", "qrsInterval :: "+qrsInterval);
//            Log.v("Upload :: ", "prInterval :: "+prInterval);
//            Log.v("Upload :: ", "qtInterval :: "+qtInterval);
//            Log.v("Upload :: ", "qrsAxis :: "+qrsAxis);
//            Log.v("Upload :: ", "heartRate :: "+heartRate);

            // Get the external storage directory
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(directory, fileName);

            File  tmpFile = new File(directory, fileNameTmp);
            if (!tmpFile.exists()) {
                tmpFile.createNewFile(); // Create empty file
            }

            // Create output stream
            FileOutputStream fos = new FileOutputStream(file);

            int headerSize = 161;
            int ecgDataSize = 12 * 2500 * 2; // 12 leads × 5000 samples × 2 bytes per sample
            int totalFileSize = headerSize + ecgDataSize;

            // Create a byte buffer to store data
            ByteBuffer buffer = ByteBuffer.allocate(headerSize + (ecgDataSize));
            buffer.order(ByteOrder.LITTLE_ENDIAN); // Ensure correct byte order

            // Write header information
            buffer.position(0); // Move to the 8th byte
            buffer.putInt(totalFileSize); // File size placeholder
            buffer.position(4); // Move to the 8th byte
            buffer.putInt(patientId); // Patient ID at 8th byte
            buffer.position(13); // Move to the 13th byte
            buffer.putShort((short) apkVersion); // APK Version at 13th byte
            buffer.position(14); // Move to the 14th byte
            buffer.putShort((short) 0); // APK Version at 13th byte
            buffer.position(157);
            buffer.putShort((short) heartRate); // Heart rate at 157th byte
            buffer.position(147);
            buffer.putShort((short) qrsInterval); // QRS Interval at 147th byte
            buffer.position(145);
            buffer.putShort((short) prInterval); // PR Interval at 145th byte
            buffer.position(149);
            buffer.putShort((short) qtInterval); // QT Interval at 149th byte
            buffer.position(151);
            buffer.putShort((short) pAxis); // P Axis at 151st byte
            buffer.position(153);
            buffer.putShort((short) qrsAxis); // QRS Axis at 153rd byte
            buffer.position(155);
            buffer.putShort((short) tAxis); // T Axis at 155th byte
//            Log.v("Upload :: ", "ecgWaveformData Length :: "+ecgWaveformData.length);

            // Example ECG data (12 leads, each with 2500 samples)
            float[][] ecgDataArray = new float[12][2500];
            int dataLength = 6500;
            Baseline_remove1 baseLineRemove = new Baseline_remove1();

            for (int ch = 0; ch < 12; ch++) {
                float[] chData = new float[dataLength];
                double[] filteredData = new double[dataLength];
                for (int i = 0; i < dataLength; i++) {
                    filteredData[i] = ecgData.filtered_data[i + start_position][ch];
                }
                double[] baselineCorrectedData = baseLineRemove.Remove(filteredData);
                final int[] index_map = {11, 0, 1, 7, 9, 8, 10, 6, 2, 3, 4, 5};
                for (int j = 0, index = 0; j < 5000; j += 2, index++) {
                    ecgDataArray[index_map[ch]][index]= (float) baselineCorrectedData[j+250];
                }
            }

            // 10. SineWave Data (12 leads * 5000 samples per lead)
            buffer.position(headerSize); // Start ECG data after the header
            for (float[] leadData : ecgDataArray) {
                for (float value : leadData) {
//                    buffer.putShort((short) Math.round(value + 2048)); // ✅ Uses 2 bytes per value
                    buffer.putShort((short) Math.round(value * 6 + 2048)); // ✅ Uses 2 bytes per value
                }
            }

            Log.d("ECGData", "Buffer Data (Hex): " + byteBufferToHex(buffer));

            // Writing to file
            fos.write(buffer.array());
            fos.flush();

            Log.d("ECGFileGenerator", "ECG file generated successfully: " + file.getAbsolutePath());

            return file.getAbsolutePath();

        } catch (IOException e) {
            Log.e("ECGFileGenerator", "Error generating ECG file", e);
        }
        return "";
    }

    private static String byteBufferToHex(ByteBuffer buffer) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < buffer.capacity(); i++) {
            hexString.append(String.format("%02X ", buffer.get(i))); // Convert each byte to Hex
        }
        return hexString.toString();
    }

    public static String convertEcgDataToJson(short[][] ecgData) {
        try {
            JSONArray leadsArray = new JSONArray();

            for (short[] lead : ecgData) {
                JSONArray leadArray = new JSONArray();
                for (short sample : lead) {
                    leadArray.put(sample);
                }
                leadsArray.put(leadArray);
            }

            JSONObject json = new JSONObject();
            json.put("ecgData", leadsArray);

            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    public static String convertEcgDataToJsonFloat(float[][] ecgData) {
        try {
            JSONArray leadsArray = new JSONArray();

            for (float[] lead : ecgData) {
                JSONArray leadArray = new JSONArray();
                for (float sample : lead) {
                    leadArray.put(Math.round(Math.round((sample * 3 + 2048) * 1000f) / 1000f));
                }
                leadsArray.put(leadArray);
            }

            JSONObject json = new JSONObject();
            json.put("ecgData", leadsArray);

            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    public static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static short[][] convertFloatToShort(float[][] floatArray) {
        int rows = floatArray.length;
        int cols = floatArray[0].length; // Assuming all rows have the same length

        short[][] shortArray = new short[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                shortArray[i][j] = (short) Math.round(floatArray[i][j]); // Convert float to short
            }
        }

        return shortArray;
    }
}

