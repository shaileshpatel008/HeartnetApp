
package in.dibc.kavitul.drcardio;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

public class CrashShareActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            File reportFile = new File(
                    getFilesDir(), "crash_report.txt");

            if (!reportFile.exists() || reportFile.length() == 0) {
                Toast.makeText(this,
                        "Crash report not found",
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            Uri reportUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    reportFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Dr Cardio App Crash Report"
            );
            shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Please find the attached crash report."
            );
            shareIntent.putExtra(Intent.EXTRA_STREAM, reportUri);
            shareIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(Intent.createChooser(
                    shareIntent,
                    "Share Crash Report with Developer"
            ));

        } catch (Exception e) {
            Log.e("APP_CRASH", "Unable to open share chooser", e);
            Toast.makeText(this,
                    "Unable to open crash report sharing",
                    Toast.LENGTH_LONG).show();
        } finally {
            finish();
        }
    }
}
