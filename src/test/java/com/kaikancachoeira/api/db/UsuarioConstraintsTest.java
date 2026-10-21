package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioConstraintsTest extends DbIntegrationTest {

    private static final String INSERT = """
            insert into usuario (cargo_id, nome, email, senha, status)
            values (?, 'Fulano', ?, 'hash', ?)""";

    private int hojeIdentidade() {
        return cargoId("OPERADOR");
    }

    @Test
    void statusPadraoDeveSerPendente() {
        jdbc.update("insert into usuario (cargo_id, nome, email, senha) values (?, 'F', 'padrao@t.com', 'h')",
                hojeIdentidade());

        assertThat(jdbc.queryForObject("select status from usuario where email = 'padrao@t.com'", String.class))
                .isEqualTo("PENDENTE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATIVO", "PENDENTE", "DESATIVADO"})
    void deveAceitarOsTresStatus(String status) {
        int linhas = jdbc.update(INSERT, hojeIdentidade(), "ok-" + status + "@t.com", status);

        assertThat(linhas).isEqualTo(1);
    }

    /** Documenta: o CHECK é case-sensitive; a história H1 fala em 'ativo'/'pendente' minúsculos. */
    @ParameterizedTest
    @ValueSource(strings = {"ativo", "pendente", "BLOQUEADO", ""})
    void deveRejeitarStatusForaDaLista(String status) {
        assertThatThrownBy(() -> jdbc.update(INSERT, hojeIdentidade(), "ruim@t.com", status))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_usuario_status");
    }

    @Test
    void emailDeveSerUnico() {
        assertThatThrownBy(() -> jdbc.update(INSERT, hojeIdentidade(), "admin@email.com", "ATIVO"))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("usuario_email_key");
    }

    /** Documenta: a unicidade não ignora maiúsculas; 'ADMIN@email.com' é aceito como outro usuário. */
    @Test
    void emailComCaixaDiferenteHojeEhAceitoComoOutroUsuario() {
        int linhas = jdbc.update(INSERT, hojeIdentidade(), "ADMIN@EMAIL.COM", "ATIVO");

        assertThat(linhas).isEqualTo(1);
    }

    @Test
    void cargoInexistenteDeveSerRejeitado() {
        assertThatThrownBy(() -> jdbc.update(INSERT, 999_999, "x@t.com", "ATIVO"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_usuario_cargo");
    }

    @Test
    void naoDeveExcluirCargoComUsuarios() {
        assertThatThrownBy(() -> jdbc.update("delete from cargo where nome = 'ADMINISTRADOR'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nomeEEmailESenhaSaoObrigatorios() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into usuario (cargo_id, nome, email, senha) values (?, null, 'n@t.com', 'h')",
                hojeIdentidade()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}