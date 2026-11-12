package com.example.unipathapi.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleTokenPayload {
    private String sub;
    private String email;
    private String name;
    private String picture;
    private boolean emailVerified;
}

