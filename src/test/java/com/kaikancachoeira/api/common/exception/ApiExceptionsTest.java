package com.kaikancachoeira.api.common.exception;

import com.kaikancachoeira.api.auth.exception.CredenciaisInvalidasException;
import com.kaikancachoeira.api.auth.exception.RefreshTokenInvalidoOuExpiradoException;
import com.kaikancachoeira.api.auth.exception.TokenSenhaInvalidoOuExpiradoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiExceptionsTest {

    @Test
    void erroResponseDeDeveTerListaDeCamposVaziaEImutavel() {
        ErroResponse erro = ErroResponse.de("COD", "msg");

        assertThat(erro.codigo()).isEqualTo("COD");
        assertThat(erro.mensagem()).isEqualTo("msg");
        assertThat(erro.campos()).isEmpty();
        assertThatThrownBy(() -> erro.campos().add(new CampoErro("a", "b")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void erroResponseDevePreservarOsCamposInformados() {
        List<CampoErro> campos = List.of(new CampoErro("email", "inválido"));

        assertThat(new ErroResponse("VALIDACAO", "Dados inválidos", campos).campos())
                .containsExactlyElementsOf(campos);
    }

    @Test
    void conflitoDeveSer409() {
        ConflitoException ex = new ConflitoException("DUP", "duplicado");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getCodigo()).isEqualTo("DUP");
        assertThat(ex.getMessage()).isEqualTo("duplicado");
    }

    @Test
    void requisicaoInvalidaDeveSer400() {
        RequisicaoInvalidaException ex = new RequisicaoInvalidaException("RUIM", "ruim");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCodigo()).isEqualTo("RUIM");
    }

    @Test
    void recursoNaoEncontradoDeveSer404() {
        RecursoNaoEncontradoException ex = new RecursoNaoEncontradoException("X", "sumiu");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCodigo()).isEqualTo("X");
    }

    @Test
    void recursoNaoEncontradoDeDeveUsarCodigoPadraoEMensagemComId() {
        RecursoNaoEncontradoException ex = RecursoNaoEncontradoException.de("Produto", 5);

        assertThat(ex.getCodigo()).isEqualTo("RECURSO_NAO_ENCONTRADO");
        assertThat(ex.getMessage()).isEqualTo("Produto não encontrado: 5");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void excecoesDeAuthDevemTerStatusCodigoEMensagemDoContrato() {
        CredenciaisInvalidasException credenciais = new CredenciaisInvalidasException();
        assertThat(credenciais.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(credenciais.getCodigo()).isEqualTo("CREDENCIAIS_INVALIDAS");
        assertThat(credenciais.getMessage()).isEqualTo("Usuário ou senha inválidos");

        RefreshTokenInvalidoOuExpiradoException refresh = new RefreshTokenInvalidoOuExpiradoException();
        assertThat(refresh.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh.getCodigo()).isEqualTo("REFRESH_TOKEN_INVALIDO_OU_EXPIRADO");

        TokenSenhaInvalidoOuExpiradoException tokenSenha = new TokenSenhaInvalidoOuExpiradoException();
        assertThat(tokenSenha.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tokenSenha.getCodigo()).isEqualTo("TOKEN_INVALIDO");
    }
}