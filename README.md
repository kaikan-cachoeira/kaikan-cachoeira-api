# Kaikan Cachoeira - API

API do sistema de gestão da associação. Veja a visão geral do projeto no [README da organização](https://github.com/kaikan-cachoeira).

## Stack

- Java 21
- Spring Boot 4.1.1
- Spring Data JPA
- PostgreSQL
- Docker Compose
- Springdoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`)
- Spring Boot Actuator
- Lombok

## Como rodar

### Pré-requisitos

- Java 21+
- Docker e Docker Compose

### 1. Configurar variáveis de ambiente

```bash
cp .env.exemplo .env
```

### 2. Subir o banco (PostgreSQL)

```bash
docker compose up -d
```

Confirme que `src/main/resources/application-dev.properties` aponta para o mesmo banco configurado no `.env`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/kaikan
spring.datasource.username=kaikan
spring.datasource.password=kaikan
```

### 3. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`, com o Swagger em `http://localhost:8080/swagger-ui.html`.

## Documentação da API

A documentação interativa está disponível em `http://localhost:8080/swagger-ui.html`.

O contrato OpenAPI em YAML pode ser consultado em `http://localhost:8080/v3/api-docs.yaml` e exportado com:

```bash
curl -f http://localhost:8080/v3/api-docs.yaml -o openapi.yaml
```

Após alterações nos endpoints ou em anotações OpenAPI, regenere o arquivo `openapi.yaml` e versione a atualização junto com as mudanças correspondentes.

## Padrões

+ Este repo segue os padrões de commit, branching e API definidos em [`kaikan-cachoeira-docs`](https://github.com/kaikan-cachoeira/kaikan-cachoeira-docs):

- Commits: [`PADRONIZACAO_COMMITS.md]`](https://github.com/kaikan-cachoeira/kaikan-cachoeira-docs/blob/main/PADRONIZACAO_COMMITS.md)
- Branches: [`FLUXO_DE_DESENVOLVIMENTO.md`](https://github.com/kaikan-cachoeira/kaikan-cachoeira-docs/blob/main/FLUXO_DE_DESENVOLVIMENTO.md) (`feat/nome-da-feature → dev → main`)
- Contrato de API: `kaikan-cachoeira-ENDPOINTS.md`
