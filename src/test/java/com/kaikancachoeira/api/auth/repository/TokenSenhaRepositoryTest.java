package com.kaikancachoeira.api.auth.repository;

import com.kaikancachoeira.api.auth.entity.TokenSenha;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static com.kaikancachoeira.api.support.TestData.tokenSenha;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenSenhaRepositoryTest extends DbIntegrationTest {

    private static final String HASH_1 = "1".repeat(64);
    private static final String HASH_2 = "2".repeat(64);
    private static final String HASH_3 = "3".repeat(64);

    @Autowired private TokenSenhaRepository repository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Usuario admin;
    private Usuario operador;

    @BeforeEach
    void setUp() {
        admin = usuarioRepository.findByEmail("admin@email.com").orElseThrow();
        operador = usuarioRepository.findByEmail("operador@email.com").orElseThrow();
    }

    @Test
    void findByTokenDeveEncontrarPeloHash() {
        repository.save(tokenSenha(admin, HASH_1, false, Instant.now().plusSeconds(60)));

        TokenSenha encontrado = repository.findByToken(HASH_1).orElseThrow();

        assertThat(encontrado.getUsuario().getId()).isEqualTo(admin.getId());
        assertThat(encontrado.getUsado()).isFalse();
        assertThat(encontrado.getCriadoEm()).isNotNull();
    }

    @Test
    void findByTokenDeveRetornarVazioQuandoNaoExiste() {
        assertThat(repository.findByToken(HASH_1)).isEmpty();
    }

    @Test
    void findByUsuarioAndUsadoFalseDeveRetornarSomenteNaoUsadosDoUsuario() {
        repository.save(tokenSenha(admin, HASH_1, false, Instant.now().plusSeconds(60)));
        repository.save(tokenSenha(admin, HASH_2, true, Instant.now().plusSeconds(60)));
        repository.save(tokenSenha(operador, HASH_3, false, Instant.now().plusSeconds(60)));

        assertThat(repository.findByUsuarioAndUsadoFalse(admin))
                .extracting(TokenSenha::getToken)
                .containsExactly(HASH_1);
    }

    @Test
    void findByUsuarioAndUsadoFalseDeveRetornarListaVaziaSemTokens() {
        assertThat(repository.findByUsuarioAndUsadoFalse(admin)).isEmpty();
    }

    @Test
    void tokenDuplicadoDeveViolarUnicidade() {
        repository.saveAndFlush(tokenSenha(admin, HASH_1, false, Instant.now().plusSeconds(60)));

        assertThatThrownBy(() -> repository.saveAndFlush(
                tokenSenha(operador, HASH_1, false, Instant.now().plusSeconds(60))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}