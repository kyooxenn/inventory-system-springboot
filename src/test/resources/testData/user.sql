INSERT INTO users (username, password, email, mobile, is_verified, roles, telegram_chat_id)
VALUES
    ('john_doe', 'encoded_password_123', 'john@example.com', '1234567890', false, 'ROLE_USER', NULL),
    ('jane_smith', 'encoded_password_456', 'jane@example.com', '0987654321', true, 'ROLE_USER,ROLE_ADMIN', '123456789'),
    ('admin_user', 'encoded_admin_pass', 'admin@example.com', '5551234567', true, 'ROLE_ADMIN', NULL),
    ('test_user', 'test_pass_789', 'test@example.com', '9998887777', false, 'ROLE_USER', 'telegram_123');