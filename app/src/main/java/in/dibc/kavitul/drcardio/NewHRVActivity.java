package in.dibc.kavitul.drcardio;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class NewHRVActivity extends AppCompatActivity {
    private static final String TAG = "LoadDataActivity";
    ImageView hederBack,settings;
    List<String> file_names;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_loaddata);
        //init();
    }

    }
