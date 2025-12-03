CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id BIGINT UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE users IS 'Пользователи';
COMMENT ON COLUMN users.user_id IS 'Уникальный идентификатор пользователя (UUID)';
COMMENT ON COLUMN users.chat_id IS 'Идентификатор пользователя в телеграмме';
COMMENT ON COLUMN users.username IS 'Имя пользователя (уникальное)';
COMMENT ON COLUMN users.created_at IS 'Дата и время создания записи';

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_unique ON users(username);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_chat_id_unique ON users(chat_id);
