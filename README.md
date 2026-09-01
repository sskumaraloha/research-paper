# Store Management System

A production-style **Modular Monolith** E-Commerce and Inventory Management System for a small general store selling groceries, clothing (men's, women's, children's), household items, personal care products, and other daily essentials.

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x (Spring MVC, Spring Security, Spring Data JPA / Hibernate) |
| Build | Maven |
| Frontend | Thymeleaf, Bootstrap 5, HTML/CSS/JavaScript |
| Database | MySQL 8 |

## Architecture

Feature-based modular monolith. Each business capability lives in its own package under `com.store.app`:

```
com.store.app
├── auth          # Authentication and registration
├── user          # User accounts, roles, profiles
├── product       # Product catalog
├── category      # Product categories
├── inventory     # Stock levels and movements
├── cart          # Shopping cart
├── wishlist      # Customer wishlists
├── address       # Shipping/billing addresses
├── order         # Order placement and lifecycle
├── payment       # Payment processing
├── notification  # User notifications
├── admin         # Admin dashboard / back office
├── customer      # Customer-facing storefront
├── common        # Shared building blocks (BaseEntity, shared DTOs, utilities)
├── config        # Application-wide Spring configuration
├── security      # Spring Security configuration
└── exception     # Global exception handling
```

Conventions: constructor injection, service/repository layering, DTOs at API boundaries (no JPA entities exposed), bean validation, global exception handling.

## Prerequisites

- JDK 17+
- Maven 3.8+
- MySQL 8

## Database Setup

The `dev` profile connects to `jdbc:mysql://localhost:3306/store_db` and creates the schema automatically (`createDatabaseIfNotExist=true`, `ddl-auto: update`).

Default credentials are `root` / `root`, overridable via environment variables:

```bash
export DB_USERNAME=your_user
export DB_PASSWORD=your_password
```

To create a dedicated user manually:

```sql
CREATE DATABASE IF NOT EXISTS store_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'store_user'@'localhost' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON store_db.* TO 'store_user'@'localhost';
FLUSH PRIVILEGES;
```

## Running the Application

```bash
# dev profile (default)
mvn spring-boot:run

# explicitly select a profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

For production, provide the datasource through environment variables and activate the `prod` profile:

```bash
export DB_URL='jdbc:mysql://<host>:3306/store_db?useSSL=true&serverTimezone=UTC'
export DB_USERNAME=store_user
export DB_PASSWORD=change_me
java -jar target/store-management-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Available Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `http://localhost:8080/` | Home page (Thymeleaf) |
| GET | `http://localhost:8080/health` | Health check (JSON) |

## Profiles

| Profile | File | Purpose |
|---|---|---|
| `dev` (default) | `application-dev.yml` | Local MySQL, `ddl-auto: update`, SQL logging, Thymeleaf cache off |
| `prod` | `application-prod.yml` | Env-var datasource, `ddl-auto: validate`, minimal logging, Thymeleaf cache on |
