# SYSTEM ARCHITECTURE & SECURITY GUIDELINES

## 1. System Overview
This platform utilizes a **Hybrid Microservices Architecture** to achieve high scalability while maintaining developer productivity.

* **API Gateway (Spring WebFlux / Netty):** The non-blocking, reactive front door. It handles high-concurrency traffic, global security validation, rate-limiting, and routing.
* **Microservices (Spring Web MVC / Tomcat):** The synchronous, thread-per-request backend services (e.g., Auth Service, Data Service). These handle complex business logic and database interactions.

---

## 2. Request Lifecycle & Flow

### Flow A: User Authentication (Login / Register)
1. **Client** sends a `POST` request to `/api/v1/auth/login` with credentials.
2. **API Gateway** intercepts the request and routes it directly to the Auth Server (security is bypassed because `/api/v1/auth/**` is publicly permitted).
3. **Auth Server** queries the database and verifies the credentials.
4. **Auth Server** generates an `HS256` signed JWT. It uses standard RFC claims: `sub` for the user's email, and `scope` for the user's role.
5. **Auth Server** returns the JWT to the Client.

### Flow B: Secure API Call (Downstream Routing)
1. **Client** sends a request (e.g., `GET /api/v1/data`) with the header `Authorization: Bearer <token>`.
2. **API Gateway** intercepts the request.
3. **API Gateway** mathematically verifies the JWT signature and expiration locally (bypassing the Auth Server completely).
4. **API Gateway** asynchronously queries Redis to ensure the token's ID (`jti`) is not present in the blacklist.
5. If valid, the **API Gateway** extracts the `sub` and `scope` claims.
6. **API Gateway** injects `X-User-Email` and `X-User-Role` HTTP headers into the request.
7. **API Gateway** forwards the mutated request to the target downstream microservice.
8. **Downstream Service** reads the headers to identify the user, processes the logic, and returns the response.

### Flow C: User Logout
1. **Client** sends a `POST` request to `/api/v1/auth/logout` passing the active JWT.
2. **API Gateway** routes the request to the Auth Server.
3. **Auth Server** parses the token and calculates its remaining lifespan.
4. **Auth Server** saves the token's `jti` into Redis with a Time-To-Live (TTL) exactly matching the remaining lifespan.
5. Any future requests using this token will be instantly blocked at Step 4 of Flow B.

---

## 3. File Responsibilities

### The API Gateway (Reactive)
* **`GatewaySecurityConfig.java`**: The core security firewall. It defines public vs. secure route patterns, enforces `hasAuthority` role checks, configures global CORS, and registers the reactive JWT decoder.
* **`TokenBlacklistService.java`**: Uses `ReactiveStringRedisTemplate` to perform non-blocking queries against Redis. Returns a `Mono<Boolean>` to ensure Netty threads are never blocked.
* **`DownstreamHeaderFilter.java`**: A Global Gateway Filter (`Ordered` to run early). It extracts the validated identity from the Spring Security Context and attaches it as HTTP headers (`X-User-Email`, `X-User-Role`) before routing.

### The Auth Server (Synchronous)
* **`JwtUtil.java`**: Generates the JWT using standard claims (`setSubject()` for email, `claim("scope", ...)` for role) and signs it using the shared internal secret.
* **`TokenBlacklistService.java`**: Uses `StringRedisTemplate` to synchronously write a revoked token's `jti` to Redis during the logout process.
* **`AuthController.java`**: Exposes the public `/login`, `/register`, and `/logout` endpoints, bridging the incoming requests to the internal business logic.

---

## 4. Security Routing Rules

All route authorization is handled natively by Spring Security in the Gateway's `SecurityWebFilterChain`.

| Route Pattern | Access Level | Description |
| :--- | :--- | :--- |
| `/*/v3/api-docs/**`, `/*/swagger-ui/**` | **Public** (`permitAll`) | Wildcard rules to expose Swagger documentation globally for all downstream services. |
| `/api/v1/auth/**`, `/api/v1/public/**` | **Public** (`permitAll`) | Open endpoints for authentication and guest features. |
| `/payment.html`, `/fallback/**` | **Public** (`permitAll`) | Static assets and circuit breaker fallback routes. |
| `/api/v1/admin/**` | **Restricted** (`hasAuthority`) | Requires a valid JWT containing the `SCOPE_SYSTEM_ADMINISTRATOR` authority. |
| `/**` (Catch-all) | **Authenticated** | All other requests require a valid, non-blacklisted JWT. |

---

## 5. Developer Rules & Tooling Matrix

**WARNING:** The API Gateway runs on WebFlux/Netty. **No blocking code is permitted in the Gateway.** Making a Netty thread wait (e.g., using a traditional database call, `Thread.sleep`, or a synchronous Redis call) will crash the Gateway under load.

Developers must use the correct tools based on the service they are working in:

       Feature              | In API Gateway (WebFlux) | In Microservices (MVC) |
          |
**Execution Model**         | Non-blocking (Event Loop)     | Synchronous (Thread-per-request) |
**Data Returns**            | `Mono<T>`, `Flux<T>`          | `T`, `List<T>` |
**Redis Client**            | `ReactiveStringRedisTemplate` | `StringRedisTemplate` |
**Database Client**         | Spring Data R2DBC             | Spring Data JPA / Hibernate |
**HTTP Client**             | `WebClient`                   | `RestTemplate` or `OpenFeign` |
**Security Annotation**     | `@EnableWebFluxSecurity`      | `@EnableWebSecurity` |