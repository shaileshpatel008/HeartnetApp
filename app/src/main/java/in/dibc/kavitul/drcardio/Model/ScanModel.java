package in.dibc.kavitul.drcardio.Model;

public class ScanModel {
    private String StudyUniqueId;
    private String ScanId;
    private String StudyDate;
    private String ImageUrl;
    private String PatientName; // Assuming the image URL is directly provided or parsed from data
    private String InstitutionName;
    private String UploadUserName;
    private boolean IsAbnormal;

    public String getStudyUniqueId() {
        return StudyUniqueId;
    }

    public void setStudyUniqueId(String studyUniqueId) {
        StudyUniqueId = studyUniqueId;
    }

    public String getScanId() {
        return ScanId;
    }

    public void setScanId(String scanId) {
        ScanId = scanId;
    }

    public String getStudyDate() {
        return StudyDate;
    }

    public void setStudyDate(String studyDate) {
        StudyDate = studyDate;
    }

    public String getImageUrl() {
        return ImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        ImageUrl = imageUrl;
    }

    public String getPatientName() {
        return PatientName;
    }

    public void setPatientName(String patientName) {
        PatientName = patientName;
    }

    public String getInstitutionName() {
        return InstitutionName;
    }

    public void setInstitutionName(String institutionName) {
        InstitutionName = institutionName;
    }

    public String getUploadUserName() {
        return UploadUserName;
    }

    public void setUploadUserName(String uploadUserName) {
        UploadUserName = uploadUserName;
    }

    public boolean isAbnormal() {
        return IsAbnormal;
    }

    public void setAbnormal(boolean abnormal) {
        IsAbnormal = abnormal;
    }
}

