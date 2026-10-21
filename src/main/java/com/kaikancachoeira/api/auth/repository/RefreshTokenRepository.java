package com.kaikancachoeira.api.auth.repository;

import com.kaikancachoeira.api.auth.entity.RefreshToken;
import com.kaikancachoeira.api.auth.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.List;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Lock(PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findByUsuarioAndRevogadoFalse(Usuario usuario);
}