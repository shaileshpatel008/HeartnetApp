package in.dibc.kavitul.drcardio;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import in.dibc.kavitul.drcardio.SupportClass.appbt;

public class CsvDataActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "CsvDataActivity";
    ImageView headerBack,settings;
    ListView file_names_list;
    ArrayList<String> file_names;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_loaddata);
        init();
    }

    public void init(){
        TextView hd = findViewById(R.id.header_text);
        hd.setText(R.string.csv);
        headerBack = findViewById(R.id.header_left_btn);
        headerBack.setOnClickListener(this);
        file_names_list = findViewById(R.id.file_names_list);
        registerForContextMenu(file_names_list);
        //Populating the list view
        load_file_list();
        //On clicking an item in the list
        file_names_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                File file = new File(appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_CSV] + "/" + file_names.get(position) + ".csv");
                Uri uri = FileProvider.getUriForFile(getBaseContext(), getApplicationContext().getPackageName() + ".provider", file);
                intent.setDataAndType(uri, "text/plain");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Open CSV file using.."));
            }
        });
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


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.header_left_btn:
                onBackPressed();
                break;
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
                shareDatFile(info.position);
                return true;
            case "Delete":
                new AlertDialog.Builder(this)
                        .setTitle(R.string.delete)
                        .setMessage(R.string.are_you_sure)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                deleteDatFile(info.position);
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
    Deleting the .csv file selected by the user
     */
    public void deleteDatFile(int index){
        String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_CSV] + "/" + file_names.get(index) + ".csv";
        File file = new File(filePath);
        boolean deleted = file.delete();
        if (deleted) {
            Log.d(TAG, "Deleted");
            load_file_list();
        } else {
            Toast.makeText(getApplicationContext(), "Unable to delete the file!", Toast.LENGTH_SHORT).show();
        }
    }

    public void shareDatFile(int index) {
        String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_CSV] + "/" + file_names.get(index) + ".csv";
        File file = new File(filePath);
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("txt/*");
        Uri uri = getUriFromFile(file, this);
        if (uri != null) {
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(sharingIntent, "share file with"));
        }
    }

    private Uri getUriFromFile(File file, Context context) {
        Uri uri = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
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
    Getting the file names in the data directory and storing them in a list
     */
    private ArrayList<String> getFileNames() {
        String filePath = appbt.storageBaseDir + "/" + appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_CSV] + "/";
        File dir = new File(filePath);
        ArrayList<String> list = new ArrayList<String>();
        if (dir.isDirectory()) {
            String[] files = dir.list();
            Pattern p = Pattern.compile("^(.*?)\\.csv$");
            for (String file : files) {
                Matcher m = p.matcher(file);
                if (m.matches()) {
                    list.add(m.group(1));
                }
            }
        }
        Collections.reverse(list);
        return list;
    }

}