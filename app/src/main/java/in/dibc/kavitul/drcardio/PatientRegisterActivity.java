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
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.Objects;

import in.dibc.kavitul.drcardio.apis.ApiService;
import in.dibc.kavitul.drcardio.apis.RetrofitClient;
import in.dibc.kavitul.drcardio.apis.request.PatientRegisterRequest;
import in.dibc.kavitul.drcardio.apis.response.PatientRegisterResponse;

import in.dibc.kavitul.drcardio.utils.DateConverter;
import in.dibc.kavitul.drcardio.utils.PathUtil;
import in.dibc.kavitul.drcardio.utils.PrefHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PatientRegisterActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;

    private EditText etPatientID, etFirstName, etMiddleName, etLastName, etDOB, etMobile1, etMobile2,
            etEmail, etAddress1, etAddress2, etCity, etPincode, etIDNumber, etIDType;
    private RadioGroup genderGroup;
    private RadioButton rbMale, rbFemale;
    ImageView ivBack;
//    private Switch switchInsured;
    private Button btnRegister;
    private String selectedGender = "";
//    private boolean isInsured = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_register);

        // Initialize all fields
        etPatientID = findViewById(R.id.etPatientID);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        etDOB = findViewById(R.id.etDOB);
        etMobile1 = findViewById(R.id.etMobile1);
        etMobile2 = findViewById(R.id.etMobile2);
        etEmail = findViewById(R.id.etEmail);
        etAddress1 = findViewById(R.id.etAddress1);
        etAddress2 = findViewById(R.id.etAddress2);
        etCity = findViewById(R.id.etCity);
        etPincode = findViewById(R.id.etPincode);
        etIDNumber = findViewById(R.id.etIDNumber);
        etIDType = findViewById(R.id.etIDType);
        genderGroup = findViewById(R.id.genderGroup);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
//        switchInsured = findViewById(R.id.switchInsured);
        btnRegister = findViewById(R.id.btnRegister);
        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        if(getIntent() != null) {
            etFirstName.setText(getIntent().getExtras().getString("FirstName"));
            etMiddleName.setText(getIntent().getExtras().getString("MiddleName"));
            etLastName.setText(getIntent().getExtras().getString("LastName"));
            etMobile1.setText(getIntent().getExtras().getString("MobileNo"));
            etDOB.setText(Objects.requireNonNull(getIntent().getExtras().getString("DOB")).replaceAll("-", "/"));
            Log.v("","Register DOB :: "+getIntent().getExtras().getString("DOB"));
        }

        // Gender Selection
        genderGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbMale) {
                selectedGender = "Male";
            } else if (checkedId == R.id.rbFemale) {
                selectedGender = "Female";
            }
        });

        // Insurance Toggle
//        switchInsured.setOnCheckedChangeListener((buttonView, isChecked) -> isInsured = isChecked);

        // Date Picker for DOB
//        etDOB.setOnClickListener(v -> showDatePicker());

        // Register Button Click
        btnRegister.setOnClickListener(v -> validateAndRegister());
    }

    // Date Picker for DOB
    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(PatientRegisterActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    etDOB.setText(selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear);
                },
                year, month, day);

        // Restrict future dates
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    // Validation for all fields
    private void validateAndRegister() {
//        if (etPatientID.getText().toString().trim().isEmpty()) {
//            etPatientID.setError("Please enter Customer ID");
//            etPatientID.requestFocus();
//            return;
//        }
        if (etFirstName.getText().toString().trim().isEmpty()) {
            etFirstName.setError("Please enter First Name");
            etFirstName.requestFocus();
            return;
        }
//        if (etLastName.getText().toString().trim().isEmpty()) {
//            etLastName.setError("Please enter Last Name");
//            etLastName.requestFocus();
//            return;
//        }
        if (etDOB.getText().toString().trim().isEmpty()) {
            etDOB.setError("Please select Date of Birth");
            etDOB.requestFocus();
            return;
        }
//        if (selectedGender.isEmpty()) {
//            Toast.makeText(this, "Please select Gender", Toast.LENGTH_SHORT).show();
//            return;
//        }
        if (etMobile1.getText().toString().trim().isEmpty()) {
            etMobile1.setError("Please enter Mobile Number");
            etMobile1.requestFocus();
            return;
        }
        if (etMobile1.getText().toString().trim().length() < 10) {
            etMobile1.setError("Enter a valid 10-digit Mobile Number");
            etMobile1.requestFocus();
            return;
        }
//        if (!etEmail.getText().toString().trim().isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(etEmail.getText().toString().trim()).matches()) {
//            etEmail.setError("Enter a valid Email ID");
//            etEmail.requestFocus();
//            return;
//        }
//        if (etAddress1.getText().toString().trim().isEmpty()) {
//            etAddress1.setError("Please enter Address Line 1");
//            etAddress1.requestFocus();
//            return;
//        }
//        if (etCity.getText().toString().trim().isEmpty()) {
//            etCity.setError("Please enter City");
//            etCity.requestFocus();
//            return;
//        }
//        if (etPincode.getText().toString().trim().isEmpty()) {
//            etPincode.setError("Please enter Pincode");
//            etPincode.requestFocus();
//            return;
//        }
//        if (etPincode.getText().toString().trim().length() < 6) {
//            etPincode.setError("Enter a valid 6-digit Pincode");
//            etPincode.requestFocus();
//            return;
//        }
//        if (etIDNumber.getText().toString().trim().isEmpty()) {
//            etIDNumber.setError("Please enter ID Number");
//            etIDNumber.requestFocus();
//            return;
//        }
//        if (etIDType.getText().toString().trim().isEmpty()) {
//            etIDType.setError("Please enter ID Type");
//            etIDType.requestFocus();
//            return;
//        }

        registerPatientAPI();
    }

    private void registerPatientAPI() {

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering patient...");
        progressDialog.setCancelable(false); // Prevents dismissal on touch outside
        progressDialog.show();

        ApiService apiService = RetrofitClient.getApiService();
        // Creating a Patient object
//        PatientRegisterRequest patient = new PatientRegisterRequest(
//                "", "Shailesh", "R", "Patel", "1990-08-18",
//                "Male", "8511131818", "", "shaileshrpatel@mailinator.com",
//                "C 808, Shayona Residency", "Satellite", "Ahmedabad", "388080",
//                "123412341234", "Adhar Card", true, "", PrefHelper.getString("InstitutionGUID", "").trim()
//        );

//        Log.v("", "DOB :: "+DateConverter.convertDateFormat(etDOB.getText().toString().trim()));

        PatientRegisterRequest patient = new PatientRegisterRequest(
                etPatientID.getText().toString().trim(),
                etFirstName.getText().toString().trim(),
                etMiddleName.getText().toString().trim(),
                etLastName.getText().toString().trim(),
//                DateConverter.convertDateFormat(etDOB.getText().toString().trim()),
                DateConverter.getDobFromAge(Integer.parseInt(etDOB.getText().toString().trim())),
                selectedGender,
                etMobile1.getText().toString().trim(),
                etMobile2.getText().toString().trim(),
                etEmail.getText().toString().trim(),
                etAddress1.getText().toString().trim(),
                etAddress2.getText().toString().trim(),
                etCity.getText().toString().trim(),
                etPincode.getText().toString().trim(),
                etIDNumber.getText().toString().trim(),
                etIDType.getText().toString().trim(),
//                isInsured,
                "",
                PrefHelper.getString("InstitutionGUID", "").trim()
        );

        Log.v("", "Patient Details :: "+patient.toString().trim());

        // Make API Call
        Call<PatientRegisterResponse> call = apiService.registerPatient(patient);

        call.enqueue(new Callback<PatientRegisterResponse>() {

            @Override
            public void onResponse(Call<PatientRegisterResponse> call,
                                   Response<PatientRegisterResponse> response) {

                progressDialog.dismiss();

                if (response.code() == 401) {

                    Toast.makeText(
                            PatientRegisterActivity.this,
                            "Session expired. Please login again.",
                            Toast.LENGTH_LONG
                    ).show();

                    // Clear Session
                    PrefHelper.setBoolean("isUserLoggedIn", false);
                    PrefHelper.setString("AppToken", "");
                    PathUtil.TOKEN = "";

                    Intent intent = new Intent(
                            PatientRegisterActivity.this,
                            SignInActivity.class);

                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);

                    finish();

                    return;
                }

                if (response.isSuccessful() && response.body() != null) {

                    Log.d("API_RESPONSE",
                            "Success: " + response.body().getResponse());

                    Intent intent = new Intent(
                            PatientRegisterActivity.this,
                            PatientHistoryActivity.class);

                    intent.putExtra(
                            "PatientID",
                            response.body().getResponse());

                    startActivity(intent);

                } else {

                    Log.e("API_ERROR",
                            "Response Failed: " + response.errorBody());

                    Toast.makeText(
                            PatientRegisterActivity.this,
                            "Registration Failed",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<PatientRegisterResponse> call,
                                  Throwable t) {

                progressDialog.dismiss();

                Log.e("API_ERROR", "Failure: " + t.getMessage());

                Toast.makeText(
                        PatientRegisterActivity.this,
                        "API Call Failed",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}
