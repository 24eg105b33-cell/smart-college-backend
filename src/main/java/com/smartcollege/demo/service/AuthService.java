package com.smartcollege.demo.service;

import com.smartcollege.demo.dto.AuthResponse;
import com.smartcollege.demo.dto.LoginRequest;
import com.smartcollege.demo.dto.RegisterRequest;
import com.smartcollege.demo.dto.UserDto;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserDto getCurrentUserProfile(String email);
}
