package in.dibc.kavitul.drcardio;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.github.barteksc.pdfviewer.PDFView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import in.dibc.kavitul.bluetoothmodule.AppBluetoothHelper;
import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.mLog;

public class PDFViewerActivity extends AppCompatActivity {

    PDFView pdfView;

    ImageView ivBack;

    TextView tvTitle;

    ImageView ivShare;

    Context context;

    String filePath = "", fileName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);
        initApp();
    }

    //Initializes all the variables of appbt and then goes on to the next activity i.e. main activity or menu activity
    private void initApp() {

        pdfView = findViewById(R.id.pdfView);
        if(getIntent() != null) {
            filePath = getIntent().getStringExtra("PDFPath");
            fileName = getIntent().getStringExtra("FileName");
            Log.v("File Path :: ", ""+filePath);
//            Toast.makeText(PDFViewerActivity.this, "PSD Path :: "+filePath, Toast.LENGTH_SHORT).show();
            //displayFromFiles(new File("file: "+filePath));
        }

        //pdfView.fromUri(Uri.parse(filePath.replaceAll(" ", "%20"))).load();
//        pdfView.fromUri(Uri.parse("https://www.orimi.com/pdf-test.pdf"));

        context = this;
        new RetrievePDFfromUrl().execute(filePath.replaceAll(" ", "%20"));

//        File file = new File(filePath);
//        pdfView.fromFile(file).load();

        tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(fileName);

        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        ivShare = findViewById(R.id.ivShare);
        ivShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Uri uri = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
//                        BuildConfig.APPLICATION_ID + ".provider", file);
//
//                Intent share = new Intent();
//                share.setAction(Intent.ACTION_SEND);
//                share.setType("application/pdf");
//                share.putExtra(Intent.EXTRA_STREAM, uri);
//                share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//                startActivity(Intent.createChooser(share, "Share PDF"));



                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, filePath.replaceAll(" ", "%20"));
                startActivity(Intent.createChooser(share, "Share Text"));
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    // create an async task class for loading pdf file from URL.
    class RetrievePDFfromUrl extends AsyncTask<String, Void, InputStream> {

        ProgressDialog pd;

        Context ctx = context ;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(ctx);
            pd.setMessage("Loading PDF...");
            pd.setIndeterminate(false);
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected InputStream doInBackground(String... strings) {
            // we are using inputstream
            // for getting out PDF.
            InputStream inputStream = null;
            try {
                URL url = new URL(strings[0]);
                // below is the step where we are
                // creating our connection.
                HttpURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                if (urlConnection.getResponseCode() == 200) {
                    // response is success.
                    // we are getting input stream from url
                    // and storing it in our variable.
                    inputStream = new BufferedInputStream(urlConnection.getInputStream());
                }

            } catch (IOException e) {
                // this is the method
                // to handle errors.
                e.printStackTrace();
                return null;
            }
            return inputStream;
        }

        @Override
        protected void onPostExecute(InputStream inputStream) {
            // after the execution of our async
            // task we are loading our pdf in our pdf view.
            pdfView.fromStream(inputStream).load();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (pd != null)
                    {
                        pd.dismiss();
                    }
                }
            }, 4000);
        }
    }
}