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

CREATE TABLE IF NOT EXISTS saved_links (
	link_id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	user_id UUID NOT NULL,
	origin_url varchar(2048) NOT NULL,
	title varchar(5000) NULL,
	description varchar(5000) NULL,
	image_url varchar(2048) NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT saved_links_unique UNIQUE (origin_url, user_id),
	CONSTRAINT saved_links_users_fk FOREIGN KEY (user_id) REFERENCES users(user_id)
);

COMMENT ON TABLE saved_links IS 'Сохраненные ссылки';
COMMENT ON COLUMN saved_links.origin_url IS 'Ссылка';
COMMENT ON COLUMN saved_links.title IS 'Заголовок';
COMMENT ON COLUMN saved_links.description IS 'Описание';
COMMENT ON COLUMN saved_links.image_url IS 'Ссылка на картинку';
COMMENT ON COLUMN saved_links.created_at IS 'Дата и время добавления';
COMMENT ON COLUMN saved_links.user_id IS 'Пользователь';

CREATE INDEX IF NOT EXISTS saved_links_user_id_description_idx ON saved_links USING btree (user_id, description);
CREATE INDEX IF NOT EXISTS saved_links_user_id_idx ON saved_links USING btree (user_id);
CREATE INDEX IF NOT EXISTS saved_links_user_id_title_idx ON saved_links USING btree (user_id, title);

CREATE TABLE IF NOT EXISTS saved_link_tags (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	saved_link_link_id uuid NOT NULL,
	tags varchar NOT NULL,
	CONSTRAINT saved_link_tags_saved_links_fk FOREIGN KEY (saved_link_link_id) REFERENCES saved_links(link_id)
);
COMMENT ON TABLE saved_link_tags IS 'Теги';

CREATE INDEX IF NOT EXISTS saved_link_tags_tags_idx ON saved_link_tags (tags);
CREATE INDEX IF NOT EXISTS saved_link_tags_tags_idx ON saved_link_tags USING btree (tags, saved_link_link_id);