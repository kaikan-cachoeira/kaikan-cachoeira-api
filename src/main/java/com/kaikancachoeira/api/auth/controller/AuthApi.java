package com.kaikancachoeira.api.auth.controller;

import com.kaikancachoeira.api.auth.dto.*;
import com.kaikancachoeira.api.common.exception.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Autenticação",
        description = "Login, renovação de sessão e recuperação de senha. Rotas públicas, sem JWT.")
@SecurityRequirements
public interface AuthApi {

    @Operation(
            summary = "Autentica o usuário",
            description = """
            Valida e-mail e senha e devolve um access token (JWT de curta duração) e um refresh token.

            **Regras:**
            - A conta precisa estar `ATIVO`. Contas `PENDENTE` (primeira senha ainda não definida) \
            e `DESATIVADO` recebem a mesma resposta de credenciais inválidas.
            - O 401 é idêntico para e-mail inexistente, senha errada ou conta não ativa, \
            para não revelar quais e-mails estão cadastrados.
            - Só o hash do refresh token é persistido; o valor bruto é devolvido apenas aqui.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(value = """
                    {"codigo": "CREDENCIAIS_INVALIDAS", "mensagem": "Usuário ou senha inválidos", "campos": []}
                    """)))
    })
    ResponseEntity<LoginResponse> login(LoginRequest request);

    @Operation(
            summary = "Renova o par de tokens",
            description = """
            Troca um refresh token válido por um novo par (access + refresh).

            **Regras:**
            - Rotação: o refresh token usado é revogado e um novo é emitido. Reutilizar o antigo resulta em 401.
            - O refresh token é inválido se não existir, estiver revogado ou expirado.
            - Chamadas concorrentes com o mesmo token: apenas uma tem sucesso.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Novo par de tokens emitido"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido, revogado ou expirado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class), examples = @ExampleObject(name = "Refresh token inválido", value = """
                        {"codigo": "TOKEN_INVALIDO","mensagem": "Refresh token inválido, revogado ou expirado","campos": []}
                        """)))
    })
    ResponseEntity<RefreshTokenResponse> refresh(RefreshTokenRequest request);

    @Operation(
            summary = "Encerra a sessão",
            description = "Revoga o refresh token informado. O access token continua válido até expirar, "
                    + "então o front deve descartá-lo.")
    @ApiResponse(responseCode = "204", description = "Refresh token revogado")
    ResponseEntity<Void> logout(LogoutRequest request);

    @Operation(
            summary = "Solicita recuperação de senha",
            description = """
            Envia por e-mail um link `{url-do-front}/redefinicao-senha?token={token}`, válido por 1 hora.

            **Regras:**
            - Sempre retorna 202, exista o e-mail ou não, para não revelar quais e-mails estão cadastrados.
            - Contas `DESATIVADO` não recebem o e-mail.
            - Tokens anteriores ainda não usados do mesmo usuário são invalidados.
            - Este fluxo também define a primeira senha de contas `PENDENTE`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Requisição aceita (independe de o e-mail existir)"),
            @ApiResponse(responseCode = "400", description = "Formato de e-mail inválido",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(name = "Dados inválidos", value = """
                                    {"codigo": "VALIDACAO","mensagem": "Dados inválidos","campos": [{"campo": "email","mensagem": "deve ser um endereço de e-mail bem formado"}]}
                                    """)))
    })
    ResponseEntity<Void> recuperarSenha(RecuperacaoSenhaRequest request);

    @Operation(
            summary = "Redefine a senha a partir do token do e-mail",
            description = """
            Define a nova senha usando o token recebido por e-mail.

            **Efeitos:**
            - Marca o token como usado.
            - Revoga todos os refresh tokens do usuário.
            - Se a conta estava `PENDENTE`, passa para `ATIVO`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha redefinida"),
            @ApiResponse(responseCode = "400", description = "Token inválido, já usado ou expirado, ou dados inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErroResponse.class),
                            examples = {
                                    @ExampleObject(name = "Token inválido", value = """
                        {"codigo": "TOKEN_INVALIDO", "mensagem": "Token inválido ou expirado", "campos": []}
                        """),
                                    @ExampleObject(name = "Senha curta", value = """
                        {"codigo": "VALIDACAO", "mensagem": "Dados inválidos",
                         "campos": [{"campo": "novaSenha", "mensagem": "tamanho deve estar entre 8 e 255"}]}
                        """)
                            }))
    })
    ResponseEntity<Void> redefinirSenha(RedefinicaoSenhaRequest request);
}