# Enterprise Authorization (AuthZ) Architecture Guide: End-to-End Request Flows & Models

In distributed microservice architectures, authorization is decoupled from authentication. While Authentication (AuthN) proves user identity, Fine-Grained Authorization (AuthZ) enforces access control boundaries across API gateways and domain microservices.

This specification details the end-to-end request lifecycle—from initial token generation at the Authorization Server to final resource delivery at the domain layer—across the four primary enterprise authorization models.

---

## Part 1: The Universal Token Issuance Flow

Regardless of which authorization model a downstream service uses, the security lifecycle begins at the **Authorization Server (Identity Provider / IdP)**, such as Keycloak, Auth0, or Spring Authorization Server. 

The Auth Server is the single source of truth for user identity. It authenticates credentials, queries directory services (LDAP/SQL) for user metadata, and cryptographically signs a JSON Web Token (JWT) using an asymmetric private key (e.g., RS256) or symmetric secret (e.g., HS256).

```
+--------+            +-------------------+            +-------------+
| Client |            |   Auth Server     |            |  Directory  |
| (App)  |            |   (IdP / Issuer)  |            |  (SQL/LDAP) |
+---+----+            +---------+---------+            +------+------+
    |                           |                             |
    | 1. POST /oauth2/token     |                             |
    |    (Credentials / Code)   |                             |
    |-------------------------->| 2. Fetch User & Attributes  |
    |                           |---------------------------->|
    |                           | 3. Return Roles/Metadata    |
    |                           |<----------------------------|
    |                           |                             |
    |                           | 4. Build Claims & Sign JWT  |
    |                           |    (using Private Key)      |
    |                           |                             |
    | 5. Return Signed JWT      |                             |
    |<--------------------------|                             |
    |    {"access_token": "..."}|                             |
```

### Component Responsibilities During Issuance
* **Client Application:** Transmits user credentials or an OAuth 2.0 authorization code to the Auth Server's `/token` endpoint over TLS.
* **Auth Server / Identity Provider:** Authenticates the user, retrieves their assigned roles, permissions, or structural attributes from the persistent store, packages these values into standard JWT claims (`sub`, `iss`, `exp`, `jti`, and custom authorization payloads), and signs the token.
* **Public Key Infrastructure (JWKS):** The Auth Server exposes a read-only JSON Web Key Set (`/oauth2/jwks`) endpoint. API Gateways and microservices cache these public keys to validate JWT signatures locally without making network calls back to the IdP.

---

## Part 2: Deep-Dive into the Four Authorization Models

### 1. Standard Role-Based Access Control (RBAC)

#### Architectural Concept & Enterprise Use Case
Standard RBAC maps users directly to coarse-grained role strings (e.g., `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_USER`). Access decisions are evaluated by checking if the authenticated user possesses the specific role required by an endpoint.

* **Pros:** Minimal computational overhead; simple implementation; human-readable security audits.
* **Cons:** Causes **Role Explosion** in large enterprises. When edge cases arise (e.g., a Manager who needs read-only access to audit logs but cannot approve expenses), architects are forced to create compound roles (`ROLE_MANAGER_AUDIT_READONLY`), leading to brittle, unmaintainable application code.
* **When to Use:** Monolithic applications, early-stage MVPs, internal administrative tools, or systems with strictly static, non-overlapping user tiers.

#### JWT Payload Structure
```json
{
  "sub": "usr_88392",
  "iss": "[https://auth.enterprise.com](https://auth.enterprise.com)",
  "exp": 1784030000,
  "roles": [
    "ROLE_FINANCE_MANAGER",
    "ROLE_EMPLOYEE"
  ]
}
```

#### End-to-End Request Flow
```
+--------+      +-------------+      +-------------------+      +-------------+
| Client |      | API Gateway |      | Finance Service   |      |  Database   |
+---+----+      +------+------+      +---------+---------+      +------+------+
    |                  |                       |                       |
    | 1. GET /invoices |                       |                       |
    |    Auth: Bearer  |                       |                       |
    |----------------->| 2. Validate JWT Sig   |                       |
    |                  |    (via cached JWKS)  |                       |
    |                  |                       |                       |
    |                  | 3. Route Request +    |                       |
    |                  |    Forward JWT Header |                       |
    |                  |---------------------->| 4. Extract 'roles'    |
    |                  |                       |    claim from JWT     |
    |                  |                       |                       |
    |                  |                       | 5. Evaluate Rule:     |
    |                  |                       |    hasRole('MANAGER') |
    |                  |                       |                       |
    |                  |                       | 6. SELECT * FROM inv  |
    |                  |                       |---------------------->|
    |                  |                       | 7. Return Data Rows   |
    |                  |                       |<----------------------|
    |                  | 8. Return JSON Data   |                       |
    |<-----------------|-----------------------|                       |
```

#### Hop-by-Hop Responsibilities
* **Client:** Attaches the JWT to the HTTP request: `Authorization: Bearer <token>`.
* **API Gateway:** Intercepts the request. It performs stateless validation: checks the signature against the Auth Server's public JWKS, verifies the token hasn't expired (`exp`), and ensures the issuer (`iss`) is trusted. If valid, it forwards the request downstream.
* **Finance Microservice:** A security interceptor (e.g., Spring Security `BearerTokenAuthenticationFilter`) parses the JWT and populates the local security context. The controller method annotated with `@PreAuthorize("hasRole('FINANCE_MANAGER')")` executes. If the role string matches, the database transaction executes; otherwise, the service throws a `403 Forbidden` exception.

---

### 2. Permission-Based / Fine-Grained RBAC

#### Architectural Concept & Enterprise Use Case
Permission-Based RBAC introduces an abstraction layer between roles and code. Roles are defined as collections of atomic **Permissions** (e.g., `invoice:create`, `invoice:read`, `invoice:approve`). When generating the JWT, the Auth Server flattens the user's assigned roles into an array of explicit permission strings. Application code only checks permissions, never roles.

* **Pros:** Eliminates code refactoring when organizational structures change. If an "Auditor" role needs access to view invoices, administrators simply assign the `invoice:read` permission to the `AUDITOR` role in the database; zero microservice redeployments are required.
* **Cons:** Risk of **Token Bloat**. If a power-user accumulates 500 distinct permissions, the JWT header size increases dramatically, adding network latency and exceeding HTTP header limits on proxies.
* **When to Use:** The industry standard for distributed SaaS platforms, multi-tenant architectures, and REST APIs requiring distinct feature-toggle authorization.

#### JWT Payload Structure
```json
{
  "sub": "usr_88392",
  "iss": "[https://auth.enterprise.com](https://auth.enterprise.com)",
  "exp": 1784030000,
  "authorities": [
    "invoice:read",
    "invoice:create",
    "invoice:approve",
    "report:export"
  ]
}
```

#### End-to-End Request Flow
```
+--------+      +-------------+      +-------------------+      +-------------+
| Client |      | API Gateway |      | Finance Service   |      |  Database   |
+---+----+      +------+------+      +---------+---------+      +------+------+
    |                  |                       |                       |
    | 1. PUT /inv/102  |                       |                       |
    |    Auth: Bearer  |                       |                       |
    |----------------->| 2. Validate Sig & Exp |                       |
    |                  |                       |                       |
    |                  | 3. Coarse Scope Check:|                       |
    |                  |    Has 'invoice:*' ?  |                       |
    |                  |                       |                       |
    |                  | 4. Strip JWT & Inject |                       |
    |                  |    X-User-Authorities |                       |
    |                  |---------------------->| 5. Parse Authorities  |
    |                  |                       |    from HTTP Header   |
    |                  |                       |                       |
    |                  |                       | 6. Evaluate Rule:     |
    |                  |                       |    hasAuthority(      |
    |                  |                       |    'invoice:approve') |
    |                  |                       |                       |
    |                  |                       | 7. UPDATE invoices    |
    |                  |                       |---------------------->|
    |                  |                       | 8. Transaction OK     |
    |                  |                       |<----------------------|
    |                  | 9. 200 OK / Success   |                       |
    |<-----------------|-----------------------|                       |
```

#### Hop-by-Hop Responsibilities
* **API Gateway:** Acts as a **Policy Enforcement Point (PEP)** at the network perimeter. In addition to signature validation, the Gateway can execute coarse-grained authorization routing rules (e.g., rejecting requests to `/invoices/**` if the JWT lacks any `invoice:*` scope). To save internal bandwidth and prevent downstream services from re-parsing cryptography, the Gateway extracts the `authorities` array and forwards it via a trusted internal header: `X-User-Authorities: invoice:read,invoice:create,invoice:approve`.
* **Finance Microservice:** Trusts internal network headers injected by the Gateway. Method security enforces fine-grained checks: `@PreAuthorize("hasAuthority('invoice:approve')")`. The service operates entirely agnostic of what user role granted the permission.

---

### 3. Attribute-Based Access Control (ABAC)

#### Architectural Concept & Enterprise Use Case
ABAC abandons static tokens and permission strings in favor of dynamic, boolean logic rules evaluated at runtime. Access is granted if a policy script evaluates successfully against four attribute dimensions:
1. **Subject Attributes (Who):** Department, clearance level, job title, spending limit.
2. **Resource Attributes (What):** Owner ID, classification level, dollar amount, departmental tag.
3. **Action Attributes (How):** `READ`, `WRITE`, `APPROVE`, `DELETE`.
4. **Environment Attributes (Context):** Current server time, client IP address, device posture (managed/unmanaged), geographic location.

* **Pros:** Highly dynamic and context-aware. Enables declarative business rules (e.g., *"A manager can only approve invoices up to their personal approval limit, within their own department, from a corporate IP address during business hours"*).
* **Cons:** High architectural complexity and runtime latency. Requires integrating an external **Policy Decision Point (PDP)** like Open Policy Agent (OPA) and querying domain databases to resolve resource attributes before executing business logic.
* **When to Use:** Highly regulated environments (Banking, Defense, Healthcare/HIPAA), systems with strict Separation of Duties (SoD), or complex workflows where static permissions cannot capture domain rules.

#### JWT Payload Structure
*Note: In ABAC, the JWT carries core identity and subject attributes, not permissions.*
```json
{
  "sub": "emp_10492",
  "iss": "[https://auth.enterprise.com](https://auth.enterprise.com)",
  "exp": 1784030000,
  "attributes": {
    "department": "FINANCE",
    "clearance_level": 3,
    "approval_limit_usd": 50000
  }
}
```

#### End-to-End Request Flow
```
+--------+    +-------------+    +-----------------+    +------------+    +-----------+
| Client |    | API Gateway |    | Finance Service |    | OPA Engine |    | Database  |
|        |    |    (PEP)    |    |   (Domain PEP)  |    |   (PDP)    |    |  (PIP)    |
+---+----+    +------+------+    +--------+--------+    +-----+------+    +-----+-----+
    |                |                    |                   |                 |
    | 1. POST        |                    |                   |                 |
    |    /inv/99/appr|                    |                   |                 |
    |--------------->| 2. Validate JWT    |                   |                 |
    |                |------------------->| 3. Intercept Req  |                 |
    |                |                    |                   |                 |
    |                |                    | 4. SELECT * FROM  |                 |
    |                |                    |    invoices       |                 |
    |                |                    |    WHERE id = 99  |                 |
    |                |                    |------------------------------------>|
    |                |                    | 5. Return Resource                  |
    |                |                    |    (amount=$10k,                    |
    |                |                    |     dept=FINANCE)                   |
    |                |                    |<------------------------------------|
    |                |                    |                   |                 |
    |                |                    | 6. POST /v1/data/expense/approve    |
    |                |                    |    {"subject": {"limit": 50000...}, |
    |                |                    |     "resource": {"amount": 10000...}}|
    |                |                    |------------------>|                 |
    |                |                    |                   | 7. Evaluate     |
    |                |                    |                   |    Rego Policy  |
    |                |                    |                   |                 |
    |                |                    | 8. {"allow": true}|                 |
    |                |                    |<------------------|                 |
    |                |                    |                   |                 |
    |                |                    | 9. Execute State Change             |
    |                |                    |------------------------------------>|
    |                |                    | 10. 200 OK / Approved               |
    |<---------------|--------------------|                                     |
```

#### Hop-by-Hop Responsibilities
* **API Gateway:** Performs standard JWT signature and expiration validation. Forwards subject attributes downstream via headers (`X-User-Dept`, `X-User-Approval-Limit`).
* **Finance Microservice (Domain PEP):** Intercepts the request before business logic executes. To evaluate ABAC, it must act as a **Policy Enforcement Point**. It queries the local database (**Policy Information Point / PIP**) to load the target domain object (`Invoice #99`).
* **Open Policy Agent / OPA Engine (PDP):** The microservice makes a sub-millisecond HTTP/gRPC call to a co-located OPA sidecar container. It passes a structured JSON document containing the combined Subject, Resource, Action, and Environment attributes. OPA evaluates its written **Rego** policies against the JSON input and returns a definitive boolean decision (`allow: true` or `allow: false`). If allowed, the microservice commits the database transaction.

---

### 4. Relationship-Based Access Control (ReBAC)

#### Architectural Concept & Enterprise Use Case
Derived from Google’s **Zanzibar** specification, ReBAC models authorization as a directed graph of relationships between objects and subjects. Instead of assigning attributes or permissions to a user, permissions are inferred by traversing graph edges: ownership, parent-child folder structures, team memberships, or collaborative shares. 

* **Pros:** Handles deeply nested hierarchies, arbitrary object sharing, and multi-tenant organizational structures with sub-second traversal times. Scaling is decoupled from token size.
* **Cons:** Requires running and maintaining a specialized, highly available graph database or tuple store (e.g., OpenFGA, Authzed/SpiceDB). Designing non-looping schema models requires a shift from relational database thinking to graph theory.
* **When to Use:** Collaborative document platforms (Google Workspace, Notion, Figma), social networks, cloud infrastructure management consoles (AWS IAM, GCP Resource Manager), or any domain where users dynamically share resources with specific teams or individuals.

#### JWT Payload Structure
*Note: ReBAC tokens are minimalist. They only require an immutable, globally unique user identifier. All relationship data resides inside the graph database.*
```json
{
  "sub": "usr_99201",
  "iss": "[https://auth.enterprise.com](https://auth.enterprise.com)",
  "exp": 1784030000
}
```

#### End-to-End Request Flow
```
+--------+    +-------------+    +------------------+    +-----------------+    +-----------+
| Client |    | API Gateway |    | Document Service |    | ReBAC Engine    |    | Database  |
|        |    |             |    |                  |    | (OpenFGA/Zanz.) |    | (Content) |
+---+----+    +------+------+    +--------+---------+    +--------+--------+    +-----+-----+
    |                |                    |                       |                   |
    | 1. GET         |                    |                       |                   |
    |    /doc/771/vw |                    |                       |                   |
    |--------------->| 2. Validate JWT    |                       |                   |
    |                |------------------->| 3. Extract User ID    |                   |
    |                |                    |    (usr_99201)        |                   |
    |                |                    |                       |                   |
    |                |                    | 4. Check Permission   |                   |
    |                |                    |    check(usr_99201,   |                   |
    |                |                    |          can_view,    |                   |
    |                |                    |          doc:771)     |                   |
    |                |                    |---------------------->|                   |
    |                |                    |                       | 5. Traverse Graph:|
    |                |                    |                       |    doc:771 ->     |
    |                |                    |                       |    folder:22 ->   |
    |                |                    |                       |    team:eng ->    |
    |                |                    |                       |    usr_99201      |
    |                |                    |                       |                   |
    |                |                    | 6. {"allowed": true}  |                   |
    |                |                    |<----------------------|                   |
    |                |                    |                                           |
    |                |                    | 7. SELECT content FROM docs WHERE id=771  |
    |                |                    |------------------------------------------>|
    |                |                    | 8. Return Document Payload                |
    |                |                    |<------------------------------------------|
    |                |                    | 9. 200 OK / Document JSON                 |
    |<---------------|--------------------|                                           |
```

#### Hop-by-Hop Responsibilities
* **API Gateway:** Validates the JWT signature and expiration. Forwards the user's canonical identifier (`X-User-Id: usr_99201`) to the Document Microservice.
* **Document Microservice:** Receives the request to view `doc:771`. *Before* executing any database queries against the primary document store, the service issues an RPC check call to the external ReBAC engine: `check(subject: "user:usr_99201", relation: "can_view", object: "doc:771")`.
* **ReBAC Graph Engine (Zanzibar / OpenFGA):** The engine accesses its specialized tuple store (storing relations like `doc:771 #parent@folder:22`, `folder:22 #viewer@team:eng#member`, and `team:eng #member@user:usr_99201`). It performs an in-memory graph traversal to determine if a valid path exists between the subject and the target relation. It returns a boolean allow/deny decision in single-digit milliseconds. If true, the microservice fetches the actual document content from PostgreSQL/MongoDB and returns it to the client.

---

## Part 3: Comparative Architectural Matrix

| Metric / Dimension | Standard RBAC | Permission-Based RBAC | ABAC | ReBAC |
| :--- | :--- | :--- | :--- | :--- |
| **Primary Authorization Factor** | Static User Role | Atomic Permission String | Dynamic Context Attributes | Graph Relationship Tuples |
| **JWT Payload Size** | Small (1–5 role claims) | Medium to Large (10–500 authority strings) | Small (Subject attributes only) | Minimalist (Subject ID only) |
| **Policy Storage Location** | Application Code / Config | API Gateway & Service Security Annotations | Centralized Policy Store (Rego / XACML scripts) | Dedicated Tuple Database (Zanzibar / OpenFGA) |
| **Runtime Evaluation Latency** | Ultra-Low (<1 ms) | Very Low (1–2 ms) | Moderate (5–20 ms, requires database I/O for PIP) | Low (2–5 ms, optimized in-memory graph traversal) |
| **Scaling Limitation** | **Role Explosion** when edge-case rules multiply | **Token Bloat** and HTTP header limits | **Engine Overhead** and complex policy debugging | **Infrastructure Cost** of running a distributed graph store |
| **Best Target Use Case** | Internal tools, simple MVPs, static tiers | Multi-tenant SaaS, standard microservice APIs | Banking, Healthcare, complex compliance (HIPAA/GDPR) | Document sharing, social networks, dynamic hierarchy systems |

---

## Part 4: Enterprise Implementation Guidelines

> **The Perimeter vs. Domain Rule:** Never implement fine-grained ABAC or ReBAC rules at the API Gateway. The Gateway should remain stateless and fast, enforcing coarse-grained perimeter security (e.g., signature validation, expiration, and high-level OAuth scopes). Pass authenticated identity headers downstream, and let domain microservices evaluate fine-grained resource rules where the data natively resides.

> **Zero-Trust Header Forwarding:** When an API Gateway strips a JWT and forwards user identity via HTTP headers (`X-User-Id`, `X-User-Authorities`), downstream microservices must be network-isolated. Ensure microservices reject these headers if they originate from untrusted external IPs, accepting them only from the Gateway's internal subnet or via mutual TLS (mTLS).

> **Hybrid Enforcement is the Reality:** Mature enterprise architectures rarely rely on a single model. A standard production deployment uses **Permission-Based RBAC at the Gateway** to reject unauthorized requests at the perimeter, combined with **ABAC or ReBAC inside domain services** to verify row-level ownership and dynamic business compliance.

---
*Reference Specification End — Suitable for inclusion in architectural RFCs and system design documentation.* 