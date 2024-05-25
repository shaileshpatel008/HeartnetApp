package in.dibc.kavitul.drcardio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.gauravk.bubblenavigation.BubbleNavigationLinearView;
import com.gauravk.bubblenavigation.listener.BubbleNavigationChangeListener;
import com.saadahmedsoft.popupdialog.PopupDialog;
import com.saadahmedsoft.popupdialog.Styles;
import com.saadahmedsoft.popupdialog.listener.OnDialogButtonClickListener;
import com.williamww.silkysignature.views.SignaturePad;

import org.honorato.multistatetogglebutton.MultiStateToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import in.dibc.kavitul.drcardio.SupportClass.LocaleHelper;
import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.mLog;
import in.dibc.kavitul.drcardio.SupportClass.settings;
import in.dibc.kavitul.drcardio.utils.PrefHelper;

public class MyAccountsActivity extends AppCompatActivity implements View.OnClickListener {
    String TAG = "My_Accounts_Activity";

    ImageView header_left_btn;

    BubbleNavigationLinearView bubbleNavigation;

    LinearLayout llBack, llLoadData, llProfile, llSettings, llHelp, llShareApp, llFeedback, llReview, llSignout;

    TextView tvVersion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_accounts);
        initUIElements();
        mLog.d(TAG, "onCreate","noDATA", "reached");
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();

        bubbleNavigation.setCurrentActiveItem(2);
    }

    public void init() {

    }

    private void initUIElements(){
        llBack = findViewById(R.id.llBack);
        header_left_btn = findViewById(R.id.header_left_btn);
        llBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        header_left_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        bubbleNavigation = findViewById(R.id.equal_navigation_bar);
        bubbleNavigation.setNavigationChangeListener(new BubbleNavigationChangeListener() {
            @Override
            public void onNavigationChanged(View view, int position) {
                //navigation changed, do something
                if(position == 0) {
                    finish();
                } else if(position == 1) {
                    Intent intent = new Intent(MyAccountsActivity.this, ReportActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

        llLoadData = findViewById(R.id.llLoadData);
        llLoadData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyAccountsActivity.this, LoadDataActivity.class);
                startActivity(intent);
            }
        });

        llProfile = findViewById(R.id.llProfile);
        llProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyAccountsActivity.this, SettingsActivity.class);
                intent.putExtra("From", "Profile");
                startActivity(intent);
            }
        });

        llSettings = findViewById(R.id.llSettings);
        llSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyAccountsActivity.this, SettingsActivity.class);
                intent.putExtra("From", "Settings");
                startActivity(intent);
            }
        });

        llHelp = findViewById(R.id.llHelp);
        llHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helpDialog();
            }
        });

        llShareApp = findViewById(R.id.llShareApp);
        llShareApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Dr. Cardio app");
                    String shareMessage= "Revolutionize your patient's heart health with Dr. Cardio! For cardiac care and proactive wellness management Download the app now. #DrCardio #HeartHealth\n\n";
                    shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID +"\n\n";
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                    startActivity(Intent.createChooser(shareIntent, "choose one"));
                } catch(Exception e) {
                    //e.toString();
                }
            }
        });

        llFeedback = findViewById(R.id.llFeedback);
        llFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("ajinkya@kavitul.com"));
                intent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
                intent.putExtra(Intent.EXTRA_CC, new String[]{""});
                intent.putExtra(Intent.EXTRA_BCC, new String[]{""});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Dr. Cardio app feedback");
                intent.putExtra(Intent.EXTRA_TEXT, "");

                startActivity(intent);*/

                Intent email= new Intent(Intent.ACTION_SENDTO);
                email.setData(Uri.parse("mailto:support@drcardio.in"));
                email.putExtra(Intent.EXTRA_SUBJECT, "Dr. Cardio app feedback");
                email.putExtra(Intent.EXTRA_TEXT, "");
                startActivity(email);
            }
        });

        llReview = findViewById(R.id.llReview);
        llReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }*/
                openAppRating(MyAccountsActivity.this);
            }
        });

        llSignout = findViewById(R.id.llSignout);
        llSignout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MyAccountsActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Sign out?")
                        .setMessage("Are you sure you want to sign out from the application?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PrefHelper.setBoolean("isUserLoggedIn", false);
                                PrefHelper.setString("UserName", "");
                                PrefHelper.setString("UserEmail", "");
                                PrefHelper.setString("Token", "");
                                PrefHelper.deleteAllPreferences();
                                Intent intent = new Intent(MyAccountsActivity.this, SignInActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();

            }
        });

        tvVersion = findViewById(R.id.tvVersion);
        String versionName = BuildConfig.VERSION_NAME;
        tvVersion.setText("Version "+versionName);

    }

    @Override
    public void onClick(View v) {

    }

    public void openAppRating(Context context) {
        // you can also use BuildConfig.APPLICATION_ID
        String appId = context.getPackageName();
        Intent rateIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + appId));
        boolean marketFound = false;

        // find all applications able to handle our rateIntent
        final List<ResolveInfo> otherApps = context.getPackageManager()
                .queryIntentActivities(rateIntent, 0);
        for (ResolveInfo otherApp: otherApps) {
            // look for Google Play application
            if (otherApp.activityInfo.applicationInfo.packageName
                    .equals("com.android.vending")) {

                ActivityInfo otherAppActivity = otherApp.activityInfo;
                ComponentName componentName = new ComponentName(
                        otherAppActivity.applicationInfo.packageName,
                        otherAppActivity.name
                );
                // make sure it does NOT open in the stack of your activity
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // task reparenting if needed
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                // if the Google Play was already open in a search result
                //  this make sure it still go to the app page you requested
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                // this make sure only the Google Play app is allowed to
                // intercept the intent
                rateIntent.setComponent(componentName);
                context.startActivity(rateIntent);
                marketFound = true;
                break;

            }
        }

        // if GP not present on device, open web browser
        if (!marketFound) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id="+appId));
            context.startActivity(webIntent);
        }
    }

    private void helpDialog() {
        PopupDialog.getInstance(this)
                .setStyle(Styles.IOS)
                .setHeading("Help")
                .setDescription("Do you want to contact support via phone call or chat using WhatsApp?")
                .setNegativeButtonText("Call")
                .setPositiveButtonText("WhatsApp")
                .setPositiveButtonTextColor(R.color.colorPrimary)
                .setNegativeButtonTextColor(R.color.colorPrimary)
                //.setPositiveButtonBackground(R.color.colorPrimary)
                //.setPopupDialogIcon(R.drawable.btn_icon_help)
                //.setPopupDialogIconTint(R.color.colorPrimary)
                .setCancelable(true)
                .showDialog(new OnDialogButtonClickListener() {
                    @Override
                    public void onPositiveClicked(Dialog dialog) {
                        super.onPositiveClicked(dialog);

                        PackageManager packageManager = getPackageManager();
                        Intent i = new Intent(Intent.ACTION_VIEW);

                        try {
                            String phone = "+919638389333";
                            String url = "https://api.whatsapp.com/send?phone="+ phone +"&text=" + URLEncoder.encode("Hi!", "UTF-8");
                            i.setPackage("com.whatsapp");
                            i.setData(Uri.parse(url));
                            //if (i.resolveActivity(packageManager) != null) {
                            startActivity(i);
                            //}
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onNegativeClicked(Dialog dialog) {
                        super.onNegativeClicked(dialog);
                        Intent intent = new Intent(Intent.ACTION_DIAL);

                        intent.setData(Uri.parse("tel:+919638389333"));
                        startActivity(intent);
                    }
                });
    }
}
