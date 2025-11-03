package com.hip.damoa.domain.user.web.dto;

import com.hip.damoa.domain.user.model.User;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.Serializable;

@Getter
public class UserSignupRequest implements Serializable {

    private String email;
    private String password;
    private String name; // 예시 필드

    public User toEntity(PasswordEncoder passwordEncoder) {
        return User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();
    }
}