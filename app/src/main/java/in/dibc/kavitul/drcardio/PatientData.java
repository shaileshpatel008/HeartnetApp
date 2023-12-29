package in.dibc.kavitul.drcardio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.williamww.silkysignature.views.SignaturePad;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import in.dibc.kavitul.drcardio.Model.ecgData;
import in.dibc.kavitul.drcardio.image_picker.ImagePicker;
import in.dibc.kavitul.drcardio.utils.PathUtil;
import in.dibc.kavitul.drcardio.utils.PrefHelper;

public class PatientData extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "PatientData";

    public static final int REQUEST_IMAGE = 100;

    String filePath = "";
    private final DatePickerDialog.OnDateSetListener myDateListener = new
            DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker arg0,
                                      int arg1, int arg2, int arg3) {
                    // TODO Auto-generated method stub
                    // arg1 = year
                    // arg2 = month
                    // arg3 = day
                    showDate(arg1, arg2 + 1, arg3);
                }
            };
    RadioGroup patient_sex;
    Button patientdata_save;
    private int year, month, day;
    public static StringBuilder dob;
    private final String[] dd_mm_yyyy = new String[3];

    private ImageView imageView;
    private TextView textView;
    String patientPhotoPath = "";

    private SignaturePad mSilkySignaturePad;
    private Button mClearButton;
    private Button mSaveButton;
    private Button mCompressButton;

    ImageView imgProfile, imgPlus;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_patientdata);
        initUIElements();
    }

    TextView patient_id, patient_name, patient_age, patient_medications, patient_bloodpressure, patient_dob, patient_height, patient_weight, patient_comments;

    public void populateValues() {
        if (ecgData.patient_name == "" || ecgData.patient_name == null || ecgData.patient_name.length() == 0) {
            Toast.makeText(this, "Patient Name is required", Toast.LENGTH_LONG).show();
            return;
        }
        if (ecgData.patient_id != null) {
            ((EditText) findViewById(R.id.patient_id)).setText(ecgData.patient_id);
        }
        if (ecgData.patient_name != null) {
            ((EditText) findViewById(R.id.patient_name)).setText(ecgData.patient_name);
        }
        if (ecgData.patient_age != null) {
            ((EditText) findViewById(R.id.patient_age)).setText(ecgData.patient_age);
        }
        if (ecgData.patient_comments != null) {
            ((EditText) findViewById(R.id.patient_comments)).setText(ecgData.patient_comments);
        }

        RadioGroup r = findViewById(R.id.patient_sex);
        switch (ecgData.patient_sex) {
            case "Male":
                r.check(R.id.male);
                break;
            case "Female":
                r.check(R.id.female);
                break;
            default:
                r.check(R.id.other);
        }
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.patientdata_save:
                Bitmap signatureBitmap = mSilkySignaturePad.getSignatureBitmap();
                //signatureBitmap.setWidth(40);
                //signatureBitmap.setHeight(40);
                if (addJpgSignatureToGallery(signatureBitmap)) {

                    /*if(ecgData.patient_photo == null) {
                        Toast.makeText(PatientData.this, "Please select your profile photo", Toast.LENGTH_SHORT).show();
                    } else */if(signatureBitmap == null) {
                        Toast.makeText(PatientData.this, "Please do your signature", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(PatientData.this, "Patient data saved successfully", Toast.LENGTH_SHORT).show();
                        savePatientData();
                    }
                } else {
                    Toast.makeText(PatientData.this, "Unable to store the signature", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.patient_dob:
                showDialog(999);
                break;
            case R.id.iv_pdata_back:
                finish();
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
        }

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        // TODO Auto-generated method stub
        if (id == 999) {
            return new DatePickerDialog(this,
                    myDateListener, year, month, day);
        }
        return null;
    }

    public void initUIElements() {
        patient_id = findViewById(R.id.patient_id);
        patient_name = findViewById(R.id.patient_name);
        patient_age = findViewById(R.id.patient_age);
        patient_sex = findViewById(R.id.patient_sex);
        patient_medications = findViewById(R.id.patient_medications);
        patient_bloodpressure = findViewById(R.id.patient_bloodpressure);
        patient_dob = findViewById(R.id.patient_dob);
        patient_height = findViewById(R.id.patient_height);
        patient_weight = findViewById(R.id.patient_weight);
        patient_comments = findViewById(R.id.patient_comments);
        patientdata_save = findViewById(R.id.patientdata_save);
        ImageView iv_pdata_back = findViewById(R.id.iv_pdata_back);
        iv_pdata_back.setOnClickListener(this);
        patient_dob.setOnClickListener(this);
        patientdata_save.setOnClickListener(this);
        populateValues();

        imageView = (ImageView) findViewById(R.id.image_view);
        textView = (TextView) findViewById(R.id.image_stream_indicator);
        // width and height will be at least 600px long (optional).
        ImagePicker.setMinQuality(600, 600);

        imgProfile = findViewById(R.id.img_profile);
        imgPlus = findViewById(R.id.img_plus);

        imgProfile.setOnClickListener(this);
        imgPlus.setOnClickListener(this);

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
                    Toast.makeText(PatientData.this, "Signature saved into the Gallery", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PatientData.this, "Unable to store the signature", Toast.LENGTH_SHORT).show();
                }
                if (addSvgSignatureToGallery(mSilkySignaturePad.getSignatureSvg())) {
                    Toast.makeText(PatientData.this, "SVG Signature saved into the Gallery", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PatientData.this, "Unable to store the SVG signature", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mCompressButton = findViewById(R.id.compress_button);
        mCompressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap signatureBitmap = mSilkySignaturePad.getCompressedSignatureBitmap(50);
                if (addJpgSignatureToGallery(signatureBitmap)) {
                    Toast.makeText(PatientData.this, "50% compressed signature saved into the Gallery", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PatientData.this, "Unable to store the signature", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap bitmap = ImagePicker.getImageFromResult(this, requestCode, resultCode, data);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }
        InputStream is = ImagePicker.getInputStreamFromResult(this, requestCode, resultCode, data);
        if (is != null) {
            textView.setText("Got input stream!");
            patientPhotoPath = getRealPathFromURI(PatientData.this, Uri.parse("content://media"+data.getData().getPath()));
            Log.v("Patient Photo Path :: ", getRealPathFromURI(PatientData.this, Uri.parse("content://media"+data.getData().getPath())));
            try {
                is.close();
            } catch (IOException ex) {
                // ignore
            }
        } else {
            textView.setText("Failed to get input stream!");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }*/

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void onPickImage(View view) {
        ImagePicker.pickImage(this, "Select your image:");
    }

    private void showDate(int year, int month, int day) {
        dob = new StringBuilder().append(day).append("/")
                .append(month).append("/").append(year);
        patient_dob.setText(dob);
        ecgData.patient_age = String.valueOf(getAge(year, month, day));
        patient_age.setText(ecgData.patient_age);
    }

    public int getAge(int year, int month, int dayOfMonth) {
        Calendar birthCal = new GregorianCalendar(year, month, dayOfMonth);

        Calendar nowCal = new GregorianCalendar();
        int age, Age = 10;
        age = nowCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
        boolean isMonthGreater = birthCal.get(Calendar.MONTH) >= nowCal
                .get(Calendar.MONTH);

        boolean isMonthSameButDayGreater = birthCal.get(Calendar.MONTH) >= nowCal.get(Calendar.MONTH)
                && birthCal.get(Calendar.DAY_OF_MONTH) >= nowCal
                .get(Calendar.DAY_OF_MONTH);

        if (age < 18) {
            Age = age;
        } else if (isMonthGreater || isMonthSameButDayGreater) {
            Age = age - 1;
        } else {
            Age = nowCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
        }
        return Age;
    }

    public void savePatientData() {
        ecgData.patient_id = ((EditText) findViewById(R.id.patient_id)).getText().toString();
        ecgData.patient_name = ((EditText) findViewById(R.id.patient_name)).getText().toString();
        ecgData.patient_age = ((EditText) findViewById(R.id.patient_age)).getText().toString();
        Log.v("Patient Comment :: ", ""+((EditText) findViewById(R.id.patient_comments)).getText().toString());
        ecgData.patient_comments = ((EditText) findViewById(R.id.patient_comments)).getText().toString();
        //ecgData.patient_photo = patientPhotoPath;
        if(filePath != null) {
            ecgData.patient_photo = filePath;
        } else {
            ecgData.patient_photo = "";
        }


        RadioGroup r = findViewById(R.id.patient_sex);
        int id = r.getCheckedRadioButtonId();
        RadioButton rd = findViewById(id);
        ecgData.patient_sex = "Other";
        if (rd.getText().toString().equals("Male") || rd.getText().toString().equals("Female")) {
            ecgData.patient_sex = rd.getText().toString();
        }

        if (ecgData.patient_name == "" || ecgData.patient_name == null || ecgData.patient_name.length() == 0) {
            Toast.makeText(this, "Patient Name is required", Toast.LENGTH_LONG).show();
            return;
        }

        int pdfCount = PrefHelper.getInt("PDFGenerateCount", 0);
        PrefHelper.setInt("PDFGenerateCount", (pdfCount+1));

        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
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
        Intent intent = new Intent(PatientData.this, ImagePickerActivity.class);
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
        Intent intent = new Intent(PatientData.this, ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_GALLERY_IMAGE);

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    private void loadProfile(String url) {
        Log.d(TAG, "Image cache path: " + url);

        patientPhotoPath = url;

        Glide.with(this).load(url)
                .into(imgProfile);
        imgProfile.setColorFilter(ContextCompat.getColor(this, android.R.color.transparent));
    }

    private void loadProfileDefault() {
        Glide.with(this).load(R.mipmap.baseline_account_circle_black_48)
                .into(imgProfile);
        imgProfile.setColorFilter(ContextCompat.getColor(this, R.color.profile_default_tint));
    }

    @SuppressLint("LongLogTag")
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

                    //String filePath= PathUtil.getPath(PatientData.this, uri);
                    filePath= getPath(PatientData.this, uri);
                    Log.v("Patient Photo Path :::: ", filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                } /*catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }*/
            }
        }
    }

    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * Showing Alert Dialog with Settings option
     * Navigates user to app settings
     * NOTE: Keep proper title and message depending on your app
     */
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PatientData.this);
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

    @SuppressLint("LongLogTag")
    public void saveBitmapToJPG(Bitmap bitmap, File photo) throws IOException {
        Log.v("Patient Signature Path :::: ", photo.getPath());
        ecgData.patient_signature_photo = photo.getPath();
        Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        //convertToMutable(newBitmap);
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
            int width = imgIn.getWidth();
            int height = imgIn.getHeight();
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
            File photo = new File(getAlbumStorageDir(PathUtil.FolderName), String.format("PatientSignature.jpg", System.currentTimeMillis()));
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
        PatientData.this.sendBroadcast(mediaScanIntent);
    }

    @SuppressLint("DefaultLocale")
    public boolean addSvgSignatureToGallery(String signatureSvg) {
        boolean result = false;
        try {
            File svgFile = new File(getAlbumStorageDir(PathUtil.FolderName), String.format("PatientSignature.svg", System.currentTimeMillis()));
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
