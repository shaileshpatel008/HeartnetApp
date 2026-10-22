package in.dibc.kavitul.drcardio.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateConverter {
    public static String convertDateFormat(String inputDate) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try {
            Date date = inputFormat.parse(inputDate); // Parse input string to Date
            return outputFormat.format(date); // Convert to desired format
        } catch (ParseException e) {
            e.printStackTrace();
            return null; // Return null in case of error
        }
    }

    public static void main(String[] args) {
        String convertedDate = convertDateFormat("19/02/2025");
        System.out.println("Converted Date: " + convertedDate);
    }

    public static String formatDate(String inputDate) {
        // Define input format (API response format)
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

        // Define output format (desired format)
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());

        try {
            // Parse the input date string
            Date date = inputFormat.parse(inputDate);

            // Convert it into the desired format
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "Invalid Date"; // Return a default message in case of error
        }
    }

    public static String formatDateTime(String inputDate) {
        try {
            // Input format
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

            // Output format
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, yyyy hh:mm a", Locale.getDefault());

            Date date = inputFormat.parse(inputDate);
            return outputFormat.format(date);

        } catch (ParseException e) {
            e.printStackTrace();
            return inputDate; // fallback if parsing fails
        }
    }

    public static String getDobFromAge(int age) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -age);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }
}

