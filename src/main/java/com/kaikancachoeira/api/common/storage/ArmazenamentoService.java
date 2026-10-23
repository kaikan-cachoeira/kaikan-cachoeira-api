package com.kaikancachoeira.api.common.storage;

import com.kaikancachoeira.api.common.exception.RequisicaoInvalidaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class ArmazenamentoService {

    private static final Map<String, String> EXTENSOES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path diretorio;

    public ArmazenamentoService(@Value("${app.storage.diretorio}") String diretorio) {
        this.diretorio = Paths.get(diretorio).toAbsolutePath().normalize();
    }

    public String salvarImagem(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RequisicaoInvalidaException("ARQUIVO_OBRIGATORIO", "Arquivo não enviado.");
        }

        String extensao = EXTENSOES.get(arquivo.getContentType());
        if (extensao == null) {
            throw new RequisicaoInvalidaException("ARQUIVO_INVALIDO", "Formato aceito: JPG, PNG ou WEBP.");
        }

        String chave = UUID.randomUUID() + extensao;
        try {
            Files.createDirectories(diretorio);
            arquivo.transferTo(diretorio.resolve(chave));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return chave;
    }

    public void remover(String chave) {
        if (chave == null) {
            return;
        }
        try {
            Files.deleteIfExists(diretorio.resolve(chave));
        } catch (IOException ignored) {
        }
    }

    public static String url(String chave) {
        return chave == null ? null : "/arquivos/" + chave;
    }
}