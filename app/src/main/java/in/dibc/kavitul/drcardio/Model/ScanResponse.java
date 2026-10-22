package in.dibc.kavitul.drcardio.Model;

import com.google.gson.annotations.SerializedName;

public class ScanResponse {

    @SerializedName("Response")
    private String response;

    @SerializedName("Pagination")
    private Pagination pagination;   // ✅ NEW

    @SerializedName("IsSuccess")
    private boolean isSuccess;

    @SerializedName("UserID")
    private int userID;

    // 🔹 Getters
    public String getResponse() {
        return response;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public int getUserID() {
        return userID;
    }

    // ✅ INNER CLASS (Pagination)
    public static class Pagination {

        @SerializedName("totalCount")
        private int totalCount;

        @SerializedName("totalPages")
        private int totalPages;

        @SerializedName("currentPage")
        private int currentPage;

        @SerializedName("pageSize")
        private int pageSize;

        @SerializedName("hasNextPage")
        private boolean hasNextPage;

        @SerializedName("hasPreviousPage")
        private boolean hasPreviousPage;

        // 🔹 Getters
        public int getTotalCount() {
            return totalCount;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public int getPageSize() {
            return pageSize;
        }

        public boolean isHasNextPage() {
            return hasNextPage;
        }

        public boolean isHasPreviousPage() {
            return hasPreviousPage;
        }
    }
}