package com.kaikancachoeira.api.auth.dto;

import java.util.List;

public record PerfilResponse(String nome, String email, String cargo, List<String> permissoes) {
}
