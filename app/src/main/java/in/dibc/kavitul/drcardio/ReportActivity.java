package in.dibc.kavitul.drcardio;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.mFile;

public class ReportActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ReportActivity";
    ListView file_names_list;
    ArrayList<String> file_names;
    private int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_loaddata);
        init();
    }

    public void init() {
        file_names_list = findViewById(R.id.file_names_list);
        TextView hd = findViewById(R.id.header_text);
        hd.setText(R.string.reports);
        ImageView hederBack = findViewById(R.id.header_left_btn);
        hederBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
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

        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG,"1111 Permission is granted");
            load_file_list();
            //return true;
        } else {
            Log.v(TAG,"1111 Permission is revoked");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
            //return false;
        }

        //load_file_list();

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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                load_file_list();
            }
            /*else {

            }*/
        }

    }


    public void displayMenuList(int position) {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(ReportActivity.this);
        builder.setTitle("Choose an option");

        // add a list
        String[] animals = {"View", "Share", "Delete"};
        builder.setItems(animals, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        openPDFFile(file_names.get(position));
                        break;
                    case 1:
                        shareFile(position);
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
    public void load_file_list()
    {
        file_names = getFileNames();
        ArrayAdapter adapter = new ArrayAdapter<String>(this,
                R.layout.act_file_name, file_names);

        ListView listView = findViewById(R.id.file_names_list);
        listView.setAdapter(adapter);
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
                shareFile(info.position);
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

    public void shareFile(int index) {
        //#Bhavesh Change
        String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_REPORT] + "/" + file_names.get(index) + ".pdf";
        File file = new File(filePath);
        //File file = new File(mFile.getDirFile(this,mFile.DIR_REPORT),file_names.get(index) + ".pdf");
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("application/pdf");
        Uri uri = getUriFromFile(file, this);
        if (uri != null) {
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(sharingIntent, "share file with"));
        }
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
}