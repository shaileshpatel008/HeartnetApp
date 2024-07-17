package in.dibc.kavitul.drcardio.Model;

import com.google.gson.Gson;

import java.io.Serializable;

/**
 * Created by rushil on 14/12/16.
 */

public class BaseModel implements Serializable {
    public static Gson GSON = new Gson();

    /**
     * Convert Model class to JsonObject
     */
    public static String toJson(Object modelClass) {
        try {

            if (GSON == null)
                GSON = new Gson();

            return GSON.toJson(modelClass);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Convert JsonObject to Model class
     */
    public static Object toModelClass(String strJson, Class<?> modelClass) {
        try {
            if (GSON == null)
                GSON = new Gson();

            if (strJson != null && strJson.length() > 0)
                return GSON.fromJson(strJson, modelClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Convert Model class to JsonObject
     */
    public String toJson() {
        try {

            if (GSON == null)
                GSON = new Gson();

            return GSON.toJson(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
