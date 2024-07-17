package in.dibc.kavitul.drcardio.Model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class ECGDataModel extends BaseModel {

    @SerializedName("device_id")
    private String device_id;
    @SerializedName("latitude")
    private String latitude;
    @SerializedName("longitude")
    private String longitude;
    @SerializedName("device_info")
    private String device_info;
    @SerializedName("patient_info")
    private String patient_info;
    @SerializedName("address")
    private String address;
    @SerializedName("pdf_name")
    private String pdf_name;
    @SerializedName("pdf_file_path")
    private String pdf_file_path;

    @SerializedName("hr")
    private String hr;

    @SerializedName("r")
    private String r;

    @SerializedName("rr")
    private String rr;

    @SerializedName("pr")
    private String pr;

    @SerializedName("qrs")
    private String qrs;

    @SerializedName("qt")
    private String qt;

    @SerializedName("qtc")
    private String qtc;

    @SerializedName("qt_by_qtc")
    private String qt_by_qtc;

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getDevice_info() {
        return device_info;
    }

    public void setDevice_info(String device_info) {
        this.device_info = device_info;
    }

    public String getPatient_info() {
        return patient_info;
    }

    public void setPatient_info(String patient_info) {
        this.patient_info = patient_info;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPdf_name() {
        return pdf_name;
    }

    public void setPdf_name(String pdf_name) {
        this.pdf_name = pdf_name;
    }

    public String getPdf_file_path() {
        return pdf_file_path;
    }

    public void setPdf_file_path(String pdf_file_path) {
        this.pdf_file_path = pdf_file_path;
    }

    public String getHr() {
        return hr;
    }

    public void setHr(String hr) {
        this.hr = hr;
    }

    public String getR() {
        return r;
    }

    public void setR(String r) {
        this.r = r;
    }

    public String getRr() {
        return rr;
    }

    public void setRr(String rr) {
        this.rr = rr;
    }

    public String getPr() {
        return pr;
    }

    public void setPr(String pr) {
        this.pr = pr;
    }

    public String getQrs() {
        return qrs;
    }

    public void setQrs(String qrs) {
        this.qrs = qrs;
    }

    public String getQt() {
        return qt;
    }

    public void setQt(String qt) {
        this.qt = qt;
    }

    public String getQtc() {
        return qtc;
    }

    public void setQtc(String qtc) {
        this.qtc = qtc;
    }

    public String getQt_by_qtc() {
        return qt_by_qtc;
    }

    public void setQt_by_qtc(String qt_by_qtc) {
        this.qt_by_qtc = qt_by_qtc;
    }
}
