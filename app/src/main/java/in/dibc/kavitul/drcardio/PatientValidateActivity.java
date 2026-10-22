package in.dibc.kavitul.drcardio;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import in.dibc.kavitul.drcardio.Adapter.PatientAdapter;
import in.dibc.kavitul.drcardio.Model.Patient;
import in.dibc.kavitul.drcardio.SupportClass.settings;
import in.dibc.kavitul.drcardio.apis.ApiService;
import in.dibc.kavitul.drcardio.apis.RetrofitClient;
import in.dibc.kavitul.drcardio.apis.request.PatientRequest;
import in.dibc.kavitul.drcardio.apis.response.PatientResponse;
import in.dibc.kavitul.drcardio.utils.DateConverter;
import in.dibc.kavitul.drcardio.utils.PathUtil;
import in.dibc.kavitul.drcardio.utils.PrefHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PatientValidateActivity extends AppCompatActivity implements PatientAdapter.OnItemClickListener {

    private static final String TAG = "API_RESPONSE";

    private ProgressDialog progressDialog;

    private EditText etFirstName, etMiddleName, etLastName, etMobileNumber, etDateOfBirth;
    private Button btnSearch;
    private RecyclerView recyclerView;
    ImageView ivBack;
    private List<Patient> patientList;
    private PatientAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_validate);

        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        etMobileNumber = findViewById(R.id.etMobile);
        etDateOfBirth = findViewById(R.id.etDOB);
        btnSearch = findViewById(R.id.btnSearch);
        recyclerView = findViewById(R.id.recyclerView);
        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        patientList = new ArrayList<>();
        adapter = new PatientAdapter(patientList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Date Picker
//        etDateOfBirth.setOnClickListener(v -> openDatePicker());

        // Search Button Click
        btnSearch.setOnClickListener(v -> addUser());
    }

    @Override
    public void onItemClick(Patient patient) {
//        Toast.makeText(this, "Clicked: " + patient.getFirstName() + " " + patient.getLastName(), Toast.LENGTH_SHORT).show();
        Log.d("PATIENT_CLICK", "Selected Patient: " + patient.getPatient_Fname() + " " + patient.getPatient_Lname());
        Intent intent = new Intent(PatientValidateActivity.this, NewEcgActivity.class);
        intent.putExtra("PatientID", ""+patient.getPatientUniqueID());
        startActivity(intent);
        finish();
    }

    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            // Format with leading zeros
            String selectedDate = String.format("%02d-%02d-%d", dayOfMonth, month + 1, year);
            etDateOfBirth.setText(selectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis()); // Prevent future date selection
        datePickerDialog.show();
    }

    private void addUser() {

//        etFirstName.setText("Shailesh");
//        etLastName.setText("Patel");
//        etMobileNumber.setText("8511131807");
//        etDateOfBirth.setText("18-08-1990");

        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String mobile = etMobileNumber.getText().toString().trim();
        String dob = etDateOfBirth.getText().toString().trim();

        if (etFirstName.getText().toString().trim().isEmpty() && etMobileNumber.getText().toString().trim().isEmpty()) {
            etFirstName.setError("Please enter first name or mobile number to search patient details");
            etFirstName.requestFocus();
            Toast.makeText(this, "Please enter first name or mobile number to search patient details", Toast.LENGTH_SHORT).show();
            return;
        }

//        if (etFirstName.getText().toString().trim().isEmpty()) {
//            etFirstName.setError("Please enter first name");
//            etFirstName.requestFocus();
//            return;
//        }

//        if (etLastName.getText().toString().trim().isEmpty()) {
//            etLastName.setError("Please enter last name");
//            etLastName.requestFocus();
//            return;
//        }

//        if (etMobileNumber.getText().toString().trim().isEmpty()) {
//            etMobileNumber.setError("Please enter mobile number");
//            etMobileNumber.requestFocus();
//            return;
//        }
//
//        if (etDateOfBirth.getText().toString().trim().isEmpty()) {
//            etDateOfBirth.setError("Please select date of birth");
//            etDateOfBirth.requestFocus();
//            return;
//        }

        callGetPatientDetailsAPI();
    }

    private void callGetPatientDetailsAPI() {
        // Show default progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Authenticating patient...");
        progressDialog.setCancelable(false); // Prevents dismissal on touch outside
        progressDialog.show();

        ApiService apiService = RetrofitClient.getApiService();
//        PatientRequest request = new PatientRequest(
//                "Shailesh", "", "Patel", "1990-08-18", "8511131818", PrefHelper.getString("InstitutionGUID", "").trim()
//        );

        PatientRequest request = new PatientRequest(
                etFirstName.getText().toString().trim(),
                etMiddleName.getText().toString().trim(),
                etLastName.getText().toString().trim(),
//                DateConverter.convertDateFormat(etDateOfBirth.getText().toString().trim()),
                DateConverter.getDobFromAge(Integer.parseInt(etDateOfBirth.getText().toString().trim())),
                etMobileNumber.getText().toString().trim(),
                PrefHelper.getString("InstitutionGUID", "").trim()
        );

        Log.d(TAG, "Request Body: " + new Gson().toJson(request));

        Call<PatientResponse> call = apiService.getPatientList(request);
        call.enqueue(new Callback<PatientResponse>() {

            @Override
            public void onResponse(Call<PatientResponse> call, Response<PatientResponse> response) {

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                try {

                    if (response.code() == 401) {
                        handleTokenExpired();
                        return;
                    }

                    // ✅ SUCCESS RESPONSE
                    if (response.isSuccessful() && response.body() != null) {

                        PatientResponse patientResponse = response.body();

                        Log.d(TAG, "Raw Response String: " +
                                patientResponse.getResponse());

                        // ✅ Token Expired Check
                        if (!patientResponse.isSuccess()
                                && patientResponse.getResponse() != null
                                && patientResponse.getResponse()
                                .toLowerCase()
                                .contains("token")) {

                            handleTokenExpired();
                            return;
                        }

                        // ✅ Empty List
                        if (patientResponse.getResponse().equals("[]")) {

                            Intent intent = new Intent(
                                    PatientValidateActivity.this,
                                    PatientRegisterActivity.class);

                            intent.putExtra("FirstName",
                                    etFirstName.getText().toString().trim());

                            intent.putExtra("MiddleName",
                                    etMiddleName.getText().toString().trim());

                            intent.putExtra("LastName",
                                    etLastName.getText().toString().trim());

                            intent.putExtra("MobileNo",
                                    etMobileNumber.getText().toString().trim());

                            intent.putExtra("DOB",
                                    etDateOfBirth.getText().toString().trim());

                            startActivity(intent);

                            return;
                        }

                        // ✅ Parse JSONArray
                        JSONArray jsonArray =
                                new JSONArray(patientResponse.getResponse());

                        Gson gson = new Gson();

                        Type listType =
                                new TypeToken<List<Patient>>() {}.getType();

                        List<Patient> patients =
                                gson.fromJson(jsonArray.toString(), listType);

                        Log.d(TAG, "Parsed Patient List: " + patients);

                        patientList.clear();
                        patientList.addAll(patients);

                        Log.d(TAG, "Patient List Size: " + patientList.size());

                        if (!patientList.isEmpty()) {
                            Log.d(TAG, "First Patient: "
                                    + patientList.get(0).toString());
                        }

                        adapter.notifyDataSetChanged();

                    } else {

                        // ✅ ERROR RESPONSE (401 / 403 etc.)
                        String errorBody = "";

                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }

                        Log.e(TAG, "API Error Body: " + errorBody);

                        JSONObject jsonObject =
                                new JSONObject(errorBody);

                        String message =
                                jsonObject.optString("Response");

                        boolean isSuccess =
                                jsonObject.optBoolean("IsSuccess");

                        // ✅ Token Expired
                        if (!isSuccess
                                && message != null
                                && message.toLowerCase().contains("token")) {

                            handleTokenExpired();
                            return;
                        }

                        Toast.makeText(
                                PatientValidateActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                } catch (Exception e) {

                    Log.e(TAG, "Exception: " + e.getMessage());

                    Toast.makeText(
                            PatientValidateActivity.this,
                            "Something went wrong",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<PatientResponse> call, Throwable t) {

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                Log.e(TAG, "Error: " + t.getMessage());

                Toast.makeText(
                        PatientValidateActivity.this,
                        "Network error",
                        Toast.LENGTH_SHORT
                ).show();
            }

            // ✅ Common Token Expire Method
            private void handleTokenExpired() {

                Toast.makeText(
                        PatientValidateActivity.this,
                        "Session expired. Please login again.",
                        Toast.LENGTH_LONG
                ).show();

                // Clear Session
                PrefHelper.setBoolean("isUserLoggedIn", false);
                PrefHelper.setString("AppToken", "");
                PathUtil.TOKEN = "";

                // Redirect Login
                Intent intent = new Intent(
                        PatientValidateActivity.this,
                        SignInActivity.class);

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);

                finish();
            }
        });
    }
}
