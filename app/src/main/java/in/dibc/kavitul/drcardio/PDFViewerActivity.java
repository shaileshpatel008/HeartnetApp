package in.dibc.kavitul.drcardio;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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

import java.io.File;
import java.util.Objects;

import in.dibc.kavitul.bluetoothmodule.AppBluetoothHelper;
import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.mLog;

public class PDFViewerActivity extends AppCompatActivity {

    PDFView pdfView;

    ImageView ivBack;

    TextView tvTitle;

    ImageView ivShare;

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
        String filePath = "", fileName = "";
        if(getIntent() != null) {
            filePath = getIntent().getStringExtra("PDFPath");
            fileName = getIntent().getStringExtra("FileName");
            Log.v("File Path :: ", ""+filePath);
//            Toast.makeText(PDFViewerActivity.this, "PSD Path :: "+filePath, Toast.LENGTH_SHORT).show();
            //displayFromFiles(new File("file: "+filePath));
        }

        File file = new File(filePath);
        pdfView.fromFile(file).load();

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
                Uri uri = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
                        BuildConfig.APPLICATION_ID + ".provider", file);

                Intent share = new Intent();
                share.setAction(Intent.ACTION_SEND);
                share.setType("application/pdf");
                share.putExtra(Intent.EXTRA_STREAM, uri);
//                share.setPackage("com.whatsapp");
                share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivity(Intent.createChooser(share, "Share PDF"));

//                Intent share = new Intent(Intent.ACTION_SEND);
//                share.setType("text/plain");
//                share.putExtra(Intent.EXTRA_TEXT, "I'm being sent!!");
//                startActivity(Intent.createChooser(share, "Share Text"));
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}