package in.dibc.kavitul.drcardio.apis.request;

import com.google.gson.annotations.SerializedName;

public class PatientRequest {
    @SerializedName("Patient_Fname")
    private String firstName;

    @SerializedName("Patient_Mname")
    private String middleName;

    @SerializedName("Patient_Lname")
    private String lastName;

    @SerializedName("Patient_DOB")
    private String dob;

    @SerializedName("Patient_Mobile")
    private String mobile;

    @SerializedName("UnitCode")
    private String unitCode;

    public PatientRequest(String firstName, String middleName, String lastName, String dob, String mobile, String unitCode) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.dob = dob;
        this.mobile = mobile;
        this.unitCode = unitCode;
    }
}
