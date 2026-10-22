package in.dibc.kavitul.drcardio.apis.response;

import com.google.gson.annotations.SerializedName;

public class PatientDetailsResponse {
    @SerializedName("Response")
    private String response; // Treat it as String first

    @SerializedName("IsSuccess")
    private boolean isSuccess;

    @SerializedName("UserID")
    private int userId;

    public String getResponse() {
        return response;
    }

    public boolean isSuccess() {
        return isSuccess;
    }
}

