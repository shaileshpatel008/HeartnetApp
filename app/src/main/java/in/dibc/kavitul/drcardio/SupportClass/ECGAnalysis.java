package in.dibc.kavitul.drcardio.SupportClass;

import android.util.Log;

import static java.lang.Math.abs;

import in.dibc.kavitul.drcardio.Model.ecgData;
import baseline_remove1.Baseline_remove1;

public class ECGAnalysis {
    private final float FLT_MAX = 0;
    private final String TAG = "ECGAnalysis";
    private final int   MAX_LEADS = 12,
            PARAMETERS = 12,
            NO_OF_PEAKS = 12,
            OFFSET = 2500, /* samples to skip */
            MAX_ECG_SAMPLES = 2400,
            TEMP_MAX_ECG_SAMPLES = 2500,
            SAMPLING_RATE = 500,
            WINDOWSIZE_MOV_AVG = 75,
            P_WINDOW = 130,
            QRS_WINDOW = 60,
            Q_WINDOW = 40,
            S_WINDOW = 40,
            P_STARTWINDOW = 30,
            T_STARTWINDOW = 60,
            T_ENDWINDOW = 60,
            THRESHOLD = 50, /* 50 for teleecg data and 75 for CSE data */
            SKIPCOUNT = 20,
            LEAD = 1,
            R_INDEX = 0,
            Q_INDEX = 1,
            S_INDEX = 2,
            QRS_ST_INDEX = 3,
            QRS_EN_INDEX = 4,
            P_INDEX = 5,
            P_ST_INDEX = 6,
            P_EN_INDEX = 7,
            T_INDEX = 8,
            T_ST_INDEX = 9,
            T_EN_INDEX = 10;
    private int gain = 6;
    private static final float QTC = 0.55f; //Corrected QT interval assumed to be 0.45 sec
    private final float val_per_mv = settings.val_per_mv;
    private final int[] index_map = {11, 0, 1, 7, 9, 8, 10, 6, 2, 3, 4, 5};
    private final String[] lead_name = {"LEAD_I", "LEAD_II", "LEAD_III", "LEAD_aVR", "LEAD_aVL", "LEAD_aVF", "LEAD_V1", "LEAD_V2", "LEAD_V3", "LEAD_V4", "LEAD_V5", "LEAD_V6"};
    private final float[][] ecgDataArray = new float[MAX_LEADS][TEMP_MAX_ECG_SAMPLES]; /* multiarr */
    private int[][] flag_register = new int[MAX_LEADS][NO_OF_PEAKS];
    public int[][][] multiLeadData = new int[MAX_LEADS][PARAMETERS][NO_OF_PEAKS];
    public float[][] analysisArray = new float[MAX_LEADS][PARAMETERS];
    private int sum;
    boolean rawDataReady = false;

    float scaleFactorForData = 20f;
    private final Baseline_remove1 baseLineRemove = new Baseline_remove1();


    public ECGAnalysis(float[][] ecg_data, int startPoint, int g) {
        gain = g;
        if (!settings.test_mode.equals("true")) {
            sampleDataForAnalysis(ecg_data, startPoint);
        }
    }

    private void sampleDataForAnalysis(float[][] ecg_data, int startPoint) {
        if (ecgData.rawDataCount - startPoint < TEMP_MAX_ECG_SAMPLES) {
            startPoint = ecgData.rawDataCount - TEMP_MAX_ECG_SAMPLES;
        }

        int report_data_length = 6500;
        if (ecgData.rawDataCount - startPoint < report_data_length) {
            startPoint = ecgData.rawDataCount - report_data_length;
        }
        if(startPoint < 1){
            return;
        }
        for (int ch = 0; ch < 12; ch++) {
            float[] chData = new float[report_data_length];
            double[] filteredData = new double[report_data_length];
             for (int i = 0; i < report_data_length; i++) {
                filteredData[i] = ecg_data[i + startPoint][ch];
            }
            double[] baselineCorrectedData = baseLineRemove.Remove(filteredData);

            for (int j = 0; j < TEMP_MAX_ECG_SAMPLES; j++) {
                ecgDataArray[index_map[ch]][j]= (float) (baselineCorrectedData[j+250]*scaleFactorForData);
            }
        }
        rawDataReady = true;
    }

    public int analyse() {
        if (!rawDataReady) {
            return 1;
        }

        multiLeadData = new int[MAX_LEADS][PARAMETERS][NO_OF_PEAKS];
        int[][] leadData = new int[PARAMETERS][NO_OF_PEAKS];
        flag_register = new int[MAX_LEADS][NO_OF_PEAKS];

        float[] ecgVal = ecgDataArray[LEAD];

        int[] peakIndex = fiducial_peakdetection(ecgVal);

        int[] R_peakindex = R_peaks(ecgVal, peakIndex);
        float avg_heartrate = rr_intverval_HR(R_peakindex);
        Log.d(TAG, "analyse: HR"+avg_heartrate);
        int no_of_pks = peak_count(R_peakindex);
        int dispeak = no_of_pks - 2;

        if (no_of_pks < 3) {   /* 20210713 - DIBC KN : Adjusting limits to prevent index out of bounds : no_of_peaks==0 changed to no_of_peaks<3 as in parts of calculations, index no_of_peaks-3 is used. */
            return 2;
        }

        float[] f_ecgVal = lpf_ecg40(ecgVal);
        float min=minimum(f_ecgVal);
        float max=maximum(f_ecgVal);
        float[] simval = derivative_bpf(f_ecgVal);
        leadData[Q_INDEX] = Q_peakdetect(f_ecgVal,Q_WINDOW,simval,R_peakindex);
        leadData[QRS_ST_INDEX] = QRS_start(leadData[Q_INDEX],simval);
        float rr_factor=compute_rr_factor(avg_heartrate);
        int pwindow = (int) (P_WINDOW / rr_factor);
        leadData[P_INDEX] = ppeak_detect(f_ecgVal, max, min, R_peakindex, pwindow, leadData[QRS_ST_INDEX]);
        leadData[S_INDEX] = S_peakdetect(f_ecgVal, simval, S_WINDOW, R_peakindex);
        leadData[P_ST_INDEX] = P_start(leadData[P_INDEX], f_ecgVal, R_peakindex, simval, leadData[QRS_ST_INDEX]);
        leadData[P_EN_INDEX] = P_end(leadData[P_INDEX], f_ecgVal, R_peakindex, leadData[QRS_ST_INDEX], simval);
        leadData[QRS_EN_INDEX] = QRS_end(leadData[S_INDEX],f_ecgVal,simval);
        int twindow = (int) T_windowCorrection(R_peakindex, no_of_pks);
        leadData[T_INDEX] = tpeak_detect(max,min,f_ecgVal,R_peakindex,twindow,leadData[QRS_EN_INDEX],leadData[QRS_ST_INDEX], no_of_pks);
        leadData[T_ST_INDEX] = T_start(leadData[T_INDEX],f_ecgVal,simval,leadData[QRS_EN_INDEX]);
        leadData[T_EN_INDEX] = T_end(leadData[T_INDEX],f_ecgVal,simval,leadData[QRS_EN_INDEX], leadData[P_ST_INDEX]);
        float r_ampL2 = peak_amplitude(R_peakindex, f_ecgVal, dispeak, no_of_pks, leadData[P_ST_INDEX], leadData[P_EN_INDEX], leadData[QRS_ST_INDEX], leadData[QRS_EN_INDEX], leadData[T_EN_INDEX]);

        leadData[R_INDEX] = R_peakindex;
        multiLeadData[LEAD] = leadData;

        for (int LEAD=0;LEAD<12;LEAD++)
        {
            ecgVal = ecgDataArray[LEAD];
            f_ecgVal = lpf_ecg40(ecgVal);
            leadData = new int[PARAMETERS][NO_OF_PEAKS];
            min=minimum(f_ecgVal);
            max=maximum(f_ecgVal);
            float ST_level=0, ST_seg_slope=0;
            int interval = 0;
            float PQ_avginterval=0, PQ_avgsegment=0,QRS_avginterval=0, QT_avginterval=0;
            float q_amp = 0, p_amp = 0, r_amp = 0, s_amp = 0, t_amp = 0;
            if (no_of_pks!=0)
            {
                simval = derivative_bpf(f_ecgVal);
                leadData[Q_INDEX] = Q_peakdetect(f_ecgVal,Q_WINDOW,simval,R_peakindex);
                leadData[QRS_ST_INDEX] = QRS_start(leadData[Q_INDEX],simval);
                avg_heartrate=rr_intverval_HR(R_peakindex);
                rr_factor=compute_rr_factor(avg_heartrate);
                pwindow= (int) (P_WINDOW/rr_factor);
                leadData[P_INDEX] = ppeak_detect(f_ecgVal,max,min,R_peakindex,pwindow,leadData[QRS_ST_INDEX]);
                leadData[P_ST_INDEX] = P_start(leadData[P_INDEX],f_ecgVal,R_peakindex,simval,leadData[QRS_ST_INDEX]);
                leadData[P_EN_INDEX] = P_end(leadData[P_INDEX],f_ecgVal,R_peakindex,leadData[QRS_ST_INDEX],simval);
                leadData[S_INDEX] = S_peakdetect(f_ecgVal,simval,S_WINDOW,R_peakindex);
                leadData[QRS_EN_INDEX] = QRS_end(leadData[S_INDEX],f_ecgVal,simval);
                twindow = (int) T_windowCorrection(R_peakindex, no_of_pks);
                leadData[T_INDEX] = tpeak_detect(max,min,f_ecgVal,R_peakindex,twindow,leadData[QRS_EN_INDEX],leadData[QRS_ST_INDEX], no_of_pks);
                leadData[T_ST_INDEX] = T_start(leadData[T_INDEX],f_ecgVal,simval,leadData[QRS_EN_INDEX]);
                leadData[T_EN_INDEX] = T_end(leadData[T_INDEX],f_ecgVal,simval,leadData[QRS_EN_INDEX], leadData[P_ST_INDEX]);

                float avg_dc = isoelectric_line(dispeak, f_ecgVal, leadData[P_ST_INDEX], leadData[P_EN_INDEX], leadData[QRS_ST_INDEX], leadData[QRS_EN_INDEX], leadData[T_EN_INDEX]);
                ST_level=ST_segment(f_ecgVal, leadData[QRS_EN_INDEX], leadData[T_ST_INDEX], avg_dc, no_of_pks);
                ST_seg_slope=ST_slope(f_ecgVal, leadData[QRS_EN_INDEX], leadData[T_ST_INDEX], no_of_pks);
                r_amp=peak_amplitude(R_peakindex,f_ecgVal,dispeak, no_of_pks, leadData[P_ST_INDEX], leadData[P_EN_INDEX], leadData[QRS_ST_INDEX], leadData[QRS_EN_INDEX], leadData[T_EN_INDEX]);//r peak amplitude in L2
                interval= R_peakindex[no_of_pks-1]-R_peakindex[no_of_pks-2];

                multiLeadData[LEAD] = leadData;
                for (int i=0;i<NO_OF_PEAKS;i++)
                {
                    if ((leadData[Q_INDEX][i]!=0)&&(leadData[S_INDEX][i]!=0)&&(leadData[P_INDEX][i]!=0)&&(leadData[T_INDEX][i]!=0)) {
                        flag_register[LEAD][i] = 1;
                    }
                } //for

                if (flag_register[LEAD][no_of_pks-2]==1) dispeak=no_of_pks-2;
                else if (flag_register[LEAD][no_of_pks-3]==1) dispeak=no_of_pks-3;

                r_ampL2=peak_amplitude(R_peakindex,f_ecgVal,dispeak, no_of_pks, leadData[P_ST_INDEX], leadData[P_EN_INDEX], leadData[QRS_ST_INDEX], leadData[QRS_EN_INDEX], leadData[T_EN_INDEX]);

                //	PQ,ORS,QT Intervals
                PQ_avginterval=avg_peakinterval(leadData[QRS_ST_INDEX],leadData[P_ST_INDEX],LEAD,dispeak);
                PQ_avgsegment=avg_peakinterval(leadData[QRS_ST_INDEX],leadData[P_EN_INDEX],LEAD,dispeak);
                QRS_avginterval=avg_peakinterval(leadData[QRS_EN_INDEX],leadData[QRS_ST_INDEX],LEAD,dispeak);
                QT_avginterval=avg_peakinterval(leadData[T_EN_INDEX],leadData[QRS_ST_INDEX],LEAD,dispeak);
                // Amplitude
                q_amp=peak_amplitude(leadData[Q_INDEX],f_ecgVal,dispeak, no_of_pks, leadData[P_ST_INDEX], leadData[P_EN_INDEX], leadData[QRS_ST_INDEX], leadData[QRS_EN_INDEX], leadData[T_EN_INDEX]);
                p_amp=peak_amplitude(leadData[P_INDEX],f_ecgVal,dispeak, no_of_pks, leadData[P_ST_INDEX], leadData[P_EN_INDEX], leadData[QRS_ST_INDEX], leadData[QRS_EN_INDEX], leadData[T_EN_INDEX]);
                s_amp=peak_amplitude(leadData[S_INDEX],f_ecgVal,dispeak, no_of_pks, leadData[P_ST_INDEX], leadData[P_EN_INDEX], leadData[QRS_ST_INDEX], leadData[QRS_EN_INDEX], leadData[T_EN_INDEX]);
                t_amp=peak_amplitude(leadData[T_INDEX],f_ecgVal,dispeak, no_of_pks, leadData[P_ST_INDEX], leadData[P_EN_INDEX], leadData[QRS_ST_INDEX], leadData[QRS_EN_INDEX], leadData[T_EN_INDEX]);
                double QT_C = QT_avginterval * 0.002 / Math.sqrt(interval * 0.002);
                double ratio_QT_QTC = QT_avginterval * 0.002 / QT_C;

                analysisArray[LEAD][0]=avg_heartrate;
                analysisArray[LEAD][1]=r_amp/(val_per_mv* scaleFactorForData);
                analysisArray[LEAD][2]=p_amp/(val_per_mv* scaleFactorForData);
                analysisArray[LEAD][3]=q_amp/(val_per_mv* scaleFactorForData);
                analysisArray[LEAD][4]=s_amp/(val_per_mv* scaleFactorForData);
                analysisArray[LEAD][5]=t_amp/(val_per_mv* scaleFactorForData);
                analysisArray[LEAD][6]= (float) (QRS_avginterval*0.002);
                analysisArray[LEAD][7]= (float) (PQ_avginterval*0.002);
                analysisArray[LEAD][8]= (float) (QT_avginterval*0.002);
                analysisArray[LEAD][9]=ST_level/(val_per_mv* scaleFactorForData);
                analysisArray[LEAD][10]= (float) (ST_seg_slope/0.002);
                analysisArray[LEAD][11] = (float) ratio_QT_QTC;

//                String result = "lead"+LEAD;
//                for (int ii = 0; ii < 11; ii++) {
//                    result += "    "+analysisArray[LEAD][ii];
//                }
//                Log.d(TAG, "analyse: "+result);
//                display_table(panel,j); // C Panel Output
            } //if  pks


//                if (j==current_lead)
            { // C Panel Output
//                    displayValues(panel,interval,q_amp,p_amp,s_amp,t_amp,r_amp,avg_heartrate,ST_level,ST_seg_slope,r_peaks);
//                    plotpeaks(panel,peakindex,current_lead,p_amp,dispeak);
            }//if
            // C Panel Output
//                displayReport(j,panel1, avg_heartrate,interval,PQ_avginterval,QRS_avginterval,QT_avginterval,ST_level,ST_seg_slope,r_ampL2);
//                String result = " j:"+LEAD+"  avg_heartrate:"+ avg_heartrate+" interval:"+interval+" PQ_avginterval:"+PQ_avginterval+" QRS_avginterval:"+QRS_avginterval+" QT_avginterval:"+QT_avginterval+" PQ_avgsegment:"+PQ_avgsegment+" ST_level:"+ST_level+" ST_seg_slope:"+ST_seg_slope+" r_ampL2:"+r_ampL2;
//                Log.d("AnalyseECG", "analyse_MGM: "+result);
        }//for

//            Axis(panel,panel1,R_peakindexL2,dispeak);
        return 0;
    }

    private int[] fiducial_peakdetection(float[] ecgval1) {
        float[] bpf_val = casacaded_bpf(ecgval1);
        float[] deri_val = derivative_bpf(bpf_val);
        float[] square_val = square_values_in_array(deri_val);
        float[] mavg_val = mov_avg_window(WINDOWSIZE_MOV_AVG,square_val);
        int[] peakIndex = fiducial_peak_detect(THRESHOLD,SKIPCOUNT,mavg_val);

        return peakIndex;
    }
    //aplication of low pass filter followed by High pass filter (ie band pass filter
    private float[] casacaded_bpf(float[] ecg_val) {
        float[] casabpf_val = new float[MAX_ECG_SAMPLES];

        float[] hpf_val = hpf(ecg_val);
        casabpf_val = lpf(hpf_val);

        return casabpf_val;
    }

    private float[] lpf(float[] ecg_val) {
        float[] lpf_val = new float[MAX_ECG_SAMPLES],
                x = new float[MAX_ECG_SAMPLES+2],
                y = new float[MAX_ECG_SAMPLES+2];

        for (int i=2;i<MAX_ECG_SAMPLES+2;i++) {
            x[i] = ecg_val[i - 2];
        }
        for (int i=2;i<MAX_ECG_SAMPLES+2;i++) {
            y[i] = (float) ((1.6128 * y[i - 1]) - (0.6764 * y[i - 2]) + (0.0159 * x[i]) + (0.0159 * 2 * x[i - 1]) + (0.0159 * x[i - 2]));//22Hz with gain}
        }

        for (int i=2;i<MAX_ECG_SAMPLES+2;i++) {
            lpf_val[i-2]=y[i];
        }
        return lpf_val;
    }
    //12Hz High Pass flter operation. Equ  y(n)= x(n) - 2*x(n-1) + x(n-2) + 1.7874*y(n-1) - 0.8080*y(n-2)  //x coefficient multiplied by gain 0.8989
    private float[] hpf(float[] ecg_val) {
        float[] hpf_val = new float[MAX_ECG_SAMPLES],
                x = new float[TEMP_MAX_ECG_SAMPLES+2],
                y = new float[TEMP_MAX_ECG_SAMPLES+2];

        for (int i=2;i<TEMP_MAX_ECG_SAMPLES+2;i++) {
            x[i]=ecg_val[i-2];
        }
        for (int i=2;i<TEMP_MAX_ECG_SAMPLES+2;i++) {
            y[i]= (float) ((0.8989*x[i])-(0.8989*2*x[i-1])+(0.8989*x[i-2])+(1.7874*y[i-1])-(0.8080*y[i-2])); //12Hz with gain
        }

        for (int i=0;i<TEMP_MAX_ECG_SAMPLES-100;i++) {
            hpf_val[i]=y[i+102];
        }
        return hpf_val;
    }


    //filtered signal is further differenciated using equation y(n)= (2*x(n) + x(n-1) - x(n-3) - 2*x(n-4))/8
// A 4 point derivative is applied to the input signal
    private float[] derivative_bpf(float[] bpf_val) {
        float[] deri_val = new float[MAX_ECG_SAMPLES+4],
                x = new float[MAX_ECG_SAMPLES+4],
                y = new float[MAX_ECG_SAMPLES+4];

        for (int i=4;i<MAX_ECG_SAMPLES+4;i++) {
            x[i] = bpf_val[i - 4];
        }
        for (int i=4;i<MAX_ECG_SAMPLES+4;i++) {
            y[i] = ((2 * x[i]) + (x[i - 1]) - (x[i - 3]) - (2 * x[i - 4])) / 8;
        }

        for (int i=4;i<MAX_ECG_SAMPLES+4;i++) {
            deri_val[i - 4] = y[i];
        }
        return deri_val;
    }


    private float[] square_values_in_array(float[] arr){
        float[] sq_arr = new float[arr.length];
        for (int i=0;i<(MAX_ECG_SAMPLES);i++) {
            sq_arr[i] = arr[i] * arr[i];
        }
        return sq_arr;
    }
    private float[] mov_avg_window(int window_size, float[] squval)

    {
        sum=0;

        float[] mw_val = new float[MAX_ECG_SAMPLES],
                tempval = new float[MAX_ECG_SAMPLES+WINDOWSIZE_MOV_AVG];

        for (int i=0;i<(MAX_ECG_SAMPLES);i++) {
            tempval[i] = squval[i];
        }
        for (int i=0;i<(MAX_ECG_SAMPLES-5);i++)
        {
            mw_val[i]=w_avg(i,window_size,tempval); //avg value of the window is stored in first point
        }
        return mw_val;
    }
    //is used for window averaging of size samples. sum is global, a new sample value added to the window is summed
    //subtracting a sample value which goes away from window
// squared value of differentiated signal is passed as parameter to be averaged
    private float w_avg(int i, int size, float[] val) {
        float avg=0;
        if (i==0)	 //for 1st sample
        {
            for (int j=i;j<=size+i;j++) {
                sum += val[j];
            }
        }
        else  //for 2nd onwards sample
        {
            sum+=val[size+i];
            sum-=val[i-1];
        }
        avg=sum/size;
        return avg;
    }

    //tentative r peak detection   : QRS fiducial
//peaksindex[] contain sample number of fiucial positions.In this function we find the peak which is greater than threshold (10 in TeleECGdata and 50 in CSE data).
//If the peak is found skip the decreasing trend of the peak. The small peak found in the decreasing trend is skipped. The peak is registered when its value is greater than 50% of temporary peak value.
//If the registered peak value is greater than threshold, threshold is updated as per signal peak equation else threshold is updated as per noise peak equation.
//The equation are as per PanTomkins algorithm. spk= 0.125*peak_val+0.875*spk;npk= 0.125*peak_val+0.875*npk;threshold=npk+0.5*(spk+npk);
    private int[] fiducial_peak_detect(float threshold, int skipcount, float[] sim_val) {
        float spk = 0, npk = 0, peak_val,temp_pk,tempval;
        int i=0,j = 0, count, count1, peak_index;
        int[] peakindex = new int[MAX_ECG_SAMPLES];

        boolean skip_flag;

        while (i<(MAX_ECG_SAMPLES-2))
        {
            i=i+1;
            if ((sim_val[i]<=sim_val[i-1])&&(sim_val[i]>threshold))
            {
                peak_index=i-1;
                skip_flag=true;
                temp_pk= (float) (0.5*sim_val[peak_index]);
                count=skipcount;
                while ((count>0))
                {
                    if ((sim_val[i+1]<(sim_val[i]+0.05))&&(sim_val[i+1]>threshold))
                    //0.05 is added to avoid same sample values and very small rising peaks in the search
                    {
                        tempval=sim_val[i];
                        if (tempval>=sim_val[peak_index]) {
                            peak_index = i;
                        }
                        count--;
                        i++;					 //count 25 samples decreasing trend
                    }
                    else {
                        break;
                    }
                }
                //peak detected on the fallin edge. Now the tail part has to be skipped. Also if there is a
                //tendency to raise, this has to be skipped if it is not a genuine peak
                while ((skip_flag) && (i < (MAX_ECG_SAMPLES)))	  //skipping decreasing trend
                {
                    if (sim_val[i]<=sim_val[i-1])   //decreasing tendency if true
                    {
                        i++;
                        if(count>0) {
                            count--;
                        }
                        if (i>=(MAX_ECG_SAMPLES-2)) {
                            break;
                        }
                    }
                    else
                    {
                        count1=1;
                        while (sim_val[i]>=sim_val[i-1])			// count if there is increasing trend
                        {
                            tempval=sim_val[i];
                            if (tempval>=sim_val[peak_index]) {
                                peak_index = i;
                            }
                            count1++;
                            i++;
                            if (i>=(MAX_ECG_SAMPLES-2)) {
                                break;
                            }
                        }   //if increasing trend is less than 10 samples continue skipping decreasing trend
                        if (count1>=10) skip_flag=false;
                    } //else
                } //while
//checking if the sample value  is less than 50% of temporary detected temporary peak value  which confirms
//a peak
                if ((i<(MAX_ECG_SAMPLES-2))&&((sim_val[i+1]<=temp_pk)||(count==0)))
                {   peak_val=sim_val[peak_index];								 //registering peak value
                    skip_flag=true;
                    if (peak_val>threshold)
                    {
                        spk= (float) (0.125*peak_val+0.875*spk);
                        peakindex[j]=peak_index;								 //registering peak index
                        j++;
                    }
                    else npk= (float) (0.125*peak_val+0.875*npk);
                    threshold= (float) (npk+0.5*(spk+npk));							// updating threshold
                }  //if
            }//if
        }
        return peakindex;
    }

    private int[] R_peaks(float[] ecgval, int[] peakindex)
    {
        float[] lpf_val = lpf_ecg40(ecgval);  //filtered signal low pass of 40HZ
        int[] r_peakindex = peak_detect_QRS(lpf_val,QRS_WINDOW,peakindex);
        return r_peakindex;
    }


    // A low pass filter with 40Hz cut off is applied to the ECG data prior to locating end of P,Q,R,S,T waves.
//Equation y(n)= 1.3073*y(n-1) - 0.4918*y(n-2) + x(n) + 2*x(n-1) + x(n-2) //x coefficient multiplied by gain 0.0461
//40Hz LPF x coefficient multiplied with gain
    private float[] lpf_ecg40(float[] ecg_val) {
        float[] lpf_ecgval = new float[MAX_ECG_SAMPLES+2],
                x = new float[TEMP_MAX_ECG_SAMPLES+2],
                y = new float[TEMP_MAX_ECG_SAMPLES+2];

        for (int i=2;i<TEMP_MAX_ECG_SAMPLES+2;i++) {
            x[i] = ecg_val[i - 2];
        }

        for (int i=2;i<TEMP_MAX_ECG_SAMPLES+2;i++) {
            y[i] = (float) ((1.3073 * y[i - 1]) - (0.4918 * y[i - 2]) + (0.0461 * x[i]) + (0.0461 * 2 * x[i - 1]) + (0.0461 * x[i - 2]));
        }
        for (int i=0;i<TEMP_MAX_ECG_SAMPLES-100;i++)
        {
            lpf_ecgval[i]=y[i+102];
        }
        return lpf_ecgval;
    }
    //The peakindex[] contain sample index corresponding to fiducials.
//parameters passed- filtered ECG, window for searching actual R peaks, fiducial peaks,R_peakindex[] returns the index of actual R peaks
//With reference to peakindex1 (fiducial peak positions), peak is searched within window of 60 samples.
//The search begins 2 samples before fiducial peak position and within window of 60 samples highest peak is saved as R peak.R peak index is saved in R_peakindex array.
    private int[] peak_detect_QRS(float[] ecg_val, int win1, int[] peakindex1)
    {
        int k,m,q;
        int[] R_peakindex = new int[NO_OF_PEAKS];
        float maxval=0;
        float minval=0;

        q=2;

        for(int j=0;j<MAX_ECG_SAMPLES;j++)
        {
            if (ecg_val[j]<minval) {
                minval = ecg_val[j];
            }
        } //to detect minimum value in the ecg signal

        for(int j=0;j<NO_OF_PEAKS;j++) 	 //window from (fiducial point - 2) to (fiducial point +50) samples
        {
            if (peakindex1[j]>0)
            {
                k=peakindex1[j]; //fiducial point
                maxval=minval;
                //maximum point in the window
                for (m=1;m<=win1;m++)
                {
                    if ((k < MAX_ECG_SAMPLES - q) && (k > q + 9) && ecg_val.length > (k - q + 9))/* 20210713 - DIBC KN : Added ecg_val.length > (k-q+9) to prevent index out of bound */ {
                        if ((ecg_val[k - q] < ecg_val[k - q + 1]) && (ecg_val[k - q] > ecg_val[k - q - 1])) {
                            if ((ecg_val[k - q + 1] > maxval) && (ecg_val[k - q + 1] > (ecg_val[k - q + 9] + 5)) && (ecg_val[k - q + 1] > (ecg_val[k - q - 7] + 5))) //for peak,checing lowervalue in +/- 8 samples
                            //{if ((fabs(fabs(ecg_val[k-q+1])-fabs(ecg_val[k-q+9]))>5)&& (fabs(fabs(ecg_val[k-q+1])-fabs(ecg_val[k-q-7]))>5))
                            {
                                maxval = ecg_val[k - q + 1];           //registering maximum value
                                R_peakindex[j] = k - q + 1;      //registering R peak index
                            }
                            //}
                        } //if
                        else if ((ecg_val[k-q]>ecg_val[k-q+1])&&(ecg_val[k-q]> ecg_val[k-q-1]))
                        {
                            if((ecg_val[k-q]>maxval)&&(ecg_val[k-q]>(ecg_val[k-q+8]+5))&&(ecg_val[k-q]>(ecg_val[k-q-8]+5))) //for peak,checing lowervalue in +/- 8 samples
                            //{if ((fabs(fabs(ecg_val[k-q])-fabs(ecg_val[k-q+8]))>5)&& (fabs(fabs(ecg_val[k-q])-fabs(ecg_val[k-q-8]))>5))
                            {  maxval= ecg_val[k-q];
                                R_peakindex[j]=k-q;
                            } //if
                            //}
                        }//if
                        else if ((ecg_val[k-q]>ecg_val[k-q+1])&&(ecg_val[k-q]<ecg_val[k-q-1]))
                        {
                            if((ecg_val[k-q-1]>maxval)&&(ecg_val[k-q-1]>(ecg_val[k-q+7]+5))&&(ecg_val[k-q-1]>(ecg_val[k-q-9]+5))) //for peak,checing lowervalue in +/- 8 samples
                            //{if ((fabs(fabs(ecg_val[k-q-1])-fabs(ecg_val[k-q+7]))>5)&& (fabs(fabs(ecg_val[k-q-1])-fabs(ecg_val[k-q-9]))>5))
                            {   maxval= ecg_val[k-q-1];
                                R_peakindex[j]=k-q-1;
                            }//if
                            // }
                        } //if
                        k++;
                    } //if
                } //for
            } //if
        }//for
        return R_peakindex;
    }//peak_detect_QRS

    /* this function calculates the average interval between last three nonzero index peaks.
    minimum interval is calculated as 66% of avg interval.spurious peak is removed after comparing the interval with minimum interval and
    actual R peaks are copied in r_peaks where as surious peak is removed by making it zero in R_peakindex.Copy all the actual peaks from r_peaks to R_peakindex.
    Heart rate is calculatedfor each cycle and finally avgerage heart rate is calculated and returned to the calling function
    parameter passed is R peak index.R_peakindex contains all r peak indices.
    */
    private float rr_intverval_HR(int[] R_peakindex)
    {
        int j,k;
        float mininterval=0,RR_avg;
        int[] r_peaks = new int[NO_OF_PEAKS];
        float[] Hrate = new float[NO_OF_PEAKS],
                rr_int = new float[NO_OF_PEAKS];
        float avg_Hrate=0, sum_rate=0;

        for (int i = NO_OF_PEAKS - 1; i > 2; i--)  /* 20210713 - DIBC KN : Adjusting limits to prevent index out of bounds : i>0 changed to i>2 */ {
            if ((R_peakindex[i] > 0) && (R_peakindex[i - 1] > 0) && (R_peakindex[i - 2] > 0)) //last 3 nonzero peak index
            {
                RR_avg = ((R_peakindex[i - 1] - R_peakindex[i - 2]) + (R_peakindex[i] - R_peakindex[i - 1])) / 2;
                mininterval = (float) (0.6667 * RR_avg);
                break;   //
            } //if
        } //for

        j=-1;
        for (int i=NO_OF_PEAKS-1;i>0;i--)    //remove peak if rr interval is less than mininterval
        {
            if (R_peakindex[i]>0)
            {
                k=1;
                j++;
                while((r_peaks[j+1]==0)&&(i>=k))
                {
                    if ((i>=k)&&((R_peakindex[i]-R_peakindex[i-k])>mininterval))
                    {
                        r_peaks[j]= R_peakindex[i];
                        r_peaks[j+1]= R_peakindex[i-k];
                        break;
                    }//if
                    else
                    {
                        while ((i>=k)&&((R_peakindex[i]-R_peakindex[i-k])<mininterval))
                        {
                            R_peakindex[i-k]=0;
                            k++;
                        }//while
                    } //else
                }//while
            } //if
        } //for

        for (int i=0;i<NO_OF_PEAKS;i++)		  //initialising all arrays
        {
            R_peakindex[i]=0;
            Hrate[i]=0;
            rr_int[i]=0;
        } //for
        j=0;
        for (int i=NO_OF_PEAKS-1;i>=0;i--)  	  //copying actual R peaks to R_peakindex
            if (r_peaks[i]>0)
            {
                R_peakindex[j]=r_peaks[i];
                j++;
            }									//calculating HR for each cycle
        for (int i=0;i<NO_OF_PEAKS-1;i++)
            if (R_peakindex[i]>0)
            {
                rr_int[i]=R_peakindex[i+1]-R_peakindex[i];
                Hrate[i]=60/(rr_int[i]/SAMPLING_RATE);
            } //if
        for (int i=0;i<NO_OF_PEAKS;i++) 			 //calculating avg HR
        {
            if (Hrate[i]>0)
            {
                sum_rate=sum_rate+Hrate[i];
                avg_Hrate= sum_rate/(i+1);
            }
        } //for

        return avg_Hrate;
    }  //rr_interval_HR

    private int peak_count(int[] peakindex1)
    {
        int countpks = 0;
        for (int i=0;i<NO_OF_PEAKS;i++)	// comparing two arrays for same no of peaks
        {
            if (peakindex1[i]>0) {
                countpks++;
            }
        } //for
        return  countpks;
    }
    private float minimum(float[] ecg_val)
    {

        float minval=FLT_MAX;

        for(int i=0;i<MAX_ECG_SAMPLES;i++) {
            if (ecg_val[i] < minval) {
                minval = ecg_val[i];
            }
        }
        return minval;
    }

    private float maximum(float[] ecg_val)
    {

        float maxval=-FLT_MAX;


        for(int i=0;i<MAX_ECG_SAMPLES;i++) {
            if (ecg_val[i] > maxval) {
                maxval = ecg_val[i];
            }
        }
        return maxval;
    }


    //Q peak detected from differenciated signal, peak is zero crossing point,slope changes at Q point
//Q peak detected from differentiated (ecg_der) signal, peak is zero crossing point, slope changes at Q point.Within the window of 40 samples from R peak position first zero crossing point is searched.
//If the value of q point is less than 30% of R peak value, its index is saved as Q peak index in qpeakindex1 array else the search continues.Consideration for positive as well as negative R peak is taken care.
    private int[] Q_peakdetect(float[] ecg_val, int win1, float[] ecg_der, int[] rpeakindex1)
    {
        int[] qpeakIndex = new int[NO_OF_PEAKS];
        int k;
        float min_q;
        float[] min = new float[MAX_ECG_SAMPLES];
        //float minval;

        for (int i=0;i<MAX_ECG_SAMPLES;i++) {
            min[i] = 0;
        }
        for (int i=0;i<NO_OF_PEAKS;i++) {
            qpeakIndex[i] = 0;
        }
        for(int j=0;j<NO_OF_PEAKS;j++)
        {
            if (rpeakindex1[j]>0)   //consider only peaks
            {
                k=rpeakindex1[j];	   //R peak index
                if(ecg_der[k-3]>0)
                {
                    //for (m=1;m<=win1;m++) {
                    while ((k>1)&&(ecg_der[k]>0)) {
                        k--; //differenciated signal, peak is zero crossing point,slope changes at Q point
                    }
                    qpeakIndex[j]=k-1;
                    min_q=ecg_val[qpeakIndex[j]];
                    while((k>1)&&(ecg_val[qpeakIndex[j]]>0.3*ecg_val[rpeakindex1[j]])&&(k>(rpeakindex1[j]-win1))) //search window limit
                    {
                        //if value of Q is greater than 30% of R peak, search next zero crossing point//30% for CSE database
                        while ((k>1)&&(ecg_der[k]>0)) k--;
                        if (ecg_val[k-1]<min_q)
                        {qpeakIndex[j]=k-1; //check for minimum value index
                            min_q=ecg_val[qpeakIndex[j]];
                        }
                        k--;
                    } //while
//                    }
                }
                else if (ecg_der[k-3]<0) {
                    while ((k>1)&&(ecg_der[k]<0))	k--; //differenciated signal, peak is zero crossing point,slope changes at Q point
                    qpeakIndex[j]=k-1;
                    min_q=ecg_val[qpeakIndex[j]];
                    while((k>1)&&(ecg_val[qpeakIndex[j]]>0.3*ecg_val[rpeakindex1[j]])&&(k>(rpeakindex1[j]-win1))) //search window limit
                    {
                        //if value of Q is greater than 30% of R peak, search next zero crossing point//30% for CSE database
                        while ((k>1)&&(ecg_der[k]<0)) {
                            k--;
                        }
                        if (ecg_val[k-1]>min_q)
                        {
                            qpeakIndex[j]=k-1; //check for minimum value index
                            min_q=ecg_val[qpeakIndex[j]];
                        }
                        k--;
                    } //while
                }




            } //if
        } //for
        return qpeakIndex;
    } //Q_peakdetect

    /*detects QRS start position i.e. Q start. Parameters passsed to the funcion are index of Q peaks, differnciated ECG signal.qrsStart is array which will contain indices of all q start positions.
    With reference to Q peak position a small immediate negative peak is searched in differentiated signal towards left.
    Threshold is calculated.When differnciated signal crosses the threshold,QRS start position is detected.
     */
    private int[] QRS_start(int[] qminindex, float[] ecg_der)
    {
        int[] qrsStart = new int[NO_OF_PEAKS];
        float pkecg_der,maxval,constant,threshold;
        int k,c,flag1=0;
        int[]   qrs_start1 = new int[NO_OF_PEAKS],
                qrs_start2 = new int[NO_OF_PEAKS];

        for (int i=0;i<NO_OF_PEAKS;i++)
        {
            qrs_start1[i]=0;
            qrs_start2[i]=0;
        }
        for(int j=0;j<NO_OF_PEAKS;j++)
        {
            if (qminindex[j]>0)
            {
                k=qminindex[j];
                flag1=1;
                if(k>10)
                {	 pkecg_der = ecg_der[k];
                    for (c=1; c<10; c++)
                    {
                        if (ecg_der[k-c] < pkecg_der) {
                            pkecg_der = ecg_der[k - c];
                        }
                    }	//to find maximumpeak of differentiated sig i.e. max slope of QRS
                    while ((k>3)&&(flag1==1))
                    {
                        if (ecg_der[k]<ecg_der[k+1]&&ecg_der[k]<=ecg_der[k-1]) //negative peak
                        {
                            maxval=ecg_der[k];
                            constant=1.8f;
                            threshold=maxval/constant;
                            while ((k>3)&&(flag1==1))
                            {
                                if (ecg_der[k]<threshold) {
                                    flag1=1;k--;
                                }
                                else {
                                    flag1 = 0;
                                }
                            } //while
                            qrs_start1[j]=k-3;
                        } //if
                        k--;
                    } //while

                    k=qminindex[j];

                    flag1=1;
                    while ((k>3)&&(flag1==1))
                    {
                        if (ecg_der[k]>ecg_der[k+1]&&ecg_der[k]>=ecg_der[k-1]) //positive peak
                        {
                            maxval=ecg_der[k];
                            constant=1.8f;
                            threshold=maxval/constant;
                            while ((k>3)&&(flag1==1))
                            {
                                if (ecg_der[k]>threshold)
                                {
                                    flag1=1;k--;
                                }
                                else {
                                    flag1 = 0;
                                }
                            } //while
                            qrs_start2[j]=k-3;
                        } //if
                        k--;
                    } //while

                    if	(qrs_start1[j]>qrs_start2[j]) {
                        qrsStart[j] = qrs_start1[j];
                    }
                    else {
                        qrsStart[j] = qrs_start2[j];
                    }

                } //if
            }//if
        }//for
        return qrsStart;
    } //QRS_start


    /*rr_factor is calculated depending upon the average heart rate. If the heart rate is slow or normal i.e less than 130 rr_factor is avg_Hrate/72 and if heart rate is above 130, rr_factor is avg_Hrate/30
        This rr_factor is used to compute Pwindow variable with heart rate.
        */
    private static float compute_rr_factor(float avg_Hrate)
    {
        if (avg_Hrate<130) {
            return avg_Hrate / 72;
            //for slow and normal heart rate
        }
        else {
            return avg_Hrate / 30;
            //avg_Hrate/45; //for very high heart rate
        }
    } //compute_rr_factor


    //p peak detected by searching maximum value withing the window from q peak. Two sample comparison
    /*  p peak detected by searching maximum value within the window from q peak. P peak detection by searching highest peak within window from Q peak.
    Parameters passed to the function are filtered ECG signal, index of r peaks, search window from Q peak, index of Q peaks.In lead 2, search starts from (Q start position -10) samples till window of 40 samples.
    In other leads we take reference of p peak found in lead 2. The search starts from (Ppeak index in lead 2 \96 20) samples and the window is 40.
    Maximum and minimum peak is searched. Peak is checked with respect to value of 15 samples on either side and also above threshold which is (0.01* peak value). Area under peak is calculated.
    The broader peak whose area is greater is considered as p peak. P peak indices are stored in ppeakindex is array. If any of maximum or minimum peak is not detected, the other peak detected is stored in ppeakindex array.
    */
    private int[] ppeak_detect(float[] ecg_val, float max, float min, int[] rpeakindex1, int win1, int[] Qstartindex) {
        int j,k,m;
//	float ecg_val[MAX_ECG_SAMPLES];
        float minval,maxval=0;
        int[]   ppeakindex = new int[NO_OF_PEAKS],
                mxindex = new int[NO_OF_PEAKS],
                mnindex = new int[NO_OF_PEAKS];
        //float ref_index, mxpeak_amp,mnpeak_amp;
        float avg_dc1;
        int dc_length;
        float sum1,threshold, peak_val;
        float mxpeakarea=0,mnpeakarea=0;

        for (j=0;j<NO_OF_PEAKS;j++)
        {

            sum1=0;
            dc_length = 0;
            avg_dc1=0;
            if (Qstartindex[j]>15)
            {
                for (int i=0;i<15;i++)
                {
                    sum1+=ecg_val[Qstartindex[j]-i];
                    dc_length++;
                }
            }
            if (dc_length>0) avg_dc1 =  sum1/dc_length;//calculating temporary isoelectric point

            peak_val= calculate_avg_level(ecg_val[rpeakindex1[j]],avg_dc1);//calculating R peak value wrt temporary isoelectric level
            threshold= (float) (0.01* abs(peak_val));

            if (Qstartindex[j]>0)
            {
                if(multiLeadData[1][P_INDEX][j]>0)
                {
                    k=multiLeadData[1][P_INDEX][j]+20;
                    win1=40;
                }
                else
                {
                    k = Qstartindex[j] - 10;  // forsearch start 10 samples after Q start towards left
                }

                maxval=min;
                //searching maximum value withing the window

                for (m=1;m<=win1;m++)
                {
                    if ((k>15)&&(k<MAX_ECG_SAMPLES-15))
                    {
                        if (ecg_val[k]>maxval&&(ecg_val[k]>(ecg_val[k+15]+threshold))&&(ecg_val[k]>(ecg_val[k-15]+threshold)))
                        {
                            maxval= ecg_val[k];
                            //tpeakindex[j]=k;
                            mxindex[j]=k;
                        }
                    }
                    k--;
                }//for

                if(mxindex[j]!=0) {
                    mxpeakarea = peakArea(ecg_val, mxindex[j], Qstartindex[j], rpeakindex1[j]);
                }

                if(multiLeadData[1][P_INDEX][j]>0)
                {
                    k=multiLeadData[1][P_INDEX][j]+15;
                    win1=40;
                }
                else {
                    k = Qstartindex[j] - 10;    //-10 for minimum value
                }
                minval=max;
                for (m=1;m<=win1;m++)
                {
                    if ((k>15)&&(k<MAX_ECG_SAMPLES-15))
                    {
                        if (ecg_val[k]<minval&&(ecg_val[k]<(ecg_val[k+15]-threshold))&&(ecg_val[k]<(ecg_val[k-15]-threshold)))
                        {
                            minval= ecg_val[k];
                            mnindex[j]=k;}
                    }
                    k--;
                }//for

                if(mnindex[j]!=0) {
                    mnpeakarea = peakArea(ecg_val, mnindex[j], Qstartindex[j], rpeakindex1[j]);
                }

                if ((mxindex[j]>0)&&(mnindex[j]==0)) {
                    ppeakindex[j] = mxindex[j];//only maxindex is detected
                }
                if ((mnindex[j]>0)&&(mxindex[j]==0)) {
                    ppeakindex[j] = mnindex[j]; //only minimum index is detected
                }

                if(ppeakindex[j]==0)
                {
                    if (mxpeakarea>=mnpeakarea-threshold) {
                        ppeakindex[j] = mxindex[j];
                    }
                    else {
                        ppeakindex[j] = mnindex[j];
                    }
                }

            } //if if (Qpeakindex[j]>0)
        } // for
        return ppeakindex;
    }// ppeak_detect
    private float calculate_avg_level(float ecg_val,float avgDC)
    {
        float peak_ecg_val=ecg_val;
        if (avgDC>=0)
        {
            if (ecg_val>=0) {
                peak_ecg_val = ecg_val - avgDC;
            } else {
                peak_ecg_val = -(Math.abs(ecg_val) + avgDC);
            }
        }
        else
        {
            if (ecg_val>=0) {
                peak_ecg_val = ecg_val + Math.abs(avgDC);
            } else {
                peak_ecg_val = ecg_val - avgDC;
            }
        }
        return peak_ecg_val;
    }

    private float peakArea(float[] ecg_val1, int mxindex, int qstartindex, int rpeakindex1)
    {
        float avg_val,avg=0,pkArea=0,sum=0;

        float[] ecg_val = new float[MAX_ECG_SAMPLES];
        float avg_dc1, min_val;
        int dc_length;
        float sum1,threshold, peak_val;

        min_val=minimum(ecg_val1);
        for(int i=0;i<MAX_ECG_SAMPLES;i++) {
            ecg_val[i] = ecg_val1[i] - min_val;
        }

        sum1=0 ;
        dc_length = 0;
        avg_dc1=0;
        if (qstartindex>15)
        {
            for (int i=0;i<15;i++)
            {
                sum1+=ecg_val[qstartindex-i];
                dc_length++;
            }
        }
        if (dc_length>0) {
            avg_dc1 = sum1 / dc_length;
        }
        peak_val= calculate_avg_level(ecg_val[rpeakindex1],avg_dc1);
        threshold= (float) (0.01*Math.abs(peak_val));

        avg_val=(ecg_val[mxindex+15]+ecg_val[mxindex-15])/2;

        if ((ecg_val[mxindex]>(ecg_val[mxindex+15]+threshold))&&(ecg_val[mxindex]>(ecg_val[mxindex-15]+threshold)))
        {
            for (int i=mxindex-15;i<=mxindex+15;i++)
            {
                if (ecg_val[i]<avg_val) {
                    sum = sum + avg_val;
                }
                else {
                    sum = sum + ecg_val[i];
                }
                avg=avg+avg_val;
            } //for
            pkArea=abs(abs(sum)-abs(avg));
        } //if
        else if ((ecg_val[mxindex]<(ecg_val[mxindex+15]-threshold))&&(ecg_val[mxindex]<(ecg_val[mxindex-15]-threshold)))
        {
            for (int i=mxindex-15;i<=mxindex+15;i++)
            {
                if (ecg_val[i]>avg_val) {
                    sum = sum + avg_val;
                }
                else {
                    sum = sum + ecg_val[i];
                }
                avg=avg+avg_val;
            }//for
            pkArea=abs(abs(avg)-abs(sum));
        }//else-if

        return pkArea;

    }


/*S peak is detected from differentiated (ecg_der) signal, peak is zero crossing point, slope changes at S point.
If value of s peak is less than 30% of R peak, further search is continued.
Parameters passsed to the funcion are filtered ECG signal, differenciated signal, search window from R peak, index of R peaks.
sminindex is array which will contain indices of all s peak positions..*/

    private int[] S_peakdetect(float[] fEcgVal, float[] ecg_der, int win1, int[] rpeakindex1){
        int[] sminindex = new int[NO_OF_PEAKS];
        int j,k;
        float minval=0;

        for(j=0;j<NO_OF_PEAKS;j++)
        {
            if (rpeakindex1[j]>0)   //consider only peaks
            {
                k=rpeakindex1[j];	   //R peak index

                    while ((k<MAX_ECG_SAMPLES-1)&&(ecg_der[k]>0)) {
                        k++; //differenciated signal, peak is zero crossing point,slope changes at Q point
                    }
                    sminindex[j]=k+1;
                    minval=fEcgVal[sminindex[j]];
                    while((k<MAX_ECG_SAMPLES-1)&&(fEcgVal[sminindex[j]]>0.3*fEcgVal[rpeakindex1[j]])&&(k<(rpeakindex1[j]+win1))) //search window limit
                    {
                        //if value of Q is greater than 30% of R peak, search next zero crossing point//30% for CSE database
                        while ((k<MAX_ECG_SAMPLES-1)&&(ecg_der[k]>0)) {
                            k++;
                        }
                        if (fEcgVal[k+1]<minval)
                        {
                            sminindex[j]=k+1; //check for minimum value index
                            minval=fEcgVal[sminindex[j]];
                        }
                        k++;
                    } //while
                }
            } //if
        return sminindex;
    } // S_peakdetect



/*Detects Start position of P wave. Parameters passed to the function are index of P peaks, filtered ECG signal, R peak index, differentiated ECG signal and qrs start index.
Distinct P Positive peak (maximum value) and negative peak (minimum value) is checked with respect to value of 15 samples on either side
With reference to P peak position when a positive peak is detected in filtered ECG,a small immediate positive peak is searched in differentiated signal towards left.
Threshold is calculated. When differnciated signal crosses the threshold, P start position is detected. if P start is greater than 0.1* amplitude of P peak, next peak is searched.
Search is locked within p_startwindow samples from P peak towards left. p_startwindow is 30 samples. Similar approach is taken if we have negative P peak.*/

    private int[] P_start(int[] ppeakindex1, float[] ecg_val, int[] maxindex1, float[] ecg_der, int[] qrs_start)
    {
        float constant,peak_val,pstart_ecg_val;
        int[] p_start = new int[NO_OF_PEAKS];
        int k,j,p_startwindow;
        boolean flag1;
        constant=1.35f;
        //p_startwindow=P_STARTWINDOW/rr_factor;
        p_startwindow=P_STARTWINDOW;

        for(j=0;j<NO_OF_PEAKS;j++)

        {  if((ppeakindex1[j]>0)&&(ppeakindex1[j]>11)) //(ppeakindex1[j]<(MAX_ECG_SAMPLES-11) check for p index
        {

            peak_val= calculate_avg_level(ecg_val[ppeakindex1[j]],ecg_val[qrs_start[j]]);
            k=ppeakindex1[j]+2;//2 is added for compensation of samples after differenciation
            flag1=true;

            if (((ecg_val[ppeakindex1[j]])>(ecg_val[ppeakindex1[j]-15]))&&((ecg_val[ppeakindex1[j]])>(ecg_val[ppeakindex1[j]+15])))  //checking distint positive p peak by camparing with +/- 3rd sample from peak

            {while ((k>0)&&(flag1)&&(k>(ppeakindex1[j]-p_startwindow)))    //p start should not be beyond 30 samples from p peak towards left

            {if (ecg_der[k]>ecg_der[k+1]&&ecg_der[k]>=ecg_der[k-1])  // searching positive peak in differenciated signal after p peak towards left
            {
                flag1=check_posPeakstart(p_startwindow,ppeakindex1,ecg_der,j,k,constant,p_start); // TODO: 1/21/21 &k used in c. check its translation in java
                pstart_ecg_val=ecg_val[k];
                pstart_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrs_start[j]]);

                while((k>0)&&(pstart_ecg_val>0.1*peak_val)&&(k>(ppeakindex1[j]-p_startwindow)))   //if p end detected is greater than 30% of P peak and should not be beyond 30 samples from p peak towards left, search again
                {
                    flag1=true;
                    while ((k>0)&&(flag1)&&(k>(ppeakindex1[j]-p_startwindow)))			//p start should not be beyond 30 samples from p peak towards left
                    {
                        if (ecg_der[k]>ecg_der[k+1]&&ecg_der[k]>=ecg_der[k-1])
                            flag1=check_posPeakstart(p_startwindow, ppeakindex1,ecg_der,j,k,constant,p_start); // TODO: 1/21/21 &k used in c. check its translation in java
                        //}//if
                        pstart_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrs_start[j]]);
                        k--;
                    } //while
                    k--;
                }//while
            }//if
                k--;
            }//while

                if (k<0) p_start[j]=1;
            }//if


            if (((ecg_val[ppeakindex1[j]])<(ecg_val[ppeakindex1[j]-15]))&&((ecg_val[ppeakindex1[j]])<(ecg_val[ppeakindex1[j]+15])))  //negative p peak

            {
                while ((k>0)&&(flag1)&&(k>(ppeakindex1[j]-p_startwindow)))			   //p start should not be beyond 30 samples from p peak towards left

                {
                    if (ecg_der[k]<ecg_der[k+1]&&ecg_der[k]<=ecg_der[k-1])	    // searching negative peak in differenciated signal after p peak towards left
                    {
                        flag1=check_negPeakstart(p_startwindow, ppeakindex1,ecg_der,j,k,constant,p_start); // TODO: 1/21/21 &k used in c. check its translation in java
                        pstart_ecg_val=ecg_val[k];
                        pstart_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrs_start[j]]);

                        while((k>0)&&(pstart_ecg_val<0.1*peak_val)&&(k>(ppeakindex1[j]-p_startwindow)))	  //if p end detected is greater than 30% of P peak and should not be beyond 30 samples from p peak towards left, search again
                        {
                            flag1=true;
                            while ((k>0)&&(flag1)&&(k>(ppeakindex1[j]-p_startwindow)))
                            {if (ecg_der[k]<ecg_der[k+1]&&ecg_der[k]<=ecg_der[k-1])
                                flag1=check_negPeakstart(p_startwindow, ppeakindex1,ecg_der,j,k,constant,p_start); // TODO: 1/21/21 &k used in c. check its translation in java
                                //}//if
                                pstart_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrs_start[j]]);
                                k--;
                            }//while
                            k--;
                        }//while
                    } //if
                    k--;
                }//while

                if (k<0) p_start[j]=1;
            }//if

        }//if

        }//for
        return p_start;
    }

    private static boolean check_posPeakstart(int startwindow, int[] peakindex1, float[] ecg_der, int j, int k, float constant, int[] pk_start) // TODO: 1/21/21 *k used in c. check its translation in java
    {
        float maxval,threshold ;
//        int k;
//
//        k=k1; // TODO: 1/21/21 *k used in c. check its translation in java
        maxval=ecg_der[k];
//	constant=2.0;
        threshold=maxval/constant;
        boolean flag1 =true;
        while ((k>0)&&(flag1)&&(k>(peakindex1[j]-startwindow)))		   // t start should not be beyond 50 samples from t peak towards left
        {
            if (ecg_der[k]>=threshold) k--;
            else flag1=false;
        } //while
        if(k>(peakindex1[j]-startwindow)) pk_start[j]=k-2;
        else {pk_start[j]=k-2; flag1=false;}

//        k1=k; // TODO: 1/21/21 *k used in c. check its translation in java
        return flag1;


    } //temp2


    private static boolean check_negPeakstart(int startwindow, int[] peakindex1, float[] ecg_der, int j, int k, float constant, int[] pk_start) // TODO: 1/21/21 *k used in c. check its translation in java
    {
        float maxval,threshold ;
//        int k;
//        k=k1; // TODO: 1/21/21 *k used in c. check its translation in java
        maxval=ecg_der[k];
        //constant=2.0;
        threshold=maxval/constant;
        boolean flag1=true;
        while ((k>0)&&(flag1)&&(k>(peakindex1[j]-startwindow)))				 // t start should not be beyond 30 samples from t peak towards left
        {
            if (ecg_der[k]<=threshold) k--;
            else flag1 = false;
        } //while
        if(k>(peakindex1[j]-startwindow)) pk_start[j]=k-2;
        else {pk_start[j]=k-2; flag1=false;}

//        k1=k; // TODO: 1/21/21 *k used in c. check its translation in java
        return flag1;

    }//temp2


    /*Detects end position of P wave. Parameters passed to the function are index of P peaks, filtered ECG signal, R peak index Q start index, differentiated ECG signal.
    qrs start is determined to limit the search of P end till Q start. Distinct P Positive peak (maximum value) and negative peak (minimum value) is checked with respect to value of 10 samples on either side
    With reference to P peak position when a positive peak is detected in filtered ECG, a small immediate negative peak is searched in differentiated signal towards right.
    Threshold is calculated. When differentiated signal crosses the threshold, P end position is detected (check_posPeakend) if P end is greater than 0.1* amplitude of P peak, further search is continued.
    P end is further confirmed if the next sample is of same value and further small rise.
    Similar approach is taken if we have negative P peak.
    .*/
    private int[] P_end(int[] ppeakindex1, float[] ecg_val, int[] maxindex1, int[] qrs_start, float[] ecg_der)
    {
        float constant = 2.0f,peak_val,pend_ecg_val;
        int i,k,flag1;

        int[] p_end = new int[NO_OF_PEAKS];

        for(int j=0;j<NO_OF_PEAKS;j++)
        {   //if(ppeakindex1[j]>0)

            if ((ppeakindex1[j]>0)&&(qrs_start[j]>0)&&(ppeakindex1[j]>(11)))
            {

                peak_val= calculate_avg_level(ecg_val[ppeakindex1[j]],ecg_val[qrs_start[j]]);

                k=ppeakindex1[j]+2;//2 is added for compensation of samples after differenciation
                flag1=1;
                if (((ecg_val[ppeakindex1[j]])>(ecg_val[ppeakindex1[j]-10]))&&((ecg_val[ppeakindex1[j]])>(ecg_val[ppeakindex1[j]+10])))  //detect positive peak
                {
                    while ((flag1==1)&&(k<MAX_ECG_SAMPLES-1))
                    {
                        if (ecg_der[k]<ecg_der[k+1]&&ecg_der[k]<=ecg_der[k-1])			   //negative peak in differentiated signal
                        {
                            flag1=check_posPeakend(qrs_start,ecg_der,j,k,constant,p_end); // TODO: 1/21/21 &k used in c.. check its translation in java
                            pend_ecg_val=ecg_val[k];
                            pend_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrs_start[j]]);

                            while((k<MAX_ECG_SAMPLES-1)&&(pend_ecg_val>0.1*peak_val))
                            {
                                flag1=1;
                                while ((flag1==1)&&(k<MAX_ECG_SAMPLES-1))
                                {
                                    if (ecg_der[k]<ecg_der[k+1]&&ecg_der[k]<=ecg_der[k-1]) {
                                        flag1 = check_posPeakend(qrs_start, ecg_der, j, k, constant, p_end); // TODO: 1/21/21 &k used. check its translation in java
                                    }
                                    p_end[j]=p_end[j]-2;
                                    k++;
                                } //while
                                pend_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrs_start[j]]);
                                k++;
                            }//while
                        } //if
                        k++;
                    }//while
                } //if
                if (flag1==0)
                {
                    i=p_end[j];
                    while ((i<(qrs_start[j]-4))&&(i<MAX_ECG_SAMPLES-1))
                    {
                        if (((ecg_val[i]==ecg_val[i+1])&&(ecg_val[i]<ecg_val[i+2]))||(ecg_val[i]<ecg_val[i+1]))   //further search to get P end location
                        {
                            p_end[j]=i;
                            break;
                        }//if
                        i++;
                    } //while
                }//if
                if (((ecg_val[ppeakindex1[j]])<(ecg_val[ppeakindex1[j]-10]))&&((ecg_val[ppeakindex1[j]])<(ecg_val[ppeakindex1[j]+10])))  //detect negative peak
                {
                    while ((flag1==1)&&(k<MAX_ECG_SAMPLES-1))
                    {
                        if (ecg_der[k]>ecg_der[k+1]&&ecg_der[k]>=ecg_der[k-1])
                        {   flag1=check_negPeakend(qrs_start,ecg_der,j,k,constant,p_end); // TODO: 1/21/21 &k used. check its translation in java
                            pend_ecg_val=ecg_val[k];
                            pend_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrs_start[j]]);

                            while((k<MAX_ECG_SAMPLES-1)&&(pend_ecg_val<0.1*peak_val))
                            {
                                flag1=1;
                                while ((flag1==1)&&(k<MAX_ECG_SAMPLES-1))
                                {
                                    if (ecg_der[k]>ecg_der[k+1]&&ecg_der[k]>=ecg_der[k-1])
                                        flag1=check_negPeakend(qrs_start,ecg_der,j,k,constant,p_end); // TODO: 1/21/21 &k used. check its translation in java
                                    p_end[j]=p_end[j]-2;
                                    k++;
                                }//while
                                pend_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrs_start[j]]);
                                k++;
                            }//while
                        }//if
                        k++;

                    } //while

                    if (flag1==0)
                    {i=p_end[j];
                        while((i<(qrs_start[j]-4))&&(i<MAX_ECG_SAMPLES-1))
                        { if(((ecg_val[i]==ecg_val[i+1])&&(ecg_val[i]>ecg_val[i+2]))||(ecg_val[i]>ecg_val[i+1]))  //further search to get P end location
                        {p_end[j]=i;
                            break;}
                            i++;
                        }//while
                    }//if
                } //if

            }   //if

        } //for
        return p_end;
    }


    private int check_posPeakend(int[] index, float[] ecg_der, int j, int k, float constant, int[] pk_end)
    {
        float maxval,threshold ;
        int flag1;

//        k=k1; // TODO: 1/21/21 *k1 used in c.. check its translation in java
        maxval=ecg_der[k];
        threshold=maxval/constant;
        flag1 =1;
        while ((flag1==1)&&(k<MAX_ECG_SAMPLES-1))
        {
            if (ecg_der[k]<threshold) k++;
            else flag1=0;
        } //while
        if(k<index[j]) pk_end[j]=k+2;
        else
        {
            pk_end[j]=index[j];
            flag1=0;
        }//else
//        k1=k;  // TODO: 1/21/21 *k used in c. check its translation in java
        return flag1;
    }

    private int check_negPeakend(int[] index, float[] ecg_der, int j, int k, float constant, int[] pk_end)// TODO: 1/21/21 &k used. check its translation in java
    {
        float maxval,threshold ;
        int flag1;
//        k=k1; // TODO: 1/21/21 &k used. check its translation in java
        maxval=ecg_der[k];
        threshold=maxval/constant;
        flag1=1;
        while ((flag1==1)&&(k<MAX_ECG_SAMPLES-1))
        {
            if (ecg_der[k]>threshold) k++;
            else flag1=0;
        } //while
        if(k<index[j]) pk_end[j]=k+2;
        else
        {pk_end[j]=index[j]; flag1=0;}

//        k1=k;  // TODO: 1/21/21 *k used in c. check its translation in java
        return flag1;
    }


/*detects QRS end position i.e. S end. Parameters passsed to the funcion are index of S peaks, differnciated ECG signal.qrs_end is array which will contain indices of all s end positions.
With reference to S peak position a small immediate positive peak is searched in differentiated signal towards right.
Threshold is calculated.When differnciated signal crosses the threshold,QRS end position is detected.
 */

    private int[] QRS_end(int[] sminindex, float[] ecg_val, float[] ecg_der)
    {
        int[] qrs_end = new int[NO_OF_PEAKS];
        float pkecg_der,maxval,constant,threshold;
        int k,c,flag1;

        for(int j=0;j<NO_OF_PEAKS;j++)
        {
            if (sminindex[j]>0)
            {
                k=sminindex[j]+2;//2 is added for compensation of samples after differenciation
                flag1=1;
                if(k>40)
                {
                    pkecg_der = ecg_der[k];
                    for (c=1; c<40; c++) {        //detecting peak in differnciated signal
                        if (ecg_der[k - c] > pkecg_der) {
                            pkecg_der = ecg_der[k - c];
                        }
                    }
                    while ((k<MAX_ECG_SAMPLES-1)&&(flag1==1))
                    {
                        if (ecg_der[k]>ecg_der[k+1]&&ecg_der[k]>=ecg_der[k-1]) //detecting first positive peak after S peak towards right
                        {
                            maxval=ecg_der[k];
                            constant=(maxval*10)/pkecg_der;
                            if (constant<=4) {
                                constant = 3;
                            }
                            else if ((4<constant)&&(constant<=4.75)) {
                                constant = 8;
                            }
                            else if ((4.75<constant)&&(constant<6.20)) {
                                constant = 9;
                            }
                            else if (constant>=6.20) {
                                constant = 12;
                            }
                            threshold=maxval/constant;
                            while ((k<MAX_ECG_SAMPLES-1)&&(flag1==1))
                            {
                                if (ecg_der[k]>threshold)
                                {
                                    flag1=1;k++;
                                }//if
                                else {
                                    flag1 = 0;
                                }
                            } //while
                            qrs_end[j]=k-2;
                        } //if
                        k++;
                    } //while
                }//if

            }//if
        } //for
        return qrs_end;
    } //QRS_end

    private float T_windowCorrection(int[] R_peakindex, int no_of_pks)
    {
        float QT_window;
        float RR_int;

        RR_int=(float)(R_peakindex[no_of_pks-2]-R_peakindex[no_of_pks-3])/500;

        QT_window= (float) ((QTC*(Math.sqrt(RR_int)))*500);
        return QT_window;

    }


    /*  t peak detection by searching highest peak within window (calculated from T_windowCorrection) from Qend position. Positive peak (maximum value) and negative peak (minimum value) is searched.
    Peak is checked with respect to value of 25 samples on either side and also above threshold which is (0.04* peak value).  If only one peak is detected, it is considered as T wave.
    Among both positive and negative peak,the peak nearer to reference index is considered as T wave. Reference index is calculated using ref_index= Qstartindex[j]+((0.33*sqrt(RR_int))*500).
    The index of T wave detected is saved in tpeakindex.Parameters passed to the function are filtered ECG signal, Rpeak index, search window, index of QRS start and end, maximum and minimum of filtered ECG.
    */
    private int[] tpeak_detect(double max, double minimum_val, float[] ecg_val, int[] R_peakindex, int win1, int[] Qendindex, int[] Qstartindex, int no_of_pks) {
        int j,k,m,win,ref_index,dc_length;
        float maxval,minval,RR_int,threshold,peak_val,sum1,avg_dc1;
        int[]   tpeakindex = new int[NO_OF_PEAKS],
                mxindex = new int[NO_OF_PEAKS],
                mnindex = new int[NO_OF_PEAKS];


        for (int i=0;i<NO_OF_PEAKS;i++)
        {
            tpeakindex[i]=0;
            mxindex[i]=0;
            mnindex[i]=0;
        }

        for(j=0;j<NO_OF_PEAKS;j++)
        {
            sum1=0;
            dc_length = 0;
            avg_dc1=0;
            if (Qendindex[j]<MAX_ECG_SAMPLES-25)
            {
                for (int i=0;i<25;i++)
                {
                    sum1+=ecg_val[Qendindex[j]+i];
                    dc_length++;
                }
            }
            if (dc_length>0) {
                avg_dc1 = sum1 / dc_length;
            }
            peak_val= calculate_avg_level(ecg_val[R_peakindex[j]],avg_dc1);

            threshold= (float) (0.04* abs(peak_val));
            if (Qendindex[j]>0)
            {
                k=Qendindex[j]+22;

                maxval= (float) minimum_val;
                win=win1-((Qendindex[j]+20)-Qstartindex[j]);
                for (m=1;m<=win;m++)
                {
                    if ((k>0)&&(k<MAX_ECG_SAMPLES-25))
                    {
                        if ((ecg_val[k]>maxval)&&(ecg_val[k]>(ecg_val[k+25]+threshold))&&(ecg_val[k]>(ecg_val[k-25]+threshold)))
                        {  maxval= ecg_val[k];
                            mxindex[j]=k;
                        }
                    }
                    k++;
                }//for

                k=Qendindex[j]+22;		//for minimum value
                minval= (float) max;
                for (m=1;m<=win;m++)
                {
                    {
                        if (k>0&&(k<MAX_ECG_SAMPLES-25))
                        {
                            if ((ecg_val[k]<minval)&&(ecg_val[k]<(ecg_val[k+25]-threshold))&&(ecg_val[k]<(ecg_val[k-25]-threshold)))
                            {
                                minval= ecg_val[k];
                                mnindex[j]=k;
                            }
                        }
                    }
                    k++;
                }//for

                if ((mxindex[j]>0)&&(mnindex[j]==0)) tpeakindex[j]=mxindex[j];//only maxindex is detected
                if ((mnindex[j]>0)&&(mxindex[j]==0)) tpeakindex[j]=mnindex[j]; //only minimum index is detected


                RR_int=(float)(R_peakindex[no_of_pks-2]-R_peakindex[no_of_pks-3])/500;
                ref_index= (int) (Qstartindex[j]+((0.33*Math.sqrt(RR_int))*500));

                if ((mxindex[j]>0)&&(mnindex[j]>0))
                {
                    if ((abs(ref_index-mxindex[j]))<(abs(ref_index-mnindex[j]))) {
                        tpeakindex[j] = mxindex[j];
                    }
                    else {
                        tpeakindex[j] = mnindex[j];
                    }
                }

            } //if
        }//for
        return tpeakindex;
    }  //tpeak_detect


/*Detects start position of T wave. Parameters passed to the function are index of T peaks, filtered ECG signal and differentiated ECG signal.
Distinct T Positive peak (maximum value) and negative peak (minimum value) is checked with respect to value of 10 samples on either side
With reference to t peak position when a positive peak is detected in filtered ECG and within search window of 60 samples, a small immediate
positive peak is searched in differentiated signal towards left. Threshold is calculated. When differentiated signal crosses the threshold,T start position is detected (check_posPeakstart).
If T start is greater than 0.1* amplitude of T peak, next peak is searched. Search is locked within T_startwindow samples from T peak towards left.
T_startwindow is 30 samples. . If Tstart is not determined as above or within the search window of 30 samples, search end point is considered as Tstart.
If Tstart is determined before qrsend , qrsend index is considered as T start point. Similar approach is taken if we have negative T peak.*/

    private int[] T_start(int[] tpeakindex1 , float[] ecg_val , float[] ecg_der , int[] qrsend )
    {
        int[] t_start = new int[NO_OF_PEAKS];
        float constant,peak_val,tstart_ecg_val;
        int k,j,t_startwindow;
        boolean flag1;
        constant=2;
        t_startwindow=T_STARTWINDOW;
        for(j=0;j<NO_OF_PEAKS;j++)
        {
            if((tpeakindex1[j]>0)&&(tpeakindex1[j]<(MAX_ECG_SAMPLES-11)))
            {
                peak_val= calculate_avg_level(ecg_val[tpeakindex1[j]],ecg_val[qrsend[j]]);
                k=tpeakindex1[j]+2;//2 is added for compensation of samples after differenciation
                flag1=true;
                if (((ecg_val[tpeakindex1[j]])>(ecg_val[tpeakindex1[j]-10]))&&((ecg_val[tpeakindex1[j]])>=(ecg_val[tpeakindex1[j]+10])))  //checking positive T peak
                {while ((flag1==true)&&(k>(tpeakindex1[j]-t_startwindow)))  // t start should not be beyond 60 samples from t peak towards left
                {  if (ecg_der[k]>ecg_der[k+1]&&ecg_der[k]>=ecg_der[k-1])			//checking positive peak in differenciated signal
                {  flag1=check_posPeakstart(t_startwindow, tpeakindex1,ecg_der,j,k,constant,t_start);  // TODO: 1/21/21 &k used in c. check its translation in java
                    if(t_start[j]<qrsend[j]) t_start[j]=qrsend[j];
                    tstart_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrsend[j]]);

                    while ((k>(tpeakindex1[j]-t_startwindow))&&(tstart_ecg_val>0.1*peak_val))					//if t start detected is more than 30% of T peak, search again
                    {  flag1=true;
                        while ((flag1)&&(k>(tpeakindex1[j]-t_startwindow)))
                        {  if (ecg_der[k]>ecg_der[k+1]&&ecg_der[k]>=ecg_der[k-1])
                            flag1= check_posPeakstart(t_startwindow, tpeakindex1,ecg_der,j,k,constant,t_start); // TODO: 1/21/21 &k used in c. check its translation in java
                            if(t_start[j]<qrsend[j]) t_start[j]=qrsend[j];
                            tstart_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrsend[j]]);
                            k--;
                        }//while
                        k--;
                    }//while
                } //if
                    k--;
                } //while
                }  //if of positive peak
                if (((ecg_val[tpeakindex1[j]])<(ecg_val[tpeakindex1[j]-10]))&&((ecg_val[tpeakindex1[j]])<=(ecg_val[tpeakindex1[j]+10]))) //checking negative t peak
                {while ((flag1)&&(k>(tpeakindex1[j]-t_startwindow)))
                {if (ecg_der[k]<ecg_der[k+1]&&ecg_der[k]<=ecg_der[k-1])			 //checking negative peak in differenciated signal
                {
                    flag1=check_negPeakstart(t_startwindow, tpeakindex1,ecg_der,j,k,constant,t_start);  // TODO: 1/21/21 &k used in c. check its translation in java
                    if(t_start[j]<qrsend[j]) t_start[j]=qrsend[j];
                    tstart_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrsend[j]]);
                    while ((k>(tpeakindex1[j]-t_startwindow))&&(tstart_ecg_val<0.1*peak_val))				//if t start detected is more than 30% of T peak, search again
                    { flag1=true;
                        while ((flag1)&&(k>(tpeakindex1[j]-t_startwindow)))
                        {
                            if (ecg_der[k]<ecg_der[k+1]&&ecg_der[k]<=ecg_der[k-1])
                                flag1=check_negPeakstart(t_startwindow, tpeakindex1,ecg_der,j,k,constant,t_start);  // TODO: 1/21/21 &k used in c. check its translation in java
                            if(t_start[j]<qrsend[j]) t_start[j]=qrsend[j];
                            tstart_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrsend[j]]);
                            k--;
                        }//while
                        k--;
                    }//while
                }//if
                    k--;
                }//while
                }//if of negative peak
            }//if //if(tpeakindex1[j]>0)
        }//for
        return t_start;
    }  //T_start


    /*Detects end position of T wave. Parameters passed to the function are index of T peaks, filtered ECG signal and differentiated ECG signal. Distinct T Positive peak (maximum value) and negative peak (minimum value) is checked with respect to value of 10 samples on either side
    With reference to t peak position when a positive peak is detected in filtered ECG, check_postv_Tend function is called which determines positive T peaks end index.
    Similar approach is taken if we have negative T peak by calling check_negtv_Tend to determine negative T peaks end index.*/
    private int[] T_end(int[] tpeakindex1, float[] ecg_val, float[] ecg_der, int[] qrsend, int[] p_start)
    {
        int[] t_end = new int[NO_OF_PEAKS];
        float pkecg_der,peak_val;
        int c,k,j,flag1,t_endwindow;

        t_endwindow=T_ENDWINDOW;
        //t_endwindow=T_ENDWINDOW/rr_factor;
        for(j=0;j<NO_OF_PEAKS;j++)
        {
            if((tpeakindex1[j]>0)&&(tpeakindex1[j]<(MAX_ECG_SAMPLES-t_endwindow)))
            {
                peak_val= calculate_avg_level(ecg_val[tpeakindex1[j]],ecg_val[qrsend[j]]);
                k=tpeakindex1[j]+2;//2 is added for compensation of samples after differenciation
                flag1=1;
                pkecg_der = ecg_der[k];
                for (c=1; c<100; c++)   //searching peak in differnciated signal
                    if (ecg_der[k-c]>pkecg_der) pkecg_der= ecg_der[k-c];

                if (((ecg_val[tpeakindex1[j]])>(ecg_val[tpeakindex1[j]-10]))&&((ecg_val[tpeakindex1[j]])>=(ecg_val[tpeakindex1[j]+10])))  //checking positive T peak
                    t_end = check_postv_Tend(tpeakindex1,ecg_val, ecg_der,p_start, k,j,pkecg_der, peak_val,qrsend);
                if (((ecg_val[tpeakindex1[j]])<(ecg_val[tpeakindex1[j]-10]))&&((ecg_val[tpeakindex1[j]])<=(ecg_val[tpeakindex1[j]+10]))) //negative peak
                    t_end = check_negtv_Tend(tpeakindex1,ecg_val, ecg_der,p_start, k,j,pkecg_der, peak_val,qrsend);
            }//if//if(tpeakindex1[j]>0)
        } //for
        return t_end;
    }//T_end


    /*With T peak index as reference and within search window of 60 samples, a small immediate negative peak is searched in differentiated signal towards right.
    Threshold is calculated (compute_threshold). When differentiated signal crosses the threshold,T end position is detected. If T end is greater than 10% of amplitude of T peak, next peak is searched.
    Search is locked within t_endwindow samples from T peak towards right. t_endwindow is 60 samples.If Tend is not determined as above or within the search window of 60 samples,
    search end point is considered as Tend.If Pstart of next cycle is before T end of previous cycle, Pstart index is considered as T end point.*/
    private int[] check_postv_Tend(int[] tpeakindex1, float[] ecg_val, float[] ecg_der, int[] p_start, int k, int javal, float   pkecg_der, float peak_val, int[] qrsend)
    {
        int[] t_end = new int[NO_OF_PEAKS];
        float threshold,tend_ecg_val;
        int flag1,j,t_endwindow;
        t_endwindow=T_ENDWINDOW;

        flag1=1; j=javal;
        while ((flag1==1)&&(k<(tpeakindex1[j]+t_endwindow))&&(k<MAX_ECG_SAMPLES-1))	  //t end should not be beyond 60 samples after t peak towards right
        {
            if (ecg_der[k]<ecg_der[k+1]&&ecg_der[k]<=ecg_der[k-1])				  //searching negative peak in differnciated signal
            {
                threshold=compute_threshold(pkecg_der,ecg_der[k]);
                while ((flag1==1)&&(k<(tpeakindex1[j]+t_endwindow))&&(k<MAX_ECG_SAMPLES-1))	    //t end should not be beyond 50 samples after t peak towards right
                {
                    if (ecg_der[k]<threshold) {flag1=1;k++;}
                    else flag1=0;
                }//while
                if (k<(tpeakindex1[j]+t_endwindow))	   //t end should not be beyond 60 samples after t peak towards right
                    t_end[j]=k+2;
                else {t_end[j]=k+2; flag1=0;}
                if(j<p_start.length-1) {
                    if (p_start[j + 1] > 0) {
                        if ((p_start[j + 1]) > 0 && (t_end[j] > p_start[j + 1]))
                            t_end[j] = p_start[j + 1];
                    }
                }
                tend_ecg_val=ecg_val[k];
                tend_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrsend[j]]);
                while ((k<MAX_ECG_SAMPLES-1)&&(tend_ecg_val>0.1*peak_val))	  //if t end detected is more than 10% of T peak, search again
                {
                    flag1=1;
                    while ((flag1==1)&&(k<(tpeakindex1[j]+t_endwindow))&&(k<MAX_ECG_SAMPLES-1))		 //t end should not be beyond 60 samples after t peak towards right
                    {
                        if (ecg_der[k]<ecg_der[k+1]&&ecg_der[k]<=ecg_der[k-1])
                        {
                            threshold=compute_threshold(pkecg_der,ecg_der[k]);
                            while ((flag1==1)&&(k<(tpeakindex1[j]+t_endwindow))&&(k<MAX_ECG_SAMPLES-1))		   //t end should not be beyond 50 samples after t peak towards right
                            { if (ecg_der[k]<threshold) {flag1=1;k++;}
                            else flag1=0;
                            } //while
                            if (k<(tpeakindex1[j]+t_endwindow)) t_end[j]=k;
                            else  {t_end[j]=k; flag1=0;}
                            if ((p_start[j+1])>0&&(t_end[j]>p_start[j+1]))
                                t_end[j]=p_start[j+1];
                        }//if
                        tend_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrsend[j]]);
                        k++;
                    }//while
                    k++;
                }  //while
            } //if
            k++;
        } //while
        return t_end;
    }//check_postv_Tend

    /*With T peak index as reference and within search window of 60 samples, a small immediate positive peak is searched in differentiated signal towards right.
    Threshold is calculated (compute_threshold). When differentiated signal crosses the threshold, T end position is detected. If T end is greater than 10% of amplitude of T peak,
    next peak is searched. Search is locked within t_endwindow samples from T peak towards right. t_endwindow is 60 samples. If Tend is not determined as above or within the search window of 60 samples,
    search end point is considered as Tend. If Pstart of next cycle is before T end of previous cycle, Pstart index is considered as T end point.*/
    private int[] check_negtv_Tend(int[] tpeakindex1, float[] ecg_val, float[] ecg_der, int[] p_start, int k, int jval, float   pkecg_der, float peak_val, int[] qrsend)
    {
        int[] t_end = new int[NO_OF_PEAKS];
        float threshold,tend_ecg_val;
        int flag1,j,t_endwindow;
        t_endwindow=T_ENDWINDOW;
        flag1=1; j=jval;
        while ((flag1==1)&&(k<(tpeakindex1[j]+t_endwindow))&&(k<MAX_ECG_SAMPLES-1))						   //t end should not be beyond 50 samples after t peak towards right
        {
            if (ecg_der[k]>ecg_der[k+1]&&ecg_der[k]>=ecg_der[k-1])								  //searching positive peak in differenciated signal
            {
                threshold=compute_threshold(pkecg_der,ecg_der[k]);
                while ((flag1==1)&&(k<(tpeakindex1[j]+t_endwindow))&&(k<MAX_ECG_SAMPLES-1))
                {
                    if (ecg_der[k]>threshold) {flag1=1;k++;}
                    else flag1=0;
                } //while
                if (k<(tpeakindex1[j]+t_endwindow))
                    t_end[j]=k+2;
                else
                {
                    t_end[j]=k+2;
                    flag1=0;
                }
                if ((p_start[j+1])>0&&(t_end[j]>p_start[j+1]))
                    t_end[j]=p_start[j+1];
                tend_ecg_val=ecg_val[k];
                tend_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrsend[j]]);
                while((k<MAX_ECG_SAMPLES-1)&&(tend_ecg_val<0.1*peak_val))		   //if t end detected is more than 30% of T peak, search again
                {
                    flag1=1;
                    while ((flag1==1)&&(k<(tpeakindex1[j]+t_endwindow))&&(k<MAX_ECG_SAMPLES-1))
                    {
                        if (ecg_der[k]>ecg_der[k+1]&&ecg_der[k]>=ecg_der[k-1])
                        {
                            threshold=compute_threshold(pkecg_der,ecg_der[k]);
                            while ((flag1==1)&&(k<(tpeakindex1[j]+t_endwindow))&&(k<MAX_ECG_SAMPLES-1))
                            {
                                if (ecg_der[k]>threshold) {flag1=1;k++;}
                                else flag1=0;
                            }
                            if (k<(tpeakindex1[j]+t_endwindow))
                                t_end[j]=k+2;
                            else
                            {t_end[j]=k+2; flag1=0;}
                            if ((p_start[j+1])>0&&(t_end[j]>p_start[j+1]))
                                t_end[j]=p_start[j+1];
                        }//if
                        tend_ecg_val= calculate_avg_level(ecg_val[k],ecg_val[qrsend[j]]);
                        k++;
                    }//while
                    k++;
                }//while
            }//if
            k++;
        } //while
        return t_end;
    }//check_negtv_Tend


    private static float compute_threshold(float pkecg_der,float maxval)
    {
        float constant,threshold1;

        constant=(maxval*10)/pkecg_der;
        if (constant<=0.13) constant=4.0f;
        else if ((0.13<constant)&&(constant<=0.20)) constant=5.0f;
        else if ((0.20<constant)&&(constant<=0.41)) constant=6.0f;
        else if (constant>0.41) constant=7.0f;

        threshold1=maxval/constant;
        return threshold1;
    }

    private float peak_amplitude(int[] peakindex, float[] ecg_val, int dispeak, int no_of_pks, int[] p_start, int[] p_end, int[] qrs_start, int[] qrs_end, int[] t_end)
    {
        float sum_amp,avg_dc1,avg_amp=0;
        float[] peak_amp = new float[NO_OF_PEAKS];
        int length;
        for (int i=0;i<NO_OF_PEAKS;i++) {
            peak_amp[i] = 0;
        }
        sum_amp=0;
        length=0;
        for (int i=1;i<no_of_pks-1;i++)
        {
            if (peakindex[i]>0)							//calculating only for peaks
            {
                if((multiLeadData[1][T_INDEX][i]>0)&&(multiLeadData[1][P_INDEX][i]>0))
                {
                    if ((abs(peakindex[i]-multiLeadData[1][T_INDEX][i]))<(abs(peakindex[i]-multiLeadData[1][P_INDEX][i]))) {
                        avg_dc1 = isoelectric_line(i, ecg_val, p_start, p_end, qrs_start, qrs_end, t_end);
                    }
                    else {
                        avg_dc1 = isoelectric_line(i - 1, ecg_val, p_start, p_end, qrs_start, qrs_end, t_end);
                    }
                }
                else {
                    avg_dc1 = isoelectric_line(i, ecg_val, p_start, p_end, qrs_start, qrs_end, t_end);
                }
                if (avg_dc1>=0) {
                    peak_amp[i] = ecg_val[peakindex[i]] - avg_dc1;
                }
                else {
                    peak_amp[i] = ecg_val[peakindex[i]] + Math.abs(avg_dc1);
                }

                sum_amp+=peak_amp[i];
                length++;
                avg_amp=sum_amp/length;
            }	//if

        }//for

        return peak_amp[dispeak];
        //return avg_amp;
    }
    private float isoelectric_line(int i, float[] fecgval, int[] p_start, int[] p_end, int[] qrs_start, int[] qrs_end, int[] t_end)
    {
        float sum1,avg_dc1;
        float[] ecg_val = new float [MAX_ECG_SAMPLES];
        int n,dc_length,start_count,end_count;

        sum1=0 ;
        dc_length =0;
        avg_dc1=0;

        for (int j=0;j<MAX_ECG_SAMPLES;j++) {
            ecg_val[j] = fecgval[j];
        }

        start_count=0;
        end_count=0;

        if (t_end[i]>0)
        {
            if(p_start[i+1]>0)
            {
                start_count=t_end[i];
                end_count=p_start[i+1];
            }
            else if ((p_start[i+1]==0)&&(qrs_start[i]>0))
            {
                start_count=t_end[i];
                end_count=qrs_start[i+1];
            }
        }
        else if ((t_end[i]==0)&&(qrs_end[i]>0))
        {
            if(p_start[i+1]>0)
            {
                start_count=qrs_end[i];
                end_count=p_start[i+1];
            }
            else if ((p_start[i+1]==0)&&(qrs_start[i]>0))
            {
                start_count=qrs_end[i];
                end_count=qrs_start[i+1];
            }
        }

        if(!((start_count==0)||(end_count==0)))
        {
            if (start_count==end_count)
            {
                end_count = start_count + 10;
            }

            if ((start_count>0)&&(end_count>0))
            {
                for(n=start_count; n<=end_count; n++)
                    if ((ecg_val[n]==ecg_val[n-1])&&(ecg_val[n]==ecg_val[n+1]))
                    { sum1+=ecg_val[n]; dc_length++; }
                if(dc_length>0) avg_dc1 =  sum1/(dc_length);
            }//if										//isoelectric line

            if (dc_length<10)
            {
                sum1=0;
                dc_length = 0;
                avg_dc1=0;
                for(n=start_count; n<=end_count; n++)
                {
                    sum1+=ecg_val[n];
                    dc_length++;
                }
                if(dc_length>0) {
                    avg_dc1 = sum1 / dc_length;
                }
            }
        }
        else avg_dc1=0;
        return avg_dc1;
    }

    private float ST_segment(float[] ecg_val, int[] qrs_end, int[] t_start, float avg_dc, int no_of_pks)

    {
        float[] st_level = new float[NO_OF_PEAKS];
        int[] st_levelindex = new int[NO_OF_PEAKS];

        int k;

        for (int j=0;j<NO_OF_PEAKS;j++)

        {
            if (qrs_end[j]>0)
            {
                k=qrs_end[j]+35;
                if (k<t_start[j]) st_levelindex[j]=k;
                else st_levelindex[j]=t_start[j];

                st_level[j]= calculate_avg_level(ecg_val[st_levelindex[j]],avg_dc);
            }  //if
        }//for
        return st_level[no_of_pks-2];

    }


    private float ST_slope(float[] ecg_val, int[] qrs_end, int[] t_start, int no_of_pks)
    {
        float[] st_slope = new float[NO_OF_PEAKS];
        int[] slope_end = new int[NO_OF_PEAKS];

        for (int j=0;j<NO_OF_PEAKS;j++)

        {
            if (qrs_end[j]>0) {
                if (t_start[j] > 0) {
                    slope_end[j] = t_start[j];
                } else {
                    slope_end[j] = qrs_end[j] + 60;
                }
                if (slope_end[j] < ecg_val.length) {   /* 20210713 - DIBC KN : Added if condition to prevent index out of bounds */
                    st_slope[j] = (ecg_val[slope_end[j]] - ecg_val[qrs_end[j]]) / (slope_end[j] - qrs_end[j]);
                }
            }
        }
        return st_slope[no_of_pks-2];
    }

    /* calculates avg of interval between two points for all the cycles.Two arrays with indices for all cycles are passed.First same no. of peaks are confirmed.
    after counting peaks in each array else error message will popup. interval between two positions for last 3 nonzero peaks are averaged.
    */
    private float avg_peakinterval(int[] peakindex1, int[] peakindex2, int lead_index, int dispeak)
    {
        float interval,count;
        float avg_int=0;
        String s;

        interval=0;
        count=0;
        for (int i=0;i<NO_OF_PEAKS;i++)
        {
            if (peakindex1[i]>0)
                if (peakindex2[i]>0)
                {
                    interval= interval+(peakindex1[i]-peakindex2[i]);
                    count=count+1;
                }
        }
        s = lead_name[lead_index];
        if (count>0) {
            avg_int = interval / count;
        }
        else {
//            MessagePopup(s, "No peaks detected,so interval cannot be calculated");
            // TODO: 1/21/21 alert user
        }

//        return (peakindex1[dispeak]-peakindex2[dispeak]);
        return avg_int;
        // TODO: 29-01-2021 Check with BARC. average not being used
    } // avg_peakinterval

}


