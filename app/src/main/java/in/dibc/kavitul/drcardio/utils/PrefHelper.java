package in.dibc.kavitul.drcardio.utils;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import in.dibc.kavitul.drcardio.Model.ECGDataModel;
import in.dibc.kavitul.drcardio.SupportClass.App;

/**
 * This class is help to store/get data into preference
 * */
public class PrefHelper {

	public static void setString(String key, String value) {
		final Editor editor = App.myPref.edit();
		editor.putString(key, value);
		apply(editor);
	}

	public static String getString(String key, String defaultValue) {
		return App.myPref.getString(key, defaultValue);
	}

	public static void setInt(String key, int value) {
		final Editor editor = App.myPref.edit();
		editor.putInt(key, value);
		apply(editor);
	}

	public static int getInt(String key, int defaultValue) {
		return App.myPref.getInt(key, defaultValue);
	}

	public static boolean getBoolean(String key, boolean defaultValue) {
		return App.myPref.getBoolean(key, defaultValue);
	}

	public static void setBoolean(String key, boolean value) {
		final Editor editor = App.myPref.edit();
		editor.putBoolean(key, value);
		apply(editor);
	}

	public static long getLong(String key, long defaultValue) {
		return App.myPref.getLong(key, defaultValue);
	}

	public static void setLong(String key, long value) {
		final Editor editor = App.myPref.edit();
		editor.putLong(key, value);
		apply(editor);
	}

	public static double getDouble(String key, double defaultValue) {
//		return WellingtonInfoApplication.myPref.getLong(key, defaultValue);
		return Double.longBitsToDouble(App.myPref.getLong(key, Double.doubleToLongBits(defaultValue)));
	}

	public static void setDouble(String key, double value) {
		final Editor editor = App.myPref.edit();
		editor.putLong(key, Double.doubleToRawLongBits(value));
		apply(editor);
	}

	public static void deletePreference(String key) {
		final Editor editor = App.myPref.edit();
		editor.remove(key);
		apply(editor);
	}

	public static void deletePreferences(String[] keys) {
		if(keys!=null && keys.length>0)
		{
//			Editor editor = null;
			for(int i=0;i<keys.length;i++)
			{
				final Editor editor= App.myPref.edit();
				editor.remove(keys[i]);
				apply(editor);
			}
		}
	}

	public static void deleteAllPreferences() {
		final Editor editor = App.myPref.edit();
		editor.clear();
		apply(editor);
	}

	/*Delete all preference of app except passed argument*/
	public static void deletePreferencesExcept(ArrayList<String> keysNotDelete) {
		//SharedPreferences pref=PreferenceManager.getDefaultSharedPreferences(WellingtonInfoApplication.appInstance);
		Map<String,?> keys = App.myPref.getAll();

		for(Map.Entry<String,?> entry : keys.entrySet()){
			Log.d("map values", entry.getKey() + ": " +
					entry.getValue().toString());
			if(!keysNotDelete.contains(entry.getKey()))
			{
				final Editor editor= App.myPref.edit();
				editor.remove(entry.getKey());
				apply(editor);
			}
		}
	}

	// Faster pref saving for high performance
	private static final Method sApplyMethod = findApplyMethod();

	private static Method findApplyMethod() {
		try {
			final Class<Editor> cls = Editor.class;
			return cls.getMethod("apply");
		} catch (final NoSuchMethodException unused) {
			// fall through
		}
		return null;
	}

	public static void apply(final Editor editor) {
		if (sApplyMethod != null) {
			try {
				sApplyMethod.invoke(editor);
				return;
			} catch (final InvocationTargetException unused) {
				// fall through
			} catch (final IllegalAccessException unused) {
				// fall through
			}
		}
		editor.commit();
	}


	private static final String URI = "URI";

	public static void setSharedPrefrenceUri(Context context, String string) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putString(URI, string).commit();
	}

	public static Uri getSharedPrefrenceUri(Context context) {
		String uri = PreferenceManager.getDefaultSharedPreferences(context).getString(URI, "");
		if (TextUtils.isEmpty(uri))
			return null;
		else
			return Uri.parse(uri);

	}

	public static final void setActualPath(Context context, String localPath, String actualPath) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putString(localPath, actualPath).commit();
	}

	public static final String getActualPath(Context context, String localPath) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(localPath, null);
	}

	private static final String DOWNLOAD_IDS = "DOWNLOAD_IDS";

	public static void saveDownloadIDs(Context context, String downloadIds) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putString(DOWNLOAD_IDS, downloadIds).commit();
	}

	public static String getDownloadIDs(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(DOWNLOAD_IDS, "");
	}
	private static final String DATA_ACCCESS_PATH = "DATA_ACCCESS_PATH";

	public static final void setStoreDataPath(Context context, String path) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putString(DATA_ACCCESS_PATH, path).commit();
	}

	public static final String getStoreDataPath(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(DATA_ACCCESS_PATH,
				"");
		//StorageUtil.getInternalStoragePath()
	}

	private static final String IS_SDCARD_SELECTED = "IS_SDCARD_SELECTED";

	public static boolean isMemoryCardSelected(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(IS_SDCARD_SELECTED, false);
	}

	public static void setMemoryCardSelected(Context context, boolean isSelectes) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(IS_SDCARD_SELECTED, isSelectes)
				.commit();
	}

	public static void setECGDataListResponse(String key, ArrayList<ECGDataModel> getECGDataModel) {
		final Editor editor = App.myPref.edit();
		Gson gson = new Gson();
		String json = gson.toJson(getECGDataModel);
		editor.putString(key, json);
		apply(editor);
	}
	public static ArrayList<ECGDataModel> getECGDataListResponse(String key) {
		Gson gson = new Gson();
		String json = App.myPref.getString(key, "");
		return gson.fromJson(json, new TypeToken<ArrayList<ECGDataModel>>() {
		}.getType());
	}
}
