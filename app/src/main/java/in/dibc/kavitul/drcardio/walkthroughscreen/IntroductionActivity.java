package in.dibc.kavitul.drcardio.walkthroughscreen;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.codemybrainsout.onboarder.AhoyOnboarderActivity;
import com.codemybrainsout.onboarder.AhoyOnboarderCard;

import java.util.ArrayList;
import java.util.List;

import in.dibc.kavitul.drcardio.MainActivity;
import in.dibc.kavitul.drcardio.R;
import in.dibc.kavitul.drcardio.StartupActivity;
import in.dibc.kavitul.drcardio.utils.PrefHelper;

public class IntroductionActivity extends AhoyOnboarderActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        AhoyOnboarderCard ahoyOnboarderCard1 = new AhoyOnboarderCard("Place the electrode on chest", "The portable ECG machine is lightweight and can be controlled with a smartphone.\n\n\n\n", R.drawable.introduction1);
        AhoyOnboarderCard ahoyOnboarderCard2 = new AhoyOnboarderCard("Connect app and device via bluetooth", "It simultaneously acquires all 12 channels of ECG with clinical-grade accuracy.\n\n\n\n", R.drawable.introduction2);
        AhoyOnboarderCard ahoyOnboarderCard3 = new AhoyOnboarderCard("Test and generate a report", "Its easy handling makes it convenient for many customers. For a demonstration of the Dr. Cardio portable ECG machine, please contact Kavitul Technologies Pvt. Ltd.", R.drawable.introduction3);

        ahoyOnboarderCard1.setIconLayoutParams(width - 400, width - 300, 30,30,30,0);
        ahoyOnboarderCard2.setIconLayoutParams(width - 400, width - 300, 30,30,30,0);
        ahoyOnboarderCard3.setIconLayoutParams(width - 400, width - 300, 30,30,30,0);

        ahoyOnboarderCard1.setBackgroundColor(R.color.white);
        ahoyOnboarderCard2.setBackgroundColor(R.color.white);
        ahoyOnboarderCard3.setBackgroundColor(R.color.white);

        List<AhoyOnboarderCard> pages = new ArrayList<>();

        pages.add(ahoyOnboarderCard1);
        pages.add(ahoyOnboarderCard2);
        pages.add(ahoyOnboarderCard3);



        for (AhoyOnboarderCard page : pages) {
            page.setTitleColor(R.color.black);
            page.setDescriptionColor(com.codemybrainsout.onboarder.R.color.grey_600);
        }

        setFinishButtonTitle("Get Started");
        showNavigationControls(false);
        setFinishButtonDrawableStyle(ContextCompat.getDrawable(this, R.drawable.btn_roundedrect_black));

        List<Integer> colorList = new ArrayList<>();
        colorList.add(R.color.colorPrimary);
        colorList.add(R.color.colorPrimary);
        colorList.add(R.color.colorPrimary);

        setColorBackground(colorList);

        /*Typeface face = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        setFont(face);*/

        setOnboardPages(pages);

    }

    @Override
    public void onFinishButtonPressed() {
        //Toast.makeText(this, "Finish Pressed", Toast.LENGTH_SHORT).show();

        PrefHelper.setBoolean("isFirstTimeLoad", false);
        Intent intent = new Intent(IntroductionActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
