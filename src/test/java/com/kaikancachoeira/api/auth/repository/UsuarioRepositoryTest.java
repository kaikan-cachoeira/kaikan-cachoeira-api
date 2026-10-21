package com.kaikancachoeira.api.auth.repository;

import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.enums.Status;
import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioRepositoryTest extends DbIntegrationTest {

    @Autowired private UsuarioRepository repository;

    @Test
    void findByEmailDeveCarregarUsuarioComCargoEPermissoes() {
        Usuario admin = repository.findByEmail("admin@email.com").orElseThrow();

        assertThat(admin.getStatus()).isEqualTo(Status.PENDENTE);
        assertThat(admin.getCargo().getNome()).isEqualTo("ADMINISTRADOR");
        assertThat(admin.getCargo().getPermissoes()).hasSize(10);
    }

    @Test
    void operadorDeveCarregarSomenteSuasPermissoes() {
        Usuario operador = repository.findByEmail("operador@email.com").orElseThrow();

        assertThat(operador.getCargo().getPermissoes())
                .extracting("nome")
                .containsExactlyInAnyOrder("VENDAS_MENU", "CAIXA_MENU", "EVENTOS_LEITURA",
                        "PRODUTOS_LEITURA", "CARDAPIO_EVENTO_LEITURA");
    }

    @Test
    void findByEmailDeveRetornarVazioQuandoNaoExiste() {
        assertThat(repository.findByEmail("naoexiste@email.com")).isEmpty();
    }

    /** Documenta: a busca por e-mail diferencia maiúsculas de minúsculas. */
    @Test
    void findByEmailEhCaseSensitive() {
        assertThat(repository.findByEmail("ADMIN@EMAIL.COM")).isEmpty();
    }

    @Test
    void findByEmailAndStatusDeveFiltrarPeloStatus() {
        assertThat(repository.findByEmailAndStatus("admin@email.com", Status.PENDENTE)).isPresent();
        assertThat(repository.findByEmailAndStatus("admin@email.com", Status.ATIVO)).isEmpty();
        assertThat(repository.findByEmailAndStatus("admin@email.com", Status.DESATIVADO)).isEmpty();
    }

    @Test
    void findByEmailAndStatusDeveEnxergarMudancaDeStatus() {
        jdbc.update("update usuario set status = 'ATIVO' where email = 'admin@email.com'");

        assertThat(repository.findByEmailAndStatus("admin@email.com", Status.ATIVO)).isPresent();
        assertThat(repository.findByEmailAndStatus("admin@email.com", Status.PENDENTE)).isEmpty();
    }

    @Test
    void saveDevePreencherTimestampsEStatusPadrao() {
        Usuario base = repository.findByEmail("admin@email.com").orElseThrow();
        Usuario novo = new Usuario();
        novo.setNome("Novo");
        novo.setEmail("novo@teste.com");
        novo.setSenha("hash");
        novo.setCargo(base.getCargo());

        Usuario salvo = repository.saveAndFlush(novo);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getStatus()).isEqualTo(Status.PENDENTE);
        assertThat(salvo.getCriadoEm()).isNotNull();
        assertThat(salvo.getAtualizadoEm()).isNotNull();
    }
}