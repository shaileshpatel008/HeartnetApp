package in.dibc.kavitul.drcardio;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import in.dibc.kavitul.drcardio.SupportClass.appbt;
import in.dibc.kavitul.drcardio.SupportClass.mFile;


public class LoadPrevVerDataActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "FilterTest";
    ImageView hederBack,settings;
    ListView file_names_list;
    ArrayList<String> file_names;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_filter_test);
        init();
    }

    public void init(){
        hederBack = findViewById(R.id.header_left_btn);
        hederBack.setOnClickListener(this);
        file_names_list = findViewById(R.id.file_names_list);
        registerForContextMenu(file_names_list);
        //Populating the list view
        load_file_list();
        //On clicking an item in the list
        file_names_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                //send to mFile to make file compatible
                String file_name = mFile.makeCompatible(file_names.get(position));
                if(file_name == null || file_name.equals("")){
                    Toast.makeText(getApplicationContext(), "Unable to read the file.", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(getApplicationContext(), NewEcgActivity.class);
                Bundle bundle = new Bundle();
                //Add file name to send to NewECGActivity
                bundle.putString("file_name", file_name);
                //Add the bundle to the intent
                intent.putExtras(bundle);
                //Fire the second activity
                startActivity(intent);
                finish();

//                Intent intent = new Intent(getApplicationContext(), NewEcgActivity.class);
//                Bundle bundle = new Bundle();
//                //Add file name to send to NewECGActivity
//                bundle.putString("file_name", file_names.get(position));
//                //Add the bundle to the intent
//                intent.putExtras(bundle);
//                //Fire the second activity
//                startActivity(intent);
//                finish();
            }
        });
    }

    /* Populating the list view */
    public void load_file_list()
    {
        file_names = getFileNames();
        ArrayAdapter adapter = new ArrayAdapter<String>(this,
                R.layout.act_file_name, file_names);

        ListView listView = (ListView) findViewById(R.id.file_names_list);
        listView.setAdapter(adapter);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.header_left_btn:
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
                finish();
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
            menu.add(info.position, 0, 0,  "Delete");
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(String.valueOf(item.getTitle())) {
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
    Deleting the .dat file selected by the user
     */
    public void deleteDatFile(int index){
        Log.d(TAG, "deleteDatFile: "+ index);
        String filePath = appbt.storageBaseDir +"/"+ appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_OLD_DATA] + "/" + file_names.get(index) + ".dat";
        File file = new File(filePath);
        boolean deleted = file.delete();
        if(deleted){
            Log.d(TAG,"Deleted");
            load_file_list();
        }else{
            Toast.makeText(getApplicationContext(),(String)"Unable to delete the file!", Toast.LENGTH_SHORT).show();
        }
    }

    /*
    Getting the file names in the data directory and storing them in a list
     */
    private ArrayList<String> getFileNames(){
        String filePath = appbt.storageBaseDir +"/"+ appbt.storageSubDirs[appbt.STORAGE_SUB_DIR_OLD_DATA] + "/";
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
        return list;
    }

}
