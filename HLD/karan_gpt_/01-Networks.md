# Complete Networking for HLD Interviews

Based on Karan Pratap Singh's [System Design Handbook](https://github.com/karanpratapsingh/system-design/blob/main/README.md?utm_source=chatgpt.com) • SDE-1 / SDE-2 interview preparation

I've examined the repository's table of contents and its networking-related sections across Chapters I–IV, along with the practical system designs in Chapter V.

One important distinction: knowing what a networking concept means is different from knowing when to introduce it into a system design.

For instance, knowing that WebSockets provide bidirectional communication isn't enough. In an HLD interview, you should be able to explain:

- Why WhatsApp needs persistent connections.
- How messages travel between users connected to different servers.
- What happens when a WebSocket server crashes.
- Why Kafka or Redis might be necessary even after you've introduced WebSockets.
- How you would prevent lost or duplicate messages.

That's the depth we'll aim for.

## 1. Networking concepts identified in the repository

I've grouped the networking and network-adjacent concepts into six learning areas, rather than simply following the repository's chapter order.

| Area                    | Concepts                                                                     | HLD importance |
| ----------------------- | ---------------------------------------------------------------------------- | -------------- |
| Networking fundamentals | IP, IPv4/IPv6, OSI, TCP, UDP, DNS                                            | High           |
| Traffic routing         | Load balancing, reverse/forward proxy, CDN                                   | Very high      |
| API communication       | HTTP, REST, GraphQL, gRPC, API Gateway, BFF                                  | Very high      |
| Real-time communication | Polling, long polling, WebSockets, SSE                                       | Very high      |
| Distributed networking  | Service discovery, service mesh, clustering, circuit breakers, rate limiting | Very high      |
| Network security        | TLS, SSL, mTLS, OAuth 2.0, OIDC, SSO                                         | High           |

I'll also cover closely related concepts from the repository—caching, queues, pub/sub, event-driven communication, geohashing, availability, and disaster recovery—where they affect networking decisions.

The main worked HLD problems will be:

1. Design a URL Shortener — DNS, HTTPS, load balancing, Redis and redirects.
2. Design WhatsApp — WebSockets, real-time message delivery, service discovery and Kafka.
3. Design Netflix — CDN, HTTP streaming, caching and global traffic routing.
4. Design Uber — real-time location updates, geohashing and location fan-out.
5. Design a Flash Sale System — rate limiting, high concurrency, backpressure and failure handling.
6. Design a Microservices API Platform — API Gateway, REST, GraphQL, gRPC, TLS and mTLS.

For each concept, I'll explain how it works, when to use it, what technical decision to make, and how to defend that decision in an interview.

# Part I — Networking fundamentals

## 2. IP addresses: How machines find one another

An IP address identifies a network interface on an IP network and allows packets to be routed toward their destination.

Imagine a shopping application with these components:

```
User's phone
    |
    v
Public API endpoint
    |
    v
Private application server
    |
    v
Private database server
```

Each component needs a way to communicate with the next.

### Four IP classifications you must understand

| Type       | Meaning                                      | HLD use                                      |
| ---------- | -------------------------------------------- | -------------------------------------------- |
| Public IP  | Globally routable address                    | Internet-facing endpoints                    |
| Private IP | Used within private networks                 | Backend servers and databases                |
| Static IP  | Fixed or stable assignment                   | Fixed endpoint and allowlisting requirements |
| Dynamic IP | Automatically assigned, potentially changing | Autoscaled application instances             |

IPv4 vs IPv6: IPv4 uses 32-bit addresses, whereas IPv6 uses 128-bit addresses. A globally accessible API may support both through DNS A and AAAA records.

### Interview application: Design a secure e-commerce backend

Suppose an interviewer asks:

Design the backend architecture of an e-commerce platform that serves millions of users.

You should not give every EC2 instance a public IP address.

Instead, use a network layout such as:

Public Internet

Customers accessing shop.example.com

HTTPS

Public subnets

Public Load Balancer

Accepts customer traffic

Private application subnets

App Server A

Private IP

App Server B

Private IP

Private Database

Accessible only by authorized backend components

How the solution works:

1. A customer connects to the public endpoint of the load balancer.
2. The load balancer forwards the request to an application instance over private networking.
3. The application accesses the database using a private endpoint.
4. Security groups and network policies restrict which components may communicate.
5. If an application instance is replaced, its IP can change without affecting customers because the load balancer and service discovery handle routing.

Why this architecture? It reduces the publicly exposed attack surface, supports autoscaling, and separates network access from application identity.

An important distinction: a private IP alone is not a security mechanism. You still need firewall rules, authentication, and encryption where appropriate.

Interview takeaway: IP networking matters when discussing public vs private infrastructure, multi-AZ deployments, network isolation, traffic routing, and service connectivity.

## 3. OSI model: Understanding where networking problems occur

The OSI model divides communication into seven conceptual layers.

| Layer           | Responsibility                             | Example             |
| --------------- | ------------------------------------------ | ------------------- |
| 7. Application  | Application protocols                      | HTTP, DNS, gRPC     |
| 6. Presentation | Encoding, encryption, compression concepts | TLS, JSON, Protobuf |
| 5. Session      | Communication sessions                     | Session management  |
| 4. Transport    | End-to-end transport                       | TCP, UDP            |
| 3. Network      | IP addressing and routing                  | IPv4, IPv6          |
| 2. Data Link    | Local network frames                       | Ethernet            |
| 1. Physical     | Transmitting signals                       | Fiber, Wi-Fi radio  |

Real-world TCP/IP stacks don't follow all seven boundaries literally. Use OSI as a reasoning framework rather than as a strict implementation diagram.

### Interview application: Design an API handling massive traffic

Suppose your service is experiencing high latency.

You need to determine where the latency originates.

Layer 3: Are packets taking inefficient routes? Is there a network connectivity issue?

Layer 4: Are TCP connections being repeatedly established? Are connections saturating or being reset?

Layer 7: Are backend endpoints slow? Is JSON serialization expensive? Are database queries causing delays?

This also explains two frequently discussed load balancers:

- A Layer 4 load balancer forwards traffic primarily using transport/network information such as IP addresses and ports.
- A Layer 7 load balancer understands HTTP and can route using hostnames, paths, headers, or other application information.

You should be able to choose the correct type instead of blindly writing "load balancer" in every architecture.

## 4. TCP vs UDP: Reliability versus timely delivery

### TCP internally

TCP establishes a connection using the three-way handshake:

The TCP three-way handshake establishes connection state before application data is exchanged.

After connection establishment, TCP provides an ordered byte stream using sequence numbers, acknowledgments, retransmissions, flow control, and congestion control.

TCP is appropriate when missing or misordered bytes are unacceptable.

UDP does not establish this kind of transport connection. It sends independent datagrams without TCP-style ordering or retransmission.

However, an application can implement reliability above UDP, as QUIC does.

### Interview application: Design Zoom

Requirements: Two users are on a live video call. They need low latency and reasonably smooth audio/video.

Suppose a frame is delayed by 800 milliseconds.

Would you prefer to retransmit that stale frame or show the newest frame?

For an interactive call, fresh media is usually more valuable than an old frame arriving perfectly.

Recommended solution:

1. Capture video/audio on each participant's device.
2. Encode and compress the media.
3. Send the media using WebRTC, usually with UDP-based media transport.
4. Use an SFU (Selective Forwarding Unit) for multiparty calls so users do not need to maintain a separate full media stream for every participant.
5. Adapt bitrate and quality using congestion feedback.
6. Use packet-loss recovery techniques such as retransmission when timely, forward error correction, or concealment.
7. Fall back to TCP/TLS-based transport when networks block UDP, accepting possible performance costs.

Why not simply TCP? TCP's ordered delivery can cause head-of-line blocking: later bytes wait for lost earlier bytes to be retransmitted.

Why not raw UDP? You would need to build congestion control, security, session negotiation, and media handling yourself.

A useful distinction is that Netflix streaming and Zoom calling are different networking problems. Netflix can buffer future video segments; Zoom needs very low conversational latency.

| System                    | Typical choice           | Reason                                          |
| ------------------------- | ------------------------ | ----------------------------------------------- |
| Banking API               | HTTPS over TCP or QUIC   | Correct, secure request processing              |
| WhatsApp messages         | WebSockets over TCP      | Reliable, ordered transport within a connection |
| Zoom live media           | WebRTC, commonly UDP     | Low-latency interactive delivery                |
| Netflix video playback    | HTTP over TCP or QUIC    | Segmented delivery and buffering                |
| Multiplayer game movement | Often UDP-based protocol | Latest state may supersede old state            |
| File transfer             | TCP or QUIC              | Data integrity and reliable delivery            |

One correction to a common interview misconception: TCP guarantees reliable ordered transport while the connection operates, not exactly-once business processing. A network failure can leave a client uncertain whether the server processed its request.

For payments and orders, you still need idempotency and application-level acknowledgments.

# Part II — DNS, load balancing, proxies, and CDNs

These concepts answer four different questions:

- DNS: Where should the client connect?
- Load balancer: Which healthy server should process the request?
- Reverse proxy: How should incoming application traffic be handled and forwarded?
- CDN: Can content be served closer to the user without contacting the origin?

## 5. DNS — Domain Name System

DNS translates a domain such as `api.example.com` into the information needed to locate a server or service.

### How DNS resolution works

When you open `https://example.com`, the following may happen:

1. The browser or operating system checks for a cached DNS answer.
2. If one isn't available, a recursive resolver is queried.
3. The resolver checks its cache, then obtains DNS delegation information from root and TLD nameservers when necessary.
4. The authoritative nameserver provides the relevant record.
5. The resolver returns the answer to the client.
6. The client connects to the resolved endpoint.

The hierarchy looks like:

```
Client
  |
  v
Recursive DNS Resolver
  |
  +--> Root Nameserver
  |       |
  |       v
  +--> .com TLD Nameserver
  |       |
  |       v
  +--> example.com Authoritative DNS
  |
  v
IP address / DNS answer
```

This full lookup doesn't occur for every HTTP request because DNS answers are cached.

### DNS record types relevant to HLD

| Record | Purpose                              | Example                          |
| ------ | ------------------------------------ | -------------------------------- |
| A      | Domain → IPv4                        | `api.example.com → 203.0.113.10` |
| AAAA   | Domain → IPv6                        | IPv6-enabled API                 |
| CNAME  | Domain → another hostname            | `www → edge.example.net`         |
| NS     | Delegates nameserver authority       | Managed DNS                      |
| MX     | Mail server routing                  | Email infrastructure             |
| TXT    | Text-based verification and policies | SPF, DKIM-related records        |
| SRV    | Service hostname and port            | Some service-discovery protocols |
| PTR    | Reverse DNS lookup                   | Email infrastructure             |

DNS TTL controls how long an answer may be cached. A short TTL can make updates visible sooner, but it cannot guarantee an instantaneous global switch.

### HLD question: Design a multi-region application

Scenario: Your application serves users in India, Europe, and the US. Users should ideally access a nearby healthy region.

Proposed architecture:

```
                   Global DNS
                  /    |     \
                 /     |      \
                v      v       v
            India    Europe    US
             LB        LB      LB
              |         |       |
          App Cluster  Cluster  Cluster
```

We can use latency-aware or geographically informed DNS routing to direct clients to an appropriate regional endpoint.

For example:

- A Delhi user may be routed to an Indian region.
- A German user may be routed to a European region.
- If the Indian region becomes unhealthy, traffic may be directed to a healthy alternative.

However, DNS routing alone is not complete regional failover.

We need:

1. Health checks to stop returning unhealthy regional endpoints.
2. A plan for clients holding cached DNS answers.
3. Connection retry and re-resolution logic.
4. Database replication or a clear strategy for data ownership across regions.
5. An explicit decision about data consistency, recovery time, and acceptable data loss.

DNS tells the client where to go. It does not automatically replicate the application state required to operate there.

Interview answer: "I'd use a managed global DNS service with health-aware routing and regional load balancers. I'd also account for DNS caching and ensure application data is available in the failover region before sending traffic there."

## 6. Load balancing — Distributing incoming requests

Imagine a single backend server can handle 1,000 requests per second but your application needs 8,000 requests per second.

Simply upgrading the server might help temporarily, but horizontal scaling is often more flexible.

A load balancer distributes traffic among multiple healthy servers.

### Layer 4 vs Layer 7

|                       | L4 load balancer                              | L7 load balancer                |
| --------------------- | --------------------------------------------- | ------------------------------- |
| Understands           | IPs, ports, transport connections             | HTTP hosts, paths, headers      |
| Route by `/payments`? | Not directly                                  | Yes                             |
| Transport             | TCP/UDP-oriented                              | HTTP-oriented                   |
| Useful for            | Connection-level traffic, transport protocols | API and web application routing |
| Example               | AWS Network Load Balancer                     | AWS Application Load Balancer   |

### Important load-balancing algorithms

Round robin: Requests are distributed sequentially. This works when backend capacities and request costs are similar.

Weighted round robin: More requests go to servers with higher assigned capacity.

Least connections: New requests go to the backend with fewer active connections. This may be appropriate for long-lived connections, although connection count alone doesn't measure CPU cost.

Least response time: Backend selection considers latency and load.

Consistent hashing / key-based routing: Requests with the same key tend to reach the same backend. Useful for affinity-sensitive architectures, although changes in cluster membership and failure handling need attention.

### HLD question: Design a high-traffic shopping application

Suppose the system has separate catalog, payment, and order services.

Client → HTTPS Load Balancer

Path-based routing

/products

Catalog

4 instances

/orders

Orders

3 instances

/payments

Payments

3 instances

Complete request flow:

1. The client resolves the shopping domain and connects to a public load balancer.
2. The load balancer terminates HTTPS, checks the HTTP path, and forwards the request to the relevant target group.
3. For `/products/123`, the request reaches a healthy Catalog Service instance.
4. The instance retrieves product information from cache or database.
5. The response returns through the load balancer to the client.

If a Catalog Service instance fails its health checks, it is taken out of routing. Other healthy instances continue serving requests.

For high availability, deploy backend instances across multiple availability zones and use a highly available load-balancing setup.

### Interview follow-up: What about sticky sessions?

Sticky sessions direct a client to the same backend repeatedly.

They may be needed when a server keeps local session state, but they make failover and scaling harder.

For an ordinary REST API, I would prefer stateless backend instances with authentication tokens or shared session storage.

For WebSockets, the established connection remains attached to a server until it disconnects; we still need a separate mechanism to locate that connection across the cluster.

## 7. Forward proxy vs reverse proxy

A proxy is an intermediary communicating on behalf of another party.

Forward proxy

Client

Forward Proxy

Internet Server

Acts on behalf of clients

Reverse proxy

Client

Reverse Proxy

Backend Servers

Acts on behalf of servers

### When to use a forward proxy

Interview scenario: Design an enterprise infrastructure where employee applications are only allowed to access approved third-party services.

Route outbound requests through a forward proxy that can enforce access policies, log relevant metadata, and control permitted destinations.

For example, a backend might be allowed to call a payment provider but blocked from accessing arbitrary external domains.

### When to use a reverse proxy

Interview scenario: Design a web application with multiple internal services.

Use NGINX, Envoy, or another reverse proxy to:

- Terminate TLS.
- Route requests to backend services.
- Apply selected rate-limiting or authentication policies.
- Forward client identity and tracing headers.
- Support load balancing and selected response caching.

A reverse proxy can serve as a load balancer, but these are not identical concepts.

Interview distinction: A reverse proxy is a traffic intermediary. A load balancer is a traffic-distribution function. One product can perform both.

## 8. CDN — Content Delivery Network

A CDN consists of geographically distributed edge servers that can serve cached content closer to end users.

### Interview application: Design Netflix

Suppose your video files are stored in a single US region, but users across India are streaming content.

Without a CDN:

```
India User
    |
    v
US Origin Server
    |
    v
Video Data
```

Repeatedly sending large video objects across the world increases latency, origin load, and bandwidth usage.

With a CDN:

Netflix Origin / Object Storage

Original encoded video segments

India CDN Edge

Viewer

Europe CDN Edge

Viewer

US CDN Edge

Viewer

### Complete video playback solution

Step 1 — Video upload: A content ingestion service stores the original media.

Step 2 — Transcoding: Background workers produce versions at different resolutions and bitrates, such as 480p, 720p, and 1080p.

Step 3 — Segmentation: The videos are divided into small segments, commonly using protocols such as HLS or MPEG-DASH.

Step 4 — CDN distribution: The segments are made available through the CDN. The CDN may cache popular objects when they are first requested.

Step 5 — Playback: The viewer downloads a playlist or manifest describing available quality levels and segment locations.

Step 6 — Adaptive bitrate: The player estimates network throughput and buffer health. If bandwidth decreases, it requests lower-bitrate segments.

Step 7 — Recovery: If an edge cache misses, the CDN fetches the required object from its origin or another configured cache tier.

### Why CDN instead of Redis?

Redis and CDN caches solve different problems.

| Redis                                            | CDN                                                   |
| ------------------------------------------------ | ----------------------------------------------------- |
| Low-latency data access near application servers | Content delivery near geographically dispersed users  |
| Product records, counters, sessions              | Images, JS, CSS, media segments                       |
| Primarily reduces database reads                 | Primarily reduces origin traffic and delivery latency |
| Application controls keys and invalidation       | HTTP cache policies and CDN invalidation mechanisms   |

Interview answer: "For Netflix, I'd use object storage as the durable video origin and a CDN to distribute pre-encoded media segments. Clients would use adaptive bitrate streaming to handle changing bandwidth instead of downloading one giant video file."

# Part III — APIs and communication between services

## 9. HTTP and HTTPS

HTTP is the application protocol used by web clients and many service APIs.

HTTPS is HTTP protected by TLS.

For system design, the key details are HTTP semantics, connection management, caching, and the cost of network round trips.

### HTTP versions

| Version  | Important property                                       | HLD consideration                                                         |
| -------- | -------------------------------------------------------- | ------------------------------------------------------------------------- |
| HTTP/1.1 | Persistent connections, but limited request multiplexing | Connection reuse and connection-pool sizing                               |
| HTTP/2   | Multiplexed streams on one TCP connection                | Efficient concurrent API calls; gRPC commonly uses this                   |
| HTTP/3   | HTTP over QUIC                                           | Better behavior under some packet loss and connection migration scenarios |

HTTP/2 eliminates application-level HTTP/1.1 request serialization on a connection but still runs over TCP, so transport-level head-of-line blocking can occur.

HTTP/3 uses QUIC, which runs over UDP while providing reliable transport for HTTP streams.

### HLD application: An API with high latency

Suppose one page requires information from five backend services.

If the client calls each service sequentially and each takes 100 ms, the downstream time alone could approach 500 ms.

Instead, the application layer can issue independent requests concurrently. Connection pooling and HTTP/2 multiplexing can reduce connection overhead.

But concurrency does not eliminate slow backend queries.

For a high-latency system, examine:

- Network round trips.
- TLS handshake frequency.
- Connection reuse.
- Backend processing time.
- Sequential versus parallel downstream calls.
- Payload size and serialization.
- Cache hit ratio.

The HLD lesson: Network optimization is not only about choosing HTTP/2 or HTTP/3. Reducing unnecessary calls can have a much bigger effect.

## 10. REST vs GraphQL vs gRPC

All three are ways of exposing application capabilities, but they differ in abstraction and intended usage.

|                       | REST                  | GraphQL                            | gRPC                                             |
| --------------------- | --------------------- | ---------------------------------- | ------------------------------------------------ |
| Core abstraction      | Resources             | Schema and queries                 | Services and methods                             |
| Typical encoding      | JSON                  | Usually JSON responses             | Protobuf                                         |
| Browser compatibility | Excellent             | Excellent                          | Usually needs gRPC-Web or a gateway for browsers |
| Strong contracts      | OpenAPI optional      | Schema-based                       | Protobuf contracts                               |
| Streaming             | Additional mechanisms | Subscriptions where supported      | Native streaming RPCs                            |
| Best fit              | General public APIs   | Flexible frontend data composition | Internal typed service communication             |

### 10.1 REST — For resource-oriented APIs

Question: Design an e-commerce order system.

A natural API is:

```
POST /orders
GET /orders/123
GET /users/45/orders
PATCH /orders/123
```

REST works well because the system exposes identifiable resources such as orders, users, and products.

REST's statelessness means that each request carries the information necessary to process it. It doesn't mean the backend cannot use a database or store user sessions.

For order creation, we must consider idempotency:

```
POST /orders
Idempotency-Key: checkout-abc123
```

The backend should associate the key with the operation's result so repeated client requests cannot accidentally create multiple orders.

A typical solution uses a durable uniqueness constraint or transactional idempotency record.

### 10.2 GraphQL — When clients need different shapes of data

Question: Design a dashboard with web and mobile clients.

The mobile screen requires:

- Name.
- Order status.
- Product thumbnail.

The web application additionally needs:

- Billing details.
- Shipment history.
- Product reviews.

With a rigid REST backend, clients may need several requests or receive unnecessary fields.

With GraphQL, they can request a tailored result:

```
query {
  order(id: "123") {
    status
    product {
      name
      thumbnail
    }
  }
}
```

Architecture:

```
Web / Mobile
     |
     v
GraphQL BFF
     |
     +--> Order Service
     +--> Product Service
     +--> User Service
```

The GraphQL layer resolves fields by calling relevant backend services.

However, it requires safeguards against costly nested queries, N+1 service/database calls, and uncontrolled query complexity.

GraphQL is not automatically faster than REST. Its main advantage is flexible data composition.

### 10.3 gRPC — Efficient internal communication

Question: Your Order Service frequently requests information from Inventory Service.

We can define a typed interface:

```
service InventoryService {
  rpc CheckStock (StockRequest) returns (StockResponse);
}

message StockRequest {
  string product_id = 1;
  int32 quantity = 2;
}

message StockResponse {
  bool available = 1;
}
```

The Protobuf definition creates a strong contract between services. Generated clients and servers handle serialization and RPC integration.

gRPC is valuable when you need typed contracts, efficient payloads, streaming, or frequent internal RPC communication.

But gRPC alone does not provide durable messaging or solve transaction consistency.

### Interview decision

For a microservices e-commerce platform, a reasonable combination is:

- REST for public mobile/web APIs.
- GraphQL BFF for data-heavy dashboards with varying client requirements.
- gRPC for selected high-volume internal service calls.
- Kafka for asynchronous events such as `OrderCreated`.

The important decision is not to standardize everything blindly on one protocol.

## 11. API Gateway and Backend for Frontend

The API Gateway is a managed entry point for client traffic.

Without a gateway, a client may need to know the public URLs of many independent services.

With a gateway:

```
Mobile / Web Client
         |
         v
      API Gateway
         |
     +---+---+
     |   |   |
     v   v   v
    Auth Order Product
```

### Interview application: Design a microservices platform

Suppose we have 15 Spring Boot services.

Solution:

1. The client connects to a public API Gateway using HTTPS.
2. The gateway performs coarse-grained request authentication, throttling, and routing.
3. Requests are forwarded to the relevant internal service.
4. The service still performs business-level authorization.
5. Internal services communicate using REST/gRPC or asynchronous events.
6. Logs and distributed traces carry a correlation identifier across services.

For complex mobile screens, introduce a separate BFF responsible for fetching and combining data from multiple services.

### API Gateway vs Load Balancer

An API Gateway provides API-specific policies and management. A load balancer distributes connections or requests across healthy instances.

A deployed architecture can use both, and a gateway product may contain load-balancing capabilities itself.

Important interviewer follow-up: Does an API Gateway become a single point of failure?

It can if deployed as one instance. Use a managed highly available gateway or multiple redundant gateway instances and maintain capacity and health monitoring.

# Part IV — Real-time networking

## 12. Polling vs Long Polling vs WebSockets vs SSE

This is one of the highest-value networking comparisons for HLD interviews.

Suppose a user is waiting for a food-delivery status update.

There are four ways to communicate the status.

| Method        | How it works                                        | Suitable systems                            |
| ------------- | --------------------------------------------------- | ------------------------------------------- |
| Short polling | Client asks repeatedly at fixed intervals           | Occasional status checks                    |
| Long polling  | Server holds the HTTP request until data or timeout | Chat fallback, infrequent updates           |
| WebSockets    | Persistent bidirectional connection                 | Chat, collaboration, live interactions      |
| SSE           | Persistent server-to-client event stream            | Notifications, dashboards, progress updates |

### Short polling

```
Client                     Server
  |---- Any update? -------->|
  |<------ No ---------------|
  |                          |
  |---- Any update? -------->|
  |<------ No ---------------|
  |                          |
  |---- Any update? -------->|
  |<------ Delivered --------|
```

It is simple, but mostly empty requests create overhead.

For example, if 100,000 connected clients poll every five seconds, that's approximately 20,000 polling requests per second, even when nothing changes.

### Long polling

The client sends a request, and the server waits for an event or timeout before responding.

```
Client                     Server
  |---- GET /updates ------->|
  |                          |
  |      No event yet        |
  |                          |
  |<----- New message -------|
  |                          |
  |---- GET /updates ------->|
```

The client immediately opens another request after receiving a response.

Long polling reduces repeated empty requests, but held connections still consume resources. Backpressure and maximum wait times matter.

### WebSockets

A WebSocket connection usually begins with an HTTP/1.1 Upgrade handshake. Once established, both client and server can send messages without a new HTTP request for each event.

```
Client                      Server
  |----- WebSocket setup ---->|
  |<---- Connection ready ----|
  |                           |
  |------ Message A --------->|
  |<----- Message B ----------|
  |<----- Status update ------|
  |------ Typing event ------>|
```

Modern WebSocket deployments can also support extended CONNECT over HTTP/2 or HTTP/3, depending on the stack.

### SSE

With Server-Sent Events, a browser normally opens an HTTP connection using `EventSource`, and the server streams text-based events.

The client can receive updates, but it sends commands using separate HTTP requests.

This is excellent for one-way streaming, such as a job progress page.

## 13. Full HLD: Design WhatsApp

This example combines WebSockets, load balancing, service discovery, persistent storage, pub/sub, and failure handling.

### Requirements

Assume we're designing one-to-one messaging with:

- Real-time message exchange.
- Offline message delivery.
- Message history.
- Delivery acknowledgments.
- Ordering within a conversation.
- Support for millions of connected clients.

### Architecture

Alice

WebSocket client

Bob

WebSocket client

Connection Load Balancer

Maintains long-lived client connections

WebSocket Gateway A

WebSocket Gateway B

Message Service

Validation, persistence, routing and delivery

Message DB

Durable history

Event Bus

Delivery events

Connection Registry

userId → Gateway instance(s)

Logical architecture. Gateways also communicate directly with the registry and event delivery path; the diagram simplifies those connections.

### Step-by-step message flow

Step 1 — Alice establishes a WebSocket connection.

Alice opens the application. The client connects to a WebSocket endpoint through a load balancer.

Gateway A accepts the connection, authenticates Alice, and records her active connection in a registry.

Conceptually:

```
alice -> Gateway A, Connection 111
bob   -> Gateway B, Connection 742
```

The registry may use Redis or another low-latency distributed store. Connections also need heartbeat/lease handling to remove stale mappings.

Step 2 — Alice sends a message.

```
{
  "messageId": "client-unique-id",
  "conversationId": "c123",
  "receiverId": "bob",
  "content": "Hello"
}
```

Gateway A forwards the message to the Message Service.

Step 3 — Message persistence.

The Message Service validates membership in the conversation and writes the message to durable storage.

It also assigns a server-side sequence number or ordering position suitable for the conversation.

A successful persistence acknowledgment is not the same thing as successful delivery to Bob.

Step 4 — Determine whether Bob is online.

The delivery component checks the connection registry.

```
bob -> Gateway B
```

Step 5 — Route the message.

The service publishes a delivery event to a message broker, or routes through a dedicated internal messaging channel.

Gateway B receives the event and pushes the message to Bob through the existing WebSocket connection.

Step 6 — Bob acknowledges receipt.

Bob's client sends a delivery acknowledgment.

The server updates delivery state and communicates it to Alice.

Step 7 — Offline handling.

If Bob is offline, the message remains durably stored.

When Bob reconnects, his client sends the last acknowledged conversation position. The server queries the missing messages and replays them in the appropriate order.

### What if a WebSocket gateway crashes?

Suppose Gateway B crashes while Bob is connected.

1. Bob's TCP/WebSocket connection breaks.
2. The client detects the failure or heartbeat timeout.
3. The client reconnects, preferably using exponential backoff with jitter.
4. The load balancer chooses another healthy gateway.
5. The new gateway registers Bob's connection.
6. Bob requests messages after his last acknowledged sequence number.
7. Deduplication prevents replayed messages from appearing twice.

### Why Kafka alone isn't enough

Kafka can reliably transfer messages between backend components, but it does not know which WebSocket connection belongs to which user.

We still need the connection registry and gateway routing layer.

Similarly, WebSockets alone do not provide durable message storage, offline delivery, or exactly-once user-visible processing.

### How to maintain ordering

Partition or serialize messages by conversation identifier at the appropriate stage.

For example, Kafka events can use `conversationId` as the key so events for that conversation go to the same partition, preserving partition order.

The application must also handle concurrent senders, retries, and message-sequence assignment.

### Strong SDE-2 answer

"In a chat system, I'd use WebSockets for persistent client communication, but I'd keep connection handling separate from durable message processing. I'd store each message before acknowledging server acceptance, maintain a distributed user-to-gateway registry, and route delivery events to the appropriate gateway. Clients would use stable message IDs and sequence-based synchronization to recover after disconnects."

This is much stronger than simply saying, "I'll use WebSockets because WhatsApp is real time."

# Part V — Distributed networking and fault tolerance

## 14. Service discovery

In a microservices architecture, service instances frequently change.

Suppose an Inventory Service has three instances:

```
10.0.1.10:8080
10.0.1.11:8080
10.0.1.12:8080
```

If Kubernetes removes one instance and creates another with a different IP, the Order Service cannot depend on those addresses being permanent.

Service discovery solves this problem by finding current service endpoints dynamically.

### Client-side discovery

```
Order Service
      |
      v
Service Registry
      |
      v
Inventory instances
```

The caller queries a service registry, chooses an instance, and makes the call.

Technologies include Eureka or Consul-style registries and client-side load-balancing libraries.

### Server-side discovery

```
Order Service
      |
      v
inventory-service DNS
      |
      v
Service / Load Balancer
      |
      v
Healthy Inventory Pod
```

A server-side intermediary chooses the backend.

For Kubernetes deployments, ordinary `ClusterIP` Services plus Kubernetes DNS typically provide a convenient default.

### HLD application: Dynamic Spring Boot services

Consider 20 independently deployable Spring Boot microservices.

Instead of hardcoding IP addresses:

```
// Fragile: target changes during deployment
http://10.0.1.10:8080/inventory
```

The application calls a stable service name:

```
http://inventory-service:8080/inventory
```

The cluster networking system routes it to an appropriate backend.

Why this matters: It allows rolling deployments, scaling, instance replacement, and backend failure handling without modifying callers each time an instance changes.

## 15. Service mesh

A service mesh manages communication between services using infrastructure separate from business logic.

It can provide:

- Service-to-service mTLS.
- Retries and timeouts.
- Traffic splitting.
- Service identity.
- Distributed tracing and metrics.
- Policy enforcement.

Typical technologies include Istio and Linkerd, although their architectures and data planes differ.

### HLD application: Canary deployment

Suppose you want to deploy Inventory Service v2 without exposing all users to potential defects.

A service mesh or equivalent traffic-management layer could apply:

```
Order Service
      |
      v
Traffic Policy
      |
      +------ 95% -----> Inventory v1
      |
      +------- 5% -----> Inventory v2
```

Compare error rates and latency between versions. Increase the v2 percentage if it performs correctly.

When to introduce a mesh: When you have enough services and cross-cutting networking concerns to justify its complexity.

For a small three-service application, ordinary service discovery, TLS, and application resilience libraries may be sufficient.

## 16. Circuit breaker

A circuit breaker prevents an application from repeatedly calling a downstream service that is consistently failing.

### Interview problem

The Order Service calls Payment Service.

Payment Service becomes extremely slow.

Each order request now holds backend resources while waiting. Incoming requests accumulate, connection pools saturate, and the Order Service becomes unhealthy even though its own code works.

This is a cascading failure.

### Circuit breaker states

CLOSED

Calls pass; failures monitored

OPEN

Calls fail fast

HALF-OPEN

Limited probe calls

Failures rise → Open → Recovery timer → Half-open → Success → Closed

### Solution

1. Put a strict timeout on Payment Service calls.
2. Monitor failures over a rolling window.
3. If failures exceed a threshold and sufficient requests have been observed, open the circuit.
4. While open, reject new downstream calls immediately or return an appropriate fallback.
5. After a configured delay, allow a limited number of probe calls.
6. Close the circuit if the service recovers.

With Java Spring Boot, Resilience4j is a common implementation choice.

### Important SDE-2 nuance

Do not blindly retry every failure.

If a payment request times out, the charge might already have succeeded.

A naive retry could create a duplicate payment unless the provider supports idempotency and the application uses a stable idempotency key.

For uncertain payment outcomes, return a pending status and reconcile with the payment provider.

The correct combination is often timeouts + selective retries + circuit breakers + idempotency.

## 17. Rate limiting

Rate limiting controls the volume of operations allowed over time.

The repository discusses token bucket, leaky bucket, sliding-window approaches, and distributed rate-limiting challenges.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://raw.githubusercontent.com\&sz=32)

GitHub



### Algorithms

| Algorithm      | Main behavior                                  | Best suited for                  |
| -------------- | ---------------------------------------------- | -------------------------------- |
| Fixed window   | Counts requests in fixed time intervals        | Simple quotas                    |
| Sliding window | Measures usage over a moving interval          | Fairer API limits                |
| Token bucket   | Refills tokens at a fixed rate, permits bursts | Public API traffic               |
| Leaky bucket   | Smooths processing at a defined rate           | Controlled downstream throughput |

### HLD question: Design a public API

Suppose each user is allowed 100 requests per minute.

If the system has ten gateway instances and each independently allows 100 requests, the effective total could be much higher than intended.

To enforce a shared limit, use a centralized or distributed coordination mechanism such as Redis with an atomic operation.

```
Client
   |
   v
API Gateway
   |
   v
Distributed Rate Limiter
   |
   +---- Allowed ---> Backend
   |
   +---- Rejected --> HTTP 429
```

A Redis Lua script or equivalent atomic mechanism can check and update the rate-limit state without races between gateway instances.

### Token bucket example

Suppose an API allows:

- Sustained rate: 10 requests/second.
- Burst capacity: 30 requests.

Tokens accumulate up to 30. Each request consumes one token.

If 25 requests arrive simultaneously after an idle period, they can all be admitted.

If another 20 immediately arrive, only those covered by the remaining or replenished tokens are admitted.

This balances short bursts with sustained traffic limits.

### SDE-2 follow-up: What if Redis fails?

You must choose explicitly between:

- Fail open: Permit traffic temporarily, preserving availability but risking overload.
- Fail closed: Reject traffic until the limiter recovers, preserving stricter enforcement.

For a general read endpoint, bounded local fallback limits might be reasonable. For an expensive or abuse-sensitive operation, stricter behavior may be appropriate.

You should monitor Redis availability, limiter latency, rejected requests, and backend saturation.

## 18. Clustering and high availability

Clustering means multiple computing instances cooperate to provide capacity, redundancy, or distributed processing.

### Active-active

Multiple instances serve traffic simultaneously.

Useful for stateless application servers and regional read workloads.

### Active-passive

One instance actively serves while another waits to take over.

Useful for systems that require a single active coordinator, primary writer, or simpler failover model.

### HLD application: Design a highly available database-backed API

Deploy stateless application instances across multiple availability zones.

For the database, choose a suitable primary/replica configuration with automated failover.

An application load balancer can route around unhealthy application instances, but it cannot by itself solve database failover.

A complete design must cover database promotion, transaction consistency, connection re-establishment, and recovery procedures.

## 19. SLA, SLO, and SLI

These concepts convert vague reliability goals into measurable system requirements.

| Term | Definition                | Example                                |
| ---- | ------------------------- | -------------------------------------- |
| SLI  | Actual measured indicator | Percentage of successful requests      |
| SLO  | Internal target           | 99.95% successful requests             |
| SLA  | Contractual commitment    | Customer-facing availability agreement |

### HLD application

Suppose the interviewer says the payment platform needs 99.99% monthly availability.

That permits approximately 4.3 minutes of downtime in a 30-day month.

You should now design for a measurable reliability target:

- Multi-AZ application deployment.
- Redundant ingress.
- Database failover.
- Safe dependency timeouts.
- Health monitoring.
- Incident recovery.
- Load testing and capacity planning.

Availability and reliability aren't exactly the same: a service can respond successfully while occasionally producing incorrect results.

For payments, both matter.

## 20. Backpressure and asynchronous communication

Backpressure handles the situation in which incoming work exceeds the rate at which a system can process it.

### HLD application: Notification service

Suppose 100,000 notifications arrive within one minute, but your external push provider only accepts 2,000 per minute.

If the frontend request waits for every push to complete, response times and failure rates will explode.

Instead:

```
API
 |
 v
Durable Queue / Kafka
 |
 v
Worker Pool
 |
 v
Rate-limited Provider Calls
```

The API accepts or rejects work based on available capacity and durability guarantees.

Workers consume jobs at a safe rate. Queue depth grows temporarily and drains as capacity becomes available.

Use bounded buffers, queue-lag monitoring, retry limits, and dead-letter handling.

A queue is not infinite capacity. If demand exceeds processing capacity for long enough, lag and storage continue increasing.

### Queue vs Pub/Sub vs Kafka

Task queue: Distributes jobs among workers. For example, one notification worker processes a notification task.

Publish-subscribe: Multiple interested consumers can receive events. For example, an `OrderCreated` event may trigger billing, analytics, and notifications.

Kafka: A partitioned, durable event log where consumer groups can independently consume and replay events.

For Kafka, ordering is per partition rather than automatically global across all partitions.

For all of these, business-level idempotency is necessary if repeated delivery or processing is possible.

# Part VI — Networking security

## 21. TLS vs SSL vs mTLS

The repository includes SSL, TLS, and mTLS in Chapter IV. SSL is obsolete; modern deployments should use TLS.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://raw.githubusercontent.com\&sz=32)

GitHub



### TLS — Encryption and server authentication

When your browser connects to a secure website, it typically authenticates the server using its certificate.

A simplified TLS 1.3 connection setup works as follows:

1. The client sends a `ClientHello` with supported cryptographic parameters and key-sharing information.
2. The server selects the parameters and sends its key-sharing information and certificate.
3. The client validates the certificate chain and requested hostname.
4. Both sides derive shared session keys.
5. Subsequent application traffic is encrypted and integrity-protected.

The actual protocol contains additional handshake messages and verification steps.

### mTLS — Mutual authentication

With mTLS, the server also asks the client to present a certificate.

Both sides authenticate one another.

|                             | TLS                                               | mTLS                                     |
| --------------------------- | ------------------------------------------------- | ---------------------------------------- |
| Server certificate verified | Yes                                               | Yes                                      |
| Client certificate verified | Usually not required                              | Yes                                      |
| Typical use                 | Browser to HTTPS website                          | Service-to-service identity              |
| Security benefit            | Confidentiality, integrity, server authentication | Adds cryptographic client authentication |

### HLD question: Secure a banking microservices platform

Imagine:

```
Mobile App
    |
    v
API Gateway
    |
    v
Account Service
    |
    v
Transaction Service
```

Recommended design:

- Use HTTPS/TLS for communication between mobile clients and the public API endpoint.
- Authenticate end users using an appropriate authentication protocol.
- Authorize operations at the relevant business service.
- Use mTLS between sensitive internal services where required.
- Use short-lived certificates and automated certificate rotation.
- Record security-relevant operations in an audit trail.

### Why mTLS if services are already in private networks?

A private network restricts exposure, but it doesn't establish that every reachable workload is trustworthy.

If a compromised internal workload can connect to a sensitive service, network location alone doesn't provide sufficient identity assurance.

mTLS can authenticate a workload's cryptographic identity.

However, mTLS doesn't replace authorization. The Transaction Service must still check whether the authenticated caller is permitted to initiate the requested action.

## 22. OAuth 2.0, OpenID Connect, and SSO

These appear in the handbook's security section and matter whenever external identity is part of an HLD question.

OAuth 2.0 is an authorization framework for delegated access.

OpenID Connect (OIDC) adds authentication and identity information on top of OAuth 2.0.

SSO is the user experience of signing in once and accessing multiple applications, typically built using identity federation protocols such as OIDC or SAML.

### Interview application: Login with Google

For a modern web application, use Authorization Code Flow with PKCE.

A simplified flow:

```
User
 |
 v
Your Web Application
 |
 v
Google Authorization Server
 |
 v
User authenticates + consents
 |
 v
Authorization Code
 |
 v
Backend exchanges code for tokens
 |
 v
Validate identity and create app session
```

Your backend validates the ID token appropriately, maps the Google identity to an application user, and establishes an application session.

The Google ID token is not automatically the same as an access token issued for your own backend API.

For high-security applications, use appropriate session management, secure cookies, CSRF protection where relevant, and explicit authorization checks.

### Where does JWT fit?

JWT is a token format, not an authentication protocol.

An access token may be a JWT or an opaque token.

If your service accepts JWT access tokens, validate the issuer, audience, expiration, signature, and relevant permissions.

HLD takeaway: TLS protects data in transit; OIDC establishes user identity; OAuth provides delegated access; JWT may carry token claims; mTLS authenticates workloads. These solve different problems.

# Part VII — Complete HLD applications

The following are larger interview exercises combining the concepts we've covered.

## 23. Design a URL Shortener

Foundational HLD

### Requirements

Users can create a short link such as:

```
https://sho.rt/abc123
```

Visiting the link should redirect to the original URL.

Assume reads are much more frequent than writes, and redirects should be very fast.

### Architecture

```
                  User
                   |
                   v
                  DNS
                   |
                   v
               CDN / Edge
                   |
                   v
              Load Balancer
                   |
                   v
          URL Redirect Service
              /          \
             v            v
          Redis        Database
         (Cache)       (Durable)
```

### API design

```
POST /api/urls
Content-Type: application/json

{
  "longUrl": "https://example.com/products/123"
}
```

Response:

```
{
  "shortUrl": "https://sho.rt/abc123"
}
```

Redirect request:

```
GET /abc123
```

Possible response:

```
HTTP/1.1 302 Found
Location: https://example.com/products/123
```

### Full design explanation

1\. Short-link creation

The service validates the original URL and generates a unique short code.

Possible approaches include a distributed ID generator encoded in Base62 or a random code with collision detection.

It stores:

```
shortCode -> originalURL
```

in a durable database.

2\. DNS resolution

When a user opens the short link, DNS resolves `sho.rt` to the service's public endpoint.

3\. Load balancing

A load balancer directs the request to an available redirect-service instance.

Instances are stateless so any healthy instance can handle the lookup.

4\. Redis cache

The service first checks Redis for `abc123`.

If the key exists, it obtains the original URL without querying the database.

On a cache miss, the service queries the database, caches the result, and proceeds.

5\. HTTP redirect

The service returns an HTTP redirect response with the original destination in the `Location` header.

A `301` or `308` is appropriate for a genuinely permanent redirect, while `302` or `307` can be preferable when the destination may change.

The chosen status also affects client caching behavior. If per-click analytics matter, aggressive redirect caching can mean fewer requests reach the application.

6\. Analytics

Publish a click event asynchronously to an analytics pipeline rather than blocking redirects on analytics writes.

7\. Failure handling

If Redis fails, temporarily fall back to the database while controlling traffic to avoid a cache-stampede overload.

If the primary application instance fails, the load balancer routes to another instance.

### Networking decisions demonstrated

| Concept           | Where used             | Why                       |
| ----------------- | ---------------------- | ------------------------- |
| DNS               | Resolving short domain | Stable public name        |
| HTTPS             | Client communication   | Secure transport          |
| L7 load balancing | Redirect traffic       | Scale stateless instances |
| Redis caching     | URL lookup             | Reduce repeated DB reads  |
| HTTP redirects    | Final response         | Native browser navigation |
| Rate limiting     | URL creation           | Control abuse             |
| Queue / Kafka     | Analytics              | Keep redirect path fast   |

Interview challenge: What if a short URL suddenly gets 100 million visits?

Popular mappings should remain hot in cache, redirect instances should scale horizontally, and origin load should be controlled. A CDN or edge caching strategy may help, but its suitability depends on mutable links and per-click analytics requirements.

## 24. Design Uber — Real-time driver location

Advanced networking

This question tests a different set of concepts: spatial indexing, frequent location updates, fan-out, and real-time subscriptions.

### Requirements

- Drivers continuously update their locations.
- Riders can find nearby available drivers.
- Riders can see their assigned driver's live location.
- The system must handle large numbers of moving devices.

### Architecture

```
Driver App
    |
    | GPS updates
    v
Location Ingestion Service
    |
    +----> Current Location Store
    |           |
    |           v
    |      Geospatial Index
    |
    +----> Event Stream
                 |
                 v
         Trip Tracking Service
                 |
                 v
          WebSocket / SSE
                 |
                 v
             Rider App
```

### Why geohashing?

Geohashing divides geographical coordinates into hierarchical cells represented by strings.

Instead of scanning all drivers globally, we can search drivers in the rider's cell and surrounding cells.

The location index reduces candidate discovery cost.

But shared geohash prefixes don't guarantee exact nearest-neighbor ordering, and cell boundaries can separate nearby drivers.

Therefore:

1. Find candidate drivers from nearby cells.
2. Include neighboring cells.
3. Calculate actual distance for shortlisted candidates.
4. Apply business filters such as availability, vehicle type, and ETA.
5. Expand the search region if too few eligible drivers are found.

Geohashing and quadtrees are spatial-indexing methods rather than transport protocols, but they're crucial to Uber-style HLD.

### How to transmit driver locations

A driver sends location updates periodically or based on meaningful movement.

For example:

```
{
  "driverId": "d123",
  "latitude": 28.60,
  "longitude": 77.21,
  "timestamp": 1792080000,
  "sequence": 1024
}
```

The system ingests these through a secure mobile API or persistent communication channel.

The location service updates the latest known driver location and publishes relevant movement events.

### How the rider sees movement

When a trip is active, the rider subscribes to the location updates of the assigned driver.

A WebSocket is useful if bidirectional real-time updates are needed. SSE could be sufficient for a browser experience where the server only pushes updates.

### What if location updates arrive out of order?

Suppose the service receives:

```
Update 105
Update 103
Update 104
```

If we blindly overwrite the location on every arrival, the driver's marker can move backward.

Maintain an appropriate timestamp or monotonically increasing sequence and reject stale updates for the current-location view.

### What if a driver loses network access?

The backend records the last update time.

After a freshness threshold, it marks the location stale and can exclude that driver from new assignments.

For an active ride, the UI may display the last known location and a delayed-update indicator.

### When to use which networking concepts

| Requirement              | Solution                              |
| ------------------------ | ------------------------------------- |
| Driver GPS ingestion     | HTTPS or persistent mobile connection |
| Find nearby drivers      | Geospatial indexing                   |
| Rider live tracking      | WebSockets or SSE                     |
| Process location events  | Partitioned event stream              |
| Prevent stale locations  | Sequence/timestamp checks             |
| Scale client connections | WebSocket gateway cluster             |
| Recover connectivity     | Reconnect and fetch current state     |
| Secure trip data         | TLS, auth, trip-level authorization   |

A useful HLD distinction: Location storage and location delivery are different problems. You don't need to write every transient GPS sample to the primary transactional database before a rider's map can move. You do need appropriate durability for trip events and business records.

## 25. Design Flipkart Big Billion Days iPhone Flash Sale

High concurrency

This is a particularly strong interview exercise because normal horizontal scaling doesn't solve the central problem: thousands of users competing for a very small inventory.

### Problem statement

Assume:

- 100,000 iPhones are available.
- 5 million users attempt to buy one at the beginning of the sale.
- Requests arrive in a concentrated burst.
- Overselling must not occur.
- The checkout experience should remain understandable when the system is saturated.

### Architecture

5M Potential Buyers

Web and mobile applications

CDN + WAF + API Gateway

Static content, bot mitigation, admission control

Flash Sale Admission Service

Authenticated eligibility and waiting room

Reservation Service

Atomic stock reservation and limits

Durable Database / Reservation Ledger

Source of truth for inventory commitments

Checkout + Payment Services

Idempotent processing and status tracking

Order Events / Notification Workers

Asynchronous follow-up operations

### Step 1 — Serve static content through a CDN

Product images and frontend assets should be delivered from a CDN.

We don't want millions of users fetching identical product images directly from application servers.

### Step 2 — Prevent uncontrolled backend admission

Introduce API rate limiting, bot mitigation, and potentially a virtual waiting room.

A waiting room gives users an admission token when it is their turn.

The admission service verifies that token before allowing a checkout attempt.

This is different from simply returning HTTP 429 to everyone.

### Step 3 — Separate product browsing from purchasing

Product listing is read-heavy and cacheable.

Inventory reservation is a critical write operation and must be handled with stronger consistency.

A Redis count displayed on the product page can be approximate. The authoritative decision about whether a unit can be reserved cannot rely on an asynchronously updated approximate count.

### Step 4 — Atomic inventory reservation

Suppose the remaining stock is 1 and two users attempt to reserve it.

The service must ensure only one reservation succeeds.

One database-based implementation is:

```
UPDATE inventory
SET available = available - 1
WHERE product_id = 123
  AND available > 0;
```

A successful row update can be followed by creating a reservation record within an appropriate transaction.

For a full implementation, use idempotency keys, uniqueness constraints, reservation ownership, and transactionally consistent state transitions.

At high scale, a single inventory row can become a contention bottleneck. We might introduce controlled reservation buckets or preallocated inventory partitions, but those require careful accounting to prevent overselling.

### Step 5 — Handle payment asynchronously where appropriate

After successful reservation, the user proceeds to payment.

Don't hold a database transaction open while waiting for a third-party payment provider.

Instead, maintain reservation states such as:

```
RESERVED
   |
   +--> PAYMENT_SUCCESS --> CONFIRMED
   |
   +--> EXPIRED / FAILED --> RELEASED
```

Use durable state transitions, payment idempotency, and reconciliation.

### Step 6 — Apply backpressure

Suppose the payment service supports only a fraction of the incoming checkout requests.

You should cap active checkout processing, maintain an appropriate bounded backlog, and communicate pending/rejected states to clients.

Queue depth, processing latency, and expiration policies become important.

### Step 7 — Failure handling

What if the payment API times out? Don't automatically release inventory or repeat a potentially successful charge without checking the payment outcome.

What if Redis fails? Inventory correctness must not depend solely on an unprotected cache counter.

What if a customer retries checkout? Use a stable idempotency key and enforce one valid reservation or purchase according to business rules.

What if the checkout server crashes? Durable reservation state lets another worker resume or reconcile the process.

### SDE-2 interview response

"I'd isolate the read-heavy browsing path from the consistency-sensitive purchase path. The CDN and cache would absorb browsing traffic, while a waiting room and rate limiter would control admission. A reservation service would atomically allocate inventory in durable storage, then initiate payment outside the inventory transaction. Every order and payment operation would be idempotent, and I'd use queues, timeouts, and reconciliation to handle downstream failures without overselling."

# Part VIII — Remaining networking-related concepts in the handbook

We've covered the major protocols and HLD applications. There are additional concepts in the repository that connect directly to networking and distributed communication.

## 26. Caching strategies

Caching is especially important when a network call is expensive or repeatedly retrieves the same information.

| Strategy           | How it works                                        | HLD application                                                          |
| ------------------ | --------------------------------------------------- | ------------------------------------------------------------------------ |
| Cache-aside        | App checks cache, then DB on miss                   | Product catalog                                                          |
| Read-through       | Cache layer loads missing data                      | Frequently read profiles                                                 |
| Write-through      | Writes update cache and backing store synchronously | Data requiring coordinated cache updates                                 |
| Write-behind       | Cache buffers writes for later persistence          | Selected high-write workloads where durability trade-offs are acceptable |
| TTL expiration     | Data expires after configured time                  | Short-lived API results                                                  |
| Cache invalidation | Removes or refreshes stale entries                  | Product price changes                                                    |

### HLD scenario: A viral social media post

A celebrity publishes a post and millions of people request it.

Without caching, the database receives an enormous number of duplicate reads.

Use a distributed cache to serve the post data, and a CDN to serve its images and videos.

But what happens if that cached entry expires while a million users are requesting it?

A cache stampede occurs: many requests simultaneously hit the database.

Possible defenses include request coalescing, soft expiration, prewarming, and controlled refresh.

The interview distinction is between reducing network traffic through a CDN and reducing backend data retrieval through an application cache.

## 27. Consistent hashing

Consistent hashing maps keys and backend nodes onto a logical hash ring.

When the cluster membership changes, a relatively small fraction of keys needs to move, compared with naive modulo-based hashing.

### HLD application: Distributed cache cluster

Suppose user sessions are distributed over ten cache nodes.

With:

```
hash(userId) % 10
```

adding an eleventh node changes many mappings.

A consistent hashing ring with virtual nodes can distribute keys more stably when nodes join or leave.

When to use: Distributed cache placement, partition routing, or systems that need relatively stable key ownership.

When not to introduce it: Ordinary stateless HTTP load balancing often doesn't need application-managed consistent hashing. A managed cache cluster may already provide its own partitioning.

## 28. N-tier architecture and microservices communication

N-tier architecture separates responsibilities into logical layers.

For example:

```
Presentation Tier
      |
      v
Application Tier
      |
      v
Data Tier
```

The tiers may be deployed separately or together.

Microservices are a different type of decomposition: independent services organized around capabilities or bounded contexts.

### HLD application: Online shopping

A shopping application can have:

- Web frontend.
- Catalog Service.
- Order Service.
- Inventory Service.
- Payment Service.
- Databases and caches.

Communication occurs through synchronous APIs and asynchronous events.

A good answer should avoid turning every tiny function into a remote service, because each network boundary introduces latency, deployment complexity, and failure possibilities.

## 29. Message brokers, queues, pub/sub, and EDA

These are central to distributed communication.

### Synchronous vs asynchronous

Synchronous:

```
Order Service --HTTP/gRPC--> Inventory Service
```

The caller ordinarily waits for a response.

Asynchronous:

```
Order Service --> Kafka --> Notification Service
```

The caller can finish its immediate operation without waiting for the notification to be delivered.

### HLD application: Order lifecycle

When a customer orders a product:

1. Create the order with a durable transaction.
2. Persist an event through a transactional outbox or equivalent mechanism.
3. Publish `OrderCreated`.
4. Inventory, notification, and analytics consumers process it according to their responsibilities.
5. Consumers use idempotency and appropriate ordering guarantees.

An event bus decouples services, but it introduces eventual consistency and delivery failure handling.

### Event sourcing and CQRS

Event sourcing: The authoritative state can be reconstructed from an event history, subject to the chosen event model and snapshot strategy.

CQRS: Separate command and query models when read and write workloads have substantially different requirements.

A trading or financial ledger may justify event-oriented auditability. A simple user-profile CRUD service often does not.

### Enterprise Service Bus (ESB)

An ESB is an integration architecture in which a central bus provides capabilities such as routing, transformation, and protocol mediation.

For an enterprise integrating several legacy SOAP systems with newer services, an ESB or integration platform might be appropriate.

For a new microservices product, lightweight APIs and messaging are often simpler, but the correct choice depends on integration requirements.

## 30. Geohashing vs Quadtrees

Both improve geographic queries, but their partitioning strategies differ.

| Geohashing                                           | Quadtrees                                              |
| ---------------------------------------------------- | ------------------------------------------------------ |
| Encodes locations using hierarchical string prefixes | Recursively subdivides spatial regions                 |
| Convenient for prefix-based spatial grouping         | Can adapt subdivision to local data density            |
| Requires neighbor-cell checks                        | Can support region and range searches                  |
| Often useful for distributed geographic indexing     | Useful for spatial search and indexing implementations |

HLD application: Uber driver discovery, nearby restaurants, delivery assignment, or location-based recommendations.

A geohash index reduces the initial search space; an exact distance or travel-time calculation determines final ranking.

## 31. Disaster recovery

Disaster recovery asks what happens if an availability zone or entire region becomes unavailable.

Two core metrics are:

RTO (Recovery Time Objective): How long can the service remain unavailable?

RPO (Recovery Point Objective): How much data loss is acceptable?

### HLD application: Banking system

Imagine the primary region experiences an outage.

Your recovery strategy might include:

- Backups and verified restores.
- A standby region.
- Cross-region database replication.
- DNS or global traffic failover.
- Infrastructure automation.
- Regular recovery drills.

Be careful: asynchronous replication can lose recently committed data during a regional failure. If the system cannot tolerate that, you need a stronger durability design, often with additional latency and availability trade-offs.

Disaster recovery is not achieved merely by deploying two application servers.

## 32. Virtual machines and containers

A virtual machine virtualizes a complete operating-system environment. A container isolates application processes while sharing the host operating-system kernel.

For HLD, the networking implications matter more than memorizing definitions.

### HLD application: Deployment of 30 Spring Boot services

With Kubernetes, each Pod has a network identity, while Kubernetes Services give clients stable logical endpoints.

Containers can be recreated or rescheduled, and their network addresses can change.

This brings us back to service discovery, load balancing, health checks, and graceful shutdown.

A robust deployment should stop routing new traffic to terminating instances and allow existing requests to complete within a bounded grace period.

# Part IX — Putting it all together

## 33. End-to-end design: A production-ready microservices platform

Suppose the interviewer asks:

Design a high-traffic e-commerce application supporting product discovery, orders, payments, and notifications across multiple regions.

Here is a combined architecture that applies the networking decisions from the handbook.

Web / Mobile Clients

DNS + Global Traffic Routing

Select a healthy region

CDN + WAF

Cache static content and filter unwanted traffic

API Gateway / L7 Load Balancer

TLS, auth, rate limits, routing

Catalog Service

Order Service

Inventory Service

Payment Service

Internal communication: REST/gRPC, service discovery, optional mTLS and service mesh

Redis / Database

Cache and durable state

Kafka / Queue

Async events

Notifications, Analytics and Workers

Independently scalable consumers

This is a logical diagram, not a mandatory technology stack. The exact services, storage architecture, and regional consistency model depend on the requirements.

### Walk through a customer placing an order

1\. Client connectivity

The browser resolves the application domain using DNS and establishes a secure connection.

2\. Static asset delivery

The CDN serves cached frontend assets and product images without unnecessarily contacting the application's origin.

3\. Request routing

The API Gateway authenticates the request, applies admission policies, and routes it to the Order Service.

4\. Internal communication

The Order Service discovers and calls the Inventory Service using a stable service endpoint.

It applies appropriate timeouts and failure handling.

5\. Consistency-sensitive processing

Inventory is reserved atomically, and the order transitions into an appropriate state.

6\. Payment integration

The system uses idempotency and durable state transitions to handle payment uncertainty, retries, and external provider failures.

7\. Asynchronous processing

Once the order reaches a suitable state, durable events trigger notifications and analytics.

8\. Real-time user feedback

The frontend can poll a status endpoint or subscribe through SSE/WebSockets, depending on how frequently statuses change and whether bidirectional communication is needed.

9\. Failure handling

Health checks remove unhealthy instances. Circuit breakers and bounded retries protect dependencies. Queue consumers can recover work from durable state.

10\. Regional failure

A disaster recovery plan determines whether traffic can shift to another region and whether the required application state is available there.

That is an HLD answer in which every networking component has a reason for existing.

# Part X — Interview decision-making cheat sheet

## 34. What to introduce when the interviewer gives a requirement

| Requirement / Problem                  | First concepts to consider                          |
| -------------------------------------- | --------------------------------------------------- |
| Millions of HTTP requests              | Horizontal scaling, L7 load balancing               |
| Users across continents                | CDN, regional routing, DNS                          |
| Large image/video traffic              | Object storage, CDN, HTTP caching                   |
| Real-time chat                         | WebSockets, connection registry, durable messaging  |
| Live sports score dashboard            | SSE or WebSockets, pub/sub                          |
| Live video calling                     | WebRTC, UDP media, SFU                              |
| Low-latency service-to-service calls   | gRPC/REST, pooling, timeouts                        |
| Services constantly changing IP        | Service discovery                                   |
| One downstream service failing         | Timeout, circuit breaker, controlled retry          |
| Traffic spikes beyond capacity         | Rate limiting, admission control, backpressure      |
| Sensitive internal communications      | mTLS, workload identity, authorization              |
| Repeated database reads                | Redis or equivalent caching                         |
| Multiple consumers need the same event | Pub/sub or Kafka                                    |
| Background task processing             | Durable work queue                                  |
| Find nearby drivers                    | Geohashing, spatial indexing                        |
| Zero overselling                       | Atomic inventory reservation, idempotency           |
| Region becomes unavailable             | Multi-region failover, RTO/RPO                      |
| Multiple frontend data requirements    | BFF, possibly GraphQL                               |
| Rolling deployment                     | Service discovery, health checks, graceful shutdown |

## 35. Which topics should you master first?

For SDE-2 interviews, I'd prioritize the topics in this order. The handbook places these across several chapters, but this sequence makes their dependencies easier to learn.

Networking HLD mastery checklist

0/12

1\. DNS, HTTP, HTTPS and TLS

Trace what happens from entering a URL to receiving an API response.

2\. TCP, UDP and connection management

Understand reliability, latency, connection reuse and long-lived connections.

3\. Load balancing, proxy and CDN

Explain public ingress, healthy backend routing and edge caching.

4\. REST, GraphQL, gRPC and API Gateway

Choose an interface based on client requirements and service boundaries.

5\. Polling, WebSockets and SSE

Design chat, progress updates and live dashboards.

6\. Service discovery and service mesh

Handle dynamic instances, internal routing and workload identity.

7\. Timeouts, retries, circuit breakers

Prevent failures in one service from cascading across the system.

8\. Rate limiting and backpressure

Protect systems during flash sales and demand spikes.

9\. Caching, queues and pub/sub

Reduce synchronous dependencies and absorb variable traffic.

10\. mTLS, OAuth, OIDC and JWT

Separate transport security, workload identity, user authentication and authorization.

11\. Geospatial indexing

Apply geohashing or spatial trees to proximity-based applications.

12\. Availability and disaster recovery

Defend multi-AZ, regional failover and data recovery choices.

Reset checklist

## 36. The seven questions to ask about any networking component

If you mention a networking technology in an HLD interview, make sure you can answer these:

1. What problem does it solve? Describe the requirement, not just the technology.
2. Where is it placed? Client-to-server, edge-to-origin, or service-to-service?
3. How does communication happen? Protocol, connections, data flow, request/response or events.
4. How does it scale? Connections, throughput, partitioning, load balancing, and memory.
5. What happens on failure? Timeouts, reconnects, retries, health checks, replay, and recovery.
6. What consistency or delivery guarantees exist? Ordered transport, durable messages, duplicate processing, and idempotency.
7. Why not the obvious alternative? WebSockets vs SSE, gRPC vs REST, L4 vs L7, Redis vs CDN, synchronous vs asynchronous.

This is the most important change in how to approach these topics: don't introduce a technology merely because it is popular. Introduce it because a particular system requirement makes it useful, then explain its cost and failure modes.

### A note about the resource

The coverage above follows the [handbook's topic structure](https://github.com/karanpratapsingh/system-design/blob/main/README.md?utm_source=chatgpt.com), with additional implementation detail, corrected oversimplifications, and original HLD examples. It is not a verbatim reproduction.

Some useful extensions worth studying beyond the repository's explicit networking topics are QUIC/HTTP3, WebRTC, connection-pool exhaustion, NAT, VPC subnetting, WAFs, zero-trust service identity, and distributed tracing. These often become the follow-up questions that distinguish a basic architecture answer from a more convincing SDE-2 answer.
