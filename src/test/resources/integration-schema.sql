CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    google_subject VARCHAR(255) UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    picture_url VARCHAR(1000),
    username VARCHAR(50) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE visual_items (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(120),
    image_url VARCHAR(2000) NOT NULL,
    image_path VARCHAR(2000),
    width INTEGER,
    height INTEGER,
    aspect_ratio NUMERIC(10, 4),
    file_size BIGINT,
    mime_type VARCHAR(120),
    description VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    moderation_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    share_key VARCHAR(36) UNIQUE
);

CREATE TABLE user_follows (
    follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, following_id)
);

CREATE TABLE saved_pins (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    visual_item_id BIGINT NOT NULL REFERENCES visual_items(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, visual_item_id)
);

CREATE TABLE pin_likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    visual_item_id BIGINT NOT NULL REFERENCES visual_items(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pin_likes_user_pin UNIQUE (user_id, visual_item_id)
);

CREATE TABLE user_interactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    visual_item_id BIGINT NOT NULL REFERENCES visual_items(id) ON DELETE CASCADE,
    interaction_type VARCHAR(30) NOT NULL,
    duration_ms BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_interests (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(120) NOT NULL,
    score DOUBLE PRECISION NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, category)
);

CREATE TABLE app_settings (
    setting_key VARCHAR(120) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    setting_value VARCHAR(120),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE admin_activity_log (
    id BIGSERIAL PRIMARY KEY,
    admin_name VARCHAR(160) NOT NULL,
    action VARCHAR(120) NOT NULL,
    target VARCHAR(240),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    visual_item_id BIGINT REFERENCES visual_items(id) ON DELETE CASCADE,
    reported_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    target_type VARCHAR(20) NOT NULL DEFAULT 'PIN',
    target_id BIGINT,
    reporter_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reason VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,
    reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_visual_items_feed ON visual_items (moderation_status, id DESC);
CREATE INDEX idx_visual_items_creator ON visual_items (uploaded_by, id DESC);
CREATE INDEX idx_pin_likes_user ON pin_likes (user_id);
CREATE INDEX idx_pin_likes_pin ON pin_likes (visual_item_id);
CREATE INDEX idx_saved_pins_user_created ON saved_pins (user_id, created_at DESC);
CREATE INDEX idx_follows_following_created ON user_follows (following_id, created_at DESC);
CREATE INDEX idx_follows_follower_created ON user_follows (follower_id, created_at DESC);
CREATE INDEX idx_user_interactions_pin ON user_interactions (visual_item_id);
CREATE INDEX idx_user_interactions_user_pin_type ON user_interactions (user_id, visual_item_id, interaction_type);
CREATE INDEX idx_reports_status_created ON reports (status, created_at DESC);
CREATE INDEX idx_reports_duplicate_check ON reports (reporter_id, target_type, target_id, status);

INSERT INTO roles (code) VALUES ('USER'), ('ADMIN');
INSERT INTO app_settings (setting_key, enabled, setting_value) VALUES
    ('recommendations_enabled', TRUE, NULL),
    ('registration_enabled', TRUE, NULL),
    ('image_uploads_enabled', TRUE, NULL),
    ('public_profiles_enabled', TRUE, NULL),
    ('session_duration_days', TRUE, '30');
