package com.kaikancachoeira.api.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class ArquivosConfig implements WebMvcConfigurer {

    private final String diretorio;

    public ArquivosConfig(@Value("${app.storage.diretorio}") String diretorio) {
        this.diretorio = diretorio;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/arquivos/**")
                .addResourceLocations(Paths.get(diretorio).toAbsolutePath().normalize().toUri().toString());
    }
}
