package com.template.service;

import com.template.entity.User;
import java.util.Set;

public interface UserService {
    User assignRoles(Long userId, Set<String> roleNames);
    User removeRoles(Long userId, Set<String> roleNames);
    void onLoginSuccess(User user);
    void onLoginFailure(User user, int maxAttempts, int lockDurationMinutes);
    boolean userExists(String username);
    boolean updatePassword(String username, String newPassword);
    boolean isAccountExpired(String username);
    boolean activateAccount(String username);
}

