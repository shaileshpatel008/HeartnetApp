package in.dibc.kavitul.drcardio.apis.request;

import com.google.gson.annotations.SerializedName;

public class PatientRegisterRequest {
    @SerializedName("PatientID")
    private String patientID;

    @SerializedName("Patient_Fname")
    private String firstName;

    @SerializedName("Patient_Mname")
    private String middleName;

    @SerializedName("Patient_Lname")
    private String lastName;

    @SerializedName("Patient_DOB")
    private String dob;

    @SerializedName("Patient_Gender")
    private String gender;

    @SerializedName("Patient_Mobile1")
    private String mobile1;

    @SerializedName("Patient_Mobile2")
    private String mobile2;

    @SerializedName("Patient_Mailid")
    private String email;

    @SerializedName("Patient_Addr1")
    private String address1;

    @SerializedName("Patient_Addr2")
    private String address2;

    @SerializedName("Patient_City")
    private String city;

    @SerializedName("Patient_PinCode")
    private String pincode;

    @SerializedName("IDNumber")
    private String idNumber;

    @SerializedName("IDType")
    private String idType;

    @SerializedName("IsInsured")
    private boolean isInsured;

    @SerializedName("InsuranceProviderID")
    private String insuranceProviderID;

    @SerializedName("InstGUID")
    private String instGUID;

    // Constructor
    public PatientRegisterRequest(String patientID, String firstName, String middleName, String lastName, String dob,
                   String gender, String mobile1, String mobile2, String email, String address1,
                   String address2, String city, String pincode, String idNumber, String idType,
                   /*boolean isInsured,*/ String insuranceProviderID, String instGUID) {
        this.patientID = patientID;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.dob = dob;
        this.gender = gender;
        this.mobile1 = mobile1;
        this.mobile2 = mobile2;
        this.email = email;
        this.address1 = address1;
        this.address2 = address2;
        this.city = city;
        this.pincode = pincode;
        this.idNumber = idNumber;
        this.idType = idType;
//        this.isInsured = isInsured;
        this.insuranceProviderID = insuranceProviderID;
        this.instGUID = instGUID;
    }
}
