CREATE TABLE token_email (
                             id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             usuario_id  INT                      NOT NULL,
                             token       VARCHAR(255)             NOT NULL UNIQUE,
                             usado       BOOLEAN                  NOT NULL DEFAULT FALSE,
                             expira_em   TIMESTAMP WITH TIME ZONE NOT NULL,
                             novo_email  VARCHAR(255)             NOT NULL,
                             CONSTRAINT fk_token_email_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);