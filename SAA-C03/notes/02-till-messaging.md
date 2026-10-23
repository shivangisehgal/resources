# AWS SAA-C03 Notes — Part 2 (Slides pg 227–414)

**Covers:** Classic Solutions Architecture · Elastic Beanstalk · Amazon S3 (basics → advanced → security) · CloudFront & Global Accelerator · AWS Storage Extras (Snowball, FSx, Storage Gateway, Transfer Family, DataSync) · Integration & Messaging (SQS, SNS, Kinesis, Data Firehose, Amazon MQ)

> Legend: 🎯 = exam favourite / keyword trigger · ➕ = extra info not on the slides but useful for the exam

---

## 0. Mini Glossary for this part

| Term | Plain English |
|---|---|
| **Stateless app** | Server keeps no user data between requests → any server can answer any request. Easy to scale. |
| **Stateful app** | Server must remember things (shopping cart, login). Harder to scale. |
| **Elastic IP** | A fixed public IPv4 address you own and can attach to an EC2 instance. |
| **EBS** | Elastic Block Store — a network "hard drive" attached to **one EC2 instance in one AZ**. |
| **EFS** | Elastic File System — a network file system (NFS) shared by **many Linux instances across AZs**. Attaches via an **ENI** (network interface). |
| **Object storage (S3)** | Store whole files ("objects") via HTTP API; not a disk you mount. |
| **Block storage** | Raw disk (EBS, instance store). **File storage** = shared folders (EFS, FSx). |
| **Protocols** | **NFS** = Linux file sharing · **SMB** = Windows file sharing · **iSCSI** = block storage over network · **FTP/SFTP** = file transfer. |
| **Decoupling** | Putting a buffer (queue/topic/stream) between apps so they don't depend on each other's speed or uptime. |
| **Edge location** | AWS mini data center near users (hundreds worldwide), used by CloudFront & Global Accelerator. |

---

# PART A — Classic Solutions Architecture

> These case studies show *how services fit together*. The exam is scenario-based, so be 100% comfortable here.

## 1. WhatIsTheTime.com — Stateless Web App
**Requirements:** tells people the time; **no database**; start small (downtime OK) → later scale vertically & horizontally with **no downtime**.

The evolution (each step fixes the previous problem):

| Step | Architecture | Problem |
|---|---|---|
| 1 | One **public EC2 + Elastic IP** | Can't handle more load |
| 2 | **Scale vertically** (e.g. upgrade to M5) | **Downtime** during upgrade |
| 3 | **Scale horizontally**: several EC2s each with Elastic IP | Users must know many IPs; limited Elastic IPs |
| 4 | Drop Elastic IPs; **Route 53 A record** (TTL 1 hour) with all IPs | When an instance is removed, users still have the old IP cached for 1 hour → **errors** |
| 5 | **ELB + health checks**, Route 53 **Alias record** → ELB; EC2s now **private**; **security group** on EC2 only allows the LB | Adding/removing instances is manual |
| 6 | Add an **Auto Scaling Group** behind the ELB | Single AZ = disaster risk |
| 7 | ELB + ASG **Multi-AZ** (AZ 1–3) | Cost |
| 8 | Minimum 2 AZs always running → **reserve that minimum capacity** (Reserved Instances) | ✅ Cost savings |

**Lessons:** public vs private IPs · Elastic IP vs Route 53 vs Load Balancers · Route 53 TTL, A records vs Alias · manual EC2 vs ASG · Multi-AZ to survive disasters · ELB health checks · security group rules · reserve capacity for cost savings.

## 2. MyClothes.com — Stateful Web App
**Requirements:** online clothes shop with **shopping cart**, hundreds of concurrent users; must scale horizontally and stay **as stateless as possible**; users **must not lose their cart**; user details (address…) stored in a **database**.

Start: Route 53 → Multi-AZ ELB → ASG (3 AZs). Problem: user's next request hits another instance → **cart lost**. Solutions:

| Option | How | Pros / Cons |
|---|---|---|
| **ELB Stickiness** (session affinity) | Same user → same instance | Works, but if that instance dies the cart is lost; load imbalance |
| **User cookies** | Store cart contents in browser cookies | App becomes **stateless** ✅ but HTTP requests heavier, **security risk** (cookies can be altered), cookies must be validated, **< 4 KB** |
| **Server session** 🎯 | Send only a **session_id** in cookie; store session data in **ElastiCache** (alternative: **DynamoDB**) | Best: secure, stateless instances |

**Then the data layer:**
- **Store user data** (address, name) in **Amazon RDS**.
- **Scale reads**: **RDS Read Replicas** (master handles writes), **or** cache with **ElastiCache (Lazy Loading)** → cache hit avoids DB.
- **Survive disasters**: **Multi-AZ** for ELB, ASG, **ElastiCache (Redis Multi-AZ)** and **RDS Multi-AZ**.
- **Security groups** 🎯 (chained referencing):
  - ELB: open **HTTP/HTTPS to 0.0.0.0/0**
  - EC2: allow traffic **only from the LB's SG**
  - ElastiCache: allow only from **EC2's SG**
  - RDS: allow only from **EC2's SG**

**This is the classic 3-tier web architecture.**

## 3. MyWordPress.com — Stateful Web App with shared files
**Requirements:** fully scalable WordPress site; must display **uploaded pictures**; user data & blog content in **MySQL**.

- **DB layer:** RDS Multi-AZ → better: **Aurora MySQL** (easy **Multi-AZ + Read Replicas**, less ops).
- **Storing images:**
  - **EBS** works for a **single instance** — but with multiple instances in multiple AZs, each instance has its **own EBS volume** → an image uploaded to one isn't visible on another ❌.
  - **EFS** 🎯 → network file system shared by **all instances across AZs** (each AZ connects via an **ENI**) ✅.
- **Lesson:** **EBS = single-instance app**, **EFS = distributed app** (shared files).

## 4. Instantiating Applications Quickly
Launching a full stack (EC2, EBS, RDS) is slow: install apps, insert initial/recovery data, configure, launch. Speed it up:

- **EC2**:
  - **Golden AMI** 🎯: pre-install apps & OS dependencies → launch from it.
  - **User Data bootstrapping**: scripts for **dynamic** configuration at boot.
  - **Hybrid**: Golden AMI + User Data (this is what **Elastic Beanstalk** does).
- **RDS**: **restore from a snapshot** → schemas & data ready.
- **EBS**: **restore from a snapshot** → disk already formatted with data.

## 5. Typical 3-Tier Web App (memorise this picture)
```
Route 53 → ELB (PUBLIC subnet, Multi-AZ)
         → ASG of EC2 (PRIVATE subnet, 3 AZs)
         → ElastiCache (sessions + cached data) & Amazon RDS (read/write)  (DATA subnet)
```

## 6. AWS Elastic Beanstalk
**Developer problems on AWS:** managing infra, deploying code, configuring DBs & LBs, scaling. Most web apps use the same architecture (ALB + ASG). Developers just want their **code to run**, consistently across apps & environments.

**Overview**
- **Developer-centric** way to deploy an app on AWS.
- Uses components we know: **EC2, ASG, ELB, RDS…**
- **Managed**: handles capacity provisioning, load balancing, scaling, health monitoring, instance configuration.
- Developer is only responsible for the **application code**; still **full control over configuration**.
- 🎯 **Beanstalk is free** — pay only for underlying resources.

**Components**
- **Application**: collection of Beanstalk components (environments, versions, configs).
- **Application Version**: one iteration of your code.
- **Environment**: AWS resources running **one application version at a time**.
  - **Tiers**: **Web Server Environment Tier** & **Worker Environment Tier**.
  - You can create multiple environments (dev, test, prod).
- Flow: Create Application → Upload Version → Launch Environment → Manage Environment (→ deploy new versions).

**Supported platforms:** Go, Java SE, Java with Tomcat, .NET Core on Linux, .NET on Windows Server, Node.js, PHP, Python, Ruby, Packer Builder, Single-Container Docker, Multi-container Docker, Preconfigured Docker.

**Web Server Tier vs Worker Tier** 🎯
- **Web tier**: ELB → ASG of web-server EC2s (URL like `myapp.us-east-1.elasticbeanstalk.com`).
- **Worker tier**: ASG of worker EC2s that **pull messages from an SQS queue**.
  - **Scales based on number of SQS messages**.
  - The web tier can **push messages to the worker's SQS queue** (offload long jobs).

**Deployment modes**
- **Single Instance** (great for **dev**): 1 EC2 + Elastic IP (+ RDS master).
- **High Availability with Load Balancer** (great for **prod**): ALB + ASG across AZs + RDS Master/Standby (Multi-AZ).

---

# PART B — Amazon S3 (Simple Storage Service)

## 7. S3 Basics
- One of the **main building blocks** of AWS; "**infinitely scaling**" storage; backbone of many websites and integrated with many AWS services.

**Use cases:** backup & storage · disaster recovery · archive · hybrid cloud storage · application hosting · media hosting · data lakes & big data analytics · software delivery · **static websites**. (e.g. Nasdaq stores 7 years of data in S3 Glacier; Sysco runs analytics on S3 data.)

### Buckets
- Store **objects (files)** in **buckets** (top-level "directories").
- 🎯 Buckets are created **in a Region** (S3 *looks* global, but buckets are regional).
- **Naming:**
  - **Shared Global Namespace** – name must be **globally unique** (all regions, all accounts).
  - **Account Regional Namespace** – allows reusing the same bucket name across regions.
- **Naming rules:** no uppercase, no underscore, not an IP, 3–63 chars ➕, must start with **lowercase letter or number**, must **not** start with `xn--`, must **not** end with `-s3alias`.

### Objects
- Each object has a **Key** = the **full path**: `s3://my-bucket/my_folder1/another_folder/my_file.txt`.
- Key = **prefix** (`my_folder1/another_folder/`) + **object name** (`my_file.txt`).
- 🎯 **No real directories** — just long key names containing `/` (the console fakes folders).
- **Object value** = file content.
  - **Max object size: 50 TB** (per slides).
  - Uploading **> 5 GB → must use Multi-Part Upload**.
- **Metadata**: key/value pairs (system or user).
- **Tags**: Unicode key/value, **up to 10** → useful for security & lifecycle.
- **Version ID** (if versioning enabled).

## 8. S3 Security (access)
- **User-based**: **IAM policies** → which API calls a specific IAM user can make.
- **Resource-based**:
  - **Bucket policies** 🎯 – bucket-wide rules, **allow cross-account**.
  - **Object ACL** – finer grain (can be disabled).
  - **Bucket ACL** – less common (can be disabled).
- 🎯 An IAM principal can access an object if **(IAM permissions ALLOW it OR resource policy ALLOWS it) AND there's no explicit DENY**.
- **Encryption**: encrypt objects with keys (see §15).

### Bucket Policies
- **JSON**. Elements:
  - **Resources**: buckets and objects
  - **Effect**: Allow / Deny
  - **Actions**: set of APIs (e.g. `s3:GetObject`)
  - **Principal**: account or user the policy applies to
- Use to: **grant public access**, **force encryption at upload**, **grant cross-account access**.

**Who-uses-what examples** 🎯
| Scenario | Use |
|---|---|
| Anonymous public website visitors | **Bucket policy** allowing public access |
| IAM user in your account | **IAM policy** |
| EC2 instance needs S3 | **IAM Role** attached to EC2 (never put keys on the instance) |
| User in **another AWS account** | **Bucket policy** allowing cross-account |

### Block Public Access
- Settings to **prevent company data leaks**. If a bucket should never be public, **leave them ON**.
- Can be set at the **account level**.

## 9. S3 Static Website Hosting
- Host static sites accessible on the internet.
- URL (depends on region): `http://bucket-name.s3-website-aws-region.amazonaws.com` **or** `http://bucket-name.s3-website.aws-region.amazonaws.com`.
- 🎯 **403 Forbidden?** → make sure the **bucket policy allows public reads** (and Block Public Access is off).

## 10. S3 Versioning
- Enabled at the **bucket level**.
- Overwriting the same key creates a new version: 1, 2, 3…
- Best practice: protects against **unintended deletes** (restore a version) and allows **easy rollback**.
- Notes: files uploaded **before** enabling versioning have version **`null`**; **suspending** versioning **does not delete** previous versions.

## 11. S3 Replication (CRR & SRR)
- 🎯 **Versioning must be enabled on source AND destination**.
- **CRR** = Cross-Region Replication; **SRR** = Same-Region Replication.
- Buckets can be in **different AWS accounts**.
- Copying is **asynchronous**. Must give **proper IAM permissions** to S3.
- **Use cases:** **CRR** → compliance, lower-latency access, cross-account replication. **SRR** → log aggregation, live replication between prod and test accounts.

**Replication notes** 🎯
- Only **new objects** are replicated after enabling. For **existing** objects (and failed ones) → **S3 Batch Replication**.
- **DELETE operations:**
  - Can replicate **delete markers** (optional setting).
  - Deletions **with a version ID are NOT replicated** (avoid malicious deletes).
- **No chaining**: bucket 1 → 2 → 3 does **not** copy objects from 1 into 3.

## 12. S3 Storage Classes
Classes: **Standard (General Purpose)** · **Standard-IA** · **One Zone-IA** · **Glacier Instant Retrieval** · **Glacier Flexible Retrieval** · **Glacier Deep Archive** · **Intelligent-Tiering**. Move between them manually or via **Lifecycle configurations**.

### Durability vs Availability
- **Durability**: **99.999999999% (11 9's)** across multiple AZs — **same for all classes**. Store 10 million objects → lose one every **10,000 years** on average.
- **Availability**: how readily available; **varies by class**. e.g. S3 Standard **99.99%** = ~**53 minutes unavailable per year**.

### Each class
- **S3 Standard – General Purpose**: 99.99% availability; **frequently accessed** data; low latency, high throughput; survives **2 concurrent facility failures**. Use: big data analytics, mobile & gaming apps, content distribution.
- **Infrequent Access (IA)** — less frequent access but **rapid access when needed**; cheaper than Standard (but retrieval fee):
  - **Standard-IA**: 99.9% availability. Use: **disaster recovery, backups**.
  - **One Zone-IA**: 11 9's durability **in a single AZ** — **data lost if AZ destroyed**; 99.5% availability. Use: **secondary backup copies of on-prem data**, or **data you can recreate**.
- **Glacier** (archive/backup; pay storage + **retrieval cost**):
  - **Glacier Instant Retrieval**: **millisecond** retrieval; great for data accessed **once a quarter**; min storage **90 days**.
  - **Glacier Flexible Retrieval** (formerly "S3 Glacier"): **Expedited 1–5 min**, **Standard 3–5 hours**, **Bulk 5–12 hours (free)**; min **90 days**.
  - **Glacier Deep Archive**: long-term; **Standard 12 hours**, **Bulk 48 hours**; min **180 days**; cheapest.
- **Intelligent-Tiering**: small monthly **monitoring & auto-tiering fee**; moves objects automatically based on usage; **no retrieval charges**. Tiers:
  - **Frequent Access** (automatic, default)
  - **Infrequent Access** (automatic): not accessed **30 days**
  - **Archive Instant Access** (automatic): not accessed **90 days**
  - **Archive Access** (optional): configurable **90 → 700+ days**
  - **Deep Archive Access** (optional): configurable **180 → 700+ days**

### Comparison table 🎯
| | Standard | Intelligent-Tiering | Standard-IA | One Zone-IA | Glacier Instant | Glacier Flexible | Glacier Deep Archive |
|---|---|---|---|---|---|---|---|
| Durability | 11 9's | 11 9's | 11 9's | 11 9's | 11 9's | 11 9's | 11 9's |
| Availability | 99.99% | 99.9% | 99.9% | 99.5% | 99.9% | 99.99% | 99.99% |
| Availability SLA | 99.9% | 99% | 99% | 99% | 99% | 99.9% | 99.9% |
| AZs | ≥3 | ≥3 | ≥3 | **1** | ≥3 | ≥3 | ≥3 |
| Min storage duration | None | None | 30 days | 30 days | 90 days | 90 days | 180 days |
| Min billable object size | None | None | 128 KB | 128 KB | 128 KB | 40 KB | 40 KB |
| Retrieval fee | None | None | per GB | per GB | per GB | per GB | per GB |
| Retrieval time | instant | instant | instant | instant | **ms** | 1 min – 12 h | 12 – 48 h |

**Price example (us-east-1, storage per GB/month):** Standard $0.023 · Intelligent-Tiering $0.0025–$0.023 · Standard-IA $0.0125 · One Zone-IA $0.01 · Glacier Instant $0.004 · Glacier Flexible $0.0036 · Deep Archive $0.00099. Intelligent-Tiering monitoring: $0.0025 per 1000 objects. Glacier Flexible retrieval: Expedited $10, Standard $0.05, Bulk free (per 1000 requests); Deep Archive: Standard $0.10, Bulk $0.025.
*(Don't memorise prices — just the order: Standard > IA > One Zone-IA > Glacier Instant > Flexible > Deep Archive.)*

### S3 Express One Zone
- **High-performance, single-AZ** storage class; objects stored in a **Directory Bucket** (bucket in one AZ).
- **100,000s requests/sec** with **single-digit ms latency**.
- Up to **10× faster than S3 Standard**, **50% lower** request costs.
- Durability 11 9's, availability **99.95%**.
- **Co-locate storage & compute in the same AZ** → lower latency.
- Use cases: latency-sensitive apps, data-intensive apps, **AI/ML training**, financial modeling, media processing, **HPC**. Best integrated with **SageMaker Model Training, Athena, EMR, Glue**.

## 13. Lifecycle Rules
- Transition objects between classes: infrequent → **Standard-IA**; archive → **Glacier / Deep Archive**. Automate with **Lifecycle Rules**.
- **Transition actions**: e.g. move to Standard-IA **60 days** after creation; to Glacier after **6 months**.
- **Expiration actions** (delete after some time):
  - Access log files deleted after **365 days**
  - Delete **old versions** (if versioning enabled)
  - Delete **incomplete Multi-Part uploads** 🎯
- Rules can target a **prefix** (`s3://mybucket/mp3/*`) or **object tags** (`Department: Finance`).

**Scenario 1:** Thumbnails (re-creatable, keep 60 days) + source images (immediate access for 60 days, then up to 6 h wait OK).
→ Source images: **Standard**, lifecycle → **Glacier** after 60 days. Thumbnails: **One Zone-IA**, lifecycle **expire after 60 days**.

**Scenario 2:** Recover deleted objects **immediately for 30 days**, then **within 48 hours up to 365 days**.
→ **Enable Versioning** (deletes become hidden "delete markers" → recoverable). Transition **noncurrent versions → Standard-IA**, then later → **Glacier Deep Archive**.

### S3 Analytics – Storage Class Analysis
- Helps decide **when to transition** objects.
- Recommendations for **Standard and Standard-IA only** (**not One Zone-IA or Glacier**).
- **.csv report updated daily**; **24–48 hours** to start seeing data.
- Good first step to create/improve Lifecycle Rules.

## 14. More S3 Features

### Requester Pays
- Normally the **bucket owner pays** storage + data transfer.
- With **Requester Pays**, the **requester pays** for the request and **data download**; owner still pays storage.
- Good for **sharing large datasets** with other accounts.
- 🎯 Requester **must be authenticated in AWS** (not anonymous).

### S3 Event Notifications
- Events: `S3:ObjectCreated`, `S3:ObjectRemoved`, `S3:ObjectRestore`, `S3:Replication`…
- **Object name filtering** possible (`*.jpg`).
- Use case: **generate thumbnails** of uploaded images.
- Create as many events as you want.
- Destinations: **SNS, SQS, Lambda**.
- Usually delivered **in seconds**, sometimes a minute or longer.
- 🎯 **Permissions**: done via **resource (access) policies on the destination** — SNS resource policy, SQS access policy, Lambda resource policy (not IAM roles on S3).

**With Amazon EventBridge:** *all* S3 events → EventBridge → rules → **over 18 AWS services** as destinations.
- **Advanced filtering** with JSON rules (metadata, object size, name…)
- **Multiple destinations** (e.g. Step Functions, Kinesis Streams/Firehose)
- EventBridge capabilities: **archive, replay events, reliable delivery**.

## 15. S3 Performance
### Baseline
- Auto-scales to high request rates; latency **100–200 ms**.
- 🎯 **3,500 PUT/COPY/POST/DELETE** and **5,500 GET/HEAD** requests **per second per prefix**.
- **No limit on number of prefixes**.
- Prefix examples: `bucket/folder1/sub1/file` → prefix `/folder1/sub1/`; `bucket/1/file` → `/1/`.
- Spread reads across **4 prefixes** → **22,000 GET/HEAD per second**.

### Optimising uploads
- **Multi-Part Upload**: **recommended > 100 MB**, **must use > 5 GB**; parallelises uploads.
- **S3 Transfer Acceleration** 🎯: upload to a nearby **edge location**, which forwards over the **private AWS network** to the bucket in the target region (e.g. file in USA → bucket in Australia). **Compatible with multi-part upload**.

### Optimising downloads — Byte-Range Fetches
- **Parallelise GETs** by requesting specific **byte ranges**; better resilience to failures.
- Speed up downloads, or retrieve **only part** of a file (e.g. the **header / first XX bytes**).

## 16. S3 Batch Operations
- Bulk operations on **existing objects** with **one request**:
  - Modify object metadata & properties
  - Copy objects between buckets
  - **Encrypt un-encrypted objects** 🎯
  - Modify ACLs, tags
  - Restore objects from Glacier
  - Invoke **Lambda** for custom action on each object
- A **job** = list of objects + action + optional parameters.
- Manages **retries, progress tracking, completion notifications, reports**.
- Use **S3 Inventory** to get the object list and **Athena** to query/filter it → feed into Batch Operations.

## 17. S3 Storage Lens
- Understand, analyse & optimise storage across an entire **AWS Organization**.
- Discover anomalies, cost efficiencies, data-protection best practices (**30 days** of usage & activity metrics).
- Aggregate by Organization, accounts, regions, buckets, or prefixes.
- Default dashboard or custom dashboards. **Export metrics daily** to an S3 bucket (**CSV, Parquet**).
- **Default dashboard**: multi-region & multi-account data; preconfigured by S3; **can't be deleted, but can be disabled**.

**Metric categories:**
| Category | Examples | Use |
|---|---|---|
| **Summary** | StorageBytes, ObjectCount | Fastest-growing / unused buckets & prefixes |
| **Cost-Optimization** | NonCurrentVersionStorageBytes, IncompleteMultipartUploadStorageBytes | Incomplete multipart > 7 days; objects to move to cheaper class |
| **Data-Protection** | VersioningEnabledBucketCount, MFADeleteEnabledBucketCount, SSEKMSEnabledBucketCount, CrossRegionReplicationRuleCount | Buckets not following best practices |
| **Access-management** | ObjectOwnershipBucketOwnerEnforcedBucketCount | Which Object Ownership settings are used |
| **Event** | EventNotificationEnabledBucketCount | Which buckets have event notifications |
| **Performance** | TransferAccelerationEnabledBucketCount | Which buckets use Transfer Acceleration |
| **Activity** | AllRequests, GetRequests, PutRequests, ListRequests, BytesDownloaded | How storage is requested |
| **Detailed Status Code** | 200OKStatusCount, 403ForbiddenErrorCount, 404NotFoundErrorCount | HTTP status insights |

**Free vs Paid:**
- **Free**: available to all; ~**28 usage metrics**; data queryable for **14 days**.
- **Advanced (paid)**: Activity, Advanced Cost Optimization, Advanced Data Protection, Status Code metrics; **CloudWatch publishing** (no extra charge); **prefix-level aggregation**; data kept **15 months**.

## 18. S3 Encryption 🎯 (very common on exam)

### 4 methods
| Method | Who manages keys | Header | Notes |
|---|---|---|---|
| **SSE-S3** | AWS (S3-owned keys) | `x-amz-server-side-encryption: AES256` | **AES-256**; **enabled by default** for new buckets & objects |
| **SSE-KMS** | **AWS KMS** (you control keys) | `x-amz-server-side-encryption: aws:kms` | **User control + audit key usage with CloudTrail** |
| **SSE-C** | **Customer**, outside AWS | Key sent in HTTP headers on **every request** | S3 **does NOT store** your key; **HTTPS mandatory** |
| **Client-Side** | **Customer** fully | — | Encrypt **before** upload, decrypt **after** download (e.g. S3 Client-Side Encryption Library) |

**SSE-KMS limitation** 🎯
- Upload calls **`GenerateDataKey`** KMS API; download calls **`Decrypt`**.
- Counts toward the **KMS quota per second** (**5,500 / 10,000 / 30,000 req/s** depending on region) → possible **throttling** at high throughput.
- Request a quota increase via **Service Quotas Console**.
- ➕ **S3 Bucket Keys** reduce KMS API calls (and cost) by up to 99%.

### Encryption in transit (SSL/TLS)
- S3 has an **HTTP endpoint** (unencrypted) and **HTTPS endpoint** (encrypted in flight). HTTPS recommended; **mandatory for SSE-C**; most clients use HTTPS by default.
- 🎯 **Force encryption in transit**: bucket policy **Deny** when **`aws:SecureTransport` = false**.

### Default Encryption vs Bucket Policies
- **SSE-S3 is automatically applied** to new objects.
- Optionally **"force encryption"** with a bucket policy that refuses PUTs without encryption headers (SSE-KMS or SSE-C).
- 🎯 **Bucket policies are evaluated BEFORE default encryption.**

## 19. CORS (Cross-Origin Resource Sharing) 🎯
- **Origin** = scheme (protocol) + host (domain) + port. e.g. `https://www.example.com` (implied port 443 for HTTPS, 80 for HTTP).
- **Browser-based** mechanism to allow requests to **other origins** while visiting the main origin.
  - Same origin: `http://example.com/app1` & `http://example.com/app2`
  - Different origins: `http://www.example.com` & `http://other.example.com`
- The other origin must allow it via **CORS headers** (e.g. **`Access-Control-Allow-Origin`**).
- Flow: browser sends **preflight `OPTIONS`** request with `Origin` header → server replies with `Access-Control-Allow-Origin` and `Access-Control-Allow-Methods` (GET, PUT, DELETE) → browser makes the actual request.
- **S3 CORS:** if a client makes a cross-origin request to your bucket, **enable CORS headers on the bucket being requested** (the *assets* bucket, not the HTML bucket). Allow a specific origin or `*`.
  - e.g. `index.html` from `my-bucket-html` loads `coffee.jpg` from `my-bucket-assets` → configure CORS on **my-bucket-assets**.

## 20. MFA Delete
- **MFA** = Multi-Factor Authentication (code from phone app like Google Authenticator or a hardware device).
- **MFA required to:** **permanently delete an object version**, **suspend versioning**.
- **Not required to:** enable versioning, list deleted versions.
- 🎯 **Versioning must be enabled**; **only the bucket owner (root account)** can enable/disable MFA Delete.

## 21. S3 Access Logs
- Log **all requests** (any account, authorized or denied) into **another S3 bucket** for audit; analyse with data tools (e.g. Athena).
- Target logging bucket **must be in the same AWS Region**.
- ⚠️ **Never make the logging bucket the monitored bucket** → **logging loop**, bucket grows exponentially.

## 22. Pre-Signed URLs 🎯
- Generate via **S3 Console, AWS CLI or SDK**.
- **Expiration:** Console **1 min – 720 min (12 h)**; CLI `--expires-in` in seconds (**default 3600 s, max 604800 s ≈ 168 h / 7 days**).
- Users with the URL **inherit the permissions of the user who generated it** (for GET / PUT).
- Examples:
  - Only **logged-in users** can download a **premium video** from a private bucket
  - Ever-changing list of users downloading files → generate URLs dynamically
  - Temporarily let a user **upload** to a precise location in the bucket

## 23. WORM: Glacier Vault Lock & S3 Object Lock 🎯
**WORM = Write Once Read Many** (can't modify/delete) — for **compliance & data retention**.

**S3 Glacier Vault Lock**
- Create a **Vault Lock Policy**, then **lock it** → can **never be changed or deleted**. Objects can't be deleted.

**S3 Object Lock** (**versioning must be enabled**)
- Block deletion of an **object version** for a specified time.
- **Compliance mode**: **nobody** (not even **root**) can overwrite/delete; retention mode can't change, period **can't be shortened**.
- **Governance mode**: most users can't overwrite/delete or change lock settings; **some users with special permissions can**.
- **Retention period**: protect for a fixed period; **can be extended**.
- **Legal Hold**: protect **indefinitely**, independent of retention period; placed/removed freely with **`s3:PutObjectLegalHold`** IAM permission.

## 24. S3 Access Points
- Simplify security management for buckets with many user groups.
- e.g. **Finance Access Point** (R/W `/finance` prefix), **Sales Access Point** (R/W `/sales`), **Analytics Access Point** (Read entire bucket) — bucket policy stays simple.
- Each Access Point has **its own DNS name** (**Internet origin** or **VPC origin**) and an **access point policy** (like a bucket policy) → manage security at scale.

**VPC Origin**
- Access point accessible **only from within a VPC**.
- Must create a **VPC Endpoint** (Gateway or Interface) to reach it.
- **VPC Endpoint Policy must allow access to the target bucket and Access Point** (three layers: endpoint policy → access point policy → bucket policy).

## 25. S3 Object Lambda
- Use **Lambda functions to change an object before it's returned** to the caller.
- Only **one bucket** needed; create an **S3 Access Point** + **S3 Object Lambda Access Points** on top.
- Use cases 🎯:
  - **Redacting PII** for analytics or non-prod environments
  - **Converting formats** (XML → JSON)
  - **Resizing & watermarking images** on the fly per requester
- e.g. e-commerce app reads original; analytics app gets a **redacted** object; marketing app gets an **enriched** object (Lambda joins with a customer loyalty DB).

---

# PART C — CloudFront & Global Accelerator

## 26. Amazon CloudFront
- **CDN (Content Delivery Network)**: improves **read performance** by **caching content at the edge** → better user experience.
- **Hundreds of Points of Presence** (edge locations, caches) worldwide.
- **DDoS protection** (because worldwide), integrates with **AWS Shield** and **AWS WAF**.

### Origins
- **S3 bucket**: distribute & cache files at the edge; also **upload files to S3 through CloudFront**; secured with **Origin Access Control (OAC)** 🎯.
- **VPC Origin**: apps in **VPC private subnets** — private **ALB / NLB / EC2**.
- **Custom Origin (HTTP)**: **S3 website** (bucket must be static-website enabled) or **any public HTTP backend** (e.g. public ALB).

### How it works
Client → **edge location** → if in **local cache** return it; else **forward request to origin** (S3 or HTTP), cache the response, return it.

### S3 as origin
- Edges (LA, Mumbai, Melbourne, São Paulo…) fetch from the S3 bucket over the **private AWS network**.
- Bucket stays private; use **OAC + S3 bucket policy** allowing only CloudFront.

### CloudFront vs S3 Cross-Region Replication 🎯
| CloudFront | S3 CRR |
|---|---|
| Global edge network | Set up **per region** you want |
| Files **cached for a TTL** (maybe a day) | Files updated **near real-time** |
| Great for **static** content available **everywhere** | **Read only**; great for **dynamic** content at low latency in **a few regions** |

### ALB or EC2 as origin
- **Using VPC Origins** (recommended): deliver from **private subnets** (private ALB, NLB, EC2) — **no internet exposure**.
- **Using Public Network**:
  - EC2 as origin → EC2 **must be public**; SG must **allow public IPs of edge locations** (list: `http://d7uri8nf7uskq.cloudfront.net/tools/list-cloudfront-ips`).
  - ALB as origin → **ALB must be public** (SG allows edge IPs); EC2 behind it **can be private** (SG allows the ALB's SG).

### Geo Restriction
- **Allowlist**: only users in approved countries.
- **Blocklist**: block users in banned countries.
- Country determined via a **3rd-party Geo-IP database**.
- ➕ Use case: copyright/licensing laws.

### Cache Invalidations 🎯
- If you update the origin, CloudFront only fetches new content **after TTL expires**.
- Force a full or partial refresh with a **CloudFront Invalidation**: all files (`*`) or a path (`/images/*`).

## 27. AWS Global Accelerator
**Problem:** global users hitting your app (e.g. public ALB in India) over the **public internet** → many **hops**, high latency. We want to use the **AWS network** ASAP.

**Unicast vs Anycast IP**
- **Unicast**: one server holds one IP.
- **Anycast**: **all servers hold the same IP**, client is routed to the **nearest** one.

**How Global Accelerator works**
- Creates **2 Anycast IPs** for your app.
- Anycast IPs send traffic to the nearest **edge location** → then over the **private AWS network** to your app.

**Features**
- Works with **Elastic IP, EC2, ALB, NLB** — **public or private**.
- **Consistent performance**: intelligent routing to lowest latency, **fast regional failover**; **no client cache issues** (IP never changes); internal AWS network.
- **Health checks**: makes app global; **failover < 1 minute**; great for **disaster recovery**.
- **Security**: only **2 external IPs to whitelist**; **DDoS protection via AWS Shield**.

### Global Accelerator vs CloudFront 🎯
**Both** use the AWS global network & edge locations, and integrate with **AWS Shield**.

| CloudFront | Global Accelerator |
|---|---|
| Improves **cacheable** content (images, videos) **and dynamic** content (API acceleration, dynamic sites) | Improves performance for many apps over **TCP or UDP** |
| Content is **served (cached) at the edge** | **Proxies packets** at the edge to apps in one or more Regions (**no caching**) |
| HTTP(S) | Good for **non-HTTP**: **gaming (UDP), IoT (MQTT), Voice over IP** |
| — | HTTP apps needing **static IP addresses** |
| — | HTTP apps needing **deterministic, fast regional failover** |

---

# PART D — AWS Storage Extras

## 28. AWS Snowball (Snow Family)
- **Highly secure, portable devices** to **collect & process data at the edge** and **migrate data into/out of AWS** — up to **petabytes**.

| Device | vCPUs | Memory | Storage (SSD) |
|---|---|---|---|
| **Snowball Edge Storage Optimized** | 104 | 416 GB | **210 TB** |
| **Snowball Edge Compute Optimized** | 104 | 416 GB | 28 TB |

### Data migrations
Network transfer times:
| Data | 100 Mbps | 1 Gbps | 10 Gbps |
|---|---|---|---|
| 10 TB | 12 days | 30 hours | 3 hours |
| 100 TB | 124 days | 12 days | 30 hours |
| 1 PB | 3 years | 124 days | 12 days |

- Challenges with network: limited connectivity, limited bandwidth, high network cost, shared bandwidth, connection stability.
- 🎯 **Rule: if it takes more than a week over the network → use Snowball.**
- Flow: AWS ships device → you load data → **ship back** → AWS **imports into S3**.

### Edge Computing
- Process data **where it's created**, at places with limited internet & no compute: a **truck, a ship, a mining station**.
- Use **Snowball Edge** (Compute Optimized dedicated for it, or Storage Optimized) to **run EC2 instances or Lambda functions** at the edge.
- Use cases: **preprocess data, machine learning, transcoding media**.

### Snowball into Glacier 🎯
- **Snowball can't import directly into Glacier** → import into **S3** first, then use an **S3 Lifecycle policy** to transition to Glacier.

> ➕ Note: AWS has been phasing out Snowcone/Snowmobile and limiting new Snowball customers; exam questions may still reference them. Snowmobile = truck for exabyte-scale.

## 29. Amazon FSx
Launch **3rd-party high-performance file systems** on AWS, **fully managed**. Four flavours: **Lustre, Windows File Server, NetApp ONTAP, OpenZFS**.

### FSx for Windows File Server
- Fully managed **Windows file share**.
- Supports **SMB** protocol & **Windows NTFS**.
- **Microsoft Active Directory** integration, ACLs, user quotas.
- **Can be mounted on Linux EC2** too.
- Supports Microsoft **DFS (Distributed File System) Namespaces** — group files across multiple file systems.
- Scales to **10s of GB/s, millions of IOPS, 100s of PB**.
- Storage: **SSD** (latency-sensitive: databases, media processing, analytics) or **HDD** (broad: home directories, CMS).
- Accessible **from on-premises** (VPN or Direct Connect).
- Can be **Multi-AZ** (HA). Data **backed up daily to S3**.

### FSx for Lustre 🎯
- **Lustre** = parallel distributed file system for **large-scale computing** (name = "**Linux**" + "**cluster**").
- Use: **Machine Learning, HPC (High Performance Computing)**, video processing, financial modeling, electronic design automation.
- Scales to **100s GB/s, millions of IOPS, sub-ms latency**.
- Storage: **SSD** (low latency, IOPS-intensive, small & random files) or **HDD** (throughput-intensive, large & sequential files).
- 🎯 **Seamless S3 integration**: **read S3 as a file system** and **write results back to S3**.
- Usable from on-premises (VPN / Direct Connect).

**Deployment options**
| Scratch File System | Persistent File System |
|---|---|
| **Temporary** storage | **Long-term** storage |
| Data **not replicated** (lost if server fails) | Data **replicated within same AZ** |
| **High burst** (6× faster, 200 MBps per TiB) | Failed files replaced within minutes |
| Short-term processing, **optimise cost** | Long-term processing, **sensitive data** |

### FSx for NetApp ONTAP
- Managed **NetApp ONTAP**; compatible with **NFS, SMB, iSCSI**.
- Move workloads running on **ONTAP or NAS** to AWS.
- Works with Linux, Windows, macOS, VMware Cloud on AWS, Amazon WorkSpaces & AppStream 2.0, EC2, ECS, EKS.
- Storage **shrinks or grows automatically**.
- Snapshots, replication, low cost, **compression and data de-duplication**.
- **Point-in-time instantaneous cloning** (test new workloads).
- 🎯 Keyword: "**broadest OS compatibility**" / "NFS + SMB + iSCSI".

### FSx for OpenZFS
- Managed **OpenZFS**; compatible with **NFS (v3, v4, v4.1, v4.2)** only.
- Move workloads running on **ZFS** to AWS.
- Works with same list (Linux, Windows, macOS, VMware Cloud, WorkSpaces, AppStream, EC2, ECS, EKS).
- Up to **1,000,000 IOPS with < 0.5 ms latency**.
- Snapshots, compression, low cost; **point-in-time instantaneous cloning**.
- (No de-duplication, unlike ONTAP.)

## 30. Hybrid Cloud & AWS Storage Gateway

**Hybrid cloud** = part of infra in the cloud, part on-premises. Reasons: long migrations, security, compliance, IT strategy.
S3 is proprietary (unlike EFS/NFS) → how to expose S3 data on-prem? → **AWS Storage Gateway**.

**AWS cloud-native storage options**
- **Block**: Amazon **EBS**, EC2 **Instance Store**
- **File**: Amazon **EFS**, Amazon **FSx**
- **Object**: Amazon **S3**, **Glacier**

### Storage Gateway overview
- **Bridge between on-premises data and cloud data**.
- Use cases: **disaster recovery, backup & restore, tiered storage, on-prem cache & low-latency file access**.
- Types: **S3 File Gateway**, **Volume Gateway**, **Tape Gateway**.

### S3 File Gateway
- S3 buckets accessible on-prem via **NFS and SMB**.
- **Most recently used data cached** in the gateway.
- Supports **S3 Standard, Standard-IA, One Zone-IA, Intelligent-Tiering**.
- Transition to **Glacier via Lifecycle Policy**.
- Bucket access via **IAM roles** per File Gateway.
- **SMB** integrates with **Active Directory** for user authentication.

### Volume Gateway
- **Block storage via iSCSI**, backed by S3.
- Backed by **EBS snapshots** → can help **restore on-prem volumes**.
- **Cached volumes**: low-latency access to **most recent** data (full data in AWS).
- **Stored volumes**: **entire dataset on-premises**, scheduled backups to S3.

### Tape Gateway
- For companies with **physical tape** backups.
- **Virtual Tape Library (VTL)** backed by **S3 and Glacier**.
- Use **existing tape-based processes** (iSCSI interface); works with leading backup software vendors.
- Virtual tapes stored in **S3**; archived tapes in **Glacier / Deep Archive**.

### Deployment summary 🎯
| Gateway | On-prem protocol | Stored in AWS |
|---|---|---|
| **File Gateway** | **NFS / SMB** (file shares) | **S3** (any class, incl. Glacier via lifecycle) |
| **Volume Gateway** | **iSCSI** (block) | **S3** + **EBS snapshots** (not Glacier) |
| **Tape Gateway** | **iSCSI VTL** | **S3** tape library → **Glacier / Deep Archive** archive |

- All have a **local cache**; run as a **VM (VMware, Hyper-V, KVM)** on-prem ➕ (or a hardware appliance); **encryption in transit**; connect over Internet or **Direct Connect**.

## 31. AWS Transfer Family
- Fully managed **file transfers into/out of S3 or EFS** using **FTP protocols**.
- Protocols:
  - **AWS Transfer for FTP** (File Transfer Protocol) — **only within VPC**
  - **AWS Transfer for FTPS** (FTP over SSL)
  - **AWS Transfer for SFTP** (Secure FTP)
- Managed, scalable, reliable, **highly available (multi-AZ)**.
- Pricing: **per provisioned endpoint per hour + data transfer per GB**.
- Store & manage users' credentials in the service, or **integrate** with existing auth (**Microsoft AD, LDAP, Okta, Amazon Cognito, custom**).
- Uses an **IAM Role** to access S3/EFS; optional **Route 53** for your own hostname.
- Usage: sharing files, public datasets, CRM, ERP…

## 32. AWS DataSync 🎯
- Move **large amounts of data** to and from:
  - **On-premises / other cloud → AWS** (NFS, SMB, HDFS, S3 API…) — **needs an agent**
  - **AWS → AWS** (between storage services) — **no agent needed**
- Can sync to: **S3 (any class, including Glacier)**, **EFS**, **FSx** (Windows, Lustre, NetApp, OpenZFS).
- **Scheduled** replication: hourly, daily, weekly (not continuous).
- 🎯 **Preserves file permissions & metadata** (NFS POSIX, SMB…).
- **One agent task can use 10 Gbps**; can set a **bandwidth limit**.
- On-prem: DataSync **Agent** connects over **TLS**; AWS **Snowcone** has the agent **pre-installed** (for limited-network sites).

## 33. Storage Comparison 🎯 (summary)
| Service | What it is |
|---|---|
| **S3** | Object storage |
| **S3 Glacier** | Object archival |
| **EBS volumes** | Network storage for **one EC2 instance at a time** |
| **Instance Storage** | **Physical** storage on the EC2 host (**high IOPS**, ephemeral) |
| **EFS** | Network file system for **Linux** instances, POSIX |
| **FSx for Windows** | Network file system for **Windows** servers |
| **FSx for Lustre** | **HPC Linux** file system |
| **FSx for NetApp ONTAP** | **High OS compatibility** |
| **FSx for OpenZFS** | Managed **ZFS** file system |
| **Storage Gateway** | S3 & FSx File Gateway, Volume Gateway (cached & stored), Tape Gateway |
| **Transfer Family** | **FTP, FTPS, SFTP** interface on top of S3 or EFS |
| **DataSync** | **Scheduled** data sync on-prem → AWS, or AWS → AWS |
| **Snowcone / Snowball / Snowmobile** | Move large data to the cloud **physically** |
| **Database** | Specific workloads, usually with indexing & querying |

> ➕ Also: **FSx File Gateway** (mentioned in the summary) gives on-prem **low-latency cached access to FSx for Windows** file shares.

---

# PART E — AWS Integration & Messaging (SQS, SNS, Kinesis, MQ)

## 34. Why messaging?
Multiple apps need to talk. Two patterns:
1. **Synchronous** (app → app directly): e.g. Buying Service → Shipping Service. Problematic with **sudden traffic spikes** (e.g. normally encode 10 videos, suddenly 1000).
2. **Asynchronous / event-based** (app → **queue** → app).

**Decouple** apps with:
- **SQS** — queue model
- **SNS** — pub/sub model
- **Kinesis** — real-time streaming model

These **scale independently** of your app.

## 35. Amazon SQS (Simple Queue Service)
**Queue** = producers **send** messages → queue stores them → consumers **poll** messages.

### Standard Queue
- **Oldest** AWS offering (10+ years); fully managed; used to **decouple** apps.
- **Unlimited throughput**, **unlimited number of messages** in queue.
- Retention: **default 4 days, max 14 days**.
- **Low latency** (< 10 ms publish & receive).
- Max message size **1,024 KB** (1 MB) (per slides).
- **At-least-once delivery** → can have **duplicates** occasionally.
- **Best-effort ordering** → can be **out of order**.

### Producing
- Via SDK **`SendMessage`** API. Message **persists until a consumer deletes it**.
- e.g. send an order: order id, customer id, any attributes.

### Consuming
- Consumers (EC2, servers, **Lambda**) **poll** SQS (**up to 10 messages at a time**).
- Process (e.g. insert into RDS), then **delete** with **`DeleteMessage`** API.
- **Multiple consumers** receive & process **in parallel**; scale consumers **horizontally** for throughput.

### SQS + Auto Scaling Group 🎯
- CloudWatch metric **Queue Length = `ApproximateNumberOfMessages`** → CloudWatch Alarm → **scale the ASG** of consumers.

### Patterns 🎯
- **Decouple application tiers**: front-end web app → `SendMessage` → SQS (infinitely scalable) → back-end processing app (e.g. video processing). Each tier auto-scales independently.
- **SQS as a buffer to database writes**: if load is too big, direct DB inserts may **lose transactions** → enqueue writes in SQS, consumers dequeue & insert into RDS/Aurora/DynamoDB → **no data loss**.

### Security
- **Encryption**: in-flight via **HTTPS API**; at-rest via **KMS**; **client-side** if desired.
- **Access control**: **IAM policies** for the SQS API.
- **SQS Access Policies** (like bucket policies): for **cross-account** access and allowing **other services (SNS, S3…) to write** to the queue.

### Message Visibility Timeout 🎯
- After a consumer polls a message, it becomes **invisible to other consumers**.
- **Default 30 seconds** → message must be processed within that time.
- After timeout, message is **visible again** → may be **processed twice**.
- Consumer can call **`ChangeMessageVisibility`** to get more time.
- Too **high** (hours) → if consumer crashes, re-processing takes long. Too **low** (seconds) → **duplicates**.

### Long Polling 🎯
- Consumer **waits** for messages to arrive if queue is empty.
- **Fewer API calls**, more efficient, **lower latency**.
- Wait time **1–20 seconds** (**20 preferable**). Preferable to short polling.
- Enable at **queue level** or per API call with **`WaitTimeSeconds`**.

### FIFO Queue 🎯
- **First In, First Out** ordering.
- **Limited throughput**: **300 msg/s without batching, 3,000 msg/s with batching**.
- **Exactly-once send** (removes duplicates via **Deduplication ID**).
- Messages processed **in order** by consumer.
- Ordering by **Message Group ID** (mandatory) — messages in the same group are ordered.
- ➕ FIFO queue names must end with `.fifo`.

> ➕ Also know: **Dead-Letter Queue (DLQ)** for messages that fail repeatedly; **Delay Queue** (delay up to 15 min) — "individual message delay capability".

## 36. Amazon SNS (Simple Notification Service)
**Problem:** one message → **many receivers** (email, fraud service, shipping service, SQS queue). Direct integrations = messy. → **Pub/Sub**: Buying Service publishes **once to an SNS topic**; all subscribers receive it.

- **Event producer** sends to **one SNS topic**.
- **As many subscribers** as you want; **each subscriber gets all messages** (unless filtered).
- **Up to 12,500,000 subscriptions per topic**; **100,000 topics** limit.
- **Subscribers:** **SQS, Lambda, Kinesis Data Firehose, Emails, SMS & mobile notifications, HTTP(S) endpoints**.
- Many AWS services publish to SNS: **CloudWatch Alarms, AWS Budgets, Lambda, ASG notifications, S3 events, DynamoDB, CloudFormation state changes, AWS DMS, RDS events**…

### How to publish
- **Topic Publish (SDK)**: create topic → create subscription(s) → publish to topic.
- **Direct Publish (mobile apps SDK)**: create platform application → create platform endpoint → publish to endpoint. Works with **Google GCM, Apple APNS, Amazon ADM**.

### Security
- Same as SQS: **HTTPS in-flight**, **KMS at-rest**, client-side optional; **IAM policies**; **SNS Access Policies** for **cross-account** and allowing **other services (S3…)** to write to the topic.

### SNS + SQS: Fan Out 🎯
- **Push once to SNS, receive in all subscribed SQS queues**.
- Fully decoupled, **no data loss**.
- SQS adds: **data persistence, delayed processing, retries**.
- Add more SQS subscribers over time.
- ⚠️ SQS **access policy must allow SNS to write**.
- **Cross-Region delivery**: works with SQS queues in other regions.

**Application: S3 events to multiple queues** 🎯
- For the same **event type + prefix** (e.g. object create on `images/`), you can only have **one S3 event rule**.
- To send the same S3 event to many queues → **S3 → SNS topic → fan-out** to multiple SQS queues (and Lambda).

**Application: SNS → S3 via Kinesis Data Firehose**
- Buying Service → SNS topic → **Kinesis Data Firehose** → **S3** (or any supported KDF destination).

### SNS FIFO Topic
- Ordered (FIFO) topic. Similar to SQS FIFO:
  - Ordering by **Message Group ID**
  - **Deduplication** via Deduplication ID or **Content-Based Deduplication**
- Subscribers can be **SQS Standard and FIFO** queues.
- **Limited throughput** (same as SQS FIFO).
- 🎯 **SNS FIFO + SQS FIFO fan-out** → when you need **fan-out + ordering + deduplication**.

### Message Filtering 🎯
- **JSON filter policy** on each subscription filters which messages it gets.
- **No filter policy → receives every message**.
- e.g. new transaction `{Order: 1036, Product: Pencil, Qty: 4, State: Placed}` → queue with filter `State: Placed` gets it; queues for `Cancelled`, `Declined` don't; queue with no filter (All) gets everything.

## 37. Amazon Kinesis Data Streams
- **Collect and store streaming data in real time**.
- **Producers**: applications, **click streams**, **IoT devices**, **metrics & logs**, **Kinesis Agent**.
- **Consumers**: applications, **Lambda**, **Amazon Data Firehose**, **Managed Service for Apache Flink**.

**Features**
- Retention **up to 365 days**.
- 🎯 Consumers can **reprocess (replay)** data.
- Data **can't be deleted** (until it expires).
- Records up to **10 MiB** (typically many small real-time records).
- **Ordering guaranteed** for data with the same **Partition ID**.
- **KMS at-rest** + **HTTPS in-flight** encryption.
- **KPL (Kinesis Producer Library)** for optimised producers; **KCL (Kinesis Client Library)** for optimised consumers.

**Capacity modes** 🎯
| Provisioned | On-demand |
|---|---|
| Choose number of **shards** | No capacity management |
| Each shard: **1 MB/s in** (or 1000 records/s), **2 MB/s out** | Default **4 MB/s in** (or 4000 records/s) |
| **Scale manually** (add/remove shards) | **Auto-scales** based on observed peak in last **30 days** |
| Pay **per shard per hour** | Pay **per stream per hour + data in/out per GB** |

## 38. Amazon Data Firehose (formerly "Kinesis Data Firehose")
- **Fully managed**, **serverless**, **auto-scaling**, pay for what you use.
- **Producers**: apps, clients, SDK, Kinesis Agent, **Kinesis Data Streams**, **CloudWatch (Logs & Events)**, **AWS IoT**. Records up to **1 MB**.
- Optional **data transformation with Lambda** (e.g. CSV → JSON).
- **Batch writes** to destinations:
  - **AWS**: **Amazon S3, Amazon Redshift, Amazon OpenSearch**
  - **3rd-party**: Splunk, MongoDB, Datadog, New Relic…
  - **Custom HTTP Endpoint**
- Can send **all or failed data** to an **S3 backup bucket**.
- 🎯 **Near real-time** (buffers by **size / time**).
- Supports CSV, JSON, Parquet, Avro, raw text, binary. **Converts to Parquet / ORC**, compresses with **gzip / snappy**.

### Kinesis Data Streams vs Data Firehose 🎯
| Kinesis Data Streams | Amazon Data Firehose |
|---|---|
| Streaming data **collection** | **Load** streaming data into S3 / Redshift / OpenSearch / 3rd party / HTTP |
| You write **producer & consumer code** | **Fully managed** |
| **Real-time** | **Near real-time** |
| Provisioned / On-Demand | **Automatic scaling** |
| **Data storage up to 365 days** | **No data storage** |
| **Replay capability** | **No replay** |

## 39. SQS vs SNS vs Kinesis 🎯
| SQS | SNS | Kinesis |
|---|---|---|
| Consumers **pull** data | **Push** data to many subscribers | Standard: **pull** (2 MB/s per shard); **Enhanced fan-out: push** (2 MB/s per shard **per consumer**) |
| Data **deleted after consumed** | Data **not persisted** (lost if not delivered) | **Replay** possible; data **expires after X days** |
| As many workers as you want | Up to 12,500,000 subscribers; 100,000 topics | Meant for **real-time big data, analytics, ETL** |
| No throughput provisioning | No throughput provisioning | Provisioned or on-demand |
| Ordering **only on FIFO** queues | **FIFO** capability (for SQS FIFO subscribers) | Ordering **at shard level** |
| **Individual message delay** | Integrates with SQS for **fan-out** | |

## 40. Amazon MQ 🎯
- SQS & SNS are **cloud-native, proprietary AWS protocols**.
- Traditional **on-premises** apps may use **open protocols**: **MQTT, AMQP, STOMP, OpenWire, WSS**.
- When **migrating to the cloud**, instead of re-engineering for SQS/SNS → use **Amazon MQ**.
- **Managed message broker** for **RabbitMQ** and **ActiveMQ**.
- **Doesn't scale as much** as SQS/SNS.
- **Runs on servers**; can run **Multi-AZ with failover**.
- Has both **queue** features (~SQS) and **topic** features (~SNS).

**High Availability:** **Active** broker in AZ-a + **Standby** broker in AZ-b; on failure the client **fails over** to standby. Both share storage on **Amazon EFS**.

---

# 🔑 Rapid-Fire Exam Cheat Sheet (this part)

**Architecture**
- Keep app stateless while storing sessions → **ElastiCache** (or **DynamoDB**) session store with session_id cookie
- Shared uploaded files across instances in multiple AZs → **EFS** (not EBS)
- Speed up EC2 boot → **Golden AMI** (+ User Data for dynamic config); DB/disk ready → **restore from snapshot**
- Developers just want to deploy code → **Elastic Beanstalk**; long background jobs → **Beanstalk Worker tier + SQS**
- Tight security in 3-tier → **security groups referencing each other**

**S3**
- Upload > 5 GB → **Multi-Part Upload** (recommended > 100 MB)
- Faster long-distance uploads → **Transfer Acceleration**; faster/partial downloads → **Byte-Range Fetches**
- 3,500 writes / 5,500 reads per second **per prefix** → spread across prefixes
- Replicate existing objects → **S3 Batch Replication**; encrypt existing unencrypted objects → **S3 Batch Operations**
- Unknown/changing access patterns → **Intelligent-Tiering**; re-creatable infrequent data → **One Zone-IA**; quarterly access, ms retrieval → **Glacier Instant**; cheapest, 12–48 h OK → **Deep Archive**
- Delete incomplete multipart uploads / old versions → **Lifecycle expiration**
- Recover accidentally deleted objects → **Versioning**; protect against accidental permanent delete → **MFA Delete**
- Can't be deleted by anyone incl. root → **Object Lock Compliance mode** / **Glacier Vault Lock**; indefinite hold → **Legal Hold**
- Share large dataset, others pay transfer → **Requester Pays**
- Temporary access to a private object → **Pre-signed URL**
- Audit of KMS key usage → **SSE-KMS** (CloudTrail); KMS throttling → quota increase / Bucket Keys
- Customer keeps keys outside AWS, S3 encrypts → **SSE-C** (HTTPS only)
- Force HTTPS → bucket policy with **`aws:SecureTransport`**
- Website JS on one bucket fetching from another → **CORS** on the target bucket
- Static website 403 → **bucket policy public read**
- Different teams/prefixes, simple bucket policy → **S3 Access Points**
- Redact PII / transform object on read → **S3 Object Lambda**
- Org-wide storage insights → **S3 Storage Lens**; when to transition → **S3 Analytics**
- One S3 event to many consumers → **S3 → SNS → SQS fan-out** (or **EventBridge**)

**CloudFront / Global Accelerator**
- Cache static content globally, protect private S3 → **CloudFront + OAC**
- Private ALB/EC2 behind CDN → **CloudFront VPC Origins**
- Block countries → **CloudFront Geo Restriction**
- Updated content not showing → **CloudFront Invalidation**
- Static IPs, UDP/TCP, gaming/IoT/VoIP, fast regional failover → **Global Accelerator**

**Storage extras**
- > 1 week to transfer over network / PBs → **Snowball**; compute in a ship/truck/mine → **Snowball Edge**
- Snowball to Glacier → **S3 + lifecycle**
- Windows SMB + Active Directory → **FSx for Windows**
- HPC / ML, S3 integration → **FSx for Lustre** (Scratch = temp/cheap, Persistent = long-term)
- NFS + SMB + iSCSI, broad compatibility, dedup → **FSx for NetApp ONTAP**; ZFS → **FSx for OpenZFS**
- On-prem NFS/SMB to S3 → **S3 File Gateway**; iSCSI block → **Volume Gateway**; tape backups → **Tape Gateway**
- FTP/SFTP into S3/EFS → **Transfer Family**
- Scheduled sync on-prem ↔ AWS, preserve permissions/metadata → **DataSync**

**Messaging**
- Decouple tiers / buffer DB writes → **SQS**
- Scale consumers → ASG on **`ApproximateNumberOfMessages`**
- Message processed twice → increase **visibility timeout** / `ChangeMessageVisibility`
- Reduce API calls / empty responses → **Long Polling** (20 s)
- Ordering + exactly-once → **SQS FIFO** (300/3000 msg/s, Message Group ID)
- One message to many → **SNS**; with persistence & retries → **SNS + SQS fan-out**; with ordering → **SNS FIFO + SQS FIFO**
- Subscribers get only some messages → **SNS message filtering**
- Real-time streaming, replay, custom consumers → **Kinesis Data Streams**
- Load streaming data into S3/Redshift/OpenSearch, near real-time, no code → **Data Firehose**
- Migrating on-prem apps using MQTT/AMQP/STOMP (RabbitMQ/ActiveMQ) → **Amazon MQ**
