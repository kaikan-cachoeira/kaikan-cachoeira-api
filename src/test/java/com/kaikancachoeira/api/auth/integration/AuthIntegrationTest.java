package com.kaikancachoeira.api.auth.integration;

import com.jayway.jsonpath.JsonPath;
import com.kaikancachoeira.api.auth.entity.Cargo;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.enums.Status;
import com.kaikancachoeira.api.auth.repository.RefreshTokenRepository;
import com.kaikancachoeira.api.auth.repository.TokenSenhaRepository;
import com.kaikancachoeira.api.auth.repository.UsuarioRepository;
import com.kaikancachoeira.api.auth.security.TokenOpacoService;
import com.kaikancachoeira.api.email.dto.EmailMensagem;
import com.kaikancachoeira.api.support.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

abstract class AuthIntegrationTest extends IntegrationTestBase {

    protected static final String SENHA = "Senha@12345";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected RefreshTokenRepository refreshTokenRepository;
    @Autowired protected TokenSenhaRepository tokenSenhaRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected TokenOpacoService tokenOpacoService;
    @Autowired private EntityManager entityManager;

    private final List<Usuario> criados = new ArrayList<>();

    @AfterEach
    void limparDados() {
        // ordem importa por causa das FKs: tokens antes dos usuários
        refreshTokenRepository.deleteAll();
        tokenSenhaRepository.deleteAll();
        usuarioRepository.deleteAll(criados);
        criados.clear();
    }

    protected Usuario criarUsuario(Status status) {
        return criarUsuario(status, "ADMINISTRADOR");
    }

    protected Usuario criarUsuario(Status status, String nomeCargo) {
        Cargo cargo = entityManager
                .createQuery("select c from Cargo c where c.nome = :nome", Cargo.class)
                .setParameter("nome", nomeCargo)
                .getSingleResult();

        Usuario usuario = new Usuario();
        usuario.setNome("Usuario Teste");
        usuario.setEmail("user-" + UUID.randomUUID() + "@teste.com");
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setStatus(status);
        usuario.setCargo(cargo);

        Usuario salvo = usuarioRepository.save(usuario);
        criados.add(salvo);
        return salvo;
    }

    protected ResultActions postJson(String caminho, String corpo) throws Exception {
        return mockMvc.perform(post(caminho)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    protected ResultActions login(String email, String senha) throws Exception {
        return postJson("/auth/login", """
                {"email": "%s", "senha": "%s"}""".formatted(email, senha));
    }

    protected String refreshTokenDoLogin(Usuario usuario) throws Exception {
        String corpo = login(usuario.getEmail(), SENHA)
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.refreshToken");
    }

    protected ResultActions refresh(String refreshToken) throws Exception {
        return postJson("/auth/refresh", """
                {"refreshToken": "%s"}""".formatted(refreshToken));
    }

    protected ResultActions recuperarSenha(String email) throws Exception {
        return postJson("/auth/recuperacao-senha", """
                {"email": "%s"}""".formatted(email));
    }

    protected ResultActions redefinirSenha(String token, String novaSenha) throws Exception {
        return postJson("/auth/redefinicao-senha", """
                {"token": "%s", "novaSenha": "%s"}""".formatted(token, novaSenha));
    }

    /** Extrai o token puro do link enviado por e-mail (verifica que houve exatamente um envio). */
    protected String tokenDoEmailEnviado() {
        ArgumentCaptor<EmailMensagem> captor = ArgumentCaptor.forClass(EmailMensagem.class);
        verify(emailService).enviar(captor.capture());

        Matcher matcher = Pattern.compile("token=([0-9a-f]{64})")
                .matcher(captor.getValue().corpoHtml());
        assertThat(matcher.find()).as("link com token no corpo do e-mail").isTrue();
        return matcher.group(1);
    }

    protected Usuario recarregar(Usuario usuario) {
        return usuarioRepository.findById(usuario.getId()).orElseThrow();
    }
}