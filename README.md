# Store Management System

A production-style **Modular Monolith** E-Commerce and Inventory Management System for a small general store selling groceries, clothing (men's, women's, children's), household items, personal care products, and daily essentials.

## 1. Project Overview

One Spring Boot application containing both the customer-facing shop (Thymeleaf storefront + JSON APIs) and the back office (admin dashboard, catalog, inventory, and order management). Business capabilities live in isolated, feature-based modules that communicate through service interfaces — the deployment simplicity of a monolith with the internal boundaries of services.

## 2. Features

**Customer**
- Registration with phone OTP verification; login by phone number + password
- Forgot-password flow (OTP → single-use reset token → new password, revoking outstanding JWTs)
- Storefront: home page, product browsing with category/brand/price filters, search, sorting, pagination, product detail with image gallery and related products
- Shopping cart (stock-validated, live pricing with discount and price-change indicators)
- Wishlist with move-to-cart; address book with default-address handling
- Transactional checkout (COD; online payments pluggable), order history, order cancellation
- Profile management: name/email update, password change

**Admin**
- Dashboard: sales, orders, customers, stock alerts, recent orders, top sellers
- Hierarchical category management (unlimited depth, cycle-safe)
- Product management with multi-image upload, search, pagination
- Inventory: stock in/out/adjust with a gapless audit trail, low/out-of-stock views
- Order management: filters, detail view, state-machine status transitions (COD collected on delivery)

## 3. Architecture

Feature-based modular monolith under `com.store.app`:

```
auth        Registration, login, OTP, forgot password
user        User accounts, roles, profile
product     Catalog, images, storefront queries
category    Hierarchical categories
inventory   Authoritative stock + transaction ledger (pessimistic locking)
cart        Per-user cart with computed totals
wishlist    Saved products
address     Address book (default handling)
order       Checkout, order lifecycle state machine, admin order management
payment     Payment strategy (PaymentService per method; COD implemented)
admin       Dashboard aggregation
customer    Storefront + profile pages
common      BaseEntity, shared DTOs, file storage abstraction, utilities
config      Auditing, web, OpenAPI, seed data
security    Dual filter chains (JWT for APIs, sessions for pages), JWT stack
exception   Global exception handling + meaningful exception types
```

Conventions throughout: constructor injection, service/repository layering, DTOs at every API boundary (JPA entities are never serialized), bean validation, ownership-scoped queries (`findByIdAndUserId` → 404), and a single `@RestControllerAdvice` translating exceptions into consistent JSON errors.

Key design decisions:
- **Two security chains**: stateless JWT for `/api/**`, session form-login (HttpOnly cookie + CSRF) for server-rendered pages.
- **Inventory correctness**: every stock change locks the row (`SELECT … FOR UPDATE`), validates, and writes an immutable `InventoryTransaction` — one code path, no exceptions.
- **Checkout is one transaction**: stock reduction, order + snapshot creation, payment record, and cart clearing commit or roll back together (no partial orders, no overselling).
- **Snapshots**: order items copy name/SKU/price at purchase; orders embed the shipping address — history never changes because the catalog or address book did.
- **Pluggable integrations**: `FileStorageService` (local now, S3-ready), `PaymentService` strategy registry (COD now, Razorpay-ready), `OtpService` delivery abstraction (console now, Twilio-ready).

## 4. Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3 (Spring MVC, Spring Security, Spring Data JPA / Hibernate) |
| Auth | Session form-login (pages), JWT via jjwt (APIs), BCrypt |
| Build | Maven |
| Frontend | Thymeleaf, Bootstrap 5, vanilla JS |
| Database | MySQL 8 (H2 in-memory for tests) |
| API docs | springdoc-openapi (Swagger UI) |

## 5. Database Setup

The `dev` profile connects to `jdbc:mysql://localhost:3306/store_db` and creates the schema automatically (`createDatabaseIfNotExist=true`, `ddl-auto: update`). Default credentials `root`/`root`, overridable via `DB_USERNAME` / `DB_PASSWORD`.

Dedicated user (optional):

```sql
CREATE DATABASE IF NOT EXISTS store_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'store_user'@'localhost' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON store_db.* TO 'store_user'@'localhost';
FLUSH PRIVILEGES;
```

Schema highlights: unique constraints on user email/phone, category/product slugs, SKU, cart user, cart line (cart+product), wishlist pair (user+product), order number, payment order; indexes on hot lookups (OTP phone+purpose, inventory transactions by product, orders/addresses by user). In `prod`, `ddl-auto: validate` never alters the schema.

## 6. Configuration

| Key | Purpose | Default |
|---|---|---|
| `spring.profiles.active` | `dev` or `prod` | `dev` |
| `app.jwt.secret` | Base64 HMAC key (≥ 256 bits). Dev key ships in `application-dev.yml`; **prod requires `JWT_SECRET`** (startup fails without it) | — |
| `app.jwt.expiration-minutes` | Access-token lifetime | 60 |
| `otp.provider` | OTP delivery (`dummy` logs to console) | `dummy` |
| `otp.expiry-minutes` / `max-attempts` / `resend-cooldown-seconds` / `max-requests-per-hour` | OTP limits | 5 / 5 / 60 / 5 |
| `otp.reset-token-expiry-minutes` | Password-reset token lifetime | 10 |
| `app.storage.provider` | File storage (`local`) | `local` |
| `app.storage.local-base-dir` / `url-prefix` / `max-file-size-mb` | Upload location, URL, size cap | `uploads` / `/uploads` / 2 |

Prod additionally reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and disables Swagger.

## 7. How to Run

```bash
# Prerequisites: JDK 17+, Maven 3.8+, MySQL 8 running locally

mvn test                 # full test suite (H2, no MySQL needed)
mvn spring-boot:run      # dev profile: http://localhost:8080

# Production
mvn package
export DB_URL='jdbc:mysql://<host>:3306/store_db?useSSL=true&serverTimezone=UTC'
export DB_USERNAME=store_user DB_PASSWORD=change_me
export JWT_SECRET=<base64-encoded-256-bit-key>
java -jar target/store-management-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 8. Default Development Admin

Seeded **only when the `dev` profile is active** (never in prod):

- Phone: `9999999999` · Password: `Admin@123` · Email: `admin@store.com`

Development-only credentials — rotate immediately if ever exposed. OTP codes in dev are printed to the application console.

## 9. Available URLs

**Public**: `/` (home), `/products` (+filters), `/products/{slug}`, `/register`, `/verify-otp`, `/login`, `/forgot-password`, `/verify-reset-otp`, `/reset-password`, `/health`

**Customer** (login + verified phone): `/cart`, `/checkout`, `/customer/profile`, `/customer/orders[/{id}]`, `/customer/wishlist`, `/customer/addresses`

**Admin** (ROLE_ADMIN): `/admin/dashboard`, `/admin/orders[/{id}]`, `/admin/categories`, `/admin/products`, `/admin/inventory` (+`/low-stock`, `/out-of-stock`, `/transactions`)

## 10. API Documentation

Swagger UI (dev only): **http://localhost:8080/swagger-ui.html** (OpenAPI JSON at `/v3/api-docs`). Authenticate with `POST /api/auth/login`, then use the Authorize button with the returned Bearer token.

API groups: `/api/auth/**` (register, login, OTP, forgot password — public), `/api/categories/**` (public catalog), `/api/cart`, `/api/customer/**` (profile, addresses, wishlist, orders — ROLE_CUSTOMER), `/api/admin/**` (categories, products, inventory, orders, dashboard — ROLE_ADMIN).

## 11. Future Improvements

- Razorpay (or another gateway) `PaymentService` implementation + webhook controller
- Twilio `OtpService` implementation for real SMS delivery
- S3 `FileStorageService` implementation; magic-byte sniffing on uploads
- Refresh tokens / token rotation for long-lived API sessions
- Sales-based popularity ranking (replacing the stock-based placeholder)
- Product reviews and ratings; coupon codes
- Email notifications (order confirmation, shipping updates) via the notification module
- Flyway/Liquibase migrations instead of `ddl-auto`
- Caching (category tree, storefront queries) and full-text product search
