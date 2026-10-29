CREATE TABLE refresh_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  token VARCHAR(500) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  expiracion TIMESTAMP NOT NULL,
  revocado BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_revocado ON refresh_tokens(revocado);
CREATE INDEX idx_refresh_tokens_expiracion ON refresh_tokens(expiracion);
