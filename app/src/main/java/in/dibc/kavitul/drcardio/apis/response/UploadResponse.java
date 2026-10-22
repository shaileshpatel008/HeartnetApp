package in.dibc.kavitul.drcardio.apis.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UploadResponse {
    @SerializedName("Response")
    private String response;

    @SerializedName("IsSuccess")
    private boolean isSuccess;

    @SerializedName("UserID")
    private int userID;

    public String getResponse() {
        return response;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public int getUserID() {
        return userID;
    }

    @Override
    public String toString() {
        return "UploadResponse{" +
                "response='" + response + '\'' +
                ", isSuccess=" + isSuccess +
                ", userID=" + userID +
                '}';
    }
}
