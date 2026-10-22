package in.dibc.kavitul.drcardio;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;

import in.dibc.kavitul.drcardio.Adapter.ScanAdapter;
import in.dibc.kavitul.drcardio.Model.ScanModel;
import in.dibc.kavitul.drcardio.Model.ScanResponse;
import in.dibc.kavitul.drcardio.Model.ecgData;
import in.dibc.kavitul.drcardio.SupportClass.ECGReportGenerator;
import in.dibc.kavitul.drcardio.apis.ApiService;
import in.dibc.kavitul.drcardio.apis.RetrofitClient;
import in.dibc.kavitul.drcardio.apis.response.DownloadResponse;
import in.dibc.kavitul.drcardio.apis.response.DownloadResponseData;
import in.dibc.kavitul.drcardio.apis.response.GetScanResponseModel;
import in.dibc.kavitul.drcardio.utils.PathUtil;
import in.dibc.kavitul.drcardio.utils.PrefHelper;
import in.dibc.kavitul.drcardio.utils.VerticalSpaceItemDecoration;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScanListFragment extends Fragment implements ScanAdapter.OnScanClickListener{

    private static final String ARG_REPORTED = "isReported";
    private boolean isReported;
    private RecyclerView recyclerView;
    private ScanAdapter adapter;
    private ProgressDialog progressDialog;

    private int currentPage = 1;
    private int totalPages = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int pageSize = 10;

    public static ScanListFragment newInstance(boolean isReported) {
        ScanListFragment fragment = new ScanListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_REPORTED, isReported);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) isReported = getArguments().getBoolean(ARG_REPORTED);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_list, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(20));
        adapter = new ScanAdapter();
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0) {

                            // 🔥 Load next page
                            fetchScans(currentPage + 1);
                        }
                    }
                }
            }
        });

        fetchScans(1);

        return view;
    }

    @Override
    public void onScanClick(ScanModel model, int position) {
        // Handle the click event here
//        Toast.makeText(getContext(), "Clicked: " + model.getPatientName(), Toast.LENGTH_SHORT).show();

        if(isReported) {
            callDownloadReportsAPI(model.getScanId());
        } else {
            Intent intent = new Intent(getContext(), FullScreenImageActivity.class);
            intent.putExtra("ImagePath", model.getImageUrl().replaceAll("//ECGImages/", "//"));
            startActivity(intent);
        }
    }

    private void fetchScans(int page) {

//        if (page == 1) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Getting scans...");
            progressDialog.setCancelable(false);
            progressDialog.show();
//        }

        isLoading = true;

        ApiService apiService = RetrofitClient.getApiService();

        String strReported = "False";
        if(isReported) {
            strReported = "True";
        }

        Call<ScanResponse> call = apiService.getScans(
//                pageSize,
                strReported,
//                "",
                "",
                true,
                pageSize,
                page,
                PathUtil.TOKEN
        );

        call.enqueue(new Callback<ScanResponse>() {

            @Override
            public void onResponse(Call<ScanResponse> call, Response<ScanResponse> response) {

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                isLoading = false;

                try {

                    // 🔥 FIX: check the actual HTTP status code FIRST, before trying
                    // to parse the body at all. This is the authoritative signal for
                    // "the token is no longer valid" - unlike matching on the body's
                    // message text (which broke because the server sends
                    // {"message":"Token has expired"}, not the {"Response":...,
                    // "IsSuccess":...} shape the old code was looking for), a 401
                    // status code can't be missed just because the wording of the
                    // error message changes on the backend.
                    if (response.code() == 401) {
                        handleTokenExpired();
                        return;
                    }

                    // ✅ SUCCESS RESPONSE
                    if (response.isSuccessful() && response.body() != null) {

                        ScanResponse scansResponse = response.body();

                        // ✅ Token Expired Check (kept as a secondary check, in case
                        // some other endpoint reports token expiry via a 200 status
                        // with a failure flag in the body instead of a 401)
                        if (!scansResponse.isSuccess()
                                && scansResponse.getResponse() != null
                                && scansResponse.getResponse().toLowerCase().contains("token")) {

                            handleTokenExpired();
                            return;
                        }

                        // ✅ Parse List
                        JSONArray jsonArray =
                                new JSONArray(scansResponse.getResponse());

                        Gson gson = new Gson();

                        Type listType =
                                new TypeToken<List<ScanModel>>() {}.getType();

                        List<ScanModel> scanList =
                                gson.fromJson(jsonArray.toString(), listType);

                        // ✅ Pagination
                        if (scansResponse.getPagination() != null) {

                            currentPage =
                                    scansResponse.getPagination().getCurrentPage();

                            totalPages =
                                    scansResponse.getPagination().getTotalPages();

                            if (currentPage >= totalPages) {
                                isLastPage = true;
                            }
                        }

                        // ✅ Set / Append
                        if (currentPage == 1) {
                            adapter.setScans(scanList, ScanListFragment.this);
                        } else {
                            adapter.addScans(scanList);
                        }

                    } else {

                        // ✅ ERROR RESPONSE (other 4xx/5xx, not 401 - that's handled above)
                        String errorBody = "";

                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }

                        Log.e("API_ERROR", errorBody);

                        // 🔥 FIX: this JSON shape ({"message": "..."}) is what your
                        // server actually sends for errors - "Response"/"IsSuccess"
                        // (checked below as a fallback) never matched it.
                        JSONObject jsonObject = new JSONObject(errorBody);

                        String message = jsonObject.optString("message");
                        if (message.isEmpty()) {
                            // Fallback for endpoints that DO use the old
                            // Response/IsSuccess shape.
                            message = jsonObject.optString("Response");
                        }

                        boolean isSuccess = jsonObject.optBoolean("IsSuccess", false);

                        // ✅ Token Expired (belt-and-suspenders text match, in case a
                        // future endpoint reports it via 200 + a message instead of a
                        // real 401 - the response.code() == 401 check above is still
                        // the primary, more reliable path)
                        if (!isSuccess &&
                                message != null &&
                                message.toLowerCase().contains("token")) {

                            handleTokenExpired();
                            return;
                        }

                        Toast.makeText(getActivity(),
                                message.isEmpty() ? "Something went wrong" : message,
                                Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {

                    Log.e("EXCEPTION", e.getMessage());

                    Toast.makeText(getActivity(),
                            "Something went wrong",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ScanResponse> call, Throwable t) {

                isLoading = false;

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                Log.e("API_FAILURE", t.getMessage());

                Toast.makeText(getActivity(),
                        "Network error",
                        Toast.LENGTH_SHORT).show();
            }


        });
    }

    // ✅ Common Method
    private void handleTokenExpired() {

        Toast.makeText(getActivity(),
                "Session expired. Please login again.",
                Toast.LENGTH_LONG).show();

                // Clear Session
                PrefHelper.setBoolean("isUserLoggedIn", false);
                PrefHelper.setString("AppToken", "");
                PathUtil.TOKEN = "";

                // Redirect Login
                Intent intent =
                        new Intent(getActivity(), SignInActivity.class);

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);

                requireActivity().finish();
    }

    private void callDownloadReportsAPI(String ScanID) {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Downloading report...");
        progressDialog.setCancelable(false); // Prevents dismissal on touch outside
        progressDialog.show();

        ApiService apiService = RetrofitClient.getApiService();

        Log.v("Token ::", "Token ::"+PathUtil.TOKEN);

//        DownloadRequest downloadRequest = new DownloadRequest(ScanID);

        Call<DownloadResponse> call = apiService.downloadReport(PathUtil.TOKEN, ScanID);
        call.enqueue(new Callback<DownloadResponse>() {
            @Override
            public void onResponse(Call<DownloadResponse> call, Response<DownloadResponse> response) {
                progressDialog.dismiss();

                if (response.code() == 401) {
                    handleTokenExpired();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {

                    Log.d("GET_SUCCESS", "Response: " + response.body().getResponse());

                    // 🔥 FIX: check the actual HTTP status code FIRST, before trying
                    // to parse the body at all. This is the authoritative signal for
                    // "the token is no longer valid" - unlike matching on the body's
                    // message text (which broke because the server sends
                    // {"message":"Token has expired"}, not the {"Response":...,
                    // "IsSuccess":...} shape the old code was looking for), a 401
                    // status code can't be missed just because the wording of the
                    // error message changes on the backend.

                    try {
                        // Get Response string
                        String responseString = response.body().getResponse();

                        // Response is a JSON Array
                        JSONArray jsonArray = new JSONArray(responseString);

                        if (jsonArray.length() > 0) {

                            // Get first report object
                            JSONObject reportObject = jsonArray.getJSONObject(0);

                            // Report Details
                            String reportAnalysis = reportObject.optString("ReportDetails", "");
                            Log.v(
                                    "ReportDetails :: ",
                                    "ReportDetails :: " + reportAnalysis
                            );

                            // Unique Study ID
                            String uniqueStudyId = reportObject.optString("UniqueStudyId", "");
                            Log.v(
                                    "UniqueStudyId :: ",
                                    "UniqueStudyId :: " + uniqueStudyId
                            );

                            // Cardiologist Name
                            String cardiologistName = reportObject.optString("Cardiologist", "");
                            Log.v(
                                    "CardiologistName :: ",
                                    "CardiologistName :: " + cardiologistName
                            );

                            // Cardiologist Signature URL
                            String cardiologistSignatureURL =
                                    reportObject.optString("CardiologistSignatureURL", "");

                            Log.v(
                                    "CardiologistSignatureURL :: ",
                                    "CardiologistSignatureURL :: " + cardiologistSignatureURL
                            );

                            // Call next API
                            callGetReportsDetailsAPI(
                                    uniqueStudyId,
                                    reportAnalysis,
                                    cardiologistName,
                                    cardiologistSignatureURL
                            );

                        } else {

                            progressDialog.dismiss();

                            Log.e(
                                    "JSON_PARSING_ERROR",
                                    "Response array is empty"
                            );
                        }

                    } catch (JSONException e) {

                        progressDialog.dismiss();

                        Log.e(
                                "JSON_PARSING_ERROR",
                                "JSON Parsing Error: " + e.getMessage(),
                                e
                        );

                    } catch (Exception e) {

                        progressDialog.dismiss();

                        Log.e(
                                "ERROR",
                                "Error processing response: " + e.getMessage(),
                                e
                        );
                    }

                } else {

                    progressDialog.dismiss();

                    Log.e(
                            "GET_ERROR",
                            "Error: " + response.errorBody()
                    );
                }
            }

            @Override
            public void onFailure(Call<DownloadResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("GET_FAILURE", "Failed: " + t.getMessage());
            }
        });
    }

    private void callGetReportsDetailsAPI(String studyUniqueId, String reportAnalysis, String cardiologistName, String cardiologistSignatureURL) {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Fetching report details...");
        progressDialog.setCancelable(false); // Prevents dismissal on touch outside
        progressDialog.show();

        ApiService apiService = RetrofitClient.getApiService();

        Log.v("Token ::", "Token ::"+PathUtil.TOKEN);

//        DownloadRequest downloadRequest = new DownloadRequest(ScanID);

        Call<GetScanResponseModel> call = apiService.getReportDetails(PathUtil.TOKEN, studyUniqueId);
        call.enqueue(new Callback<GetScanResponseModel>() {
            @Override
            public void onResponse(Call<GetScanResponseModel> call, Response<GetScanResponseModel> response) {
                progressDialog.dismiss();

                if (response.code() == 401) {
                    handleTokenExpired();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {

                    Log.d(
                            "GET_SUCCESS",
                            "Response: " + response.body().getResponse()
                    );

                    try {

                        DownloadResponseData responseData =
                                response.body().getResponse();

                        if (responseData != null) {

                            String reportImageURL =
                                    responseData.getImageUrl();

                            Log.v(
                                    "ReportImageURL GIF :: ",
                                    "ReportImageURL GIF :: " + reportImageURL
                            );

                            if (reportImageURL != null &&
                                    !reportImageURL.isEmpty()) {

                                new ECGReportGenerator.ECGReportGeneratorTask(
                                        getContext(),
                                        reportImageURL,
                                        reportAnalysis,
                                        "",
                                        cardiologistName,
                                        cardiologistSignatureURL
                                ).execute();

                            } else {

                                progressDialog.dismiss();

                                Log.e(
                                        "API_ERROR",
                                        "ImageUrl is empty or null"
                                );
                            }

                        } else {

                            progressDialog.dismiss();

                            Log.e(
                                    "API_ERROR",
                                    "Response object is null"
                            );
                        }

                    } catch (Exception e) {

                        progressDialog.dismiss();

                        Log.e(
                                "API_ERROR",
                                "Error: " + e.getMessage(),
                                e
                        );
                    }

                } else {

                    progressDialog.dismiss();

                    Log.e(
                            "GET_ERROR",
                            "Error: " + response.errorBody()
                    );
                }
            }

            @Override
            public void onFailure(Call<GetScanResponseModel> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("GET_FAILURE", "Failed: " + t.getMessage());
            }
        });
    }
}