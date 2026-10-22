package in.dibc.kavitul.drcardio;

import static android.os.Build.VERSION.SDK_INT;

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
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

//import com.github.barteksc.pdfviewer.PDFView;
//import com.github.barteksc.pdfviewer.listener.OnRenderListener;
//import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
//import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.rajat.pdfviewer.PdfRendererView;
import com.rajat.pdfviewer.PdfViewerActivity;
import com.ymg.pdf.viewer.PDFView;
import com.ymg.pdf.viewer.listener.OnErrorListener;
import com.ymg.pdf.viewer.listener.OnLoadCompleteListener;
import com.ymg.pdf.viewer.scroll.DefaultScrollHandle;
import com.ymg.pdf.viewer.util.FitPolicy;

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
import in.dibc.kavitul.drcardio.utils.PathUtil;

public class PDFViewerActivity extends AppCompatActivity {

    private static final String TAG = "PDFViewerActivity";

    ProgressDialog pDialog;

    ImageView ivBack;

    TextView tvTitle;

    ImageView ivShare;

    Context context;

    String filePath = "", fileName = "", localFilePath = "";

    PDFView pdfView8;

    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);
        initialiseApps();
    }

    //Initializes all the variables of appbt and then goes on to the next activity i.e. main activity or menu activity
    private void initialiseApps() {

        pdfView8 = findViewById(R.id.pdfView8);

        if(getIntent() != null) {
            filePath = getIntent().getStringExtra("PDFPath");
            fileName = getIntent().getStringExtra("FileName");
            localFilePath = getIntent().getStringExtra("LocalFilePath");
            Log.v("File Path :: ", "File Path :: "+filePath);
        }

        context = this;
        if(filePath.contains("http")) {
            new RetrievePDFfromUrl().execute(filePath.replaceAll(" ", "%20"));
        } else {
            pd = new ProgressDialog(PDFViewerActivity.this);
            pd.setMessage("Loading PDF...");
            pd.setIndeterminate(false);
            pd.setCancelable(false);
            pd.show();
            pdfView8.fromFile(new File(filePath))
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .onLoad(new OnLoadCompleteListener() {
                        @Override
                        public void loadComplete(int nbPages) {
                            // Handle loading completion
                            pd.dismiss();
                        }
                    })
                    .onError(new OnErrorListener() {
                        @Override
                        public void onError(Throwable t) {
                            Log.v("ERROR :: ", t.getMessage().toString());
                        }
                    })
                    .load();
        }


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

        if(filePath.contains("http")) {
            ivShare.setVisibility(View.VISIBLE);
        } else {
            ivShare.setVisibility(View.GONE);
        }
        ivShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(PathUtil.INTERNET_STATUS) {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, filePath.replaceAll(" ", "%20"));
                    startActivity(Intent.createChooser(share, "Share Text"));
                } else {
                    String filePath = localFilePath;
                    Log.v("Share File Path :::: ", filePath);
                    File file = new File(filePath);
                    //File file = new File(mFile.getDirFile(this,mFile.DIR_REPORT),file_names.get(index) + ".pdf");
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    sharingIntent.setType("application/pdf");
                    Uri uri = getUriFromFile(file, PDFViewerActivity.this);
                    if (uri != null) {
                        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        startActivity(Intent.createChooser(sharingIntent, "share file with"));
                    }
                }
            }
        });
    }

    private Uri getUriFromFile(File file, Context context) {
        Uri uri = null;
        if (SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//            Uri.fromFile(file)
            uri = Uri.parse("file://" + file.getAbsolutePath());
        } else {
            try {
                uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            } catch (Exception e) {
                if (!e.getMessage().equals("") || e.getMessage().contains("ProviderInfo.loadXmlMetaData") == true) {
                    Log.e(TAG, "getUriFromFile: FileProvider is not set or doesn't have needed permissions");
                } else {
                    Log.d(TAG, "getUriFromFile: ", e);
                }
            }
        }
        return uri;
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
            pdfView8.fromStream(inputStream)
                    .fitEachPage(false)
                    .enableSwipe(true)
                    .enableDoubletap(true)
                    .pageFling(true)
                    .pageSnap(true)
                    .pageFitPolicy(FitPolicy.BOTH)
                    .nightMode(false)
                    .spacing(20)
                    .scrollHandle(new DefaultScrollHandle(PDFViewerActivity.this))
                    .load();
            pdfView8.useBestQuality(true);

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