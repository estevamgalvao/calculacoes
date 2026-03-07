REST API for processing stock portfolios and reporting them on income tax returns.

## Stack

- **Java 21** + **Spring Boot 3.5.8**
- **Maven 3.8.7**
- **Docker** (multi-stage build)

## Running Locally

### Prerequisites

- Java 21+
- Maven 3.8+

### Steps

1. Clone the repository:

    ```bash
    git clone https://github.com/estevamgalvao/calculacoes.git
    cd calculacoes
    ```

2. Run the server:

   ```bash
   mvn spring-boot:run
   ```

3. Access the API:

- Health check: `http://localhost:8080/api/portfolio/health`
- Swagger/OpenAPI: `http://localhost:8080/swagger-ui.html`

## Running with Docker

```bash
docker build -t calculacoes-backend .
docker run -p 8080:8080 \
  -e CORS_ALLOWED_ORIGINS=http://localhost:4200 \
  calculacoes-backend

```

## Environment Variables

See `.env.example` for the complete list.

### Main:

- `CORS_ALLOWED_ORIGINS`: Allowed frontend URL(s)
- `SPRING_PROFILES_ACTIVE`: `dev` or `prod`
- `SERVER_PORT`: Default port 8080

## Main Endpoints

- `POST /api/portfolio/upload` - Upload CSV with trade history
- `GET /api/portfolio/health` - Health check

## Project Structure

```
src/main/java/com/estevam/calculacoes/
├── core/
│ ├── config/ # Configurations (CORS, OpenAPI, etc.)
│ ├── exception/ # Exception handling
│ ├── response/ # Default response structure
│ └── util/ # Utilities
├── asset/ # Asset model
├── operation/ # Operation types
├── parser/ # CSV parser
└── portfolio/ # Portfolio logic (controller, service, DTO)
```