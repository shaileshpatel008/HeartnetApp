package in.dibc.kavitul.drcardio;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.rajat.pdfviewer.PdfViewerActivity;
import com.rajat.pdfviewer.util.saveTo;
import com.simform.custombottomnavigation.Model;
import com.simform.custombottomnavigation.SSCustomBottomNavigation;
//import com.gauravk.bubblenavigation.BubbleNavigationLinearView;
//import com.gauravk.bubblenavigation.listener.BubbleNavigationChangeListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import in.dibc.kavitul.drcardio.Adapter.ItemListViewAdapter;
import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.mFile;
import in.dibc.kavitul.drcardio.SupportClass.settings;
import in.dibc.kavitul.drcardio.utils.PathUtil;
import in.dibc.kavitul.drcardio.utils.PrefHelper;

public class ReportActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ReportActivity";
    ListView file_names_list;
    ArrayList<String> file_names = new ArrayList<>();
    ArrayList<String> filePathList = new ArrayList<>();

    ArrayList<String> listStatus = new ArrayList<>();

    TextView tvNoReports;

    EditText etSearch;

    LinearLayout llSearch;

    TextView header_text;

    private static final int ID_HOME = 0;
    private static final int ID_REPORTS = 1;
    private static final int ID_SETTINGS = 2;

//    BubbleNavigationLinearView bubbleNavigation;

    SSCustomBottomNavigation ssCustomBottomNavigation;

    private int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_reports);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();

//        bubbleNavigation.setCurrentActiveItem(1);

        ssCustomBottomNavigation.setSelectedIndex(1);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ssCustomBottomNavigation.setSelectedIndex(0);
    }

    public void init() {
        file_names_list = findViewById(R.id.file_names_list);
        header_text = findViewById(R.id.header_text);
        //hd.setText("REPORTS");
        ImageView hederBack = findViewById(R.id.header_left_btn);
        hederBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
                ssCustomBottomNavigation.setSelectedIndex(0);
//                Intent intent = new Intent(ReportActivity.this, PDFViewerActivity.class);
//                intent.putExtra("PDFPath", "/storage/emulated/0/Documents/Dr.cardio/Reports/Test_2024_03_14_11_23.pdf");
//                startActivity(intent);
            }
        });

//        bubbleNavigation = findViewById(R.id.equal_navigation_bar);
//        bubbleNavigation.setNavigationChangeListener(new BubbleNavigationChangeListener() {
//            @Override
//            public void onNavigationChanged(View view, int position) {
//                //navigation changed, do something
//                if(position == 0) {
//                    finish();
//                } else if(position == 2) {
//                    Intent intent = new Intent(ReportActivity.this, MyAccountsActivity.class);
//                    startActivity(intent);
//                    finish();
//                }
//            }
//        });

        ssCustomBottomNavigation = findViewById(R.id.bottomNavigation);
        ssCustomBottomNavigation.setSelectedIndex(0);
        ssCustomBottomNavigation.add(new Model(R.drawable.ic_home_new, ID_HOME, ID_HOME, R.string.menu_1, R.string.menu_1));
        ssCustomBottomNavigation.add(new Model(R.drawable.ic_reports_new, ID_REPORTS, ID_REPORTS, R.string.menu_2, R.string.menu_2));
        ssCustomBottomNavigation.add(new Model(R.drawable.ic_settings, ID_SETTINGS, ID_SETTINGS, R.string.menu_3, R.string.menu_3));
        ssCustomBottomNavigation.clearAllCounts();

        ssCustomBottomNavigation.setOnClickMenuListener(item -> {
            String name;
            Intent intent;
            switch (item.getId()) {
                case ID_HOME:
                    name = "HOME";
                    intent = new Intent(ReportActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    ssCustomBottomNavigation.setSelectedIndex(0);
                    break;
                case ID_REPORTS:
//                    name = "REPORTS";
//                    intent = new Intent(ReportActivity.this, ReportActivity.class);
//                    startActivity(intent);
//                    finish();
//                    ssCustomBottomNavigation.setSelectedIndex(0);
                    break;
                case ID_SETTINGS:
                    name = "MY ACCOUNTS";
                    intent = new Intent(ReportActivity.this, MyAccountsActivity.class);
                    startActivity(intent);
                    finish();
                    ssCustomBottomNavigation.setSelectedIndex(0);
                    break;
                default:
                    name = "";
            }
            // Use the 'name' variable here if needed
            return null;
        });

        tvNoReports = findViewById(R.id.tvNoReports);
        llSearch = findViewById(R.id.llSearch);

        //registerForContextMenu(file_names_list);
        //Populating the list view

        //#Bhavesh Change
        /*if (SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                load_file_list();
            } else { //request for the permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        } else {
            //below android 11=======
            if (SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG,"1111 Permission is granted");
                    load_file_list();
                    //return true;
                } else {
                    Log.v(TAG,"1111 Permission is revoked");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    //return false;
                }
            }
            else { //permission is automatically granted on sdk<23 upon installation
                Log.v(TAG,"2222 Permission is granted");
                load_file_list();
                //return true;
            }
        }*/



//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
//                Log.v(TAG,"1111 Permission is granted");
//                load_file_list();
//                //return true;
//            } else {
//                Log.v(TAG,"1111 Permission is revoked");
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
//                //return false;
//            }
//        } else {
//            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//                Log.v(TAG,"1111 Permission is granted");
//                load_file_list();
//                //return true;
//            } else {
//                Log.v(TAG,"1111 Permission is revoked");
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
//                //return false;
//            }
//        }



        //load_file_list();

        if(PathUtil.INTERNET_STATUS) {
            callGetReportsECGsListApi();
        } else {
            load_file_list();
        }

        File extStorageDir = new File(Environment.getExternalStorageDirectory().toString());
        String[] fileList = extStorageDir.list();

        //Log.v("Documents Files :: ",""+extStorageDir.list(FilenameFilter));

        for(String fileName:fileList)
            Log.v("FileName=", fileName.toString());


        //On clicking an item in the list
        file_names_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                displayMenuList(position);

            }
        });

//        getReportsCount();
    }

    private ArrayList<String> getReportsCount() {
        String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_DATA] + "/";
        File dir = new File(filePath);
        ArrayList<String> list = new ArrayList<String>();
        if (dir.isDirectory()) {
            String[] files = dir.list();
            Pattern p = Pattern.compile("^(.*?)\\.dat$");
            for (String file : files) {
                Matcher m = p.matcher(file);
                if (m.matches()) {
                    list.add(m.group(1));
                }
            }
        }

        if(list.size() > 0) {
            header_text.setText("Reports ("+list.size()+")");
        }
        return list;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                load_file_list();
            }
        }

    }


    public void displayMenuList(int position) {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(ReportActivity.this);
        builder.setTitle("Choose an option");

        // add a list
        String[] animals = {"View", "Share"/*, "Delete"*/};
        builder.setItems(animals, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
//                        openPDFFile(file_names.get(position));

//                        String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + file_names.get(position) + ".pdf";
//                        Intent intent = new Intent(ReportActivity.this, PDFViewerActivity.class);
//                        intent.putExtra("PDFPath", filePath);
//                        intent.putExtra("FileName", file_names.get(position) + ".pdf");
//                        startActivity(intent);


                        String localFilePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + file_names.get(position).replaceAll("@reported@", "") + ".pdf";
                        if(PathUtil.INTERNET_STATUS) {
//                            Intent intent = new Intent(ReportActivity.this, PDFViewerActivity.class);
//                            intent.putExtra("PDFPath", filePathList.get(position));
//                            intent.putExtra("FileName", file_names.get(position).replaceAll("@reported@", "") + ".pdf");
//                            intent.putExtra("LocalFilePath", localFilePath);
//                            startActivity(intent);

                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(filePathList.get(position)));
                            startActivity(browserIntent);
                        } else {
                            openPDFFile(file_names.get(position));
                        }

                        break;
                    case 1:
                        if(PathUtil.INTERNET_STATUS) {
                            shareFile(position, filePathList.get(position));
                            /*String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + file_names.get(position).replaceAll("@reported@", "") + ".pdf";
                            Log.v("Share File Path :::: ", filePath);
                            File file = new File(filePath);
                            //File file = new File(mFile.getDirFile(this,mFile.DIR_REPORT),file_names.get(index) + ".pdf");
                            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                            sharingIntent.setType("application/pdf");
                            Uri uri = getUriFromFile(file, ReportActivity.this);
                            if (uri != null) {
                                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                startActivity(Intent.createChooser(sharingIntent, "share file with"));
                            }*/
                        } else {
                            String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + file_names.get(position).replaceAll("@reported@", "") + ".pdf";
                            Log.v("Share File Path :::: ", filePath);
                            File file = new File(filePath);
                            //File file = new File(mFile.getDirFile(this,mFile.DIR_REPORT),file_names.get(index) + ".pdf");
                            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                            sharingIntent.setType("application/pdf");
                            Uri uri = getUriFromFile(file, ReportActivity.this);
                            if (uri != null) {
                                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                startActivity(Intent.createChooser(sharingIntent, "share file with"));
                            }
                        }

                        break;
                    case 2:
                        new AlertDialog.Builder(ReportActivity.this)
                                .setTitle(R.string.delete)
                                .setMessage(R.string.are_you_sure)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        deletePdfFile(position);
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .create()
                                .show();
                        break;
                }
            }
        });

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

        }

    }

    private void openPDFFile(String file_name) {
//        AssetManager assetManager = getAssets();
//        InputStream in = null;
//        OutputStream out = null;

        //#Bhavesh Change
        String filepath = appbt.storageBaseDir +"/"+ appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + file_name + ".pdf";
        File file = new File(filepath);//here schedule1.pdf is the pdf file name which is keep in assets folder.
        //Log.v("Share PDF Path :::: ",""+file.getAbsolutePath());

        //File file = new File(mFile.getDirFile(this,mFile.DIR_REPORT),file_name + ".pdf");

        /*
        try {
            in = assetManager.open(path);
            out = openFileOutput(file.getName(), Context.MODE_WORLD_READABLE);

            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + appbt.storageBaseDir +"/"+ appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + file_name + ".pdf"), "application/pdf");
        startActivity(intent);
         */
        String mimeType = "application/pdf";

        try{
            Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setType(mimeType);
//        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeType);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = FileProvider.getUriForFile(getBaseContext(), getApplicationContext().getPackageName() + ".provider", file);
            intent.setDataAndType(uri,mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        PackageManager pm = getPackageManager();
//        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            startActivity(Intent.createChooser(intent, "Open PDF report using.."));
        } catch(ActivityNotFoundException e) {
            Toast.makeText(this, "Unable to open report. make sure your phone has a PDF viewer installed. Report file: "+ file, Toast.LENGTH_LONG).show();
        }
    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }


    /* Populating the list view */
    public void load_file_list() {
        file_names = getFileNames();

        /*ArrayAdapter adapter = new ArrayAdapter<String>(this,
                R.layout.act_file_name, file_names);*/

        ListView listView = findViewById(R.id.file_names_list);
        //listView.setAdapter(adapter);

        ItemListViewAdapter itemAdapter = new ItemListViewAdapter(this, R.layout.item_list_name, file_names);
        listView.setAdapter(itemAdapter);

        etSearch = findViewById(R.id.etSearch);
        // Add Text Change Listener to EditText
        etSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Call back the Adapter with current character to Filter
                itemAdapter.getFilter().filter(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        if(file_names.size() == 0) {
            tvNoReports.setVisibility(View.VISIBLE);
            llSearch.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
        } else {
            tvNoReports.setVisibility(View.GONE);
            llSearch.setVisibility(View.VISIBLE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    /*
        Creating the context menu
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        Log.d(TAG, "onCreateContextMenu: "+ menuInfo);
        if (v.getId() == R.id.file_names_list) {
            menu.setHeaderTitle("Choose an option");
            menu.add(info.position, 0, 0, "Delete");
            menu.add(info.position, 1, 1, "Share");
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(String.valueOf(item.getTitle())) {
            case "Share":
                shareFile(info.position, "");
                return true;
            case "Delete":
                new AlertDialog.Builder(this)
                        .setTitle(R.string.delete)
                        .setMessage(R.string.are_you_sure)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                deletePdfFile(info.position);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create()
                        .show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /*
   Deleting the .dat file selected by the user
    */
    public void deletePdfFile(int index) {
        Log.d(TAG, "deleteDatFile: " + index);
        //#Bhavesh Change
        String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + file_names.get(index) + ".pdf";
        File file = new File(filePath);
        //File file = new File(mFile.getDirFile(this,mFile.DIR_REPORT),file_names.get(index) + ".pdf");
        boolean deleted = file.delete();
        if (deleted) {
            Log.d(TAG, "Deleted");
            load_file_list();
        } else {
            Toast.makeText(getApplicationContext(), "Unable to delete the file!", Toast.LENGTH_SHORT).show();
        }
    }

    public void shareFile(int index, String fileURL) {
        //#Bhavesh Change
//        String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + file_names.get(index).replaceAll("@reported@", "") + ".pdf";
//        Log.v("Share File Path :::: ", filePath);
//        File file = new File(filePath);
//        //File file = new File(mFile.getDirFile(this,mFile.DIR_REPORT),file_names.get(index) + ".pdf");
//        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
//        sharingIntent.setType("application/pdf");
//        Uri uri = getUriFromFile(file, this);
//        if (uri != null) {
//            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
//            startActivity(Intent.createChooser(sharingIntent, "share file with"));
//        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, fileURL.replaceAll(" ", "%20"));
        startActivity(Intent.createChooser(share, "Share Text"));
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

    /*
        Getting the PDF file names in the data directory and storing them in a list
   */
    private ArrayList<String> getFileNames() {
        String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/";
        //String filePath = mFile.getDirFile(this,mFile.DIR_REPORT).getAbsolutePath();
        Log.v("Reports File Path :: ", ""+filePath);
        File dir = new File(filePath);
        Log.v("Directory Created :: ", ""+dir.isDirectory());
        ArrayList<String> list = new ArrayList<String>();
        if (dir.isDirectory()) {
            Log.v("Directory Length :: ", ""+dir.listFiles().length);
            if(dir.list() != null) {
                String[] files = dir.list();
                Pattern p = Pattern.compile("^(.*?)\\.pdf$");
                Log.v("Files Length :: ", ""+files.length);
                for (String file : files) {
                    Matcher m = p.matcher(file);
                    if (m.matches()) {
                        list.add(m.group(1));
                    }
                }
            }
        }
        Collections.reverse(list);
        return list;
    }

    private void callGetReportsECGsListApi() {

        ProgressDialog pd = new ProgressDialog(ReportActivity.this);
        pd.setMessage("Getting...");
        pd.setCancelable(false);
        pd.show();

        // url to post our data
        String url = "https://cloudecg.drcardio.in/api/ecg-list";
//        String url = "http://192.168.1.2/api/ecg-list";
//        loadingPB.setVisibility(View.VISIBLE);

        // creating a new variable for our request queue
        RequestQueue queue = Volley.newRequestQueue(ReportActivity.this);

        // on below line we are calling a string
        // request method to post the data to our API
        // in this we are calling a post method.
        StringRequest request = new StringRequest(Request.Method.GET, url, new com.android.volley.Response.Listener<String>() {
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
                        Log.v("List Count :: ", ""+respObj.getJSONArray("success_data").length());
                        pd.dismiss();
                        JSONArray jArray = new JSONArray(respObj.getJSONArray("success_data").toString());
                        for(int i=0; i<jArray.length(); i++) {
                            String fileName = jArray.getJSONObject(i).getString("document_name").toString();
                            if(jArray.getJSONObject(i).getString("status").contains("reported")) {
                                file_names.add("@reported@"+fileName);
                            } else {
                                file_names.add(fileName);
                            }
                            Log.v("Status "+i, jArray.getJSONObject(i).getString("status"));
                            //  SP file_names.add(fileName); //jArray.getJSONObject(i).getString("status")+"$$"+
//                            listStatus.add(jArray.getJSONObject(i).getString("status"));
                            filePathList.add(jArray.getJSONObject(i).getString("document_path"));
                        }

                        if(file_names.size() > 0) {
                            header_text.setText("Reports ("+file_names.size()+")");
                        }

                        ListView listView = findViewById(R.id.file_names_list);
                        //listView.setAdapter(adapter);

                        ItemListViewAdapter itemAdapter = new ItemListViewAdapter(ReportActivity.this, R.layout.item_list_name, file_names);
                        listView.setAdapter(itemAdapter);

                        etSearch = findViewById(R.id.etSearch);
                        // Add Text Change Listener to EditText
                        etSearch.addTextChangedListener(new TextWatcher() {

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                // Call back the Adapter with current character to Filter
                                itemAdapter.getFilter().filter(s.toString());
                            }

                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count,int after) {
                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                            }
                        });

                        if(file_names.size() == 0) {
                            tvNoReports.setVisibility(View.VISIBLE);
                            llSearch.setVisibility(View.GONE);
                            listView.setVisibility(View.GONE);
                        } else {
                            tvNoReports.setVisibility(View.GONE);
                            llSearch.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.VISIBLE);
                        }

                    } else {
                        pd.dismiss();
                        Toast.makeText(ReportActivity.this, respObj.getString("error_data").toString(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(ReportActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
            }
        }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = PrefHelper.getString("Token", "");
                Log.v("Token :: ", token.toString());
                Map<String, String> params = new HashMap<String, String>();
//                    params.put("Content-Type", "multipart/form-data; charset=UTF-8");
                params.put("Authorization", "Bearer "+token);
                Log.v("Authorization :: ", params.toString());
                return params;
            }
            @Override
            protected Map<String, String> getParams() {
                // storing our values in key and value pair.
                Map<String, String> params = new HashMap<String, String>();

//                params.put("email", edtUserName.getText().toString());
//                params.put("password", edtPassword.getText().toString());

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