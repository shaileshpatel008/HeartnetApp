package in.dibc.kavitul.drcardio;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import in.dibc.kavitul.drcardio.Adapter.ScanPagerAdapter;

public class ScanListActivity extends AppCompatActivity {

    TabLayout tabLayout;
    ViewPager2 viewPager;
    ImageView ivBack;
    ScanPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scans_list);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        tabLayout.setTabTextColors(
                ContextCompat.getColor(ScanListActivity.this, R.color.default_inactive),
                ContextCompat.getColor(ScanListActivity.this, R.color.white)
        );

        pagerAdapter = new ScanPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(position == 0 ? "Pending Scans" : "Reported Scans")).attach();
    }
}