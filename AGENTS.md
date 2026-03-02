# AGENTS.md — Backend (supportops-admin-be)

## Tech Stack
- Java 21 (use modern features: records, pattern matching, text blocks)
- Spring Boot 3.5.x
- Gradle
- PostgreSQL 16 + Flyway
- Redis (caching + rate limiting)
- JWT (jjwt library)
- Lombok (boilerplate reduction)

## Package Structure

```
com.supportops.api/
├── config/              # Spring configuration classes
│   ├── AppAuthProperties.java
│   ├── CorsConfig.java
│   └── SecurityConfig.java
├── common/              # Cross-cutting concerns
│   ├── dto/            # ApiResponse, ApiErrorResponse, PageMeta, ApiErrorDetail
│   ├── exception/      # Custom exceptions + GlobalExceptionHandler
│   ├── security/       # JwtUtil, JwtAuthenticationFilter, CurrentUser
│   └── util/           # Utility classes (HashUtil, AuthCookieUtil)
├── modules/             # Feature modules
│   ├── auth/           # Authentication (login, register, refresh, logout)
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/     # RefreshToken
│   │   ├── repository/
│   │   ├── seed/
│   │   └── service/
│   ├── user/           # User management
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/     # User
│   │   └── repository/
│   └── [future modules: product, message, dashboard, kanban, subscription, billing, invoice, file]
└── SupportopsAdminBeApplication.java
```

## Rules for Writing Code

### Module Structure
Every module follows this structure:
```
modules/[name]/
├── controller/
│   └── [Name]Controller.java      # REST endpoints
├── service/
│   └── [Name]Service.java         # Business logic
├── repository/
│   └── [Name]Repository.java      # JPA repository
├── dto/
│   ├── [Name]Response.java        # Outgoing DTO (record)
│   ├── Create[Name]Request.java   # Incoming DTO for create (record)
│   └── Update[Name]Request.java   # Incoming DTO for update (record)
├── entity/
│   └── [Name].java                # JPA entity
└── seed/                           # Optional: dev data seeders
```

### Controller Rules
- Annotate with `@RestController` and `@RequestMapping("/api/v1/[resource]")`
- Keep THIN — delegate ALL logic to Service
- Return `ApiResponse<T>` for success responses
- Use `@Valid` for request body validation
- Use `@PreAuthorize` for role-based access (if needed in future)
- Document with OpenAPI annotations (future)
- Example:
```java
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<List<ProductResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        var result = productService.list(page, size, search);
        return ApiResponse.of(result.getContent(), PageMeta.from(result));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.of(productService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(productService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.of(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        productService.delete(id);
    }
}
```

### Service Rules
- Annotate with `@Service`
- Contains ALL business logic
- Use `@Transactional` for write operations
- Throw custom exceptions (NEVER return null for missing data)
- Access tenant via `tenantId` from JWT (extracted by security filter)
- Use constructor injection via `@RequiredArgsConstructor`
- Example:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CurrentUser currentUser;

    public Page<ProductResponse> list(int page, int size, String search) {
        UUID tenantId = currentUser.getTenantId();
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Product> products;
        if (search != null && !search.isBlank()) {
            products = productRepository
                .findByTenantIdAndNameContainingIgnoreCase(
                    tenantId, search, pageable);
        } else {
            products = productRepository
                .findByTenantId(tenantId, pageable);
        }

        return products.map(this::toResponse);
    }

    public ProductResponse getById(UUID id) {
        UUID tenantId = currentUser.getTenantId();
        Product product = productRepository
            .findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Product not found: " + id));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        UUID tenantId = currentUser.getTenantId();
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setTenantId(tenantId);
        product.setName(request.name());
        product.setPrice(request.price());
        // ... set other fields
        product = productRepository.save(product);
        log.info("Product created: id={}, tenantId={}", product.getId(), tenantId);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        UUID tenantId = currentUser.getTenantId();
        Product product = productRepository
            .findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Product not found: " + id));

        // Update fields from request
        if (request.name() != null) product.setName(request.name());
        if (request.price() != null) product.setPrice(request.price());

        product = productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = currentUser.getTenantId();
        Product product = productRepository
            .findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new NotFoundException("Product not found: " + id));
        productRepository.delete(product);
        log.info("Product deleted: id={}, tenantId={}", id, tenantId);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
```

### Entity Rules
- Use `@Entity` and `@Table` annotations
- Define indexes on frequently queried columns
- Use `UUID` for all IDs
- Use `@Enumerated(EnumType.STRING)` for enums
- Use `@Column` to specify constraints (nullable, unique, length)
- Use `@PrePersist` and `@PreUpdate` for timestamps
- Multi-tenancy: include `tenantId` column on all tenant-scoped entities
- Example:
```java
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_tenant", columnList = "tenant_id"),
    @Index(name = "idx_products_name", columnList = "tenant_id, name")
})
@Getter @Setter @NoArgsConstructor
public class Product {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    private String subtitle;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
```

### Repository Rules
- Extend `JpaRepository<Entity, UUID>`
- ALL queries MUST filter by `tenantId` (multi-tenancy)
- Use Spring Data method naming for simple queries
- Use `@Query` for complex queries
- Example:
```java
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Product> findByTenantIdAndNameContainingIgnoreCase(
        UUID tenantId, String name, Pageable pageable);

    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);

    void deleteByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantId(UUID tenantId);
}
```

### DTO Rules
- Use Java **records** for immutable DTOs (requests and responses)
- Use `jakarta.validation` annotations on request DTOs
- DO NOT expose entity classes directly in controllers
- Field names must be **camelCase** (JSON serialization default)
- Example:
```java
// Request DTO
public record CreateProductRequest(
    @NotBlank(message = "Name is required")
    String name,

    String subtitle,

    @NotBlank(message = "Category is required")
    String category,

    @NotBlank(message = "Brand is required")
    String brand,

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    BigDecimal price,

    String details
) {}

// Response DTO
public record ProductResponse(
    UUID id,
    String name,
    String subtitle,
    String category,
    String brand,
    BigDecimal price,
    String details,
    Instant createdAt,
    Instant updatedAt
) {}
```

### Exception Rules
- ALL custom exceptions extend `AppException`
- `AppException` has: `HttpStatus status`, `String code`, `String message`
- Pre-built exceptions:
  - `NotFoundException` → 404
  - `ConflictException` → 409
  - `UnauthorizedException` → 401
- `GlobalExceptionHandler` catches ALL exceptions and formats them consistently
- NEVER return raw exception details in production
- Example:
```java
// Throwing
throw new NotFoundException("Product not found: " + id);
// → { "error": { "code": "NOT_FOUND", "message": "Product not found: ...", "details": null, "traceId": "..." } }

throw new ConflictException("EMAIL_ALREADY_EXISTS", "Email already in use");
throw new UnauthorizedException("Invalid credentials");
```

### Flyway Migration Rules
- File naming: `V{number}__description.sql` (double underscore)
- NEVER modify existing migrations — create new ones
- Always include `tenant_id` on tenant-scoped data tables
- Always include `created_at` and `updated_at` timestamps
- Always create indexes for foreign keys and frequently filtered columns
- Use `UUID` type for PostgreSQL
- Example: `V2__create_products.sql`
```sql
CREATE TABLE products (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    category VARCHAR(100) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_tenant ON products(tenant_id);
CREATE INDEX idx_products_name ON products(tenant_id, name);
```

### Security Rules
- **Public endpoints** (no JWT required):
  - `/api/v1/auth/login`
  - `/api/v1/auth/register`
  - `/api/v1/auth/refresh`
  - `/actuator/health`
- **All other endpoints** require valid JWT in `Authorization: Bearer <token>`
- JWT contains: `userId`, `tenantId`, `role`, `email`
- `JwtAuthenticationFilter` validates JWT and sets Spring Security context
- Access current user via `@Autowired CurrentUser currentUser` in services
- Use `@PreAuthorize` for role checks (future):
  ```java
  @PreAuthorize("hasRole('SUPER_ADMIN')")            // Super admin only
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')") // Admin+
  ```

### Logging Rules
- Use `@Slf4j` (Lombok) on service classes
- Log at appropriate levels:
  - `INFO`: Successful create/update/delete operations
  - `WARN`: Business rule violations, deprecated usage
  - `ERROR`: Unexpected exceptions, external service failures
- Include `id`, `tenantId` in log messages for traceability
- Example:
```java
log.info("Product created: id={}, tenantId={}", product.getId(), tenantId);
log.warn("Plan limit reached: tenantId={}, feature={}", tenantId, feature);
log.error("Failed to process payment: invoiceId={}", invoiceId, exception);
```

### Testing Rules
- Unit tests for Service classes (mock Repository)
- Integration tests for Controller classes (future: Testcontainers)
- Test file naming: `[Class]Test.java`
- Use `@DisplayName` for readable test names
- Follow AAA pattern: Arrange → Act → Assert
- Example:
```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CurrentUser currentUser;
    @InjectMocks private ProductService productService;

    @Test
    @DisplayName("Should throw NotFoundException when product not found")
    void getById_notFound() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        when(currentUser.getTenantId()).thenReturn(tenantId);
        when(productRepository.findByIdAndTenantId(id, tenantId))
            .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> productService.getById(id));
    }
}
```

## API Response Format

### Success Response
```json
{
  "data": T,
  "meta": {
    "page": 1,
    "size": 20,
    "total": 100,
    "totalPages": 5
  }
}
```
- `meta` is `null` for non-paginated responses

### Error Response
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      "name: Name is required",
      "price: Price must be positive"
    ],
    "traceId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

## Multi-Tenancy

- Every tenant-scoped entity has `tenant_id` column
- JWT contains `tenantId` claim
- `JwtAuthenticationFilter` extracts `tenantId` from JWT
- Services access `tenantId` via `CurrentUser.getTenantId()`
- ALL repository queries MUST filter by `tenantId`

## Important File Locations

| Purpose | Path |
|---|---|
| Main Application | `src/main/java/com/supportops/api/SupportopsAdminBeApplication.java` |
| Security Config | `src/main/java/com/supportops/api/config/SecurityConfig.java` |
| Global Exception Handler | `src/main/java/com/supportops/api/common/exception/GlobalExceptionHandler.java` |
| JWT Util | `src/main/java/com/supportops/api/common/security/JwtUtil.java` |
| JWT Filter | `src/main/java/com/supportops/api/common/security/JwtAuthenticationFilter.java` |
| Current User | `src/main/java/com/supportops/api/common/security/CurrentUser.java` |
| API Response | `src/main/java/com/supportops/api/common/dto/ApiResponse.java` |
| API Error Response | `src/main/java/com/supportops/api/common/dto/ApiErrorResponse.java` |
| Flyway Migrations | `src/main/resources/db/migration/` |
| Application Config | `src/main/resources/application.yml` |

## Coding Conventions

### Java Style
- Use Java 21 features: records, pattern matching, text blocks
- Prefer records for DTOs (immutable)
- Use Lombok annotations: `@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Slf4j`
- Constructor injection only (via `@RequiredArgsConstructor`)
- NO field injection (`@Autowired` on fields)

### Naming Conventions
| Type | Convention | Example |
|---|---|---|
| Class | PascalCase | `ProductService` |
| Interface | PascalCase | `ProductRepository` |
| Method | camelCase | `findByIdAndTenantId` |
| Variable | camelCase | `productList` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Package | lowercase | `com.supportops.api.modules.product` |

### Git Commits
- Follow Conventional Commits: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`
- Scope by module: `feat(auth): add login endpoint`, `fix(product): handle null category`
- Keep commits atomic — one logical change per commit

## How to Run

```bash
# Build
./gradlew build

# Run (dev mode)
./gradlew bootRun

# Run tests
./gradlew test

# Format code (if formatter configured)
./gradlew spotlessApply

# Infrastructure (PostgreSQL + Redis)
docker compose up -d
```

## Environment Variables

Configure via `application.yml` or environment variables:

| Variable | Description | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/supportops_java` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | JWT signing secret | (must be set) |
| `JWT_ACCESS_EXPIRATION_MS` | Access token TTL | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token TTL | `604800000` (7 days) |

## Do NOT

- ❌ Return entities directly from controllers (use DTOs/records)
- ❌ Put business logic in controllers
- ❌ Write queries without `tenant_id` filter
- ❌ Modify existing Flyway migrations
- ❌ Catch exceptions in controllers (let `GlobalExceptionHandler` handle)
- ❌ Use `@Autowired` field injection (use constructor injection)
- ❌ Store secrets in `application.yml` (use environment variables)
- ❌ Use `System.out.println` (use SLF4J logger)
- ❌ Skip validation annotations on request DTOs
- ❌ Create N+1 query patterns (use `@EntityGraph` or `JOIN FETCH`)
- ❌ Return `null` from service methods (throw exceptions)
