package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CargaInicialTest extends DbIntegrationTest {

    @Test
    void todasAsMigrationsDevemTerSidoAplicadasComSucesso() {
        assertThat(count("select count(*) from flyway_schema_history where success")).isEqualTo(29);
        assertThat(count("select count(*) from flyway_schema_history where not success")).isZero();
    }

    @Test
    void deveCarregarOsDoisCargos() {
        List<String> nomes = jdbc.queryForList("select nome from cargo order by nome", String.class);
        assertThat(nomes).containsExactly("ADMINISTRADOR", "OPERADOR");
    }

    @Test
    void deveCarregarAsDezPermissoes() {
        List<String> nomes = jdbc.queryForList("select nome from permissao", String.class);
        assertThat(nomes).containsExactlyInAnyOrder(
                "EVENTOS_MENU", "ESTOQUE_MENU", "DOACOES_MENU", "VENDAS_MENU", "CAIXA_MENU",
                "RELATORIOS_MENU", "DASHBOARD_MENU", "EVENTOS_LEITURA", "PRODUTOS_LEITURA",
                "CARDAPIO_EVENTO_LEITURA");
    }

    @Test
    void administradorDeveTerTodasAsPermissoes() {
        assertThat(count("select count(*) from cargo_permissao where cargo_id = ?",
                cargoId("ADMINISTRADOR"))).isEqualTo(10);
    }

    @Test
    void operadorDeveTerExatamenteAsCincoPermissoesDoPdv() {
        List<String> nomes = jdbc.queryForList("""
                select p.nome from cargo_permissao cp
                join permissao p on p.id = cp.permissao_id
                where cp.cargo_id = ?""", String.class, cargoId("OPERADOR"));

        assertThat(nomes).containsExactlyInAnyOrder(
                "VENDAS_MENU", "CAIXA_MENU", "EVENTOS_LEITURA", "PRODUTOS_LEITURA",
                "CARDAPIO_EVENTO_LEITURA");
    }

    @Test
    void operadorNaoDeveTerPermissoesDeGestao() {
        assertThat(count("""
                select count(*) from cargo_permissao cp
                join permissao p on p.id = cp.permissao_id
                where cp.cargo_id = ? and p.nome in
                  ('EVENTOS_MENU','ESTOQUE_MENU','DOACOES_MENU','RELATORIOS_MENU','DASHBOARD_MENU')""",
                cargoId("OPERADOR"))).isZero();
    }

    @Test
    void deveCarregarUmUsuarioPendentePorCargoComSenhaBcrypt() {
        List<String> linhas = jdbc.queryForList("""
                select u.email || '|' || c.nome || '|' || u.status || '|' || left(u.senha, 4)
                from usuario u join cargo c on c.id = u.cargo_id
                where u.email in ('admin@email.com', 'operador@email.com')
                order by u.email""", String.class);

        assertThat(linhas).containsExactly(
                "admin@email.com|ADMINISTRADOR|PENDENTE|$2a$",
                "operador@email.com|OPERADOR|PENDENTE|$2a$");
    }

    @Test
    void deveCarregarAsDuasCategoriasIniciais() {
        assertThat(jdbc.queryForList("select nome from categoria", String.class))
                .containsExactlyInAnyOrder("BEBIDA", "COMIDA");
    }

    @Test
    void deveCarregarOsTresStatusDeEventoComIdsNaOrdemEsperadaPelaRegraDoIndice() {
        // O índice uq_evento_em_andamento e o default de evento.status_id dependem destes ids.
        assertThat(jdbc.queryForObject("select codigo from status_evento where id = 1", String.class))
                .isEqualTo("NAO_INICIADO");
        assertThat(jdbc.queryForObject("select codigo from status_evento where id = 2", String.class))
                .isEqualTo("EM_ANDAMENTO");
        assertThat(jdbc.queryForObject("select codigo from status_evento where id = 3", String.class))
                .isEqualTo("ENCERRADO");

        assertThat(jdbc.queryForObject(
                "select descricao from status_evento where codigo = ?",
                String.class, "NAO_INICIADO"))
                .isEqualTo("Não iniciado");

        assertThat(jdbc.queryForObject(
                "select descricao from status_evento where codigo = ?",
                String.class, "EM_ANDAMENTO"))
                .isEqualTo("Em andamento");

        assertThat(jdbc.queryForObject(
                "select descricao from status_evento where codigo = ?",
                String.class, "ENCERRADO"))
                .isEqualTo("Encerrado");
    }

    @Test
    void deveCarregarOsOitoTiposDeMovimentacaoComDirecaoECustoCorretos() {
        List<String> linhas = jdbc.queryForList("""
                select codigo || '|' || direcao || '|' || exige_custo || '|' || gerado_pelo_sistema
                from tipo_movimentacao order by codigo""", String.class);

        assertThat(linhas).containsExactly(
                "AJUSTE_ENTRADA|ENTRADA|false|false",
                "AJUSTE_SAIDA|SAIDA|false|false",
                "CONSUMO_INTERNO|SAIDA|false|false",
                "DOACAO|ENTRADA|false|true",
                "ESTORNO_VENDA|ENTRADA|false|true",
                "PERDA|SAIDA|false|false",
                "REABASTECIMENTO|ENTRADA|true|false",
                "VENDA|SAIDA|false|true");
    }

    @Test
    void soReabastecimentoDeveExigirCusto() {
        assertThat(jdbc.queryForList("select codigo from tipo_movimentacao where exige_custo", String.class))
                .containsExactly("REABASTECIMENTO");
    }

    @Test
    void tipoGeradoPeloSistemaDeveSerCoerenteComOCodigo() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into tipo_movimentacao (codigo, direcao, gerado_pelo_sistema)
                values ('NOVO_TIPO', 'ENTRADA', true)"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_tipo_movimentacao_gerado_pelo_sistema");
    }
}