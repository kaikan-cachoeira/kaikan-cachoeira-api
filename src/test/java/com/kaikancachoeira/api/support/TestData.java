package com.kaikancachoeira.api.support;

import com.kaikancachoeira.api.auth.entity.Cargo;
import com.kaikancachoeira.api.auth.entity.Permissao;
import com.kaikancachoeira.api.auth.entity.RefreshToken;
import com.kaikancachoeira.api.auth.entity.TokenSenha;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.enums.Status;

import java.time.Instant;

public final class TestData {

    private TestData() {}

    public static Permissao permissao(String nome) {
        Permissao permissao = new Permissao();
        permissao.setNome(nome);
        return permissao;
    }

    public static Cargo cargo(String nome, String... permissoes) {
        Cargo cargo = new Cargo();
        cargo.setNome(nome);
        for (String permissao : permissoes) {
            cargo.getPermissoes().add(permissao(permissao));
        }
        return cargo;
    }

    public static Usuario usuario(Integer id, String email, Status status, Cargo cargo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Fulano");
        usuario.setEmail(email);
        usuario.setSenha("hash-bcrypt");
        usuario.setStatus(status);
        usuario.setCargo(cargo);
        return usuario;
    }

    public static RefreshToken refreshToken(Usuario usuario, String hash, boolean revogado, Instant expiraEm) {
        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setTokenHash(hash);
        token.setRevogado(revogado);
        token.setExpiraEm(expiraEm);
        return token;
    }

    public static TokenSenha tokenSenha(Usuario usuario, String hash, boolean usado, Instant expiraEm) {
        TokenSenha token = new TokenSenha();
        token.setUsuario(usuario);
        token.setToken(hash);
        token.setUsado(usado);
        token.setExpiraEm(expiraEm);
        return token;
    }
}