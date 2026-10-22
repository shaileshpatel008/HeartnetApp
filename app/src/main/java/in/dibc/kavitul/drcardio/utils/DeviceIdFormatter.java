package in.dibc.kavitul.drcardio.utils;

public class DeviceIdFormatter {

    public static String formatDeviceId(String deviceId) {
        // Check if the input contains '/'
        if (deviceId == null || !deviceId.contains("/")) {
            return null; // Handle error appropriately
        }

        // Get the part after the slash
        String[] parts = deviceId.split("/");
        String suffix = parts[1]; // e.g., "241-22FC"

        // Remove non-alphanumeric characters (e.g., remove '-')
        suffix = suffix.replaceAll("[^A-Za-z0-9]", "");

        // Pad with leading zeros to make it 10 characters
        int totalLength = 10;
        if (suffix.length() < totalLength) {
            int paddingLength = totalLength - suffix.length();
            StringBuilder padded = new StringBuilder();
            for (int i = 0; i < paddingLength; i++) {
                padded.append('0');
            }
            padded.append(suffix);
            suffix = padded.toString();
        }

        return suffix;
    }
}
