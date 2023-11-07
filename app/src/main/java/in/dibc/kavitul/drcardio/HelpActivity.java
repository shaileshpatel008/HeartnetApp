package in.dibc.kavitul.drcardio;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import in.dibc.kavitul.drcardio.Adapter.HelpListViewAdapter;
import in.dibc.kavitul.drcardio.Model.Helpmodel;

public class HelpActivity  extends Activity implements View.OnClickListener, SearchView.OnQueryTextListener {
    ImageView hederBack;
    ListView list;
    HelpListViewAdapter adapter;
    SearchView editsearch;
    String[] animalNameList;
    ArrayList<Helpmodel> arraylist = new ArrayList<Helpmodel>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_help);
        hederBack = findViewById(R.id.btn_back_press_help);
        hederBack.setOnClickListener(this);

        String[] listviewTitle =  getResources().getStringArray(R.array.help_faq_title);
        String[] listviewShortDescription = getResources().getStringArray(R.array.help_faq_desc);



        // Locate the ListView in listview_main.xml
        list = findViewById(R.id.list_view_help);

        for (int i = 0; i < listviewTitle.length; i++) {
            Helpmodel descriptionmodel = new Helpmodel(listviewTitle[i], listviewShortDescription[i]);
            // Binds all strings into an array
            arraylist.add(descriptionmodel);
        }

        // Pass results to ListViewAdapter Class
        adapter = new HelpListViewAdapter(this, arraylist);

        // Binds the Adapter to the ListView
        list.setAdapter(adapter);

        // Locate the EditText in listview_main.xml
        editsearch = findViewById(R.id.mySearchView);
        editsearch.setOnQueryTextListener(HelpActivity.this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {

        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String text = newText;
        adapter.filter(text);
        return false;
    }





    @Override
    public void onBackPressed() {
        finish();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_back_press_help:
                finish();
                break;
        }


    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

}
