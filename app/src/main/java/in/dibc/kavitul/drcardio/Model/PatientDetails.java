package in.dibc.kavitul.drcardio.Model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PatientDetails {
    @SerializedName("PatientID")
    private int patientId;

    @SerializedName("Patient_Fname")
    private String firstName;

    @SerializedName("Patient_Lname")
    private String lastName;

    @SerializedName("Patient_DOB")
    private String dateOfBirth;

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

    @SerializedName("EventTable")
    private List<Event> eventTable; // Event history

    // Getters
    public int getPatientId() { return patientId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getGender() { return gender; }
    public String getMobile1() { return mobile1; }
    public String getMobile2() { return mobile2; }
    public String getEmail() { return email; }
    public String getAddress1() { return address1; }
    public String getAddress2() { return address2; }
    public List<Event> getEventTable() { return eventTable; }
}

class Event {
    @SerializedName("EventDate")
    private String eventDate;

    @SerializedName("Eventtype")
    private String eventType;

    @SerializedName("PatientEventsID")
    private int eventId;

    public String getEventDate() { return eventDate; }
    public String getEventType() { return eventType; }
    public int getEventId() { return eventId; }
}


