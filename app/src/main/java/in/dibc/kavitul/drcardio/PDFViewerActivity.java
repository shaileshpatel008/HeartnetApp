package in.dibc.kavitul.drcardio;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

import in.dibc.kavitul.bluetoothmodule.AppBluetoothHelper;
import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.mLog;

public class PDFViewerActivity extends AppCompatActivity {

    ProgressBar mProgressBar;
    WebView webView;
    int i=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);
        initApp();
    }

    //Initializes all the variables of appbt and then goes on to the next activity i.e. main activity or menu activity
    private void initApp() {

        String filePath = "";
        if(getIntent() != null) {
            filePath = getIntent().getStringExtra("PDFPath");
            Log.v("File Path :: ", ""+filePath);
            Toast.makeText(PDFViewerActivity.this, "PSD Path :: "+filePath, Toast.LENGTH_SHORT).show();
            //displayFromFiles(new File("file: "+filePath));
        }

        File file = new File(filePath);

        webView=findViewById(R.id.webView);
        mProgressBar=findViewById(R.id.mProgressBar);
        mProgressBar.setMax(100);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("file:///" + file.getAbsolutePath());

        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                mProgressBar.setProgress(newProgress);
            }
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }
            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                super.onReceivedIcon(view, icon);
            }
        });
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String request)
            {
                if(webView.getHitTestResult().getExtra().endsWith(".pdf"))
                {

                    return true;
                }
                else
                {
                    webView.loadUrl(request);
                    return true;
                }
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                mProgressBar.setVisibility(View.GONE);
                super.onPageFinished(view, url);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}