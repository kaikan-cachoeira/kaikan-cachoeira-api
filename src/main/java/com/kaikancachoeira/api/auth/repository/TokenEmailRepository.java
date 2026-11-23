package com.kaikancachoeira.api.auth.repository;

import com.kaikancachoeira.api.auth.entity.TokenEmail;
import com.kaikancachoeira.api.auth.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenEmailRepository extends JpaRepository<TokenEmail, Integer> {
    Optional<TokenEmail> findByToken(String token);
    List<TokenEmail> findByUsuarioAndUsadoFalse(Usuario usuario);
}