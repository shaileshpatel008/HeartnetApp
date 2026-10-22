package in.dibc.kavitul.drcardio.apis.request;

import com.google.gson.annotations.SerializedName;

public class PatientHistoryRequest {
    @SerializedName("PatientID")
    private String patientID;

    @SerializedName("HistoryText")
    private String historyText;

    @SerializedName("ReferralDoctor")
    private String referralDoctor;

    // Constructor
    public PatientHistoryRequest(String patientID, String historyText, String referralDoctor) {
        this.patientID = patientID;
        this.historyText = historyText;
        this.referralDoctor = referralDoctor;
    }
}
