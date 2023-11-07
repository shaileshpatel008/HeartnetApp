package in.dibc.kavitul.drcardio.SupportClass;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static void setLocaleLanguage (Context context){
        Locale locale = new Locale(settings.language_code);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getApplicationContext().getResources().updateConfiguration(config, null);
    }
}
