package com.kaikancachoeira.api.auth.repository;

import com.kaikancachoeira.api.auth.entity.RefreshToken;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static com.kaikancachoeira.api.support.TestData.refreshToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenRepositoryTest extends DbIntegrationTest {

    private static final String HASH_1 = "a".repeat(64);
    private static final String HASH_2 = "b".repeat(64);
    private static final String HASH_3 = "c".repeat(64);

    @Autowired private RefreshTokenRepository repository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Usuario admin;
    private Usuario operador;

    @BeforeEach
    void setUp() {
        admin = usuarioRepository.findByEmail("admin@email.com").orElseThrow();
        operador = usuarioRepository.findByEmail("operador@email.com").orElseThrow();
    }

    @Test
    void findByTokenHashDeveEncontrarDentroDeTransacaoComLockPessimista() {
        // O @Lock(PESSIMISTIC_WRITE) exige transação ativa; esta classe é @Transactional.
        repository.save(refreshToken(admin, HASH_1, false, Instant.now().plusSeconds(60)));

        RefreshToken encontrado = repository.findByTokenHash(HASH_1).orElseThrow();

        assertThat(encontrado.getUsuario().getId()).isEqualTo(admin.getId());
        assertThat(encontrado.getRevogado()).isFalse();
        assertThat(encontrado.getCriadoEm()).isNotNull();
    }

    @Test
    void findByTokenHashDeveRetornarVazioQuandoNaoExiste() {
        assertThat(repository.findByTokenHash(HASH_1)).isEmpty();
    }

    @Test
    void findByTokenHashDeveEncontrarTokenJaRevogado() {
        // O filtro de revogação/expiração é responsabilidade do service, não da query.
        repository.save(refreshToken(admin, HASH_1, true, Instant.now().minusSeconds(60)));

        assertThat(repository.findByTokenHash(HASH_1)).isPresent();
    }

    @Test
    void findByUsuarioAndRevogadoFalseDeveRetornarSomenteAtivosDoUsuario() {
        repository.save(refreshToken(admin, HASH_1, false, Instant.now().plusSeconds(60)));
        repository.save(refreshToken(admin, HASH_2, true, Instant.now().plusSeconds(60)));
        repository.save(refreshToken(operador, HASH_3, false, Instant.now().plusSeconds(60)));

        assertThat(repository.findByUsuarioAndRevogadoFalse(admin))
                .extracting(RefreshToken::getTokenHash)
                .containsExactly(HASH_1);
    }

    @Test
    void revogarDeveSumirDaListaDeAtivos() {
        RefreshToken token = repository.save(
                refreshToken(admin, HASH_1, false, Instant.now().plusSeconds(60)));

        token.setRevogado(true);
        repository.saveAndFlush(token);

        assertThat(repository.findByUsuarioAndRevogadoFalse(admin)).isEmpty();
    }

    @Test
    void hashDuplicadoDeveViolarUnicidade() {
        repository.saveAndFlush(refreshToken(admin, HASH_1, false, Instant.now().plusSeconds(60)));

        assertThatThrownBy(() -> repository.saveAndFlush(
                refreshToken(operador, HASH_1, false, Instant.now().plusSeconds(60))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}