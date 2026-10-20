package com.kaikancachoeira.api.auth.repository;

import com.kaikancachoeira.api.auth.entity.TokenSenha;
import com.kaikancachoeira.api.auth.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenSenhaRepository extends JpaRepository<TokenSenha, Long> {
    Optional<TokenSenha> findByToken(String token);
    List<TokenSenha> findByUsuarioAndUsadoFalse(Usuario usuario);
}