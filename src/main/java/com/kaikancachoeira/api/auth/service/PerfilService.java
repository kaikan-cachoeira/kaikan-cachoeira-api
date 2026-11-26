package com.kaikancachoeira.api.auth.service;

import com.kaikancachoeira.api.auth.dto.PerfilResponse;
import com.kaikancachoeira.api.auth.entity.Cargo;
import com.kaikancachoeira.api.auth.entity.Permissao;
import com.kaikancachoeira.api.auth.entity.Usuario;
import com.kaikancachoeira.api.auth.repository.UsuarioRepository;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;

    public PerfilService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PerfilResponse consultarPerfil(Integer usuarioId) {
        Usuario usuario = buscarUsuario(usuarioId);
        Cargo cargo = usuario.getCargo();
        List<String> permissoes = cargo.getPermissoes().stream()
                .map(Permissao::getNome)
                .sorted()
                .toList();
        return new PerfilResponse(usuario.getNome(), usuario.getEmail(), cargo.getNome(), permissoes);
    }

    @Transactional
    public void atualizarNome(Integer usuarioId, String nome) {
        Usuario usuario = buscarUsuario(usuarioId);
        usuario.setNome(nome.strip());
    }

    private Usuario buscarUsuario(Integer usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuário", usuarioId));
    }
}
