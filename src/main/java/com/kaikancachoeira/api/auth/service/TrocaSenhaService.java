package com.kaikancachoeira.api.auth.service;

import com.kaikancachoeira.api.auth.dto.TrocaSenhaRequest;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrocaSenhaService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public TrocaSenhaService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void trocarSenha(Integer idAutenticado, TrocaSenhaRequest request) {
        if (!idAutenticado.equals(request.idUsuario())) {
            throw new AccessDeniedException("Não é permitido alterar dados de outro usuário");
        }

        Usuario usuario = usuarioRepository.findById(request.idUsuario())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuarioRepository.save(usuario);
    }
}