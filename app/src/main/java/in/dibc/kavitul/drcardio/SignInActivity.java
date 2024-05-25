package in.dibc.kavitul.drcardio;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import in.dibc.kavitul.bluetoothmodule.AppBluetoothHelper;
import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.mLog;
import in.dibc.kavitul.drcardio.SupportClass.settings;
import in.dibc.kavitul.drcardio.utils.PrefHelper;

public class SignInActivity extends AppCompatActivity {
    String TAG = "TeleECG_SignInScreen";

    EditText edtUserName, edtPassword;
    Button btnLogin;

    CheckBox chkPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        initialiseApp();
    }

    private void initialiseApp() {

        edtUserName = findViewById(R.id.edtUserName);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);

//        edtPassword.setTransformationMethod(new PasswordTransformationMethod());

        chkPassword = findViewById(R.id.chkPassword);
        edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        chkPassword.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
                if (chkPassword.isChecked()) {
                    //edtPassword.setTransformationMethod(null); // Hide password when box not checked
                    edtPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    chkPassword.setChecked(true);
                    edtPassword.setSelection(edtPassword.getText().length());
                } else {
//                    edtPassword.setTransformationMethod(new PasswordTransformationMethod()); // Show password when box checked
                    edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    chkPassword.setChecked(false);
                    edtPassword.setSelection(edtPassword.getText().length());
                }
            }
        } );

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(edtUserName.getText().length() == 0) {
                    Toast.makeText(SignInActivity.this, "Please enter email", Toast.LENGTH_LONG).show();
                } else if(edtUserName.getText().length() == 0) {
                    Toast.makeText(SignInActivity.this, "Please enter password", Toast.LENGTH_LONG).show();
                } else {
                    callSignInApi();
                }
            }
        });
    }

    private void callSignInApi() {

        ProgressDialog pd = new ProgressDialog(SignInActivity.this);
        pd.setMessage("Authentication...");
        pd.setCancelable(false);
        pd.show();

        // url to post our data
        String url = "https://drcadio.soluspottechnolabs.com/api/login";
//        loadingPB.setVisibility(View.VISIBLE);

        // creating a new variable for our request queue
        RequestQueue queue = Volley.newRequestQueue(SignInActivity.this);

        // on below line we are calling a string
        // request method to post the data to our API
        // in this we are calling a post method.
        StringRequest request = new StringRequest(Request.Method.POST, url, new com.android.volley.Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // inside on response method we are
                // hiding our progress bar
                // and setting data to edit text as empty
                //loadingPB.setVisibility(View.GONE);

                // on below line we are displaying a success toast message.

                try {
                    // on below line we are parsing the response
                    // to json object to extract data from it.
                    JSONObject respObj = new JSONObject(response);
                    Log.v("Response :: ", respObj.toString());

                    //String name = respObj.getString("name");
                    //String job = respObj.getString("job");

                    // on below line we are setting this string s to our text view.
                    //responseTV.setText("Name : " + name + "\n" + "Job : " + job);

                    if(respObj.getString("status").equalsIgnoreCase("success")) {
                        Log.v("User Name :: ", respObj.getJSONObject("success_data").getString("name"));
                        pd.dismiss();
                        edtUserName.setText("");
                        edtPassword.setText("");
                        PrefHelper.setBoolean("isUserLoggedIn", true);
                        PrefHelper.setString("UserName", respObj.getJSONObject("success_data").getString("name"));
                        PrefHelper.setString("UserEmail", respObj.getJSONObject("success_data").getString("email"));
                        PrefHelper.setString("Token", respObj.getJSONObject("success_data").getString("remember_token"));
                        settings.doctor_name = respObj.getJSONObject("success_data").getString("doctor_name");
                        settings.doctor_address = respObj.getJSONObject("success_data").getString("address");
                        settings.doctor_website = respObj.getJSONObject("success_data").getString("hospital_name");
                        settings.saveSettings();
                        Toast.makeText(SignInActivity.this, "User login successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        pd.dismiss();
                        Toast.makeText(SignInActivity.this, respObj.getString("error_data").toString(), Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    pd.dismiss();
                    throw new RuntimeException(e);
                }
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // method to handle errors.
                Toast.makeText(SignInActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // below line we are creating a map for
                // storing our values in key and value pair.
                Map<String, String> params = new HashMap<String, String>();

                JSONObject jDeviceInfo = new JSONObject();
                try {
//                        jDeviceInfo.put("OS", System.getProperty("os.version"));
                    jDeviceInfo.put("API Level", android.os.Build.VERSION.SDK_INT);
                    jDeviceInfo.put("Device", android.os.Build.DEVICE);
                    jDeviceInfo.put("Model", android.os.Build.MODEL );
                    jDeviceInfo.put("FullDeviceInfo", Build.BRAND+" "+android.os.Build.MODEL);
                    Log.v("Device Informations :: ", jDeviceInfo.toString());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // on below line we are passing our key
                // and value pair to our parameters.
                params.put("email", edtUserName.getText().toString());
                params.put("password", edtPassword.getText().toString());
                params.put("device_info", jDeviceInfo.toString());

                Log.v("Request :: ", params.toString());

                // at last we are
                // returning our params.
                return params;
            }
        };
        // below line is to make
        // a json object request.
        queue.add(request);
    }

}
