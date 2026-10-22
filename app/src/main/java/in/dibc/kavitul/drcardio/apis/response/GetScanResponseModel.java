package in.dibc.kavitul.drcardio.apis.response;

public class GetScanResponseModel {

    private DownloadResponseData Response;
    private boolean IsSuccess;
    private int UserID;

    public DownloadResponseData getResponse() {
        return Response;
    }

    public boolean isSuccess() {
        return IsSuccess;
    }

    public int getUserID() {
        return UserID;
    }
}
