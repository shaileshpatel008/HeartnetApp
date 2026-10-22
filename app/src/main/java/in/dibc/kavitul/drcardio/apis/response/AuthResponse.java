package in.dibc.kavitul.drcardio.apis.response;

public class AuthResponse {
    private String Response;
    private boolean IsSuccess;
    private int UserID;

    public String getToken() {
        return Response;  // JWT token
    }

    public boolean isSuccess() {
        return IsSuccess;
    }

    public int getUserID() {
        return UserID;
    }
}

