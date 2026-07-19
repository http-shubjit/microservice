====================================================================
REQUEST EXECUTION FLOW: CLASS, BEAN, AND SEQUENCE
====================================================================

1. THE SECURITY WRAPPER (Runs First)
   Container: springSecurityFilterChain (WebFilter)
--------------------------------------------------------------------
Order   | Filter Class              | Bean / Implementation Used
--------------------------------------------------------------------
1       | AuthenticationWebFilter   | Invokes your 'ReactiveJwtDecoder' Bean 
        |                           | & 'TokenBlacklistService' Bean.
2       | AuthorizationWebFilter    | Uses 'GatewaySecurityConfig' (SecurityWebFilterChain Bean).

- RESULT: If this chain finishes successfully, user identity is 
  stored in the 'ReactiveSecurityContext'. If it fails, the request 
  is rejected immediately (401/403) and NOTHING ELSE below runs.


2. THE GATEWAY ENGINE (Runs Second)
   Container: FilteringWebHandler (GatewayFilterChain)
--------------------------------------------------------------------
Order   | Filter Class              | Bean / Implementation Used
--------------------------------------------------------------------
1       | DownstreamHeaderFilter    | Your Custom Class (@Order(-1)).
2       | RouteToRequestUrlFilter   | Built-in (Reads application.properties).
3       | NettyRoutingFilter        | Built-in (Order MAX_VALUE).

- RESULT: The request is now modified (headers added), routed 
  to the correct URL, and sent over the network.


--------------------------------------------------------------------
DETAILED SEQUENCE & CLASS RESPONSIBILITIES
--------------------------------------------------------------------

Step 1: springSecurityFilterChain (AuthenticationWebFilter)
   - Responsibility: Identify the user.
   - Bean: 'ReactiveJwtDecoder'.
   - Method: It calls your bean to decode the JWT and your 
     'TokenBlacklistService' to check if the token is revoked.

Step 2: springSecurityFilterChain (AuthorizationWebFilter)
   - Responsibility: Check permissions.
   - Bean: 'GatewaySecurityConfig' (SecurityWebFilterChain bean).
   - Method: Validates the request path (e.g., /login vs /api/data) 
     against the identity verified in Step 1.

Step 3: GatewayFilterChain (DownstreamHeaderFilter)
   - Responsibility: Modify the request.
   - Class: 'DownstreamHeaderFilter' (Your class).
   - Method: Executes 'filter()' method; extracts identity from 
     'ReactiveSecurityContext' and injects X-User-Email header.

Step 4: GatewayFilterChain (RouteToRequestUrlFilter)
   - Responsibility: Resolve destination.
   - Config: 'application.properties'.
   - Method: Reads properties to match the path to a service URL.

Step 5: GatewayFilterChain (NettyRoutingFilter)
   - Responsibility: Dispatch.
   - Method: Opens non-blocking connection to the downstream service.
====================================================================