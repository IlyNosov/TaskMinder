CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       telegram_id BIGINT UNIQUE NOT NULL,
                       timezone VARCHAR(50),
                       created_at TIMESTAMP NOT NULL
);

CREATE TABLE reminders (
                           id UUID PRIMARY KEY,
                           user_id UUID NOT NULL,
                           text TEXT NOT NULL,
                           scheduled_at TIMESTAMP NOT NULL,
                           status VARCHAR(20) NOT NULL,
                           created_at TIMESTAMP NOT NULL,
                           CONSTRAINT fk_user
                               FOREIGN KEY (user_id)
                                   REFERENCES users(id)
);