### ShopCube Project — From Zero to Production Security

> **Project**: ShopCube — A Spring Boot 4.1.0 REST API for an e-commerce platform  
> **Stack**: Spring Boot · Spring Security · JWT (JJWT 0.12.7) · Spring Data JPA · PostgreSQL · Lombok · Java 21  
> **Base Package**: `me.mano.shopCube`

---

## Table of Contents

1. [Project Architecture Overview](#1-project-architecture-overview)
2. [Maven Dependencies Explained](#2-maven-dependencies-explained)
3. [What Is Spring Security? (Beginner Foundation)](#3-what-is-spring-security-beginner-foundation)
4. [The Security Filter Chain — The Heart of Spring Security](#4-the-security-filter-chain--the-heart-of-spring-security)
5. [SecurityConfig.java — Deep Dive](#5-securityconfigjava--deep-dive)
6. [What Is JWT? (Complete Beginner Guide)](#6-what-is-jwt-complete-beginner-guide)
7. [JwtService.java — Deep Dive](#7-jwtservicejava--deep-dive)
8. [JwtFilter.java — Deep Dive](#8-jwtfilterjava--deep-dive)
9. [UserDetails & UserDetailsService — Spring Security's Identity Contract](#9-userdetails--userdetailsservice--spring-securitys-identity-contract)
10. [UserPrincipal.java — Deep Dive](#10-userprinipaljava--deep-dive)
11. [MyUserDetailsService.java — Deep Dive](#11-myuserdetailsservicejava--deep-dive)
12. [Authentication vs Authorization — A Critical Distinction](#12-authentication-vs-authorization--a-critical-distinction)
13. [AuthService.java — Deep Dive](#13-authservicejava--deep-dive)
14. [Refresh Token Strategy — Why & How](#14-refresh-token-strategy--why--how)
15. [Method-Level Security with @PreAuthorize](#15-method-level-security-with-preauthorize)
16. [The SecurityContext and SecurityContextHolder](#16-the-securitycontext-and-securitycontextholder)
17. [Entities — Deep Dive](#17-entities--deep-dive)
18. [DTOs — Design Rationale](#18-dtos--design-rationale)
19. [Repositories — JPA Query Methods](#19-repositories--jpa-query-methods)
20. [Exception Handling Architecture](#20-exception-handling-architecture)
21. [Controllers — Complete Endpoint Reference](#21-controllers--complete-endpoint-reference)
22. [Complete Request Lifecycle Diagrams](#22-complete-request-lifecycle-diagrams)
23. [Design Decisions & Why They Matter](#23-design-decisions--why-they-matter)
24. [Security Vulnerabilities & Improvements](#24-security-vulnerabilities--improvements)
25. [Quick Reference Cheat Sheet](#25-quick-reference-cheat-sheet)

---

## 1. Project Architecture Overview

Before diving into Spring Security, understand what you are building. ShopCube is a **stateless REST API** for an e-commerce backend. "Stateless" means the server does **not** store any session about the user between requests. Every single HTTP request must prove who it is by itself.

### Package Structure

```
me.mano.shopCube/
├── ShopCubeApplication.java          ← Entry point
├── config/
│   └── SecurityConfig.java           ← Spring Security configuration
├── controller/
│   ├── AuthController.java           ← /auth/* (register, login, refresh)
│   ├── ProductController.java        ← /product/* (CRUD)
│   └── TestController.java           ← /test (debugging)
├── dto/
│   ├── ErrorResponseDto.java
│   ├── authDto/
│   │   ├── AuthResponseDto.java
│   │   ├── LoginRequestDto.java
│   │   ├── RefreshTokenRequestDto.java
│   │   └── RegisterRequestDto.java
│   └── productDto/
│       ├── ProductRequestDto.java
│       └── ProductResponseDto.java
├── entity/
│   ├── Users.java                    ← User table
│   ├── Product.java                  ← Product table
│   └── RefreshToken.java             ← Refresh token table
├── enums/
│   ├── Role.java                     ← USER, ADMIN
│   └── Category.java                 ← ELECTRONICS, FASHION, ...
├── exception/
│   ├── EmailAlreadyExistsException.java
│   ├── ProductNotFoundException.java
│   └── GlobalExceptionHandler.java
├── repo/
│   ├── UserRepo.java
│   ├── RefreshTokenRepo.java
│   └── ProductRepo.java
├── security/
│   ├── JwtFilter.java                ← Custom filter (intercepts every request)
│   ├── JwtService.java               ← JWT create/validate/parse logic
│   ├── MyUserDetailsService.java     ← Loads user from DB
│   └── UserPrincipal.java            ← Spring Security's view of the user
└── service/
    ├── AuthService.java              ← Business logic for auth
    └── ProductService.java           ← Business logic for products
```

### The Security Flow at 30,000 Feet

```
HTTP Request
    ↓
[Tomcat / Servlet Container]
    ↓
[Spring Security Filter Chain]   ← JwtFilter runs here
    ↓
[DispatcherServlet]
    ↓
[@RestController / @PreAuthorize checks]
    ↓
[Service Layer]
    ↓
[Repository / Database]
    ↓
HTTP Response
```

---

## 2. Maven Dependencies Explained

Your `pom.xml` uses **Spring Boot 4.1.0** (based on Jakarta EE, Java 21). Here is every security-relevant dependency explained:

```xml
<!-- Spring Boot Parent — manages all version compatibility automatically -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
</parent>
```

### Security Dependencies

```xml
<!-- THE MOST IMPORTANT — brings in Spring Security itself -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**What this brings in:**
- `spring-security-core` — Core authentication/authorization classes
- `spring-security-config` — `@EnableWebSecurity`, `SecurityFilterChain` configuration
- `spring-security-web` — Servlet filters like `UsernamePasswordAuthenticationFilter`
- Auto-configuration that locks down ALL endpoints by default

> **Important:** The moment you add this dependency, Spring Security blocks EVERY endpoint and shows a login page unless you configure it. This is "secure by default" — a foundational Spring Security principle.

### JWT Dependencies (Three Parts)

```xml
<!-- JJWT - Java JWT library, version 0.12.7 -->

<!-- jjwt-api: The interface/contract (compile time) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.7</version>
</dependency>

<!-- jjwt-impl: The implementation (runtime only, not needed at compile time) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.7</version>
    <scope>runtime</scope>
</dependency>

<!-- jjwt-jackson: JSON parsing using Jackson (runtime only) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.7</version>
    <scope>runtime</scope>
</dependency>
```

**Why three separate JARs?** This is a good design pattern called "API/Implementation separation":
- You code against `jjwt-api` interfaces — so you don't depend on internal implementation details.
- `jjwt-impl` and `jjwt-jackson` are runtime-only, meaning they are only needed when the code actually runs, not when it compiles. This prevents you from accidentally using internal APIs.

### Validation Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

This brings in Hibernate Validator (the reference implementation of Jakarta Bean Validation). This enables annotations like `@NotBlank`, `@Email`, `@Positive`, `@PositiveOrZero` on your entity and DTO fields.

### Other Key Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-data-jpa` | JPA + Hibernate ORM for database operations |
| `spring-boot-starter-webmvc` | Spring MVC for REST controllers |
| `postgresql` | PostgreSQL JDBC driver |
| `lombok` | Code generation: `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor` |
| `spring-boot-devtools` | Hot reload during development |
| `spring-boot-starter-actuator` | Health checks and metrics endpoints |

---

## 3. What Is Spring Security? (Beginner Foundation)

### The Problem Spring Security Solves

Imagine you are building a bank. You need to:
1. **Verify the person is who they say they are** (Authentication) — "Are you really Mano?"
2. **Decide what they are allowed to do** (Authorization) — "Mano can view his account but cannot access admin reports."
3. **Protect against attacks** — CSRF, session hijacking, brute force, etc.

Without a framework, you would write this code in every single controller, every time. Spring Security centralizes all of this into a reusable, well-tested framework.

### Spring Security is a Filter Chain

The most important mental model: **Spring Security is a chain of Servlet Filters that runs BEFORE your controllers ever see the request.**

A Servlet Filter is a piece of code that sits between the client and the application. Think of it like a security checkpoint at an airport:
- Checkpoint 1: Does this person have a boarding pass? (Authentication)
- Checkpoint 2: Are they authorized to go to the first-class lounge? (Authorization)
- Checkpoint 3: Did they smuggle in liquids? (CSRF, XSS protection)

Spring Security has ~15 built-in filters. You added one custom filter: `JwtFilter`.

### The Default Behavior

When you add `spring-boot-starter-security` to a project:
- Every URL is blocked by default (requires login)
- A default login page is generated
- A random password is printed to the console on startup
- CSRF protection is enabled

Your `SecurityConfig` overrides all of this behavior with your own rules.

---

## 4. The Security Filter Chain — The Heart of Spring Security

### What Is a Filter Chain?

A filter chain is an ordered sequence of filters. Each filter:
1. Receives the request
2. Does its job (check token, check CSRF, log, etc.)
3. Either **blocks** the request and returns a response, OR passes it to the **next filter** in the chain

```
Request ──► Filter1 ──► Filter2 ──► Filter3 ──► ... ──► Controller
                                       ↓
                              (or block here and return 401/403)
```

### Spring Security's Built-in Filter Order

Spring Security's `FilterChainProxy` manages an ordered list of security filters. Here are the most important ones and where your `JwtFilter` fits:

| Order | Filter Name | Purpose |
|---|---|---|
| 1 | `DisableEncodeUrlFilter` | Prevents session IDs in URLs |
| 2 | `SecurityContextHolderFilter` | Sets up the SecurityContext |
| 3 | `LogoutFilter` | Handles logout requests |
| 4 | `UsernamePasswordAuthenticationFilter` | Form login (you disabled this) |
| **YOUR FILTER** | **`JwtFilter`** | **Intercepts Bearer tokens** |
| ... | ... | ... |
| Last | `ExceptionTranslationFilter` | Converts security exceptions to HTTP responses |
| Last | `AuthorizationFilter` | Checks if the user has permission for this URL |

### How `addFilterBefore` Works

```java
http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

This line tells Spring Security: "Insert `JwtFilter` into the chain BEFORE `UsernamePasswordAuthenticationFilter`."

**Why before UsernamePasswordAuthenticationFilter?** Because `UsernamePasswordAuthenticationFilter` handles form-based login. Since you are using JWT (not form login), you want your JWT check to happen before Spring Security tries to force form-based authentication.

---

## 5. SecurityConfig.java — Deep Dive

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
```

### `@Configuration`

Tells Spring that this class contains bean definitions (methods annotated with `@Bean`). The methods will be called by Spring to create and manage objects in the Spring container.

### `@EnableWebSecurity`

This annotation:
1. Imports Spring Security's web security support
2. Enables the `SecurityFilterChain` to be customized
3. In modern Spring Security (6+), this is often optional because Spring Boot auto-configures it, but including it makes your intent explicit and disables certain auto-configurations that might conflict.

### `@EnableMethodSecurity`

This is what makes `@PreAuthorize("hasRole('ADMIN')")` work on your controller methods. Without this annotation, the `@PreAuthorize` annotations on `ProductController` would be **silently ignored** — meaning everyone could access admin-only endpoints. This is a dangerous mistake many developers make.

It enables:
- `@PreAuthorize` — Check permission BEFORE the method executes
- `@PostAuthorize` — Check permission AFTER the method executes
- `@Secured` — Simpler role-based security (older style)

### Constructor Injection of JwtFilter

```java
private final JwtFilter jwtFilter;

public SecurityConfig(JwtFilter jwtFilter) {
    this.jwtFilter = jwtFilter;
}
```

**Why constructor injection instead of `@Autowired` on a field?**
1. **Immutability** — `final` fields can't be changed after construction.
2. **Testability** — You can pass a mock filter in unit tests without Spring context.
3. **Explicitness** — Dependencies are visible and required.
4. Spring also recommends constructor injection over field injection.

### The `SecurityFilterChain` Bean

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
```

`HttpSecurity` is a builder object. You chain configuration methods on it to describe your security rules. At the end, `.build()` assembles everything into a `SecurityFilterChain`.

Think of `HttpSecurity` like a form you fill out: "disable this, allow these URLs, require auth for others, use stateless sessions."

#### 1. CSRF Disabled

```java
http.csrf(csrf -> csrf.disable());
```

**What is CSRF?** Cross-Site Request Forgery. A malicious website tricks a logged-in user's browser into sending a request to YOUR site. Since the browser automatically sends cookies, the server thinks the request is legitimate.

**Why disable it?** CSRF attacks exploit **cookie-based authentication**. Your API uses **JWT tokens in the `Authorization` header** — the malicious website cannot access or send this header (browsers block cross-origin header access). So CSRF protection is unnecessary and would break your API clients (like mobile apps, Postman, etc. that don't use cookies).

> **Rule of Thumb:** If you use cookie-based sessions → enable CSRF. If you use token-in-header → disable CSRF.

#### 2. URL Authorization Rules

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/auth/register",
        "/auth/login",
        "/auth/refresh"
    )
    .permitAll()
    .anyRequest()
    .authenticated()
);
```

This is your access control list:

| URL Pattern | Rule | Reason |
|---|---|---|
| `/auth/register` | `permitAll()` | Anyone can register — they don't have a token yet |
| `/auth/login` | `permitAll()` | Anyone can log in — they don't have a token yet |
| `/auth/refresh` | `permitAll()` | Anyone with a refresh token can get a new access token |
| Everything else | `authenticated()` | Must have a valid JWT to access |

**`permitAll()` vs `anonymous()`:**
- `permitAll()` allows both authenticated AND unauthenticated users.
- `anonymous()` allows only unauthenticated users (authenticated users would be redirected).

**Order matters!** Spring Security evaluates rules top-to-bottom. The first matching rule wins. Always put specific rules before `anyRequest()`.

#### 3. Stateless Session Management

```java
http.sessionManagement(session -> 
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
);
```

**What is session management?** By default, Spring Security creates an `HttpSession` (a server-side session stored in memory or a database). It stores authentication data in the session so the user doesn't have to re-authenticate on every request.

**`STATELESS`** means: "Never create or use an `HttpSession`." Each request must authenticate itself completely.

**Why stateless?**
- **Scalability** — No session means any server can handle any request. Perfect for load balancing and microservices.
- **Simplicity** — No need for session storage, no session timeouts to manage server-side.
- **API-first** — REST APIs are supposed to be stateless by design (REST principle).
- The JWT token IS the session — it carries all necessary user information.

#### 4. Registering the JWT Filter

```java
http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

Already explained in Section 4 above.

#### 5. The PasswordEncoder Bean

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(11);
}
```

**Why do we need a PasswordEncoder?** You must NEVER store plaintext passwords. If your database is breached, attackers would have every user's password immediately.

**What is BCrypt?** BCrypt is a password hashing function specifically designed to be slow:
- It is computationally expensive — hard to brute-force.
- It includes a built-in random **salt** (extra random data) so two users with the same password get different hashes.
- The `11` is the **strength/cost factor** — it means 2^11 = 2048 iterations. Higher = slower = more secure but also slower to login.

**Why BCrypt over MD5/SHA?** MD5 and SHA are fast cryptographic hash functions — great for file verification, terrible for passwords. Attackers with GPUs can try billions of MD5 hashes per second. BCrypt is deliberately slow — even a modern GPU would take millions of years to crack a properly BCrypt-hashed password.

**Usage in AuthService:**
```java
user.setPassword(passwordEncoder.encode(dto.getPassword())); // When registering
authenticationManager.authenticate(...);                       // BCrypt comparison happens internally
```

#### 6. The AuthenticationManager Bean

```java
@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
}
```

**What is `AuthenticationManager`?** It is the central interface in Spring Security for performing authentication. It has one method:

```java
Authentication authenticate(Authentication authentication) throws AuthenticationException;
```

**Why expose it as a `@Bean`?** Spring Security creates an `AuthenticationManager` internally but doesn't expose it as a Spring bean by default. Your `AuthService` needs it to authenticate users during login. By declaring this `@Bean`, you make it injectable into `AuthService`.

**What happens when you call `authenticationManager.authenticate(...)`?**
1. It receives a `UsernamePasswordAuthenticationToken` with the email and password.
2. It delegates to `MyUserDetailsService.loadUserByUsername(email)` to load the user from the database.
3. It uses `BCryptPasswordEncoder` to compare the provided password against the stored hash.
4. If they match → returns an authenticated `Authentication` object.
5. If they don't match → throws `BadCredentialsException`.

---

## 6. What Is JWT? (Complete Beginner Guide)

### The Problem JWT Solves

Before JWT, web apps used **server-side sessions**:
1. User logs in → server creates a session record → stores it in memory/database → sends back a session ID cookie.
2. On every request, the browser sends the cookie → server looks up the session → finds the user.

**Problems with sessions:**
- If you have 3 servers, each server has its own session store. Server A doesn't know about sessions created on Server B.
- Session data is stored on the server — memory/database overhead.
- Sessions expire on the server, causing surprise logouts.

**JWT Solution:** The server issues a **self-contained token** that contains all the user information. The server doesn't store anything. On every request, the client sends the token, and the server **verifies** it using cryptography.

### JWT Structure

A JWT looks like this:

```
eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJtYW5vQGdtYWlsLmNvbSIsInJvbGUiOiJVU0VSIiwiaWF0IjoxNzAw...
```

It is three Base64URL-encoded parts separated by dots:

```
HEADER.PAYLOAD.SIGNATURE
```

#### Part 1: Header

```json
{
  "alg": "HS384",
  "typ": "JWT"
}
```

- `alg`: The signing algorithm. JJWT with your key size uses HMAC-SHA (HS256/HS384/HS512).
- `typ`: Always "JWT".

#### Part 2: Payload (Claims)

This is YOUR data. In ShopCube's access token:

```json
{
  "sub": "mano@gmail.com",
  "role": "USER",
  "iat": 1700000000,
  "exp": 1700003600
}
```

| Claim | Name | Meaning |
|---|---|---|
| `sub` | Subject | Who the token is about (the user's email) |
| `role` | Custom claim | The user's role (USER or ADMIN) |
| `iat` | Issued At | Unix timestamp when token was created |
| `exp` | Expiration | Unix timestamp when token expires |

> **Important:** The payload is **NOT encrypted** — it is only Base64-encoded. Anyone can decode it and read the claims. Never put sensitive information (passwords, credit card numbers) in a JWT payload.

#### Part 3: Signature

```
HMACSHA384(
  base64url(header) + "." + base64url(payload),
  secret_key
)
```

The signature is created by hashing the header + payload with your **secret key**. This ensures:
1. **Integrity** — If anyone tampers with the payload (e.g., changes role from USER to ADMIN), the signature will no longer match and the token will be rejected.
2. **Authenticity** — Only your server knows the secret key. Only your server could have created a valid signature.

### How JWT Authentication Works (Big Picture)

```
LOGIN:
Client                    Server
  │── POST /auth/login ──►│
  │   {email, password}   │ 1. Verify password against DB
  │                       │ 2. Create JWT with user info
  │◄── {accessToken,     │ 3. Return tokens
  │     refreshToken}     │

SUBSEQUENT REQUESTS:
Client                    Server
  │── GET /product/getAll ─►│
  │   Authorization:        │ 1. Extract token from header
  │   Bearer <token>        │ 2. Verify signature
  │                         │ 3. Extract user from payload
  │                         │ 4. Authorize the request
  │◄── [Product List] ─────│ 5. Return response
```

---

## 7. JwtService.java — Deep Dive

```java
@Service
public class JwtService {
```

This class is the **single place** in the entire application responsible for all JWT operations: creating tokens, validating them, and extracting data from them. This is excellent Single Responsibility Principle (SRP) design.

### Secret Keys

```java
private static final String ACCESS_SECRET = 
    "shopCubeAccessSecretKeyShopCubeAccessSecretKey123WhyNotMyNameManoharan";

private static final String REFRESH_SECRET = 
    "shopCubeRefreshSecretKeyShopCubeRefreshSecretKey123MyFriendManikandan";
```

**Why two separate secrets?** This is a critical security design decision:
- If you used ONE secret for both, a refresh token could potentially be used as an access token (or vice versa) if someone exploited a bug.
- Two separate secrets means access tokens can ONLY be validated with the access secret, and refresh tokens with the refresh secret. They are cryptographically isolated.

**Why `static final`?** These are constants — they never change at runtime. `static` means they belong to the class, not to any instance. `final` means they cannot be reassigned.

> **Security Note:** In production, secrets should NEVER be hardcoded like this. They should come from environment variables or a secrets manager (AWS Secrets Manager, HashiCorp Vault, etc.). We'll discuss this more in Section 24.

### Getting the Signing Key

```java
private SecretKey getAccessSigningKey() {
    return Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes());
}

private SecretKey getRefreshSigningKey() {
    return Keys.hmacShaKeyFor(REFRESH_SECRET.getBytes());
}
```

**`Keys.hmacShaKeyFor(byte[])`:** This JJWT utility method converts a raw byte array into a `SecretKey` object suitable for HMAC-SHA signing. JJWT automatically selects the appropriate HMAC variant (HS256, HS384, or HS512) based on the key length:
- 32 bytes → HS256
- 48 bytes → HS384
- 64+ bytes → HS512

Your secrets are both 70+ characters (bytes), so JJWT will use HS512, which is the strongest.

**Why `private`?** These methods are implementation details. No other class should ever touch the raw signing keys. Keeping them private enforces encapsulation.

### Generating the Access Token

```java
public String generateAccessToken(Users user) {
    return Jwts.builder()
              .subject(user.getEmail())       // "sub" claim = email
              .claim("role", user.getRole().name()) // Custom "role" claim
              .issuedAt(new Date())            // "iat" claim = now
              .expiration(
                new Date(
                  System.currentTimeMillis() + 1000 * 60 * 60 // 1 hour
                )
              )
              .signWith(getAccessSigningKey()) // Sign with access secret
              .compact();                      // Build the JWT string
}
```

**Line-by-line explanation:**

| Method | What it does |
|---|---|
| `Jwts.builder()` | Creates a new JWT builder |
| `.subject(email)` | Sets the `sub` claim — identifies who this token is for |
| `.claim("role", "USER")` | Adds a custom claim to the payload |
| `.issuedAt(new Date())` | Sets `iat` — when the token was created |
| `.expiration(new Date(...))` | Sets `exp` — when the token becomes invalid |
| `.signWith(key)` | Signs the token with the secret key |
| `.compact()` | Builds and returns the final Base64URL-encoded JWT string |

**Why 1 hour expiry for access tokens?** Access tokens are short-lived because:
- If an access token is stolen, the attacker can only use it for 1 hour.
- After 1 hour, the user silently gets a new access token using the refresh token — without re-entering credentials.

### Generating the Refresh Token

```java
public String generateRefreshToken(Users user) {
    return Jwts.builder()
            .subject(user.getEmail())
            .issuedAt(new Date())
            .expiration(
              new Date(
                System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 14 // 14 days
              )
            )
            .signWith(getRefreshSigningKey())
            .compact();
}
```

**Key differences from access token:**
1. **No `role` claim** — The refresh token is ONLY for getting new access tokens. It should carry minimal data.
2. **14-day expiry** — Long-lived so users don't have to log in every hour.
3. **Different signing key** — Completely isolated from access tokens.

**Note the `1000L`:** The `L` makes the number a `long`. Without it, `1000 * 60 * 60 * 24 * 14` would overflow Java's `int` range (max ~2.1 billion). The full value is 1,209,600,000 milliseconds, which exceeds int max. Always use `L` for large time calculations!

### Extracting Email from Access Token

```java
public String extractEmailFromAccessToken(String token) {
    return Jwts.parser()
              .verifyWith(getAccessSigningKey()) // Verify signature first
              .build()
              .parseSignedClaims(token)         // Parse the JWT
              .getPayload()                      // Get the claims object
              .getSubject();                     // Get the "sub" claim (email)
}
```

**Step by step:**
1. `Jwts.parser()` — Creates a JWT parser
2. `.verifyWith(key)` — Configures it to verify the signature using your secret key
3. `.build()` — Builds the parser
4. `.parseSignedClaims(token)` — Parses the token AND verifies the signature. Throws `JwtException` if signature is invalid or token is malformed.
5. `.getPayload()` — Returns the `Claims` object (the JSON payload)
6. `.getSubject()` — Returns the `sub` claim (the email you stored)

### Token Validation Methods

```java
public boolean validateAccessToken(String token) {
    try {
        Jwts.parser()
            .verifyWith(getAccessSigningKey())
            .build()
            .parseSignedClaims(token);  // Throws if invalid
        return true;
    } catch (Exception e) {
        return false;  // Any exception means invalid token
    }
}
```

**What exceptions can be thrown:**
- `ExpiredJwtException` — Token's `exp` time is in the past
- `MalformedJwtException` — Token doesn't have the correct JWT format
- `SignatureException` — Signature verification failed (tampered token)
- `UnsupportedJwtException` — Token uses an unsupported feature
- `IllegalArgumentException` — Token is null or empty

By catching `Exception` broadly and returning `false`, your code handles ALL of these cases gracefully without crashing.

---

## 8. JwtFilter.java — Deep Dive

```java
@Component
public class JwtFilter extends OncePerRequestFilter {
```

### `@Component`

Marks this class as a Spring-managed bean. Spring will create one instance of this class and inject it wherever needed (specifically, into `SecurityConfig`).

### `OncePerRequestFilter`

This is the key base class. The name explains it: it guarantees that this filter's logic runs **exactly once per HTTP request**, even if the request is forwarded or redirected internally.

Why does this matter? In some servlet configurations, a request might pass through the filter chain multiple times (e.g., internal forwards). If your JWT filter ran multiple times, it might try to authenticate the user twice. `OncePerRequestFilter` prevents this.

You override the `doFilterInternal` method (not `doFilter`). Spring calls `doFilter` internally, which calls your `doFilterInternal` and ensures the "once" guarantee.

### The Filter Logic — Step by Step

```java
@Override
protected void doFilterInternal(
    HttpServletRequest request, 
    HttpServletResponse response, 
    FilterChain filterChain
) throws ServletException, IOException {
```

**Parameters:**
- `HttpServletRequest request` — The incoming HTTP request (headers, body, URL, etc.)
- `HttpServletResponse response` — The outgoing HTTP response (you can write to it to reject the request)
- `FilterChain filterChain` — The remaining filters. Calling `filterChain.doFilter(request, response)` passes control to the next filter.

#### Step 1: Check for Authorization Header

```java
String authHeader = request.getHeader("Authorization");

if(authHeader == null || !authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);
    return;
}
```

**What is the Authorization header?** HTTP has a standard header called `Authorization`. For Bearer token auth, it looks like:
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtYW5...
```

**Why pass to next filter instead of rejecting?** Some endpoints (like `/auth/login`, `/auth/register`) are `permitAll()`. These requests won't have a Bearer token. Passing them to the next filter lets Spring Security's `AuthorizationFilter` handle the `permitAll()` rule — it will allow them through.

If you returned a 401 error here for every request without a token, your login endpoint would be unusable!

#### Step 2: Extract and Validate the Token

```java
String token = authHeader.substring(7);  // Remove "Bearer " prefix (7 chars)

if(!jwtService.validateAccessToken(token)) {
    filterChain.doFilter(request, response);
    return;
}
```

`authHeader.substring(7)` removes the `"Bearer "` prefix. The "Bearer " string is exactly 7 characters long (including the space). After this, `token` contains just the raw JWT string.

If the token is invalid (expired, tampered, malformed), we again pass to the next filter WITHOUT setting authentication. The `AuthorizationFilter` at the end of the chain will then reject the request with a 401.

#### Step 3: Extract Email from Token

```java
String email = jwtService.extractEmailFromAccessToken(token);
```

At this point, we know the token is valid (signature is correct, not expired). We extract the `sub` claim (the email). This is the user's identity.

#### Step 4: Set the Authentication in the SecurityContext

```java
if(SecurityContextHolder.getContext().getAuthentication() == null) {
    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

    UsernamePasswordAuthenticationToken authToken = 
        new UsernamePasswordAuthenticationToken(
            userDetails,     // principal (the UserDetails object)
            null,            // credentials (password — null after authentication)
            userDetails.getAuthorities()  // roles/permissions
        );

    SecurityContextHolder.getContext().setAuthentication(authToken);
}
```

**Why check `getAuthentication() == null` first?** This is a guard against setting authentication twice. In a well-functioning stateless system, it should always be null at this point (because we're stateless — no session), but it's a defensive check.

**`UsernamePasswordAuthenticationToken`:** This is Spring Security's standard `Authentication` implementation for username/password-style authentication. By passing it three arguments (principal, credentials, authorities), you are creating an **authenticated** token. If you passed only two arguments (principal, credentials), it would be **unauthenticated**.

**Why load from database here?** You might wonder: "The JWT already contains the email and role — why hit the database again?"

The reason is: you need a full `UserDetails` object. Spring Security needs:
- The `getAuthorities()` collection (to check `@PreAuthorize` roles)
- The `isEnabled()`, `isAccountNonExpired()` etc. flags

You could embed all this in the JWT to avoid the DB call — a common optimization — but it requires more complex JWT payload design. Loading from DB ensures you always get the most current user state (e.g., if an admin disabled the account, the DB will reflect that even if the JWT hasn't expired yet).

**`SecurityContextHolder.getContext().setAuthentication(authToken)`:** This is the climax of the entire filter. By setting the `Authentication` object in the `SecurityContext`, you are telling Spring Security: "This request is authenticated. The user is X with roles Y." All subsequent security checks (like `@PreAuthorize`) will read from this context.

#### Step 5: Continue the Filter Chain

```java
filterChain.doFilter(request, response);
```

After setting authentication (or if the token was missing/invalid), always pass control to the next filter. This is critical — forgetting this call would hang every request!

---

## 9. UserDetails & UserDetailsService — Spring Security's Identity Contract

### The Problem of Identity

Spring Security doesn't know or care about your `Users` entity. It was written before your project existed. It needs a **standard way** to ask: "What is this user's username? Password? What roles do they have? Is their account locked?"

This is solved through two interfaces:

### `UserDetails` Interface

```java
public interface UserDetails {
    Collection<? extends GrantedAuthority> getAuthorities();
    String getPassword();
    String getUsername();
    boolean isAccountNonExpired();
    boolean isAccountNonLocked();
    boolean isCredentialsNonExpired();
    boolean isEnabled();
}
```

Your `UserPrincipal` class implements this interface, acting as an **adapter** between your `Users` entity and Spring Security's expectations.

### `UserDetailsService` Interface

```java
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

Spring Security calls this when it needs to load a user (during form login, or when you explicitly call it as in `JwtFilter`). Your `MyUserDetailsService` implements this by looking up the user in the database by email.

---

## 10. UserPrincipal.java — Deep Dive

```java
public class UserPrincipal implements UserDetails {
    private final Users user;

    public UserPrincipal(Users user) {
        this.user = user;
    }
```

`UserPrincipal` wraps your `Users` entity and exposes it through the `UserDetails` interface. This is the **Adapter Design Pattern** in action.

**Why a separate class instead of making `Users` implement `UserDetails` directly?**
- **Separation of Concerns** — Your `Users` entity is a JPA entity (database concern). `UserPrincipal` is a security concern. Mixing them creates tight coupling.
- **Flexibility** — If you change your `Users` entity, you don't break the security contract. If Spring Security changes, you only update `UserPrincipal`.

### Authorities (Roles)

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
}
```

**`GrantedAuthority`** is Spring Security's representation of a permission or role.

**`SimpleGrantedAuthority`** is the standard implementation — it's just a string wrapper.

**Why `"ROLE_" + role.name()`?** Spring Security has a convention that roles must be prefixed with `ROLE_`. This matters for:
- `hasRole('ADMIN')` — internally looks for `ROLE_ADMIN`
- `hasAuthority('ROLE_ADMIN')` — looks for the exact string `ROLE_ADMIN`

So when you write `@PreAuthorize("hasRole('ADMIN')")`, Spring Security looks for a `GrantedAuthority` with value `"ROLE_ADMIN"`.

**Example output:** For a user with `role = Role.USER`, this returns `[ROLE_USER]`. (Your filter even prints this: `System.out.println(userDetails.getAuthorities())`)

### Username

```java
@Override
public String getUsername() {
    return user.getEmail();  // Email IS the username in this system
}
```

In ShopCube, the email address acts as the username (unique identifier). This is a common pattern.

### Account Status Methods

```java
@Override
public boolean isAccountNonExpired() {
    return UserDetails.super.isAccountNonExpired(); // Returns true
}

@Override
public boolean isAccountNonLocked() {
    return UserDetails.super.isAccountNonLocked(); // Returns true
}

@Override
public boolean isCredentialsNonExpired() {
    return UserDetails.super.isCredentialsNonExpired(); // Returns true
}

@Override
public boolean isEnabled() {
    return UserDetails.super.isEnabled(); // Returns true
}
```

These default interface methods all return `true`, meaning all accounts are always active, non-locked, and enabled. In a production system, you would add boolean fields to your `Users` entity and return them here (e.g., for account banning, email verification, etc.).

---

## 11. MyUserDetailsService.java — Deep Dive

```java
@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Users user = userRepo.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
        
        return new UserPrincipal(user);
    }
}
```

### The Bridge Between Database and Spring Security

This is a thin service — it has one job: **look up a user by email and return it as a `UserDetails` object.**

### `@Autowired` vs Constructor Injection

You used `@Autowired` field injection here, but constructor injection in other classes. This is an inconsistency in the codebase. Both work, but constructor injection is preferred (see the discussion in Section 5). It doesn't cause bugs, but it's worth being consistent.

### `orElseThrow`

```java
userRepo.findByEmail(email)
    .orElseThrow(() -> new UsernameNotFoundException("User Not Found"))
```

`findByEmail` returns an `Optional<Users>`. If the email is not found in the database, it throws `UsernameNotFoundException`. Spring Security catches this and converts it to a 401 response.

### Where Is `loadUserByUsername` Called?

**Two places:**
1. **`JwtFilter`** — After validating the JWT, to load the full user from the DB.
2. **`AuthenticationManager.authenticate()`** — During login, Spring internally calls `loadUserByUsername` to get the user, then uses `PasswordEncoder` to compare passwords.

---

## 12. Authentication vs Authorization — A Critical Distinction

Many beginners confuse these two concepts. Understanding the difference is fundamental.

### Authentication — "Who are you?"

Authentication is the process of **verifying identity**. It answers: "Is this person who they claim to be?"

- **Login** is authentication — you prove you are "mano@gmail.com" by providing the correct password.
- A valid JWT is proof of authentication — the server previously verified your identity and issued this token.

**In ShopCube:** Authentication happens in two ways:
1. At login: `authenticationManager.authenticate(...)` verifies email + password.
2. On every subsequent request: `JwtFilter` verifies the JWT signature.

### Authorization — "What are you allowed to do?"

Authorization is the process of **checking permissions**. It answers: "Does this authenticated person have the right to do this action?"

- An authenticated user (role = USER) trying to add products → **DENIED** (only ADMIN can add products).
- An authenticated user (role = USER) trying to view all products → **ALLOWED**.

**In ShopCube:** Authorization is enforced in two places:
1. **URL-level:** `anyRequest().authenticated()` in `SecurityConfig` — all URLs require authentication.
2. **Method-level:** `@PreAuthorize(...)` on controller methods — specific roles required.

### The `Authentication` Object in Spring Security

After successful authentication, Spring Security creates an `Authentication` object and stores it in the `SecurityContext`. This object contains:
- **Principal** — Who is authenticated (your `UserPrincipal`/`UserDetails` object)
- **Credentials** — Usually the password (set to null after authentication for security)
- **Authorities** — The list of `GrantedAuthority` objects (roles)
- **isAuthenticated()** — Boolean flag

---

## 13. AuthService.java — Deep Dive

The authentication business logic lives here.

### Registration

```java
public Users register(RegisterRequestDto dto) {
    if(userRepo.findByEmail(dto.getEmail()).isPresent()) {
        throw new EmailAlreadyExistsException("Oops Email already exist");
    }

    Users user = new Users();
    user.setUsername(dto.getUsername());
    user.setEmail(dto.getEmail());
    user.setPassword(passwordEncoder.encode(dto.getPassword()));  // HASH the password!
    user.setRole(Role.USER);  // Force role — never trust the client!

    return userRepo.save(user);
}
```

**Key design decisions:**

1. **Duplicate email check:** Before creating a user, check if the email already exists. The `@Column(unique = true)` on the email field would also prevent duplicates at the database level (as a constraint), but the explicit check here gives you a friendly error message instead of a cryptic database error.

2. **`passwordEncoder.encode(dto.getPassword())`:** The password is hashed using BCrypt BEFORE storing. If you forgot this line and stored a plaintext password, it would be a catastrophic security vulnerability.

3. **`user.setRole(Role.USER)`:** This is commented in your own code: *"Never allow client to choose role. Hackers can easily misuse the feature."* This is an excellent security practice. If the registration DTO included a `role` field and you set `user.setRole(dto.getRole())`, an attacker could simply send `"role": "ADMIN"` in the request body and give themselves admin privileges.

### Login

```java
public AuthResponseDto login(LoginRequestDto dto) {
    // Step 1: Authenticate
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
    );

    // Step 2: Load user from DB (needed to generate token)
    Users user = userRepo.findByEmail(dto.getEmail())
        .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

    // Step 3: Generate tokens
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    // Step 4: Delete old refresh token (one active session per user)
    refreshTokenRepo.findByUser(user)
        .ifPresent(existingToken -> {
            refreshTokenRepo.delete(existingToken);
        });

    // Step 5: Save new refresh token to DB
    RefreshToken refreshTokenEntity = new RefreshToken();
    refreshTokenEntity.setToken(refreshToken);
    refreshTokenEntity.setExpiryDate(LocalDateTime.now().plusDays(14));
    refreshTokenEntity.setUser(user);
    refreshTokenRepo.save(refreshTokenEntity);

    // Step 6: Return both tokens to client
    return new AuthResponseDto(accessToken, refreshToken);
}
```

**Step 1: `authenticationManager.authenticate(...)`**

You create a `UsernamePasswordAuthenticationToken` with email and password. This is an **unauthenticated** token (two-argument constructor). The `AuthenticationManager`:
1. Calls `MyUserDetailsService.loadUserByUsername(email)` to get the user from the DB.
2. Uses `BCryptPasswordEncoder.matches(rawPassword, storedHash)` to compare.
3. Throws `BadCredentialsException` if wrong, or returns an authenticated token if correct.

**Step 2: Load user again**

Note: You fetch the user TWICE — once inside `authenticationManager.authenticate()` and once explicitly here. This is a slight inefficiency. An optimization would be to get the authenticated `UserDetails` from the returned `Authentication` object. However, what's returned is a `UserDetails` (via `UserPrincipal`) and you need a `Users` entity to call `jwtService.generateAccessToken(user)`. This forces the second DB lookup.

**Step 4: Delete old refresh token**

This implements a **"one active session"** policy. If a user logs in again, their old refresh token is invalidated. This is a security design decision: if someone else logged in as you (with your credentials), you would be logged out. However, it also means you can't be logged in on multiple devices simultaneously with a single account.

**Step 5: Save refresh token to DB**

The refresh token is stored in the database with:
- The raw token string
- The expiry date
- A reference to the `Users` entity (via `@OneToOne`)

This is called a **"database-backed refresh token"** — the token is validated against the database, not just cryptographically. This means you can:
- Invalidate a specific refresh token (logout)
- See all active sessions
- Revoke access instantly

---

## 14. Refresh Token Strategy — Why & How

### The Problem of Short-Lived Access Tokens

Access tokens expire in 1 hour. Without refresh tokens, users would have to log in again every hour — terrible UX.

### The Solution: Two-Token System

| | Access Token | Refresh Token |
|---|---|---|
| **Purpose** | Authentication & Authorization | Get new Access Token |
| **Lifetime** | 1 hour | 14 days |
| **Storage (Client)** | Memory / HttpOnly Cookie | Secure HttpOnly Cookie / Secure Storage |
| **Storage (Server)** | NOT stored (stateless) | Stored in DB |
| **Sent with** | Every API request | Only `/auth/refresh` endpoint |
| **Contains** | email + role | email only |
| **Revocable** | No (must wait to expire) | Yes (delete from DB) |

### The Refresh Flow

```java
public AuthResponseDto refreshAccessToken(RefreshTokenRequestDto dto) {
    // Step 1: Find the refresh token in the database
    RefreshToken refreshTokenEntity = refreshTokenRepo
        .findByToken(dto.getRefreshToken())
        .orElseThrow(() -> new RuntimeException("Refresh Token Not Found"));

    // Step 2: Check if it's expired (DB-level check)
    if(refreshTokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
        refreshTokenRepo.delete(refreshTokenEntity);
        throw new RuntimeException("Refresh Token Expired");
    }

    // Step 3: Cryptographically validate the token
    if(!jwtService.validateRefreshToken(dto.getRefreshToken())) {
        throw new RuntimeException("Invalid refresh Token");
    }

    // Step 4: Extract email and load user
    String email = jwtService.extractEmailFromRefreshToken(dto.getRefreshToken());
    Users user = userRepo.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

    // Step 5: Generate new access token (reuse the same refresh token)
    String newAccessToken = jwtService.generateAccessToken(user);

    return new AuthResponseDto(newAccessToken, dto.getRefreshToken());
}
```

**Step 1:** Lookup in DB — ensures the refresh token wasn't already revoked (e.g., by logout).

**Step 2:** DB-level expiry check — even if the JWT hasn't expired cryptographically, the DB expiry date provides an independent check. This is a double-validation approach.

**Step 3:** Cryptographic validation — ensures the token wasn't tampered with.

**Step 5:** The refresh token is **reused** (not rotated). A more secure practice is **refresh token rotation**: generate a new refresh token on every refresh, and invalidate the old one. This way, if an old refresh token is used (stolen and replayed), you can detect the replay and revoke all tokens. This is not implemented here but is an important production improvement.

---

## 15. Method-Level Security with @PreAuthorize

### What is Method Security?

Spring Security can protect not just URLs but individual Java methods. When `@EnableMethodSecurity` is active, Spring wraps your beans in a proxy that checks authorization before (or after) calling the actual method.

### How It Works Internally

When you call `productController.addProducts(...)`, you don't call it directly. Spring creates a **proxy** around the controller:

```
Client Request
     ↓
Spring AOP Proxy (checks @PreAuthorize)
     ↓ (if authorized)
Actual ProductController.addProducts()
     ↓
ProductService.addProducts()
```

If the `@PreAuthorize` check fails, Spring throws `AccessDeniedException` before your actual code runs.

### `hasRole` vs `hasAnyRole` vs `hasAuthority`

```java
@PreAuthorize("hasRole('ADMIN')")         // Checks for ROLE_ADMIN
@PreAuthorize("hasAnyRole('USER','ADMIN')") // Checks for ROLE_USER OR ROLE_ADMIN
@PreAuthorize("hasAuthority('ROLE_ADMIN')") // Checks for exact string "ROLE_ADMIN"
```

`hasRole('X')` automatically prepends `ROLE_` and checks for `ROLE_X`. `hasAuthority('X')` checks for the exact string `X` without modification. They are equivalent when used with the `ROLE_` prefix convention.

### Your Endpoint Authorization Matrix

| Endpoint | Method | `@PreAuthorize` | Who Can Access |
|---|---|---|---|
| `POST /auth/register` | `AuthController` | None (permitAll) | Everyone |
| `POST /auth/login` | `AuthController` | None (permitAll) | Everyone |
| `POST /auth/refresh` | `AuthController` | None (permitAll) | Everyone |
| `POST /product/addProducts` | `ProductController` | `hasAnyRole('ADMIN')` | ADMIN only |
| `GET /product/get/{id}` | `ProductController` | None (just `authenticated()`) | Any logged-in user |
| `GET /product/getAll/` | `ProductController` | `hasAnyRole('USER','ADMIN')` | USER or ADMIN |
| `PUT /product/update/{id}` | `ProductController` | `hasRole('ADMIN')` | ADMIN only |
| `DELETE /product/remove/{id}` | `ProductController` | `hasRole('ADMIN')` | ADMIN only |
| `GET /product/get/category/{cat}` | `ProductController` | `hasAnyRole('USER','ADMIN')` | USER or ADMIN |
| `GET /product/get/name/{name}` | `ProductController` | None (just `authenticated()`) | Any logged-in user |
| `GET /test` | `TestController` | None (just `authenticated()`) | Any logged-in user |

---

## 16. The SecurityContext and SecurityContextHolder

### What is SecurityContext?

The `SecurityContext` is a thread-local storage that holds the `Authentication` object for the **current thread** processing the current HTTP request.

Think of it like this: In a coffee shop, every waiter (thread) carries a notepad (SecurityContext) that says "I'm currently serving customer Mano, who ordered a latte." The notepad is specific to that waiter's current order.

```java
// In JwtFilter:
SecurityContextHolder.getContext().setAuthentication(authToken);

// In any @Controller, @Service, etc., you can read it:
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
String email = principal.getUsername();
```

### Thread-Local Storage

`SecurityContextHolder` uses `ThreadLocal` by default. `ThreadLocal` is a Java mechanism that gives each thread its own isolated copy of a variable. This means:
- Thread 1 handling User A's request sees User A's `Authentication`.
- Thread 2 handling User B's request sees User B's `Authentication`.
- They don't interfere with each other.

### `STATELESS` and SecurityContext

Because you set `SessionCreationPolicy.STATELESS`, the `SecurityContext` is **cleared after every request**. It lives only for the duration of one HTTP request-response cycle. This is why `JwtFilter` has to set the authentication on EVERY request — it won't be remembered from the previous one.

---

## 17. Entities — Deep Dive

### Users.java

```java
@Data
@Entity
public class Users {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotBlank
    private String username;

    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime createdAt = LocalDateTime.now();
}
```

**Annotation explanations:**

| Annotation | Where | What it does |
|---|---|---|
| `@Data` | Class | Lombok: generates getters, setters, `toString`, `equals`, `hashCode` |
| `@Entity` | Class | Marks this as a JPA entity (maps to a `users` table in PostgreSQL) |
| `@Id` | Field | Marks the primary key |
| `@GeneratedValue(SEQUENCE)` | Field | Uses a DB sequence to generate IDs (PostgreSQL-friendly) |
| `@NotBlank` | Field | Validation: field cannot be null or whitespace-only |
| `@Email` | Field | Validation: must be a valid email format |
| `@Column(unique = true)` | Field | Database-level unique constraint on the email column |
| `@Enumerated(EnumType.STRING)` | Field | Stores enum as "USER"/"ADMIN" string, not 0/1 integer |

**`EnumType.STRING` vs `EnumType.ORDINAL`:**
- `ORDINAL` stores `0` for USER, `1` for ADMIN. If you ever reorder the enum, your data becomes wrong.
- `STRING` stores `"USER"` and `"ADMIN"`. Safe, readable, and refactor-friendly. Always use `STRING`.

### RefreshToken.java

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    private LocalDateTime expiryDate;

    @OneToOne
    private Users user;
}
```

**`@OneToOne` relationship:** One refresh token belongs to exactly one user. One user has at most one refresh token (enforced by the delete-before-save logic in `AuthService.login()`).

This creates a foreign key column in the `refresh_token` table that references the `users` table.

**`@Column(unique = true, nullable = false)`:** The token string must be unique (each token is different) and cannot be null. The DB enforces this even if application code has a bug.

### Product.java

```java
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank private String name;
    @NotBlank private String description;
    @Positive private double price;
    @PositiveOrZero private int stockQuantity;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String imageUrl;

    @CreationTimestamp
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    private LocalDateTime updatedAt = LocalDateTime.now();
}
```

**`@CreationTimestamp`:** Hibernate automatically sets this field to the current timestamp when the entity is first persisted.

**`@UpdateTimestamp`:** Hibernate automatically updates this field to the current timestamp whenever the entity is updated.

**`GenerationType.IDENTITY` vs `SEQUENCE`:**
- `IDENTITY` uses the database's auto-increment feature (like `SERIAL` in PostgreSQL). Simpler.
- `SEQUENCE` uses a database sequence object, which is more flexible and allows batch inserts.
- Both work. Your project uses both — `IDENTITY` for `Product`, `SEQUENCE` for `Users` and `RefreshToken`.

---

## 18. DTOs — Design Rationale

### What Is a DTO (Data Transfer Object)?

A DTO is a simple object used to **transfer data between layers** (e.g., from the HTTP request to the service layer, or from the service to the HTTP response). It's different from an entity:

| Aspect | Entity | DTO |
|---|---|---|
| Purpose | Maps to a database table | Transfers data over the network |
| Contains | All DB fields including sensitive ones | Only the fields the client needs |
| Annotations | JPA (`@Entity`, `@Id`) | Validation (`@NotBlank`) |

### Why Use DTOs Instead of Entities Directly?

**Security:** If you returned `Users` entity from the registration endpoint, the response would include the hashed password, role, createdAt — fields the client should never see. With `UserResponseDto`, you control exactly what goes out.

**Flexibility:** Your `ProductResponseDto` only includes `id`, `name`, `description`, `price` — not `stockQuantity`, `imageUrl`, `createdAt`, `updatedAt`. You might show stock info to admins but hide it from regular users by using different DTOs.

**Validation:** DTOs can have their own validation rules suited to input validation, separate from entity validation.

### Auth DTOs

```java
// What the client sends for registration
public class RegisterRequestDto {
    private String username;
    private String email;
    private String password;
}

// What the client sends for login
public class LoginRequestDto {
    private String email;
    private String password;
}

// What the client sends for token refresh
public class RefreshTokenRequestDto {
    private String refreshToken;
}

// What the server sends back after login or refresh
@AllArgsConstructor
public class AuthResponseDto {
    private String accessToken;
    private String refreshToken;
}
```

### Product DTOs

```java
// What ADMIN sends when adding/updating a product
public class ProductRequestDto {
    @NotBlank private String name;
    @NotBlank private String description;
    @Positive private double price;
    @PositiveOrZero private int stockQuantity;
    private Category category;
    private String imageUrl;
}

// What all clients receive when viewing a product (limited info)
public class ProductResponseDto {
    private Long id;
    @NotBlank private String name;
    @NotBlank private String description;
    @Positive private double price;
    // No stockQuantity, imageUrl, timestamps — intentionally hidden
}
```

---

## 19. Repositories — JPA Query Methods

### UserRepo

```java
@Repository
public interface UserRepo extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);
}
```

`JpaRepository<Users, Long>` gives you for free:
- `findById(Long id)`, `findAll()`, `save(entity)`, `delete(entity)`, `count()`, etc.

`findByEmail(String email)` is a **derived query**. Spring Data JPA parses the method name and generates SQL automatically:
```sql
SELECT * FROM users WHERE email = ?
```
The return type `Optional<Users>` forces callers to handle the case when no user is found.

### RefreshTokenRepo

```java
@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser(Users user);
}
```

- `findByToken` — Used during token refresh and logout: look up the refresh token record by the token string.
- `findByUser` — Used during login: find an existing refresh token for the user (so we can delete it before creating a new one).

### ProductRepo

```java
@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {
    List<Product> findByCategory(Category category);
    List<Product> findByNameContainingIgnoreCase(String name);
    Page<Product> findByName(String name, Pageable pageable);
}
```

- `findByCategory(Category category)` → `WHERE category = ?`
- `findByNameContainingIgnoreCase(String name)` → `WHERE LOWER(name) LIKE LOWER('%?%')` — case-insensitive search
- `findByName(String name, Pageable pageable)` → `WHERE name = ?` with pagination

`Page<Product>` vs `List<Product>`: `Page` contains:
- The content (list of products for this page)
- Total pages count
- Total elements count
- Current page number
- Page size

---

## 20. Exception Handling Architecture

### Custom Exceptions

```java
// For duplicate email detection
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);  // Passes message to RuntimeException
    }
}

// For missing products
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}
```

Both extend `RuntimeException`, which is an "unchecked" exception in Java. This means:
- You DON'T have to declare `throws EmailAlreadyExistsException` on every method.
- They propagate up the call stack automatically until caught.
- Spring's `@ExceptionHandler` catches them at the controller level.

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleEmailAlreadyExistException(
            EmailAlreadyExistsException ex) {
        ErrorResponseDto error = new ErrorResponseDto(409, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleProductNotFoundException(
            ProductNotFoundException ex) {
        ErrorResponseDto error = new ErrorResponseDto(404, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

**`@RestControllerAdvice`:** A combination of `@ControllerAdvice` (applies to all controllers) and `@ResponseBody` (returns JSON). Any exception thrown from any controller, service, or repository that bubbles up to the controller layer will be caught here.

**`@ExceptionHandler(SomeException.class)`:** Declares that this method handles `SomeException` (and its subclasses).

**The Error Response:**

```json
{
    "status": 409,
    "message": "Oops Email already exist",
    "timestamp": "2026-06-26T20:23:04"
}
```

This gives clients a consistent, structured error format instead of a stack trace.

---

## 21. Controllers — Complete Endpoint Reference

### AuthController

```java
@RestController
@RequestMapping("/auth")
public class AuthController {
```

`@RestController` = `@Controller` + `@ResponseBody`. All methods return JSON automatically.

| Endpoint | Method | Request Body | Response | Auth Required |
|---|---|---|---|---|
| `/auth/register` | POST | `RegisterRequestDto` | `Users` (entity — security note!) | No |
| `/auth/login` | POST | `LoginRequestDto` | `AuthResponseDto` | No |
| `/auth/refresh` | POST | `RefreshTokenRequestDto` | `AuthResponseDto` | No |

> **Design Issue:** The `/auth/register` endpoint returns the full `Users` entity, which exposes the hashed password and other internal fields to the client. A proper implementation would return a `UserResponseDto` with only `id`, `username`, `email`, and `createdAt`.

### ProductController

```java
@RestController
@RequestMapping("/product")
public class ProductController {
```

**Pagination in `getAllProducts`:**

```java
@GetMapping("/getAll/")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public List<ProductResponseDto> getAllProducts(
        @RequestParam(defaultValue = "1") int pageNo,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir,
        @RequestParam() String search) {

    Sort sort = null;
    if(sortDir.equalsIgnoreCase("asc")){
        sort = Sort.by(sortBy).ascending();
    } else {
        sort = sort.by(sortBy).descending();  // ← BUG: 'sort' is null here!
    }

    return productService.getAllProducts(PageRequest.of(pageNo - 1, pageSize, sort), search);
}
```

**Pagination explained:**
- `pageNo` starts at 1 (user-friendly), but Spring Data JPA's `PageRequest.of()` is 0-indexed. So `PageRequest.of(pageNo - 1, ...)` converts 1→0, 2→1, etc.
- `pageSize` = how many records per page.
- `sortBy` = which field to sort by (e.g., `"id"`, `"name"`, `"price"`).
- `sortDir` = `"asc"` or `"desc"`.

> **Bug Identified:** When `sortDir` is not `"asc"`, `sort` is `null` at that point (it was declared as `Sort sort = null`). `sort.by(sortBy).descending()` will throw a `NullPointerException`. The fix is:
> ```java
> sort = Sort.by(sortBy).descending();  // Not sort.by(), but Sort.by()
> ```

### TestController

```java
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping
    public String test() {
        return "JWT Working";
    }

    @GetMapping("/emailAlreadyExist")
    public String testException() {
        throw new EmailAlreadyExistsException("Email Already Exist");
    }

    @GetMapping("/prductNotFound")
    public String testProductNotFound() {
        throw new ProductNotFoundException("Product Not Found");
    }
}
```

This is a **development/debugging controller** to verify that JWT authentication works and that the exception handling is configured correctly. It should be **removed or secured before going to production**.

---

## 22. Complete Request Lifecycle Diagrams

### Diagram 1: User Registration

```
Client                   AuthController          AuthService           DB
  │                            │                      │                │
  ├─POST /auth/register───────►│                      │                │
  │  {username,email,password} │                      │                │
  │                            ├─register(dto)────────►│               │
  │                            │                      ├─findByEmail────►│
  │                            │                      │◄───Optional────┤
  │                            │                      │                │
  │                            │                    [email exists?]    │
  │                            │                      │─NO─►           │
  │                            │                      │                │
  │                            │                      ├─BCrypt.encode()│
  │                            │                      ├─setRole(USER)  │
  │                            │                      ├─save(user)─────►│
  │                            │                      │◄───Users entity┤
  │                            │◄─────Users──────────┤                │
  │◄───200 OK Users JSON───────┤                      │                │
  │                            │                      │                │
```

### Diagram 2: User Login

```
Client                  AuthController     AuthService    JwtService    DB
  │                          │                 │              │          │
  ├─POST /auth/login────────►│                 │              │          │
  │  {email, password}       │                 │              │          │
  │                          ├─login(dto)──────►│             │          │
  │                          │                 ├─authManager.authenticate()
  │                          │                 │  └─loadUserByUsername───►│
  │                          │                 │  ◄──UserPrincipal────────┤
  │                          │                 │  └─BCrypt.matches()      │
  │                          │                 │  [PASS or BadCredentialsException]
  │                          │                 │              │          │
  │                          │                 ├─findByEmail─────────────►│
  │                          │                 │◄────Users────────────────┤
  │                          │                 ├─generateAccessToken──────►│
  │                          │                 │◄─accessToken─────────────┤ (JJWT, no DB)
  │                          │                 ├─generateRefreshToken──►   │
  │                          │                 │◄─refreshToken──────────   │
  │                          │                 ├─findByUser──────────────►│
  │                          │                 │  (delete old token if exists)
  │                          │                 ├─save(refreshTokenEntity)►│
  │                          │◄─AuthResponseDto┤              │          │
  │◄──{accessToken,──────────┤                 │              │          │
  │    refreshToken}         │                 │              │          │
```

### Diagram 3: Authenticated Request (e.g., GET /product/getAll/)

```
Client               JwtFilter           SecurityContext    Controller    Service    DB
  │                      │                     │                │          │         │
  ├─GET /product/getAll/ ►│                     │                │          │         │
  │  Authorization:       │                     │                │          │         │
  │  Bearer <accessToken> │                     │                │          │         │
  │                       │                     │                │          │         │
  │              [Extract Authorization header] │                │          │         │
  │              [Remove "Bearer " prefix]      │                │          │         │
  │              [validateAccessToken(token)]   │                │          │         │
  │              [Signature OK, Not Expired]    │                │          │         │
  │              [extractEmail(token)]          │                │          │         │
  │              [loadUserByUsername(email)]    │                │          │         │
  │              [Create UsernamePassword       │                │          │         │
  │               AuthenticationToken]          │                │          │         │
  │                       ├─setAuthentication──►│                │          │         │
  │                       ├─filterChain.doFilter()               │          │         │
  │                                             │                │          │         │
  │                           [AuthorizationFilter reads SecurityContext]    │         │
  │                           [@PreAuthorize checks role from SecurityContext]        │
  │                           [hasAnyRole('USER','ADMIN') → PASS]           │         │
  │                                                              │          │         │
  │                                             ├─getAllProducts()►│        │         │
  │                                             │                ├─getAllProducts()►  │
  │                                             │                │         ├─findAll()►│
  │                                             │                │         │◄─Page<Product>
  │                                             │                │◄─List<ProductResponseDto>
  │◄──200 OK [...]──────────────────────────────────────────────────────────│         │
```

### Diagram 4: Invalid/Missing Token Request

```
Client                JwtFilter         AuthorizationFilter    Client
  │                       │                     │                 │
  ├─GET /product/getAll/─►│                     │                 │
  │  (no Authorization    │                     │                 │
  │   header)             │                     │                 │
  │              [authHeader == null]            │                 │
  │              [filterChain.doFilter()]        │                 │
  │                       │                     │                 │
  │                       │            [No Authentication in SecurityContext]
  │                       │            [anyRequest().authenticated() rule]
  │                       │            [DENIED — user not authenticated]
  │◄──401 Unauthorized─────────────────────────────────────────────│
```

### Diagram 5: Token Refresh Flow

```
Client                  AuthController       AuthService      JwtService    DB
  │                          │                   │                │          │
  ├─POST /auth/refresh───────►│                  │                │          │
  │  {refreshToken: "..."}    │                  │                │          │
  │                           ├─refreshAccessToken(dto)──────────►│          │
  │                           │                  ├─findByToken─────────────►│
  │                           │                  │◄─RefreshToken entity─────┤
  │                           │                  ├─[check expiryDate]        │
  │                           │                  ├─validateRefreshToken(token)►│(JJWT)
  │                           │                  ├─extractEmailFromRefreshToken()
  │                           │                  ├─findByEmail─────────────►│
  │                           │                  │◄─Users──────────────────┤
  │                           │                  ├─generateAccessToken──────►│
  │                           │                  │◄─newAccessToken──────────┤
  │                           │◄─AuthResponseDto─┤                │          │
  │◄──{newAccessToken,────────┤                  │                │          │
  │    sameRefreshToken}      │                  │                │          │
```

---

## 23. Design Decisions & Why They Matter

### 1. Stateless Architecture (STATELESS Session Policy)

**Decision:** Use `SessionCreationPolicy.STATELESS`

**Why:** REST APIs should be stateless. Each request carries its own identity (the JWT). This allows:
- **Horizontal scaling:** Add more servers without a shared session store.
- **Microservices:** Other services can verify the same JWT without calling your auth server.
- **Simplicity:** No session expiration management on the server.

### 2. Two Separate JWT Secrets

**Decision:** `ACCESS_SECRET` and `REFRESH_SECRET` are different.

**Why:** Cryptographic isolation. An access token cannot be passed to the refresh endpoint and accepted as a refresh token (different signature). This limits the blast radius of a key compromise.

### 3. Storing Refresh Tokens in the Database

**Decision:** Save refresh tokens to the `refresh_token` table.

**Why:** Unlike access tokens (which are purely cryptographic and cannot be revoked before expiry), storing refresh tokens in the DB allows:
- Logout (delete the refresh token from DB — user cannot get new access tokens)
- Forced session invalidation (admin can delete tokens)
- Fraud detection (monitor for abnormal refresh patterns)

### 4. Role Assignment Locked to SERVER Side

**Decision:** `user.setRole(Role.USER)` regardless of what the client sends.

**Why:** Client input is untrusted. If the role came from the request body, any user could register as an ADMIN. This is a classic privilege escalation attack. The role is a server-side decision.

### 5. BCrypt with Cost Factor 11

**Decision:** `new BCryptPasswordEncoder(11)`

**Why:** Cost factor 11 means 2^11 = 2048 iterations. This makes each hash computation take ~100ms on a modern server — acceptable for a login (user won't notice), but catastrophic for an attacker trying to brute-force billions of combinations. Cost factor 12 is more common in production today.

### 6. Constructor Injection for Dependencies

**Decision:** Most classes use constructor injection (`final` fields).

**Why:** Immutability, testability, and explicitness. The one exception is `MyUserDetailsService` which uses `@Autowired` field injection — this should be made consistent.

---

## 24. Security Vulnerabilities & Improvements

### 🔴 Critical — Fix Before Production

**1. Hardcoded Secrets in Source Code**
```java
// Current (DANGEROUS):
private static final String ACCESS_SECRET = "shopCubeAccessSecret...";

// Fix: Use environment variables or Spring's @Value
@Value("${jwt.access.secret}")
private String accessSecret;
```
And in `application.properties`:
```properties
jwt.access.secret=${JWT_ACCESS_SECRET}  # From environment variable
```

**2. Register Endpoint Returns Full Entity (Password Exposed)**
The `/auth/register` endpoint returns the `Users` entity including `password` (hashed but still bad practice). Create a `UserResponseDto`.

**3. No Refresh Token Rotation**
Currently, the same refresh token is reused forever (for 14 days). Implement rotation: generate a new refresh token on each `/auth/refresh` call and invalidate the old one. This enables detection of refresh token theft.

**4. TestController in Production Code**
`TestController` should be removed from production code or protected with an ADMIN role. It exposes internal exception behavior to attackers.

### 🟡 Medium — Improve When Possible

**5. NullPointerException Bug in getAllProducts**
```java
// Bug:
sort = sort.by(sortBy).descending();  // sort is null!

// Fix:
sort = Sort.by(sortBy).descending();   // Capital S, static method
```

**6. Database Hit on Every Request in JwtFilter**
`userDetailsService.loadUserByUsername(email)` hits the database on every authenticated request. Consider caching this with Spring Cache or embedding more data in the JWT payload.

**7. Password Database Stored in application.properties**
The DB password `mano13105` is in plaintext in `application.properties`. Use environment variables:
```properties
spring.datasource.password=${DB_PASSWORD}
```

**8. Generic Exception Catching in validateAccessToken**
```java
} catch (Exception e) {
    return false;
}
```
Consider logging the exception type (without sensitive data) so you can monitor for suspicious activity (many `SignatureException` might indicate token forgery attempts).

### 🟢 Enhancements — Nice to Have

**9. Add Logout Endpoint**
```java
@PostMapping("/auth/logout")
public ResponseEntity<?> logout(@RequestBody RefreshTokenRequestDto dto) {
    refreshTokenRepo.findByToken(dto.getRefreshToken())
        .ifPresent(refreshTokenRepo::delete);
    return ResponseEntity.ok("Logged out");
}
```

**10. Add Rate Limiting to /auth/login**
Without rate limiting, attackers can try thousands of passwords. Add Spring's `RateLimiter` or a library like Bucket4j.

**11. JWT Claims Validation: Issuer & Audience**
Add `issuer` and `audience` claims to your JWT for better token validation:
```java
.issuer("shopCube")
.audience().add("shopCube-client")
```

---

## 25. Quick Reference Cheat Sheet

### Key Annotations Summary

| Annotation | Where Used | What It Does |
|---|---|---|
| `@Configuration` | `SecurityConfig` | Marks as bean configuration class |
| `@EnableWebSecurity` | `SecurityConfig` | Enables Spring Security web support |
| `@EnableMethodSecurity` | `SecurityConfig` | Enables `@PreAuthorize` annotations |
| `@PreAuthorize` | Controller methods | Method-level authorization check |
| `@Component` | `JwtFilter` | Makes it a Spring-managed bean |
| `@Service` | Services | Business logic bean |
| `@Repository` | Repos | Data access bean |
| `@RestController` | Controllers | REST controller + JSON response |
| `@RestControllerAdvice` | `GlobalExceptionHandler` | Global exception handler |
| `@ExceptionHandler` | Handler methods | Handles specific exception type |
| `@Bean` | Config methods | Declares a Spring bean |

### Key Classes Summary

| Class | Package | Role |
|---|---|---|
| `SecurityConfig` | `config` | Configures the entire security setup |
| `JwtFilter` | `security` | Intercepts every request, validates JWT |
| `JwtService` | `security` | Creates, validates, parses JWTs |
| `UserPrincipal` | `security` | Adapter: `Users` → Spring Security's `UserDetails` |
| `MyUserDetailsService` | `security` | Loads `UserPrincipal` from database by email |
| `AuthService` | `service` | Business logic: register, login, refresh |
| `GlobalExceptionHandler` | `exception` | Converts exceptions to structured JSON errors |

### API Quick Reference

```
POST /auth/register      → Register (no token needed)
POST /auth/login         → Login (no token needed) → returns {accessToken, refreshToken}
POST /auth/refresh       → Get new access token (no token needed) → returns {newAccessToken, refreshToken}

GET    /product/get/{id}            → Get one product (any authenticated user)
GET    /product/getAll/?search=...  → List products paginated (USER or ADMIN)
GET    /product/get/category/{cat}  → Filter by category (USER or ADMIN)
GET    /product/get/name/{name}     → Search by name (any authenticated user)
POST   /product/addProducts         → Add products (ADMIN only)
PUT    /product/update/{id}         → Update product (ADMIN only)
DELETE /product/remove/{id}         → Delete product (ADMIN only)

GET /test                           → "JWT Working" (any authenticated user)
```

### JWT in Every Request

```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtYW5vQGdtYWlsLmNvbSIsInJvbGUiOiJVU0VSIiwiaWF0Ijo...
```

### BCrypt Password Flow

```
Registration: password → BCrypt.encode() → "$2a$11$..." stored in DB
Login:        password → BCrypt.matches("$2a$11$...", password) → true/false
                         (Spring AuthenticationManager does this automatically)
```

### Complete Security Flow Summary

```
Every Request:
  1. JwtFilter.doFilterInternal() runs
  2. Extract "Authorization: Bearer <token>" header
  3. If no token → pass to next filter (permitAll endpoints will succeed, others will get 401)
  4. Validate token signature + expiry using JwtService
  5. If invalid → pass to next filter (will get 401)
  6. Extract email from token
  7. Load UserPrincipal from DB via MyUserDetailsService
  8. Create UsernamePasswordAuthenticationToken with authorities
  9. Set it in SecurityContextHolder
  10. Pass to next filter
  11. AuthorizationFilter checks .authenticated() or @PreAuthorize rules
  12. If authorized → reach the @RestController method
  13. If unauthorized → 403 Forbidden
```

---

*Document generated from a complete analysis of the ShopCube Spring Boot project codebase.*  
*Spring Boot: 4.1.0 | Java: 21 | JJWT: 0.12.7 | Database: PostgreSQL*  
*Package: `me.mano.shopCube`*
