CREATE TABLE token_email (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    expira_em TIMESTAMP NOT NULL,
    novo_email VARCHAR(255) NOT NULL,
    CONSTRAINT fk_token_email_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);