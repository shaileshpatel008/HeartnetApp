package in.dibc.kavitul.drcardio.apis.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PatientResponse {
    @SerializedName("Response")
    private String response; // Store it as a String initially

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
}

//public class PatientResponse {
//    @SerializedName("Response")
//    private List<PatientData> response;
//
//    @SerializedName("IsSuccess")
//    private boolean isSuccess;
//
//    @SerializedName("UserID")
//    private int userID;
//
//    public List<PatientData> getResponse() {
//        return response;
//    }
//
//    public boolean isSuccess() {
//        return isSuccess;
//    }
//
//    public int getUserID() {
//        return userID;
//    }
//}
//
//// Model for individual patient data
//class PatientData {
//    @SerializedName("PatientUniqueID")
//    private int patientID;
//
//    @SerializedName("Patient_Fname")
//    private String firstName;
//
//    @SerializedName("Patient_Mname")
//    private String middleName;
//
//    @SerializedName("Patient_Lname")
//    private String lastName;
//
//    @SerializedName("Patient_DOB")
//    private String dob;
//
//    @SerializedName("Patient_Mobile1")
//    private String mobile1;
//
//    @SerializedName("Patient_Mobile2")
//    private String mobile2;
//
//    public int getPatientID() {
//        return patientID;
//    }
//
//    public String getFirstName() {
//        return firstName;
//    }
//
//    public String getMiddleName() {
//        return middleName;
//    }
//
//    public String getLastName() {
//        return lastName;
//    }
//
//    public String getDob() {
//        return dob;
//    }
//
//    public String getMobile1() {
//        return mobile1;
//    }
//
//    public String getMobile2() {
//        return mobile2;
//    }
//}

