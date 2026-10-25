package com.smartcollege.demo.service;

import com.smartcollege.demo.dto.UserDto;
import com.smartcollege.demo.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<UserDto> getAllUsers();
    UserDto getUserById(Long id);
    Optional<User> findEntityByEmail(String email);
    User getEntityById(Long id);
}
