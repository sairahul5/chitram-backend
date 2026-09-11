package com.chitram.user.service;

import com.chitram.user.entity.UserAccount;
import com.chitram.user.repository.UserAccountRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final UserAccountRepository userAccountRepository;

    public UserProfileService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount getCurrentUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Signed-in user was not found"));
    }
}
