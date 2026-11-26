package com.kaikancachoeira.api.auth.security;

import com.kaikancachoeira.api.auth.dto.LoginRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LimiteTentativasLoginFilter extends OncePerRequestFilter {

    private static final String CAMINHO_LOGIN = "/auth/login";

    private final TentativaLoginService tentativaLoginService;
    private final ObjectMapper objectMapper;

    public LimiteTentativasLoginFilter(TentativaLoginService tentativaLoginService, ObjectMapper objectMapper) {
        this.tentativaLoginService = tentativaLoginService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !request.getRequestURI().endsWith(CAMINHO_LOGIN);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        byte[] corpo = request.getInputStream().readAllBytes();
        String email = extrairEmail(corpo);
        String ip = request.getRemoteAddr();

        Optional<Duration> bloqueio = tentativaLoginService.bloqueioRestante(email, ip);

        if (bloqueio.isPresent()) {
            responderBloqueado(response, bloqueio.get());
            return;
        }

        filterChain.doFilter(new CorpoRepetivelRequest(request, corpo), response);

        int status = response.getStatus();

        if (status == HttpStatus.UNAUTHORIZED.value()) {
            tentativaLoginService.registrarFalha(email, ip);
        } else if (status >= 200 && status < 300) {
            tentativaLoginService.registrarSucesso(email);
        }
    }

    private String extrairEmail(byte[] corpo) {
        try {
            LoginRequest login = objectMapper.readValue(corpo, LoginRequest.class);
            return login == null ? null : login.email();
        } catch (Exception e) {
            return null;
        }
    }

    private void responderBloqueado(HttpServletResponse response, Duration restante) throws IOException {
        long segundos = Math.max(1, restante.toSeconds());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(segundos));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"codigo": "MUITAS_TENTATIVAS", "mensagem": "Muitas tentativas de login. Tente novamente mais tarde.", "campos": []}
                """);
    }

    private static class CorpoRepetivelRequest extends HttpServletRequestWrapper {

        private final byte[] corpo;

        CorpoRepetivelRequest(HttpServletRequest request, byte[] corpo) {
            super(request);
            this.corpo = corpo;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream origem = new ByteArrayInputStream(corpo);

            return new ServletInputStream() {
                @Override
                public int read() {
                    return origem.read();
                }

                @Override
                public boolean isFinished() {
                    return origem.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}