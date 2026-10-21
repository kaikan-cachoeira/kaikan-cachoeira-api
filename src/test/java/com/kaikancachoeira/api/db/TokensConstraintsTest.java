package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokensConstraintsTest extends DbIntegrationTest {

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    private Integer usuarioId;

    @BeforeEach
    void buscarUsuario() {
        usuarioId = jdbc.queryForObject("select id from usuario where email = 'admin@email.com'", Integer.class);
    }

    private int inserirTokenSenha(String token) {
        return jdbc.update("insert into token_senha (usuario_id, token, expira_em) values (?, ?, ?)",
                usuarioId, token, OffsetDateTime.now().plusHours(1));
    }

    private int inserirRefresh(String hash) {
        return jdbc.update("insert into refresh_token (usuario_id, token_hash, expira_em) values (?, ?, ?)",
                usuarioId, hash, OffsetDateTime.now().plusDays(7));
    }

    @Test
    void tokenSenhaDeveNascerNaoUsado() {
        inserirTokenSenha(HASH_A);

        assertThat(jdbc.queryForObject("select usado from token_senha where token = ?", Boolean.class, HASH_A))
                .isFalse();
    }

    @Test
    void tokenSenhaDeveSerUnico() {
        inserirTokenSenha(HASH_A);

        assertThatThrownBy(() -> inserirTokenSenha(HASH_A))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("token_senha_token_key");
    }

    @Test
    void tokenSenhaExigeUsuarioExistente() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into token_senha (usuario_id, token, expira_em) values (999999, ?, now())", HASH_A))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_token_senha_usuario");
    }

    @Test
    void refreshTokenDeveNascerNaoRevogado() {
        inserirRefresh(HASH_B);

        assertThat(jdbc.queryForObject("select revogado from refresh_token where token_hash = ?",
                Boolean.class, HASH_B)).isFalse();
    }

    @Test
    void refreshTokenHashDeveSerUnico() {
        inserirRefresh(HASH_B);

        assertThatThrownBy(() -> inserirRefresh(HASH_B))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("refresh_token_token_hash_key");
    }

    @Test
    void refreshTokenExigeUsuarioExistente() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into refresh_token (usuario_id, token_hash, expira_em) values (999999, ?, now())", HASH_B))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_refresh_token_usuario");
    }

    @Test
    void naoDeveExcluirUsuarioComTokensEmitidos() {
        inserirRefresh(HASH_B);

        assertThatThrownBy(() -> jdbc.update("delete from usuario where id = ?", usuarioId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}