package in.dibc.kavitul.drcardio.SupportClass;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfDocument.PageInfo;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;

import baseline_remove1.Baseline_remove1;
import in.dibc.kavitul.drcardio.Model.ecgData;
import in.dibc.kavitul.drcardio.R;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class PdfGenerator {
    /*
     PS = Post Script
     PT = Point
    */
    private static final String TAG = "PdfGenerator";
    String name;
    int x_position;
    int line_height = appbt.res.getInteger(R.integer.report_info_line_height),
            lines = appbt.res.getInteger(R.integer.report_info_lines),
            current_column = 0,
            no_of_columns = 6,
            maxLength = 20,
            pagePadding = appbt.res.getInteger(R.integer.report_page_padding),
            padding = appbt.res.getInteger(R.integer.report_padding),
            mmPerBox = 5,
            col_width = (appbt.res.getInteger(R.integer.report_page_width) - 2 * pagePadding) / no_of_columns,
            reqBoxesPerCol = 32,
            reqBoxesPerRow = 55;
    float PS_PT_per_mm = 2.835f;
    public static String sdf;

    float info_rect_height = line_height * lines + pagePadding + 2 * padding;
    int x_boxes = 14;
    int y_boxes = 8;
    double graph_duration = 2.5 + 0.3; // additional 0.1 sec of data to complete the 14 5mm boxes
    private final float strokeWidth = 0.5f;

    float x_range = x_boxes*mmPerBox*PS_PT_per_mm;
    float y_range = y_boxes*mmPerBox*PS_PT_per_mm;
    float page_horz_start = pagePadding;
    float page_horz_stop =  appbt.res.getInteger(R.integer.report_page_width) - pagePadding;
    float page_vert_start = pagePadding;
    float page_vert_stop = info_rect_height + padding + 4 * y_range;

    int thin_line_color = Color.parseColor("#FFB9DC");
    int thick_line_color = Color.parseColor("#FF3399");
    int black = Color.parseColor("#000000");
    private final int multi_column = 1;


    float val_per_mv = settings.val_per_mv;
    int X_Scale_mm_per_sec = 25;
    float x_sample_per_mm = ecgData.sampleRatePerSec / X_Scale_mm_per_sec;
    float x_PS_PT_per_sample = PS_PT_per_mm / x_sample_per_mm;
    int Y_Scale_mm_per_mv = 10;
    float y_PS_PT_per_mV = (PS_PT_per_mm * Y_Scale_mm_per_mv) / val_per_mv;

    float calibration_signal_length_mm = 5f;
    private final int report_data_length = 6500;
    private final Baseline_remove1 baseLineRemove = new Baseline_remove1();
    int x_graphs = 4, y_graphs = 3;

    float[][] analysis;

    public PdfGenerator(int x_pos, float[][] analysisArray) {
        x_position = x_pos;
        if (x_position >= (ecgData.raw_data.length - report_data_length)) {
            x_position = ecgData.raw_data.length - report_data_length;
        }

        analysis = analysisArray;
    }

    public Boolean generatePDF(Context context) {
        if(ecgData.rawDataCount < report_data_length){
            return false;
        }
        PdfDocument ecg_report = new PdfDocument();
        Paint paint = new Paint();
        paint.setTextSize(8f);
        int page_width = appbt.res.getInteger(R.integer.report_page_width);
        int page_height = appbt.res.getInteger(R.integer.report_page_height);
        PageInfo info = new PageInfo.Builder(page_width, page_height, 1).create();

        for (int i = 0; i < settings.reportTypes.length; i++) {
            if (settings.reports[i]) {
                PdfDocument.Page page = ecg_report.startPage(info);
                page = getReportPage(page, settings.reportTypes[i], paint);
                ecg_report.finishPage(page);
            }
        }

        String appendFileName = ecgData.patient_name.length() < 12 ? ecgData.patient_name : ecgData.patient_name.substring(0, 11);
//        File myFile = new File(appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + ecgData.date_time.replaceAll("-","").replaceAll("_","") + "_" + appendFileName + ".pdf");
        File myFile = new File(appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + appendFileName + "_" + ecgData.date_time + ".pdf");

        //#Bhavesh Change
        /*String fileName = appendFileName + "_" + ecgData.date_time + ".pdf";
        File dirReport = mFile.getDirFile(context,mFile.DIR_REPORT);
        if (!dirReport.exists())
            dirReport.mkdirs();
        File pdfFile = new File(dirReport.getAbsolutePath() + File.separator + fileName);*/
        try {
            ecg_report.writeTo(new FileOutputStream(myFile));
        } catch (IOException e) {
            Log.e("main", "error " + e.toString());
        }
        ecg_report.close();
        return true;
    }

    private PdfDocument.Page getReportPage(PdfDocument.Page page, String reportType, Paint paint) {
        switch (reportType) {
            case "Simultaneous 4x3":
                page = generate4x3ReportPage(true, page, paint, reportType);
                break;

            case "Sequential 4x3":
                page = generate4x3ReportPage(false, page, paint, reportType);
                break;
            case "Simultaneous 6x2":
                page = generate6x2ReportPage(true, page, paint, reportType);
                break;
            case "Sequential 6x2":
                page = generate6x2ReportPage(false, page, paint, reportType);
                break;
            case "12x1":

                break;
            case "Analysis":

                break;
            case "Vector":

                break;
        }

        return page;
    }


    private PdfDocument.Page generate4x3ReportPage(boolean simultaneous, PdfDocument.Page page, Paint paint, String reportType) {
        setDimensions4x3();
        drawGridLines(page, paint);
        drawBlueGridLines(page, paint, true);
        drawBlueOutlines(page, paint);
        draw4x3Graphs(page, paint, ecgData.graph_scale, simultaneous);
        drawInfo(page, paint, reportType);
        return page;
    }

    private PdfDocument.Page generate6x2ReportPage(boolean simultaneous, PdfDocument.Page page, Paint paint, String reportType) {
        setDimensions6x2();
        drawGridLines(page, paint);
        drawBlueGridLines(page, paint, false);
        drawBlueOutlines(page, paint);
        draw6x2Graphs(page, paint, ecgData.graph_scale, simultaneous);
        drawInfo6x2(page, paint, reportType);
        return page;
    }

    private PdfDocument.Page generate12x1ReportPage(boolean simultaneous, PdfDocument.Page page, Paint paint, String reportType) {
        setDimensionsRightInfo();
        drawGridLines(page, paint);
        drawBlueGridLines(page, paint, false);
        drawBlueOutlines(page, paint);
        draw12x1Graphs(page, paint, ecgData.graph_scale, simultaneous);
        drawInfo(page, paint, reportType);
        return page;
    }

    private void setDimensions4x3() {
        info_rect_height = line_height * lines + pagePadding + 2 * padding;
        x_boxes = 14;
        y_boxes = 8;
        graph_duration = 2.5 + 0.3; // additional 0.1 sec of data to complete the 14 5mm boxes
        y_graphs = 4;

        setDimensionsTopInfo();
    }

    private void setDimensions6x2() {
        lines = 4;
        no_of_columns = 10;
        col_width = (appbt.res.getInteger(R.integer.report_page_width) - 2 * pagePadding) / no_of_columns;
        info_rect_height = line_height * lines + pagePadding + 2 * padding;
        x_boxes = 28;
        y_boxes = 6;
        graph_duration = 5.6;
        x_graphs = 2;
        y_graphs = 6;

        setDimensionsTopInfo();
    }

    private void setDimensionsTopInfo() {
        info_rect_height = line_height * lines + pagePadding + 2 * padding;
        x_range = x_boxes * mmPerBox * PS_PT_per_mm;
        y_range = y_boxes * mmPerBox * PS_PT_per_mm;
        page_horz_start = pagePadding;
        page_horz_stop = appbt.res.getInteger(R.integer.report_page_width) - pagePadding;
        page_vert_start = pagePadding;
        page_vert_stop = info_rect_height + padding + y_graphs * y_range;
    }

    private void setDimensionsRightInfo() {
        info_rect_height = 1;
        x_boxes = 21;
        y_boxes = 6;
        graph_duration = 2.8 * 4; // additional 0.1 sec of data to complete the 14 5mm boxes

        x_range = x_boxes * mmPerBox * PS_PT_per_mm;
        y_range = y_boxes * mmPerBox * PS_PT_per_mm;
        page_horz_start = pagePadding;
        page_horz_stop = appbt.res.getInteger(R.integer.report_page_width) - pagePadding;
        page_vert_start = pagePadding;
        page_vert_stop = info_rect_height + padding + 4 * y_range;
    }

    private void draw4x3Graphs(PdfDocument.Page page, Paint myPaint, float scale, Boolean simultaneous) {
        float x, y;
        x_boxes = 14;
        y_boxes = 8;
        for (int ch = 0; ch < ecgData.NO_OF_CHANNELS; ch++) {
            int rows = (int) Math.floor(((page_vert_stop - info_rect_height - padding) / PS_PT_per_mm) / (y_boxes * mmPerBox));
            x = (((int) Math.floor(ch / rows)) * x_range) + pagePadding + mmPerBox * PS_PT_per_mm;
            y = info_rect_height + padding + ((int) (Math.floor(ch % rows)) * y_range) + y_range / 2;

            double data_start;
            if (simultaneous) {
                data_start = 0;
            } else {
                data_start = Math.floor(ch / rows) * graph_duration;
            }

            float[] chData = getChannelData(ecgData.lead_arrange[ch]);

            if(Math.floor(ch/rows) == 0){
                plotCalibration(page, myPaint, pagePadding, y, scale);
            }
            plotGraph(page, myPaint, ecgData.lead_name[ch], x, y, chData, data_start, graph_duration, scale);
        }
        /*
        For the second graph of 10 secs
         */
        x = pagePadding + mmPerBox * PS_PT_per_mm;
        y = page_vert_stop - y_range / 2;
        plotCalibration(page, myPaint, pagePadding, y, scale);
        float[] chData = getChannelData(ecgData.lead_arrange[1]);
        plotGraph(page, myPaint, ecgData.lead_name[1], x, y, chData, 0, graph_duration * 4, scale);
    }

    private void draw6x2Graphs(PdfDocument.Page page, Paint myPaint, float scale, Boolean simultaneous) {
        float x, y;
        x_boxes = 21;
        y_boxes = 6;
        for (int ch = 0; ch < ecgData.NO_OF_CHANNELS; ch++) {
            int rows = (int) Math.floor(((page_vert_stop - info_rect_height - padding) / PS_PT_per_mm) / (y_boxes * mmPerBox));
            x = (((int) Math.floor(ch / rows)) * x_range) + pagePadding + mmPerBox * PS_PT_per_mm;
            y = info_rect_height + padding + ((int) (Math.floor(ch % rows)) * y_range) + y_range / 2;
            double data_start;
            if (simultaneous) {
                data_start = 0;
            } else {
                data_start = Math.floor(ch / rows) * graph_duration;
            }

            float[] chData = getChannelData(ecgData.lead_arrange[ch]);

            if (Math.floor(ch / rows) == 0) {
                plotCalibration(page, myPaint, pagePadding, y, scale);
            }
            plotGraph(page, myPaint, ecgData.lead_name[ch], x, y, chData, data_start, graph_duration, scale);
        }
    }

    private void draw12x1Graphs(PdfDocument.Page page, Paint myPaint, float scale, Boolean simultaneous) {
        float x, y;
        x_boxes = 21;
        y_boxes = 6;
        for (int ch = 0; ch < ecgData.NO_OF_CHANNELS; ch++) {
            int rows = (int) Math.floor(((page_vert_stop - info_rect_height - padding) / PS_PT_per_mm) / (y_boxes * mmPerBox));
            x = (((int) Math.floor(ch / rows)) * x_range) + pagePadding + mmPerBox * PS_PT_per_mm;
            y = info_rect_height + padding + ((int) (Math.floor(ch % rows)) * y_range) + y_range / 2;
            double data_start;
            if (simultaneous) {
                data_start = 0;
            } else {
                data_start = Math.floor(ch / rows) * graph_duration;
            }

            float[] chData = getChannelData(ecgData.lead_arrange[ch]);

            if (Math.floor(ch / rows) == 0) {
                plotCalibration(page, myPaint, pagePadding, y, scale);
            }
            plotGraph(page, myPaint, ecgData.lead_name[ch], x, y, chData, data_start, graph_duration, scale);
        }
    }

    private float[] getChannelData(int ch) {
        float[] chData = new float[report_data_length];
        double[] filteredData = new double[report_data_length];
        if (settings.test_mode.equals("true")) {
            for (int i = 0; i < report_data_length; i++) {
                chData[i] = ecgData.filtered_data[i + x_position][ch];
            }
        } else {
            for (int i = 0; i < report_data_length; i++) {
                filteredData[i] = ecgData.filtered_data[i + x_position][ch];
            }
//            double[] baselineCorrectedData = filteredData;
            double[] baselineCorrectedData = baseLineRemove.Remove(filteredData);
            for (int i = 0; i < baselineCorrectedData.length - 250; i++) {
                chData[i] = (float) baselineCorrectedData[i + 250]; /* skip first 250 samples to let the corrected signal settle down */
            }
        }
        return chData;
    }

    private void plotGraph(PdfDocument.Page page, Paint myPaint, String label, float x, float y, float[] data, double data_start_sec, double data_len_sec, float scale) {
        // draw label
        myPaint.setColor(Color.BLUE);
        myPaint.setStyle(Paint.Style.FILL);
        page.getCanvas().drawText(label, x + padding, (y - y_range / 4 + line_height), myPaint);


        int data_start = (int) (data_start_sec * ecgData.sampleRatePerSec);
//        int data_start = 0;
        int data_length = (int) (data_len_sec * ecgData.sampleRatePerSec);
        // draw graph
        myPaint.setColor(black);

        float x_prev = x;
        float y_prev = y;
        float base_axis = y;


        for (int i = data_start; i < (data_start + data_length); i++) {
            y = base_axis - data[i] * y_PS_PT_per_mV * scale;
            if (page_horz_start < x && x < page_horz_stop && page_vert_start < y && y < page_vert_stop) {
                page.getCanvas().drawLine(x_prev, y_prev, x, y, myPaint);
            }
            x_prev = x;
            y_prev = y;
            x += x_PS_PT_per_sample;
        }


    }

    private float plotCalibration(PdfDocument.Page page, Paint myPaint, float x, float y, float scale) {
        myPaint.setColor(black);
        float x_prev = x;
        float y_prev = y;
        float base_axis = y;

        int calibration_length = (int) (calibration_signal_length_mm * x_sample_per_mm);

        for(int i=0 ; i < calibration_length; i++){
            if(i < calibration_length/5 || i > calibration_length*4/5){
                y = base_axis;
            } else {
                y = base_axis - val_per_mv*y_PS_PT_per_mV*scale;
            }
            page.getCanvas().drawLine(x_prev, y_prev, x, y, myPaint);
            x_prev = x;
            y_prev = y;
            x += x_PS_PT_per_sample;
        }
        return x;
    }

    private void drawBlueOutlines(PdfDocument.Page page, Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(1f);
        page.getCanvas().drawRect(page_horz_start, page_vert_start, page_horz_stop, info_rect_height, paint);
        page.getCanvas().drawRect(page_horz_start, (info_rect_height + padding), page_horz_stop, page_vert_stop, paint);

    }

    private void drawGridLines(PdfDocument.Page page, Paint paint){
        float y_start = info_rect_height + padding;
        paint.setStrokeWidth(strokeWidth);
        for(int i = 0; i<=(page_vert_stop - info_rect_height - padding)/PS_PT_per_mm ; i++){
            if(i%mmPerBox == 0){
                paint.setColor(thick_line_color);
            }else{
                paint.setColor(thin_line_color);
            }
            float y_curr = y_start + i * PS_PT_per_mm;
            page.getCanvas().drawLine(page_horz_start, y_curr, page_horz_stop, y_curr, paint);
        }

        for(int j = 0; j<=(page_horz_stop-pagePadding)/PS_PT_per_mm ; j++){
            float x_cur = page_horz_start + j * PS_PT_per_mm;
            if(j%mmPerBox == 0){
                paint.setColor(thick_line_color);
            }else{
                paint.setColor(thin_line_color);
            }
            page.getCanvas().drawLine(x_cur, y_start, x_cur, page_vert_stop, paint);
        }
    }

    private void drawBlueGridLines(PdfDocument.Page page, Paint paint, boolean skipLastRow) {
        paint.setStyle(Paint.Style.STROKE);
        float y_start = info_rect_height + padding;
        paint.setStrokeWidth(strokeWidth);
        for (int i = 0; i <= (page_vert_stop - info_rect_height - padding) / PS_PT_per_mm; i++) {
            float y_curr = y_start + i * PS_PT_per_mm;
            if (i % (y_boxes * mmPerBox) == 0) {
                paint.setColor(Color.BLUE);
                page.getCanvas().drawLine(page_horz_start, y_curr, page_horz_stop, y_curr, paint);
            }
        }

        for(int j = 0; j<=(page_horz_stop-pagePadding)/PS_PT_per_mm ; j++){
            float y_stop;
            float x_cur = page_horz_start + j * PS_PT_per_mm;
            if(j%(x_boxes*mmPerBox) == 5){
                if (j > (x_boxes * mmPerBox) && skipLastRow) {
                    y_stop = page_vert_stop - y_range;
                } else {
                    y_stop = page_vert_stop;
                }
                paint.setColor(Color.BLUE);
                page.getCanvas().drawLine(x_cur, y_start, x_cur, y_stop, paint);
            }
        }
    }

    private void drawInfo(PdfDocument.Page page, Paint paint, String reportType) {
        paint.setTextSize(9f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#000000"));

        drawGraphInfo(page, paint, reportType);

        int lineNo;
        lineNo = drawPatientInfo(page_horz_start, page_vert_start, paint, page);
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
        lineNo++;


        drawDoctorSignatureInfo(page_horz_start, page_vert_start, paint, page);

        if(ecgData.patient_photo != null) {
            drawPatientPhotoInfo(page_horz_start, page_vert_start, paint, page);
        }
        if(ecgData.patient_signature_photo != null) {
            drawPatientSignatureInfo(page_horz_start, page_vert_start, paint, page);
        }

        drawDoctorInfo(page_horz_start, page_vert_start, lineNo, paint, page);

        lineNo = drawComments(page_horz_start, page_vert_start, lineNo, paint, page);
        //drawCommentsTest(page_horz_start, page_vert_start, lineNo, paint, page);

        lineNo = drawAnalysis(page_horz_start, page_vert_start, lineNo, paint, page);

        Log.v("X START FROM :::: ", page_horz_start+" :: "+page_vert_start);
        // For ------
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
        lineNo = drawTitleText("---------------", 40, 75, lineNo, paint, page);

        //For Patient. Sign Text
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
        lineNo = drawTitleText("Patient Sign", -90, 85, lineNo, paint, page);
        //lineNo = drawDrSignText(page_horz_start, page_vert_start, lineNo, paint, page);

        // For ------
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines));
        lineNo = drawTitleText("---------------", -120, 75, lineNo, paint, page);

        //For Dr. Sign Text
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines));
        lineNo = drawTitleText("Dr. Sign", -240, 85, lineNo, paint, page);
        //lineNo = drawDrSignText(page_horz_start, page_vert_start, lineNo, paint, page);



    }

    private void drawInfo6x2(PdfDocument.Page page, Paint paint, String reportType) {
        paint.setTextSize(8f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#000000"));

        drawGraphInfo(page, paint, reportType);

        int lineNo;
//        lineNo = drawPatientDoctorInfoCompact(page_horz_start, page_vert_start, paint, page);
        lineNo = drawPatientInfo(page_horz_start, page_vert_start, paint, page);
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
        lineNo++;
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
        drawDoctorInfo(page_horz_start, page_vert_start, lineNo, paint, page);
        lineNo = drawComments6x2(page_horz_start, page_vert_start, lineNo, paint, page);
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
        lineNo++;
        lineNo = drawAnalysis6x2(page_horz_start, page_vert_start, lineNo, paint, page);
//        for (int i = 4; i < no_of_columns; i++) {
//            int x = pagePadding + i * col_width;
//            page.getCanvas().drawRect(x, page_vert_start, x, info_rect_height, paint);
//        }
    }

    private void drawGraphInfo(PdfDocument.Page page, Paint paint, String reportType) {
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.RIGHT);
        float vert_stop = appbt.res.getInteger(R.integer.report_page_height);
        // Date Time
        page.getCanvas().drawText("Acquired on: " + ecgData.date_time, page_horz_stop - padding, pagePadding - padding, paint);


        paint.setTextAlign(Paint.Align.LEFT);
        // Scale
        int yscale = (int) (10 * ecgData.graph_scale);
        String scale = "X = 25mm/s  Y = " + yscale + "mm/mV";
        page.getCanvas().drawText(scale, page_horz_start + padding, vert_stop - padding * 3, paint);

        if (!settings.test_mode.equals("true")) {
            // Active Filter
            page.getCanvas().drawText(settings.filter + " Filter", page_horz_start + 150, vert_stop - padding * 3, paint);
        }

        paint.setTextAlign(Paint.Align.CENTER);
        // Graph type
        //page.getCanvas().drawText(reportType, (page_horz_stop + pagePadding) / 2, pagePadding - padding, paint);
        page.getCanvas().drawText(reportType, (float) ((page_horz_stop + pagePadding) / 1.2), vert_stop - padding * 3, paint);

        // Credits
        //String credits = "Unconfirmed report from Dr. Cardio";
        String credits = "To be Finally Confirmed by the doctor";
        page.getCanvas().drawText(credits, (page_horz_stop + pagePadding) / 2, vert_stop - padding * 3, paint);

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(false);
    }

    private int drawPatientInfo(float x, float y, Paint paint, PdfDocument.Page page) {
        int lineno = drawTitleText("Patient Info", x, y, 0, paint, page);
        ArrayList<String> patientInfo = getPatientInfo();
        return drawText(patientInfo, x, y, lineno, paint, page);
    }

    private int drawPatientPhotoInfo(float x, float y, Paint paint, PdfDocument.Page page) {
        int lineno = drawTitleText("Patient Info", x, y, 0, paint, page);
        Log.v("Profile IMAGE PATH :: ", ecgData.patient_photo);
        File image = new File(ecgData.patient_photo);
        //File image = new File(getDocumentsStorageDir("Dr.cardio"), String.format("DoctorSignature.jpg", System.currentTimeMillis()));
        Log.v("PDF IMAGE PATH :: ", image.getPath());
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            bmOptions.inMutable = true;*/
        Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(),bmOptions);
        /*if (!bitmap.isMutable())
            bitmap = convertToMutable(this, bitmap);*/
        //bitmap.setWidth(80);
        //bitmap.setHeight(80);

        return drawBitmapPatientProfile(bitmap, x, y, lineno, paint, page);
    }

    private int drawDoctorSignatureInfo(float x, float y, Paint paint, PdfDocument.Page page) {
        int lineno = drawTitleText("Patient Info", x, y, 0, paint, page);
        //Log.v("PDF IMAGE PATH :: ", ecgData.patient_photo);
        //File image = new File(ecgData.patient_signature_photo);
        File image = new File(getAlbumStorageDir("DrCardioDocument"), String.format("DoctorSignature.jpg", System.currentTimeMillis()));
        Log.v("PDF IMAGE PATH :: ", image.getPath());
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            bmOptions.inMutable = true;*/
        Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(),bmOptions);
        /*if (!bitmap.isMutable())
            bitmap = convertToMutable(this, bitmap);*/
        //bitmap.setWidth(80);
        //bitmap.setHeight(80);

        return drawBitmapDoctorSignature(bitmap, x, y, lineno, paint, page);
    }

    private int drawPatientSignatureInfo(float x, float y, Paint paint, PdfDocument.Page page) {
        int lineno = drawTitleText("Patient Info", x, y, 0, paint, page);
        //Log.v("PDF IMAGE PATH :: ", ecgData.patient_photo);
        //File image = new File(ecgData.patient_signature_photo);
        File image = new File(getAlbumStorageDir("DrCardioDocument"), String.format("PatientSignature.jpg", System.currentTimeMillis()));
        Log.v("PDF IMAGE PATH :: ", image.getPath());
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inMutable = true;
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            bmOptions.inMutable = true;*/
        Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(),bmOptions);
        /*if (!bitmap.isMutable())
            bitmap = convertToMutable(this, bitmap);*/
        //bitmap.setWidth(80);
        //bitmap.setHeight(80);

        return drawBitmapPatientSignature(bitmap, x, y, lineno, paint, page);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Bitmap convertToMutable(Bitmap imgIn) {
        try {
            //this is the file going to use temporally to save the bytes.
            // This file will not be a image, it will store the raw image data.
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

            //Open an RandomAccessFile
            //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            //into AndroidManifest.xml file
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            // get the width and height of the source bitmap.
            int width = imgIn.getWidth();
            int height = imgIn.getHeight();
            Bitmap.Config type = imgIn.getConfig();

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes()*height);
            imgIn.copyPixelsToBuffer(map);
            //recycle the source bitmap, this will be no longer used.
            imgIn.recycle();
            System.gc();// try to force the bytes from the imgIn to be released

            //Create a new bitmap to load the bitmap again. Probably the memory will be available.
            imgIn = Bitmap.createBitmap(width, height, type);
            map.position(0);
            //load it back from temporary
            imgIn.copyPixelsFromBuffer(map);
            //close the temporary file and channel , then delete that also
            channel.close();
            randomAccessFile.close();

            // delete the temp file
            file.delete();


            imgIn.setWidth(80);
            imgIn.setHeight(80);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imgIn;
    }

    public File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        /*File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);*/
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), albumName);
        if (!file.mkdirs()) {
            Log.e("SignaturePad", "Directory not created");
        }
        return file;
    }

    public File getDocumentsStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
       /* File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), albumName);*/
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), albumName);
        if (!file.mkdirs()) {
            Log.e("SignaturePad", "Directory not created");
        }
        return file;
    }

    private void drawDoctorInfo(float x, float y, int lineNo, Paint paint, PdfDocument.Page page) {
        //String drInfo = /*Dr: */""+settings.doctor_name+ "   |   "+settings.doctor_website+ "   |   "+settings.doctor_contactno+ "   |   "+settings.doctor_address;
        String drInfo = /*Dr: */""+settings.doctor_name+ "   |   "+settings.doctor_website+ "   |   "+settings.doctor_address;
        page.getCanvas().drawText( drInfo, page_horz_start + padding, pagePadding - padding, paint);
    }

    private int drawDoctorInfo6x2(float x, float y, int lineNo, Paint paint, PdfDocument.Page page) {
        ArrayList<String> doctorInfo = getDoctorInfo6x2();
        return drawText(doctorInfo, x, y, lineNo, paint, page);
    }

    private int drawPatientDoctorInfoCompact(float x, float y, Paint paint, PdfDocument.Page page) {
        ArrayList<String> patientInfo = getPatientDoctorInfoCompact();
        return drawText(patientInfo, x, y, 0, paint, page);
    }

    private int drawAnalysis(float x, float y, int lineNo, Paint paint, PdfDocument.Page page) {
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
//        String title = "LEAD II ANALYSIS";
//        lineNo = drawTitleText(title, x, y, lineNo, paint, page);
        if (!settings.test_mode.equals("true")) {
            ArrayList<String> ecgAnalysis = getEcgAnalysis();
            lineNo = drawText(ecgAnalysis, x, y, lineNo, paint, page);
        }
//        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
//        title = "Ld      ST        SLOPE";
//        lineNo = drawTitleText(title, x, y, lineNo, paint, page);
//        drawTitleText(title, x, y, lineNo + 7, paint, page);
//        if (!settings.test_mode.equals("true")) {
//            ArrayList<String> ecgAnalysisSlope = getEcgAnalysisSlope();
//            lineNo = drawText(ecgAnalysisSlope, x, y, lineNo, paint, page);
//        }
        return lineNo;
    }

    private int drawAnalysis6x2(float x, float y, int lineNo, Paint paint, PdfDocument.Page page) {
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
//        String title = "LEAD II";
        if (!settings.test_mode.equals("true")) {
            ArrayList<String> ecgAnalysis = getEcgAnalysis();
            lineNo = drawText(ecgAnalysis, x, y, lineNo, paint, page);
//            ArrayList<String> ecgAnalysis1 = new ArrayList<String>();
//            ArrayList<String> ecgAnalysis2 = new ArrayList<String>();
//            ecgAnalysis1.add(ecgAnalysis.get(0));
//            ecgAnalysis1.add(ecgAnalysis.get(1));
//            ecgAnalysis1.add(ecgAnalysis.get(2));
//            ecgAnalysis2.add(ecgAnalysis.get(3));
//            ecgAnalysis2.add(ecgAnalysis.get(4));
//            ecgAnalysis2.add(ecgAnalysis.get(5));
//
//            lineNo = drawTitleText(title, x, y, lineNo, paint, page);
//            lineNo = drawText(ecgAnalysis1, x, y, lineNo, paint, page);
//            lineNo = drawTitleText(title, x, y, lineNo, paint, page);
//            lineNo = drawText(ecgAnalysis2, x, y, lineNo, paint, page);
        }
//        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
//        title = "Ld      ST        SLOPE";
//        lineNo = drawTitleText(title, x, y, lineNo, paint, page);
//        drawTitleText(title, x, y, lineNo + 3, paint, page);
//        drawTitleText(title, x, y, lineNo + 7, paint, page);
//        drawTitleText(title, x, y, lineNo + 11, paint, page);
//        if (!settings.test_mode.equals("true")) {
//            ArrayList<String> ecgAnalysisSlope = getEcgAnalysisSlope6x2();
//            lineNo = drawText(ecgAnalysisSlope, x, y, lineNo, paint, page);
//        }
        return lineNo;
    }

    private int drawComments(float x, float y, int lineNo, Paint paint, PdfDocument.Page page) {
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
        lineNo = drawTitleText("Comments", x, y, lineNo, paint, page);
        ArrayList<String> comments = getComments();
        return drawText(comments, x, y, lineNo, paint, page);
    }

    private int drawComments6x2(float x, float y, int lineNo, Paint paint, PdfDocument.Page page) {
        lineNo = drawTitleText("Comments", x, y, lineNo, paint, page);
        ArrayList<String> comments = getComments();
        return drawText(comments, x, y, lineNo, paint, page);
    }

    private int drawDrSignText(float x, float y, int lineNo, Paint paint, PdfDocument.Page page) {
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
        lineNo = drawTitleText("Dr. Sign", x, y, lineNo, paint, page);
        ArrayList<String> comments = new ArrayList<String>();
        comments.add("Dr. Sign");
        return drawText(comments, x+140, y+55, lineNo, paint, page);
    }

    private int drawDrSignDottedText(float x, float y, int lineNo, Paint paint, PdfDocument.Page page) {
        lineNo = lines * ((int) Math.ceil((float) lineNo / (float) lines)); /* skip remaining lines in the column & skip to start of new column */
        lineNo = drawTitleText("---------------", x, y, lineNo, paint, page);
        ArrayList<String> comments = new ArrayList<String>();
        comments.add("---------------");
        return drawText(comments, x-5, y+48, lineNo, paint, page);
    }

    private ArrayList<String> getPatientInfo() {
        ArrayList<String> patientInfo = new ArrayList<String>();
        patientInfo.add("Id : " + ecgData.patient_id);
        patientInfo.add("Name : " + ecgData.patient_name);
        patientInfo.add("Age : " + ecgData.patient_age);
        patientInfo.add("Gender : " + ecgData.patient_sex);
        //patientInfo.add("Device ID : Dr.Cardio/0119088");
        patientInfo.add("Device ID : "+ecgData.patinet_deviceid);


        return patientInfo;
    }

    private ArrayList<String> getPatientDoctorInfoCompact() {
        ArrayList<String> info = new ArrayList<String>();
        info.add(ecgData.patient_id + ". " + ecgData.patient_name);
        info.add("Age: " + ecgData.patient_age);
        info.add("Dr name: " + settings.doctor_name);
        info.add("Dr Contact: " + settings.doctor_contactno);
        return info;
    }

    private ArrayList<String> getEcgAnalysis() {
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
//        String[] pdfEcgLabels = appbt.res.getStringArray(R.array.ecg_labels);
        ArrayList<String> ecgAnalysis = new ArrayList<String>();
        ecgAnalysis.add("HR: " + (int) analysis[1][0] + "/min");
        ecgAnalysis.add("R(II): " + df.format(analysis[1][1]) + "mV");
        int rr = 0;
        if((int) analysis[1][0] > 0){
            rr = Math.round((60000 / analysis[1][0]));
        }
        ecgAnalysis.add("RR: " + rr + "ms");
        ecgAnalysis.add("PR: " + Math.round(analysis[1][7]*1000) + "ms");
        ecgAnalysis.add("QRS: " + Math.round(analysis[1][6]*1000) + "ms");
        ecgAnalysis.add("QT: " + Math.round(analysis[1][8]*1000) + "ms");
        ecgAnalysis.add("QTc: " + Math.round((analysis[1][8]/analysis[1][11])*1000) + "ms");
        ecgAnalysis.add("QT/QTc: " + df.format(analysis[1][11]));

//        for (int i = 0; i < pdfEcgLabels.length; i++) {
//            String s = appbt.res.getString(appbt.res.getIdentifier(pdfEcgLabels[i], "string", appbt.packageName)) + " : ";
//            int analysis_index = appbt.res.getInteger(appbt.res.getIdentifier(pdfEcgLabels[i], "integer", appbt.packageName));
//            if (i == 0) {
//                s += (int) analysis[1][analysis_index];
//            } else {
//                s += df.format(analysis[1][analysis_index]);
//            }
//            ecgAnalysis.add(s);
//        }
        return ecgAnalysis;
    }

    private ArrayList<String> getEcgAnalysisSlope() {
        ArrayList<String> ecgAnalysisSlope = new ArrayList<String>();
        DecimalFormat df = new DecimalFormat("#00.00");
//        df.setMaximumFractionDigits(2);
//        ecgAnalysisSlope.add(String.format("%10s", "LEAD")+String.format("%10s", "ST")+String.format("%10s", "SLOPE"));
        for (int i = 0; i < ecgData.NO_OF_CHANNELS; i++) {
            if (i > 1 && i % lines == 6) {
                ecgAnalysisSlope.add("");
                ecgAnalysisSlope.add("");
//                ecgAnalysisSlope.add(String.format("%10s", "LEAD")+String.format("%10s", "ST")+String.format("%10s", "SLOPE"));
            }
            String s = ecgData.lead_name[i] + "    " + df.format(analysis[9][i]) + "    " + df.format(analysis[10][i]);
            ecgAnalysisSlope.add(s);
        }
        return ecgAnalysisSlope;
    }

    private ArrayList<String> getEcgAnalysisSlope6x2() {
        ArrayList<String> ecgAnalysisSlope = new ArrayList<String>();
        DecimalFormat df = new DecimalFormat("#00.00");
//        df.setMaximumFractionDigits(2);
//        ecgAnalysisSlope.add(String.format("%10s", "LEAD")+String.format("%10s", "ST")+String.format("%10s", "SLOPE"));
        for (int i = 0; i < ecgData.NO_OF_CHANNELS; i++) {
            if (i > 1 && i % 3 == 0) {
                ecgAnalysisSlope.add("");
//                ecgAnalysisSlope.add(String.format("%10s", "LEAD")+String.format("%10s", "ST")+String.format("%10s", "SLOPE"));
            }
            String s = ecgData.lead_name[i] + "  " + df.format(analysis[9][i]) + "    " + df.format(analysis[10][i]);
            ecgAnalysisSlope.add(s);
        }
        return ecgAnalysisSlope;
    }

    private ArrayList<String> getDoctorInfo() {
        ArrayList<String> doctorInfo = new ArrayList<String>();
        doctorInfo.add("Dr name: " + settings.doctor_name);
        doctorInfo.add("Dr Contact: " + settings.doctor_contactno);
        return doctorInfo;
    }

    private ArrayList<String> getDoctorInfo6x2() {
        ArrayList<String> doctorInfo = new ArrayList<String>();
        doctorInfo.add("" + settings.doctor_name); //Dr:
        doctorInfo.add("" + settings.doctor_contactno); //Dr #:
        return doctorInfo;
    }

    private ArrayList<String> getComments() {
        ArrayList<String> comment = new ArrayList<String>();
        String data = ecgData.patient_comments;
        //Log.v("Patient Comment Data :::: ", ""+data);
        //.split("(?<=\\G.{"+maxLength+"})");
//        int max = (int) (maxLength * 2.5);
//        Pattern p = Pattern.compile("\\G\\s*(.{1," + maxLength + "})(?=\\s|$)", Pattern.DOTALL);
//        Matcher m = p.matcher(data);

//        while (m.find()) {
//            comment.add(m.group(1));
//        }
        /* SP int len = data.toCharArray().length;
        for (int i = 0; i < len; i+=maxLength) {
            if(len < i + maxLength) {
                comment.add(data.substring(i, len));
            } else {
                comment.add(data.substring(i, i + maxLength));
            }

        }*/

        for (String line: data.split("\n")) {
            //canvas.drawText(line, x, y, paint);
            //y += paint.descent() - paint.ascent();
            comment.add(line);
        }

        return comment;
    }


    private int drawTitleText(String string, float x_start, float y_start, int line_start, Paint paint, PdfDocument.Page page) {
        paint.setColor(Color.BLUE);
        ArrayList<String> title = new ArrayList<>();
        title.add(string);
        line_start = drawText(title, x_start, y_start, line_start, paint, page);
        paint.setColor(black);
        return line_start;
    }

    private int drawText(ArrayList<String> stringArray, float x_start, float y_start, int line_start, Paint paint, PdfDocument.Page page) {
        int i;
        float x, y;
        for (i = 0; i < stringArray.size(); i++) {
            int lineNo = i + line_start;
            x = (float) (x_start + padding + Math.floor(lineNo / lines) * col_width);
            y = y_start + padding + (lineNo % lines + 1) * line_height;
            page.getCanvas().drawText(stringArray.get(i), x, y, paint);
        }
        return line_start + i;
    }

    private int drawTextForComment(ArrayList<String> stringArray, float x_start, float y_start, int line_start, Paint paint, PdfDocument.Page page) {
        int i;
        float x, y;
        /*for (i = 0; i < stringArray.size(); i++) {
            int lineNo = i + line_start;
            x = (float) (x_start + padding + Math.floor(lineNo / lines) * col_width);
            y = y_start + padding + (lineNo % lines + 1) * line_height;
            page.getCanvas().drawText(stringArray.get(i), x, y, paint);
        }*/

        for (String line: ecgData.patient_comments.split("\n")) {
            int lineNo = 0 + line_start;
            x = (float) (x_start + padding + Math.floor(lineNo / lines) * col_width);
            y = y_start + padding + (lineNo % lines + 1) * line_height;
            page.getCanvas().drawText(line, x, y, paint);
            y += paint.descent() - paint.ascent();
        }
        return line_start + 0;
    }

    private int drawBitmapPatientProfile(Bitmap bitmap, float x_start, float y_start, int line_start, Paint paint, PdfDocument.Page page) {
        int i;
        float x, y;
        int lineNo = line_start;
        x = (float) (x_start + padding + Math.floor(lineNo / lines) * col_width);
        y = y_start + padding + (lineNo % lines + 1) * line_height;
        //bitmap= bitmap.copy(Bitmap.Config.ARGB_8888, true);
        //bitmap.setWidth(60);
        //bitmap.setHeight(60);

        bitmap = Bitmap.createScaledBitmap(
                bitmap, 60, 60, true);

        page.getCanvas().drawBitmap(bitmap, x+150, 30, paint);

        //page.getCanvas().drawBitmap(bitmap, /*x, y, paint*//*new Rect(0, 0, 40, 40)*/null, new RectF(0, 0, 40, 40), paint);
        return line_start;
    }

    private int drawBitmapDoctorSignature(Bitmap bitmap, float x_start, float y_start, int line_start, Paint paint, PdfDocument.Page page) {
        int i;
        float x, y;
        int lineNo = line_start;
        float vert_stop = appbt.res.getInteger(R.integer.report_page_height);
        x = (float) (x_start + padding + Math.floor(lineNo / lines) * col_width);
        y = y_start + padding + (lineNo % lines + 1) * line_height;

        if(bitmap != null) {
            bitmap = Bitmap.createScaledBitmap(
                    bitmap, 100, 50, true);

            page.getCanvas().drawBitmap(bitmap, x+650, 30, paint);
        }


        //page.getCanvas().drawBitmap(bitmap, /*x, y, paint*//*new Rect(0, 0, 40, 40)*/ new Rect(0, 0, 40, 40), new RectF(0, 0, 40, 40), paint);
        return line_start;
    }

    private int drawBitmapPatientSignature(Bitmap bitmap, float x_start, float y_start, int line_start, Paint paint, PdfDocument.Page page) {
        int i;
        float x, y;
        int lineNo = line_start;
        float vert_stop = appbt.res.getInteger(R.integer.report_page_height);
        x = (float) (x_start + padding + Math.floor(lineNo / lines) * col_width);
        y = y_start + padding + (lineNo % lines + 1) * line_height;

        bitmap = Bitmap.createScaledBitmap(
                bitmap, 100, 60, true);

        Log.v("Height of Signature :: ", ""+y);

        page.getCanvas().drawBitmap(bitmap, x+550, 30, paint);
        //page.getCanvas().drawBitmap(bitmap, /*x, y, paint*//*new Rect(0, 0, 40, 40)*/ new Rect(0, 0, 40, 40), new RectF(0, 0, 40, 40), paint);
        return line_start;
    }
}
