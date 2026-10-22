# AWS SAA-C03 Notes — Part 1 (Slides pg 118–226)

**Covers:** High Availability & Scalability (ELB + Auto Scaling Groups) · RDS, Aurora & ElastiCache · Amazon Route 53

> Legend: 🎯 = exam favourite / keyword trigger · ➕ = extra info not on the slides but useful for the exam

---

## 0. Tiny Beginner Glossary (read once)

| Term | Meaning in plain English |
|---|---|
| **Region** | A geographic area where AWS has data centers (e.g. `us-east-1` = N. Virginia). |
| **Availability Zone (AZ)** | One or more separate data centers inside a Region (e.g. `us-east-1a`, `us-east-1b`). Regions have 3+ AZs. |
| **EC2 instance** | A virtual server you rent. Instance type (`t2.micro`, `t2.large`) = its size (CPU/RAM). |
| **AMI** | Amazon Machine Image — a "template/snapshot" of a server (OS + software) used to launch EC2. |
| **VPC** | Your own private network inside AWS. **Subnets** are sections of it (public or private). |
| **Security Group (SG)** | A firewall attached to a resource. It *allows* traffic (by port, IP, or another SG). |
| **CloudWatch** | AWS monitoring: **metrics** (numbers like CPU %), **alarms** (trigger when metric crosses a threshold), **logs**. |
| **IAM** | AWS identity & permissions (users, roles, policies). |
| **KMS** | Key Management Service — manages encryption keys. |
| **Latency** | Delay/time for a request to travel and get a response. Lower = faster. |
| **Layer 3 / 4 / 7** | Network layers: **L3** = IP packets, **L4** = TCP/UDP (ports), **L7** = HTTP/HTTPS (URLs, headers). |

---

# PART A — High Availability & Scalability

## 1. Scalability vs High Availability

**Scalability** = the system can handle more load by adapting. Two kinds:

### Vertical Scalability (scale **up / down**)
- Increase the **size** of the instance. *Analogy: replace a junior call-center operator with a senior one.*
- e.g. `t2.micro` → `t2.large`. Range: `t2.nano` (0.5 GB RAM, 1 vCPU) → `u-12tb1.metal` (12.3 TB RAM, 448 vCPUs).
- Common for **non-distributed systems like databases**. 🎯 **RDS and ElastiCache scale vertically.**
- There's a **hardware limit**.

### Horizontal Scalability (scale **out / in**) = **Elasticity**
- Increase the **number** of instances. *Analogy: hire more operators.*
- Implies **distributed systems**; common for web/modern apps; easy in the cloud with EC2.
- Tools: **Auto Scaling Group** + **Load Balancer**.

### High Availability (HA)
- Run the app in **at least 2 data centers (= 2 AZs)**. Goal: **survive the loss of a data center**.
- Usually goes hand-in-hand with horizontal scaling.
- **Passive HA**: standby waits idle (e.g. **RDS Multi-AZ**).
- **Active HA**: all copies serve traffic (e.g. horizontal scaling across AZs).
- For EC2: **ASG multi-AZ + Load Balancer multi-AZ**.

> 🎯 Scalability ≠ High Availability. Scalability = handle more load. HA = survive failures.

---

## 2. Elastic Load Balancing (ELB)

### What & why
A **load balancer** is a server that forwards traffic to multiple downstream servers (e.g. EC2).

**Why use one?**
- Spread load across multiple instances
- Expose a **single point of access (DNS name)** for your app
- Seamlessly handle failures of instances
- Do regular **health checks**
- Provide **SSL termination (HTTPS)**
- Enforce **stickiness** with cookies
- **High availability across zones**
- Separate **public** traffic from **private** traffic

**Why an *Elastic* (managed) LB?**
- AWS guarantees it works, handles upgrades, maintenance, HA; gives only a few config knobs.
- Cheaper to run your own LB, but far more effort.
- Integrated with: EC2, EC2 ASG, Amazon ECS, **ACM** (certificates), CloudWatch, Route 53, AWS WAF, AWS Global Accelerator.

### Health Checks
- Let the LB know if an instance can reply to requests.
- Done on a **port + route** (e.g. HTTP, port 4567, `/health`).
- Response **not 200 (OK) → unhealthy** → LB stops sending traffic there.

### The 4 types of ELB 🎯

| Type | Gen / Year | Protocols / Layer | Key point |
|---|---|---|---|
| **CLB** – Classic | v1, 2009 (old) | HTTP, HTTPS, TCP, SSL (L4 + L7) | Legacy; avoid |
| **ALB** – Application | v2, 2016 | HTTP, HTTPS, **WebSocket** (L7) | Smart HTTP routing |
| **NLB** – Network | v2, 2017 | **TCP, TLS, UDP** (L4) | Extreme performance, static IP |
| **GWLB** – Gateway | 2020 | **IP packets (L3)** | Third-party security appliances |

- Recommended: use the **newer generation** (more features).
- LBs can be **internal (private)** or **external (public)**.

### Load Balancer Security Groups 🎯
- **LB SG**: allow HTTP (80) / HTTPS (443) **from anywhere** (`0.0.0.0/0`).
- **EC2 (application) SG**: allow HTTP **only from the LB's security group** (reference the SG, not an IP).
- Result: users can't bypass the LB and hit EC2 directly.

### 2.1 Classic Load Balancer (v1)
- TCP (L4), HTTP & HTTPS (L7). Health checks TCP or HTTP.
- Fixed hostname `XXX.region.elb.amazonaws.com`.

### 2.2 Application Load Balancer (ALB) — Layer 7
- Load balance to multiple HTTP apps across machines (**target groups**), and to multiple apps on the **same machine** (e.g. containers).
- Supports **HTTP/2** and **WebSocket**.
- Supports **redirects** (e.g. HTTP → HTTPS).
- **Routing rules** to different target groups based on:
  - **Path** in URL: `example.com/users` vs `example.com/posts`
  - **Hostname**: `one.example.com` vs `other.example.com`
  - **Query string / headers**: `example.com/users?id=123&order=false`
- 🎯 Great fit for **microservices & containers (Docker, Amazon ECS)**. Has **port mapping** to redirect to a **dynamic port in ECS**.
- With CLB you'd need **one CLB per application**; one ALB handles many.

**ALB Target Groups** (what it can send traffic to):
- **EC2 instances** (can be managed by an ASG) — HTTP
- **ECS tasks** (managed by ECS) — HTTP
- **Lambda functions** — HTTP request translated into a **JSON event**
- **IP addresses** — must be **private IPs** (e.g. on-premises servers)
- ALB can route to multiple target groups; **health checks are at the target-group level**.

*Example:* `?Platform=Mobile` → Target Group 1 (EC2 on AWS); `?Platform=Desktop` → Target Group 2 (on-prem private IPs).

**ALB good to know** 🎯
- Fixed hostname `XXX.region.elb.amazonaws.com`.
- App servers **don't see the client IP directly** (the LB terminates the connection and connects using its own private IP).
- True client IP is in header **`X-Forwarded-For`**; port in **`X-Forwarded-Port`**; protocol in **`X-Forwarded-Proto`**.

> ➕ ALB can also return **fixed responses** and supports **authentication** (Cognito / OIDC) on listener rules. ALB has **no static IP** (use NLB or Global Accelerator in front for that).

### 2.3 Network Load Balancer (NLB) — Layer 4
- Forward **TCP & UDP** traffic; handles **millions of requests/sec**; **ultra-low latency**.
- 🎯 **One static IP per AZ**, supports **Elastic IP** → useful when clients must **whitelist specific IPs**.
- 🎯 Use for **extreme performance, TCP or UDP** traffic.

**NLB Target Groups:**
- EC2 instances
- IP addresses (**private IPs**)
- **An Application Load Balancer** (NLB in front of ALB → static IP + HTTP routing 🎯)
- Health checks support **TCP, HTTP, HTTPS**.

### 2.4 Gateway Load Balancer (GWLB) — Layer 3
- Deploy, scale and manage a fleet of **3rd-party network virtual appliances**: **firewalls, IDS/IPS (Intrusion Detection & Prevention), Deep Packet Inspection**, payload manipulation.
- Operates on **IP packets (L3)**. Combines:
  - **Transparent Network Gateway** — single entry/exit for all traffic
  - **Load Balancer** — distributes traffic to your virtual appliances
- Flow: Users → (route table) → GWLB → security appliances inspect → back to GWLB → application.
- 🎯 Uses **GENEVE protocol on port 6081** (keyword = GWLB).
- Target groups: **EC2 instances**, **IP addresses (private)**.

### 2.5 Sticky Sessions (Session Affinity)
- Same client is always sent to the **same instance**.
- Works for **CLB, ALB, NLB**.
- For CLB & ALB, the stickiness **cookie has an expiration date you control**.
- Use case: user doesn't lose **session data**.
- Downside: may cause **load imbalance** across instances.

**Cookie names:**
- **Application-based cookies**
  - *Custom cookie*: generated by the **target** (your app); can hold custom attributes; name set per target group. **Don't use** `AWSALB`, `AWSALBAPP`, `AWSALBTG` (reserved).
  - *Application cookie*: generated by the **LB**; name **`AWSALBAPP`**.
- **Duration-based cookies**: generated by the **LB**; name **`AWSALB`** (ALB), **`AWSELB`** (CLB).

### 2.6 Cross-Zone Load Balancing
- **With** it: each LB node distributes evenly across **all registered instances in all AZs**.
  *e.g. AZ1 has 2 instances, AZ2 has 8 → every one of the 10 instances gets 10%.*
- **Without** it: each LB node only distributes to instances **in its own AZ**.
  *e.g. 50% to each AZ → AZ1's 2 instances get 25% each, AZ2's 8 get 6.25% each.*

| LB | Default | Inter-AZ data charge |
|---|---|---|
| **ALB** | **Enabled** (can disable at target-group level) | **No charge** |
| **NLB & GWLB** | **Disabled** | **You pay** if enabled |
| **CLB** | Disabled | No charge if enabled |

### 2.7 SSL/TLS Certificates
- SSL certificate = encrypts traffic between clients and LB (**in-flight / in-transit encryption**).
- **SSL** = Secure Sockets Layer; **TLS** = Transport Layer Security (newer). People still say "SSL" but mean TLS.
- Public certs issued by **Certificate Authorities (CA)**: Comodo, Symantec, GoDaddy, GlobalSign, Digicert, Let's Encrypt…
- Certs have an **expiration date** and must be renewed.

**On the load balancer:**
- Users → **HTTPS (encrypted) over internet** → LB → **HTTP over private VPC** → EC2 (this is "SSL termination").
- Uses an **X.509 certificate** (SSL/TLS server certificate).
- Manage certs with **ACM (AWS Certificate Manager)** or upload your own.
- **HTTPS listener**: must specify a **default certificate**; can add a list of certs for **multiple domains**; clients use **SNI** to say which hostname they want; can set a **security policy** to support older SSL/TLS versions (legacy clients).

**SNI — Server Name Indication** 🎯
- Solves loading **multiple SSL certificates on one web server** (to serve multiple websites).
- Client states the **target hostname in the initial SSL handshake**; server picks the correct cert (or returns default).
- Works only on **ALB, NLB, CloudFront**. **Not CLB.**

| LB | SSL certificates |
|---|---|
| CLB | Only **one** cert → need multiple CLBs for multiple hostnames |
| ALB | Multiple listeners, multiple certs, via **SNI** |
| NLB | Multiple listeners, multiple certs, via **SNI** |

### 2.8 Connection Draining / Deregistration Delay
- Name: **Connection Draining** (CLB); **Deregistration Delay** (ALB & NLB).
- Gives time to finish **in-flight requests** while an instance is de-registering or unhealthy.
- **Stops sending new requests** to that instance; new connections go to others.
- **1 – 3600 seconds (default 300 s)**. Set **0** to disable. Use a **low value if requests are short**.

---

## 3. Auto Scaling Groups (ASG)

### What it does
Load changes over time; the cloud lets you create/remove servers quickly. An ASG will:
- **Scale out** (add EC2) when load increases
- **Scale in** (remove EC2) when load decreases
- Keep a **minimum and maximum** number of instances
- **Automatically register new instances to a load balancer**
- **Re-create** an instance if one is terminated (e.g. unhealthy)
- 🎯 **ASGs are free** — you pay only for the EC2 instances.

**Capacity settings:** **Minimum** ≤ **Desired** ≤ **Maximum**. ASG scales out as needed, up to max.

**ASG + ELB:** the ELB can **health-check** instances; if the ASG uses ELB health checks, unhealthy instances get **terminated and replaced**.

### ASG Attributes
- A **Launch Template** (older **Launch Configurations are deprecated**) containing:
  - AMI + instance type
  - EC2 **User Data** (startup script)
  - EBS volumes
  - Security groups
  - SSH key pair
  - IAM roles for instances
  - Network + subnet info
  - Load balancer info
- **Min / Max / Initial capacity**
- **Scaling policies**

### Scaling with CloudWatch Alarms
- An alarm monitors a metric (e.g. **average CPU** or a **custom metric**), computed across all ASG instances.
- Based on the alarm → **scale-out policy** (add) or **scale-in policy** (remove).

### Scaling Policies 🎯
| Policy | How it works | Example |
|---|---|---|
| **Target Tracking** (dynamic) | Simplest. Keep a metric at a target. | "Keep average CPU ≈ 40%" |
| **Simple / Step Scaling** (dynamic) | CloudWatch alarm triggers fixed actions | CPU > 70% → add 2; CPU < 30% → remove 1 |
| **Scheduled** | Known usage patterns | Raise min capacity to 10 at 5 pm on Fridays |
| **Predictive** | Continuously **forecasts** load (ML) and schedules scaling ahead | Daily/weekly recurring patterns |

### Good metrics to scale on
- **CPUUtilization** — average CPU across instances
- **RequestCountPerTarget** — keep requests per instance stable (e.g. target value 3)
- **Average Network In / Out** — if app is network-bound
- **Any custom metric** you push to CloudWatch

### Scaling Cooldown
- After a scaling activity → **cooldown period (default 300 s)**.
- During cooldown, ASG **won't launch or terminate** more instances (lets metrics stabilise).
- Tip: use a **ready-to-use AMI** (pre-baked) → instances serve requests faster → can shorten cooldown.

> ➕ Extra exam bits: ASG health check types = **EC2** (default) or **ELB**. Default scale-in termination picks the **AZ with the most instances**, then the instance with the **oldest launch template/config**. **Lifecycle hooks** let you run actions when instances launch/terminate. **Instance Refresh** replaces instances to roll out a new launch template. **Warm-up time** stops new instances from counting in metrics until ready.

---

# PART B — RDS, Aurora & ElastiCache

## 4. Amazon RDS (Relational Database Service)

### Overview
- **Managed DB service** for databases using **SQL**. AWS manages the DB for you.
- Engines: **PostgreSQL, MySQL, MariaDB, Oracle, Microsoft SQL Server, IBM DB2, Aurora** (AWS proprietary).

### Why RDS instead of installing a DB on EC2?
RDS gives you:
- Automated provisioning, **OS patching**
- **Continuous backups** & **Point-in-Time Restore** (restore to a specific timestamp)
- Monitoring dashboards
- **Read replicas** for read performance
- **Multi-AZ** for **Disaster Recovery (DR)**
- Maintenance windows for upgrades
- Scaling (vertical & horizontal)
- Storage backed by **EBS**
- 🎯 **BUT you can't SSH into RDS instances** (except RDS Custom).

### RDS Storage Auto Scaling
- Increases storage **automatically** when running out of free space → no manual storage scaling.
- You must set a **Maximum Storage Threshold**.
- Triggers when **all** are true:
  - Free storage **< 10%** of allocated
  - Low storage lasts **≥ 5 minutes**
  - **6 hours** passed since last modification
- Good for **unpredictable workloads**. Supports **all RDS engines**.

### RDS Read Replicas — for read **scalability** 🎯
- **Up to 15 read replicas**.
- Within AZ, cross-AZ, or **cross-Region**.
- Replication is **ASYNC** → reads are **eventually consistent** (a replica may briefly lag behind).
- Replicas can be **promoted to their own standalone DB**.
- Apps must **update the connection string** to use replicas.
- **Use case:** prod DB under normal load; you want a **reporting/analytics** app → create a read replica and run it there; production unaffected.
- Replicas only for **SELECT (read)**, not INSERT/UPDATE/DELETE.

**Network cost** 🎯
- Normally AWS charges when data moves between AZs.
- **Read replicas in the same Region (different AZ) → FREE** replication traffic.
- **Cross-Region read replica → you pay ($$$)**.

### RDS Multi-AZ — for **Disaster Recovery / availability** 🎯
- **SYNC** replication from master (AZ A) to a **standby** (AZ B).
- **One DNS name** → **automatic app failover** to standby.
- Increases **availability**. Failover on loss of AZ, network, instance or storage failure.
- **No manual intervention** in apps.
- 🎯 **NOT used for scaling** (standby doesn't serve reads/writes).
- Read replicas **can also be set up as Multi-AZ** for DR.

| | Read Replicas | Multi-AZ |
|---|---|---|
| Purpose | **Scale reads** | **High availability / DR** |
| Replication | **ASYNC** | **SYNC** |
| Readable? | Yes | No (standby is passive) |
| Endpoint | Separate per replica | **One DNS name**, auto failover |
| Cross-Region | Yes | No (within Region) |

> ➕ Newer option: **Multi-AZ DB Cluster** = 1 writer + **2 readable standbys** in 3 AZs (faster failover).

### Single-AZ → Multi-AZ
- **Zero downtime** — just click **"Modify"** on the DB.
- Internally: **snapshot taken → new DB restored from snapshot in a new AZ → sync replication established**.

### RDS Custom
- Managed **Oracle & Microsoft SQL Server** with **OS and database customization**.
- Normal RDS: AWS manages entire DB + OS. **RDS Custom: full admin access** to the underlying OS and DB.
- You can: configure settings, install patches, enable native features, **access the EC2 instance via SSH or SSM Session Manager**.
- **De-activate Automation Mode** to customize — **take a DB snapshot before**.

---

## 5. Amazon Aurora

### Overview
- **Proprietary AWS** tech (not open source).
- **Compatible with PostgreSQL and MySQL** (same drivers work).
- "Cloud optimized": **5× performance of MySQL on RDS**, **3× PostgreSQL on RDS**.
- Storage **grows automatically in 10 GB increments, up to 256 TB**.
- **Up to 15 replicas**; replication faster than MySQL (**sub-10 ms lag**).
- **Instant failover**; HA-native.
- Costs **~20% more than RDS**, but more efficient.

### High Availability & Read Scaling 🎯
- **6 copies of data across 3 AZs**:
  - **4 of 6** needed for **writes**
  - **3 of 6** needed for **reads**
- **Self-healing** with peer-to-peer replication.
- Storage **striped across 100s of volumes** (shared storage volume).
- **One master** takes writes; **automated failover < 30 seconds**.
- Master + **up to 15 read replicas** serve reads.
- Supports **cross-Region replication**.

### Aurora DB Cluster — Endpoints 🎯
- **Writer Endpoint** → always points to the **master** (even after failover).
- **Reader Endpoint** → **connection load balancing** across all read replicas (works with replica auto scaling).
- Shared storage auto-expands 10 GB → 256 TB.

### Features of Aurora
Automatic failover · Backup & recovery · Isolation & security · Industry compliance · Push-button scaling · **Automated patching with zero downtime** · Advanced monitoring · Routine maintenance · **Backtrack**: restore data to any point in time **without using backups**.

### Aurora Replicas — Auto Scaling
- When read CPU usage rises, Aurora **adds replicas automatically**; the reader endpoint extends to include them.

### Aurora Custom Endpoints
- Define a **subset of instances** as a custom endpoint.
- e.g. run **analytical queries** on bigger replicas (`db.r5.2xlarge`) while others stay small.
- Reader endpoint generally **not used** after defining custom endpoints.

### Aurora Serverless 🎯
- Automated instantiation & **auto-scaling based on actual usage**.
- Good for **infrequent, intermittent or unpredictable** workloads.
- **No capacity planning**; **pay per second** → can be cheaper.
- Client connects through a **proxy fleet managed by Aurora**.

### Global Aurora 🎯
- **Aurora Cross-Region Read Replicas**: useful for DR, simple.
- **Aurora Global Database (recommended)**:
  - **1 primary Region** (read/write)
  - **Up to 10 secondary (read-only) Regions**, replication lag **< 1 second**
  - **Up to 16 read replicas per secondary Region**
  - Decreases latency for global users
  - Promoting another Region for DR: **RTO < 1 minute**
  - Typical cross-Region replication **< 1 second**

> ➕ **RTO** (Recovery Time Objective) = how long until you're back up. **RPO** (Recovery Point Objective) = how much data you can lose. Exam: "global, low-latency reads, DR < 1 min" → **Aurora Global Database**.

### Aurora Machine Learning
- Add **ML predictions** to apps **via SQL**.
- Integrates with **Amazon SageMaker** (any ML model) and **Amazon Comprehend** (sentiment analysis).
- **No ML experience needed**.
- Use cases: fraud detection, ads targeting, sentiment analysis, product recommendations.

### Babelfish for Aurora PostgreSQL
- Lets **Aurora PostgreSQL understand MS SQL Server commands (T-SQL)**.
- SQL Server apps work on Aurora PostgreSQL with **little/no code change** (same SQL Server client driver).
- Used after migrating DB with **AWS SCT** (Schema Conversion Tool) & **DMS** (Database Migration Service).

---

## 6. Backups, Restore & Cloning (RDS + Aurora)

### RDS Backups
- **Automated backups**:
  - **Daily full backup** (during backup window)
  - **Transaction logs backed up every 5 minutes** → restore to any point from oldest backup to **5 minutes ago**
  - Retention **1–35 days**; set **0 to disable**
- **Manual DB snapshots**: triggered by you; kept **as long as you want**.
- 🎯 **Trick:** a **stopped** RDS DB still costs **storage**. Stopping for a long time? → **snapshot & delete, restore later**.

### Aurora Backups
- **Automated**: **1–35 days, cannot be disabled**; point-in-time recovery in that window.
- **Manual snapshots**: kept as long as you want.

### Restore options
- Restoring a backup/snapshot **always creates a NEW database**.
- **MySQL RDS from S3**: back up on-prem DB → store in **S3** → restore onto new RDS MySQL.
- **MySQL Aurora from S3**: back up on-prem DB using **Percona XtraBackup** → store in S3 → restore onto new Aurora MySQL cluster. 🎯 (keyword: Percona XtraBackup → Aurora)

### Aurora Database Cloning 🎯
- Create a new Aurora cluster **from an existing one**; **faster than snapshot & restore**.
- Uses **copy-on-write**: the clone initially shares the same data volume (no copying); only changed data gets new storage.
- Very fast & cost-effective.
- Use case: create a **"staging" DB from "production"** without impacting prod.

---

## 7. RDS & Aurora Security 🎯
- **At-rest encryption**:
  - Master & replicas encrypted with **AWS KMS** — must be set **at launch time**.
  - If the **master isn't encrypted, read replicas can't be** encrypted.
  - To encrypt an unencrypted DB: **snapshot → restore as encrypted**.
- **In-flight encryption**: **TLS-ready by default**; use AWS TLS root certificates on the client.
- **IAM Authentication**: connect with **IAM roles** instead of username/password.
- **Security Groups**: control network access.
- **No SSH** (except **RDS Custom**).
- **Audit logs** can be sent to **CloudWatch Logs** for longer retention.

---

## 8. Amazon RDS Proxy 🎯
- **Fully managed database proxy** for RDS.
- Apps **pool and share DB connections** → reduces stress on DB (CPU, RAM), **minimizes open connections and timeouts**.
- **Serverless, autoscaling, highly available (multi-AZ)**.
- **Reduces RDS & Aurora failover time by up to 66%**.
- Supports RDS (MySQL, PostgreSQL, MariaDB, MS SQL Server) and Aurora (MySQL, PostgreSQL).
- **No code changes** needed for most apps.
- **Enforces IAM Authentication**; credentials stored in **AWS Secrets Manager**.
- **Never publicly accessible** — must be accessed **from within the VPC**.
- 🎯 Classic scenario: **many Lambda functions** opening too many DB connections → put **RDS Proxy** in between.

---

## 9. Amazon ElastiCache

### Overview
- RDS is to managed relational DBs as **ElastiCache is to managed Redis or Memcached**.
- Caches = **in-memory** databases → **very high performance, low latency**.
- **Reduces load on databases** for **read-intensive** workloads.
- Helps make your application **stateless**.
- AWS handles OS maintenance/patching, optimizations, setup, config, monitoring, failure recovery, backups.
- 🎯 **Using ElastiCache requires heavy application code changes** (app must talk to the cache).

> ➕ ElastiCache now also supports **Valkey** (open-source Redis fork). Treat it like Redis for the exam.

### Architecture 1 — DB Cache
- App queries **ElastiCache first**:
  - **Cache hit** → return data
  - **Cache miss** → read from **RDS**, then **write to cache**
- Relieves load on RDS.
- Needs an **invalidation strategy** so only current data is in cache.

### Architecture 2 — User Session Store
- User logs into any app instance → app **writes session data to ElastiCache**.
- User hits **another instance** → it reads the session from ElastiCache → user is **still logged in**.
- 🎯 Makes the app **stateless**.

### Redis vs Memcached 🎯
| Redis | Memcached |
|---|---|
| **Multi-AZ with auto-failover** | **Multi-node for partitioning (sharding)** |
| **Read replicas** to scale reads & HA | **No high availability (no replication)** |
| **Data durability** with **AOF persistence** | **Non-persistent** |
| **Backup & restore** | Backup & restore (**Serverless** only) |
| Supports **Sets and Sorted Sets** | **Multi-threaded** architecture |
| *Think: replication, HA, persistence* | *Think: simple, sharding, multi-threaded* |

### Cache Security
- **IAM Authentication for Redis** supported.
- **IAM policies** on ElastiCache are only for **AWS API-level** security (create/delete clusters, etc.).
- **Redis AUTH**: set a **password/token** when creating the cluster → extra layer on top of security groups.
- Supports **SSL in-flight encryption**.
- **Memcached**: supports **SASL-based authentication**.

### Caching Patterns 🎯
- **Lazy Loading**: all read data gets cached on a miss; data **can become stale**.
- **Write Through**: add/update cache **whenever writing to the DB** → **no stale data**.
- **Session Store**: store temporary session data using **TTL** (time-to-live → auto-expire).
- Quote: *"There are only two hard things in Computer Science: cache invalidation and naming things."*

### Redis Use Case — Gaming Leaderboard
- Leaderboards are computationally complex.
- **Redis Sorted Sets** guarantee **uniqueness + element ordering**; each new element is ranked in real time.
- 🎯 Keyword "real-time leaderboard" → **ElastiCache for Redis (Sorted Sets)**.

---

# PART C — Amazon Route 53

## 10. DNS Basics

**DNS (Domain Name System)** translates human-friendly hostnames into IP addresses: `www.google.com → 172.217.18.36`. It's the backbone of the internet and is **hierarchical**: `.com` → `example.com` → `www.example.com`, `api.example.com`.

### DNS terminology
| Term | Meaning |
|---|---|
| **Domain Registrar** | Where you buy domains: Amazon Route 53, GoDaddy… |
| **DNS Records** | A, AAAA, CNAME, NS, … |
| **Zone File** | Contains DNS records |
| **Name Server** | Resolves DNS queries (**Authoritative** or **Non-Authoritative**) |
| **TLD** (Top-Level Domain) | `.com`, `.us`, `.in`, `.gov`, `.org` |
| **SLD** (Second-Level Domain) | `amazon.com`, `google.com` |

**Anatomy of** `http://api.www.example.com.` → `http` = protocol · `api.www` = sub domain · `example` = SLD · `.com` = TLD · final `.` = **root**. Whole name = **FQDN** (Fully Qualified Domain Name).

### How DNS works (resolving `example.com`)
1. Browser asks the **Local DNS Server** (run by your company or ISP).
2. Local DNS asks the **Root DNS Server** (managed by **ICANN**) → "ask the `.com` server at 1.2.3.4".
3. Asks the **TLD DNS Server** for `.com` (managed by **IANA**, branch of ICANN) → "ask `example.com`'s name server at 5.6.7.8".
4. Asks the **SLD DNS Server** (managed by the **Domain Registrar**, e.g. Amazon Registrar) → "`example.com` = 9.10.11.12".
5. Local DNS returns IP to browser and **caches** each answer for its **TTL**. Browser connects to web server.

## 11. Amazon Route 53 Overview
- **Highly available, scalable, fully managed, Authoritative DNS**.
  - **Authoritative** = **you** can update the DNS records.
- Also a **Domain Registrar**.
- Can **health-check** your resources.
- 🎯 **The only AWS service with a 100% availability SLA**.
- Name: **53 = the traditional DNS port**.

### Records
Each record contains:
- **Domain/subdomain name** — e.g. `example.com`
- **Record type** — e.g. A, AAAA
- **Value** — e.g. `12.34.56.78`
- **Routing policy** — how Route 53 responds to queries
- **TTL** — how long the record is cached at DNS resolvers

Supported types: **must know A / AAAA / CNAME / NS**; advanced: CAA, DS, MX, NAPTR, PTR, SOA, TXT, SPF, SRV.

### Record types (must know) 🎯
- **A** → hostname to **IPv4**
- **AAAA** → hostname to **IPv6**
- **CNAME** → hostname to **another hostname** (target must have an A/AAAA record)
  - ❌ **Can't create a CNAME for the Zone Apex** (top node, e.g. `example.com`); ✅ OK for `www.example.com`
- **NS** → **Name Servers** for the hosted zone; control how traffic is routed for the domain

### Hosted Zones
- Container for records defining how to route traffic for a domain & subdomains.
- **Public Hosted Zone** → routing on the **Internet** (e.g. `application1.mypublicdomain.com`).
- **Private Hosted Zone** → routing **within one or more VPCs** (e.g. `application1.company.internal`, `db.example.internal` → private IPs).
- 🎯 **$0.50 per month per hosted zone**.

### Records TTL (Time To Live)
- Client **caches** the DNS answer for the TTL duration.
- **High TTL (e.g. 24 h)**: less Route 53 traffic (cheaper), but records may be **outdated**.
- **Low TTL (e.g. 60 s)**: more traffic (**$$**), records outdated for less time, **easy to change records**.
- TTL is **mandatory for every record except Alias records**.
- ➕ Tip: before a migration, **lower the TTL**, wait, then change the record.

### CNAME vs Alias 🎯
AWS resources (ELB, CloudFront…) expose an AWS hostname like `lb1-1234.us-east-2.elb.amazonaws.com`; you want `myapp.mydomain.com`.

| CNAME | Alias |
|---|---|
| Hostname → **any** other hostname | Hostname → **AWS resource** |
| **Only NON-root** domains (`something.mydomain.com`) | Works for **root (Zone Apex) AND non-root** |
| Charged queries | **Free of charge** |
| — | **Native health check** |

**Alias Records details:**
- Route 53-specific **extension to DNS**.
- **Automatically recognizes changes** in the resource's IP addresses.
- Can be used for the **Zone Apex** (`example.com`).
- Always of type **A / AAAA** (IPv4/IPv6).
- 🎯 **You can't set the TTL** (AWS sets it).

**Alias targets:** Elastic Load Balancers · CloudFront distributions · API Gateway · Elastic Beanstalk environments · S3 Websites · VPC Interface Endpoints · Global Accelerator · Route 53 record in the **same hosted zone**.
🎯 **You CANNOT set an Alias for an EC2 DNS name.**

## 12. Routing Policies 🎯
- Define **how Route 53 responds to DNS queries**.
- ⚠️ "Routing" here ≠ load-balancer routing. **DNS doesn't route traffic**; it only **answers queries**.
- Policies: **Simple, Weighted, Failover, Latency-based, Geolocation, Multi-Value Answer, Geoproximity (via Traffic Flow)**, plus **IP-based**.

### Simple
- Typically routes to a **single resource**.
- Can put **multiple values** in one record → all returned, **client picks one randomly**.
- With **Alias**, only **one** AWS resource.
- ❌ **Can't use health checks.**

### Weighted
- Control the **% of requests** to each resource.
- **traffic (%) = weight of this record ÷ sum of all weights**. e.g. weights 70 / 20 / 10 → 70% / 20% / 10%.
- Weights **don't need to sum to 100**.
- Records must have the **same name and type**.
- ✅ Health checks supported.
- Use cases: load balancing **between Regions**, **testing new app versions** (canary).
- **Weight 0** → stop sending traffic to that resource. If **all are 0** → all returned equally.

### Latency-based
- Route to the resource with the **lowest latency** for the user.
- Latency measured between **users and AWS Regions**.
- e.g. German users may go to the US if that's lowest latency.
- ✅ Health checks (failover capability).

### Failover (Active-Passive)
- **Primary** record with a **mandatory health check**; if unhealthy → Route 53 answers with the **Secondary** (DR).

### Geolocation
- Based on **where the user is** (≠ latency!).
- Specify by **continent, country, or US state** (most precise match wins).
- Create a **"Default" record** for no-match.
- Use cases: **website localization**, **restrict content distribution** (licensing), load balancing.
- ✅ Health checks.

### Geoproximity
- Route based on geographic location of **users AND resources**.
- **Bias** shifts more/less traffic:
  - Expand region: **1 to 99** (more traffic)
  - Shrink region: **-1 to -99** (less traffic)
- Resources: **AWS resources (specify Region)** or **non-AWS (latitude & longitude)**.
- 🎯 **Must use Route 53 Traffic Flow**.
- e.g. us-west-1 bias 0 vs us-east-1 bias 50 → the dividing line moves west; us-east-1 gets more users.

### IP-based Routing
- Based on **clients' IP addresses**.
- You provide a **CIDR collection** (list of IP ranges) mapped to locations/endpoints (e.g. `203.0.113.0/24` → location-1 → `1.2.3.4`).
- Use cases: optimize performance, reduce network costs; e.g. route users **from a particular ISP** to a specific endpoint.

### Multi-Value
- Route to **multiple resources**; Route 53 returns multiple values.
- ✅ Health checks → returns **only healthy** resources.
- **Up to 8 healthy records** returned per query.
- 🎯 **Not a substitute for an ELB** (it's client-side load balancing).
- Difference from Simple: Simple w/ multiple values has **no health checks**.

### Quick pick table
| Requirement | Policy |
|---|---|
| One resource, no health check | Simple |
| Split traffic by % / canary | Weighted |
| Fastest for user | Latency |
| Primary + DR backup | Failover |
| Content by user's country / compliance | Geolocation |
| Shift traffic between regions with bias | Geoproximity (Traffic Flow) |
| Route by client IP range / ISP | IP-based |
| Return several healthy IPs | Multi-Value |

## 13. Health Checks 🎯
- **HTTP health checks are only for public resources.**
- Health check → **automated DNS failover**. Three kinds:
  1. Monitor an **endpoint** (app, server, other AWS resource)
  2. Monitor **other health checks** (**Calculated** health checks)
  3. Monitor **CloudWatch Alarms** (full control) — e.g. DynamoDB throttles, RDS alarms, custom metrics → **helpful for private resources**
- Health checks are **integrated with CloudWatch metrics**.

### Monitor an Endpoint
- **~15 global health checkers** check the endpoint.
- Pass only with **2xx or 3xx** status codes.
- Can pass/fail based on **text in the first 5120 bytes** of the response.
- **Healthy/Unhealthy threshold: 3** (default).
- **Interval: 30 s** (can set **10 s** — higher cost).
- Protocols: **HTTP, HTTPS, TCP**.
- If **> 18% of checkers** report healthy → **Healthy**; otherwise Unhealthy.
- You can choose which checker **locations** to use.
- 🎯 Must **allow incoming requests from Route 53 health-checker IP ranges** in your firewall/security group (list at `https://ip-ranges.amazonaws.com/ip-ranges.json`).

### Calculated Health Checks
- Combine multiple health checks into one using **OR, AND, NOT**.
- Up to **256 child** health checks.
- Specify **how many children must pass** for parent to pass.
- Use: **maintenance on your website without all health checks failing**.

### Private Hosted Zones / private resources
- Route 53 health checkers are **outside the VPC** → **can't reach private endpoints** (private VPC or on-prem).
- Solution 🎯: create a **CloudWatch Metric + CloudWatch Alarm**, then a **health check that monitors the alarm**.

## 14. Domain Registrar vs DNS Service
- You **buy/register a domain** with a **Domain Registrar** (annual fee): GoDaddy, Amazon Registrar Inc., …
- Registrar usually provides DNS service, **but you can use another DNS service**.
- e.g. buy from **GoDaddy**, manage records in **Route 53**.

**Using a 3rd-party registrar with Route 53:** 🎯
1. Create a **Hosted Zone** in Route 53.
2. **Update the NS records** on the 3rd-party registrar's website to point to **Route 53's Name Servers**.

> **Domain Registrar ≠ DNS Service** (though registrars usually include some DNS features).

## 15. Route 53 Resolver & Hybrid DNS
- **Route 53 Resolver** (built into every VPC) automatically answers DNS queries for:
  - Local domain names for EC2 instances (e.g. `ec2-192-0-2-44.compute-1.amazonaws.com`)
  - Records in **Private Hosted Zones**
  - Records in **public name servers**
- **Hybrid DNS** = resolving DNS between your **VPC (Route 53 Resolver)** and **your other networks (other DNS resolvers)**.
- Networks can be: **the VPC itself / peered VPC**, or **on-premises** (via **Direct Connect** or **AWS VPN**).

### Resolver Endpoints 🎯
- **Inbound Endpoint** → **on-prem → AWS**: your on-prem DNS resolvers forward queries to it so they can resolve **AWS resources and Private Hosted Zone records** (e.g. on-prem server looks up `app.aws.private`).
- **Outbound Endpoint** → **AWS → on-prem**: Route 53 Resolver **forwards queries** to your on-prem DNS resolvers (e.g. EC2 looks up `web.onpremise.private`). Uses **forwarding rules**.
- Memory trick: direction is from **AWS's point of view** — *IN*bound = queries coming **into** AWS.

> ➕ Extra Route 53 items occasionally on SAA-C03: **DNSSEC** (protects against DNS spoofing), **Route 53 Resolver DNS Firewall** (block DNS queries to malicious domains from your VPC), **Application Recovery Controller** (readiness checks/routing controls for multi-region failover).

---

# 🔑 Rapid-Fire Exam Cheat Sheet (this section)

- Need **static IP / Elastic IP / TCP/UDP / millions of req/s** → **NLB**
- HTTP **path/host/query routing**, microservices, ECS dynamic ports, Lambda target → **ALB**
- **Firewall / IDS / IPS appliances**, **GENEVE 6081** → **GWLB**
- Get client's IP behind ALB → **`X-Forwarded-For`**
- Multiple SSL certs on one LB → **ALB/NLB with SNI** (not CLB)
- Users losing session behind LB → **sticky sessions** or (better) **ElastiCache session store**
- Let in-flight requests finish → **Deregistration delay / connection draining** (default 300 s)
- Keep CPU at X% → **Target tracking**; known event → **Scheduled**; recurring pattern forecast → **Predictive**
- Read-heavy reporting on DB → **Read Replica** (ASYNC, up to 15, same-region replication free)
- DB survives AZ failure → **Multi-AZ** (SYNC, one DNS name, not for scaling)
- Need OS access to managed Oracle/SQL Server → **RDS Custom**
- Unpredictable DB storage growth → **RDS Storage Auto Scaling**
- Unpredictable/intermittent DB workload, no capacity planning → **Aurora Serverless**
- Global app, < 1 s replication, RTO < 1 min → **Aurora Global Database**
- Staging copy of prod quickly → **Aurora Cloning**
- Rewind DB without restoring a backup → **Aurora Backtrack**
- SQL Server app on Aurora PostgreSQL → **Babelfish**
- ML predictions via SQL → **Aurora ML** (SageMaker / Comprehend)
- Encrypt existing unencrypted DB → **snapshot → copy/restore encrypted**
- Too many DB connections (Lambda) / faster failover / enforce IAM → **RDS Proxy**
- On-prem MySQL → Aurora → **Percona XtraBackup to S3**
- Cache needs HA, persistence, sorted sets, leaderboard → **Redis**; simple, multi-threaded, sharding → **Memcached**
- Avoid stale cache → **Write-through**; cache only what's read → **Lazy loading**
- Point **root domain** (`example.com`) to ELB/CloudFront → **Alias** (not CNAME); can't Alias to EC2 DNS name
- Only AWS service with **100% SLA** → **Route 53**
- Health-check a **private** resource → **CloudWatch Alarm + health check**
- Route by **user location** (compliance) → **Geolocation**; by **lowest latency** → **Latency**; with **bias** → **Geoproximity (Traffic Flow)**
- On-prem needs to resolve AWS private names → **Inbound Resolver Endpoint**; AWS needs on-prem names → **Outbound**
- Domain bought at GoDaddy, DNS in Route 53 → **create hosted zone + update NS records at GoDaddy**
