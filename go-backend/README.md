# Go Echo + PostgreSQL CRUD API

A simple CRUD backend built with [Echo](https://echo.labstack.com/) and PostgreSQL.
It manages a `products` resource with create, read, update, and delete endpoints,
documented with Swagger (OpenAPI).

## Project structure

```
go-backend/
├── main.go                  # App entry point, routes, middleware
├── db/db.go                 # PostgreSQL connection (pgx driver)
├── handlers/                # HTTP handlers (CRUD logic)
├── models/                  # Data models and request payloads
├── docs/                    # Generated Swagger/OpenAPI spec (swag init)
├── migrations/              # SQL schema (auto-applied by docker-compose)
├── docker-compose.yml       # Local PostgreSQL
└── .env.example             # Environment variable reference
```

## Prerequisites

- Go 1.24+
- Docker (for local PostgreSQL), or an existing PostgreSQL instance

## Getting started

1. Start PostgreSQL (the schema in `migrations/` is applied automatically on first start):

   ```bash
   docker compose up -d
   ```

2. Run the server:

   ```bash
   go run .
   ```

   The server listens on `http://localhost:8080` by default. Configuration is
   read from environment variables — see `.env.example`.

## API documentation (Swagger)

With the server running, open the interactive Swagger UI at:

- **http://localhost:8080/swagger/index.html**

The raw OpenAPI spec is served at `/swagger/doc.json` and also checked in under
`docs/` (`swagger.json`, `swagger.yaml`).

The docs are generated from annotations with [swag](https://github.com/swaggo/swag).
After changing handler annotations, regenerate them:

```bash
go install github.com/swaggo/swag/cmd/swag@latest
swag init --parseDependency --parseInternal
```

## API endpoints

| Method | Path                | Description        |
|--------|---------------------|--------------------|
| GET    | `/health`           | Health check       |
| GET    | `/swagger/*`        | Swagger UI & spec  |
| POST   | `/api/products`     | Create a product   |
| GET    | `/api/products`     | List all products  |
| GET    | `/api/products/:id` | Get one product    |
| PUT    | `/api/products/:id` | Update a product   |
| DELETE | `/api/products/:id` | Delete a product   |

## Example requests

```bash
# Create
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "description": "14-inch ultrabook", "price": 999.99}'

# List
curl http://localhost:8080/api/products

# Get by id
curl http://localhost:8080/api/products/1

# Update
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop Pro", "description": "16-inch", "price": 1299.99}'

# Delete
curl -X DELETE http://localhost:8080/api/products/1
```
