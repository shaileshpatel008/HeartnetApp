package in.dibc.kavitul.drcardio;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;

import in.dibc.kavitul.drcardio.apis.ApiService;
import in.dibc.kavitul.drcardio.apis.RetrofitClient;
import in.dibc.kavitul.drcardio.apis.request.PatientHistoryRequest;
import in.dibc.kavitul.drcardio.apis.request.PatientRegisterRequest;
import in.dibc.kavitul.drcardio.apis.response.PatientRegisterResponse;
import in.dibc.kavitul.drcardio.utils.PathUtil;
import in.dibc.kavitul.drcardio.utils.PrefHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PatientHistoryActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;

    private EditText etHistoryNotes, etDoctorName;
    ImageView ivBack;
//    private Switch switchInsured;
    private Button btnAdd;

    String patientID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_history);

        // Initialize all fields
        etHistoryNotes = findViewById(R.id.etHistoryNotes);
        etDoctorName = findViewById(R.id.etDoctorName);

        btnAdd = findViewById(R.id.btnAdd);
        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        if(getIntent() != null) {
            patientID = getIntent().getExtras().getString("PatientID");
            Log.v("","PatientID :: "+getIntent().getExtras().getString("PatientID"));
        }

        // Add Button Click
        btnAdd.setOnClickListener(v -> validateAndRegister());
    }

    // Validation for all fields
    private void validateAndRegister() {
//        if (etPatientID.getText().toString().trim().isEmpty()) {
//            etPatientID.setError("Please enter Customer ID");
//            etPatientID.requestFocus();
//            return;
//        }
        if (etDoctorName.getText().toString().trim().isEmpty()) {
            etDoctorName.setError("Please enter Doctor Name");
            etDoctorName.requestFocus();
            return;
        }

        registerPatientAPI();
    }

    private void registerPatientAPI() {

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Add patient history...");
        progressDialog.setCancelable(false); // Prevents dismissal on touch outside
        progressDialog.show();

        ApiService apiService = RetrofitClient.getApiService();

        PatientHistoryRequest patient = new PatientHistoryRequest(
                patientID,
                etHistoryNotes.getText().toString().trim(),
                etDoctorName.getText().toString().trim()
        );

        Log.v("", "Patient History :: "+patient.toString().trim());
        Log.v("", "Token :: "+PathUtil.TOKEN);

        // Make API Call
        Call<PatientRegisterResponse> call =
                apiService.savePatientHistory(PathUtil.TOKEN, patient);

        call.enqueue(new Callback<PatientRegisterResponse>() {

            @Override
            public void onResponse(Call<PatientRegisterResponse> call,
                                   Response<PatientRegisterResponse> response) {

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                if (response.code() == 401) {
                    handleTokenExpired();
                    return;
                }

                try {

                    // ✅ SUCCESS RESPONSE
                    if (response.isSuccessful() && response.body() != null) {

                        PatientRegisterResponse registerResponse =
                                response.body();

                        Log.d("API_RESPONSE",
                                "Success: " + registerResponse.getResponse());

                        // ✅ Token Expired Check
                        if (!registerResponse.isSuccess()
                                && registerResponse.getResponse() != null
                                && registerResponse.getResponse()
                                .toLowerCase()
                                .contains("token")) {

                            handleTokenExpired();
                            return;
                        }

                        // ✅ Success
                        Toast.makeText(
                                PatientHistoryActivity.this,
                                "Patient Registered Successfully",
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent =
                                new Intent(PatientHistoryActivity.this,
                                        NewEcgActivity.class);

                        intent.putExtra("PatientID", patientID);

                        startActivity(intent);

                        finish();

                    } else {

                        // ✅ ERROR RESPONSE (401 / 403 etc.)
                        String errorBody = "";

                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }

                        Log.e("API_ERROR", errorBody);

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
                                PatientHistoryActivity.this,
                                message.isEmpty()
                                        ? "Registration Failed"
                                        : message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                } catch (Exception e) {

                    Log.e("API_EXCEPTION", e.getMessage());

                    Toast.makeText(
                            PatientHistoryActivity.this,
                            "Something went wrong",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<PatientRegisterResponse> call,
                                  Throwable t) {

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                Log.e("API_FAILURE", t.getMessage());

                Toast.makeText(
                        PatientHistoryActivity.this,
                        "Network error",
                        Toast.LENGTH_SHORT
                ).show();
            }

            // ✅ Common Token Expired Method
            private void handleTokenExpired() {

                Toast.makeText(
                        PatientHistoryActivity.this,
                        "Session expired. Please login again.",
                        Toast.LENGTH_LONG
                ).show();

                // Clear Session
                PrefHelper.setBoolean("isUserLoggedIn", false);
                PrefHelper.setString("AppToken", "");
                PathUtil.TOKEN = "";

                // Redirect Login
                Intent intent =
                        new Intent(PatientHistoryActivity.this,
                                SignInActivity.class);

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);

                finish();
            }
        });
    }
}
