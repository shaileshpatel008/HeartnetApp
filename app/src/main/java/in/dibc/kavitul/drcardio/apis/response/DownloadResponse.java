package in.dibc.kavitul.drcardio.apis.response;

public class DownloadResponse {
    private String Response;
    private boolean IsSuccess;
    private int UserID;

    public String getResponse() {
        return Response;
    }

    public boolean isSuccess() {
        return IsSuccess;
    }

    public int getUserID() {
        return UserID;
    }
}

