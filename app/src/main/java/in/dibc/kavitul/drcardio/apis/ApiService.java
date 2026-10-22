package in.dibc.kavitul.drcardio.apis;

import java.util.List;

import in.dibc.kavitul.drcardio.Model.ScanResponse;
import in.dibc.kavitul.drcardio.apis.request.AuthRequest;
import in.dibc.kavitul.drcardio.apis.request.PatientHistoryRequest;
import in.dibc.kavitul.drcardio.apis.request.PatientRegisterRequest;
import in.dibc.kavitul.drcardio.apis.request.PatientRequest;
import in.dibc.kavitul.drcardio.apis.response.AuthResponse;
import in.dibc.kavitul.drcardio.apis.response.DownloadResponse;
import in.dibc.kavitul.drcardio.apis.response.GetScanResponseModel;
import in.dibc.kavitul.drcardio.apis.response.PatientDetailsResponse;
import in.dibc.kavitul.drcardio.apis.response.PatientRegisterResponse;
import in.dibc.kavitul.drcardio.apis.response.PatientResponse;
import in.dibc.kavitul.drcardio.apis.response.UploadResponse;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Part;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("ecgapiAdvance/getpatientlist")
    Call<PatientResponse> getPatientList(@Body PatientRequest request);

    @GET
    Call<PatientResponse> getPatients(@Url String url);

    @Headers("Content-Type: application/json")
    @POST("ecgapiAdvance/registerpatient")
    Call<PatientRegisterResponse> registerPatient(@Body PatientRegisterRequest patient);

    @GET("ecgapiAdvance/getpatientdetails")
    Call<PatientDetailsResponse> getPatientDetails(@Query("patientid") String patientId);

    @POST("ecgapiAdvance/savepatienthistory")
    Call<PatientRegisterResponse> savePatientHistory(@Header("token") String token, @Body PatientHistoryRequest patient);

    @Multipart
    @POST("ecgapiadvance/upload")  // Define endpoint
    Call<UploadResponse> uploadECGFiles(
            @Header("token") String token,
//            @Part MultipartBody.Part file1,
//            @Part MultipartBody.Part file2
            @Part List<MultipartBody.Part> files
    );

    @Headers("Content-Type: application/json")
    @POST("ecgapiAdvance/authenticate")
    Call<AuthResponse> authenticate(
            @Body AuthRequest request
    );

    @GET("ecgapiAdvance/downloadreport")
    Call<DownloadResponse> downloadReport(
            @Header("token") String token,
            @Header("ScanID") String scanID
    );

    @GET("ecgapiAdvance/getscan") //ecgapi
    Call<GetScanResponseModel> getReportDetails(
            @Header("token") String token,
            @Query("studyUniqueId") String studyUniqueId
    );

    @GET("ecgapiAdvance/getscans/?") //ecgapi
    Call<ScanResponse> getScans(
//            @Query("numberOfRows") int numberOfRows,
            @Query("isReported") String isReported,
//            @Query("search") String search,
            @Query("searchPatientName") String patientName,
            @Query("showAllRecords") boolean showAllRecords,
            @Query("pageSize") int pageSize,
            @Query("pageNumber") int pageNumber,
            @Header("token") String authHeader
    );
}

