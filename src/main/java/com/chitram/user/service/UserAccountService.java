package com.chitram.user.service;

import com.chitram.user.entity.UserAccount;
import com.chitram.user.repository.UserAccountRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final JdbcTemplate jdbcTemplate;

    public UserAccountService(UserAccountRepository userAccountRepository, JdbcTemplate jdbcTemplate) {
        this.userAccountRepository = userAccountRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public UserAccount upsertGoogleUser(OAuth2User oAuth2User) {
        String googleSubject = requiredAttribute(oAuth2User, "sub");
        String email = requiredAttribute(oAuth2User, "email");
        String displayName = optionalAttribute(oAuth2User, "name", email);
        String pictureUrl = optionalAttribute(oAuth2User, "picture", null);

        UserAccount userAccount = userAccountRepository.findByGoogleSubject(googleSubject)
                .or(() -> userAccountRepository.findByEmail(email))
                .orElseGet(() -> new UserAccount(googleSubject, email, displayName, pictureUrl));

        userAccount.linkGoogleSubject(googleSubject);
        userAccount.updateProfile(email, displayName, pictureUrl);

        if (userAccount.getUsername() == null || userAccount.getUsername().isBlank()) {
            userAccount.setUsername(generateSuggestedUsername(displayName));
        }

        UserAccount savedUser = userAccountRepository.save(userAccount);
        ensureUserRole(savedUser.getId());
        return savedUser;
    }

    public String generateSuggestedUsername(String name) {
        String base = name != null ? name.trim().toLowerCase().replaceAll("[^a-z0-9]", "") : "user";
        if (base.isBlank()) {
            base = "user";
        }
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }

        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        String candidate = base + (1000 + random.nextInt(9000));
        int attempts = 0;
        while (userAccountRepository.existsByUsername(candidate) && attempts < 20) {
            candidate = base + (1000 + random.nextInt(9000));
            attempts++;
        }
        if (userAccountRepository.existsByUsername(candidate)) {
            candidate = base + (System.currentTimeMillis() % 100000);
        }
        return candidate;
    }

    @Transactional
    public UserAccount updateProfile(Long userId, String displayName, String username) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (displayName == null || displayName.trim().isBlank()) {
            throw new IllegalArgumentException("Display name cannot be empty");
        }
        String cleanDisplayName = displayName.trim();
        if (cleanDisplayName.length() > 50) {
            throw new IllegalArgumentException("Display name is too long (maximum 50 characters)");
        }

        if (username == null || username.trim().isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        String cleanUsername = username.trim().toLowerCase();
        if (!cleanUsername.matches("^[a-z0-9_]{3,30}$")) {
            throw new IllegalArgumentException("Username must be 3-30 characters and contain only letters, numbers, and underscores");
        }

        if (userAccountRepository.existsByUsernameAndIdNot(cleanUsername, userId)) {
            throw new IllegalArgumentException("Username is already taken. Please choose another one.");
        }

        user.updateDisplayNameAndUsername(cleanDisplayName, cleanUsername);
        return userAccountRepository.save(user);
    }

    public boolean isUsernameAvailable(String username, Long currentUserId) {
        if (username == null || username.trim().isBlank()) {
            return false;
        }
        String clean = username.trim().toLowerCase();
        if (!clean.matches("^[a-z0-9_]{3,30}$")) {
            return false;
        }
        if (currentUserId != null) {
            return !userAccountRepository.existsByUsernameAndIdNot(clean, currentUserId);
        }
        return !userAccountRepository.existsByUsername(clean);
    }

    public boolean isAdmin(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id WHERE u.email = ? AND r.code = 'ADMIN'",
                Integer.class,
                email);
        return count != null && count > 0;
    }

    private void ensureUserRole(Long userId) {
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE code = 'USER' ON CONFLICT DO NOTHING",
                userId);
    }

    private String requiredAttribute(OAuth2User oAuth2User, String name) {
        String value = oAuth2User.getAttribute(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Google OAuth response is missing " + name);
        }
        return value;
    }

    private String optionalAttribute(OAuth2User oAuth2User, String name, String fallback) {
        String value = oAuth2User.getAttribute(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
