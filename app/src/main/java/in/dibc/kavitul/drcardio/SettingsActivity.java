package in.dibc.kavitul.drcardio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.williamww.silkysignature.views.SignaturePad;

import org.honorato.multistatetogglebutton.MultiStateToggleButton;
import org.honorato.multistatetogglebutton.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import in.dibc.kavitul.drcardio.SupportClass.LocaleHelper;
import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.mLog;
import in.dibc.kavitul.drcardio.SupportClass.settings;
import in.dibc.kavitul.drcardio.utils.PathUtil;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
    String TAG = "Settings_Activity";

    public static final int REQUEST_IMAGE = 100;
    SwitchCompat settings_test_mode, switch_csv;

    MultiStateToggleButton mstb;
    EditText settings_doctor_name, settings_doctor_address, settings_doctor_contactno, settings_doctor_website, settings_doctor_email;
    Spinner spinner_filter, spinner_gain, spinner_language, spinner_log;
    Button settings_save;
    String[] filters = null;
    String[] gains = null;
    String[] languages = null;
    String[] log_level = null;
    int position_filter, position_gain, position_language, position_log;
    private TextView alert_report, header_text;
    private ImageView alert_report_arrow, header_left_btn;

    private SignaturePad mSilkySignaturePad;
    private Button mClearButton;
    private Button mSaveButton;
    private Button mCompressButton;

    ImageView imgProfile, imgPlus;

    TextView tvSelected;

    LinearLayout llPatientInfo, llSettings;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_settings);
        initUIElements();
        mLog.d(TAG, "onCreate","noDATA", "reached");
        init();
    }


    public void init() {
        Toolbar toolbar = findViewById(R.id.settingsToolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        getSettings();
    }

    private void initUIElements(){
        settings_test_mode = findViewById(R.id.settings_test_mode);
        spinner_filter = findViewById(R.id.spinner_filter);
        spinner_gain = findViewById(R.id.spinner_gain);
        alert_report = findViewById(R.id.alert_report);
        alert_report_arrow = findViewById(R.id.alert_report_arrow);
        settings_save = findViewById(R.id.settings_save);
        settings_doctor_name = findViewById(R.id.settings_doctor_name);
        settings_doctor_address = findViewById(R.id.settings_doctor_address);
        settings_doctor_contactno = findViewById(R.id.settings_doctor_contactno);
        settings_doctor_website = findViewById(R.id.settings_doctor_website);
        settings_doctor_email = findViewById(R.id.settings_doctor_email);
        spinner_language = findViewById(R.id.spinner_language);
        spinner_log = findViewById(R.id.spinner_log);
        switch_csv = findViewById(R.id.switch_csv);
        llPatientInfo = findViewById(R.id.llPatientInfo);
        llSettings = findViewById(R.id.llSettings);

        tvSelected = findViewById(R.id.tvSelected);

        header_text = findViewById(R.id.header_text);
        header_left_btn = findViewById(R.id.header_left_btn);

        settings_save.setOnClickListener(this);
        alert_report.setOnClickListener(this);
        header_left_btn.setOnClickListener(this);
        alert_report_arrow.setOnClickListener(this);

        try {
            ((TextView) findViewById(R.id.settings_version)).setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        mstb = findViewById(R.id.mstb_multi_id);
        mstb.enableMultipleChoice(false);
        //mstb.setColorRes(R.color.colorPrimary, R.color.colorPrimaryLight);
        mstb.setPressedColors(getResources().getColor(R.color.colorWhite), getResources().getColor(R.color.colorPrimary));
        mstb.setNotPressedColors(getResources().getColor(R.color.colorPrimary), getResources().getColor(R.color.colorPrimaryLight));
        mstb.setOnValueChangedListener(new ToggleButton.OnValueChangedListener() {
            @Override
            public void onValueChanged(int position) {
                Log.d(TAG, "Position: " + position);
                if(position == 0) {
                    tvSelected.setText("You selected ECG Mode");
                } else {
                    tvSelected.setText("You selected TEST Mode");
                }
            }
        });

        if (getIntent() != null) {
            if(getIntent().getExtras().getString("From").equalsIgnoreCase("Settings")) {
                llSettings.setVisibility(View.VISIBLE);
                llPatientInfo.setVisibility(View.GONE);
                header_text.setText("Settings".toUpperCase());
            } else {
                llSettings.setVisibility(View.GONE);
                llPatientInfo.setVisibility(View.VISIBLE);
                header_text.setText("Profile".toUpperCase());
            }
        }
    }

    private void getSettings(){
        //Get saved settings

        //Set values in UI
        //settings_test_mode.setChecked(settings.test_mode.equals("true"));
        if(settings.test_mode.equals("true")) {
            tvSelected.setText("You selected TEST Mode");
            mstb.setValue(1);
        } else {
            tvSelected.setText("You selected ECG Mode");
            mstb.setValue(0);
        }
        switch_csv.setChecked(settings.csv);

        //Populate Filter Spinner and set value
        filters = appbt.res.getStringArray(R.array.spinner_filter);
        String[] actual_filters = appbt.res.getStringArray(R.array.spinner_actual_filter);
        position_filter = Arrays.asList(actual_filters).indexOf(settings.filter);
        populateSpinnerFilter();

        //Populate Gain Spinner and set value
        gains = appbt.res.getStringArray(R.array.spinner_gain);
        position_gain = Arrays.asList(gains).indexOf(settings.gain);
        populateSpinnerGain();

        settings_doctor_name.setText(settings.doctor_name);
        settings_doctor_address.setText(settings.doctor_address);
        settings_doctor_contactno.setText(settings.doctor_contactno);
        settings_doctor_website.setText(settings.doctor_website);
        settings_doctor_email.setText(settings.doctor_email);

        //Populate Language Spinner and set value
        languages = appbt.res.getStringArray(R.array.languages);
        position_language = Arrays.asList(languages).indexOf(appbt.pref.getString("settings_language", ""));
        populateSpinnerLanguage();

        //Populate Log Spinner and set value
        log_level = appbt.res.getStringArray(R.array.log_level);
        position_log = Arrays.asList(log_level).indexOf(settings.log);
        populateSpinnerLog();
        setReportString();

        mSilkySignaturePad = findViewById(R.id.signature_pad);
        mSilkySignaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {
            @Override
            public void onStartSigning() {
                //Toast.makeText(SettingsActivity.this, "OnStartSigning", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSigned() {
                mSaveButton.setEnabled(true);
                mClearButton.setEnabled(true);
                mCompressButton.setEnabled(true);
            }

            @Override
            public void onClear() {
                mSaveButton.setEnabled(false);
                mClearButton.setEnabled(false);
                mCompressButton.setEnabled(false);
            }
        });

        mClearButton = findViewById(R.id.clear_button);
        mSaveButton = findViewById(R.id.save_button);

        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSilkySignaturePad.clear();
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap signatureBitmap = mSilkySignaturePad.getSignatureBitmap();
                if (addJpgSignatureToGallery(signatureBitmap)) {
                    Toast.makeText(SettingsActivity.this, "Signature saved into the Gallery", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "Unable to store the signature", Toast.LENGTH_SHORT).show();
                }
                if (addSvgSignatureToGallery(mSilkySignaturePad.getSignatureSvg())) {
                    Toast.makeText(SettingsActivity.this, "SVG Signature saved into the Gallery", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "Unable to store the SVG signature", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mCompressButton = findViewById(R.id.compress_button);
        mCompressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap signatureBitmap = mSilkySignaturePad.getCompressedSignatureBitmap(50);
                if (addJpgSignatureToGallery(signatureBitmap)) {
                    Toast.makeText(SettingsActivity.this, "50% compressed signature saved into the Gallery", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "Unable to store the signature", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setReportString() {
        String reportString = "";
        int reportCount = 0;
        for (int i = 0; i < settings.reportTypes.length; i++) {
            if (settings.reports[i]) {
                reportString += settings.reportTypes[i];
                reportCount++;
            }
        }
        if (reportCount == 0) {
            settings.reports[0] = true;
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(SettingsActivity.this);
            alertDialog.setTitle("At least 1 report required. 1 report is now selected.");
            alertDialog.setPositiveButton(R.string.ok, null);
            AlertDialog alert = alertDialog.create();
            alert.setCanceledOnTouchOutside(false);
            alert.show();
            reportCount = 1;
        }
        alert_report.setText(reportCount + " selected");

    }


    private void populateSpinnerLanguage() {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            spinner_language.setAdapter(adapter);
            // Set Current Val of the spinner
            spinner_language.setSelection(position_language, true);

    }
    private void populateSpinnerLog() {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.log_level, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner_log.setAdapter(adapter);
        // Set Current Val of the spinner
        spinner_log.setSelection(position_log, true);
    }



    public void populateSpinnerFilter() {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_filter, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner_filter.setAdapter(adapter);
        // Set Current Val of the spinner
        spinner_filter.setSelection(position_filter, true);

    }

    public void populateSpinnerGain(){
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_gain, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner_gain.setAdapter(adapter);
        // Set Current Val of the spinner
        spinner_gain.setSelection(position_gain, true);
    }

    private void openGraphAlertDialogBox() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(SettingsActivity.this);
        alertDialog.setTitle("Select which reports to export");

        alertDialog.setMultiChoiceItems(settings.reportTypes, settings.reports, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    settings.reports[which]=isChecked;
                }
            })
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    setReportString();
                }
            });
        AlertDialog alert = alertDialog.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }


    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick: "+v.getId());
        switch (v.getId()) {
            case R.id.alert_report_arrow:
            case R.id.alert_report:
                openGraphAlertDialogBox();
                break;
            case R.id.settings_save:
                if (getIntent() != null) {
                    if(getIntent().getExtras().getString("From").equalsIgnoreCase("Settings")) {
                        Toast.makeText(SettingsActivity.this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
                        saveSettings();
                        Intent intent = new Intent();
                        setResult(RESULT_OK, intent);
                        finish();
                    } else {
                        Bitmap signatureBitmap = mSilkySignaturePad.getSignatureBitmap();
                        if (addJpgSignatureToGallery(signatureBitmap)) {
                            Toast.makeText(SettingsActivity.this, "Data saved into the Gallery", Toast.LENGTH_SHORT).show();
                            saveSettings();
                            Intent intent = new Intent();
                            setResult(RESULT_OK, intent);
                            finish();
                        } else {
                            Toast.makeText(SettingsActivity.this, "Unable to store the signature and save the data", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
            case R.id.img_profile:
            case R.id.img_plus:
                Dexter.withActivity(this)
                        .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .withListener(new MultiplePermissionsListener() {
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport report) {
                                if (report.areAllPermissionsGranted()) {
                                    showImagePickerOptions();
                                }

                                if (report.isAnyPermissionPermanentlyDenied()) {
                                    showSettingsDialog();
                                }
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                                token.continuePermissionRequest();
                            }
                        }).check();
                break;
            case R.id.header_left_btn:
                onBackPressed();
                break;
        }

    }

    private void saveSettings(){
        //Get value of switch test mode
        //settings.test_mode = settings_test_mode.isChecked() ? "true" : "false";
        if(mstb.getValue() == 1) {
            settings.test_mode = "true";
            tvSelected.setText("You selected TEST Mode");
        } else {
            settings.test_mode = "false";
            tvSelected.setText("You selected ECG Mode");
        }
        settings.csv = switch_csv.isChecked();


        String selected_filter = spinner_filter.getSelectedItem().toString();
        int filterIndex = settings.filter_values.indexOf(selected_filter);
        filterIndex = filterIndex < 0 ? 1 : filterIndex;
        settings.filter = settings.filter_actual_values.get(filterIndex);
        settings.gain = spinner_gain.getSelectedItem().toString();
        settings.doctor_name = settings_doctor_name.getText().toString();
        settings.doctor_address = settings_doctor_address.getText().toString();
        settings.doctor_contactno = settings_doctor_contactno.getText().toString();
        settings.doctor_website = settings_doctor_website.getText().toString();
        settings.doctor_email = settings_doctor_email.getText().toString();
        String oldLang = settings.language;
        settings.language = spinner_language.getSelectedItem().toString();
        settings.log = spinner_log.getSelectedItem().toString();
        settings.language_code = appbt.res.getString(appbt.res.getIdentifier(settings.language, "string", appbt.packageName));

        if (!oldLang.equals(settings.language)) {
//        LocaleHelper.setLocale(SettingsActivity.this, langCode);
//        recreate();
            Locale locale = new Locale(settings.language_code);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
            LocaleHelper.setLocaleLanguage(getApplicationContext());
            recreate();
        }
        settings.saveSettings();
    }

    private void showImagePickerOptions() {
        ImagePickerActivity.showImagePickerOptions(this, new ImagePickerActivity.PickerOptionListener() {
            @Override
            public void onTakeCameraSelected() {
                launchCameraIntent();
            }

            @Override
            public void onChooseGallerySelected() {
                launchGalleryIntent();
            }
        });
    }

    private void launchCameraIntent() {
        Intent intent = new Intent(SettingsActivity.this, ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_IMAGE_CAPTURE);

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);

        // setting maximum bitmap width and height
        intent.putExtra(ImagePickerActivity.INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, true);
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_WIDTH, 1000);
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_HEIGHT, 1000);

        startActivityForResult(intent, REQUEST_IMAGE);
    }

    private void launchGalleryIntent() {
        Intent intent = new Intent(SettingsActivity.this, ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_GALLERY_IMAGE);

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    private void loadProfile(String url) {
        Log.d(TAG, "Image cache path: " + url);

        Glide.with(this).load(url)
                .into(imgProfile);
        imgProfile.setColorFilter(ContextCompat.getColor(this, android.R.color.transparent));
    }

    private void loadProfileDefault() {
        Glide.with(this).load(R.mipmap.baseline_account_circle_black_48)
                .into(imgProfile);
        imgProfile.setColorFilter(ContextCompat.getColor(this, R.color.profile_default_tint));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getParcelableExtra("path");
                try {
                    // You can update this bitmap to your server
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                    // loading profile image from local cache
                    loadProfile(uri.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Showing Alert Dialog with Settings option
     * Navigates user to app settings
     * NOTE: Keep proper title and message depending on your app
     */
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setTitle(getString(R.string.dialog_permission_title));
        builder.setMessage(getString(R.string.dialog_permission_message));
        builder.setPositiveButton(getString(R.string.go_to_settings), (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();

    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    public File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        /*File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);*/
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), albumName);
        if (!file.mkdirs()) {
            Log.e("SignaturePad", "Directory not created");
        }
        return file;
    }

    public static void deleteFiles(String path) {

        File file = new File(path);

        if (file.exists()) {
            String deleteCmd = "rm -r " + path;
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
            } catch (IOException e) { }
        }
    }

    public static void deleteSingleFile(File fdelete) {

        if (fdelete.exists()) {
            if (fdelete.delete()) {
                System.out.println("file Deleted :" + Uri.parse(fdelete.getPath()).getPath());
            } else {
                System.out.println("file not Deleted :" + Uri.parse(fdelete.getPath()).getPath());
            }
        }
    }

    public void saveBitmapToJPG(Bitmap bitmap, File photo) throws IOException {
        Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        /*newBitmap = newBitmap.copy(Bitmap.Config.ARGB_8888,true);
        newBitmap.setWidth(80);
        newBitmap.setHeight(80);*/
        // SP convertToMutable(newBitmap);
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0, 0, null);
        //canvas.drawBitmap(bitmap, null, new RectF(0, 0, 140, 140), null);
        OutputStream stream = new FileOutputStream(photo);
        newBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        stream.close();
    }

    public static Bitmap convertToMutable(Bitmap imgIn) {
        try {
            //this is the file going to use temporally to save the bytes.
            // This file will not be a image, it will store the raw image data.
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

            //Open an RandomAccessFile
            //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            //into AndroidManifest.xml file
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            // get the width and height of the source bitmap.
            int width = imgIn.getWidth()/4;
            int height = imgIn.getHeight()/4;
            Bitmap.Config type = imgIn.getConfig();

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes()*height);
            imgIn.copyPixelsToBuffer(map);
            //recycle the source bitmap, this will be no longer used.
            imgIn.recycle();
            System.gc();// try to force the bytes from the imgIn to be released

            //Create a new bitmap to load the bitmap again. Probably the memory will be available.
            imgIn = Bitmap.createBitmap(width, height, type);
            map.position(0);
            //load it back from temporary
            imgIn.copyPixelsFromBuffer(map);
            //close the temporary file and channel , then delete that also
            channel.close();
            randomAccessFile.close();

            // delete the temp file
            file.delete();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imgIn;
    }

    @SuppressLint("DefaultLocale")
    public boolean addJpgSignatureToGallery(Bitmap signature) {
        boolean result = false;
        try {
            //deleteFiles(new File(getAlbumStorageDir("Documents").getPath()).getPath());
            File photo = new File(getAlbumStorageDir(PathUtil.FolderName), String.format("DoctorSignature.jpg", System.currentTimeMillis()));
            deleteSingleFile(photo);
            saveBitmapToJPG(signature, photo);
            scanMediaFile(photo);
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void scanMediaFile(File photo) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(photo);
        mediaScanIntent.setData(contentUri);
        SettingsActivity.this.sendBroadcast(mediaScanIntent);
    }

    @SuppressLint("DefaultLocale")
    public boolean addSvgSignatureToGallery(String signatureSvg) {
        boolean result = false;
        try {
            File svgFile = new File(getAlbumStorageDir(PathUtil.FolderName), String.format("DoctorSignature.svg", System.currentTimeMillis()));
            OutputStream stream = new FileOutputStream(svgFile);
            OutputStreamWriter writer = new OutputStreamWriter(stream);
            writer.write(signatureSvg);
            writer.close();
            stream.flush();
            stream.close();
            scanMediaFile(svgFile);
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
