package com.example.unipathapi.dto.response;

public class AuthResponse {
    private String token;
    private String userID;
    private String role;
    private String message;

    public AuthResponse(String token, String userID, String role, String message) {
        this.token = token;
        this.userID = userID;
        this.role = role;
        this.message = message;
    }

    public String getToken() {return token;}
    public void setToken(String token) {this.token = token;}
    public String getUserID() {return userID;}
    public void setUserID(String userID) {this.userID = userID;}
    public String getRole() {return role;}
    public void setRole(String role) {this.role = role;}
    public String getMessage() {return message;}
    public void setMessage(String message) {this.message = message;}

}
