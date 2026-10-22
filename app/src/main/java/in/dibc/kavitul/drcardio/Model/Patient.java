package in.dibc.kavitul.drcardio.Model;

public class Patient {
    private int PatientUniqueID;
    private String Patient_Fname;
    private String Patient_Mname;
    private String Patient_Lname;
    private String Patient_DOB;
    private String Patient_Mobile1;
    private String Patient_Mobile2;

    // Getters
    public int getPatientUniqueID() {
        return PatientUniqueID;
    }

    public String getPatient_Fname() {
        return Patient_Fname;
    }

    public String getPatient_Mname() {
        return Patient_Mname;
    }

    public String getPatient_Lname() {
        return Patient_Lname;
    }

    public String getPatient_DOB() {
        return Patient_DOB;
    }

    public String getPatient_Mobile1() {
        return Patient_Mobile1;
    }

    public String getPatient_Mobile2() {
        return Patient_Mobile2;
    }

    @Override
    public String toString() {
        return "Patient{" +
                "PatientUniqueID=" + PatientUniqueID +
                ", Patient_Fname='" + Patient_Fname + '\'' +
                ", Patient_Mname='" + Patient_Mname + '\'' +
                ", Patient_Lname='" + Patient_Lname + '\'' +
                ", Patient_DOB='" + Patient_DOB + '\'' +
                ", Patient_Mobile1='" + Patient_Mobile1 + '\'' +
                ", Patient_Mobile2='" + Patient_Mobile2 + '\'' +
                '}';
    }
}

