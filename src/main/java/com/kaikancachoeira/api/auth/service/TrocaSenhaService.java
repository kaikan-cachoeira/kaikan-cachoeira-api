package com.kaikancachoeira.api.auth.service;

import com.kaikancachoeira.api.auth.dto.TrocaSenhaRequest;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.exception.CredenciaisInvalidasException;
import com.kaikancachoeira.api.auth.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
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
        Usuario usuario = usuarioRepository.findById(idAutenticado)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException();
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);
    }
}