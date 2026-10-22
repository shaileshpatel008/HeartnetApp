package in.dibc.kavitul.drcardio.apis.response;

public class DownloadResponseData {
    private int AssigneeId;
    private boolean IsReported;
    private String PatientName;
    private String PatientID;
    private String StudyDate;
    private int StudyUniqueId;
    private String UploadUserName;
    private String ImageUrl;
    private String Interpretation;
    private String ScanId;
    private String UploadUserMobileNum;
    private String InstitutionName;

    public int getAssigneeId() {
        return AssigneeId;
    }

    public boolean isReported() {
        return IsReported;
    }

    public String getPatientName() {
        return PatientName;
    }

    public String getPatientID() {
        return PatientID;
    }

    public String getStudyDate() {
        return StudyDate;
    }

    public int getStudyUniqueId() {
        return StudyUniqueId;
    }

    public String getUploadUserName() {
        return UploadUserName;
    }

    public String getImageUrl() {
        return ImageUrl;
    }

    public String getInterpretation() {
        return Interpretation;
    }

    public String getScanId() {
        return ScanId;
    }

    public String getUploadUserMobileNum() {
        return UploadUserMobileNum;
    }

    public String getInstitutionName() {
        return InstitutionName;
    }
}
