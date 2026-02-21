package com.chaiyanan09.toothseg.dto;

public class MeResponse {
    public String fullName;
    public String email;

    public MeResponse(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }
}