package com.chitram.user.service;

import com.chitram.user.entity.UserAccount;
import com.chitram.user.repository.UserAccountRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class UserProfileService {

    private final UserAccountRepository userAccountRepository;

    public UserProfileService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount getCurrentUser(OAuth2User principal) {
        if (principal == null || principal.getAttribute("email") == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }

        String email = principal.getAttribute("email");
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Signed-in user was not found"));
    }
}
