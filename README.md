# Financial Tracking & Analytics Suite (fina)

Financial Tracking & Analytics Suite (fina): Developed a secure financial management platform utilizing Java and Redis for high-concurrency transaction processing and automated database backups.

## Overview

fina is a backend-focused financial platform that records transactions, tracks balances, and produces analytics. The core is a Java service backed by Redis for fast, concurrent access, with scheduled backups to protect persisted data.

> **Status:** transition in progress. The previous Next.js frontend has been removed; the Java/Redis service is next to be scaffolded.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language / runtime | Java |
| Data / cache | [Redis](https://redis.io) — high-concurrency transaction processing |
| Concurrency | [Redisson](https://redisson.org) distributed locks or Redis Lua scripts |
| Persistence | Hybrid RDB + AOF (`appendfsync everysec`) with automated backups |
| High availability | Redis Sentinel or Cluster topology with automated failover |
| API | REST (JSON) |
| Build tool | Maven or Gradle (to be chosen during scaffolding) |

## Getting Started

The Java service has not been scaffolded yet. Once it is, this section will cover:

```bash
# 1. Start Redis
redis-server

# 2. Build and run the service (command depends on the chosen build tool)
./mvnw spring-boot:run   # or: ./gradlew bootRun
```

Prerequisites:

- Java 17 or newer
- Redis 7 or newer
- Maven or Gradle

## Planned Project Structure

```text
fina/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/fina/
│   │   │       ├── api/          # REST controllers
│   │   │       ├── service/      # Business logic, transaction handling
│   │   │       ├── repository/   # Redis data access
│   │   │       ├── model/        # Domain entities
│   │   │       ├── backup/       # Scheduled backup jobs
│   │   │       └── config/       # Redis, security, scheduling
│   │   └── resources/
│   │       └── application.yml
│   └── test/java/                # Unit and integration tests
├── docker-compose.yml            # Redis + service for local development
├── README.md
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
└── LICENSE
```

## Features

- Secure handling of accounts and financial transactions
- High-concurrency transaction processing via Redis
- Automated database backups on a schedule
- Analytics over transaction history (balances, spending, trends)
- REST API for clients and integrations

## Architecture Goals

| Concern | Recommended Practice |
| --- | --- |
| Concurrency & locks | Use Redisson's distributed locks or Redis Lua scripts to handle concurrent resource contention safely. |
| Data durability | Enable hybrid RDB + AOF persistence with `fsync everysec` for a strong balance between performance and durability. |
| High availability | Deploy Redis in a Sentinel or Cluster topology to ensure automated failover if the primary node goes down. |

### Persistence Options

| Option | How it works | Trade-off |
| --- | --- | --- |
| **RDB** (Redis Database Snapshots) | Point-in-time snapshots of the dataset stored as compact `.rdb` files at configured time or write intervals (e.g., save after *N* changes every *M* seconds). | Fast restores and compact files, but writes since the last snapshot can be lost. |
| **AOF** (Append-Only File) | Logs every write operation received by the server, synced to disk every second (`fsync everysec`) or on every write. | Maximum durability and a complete operation log, but larger files and slower replay. |
| **Hybrid** (RDB + AOF) | Combines fast point-in-time snapshot restores with complete operation logs. | Best balance of restore speed and durability at the cost of running both mechanisms. |

## UML Diagrams

The diagrams below describe the intended system. They are written in [Mermaid](https://mermaid.js.org) and render directly on GitHub.

### Use Case Diagram

```mermaid
flowchart LR
    Client((API Client))
    Analyst((Analyst))
    Admin((Administrator))
    Scheduler((Scheduler))

    subgraph Fina["fina"]
        UC1([Create account])
        UC2([Record transaction])
        UC3([View balance])
        UC4([Query transaction history])
        UC5([Generate analytics])
        UC6([Run automated backup])
        UC7([Restore from backup])
        UC8([Manage access])
    end

    Client --> UC1
    Client --> UC2
    Client --> UC3
    Client --> UC4

    Analyst --> UC4
    Analyst --> UC5

    Admin --> UC8
    Admin --> UC7

    Scheduler --> UC6
    UC6 -.->|"<<include>>"| UC1
```

### Architecture Diagram

```mermaid
flowchart TD
    subgraph Clients["Clients"]
        API["REST API Client"]
    end

    subgraph Service["Java Service"]
        Controllers["API Layer<br/>Controllers"]
        Services["Service Layer<br/>Transaction logic"]
        Repos["Repository Layer<br/>Redis access"]
        Backup["Backup Scheduler"]
        Config["Config<br/>Redis · Security · Scheduling"]
    end

    subgraph Data["Data"]
        Redis[("Redis<br/>high-concurrency store")]
        Backups[("Backup storage")]
    end

    API -->|HTTPS / JSON| Controllers
    Controllers --> Services
    Services --> Repos
    Repos -->|read / write| Redis
    Backup -->|snapshot| Redis
    Backup -->|write| Backups
    Config -.-> Controllers
    Config -.-> Services
    Config -.-> Backup
```

### Class Diagram

```mermaid
classDiagram
    class Account {
        +String id
        +String ownerName
        +String email
        +BigDecimal balance
        +AccountStatus status
        +deposit(amount)
        +withdraw(amount)
    }

    class Transaction {
        +String id
        +String accountId
        +BigDecimal amount
        +TransactionType type
        +Instant timestamp
        +TransactionStatus status
    }

    class Invoice {
        +String id
        +String accountId
        +String customerName
        +String customerEmail
        +BigDecimal amount
        +LocalDate issuedOn
        +LocalDate dueOn
        +InvoiceStatus status
    }

    class BackupJob {
        +String id
        +Instant startedAt
        +Instant completedAt
        +BackupStatus status
        +run()
    }

    class TransactionService {
        +record(Transaction) Transaction
        +history(accountId) List~Transaction~
        +balance(accountId) BigDecimal
    }

    class AccountStatus {
        <<enumeration>>
        active
        suspended
        closed
    }

    class TransactionType {
        <<enumeration>>
        deposit
        withdrawal
        transfer
    }

    class TransactionStatus {
        <<enumeration>>
        pending
        completed
        failed
    }

    class InvoiceStatus {
        <<enumeration>>
        pending
        paid
        overdue
    }

    class BackupStatus {
        <<enumeration>>
        running
        success
        failed
    }

    Account "1" --> "*" Transaction : has
    Account "1" --> "*" Invoice : issued
    TransactionService --> Transaction : manages
    TransactionService --> Account : updates
    BackupJob --> Account : snapshots
    Account --> AccountStatus
    Transaction --> TransactionType
    Transaction --> TransactionStatus
    Invoice --> InvoiceStatus
    BackupJob --> BackupStatus
```

### Sequence Diagram — Record a Transaction

```mermaid
sequenceDiagram
    actor Client
    participant API as API Controller
    participant Service as Transaction Service
    participant Repo as Account Repository
    participant Redis as Redis

    Client->>API: POST /transactions
    API->>Service: record(request)
    Service->>Service: Validate amount and account
    alt Invalid request
        Service-->>API: Validation error
        API-->>Client: 400 Bad Request
    else Valid request
        Service->>Repo: load(accountId)
        Repo->>Redis: GET account:{id}
        Redis-->>Repo: Account data
        Repo-->>Service: Account
        Service->>Service: Apply debit / credit
        Service->>Repo: save(account, transaction)
        Repo->>Redis: MULTI / EXEC
        Redis-->>Repo: OK
        Service-->>API: Transaction
        API-->>Client: 201 Created
    end
```

### Sequence Diagram — Automated Backup

```mermaid
sequenceDiagram
    participant Scheduler as Backup Scheduler
    participant Backup as Backup Service
    participant Redis as Redis
    participant Store as Backup Storage

    loop On configured interval
        Scheduler->>Backup: run()
        Backup->>Redis: BGSAVE / snapshot
        Redis-->>Backup: Snapshot ready
        Backup->>Store: Persist snapshot + metadata
        Store-->>Backup: Stored
        Backup->>Backup: Record backup status
    end
```

## Roadmap

- [ ] Scaffold the Java service and choose Maven or Gradle
- [ ] Configure Redis connection, pooling, and security
- [ ] Add Redisson distributed locks or Lua scripts for concurrent writes
- [ ] Enable hybrid RDB + AOF persistence with `appendfsync everysec`
- [ ] Deploy Redis with Sentinel or Cluster for automated failover
- [ ] Implement accounts, transactions, and invoices
- [ ] Add automated backup and restore workflow
- [ ] Add analytics endpoints (balances, spending, trends)
- [ ] Add authentication and role-based access control
- [ ] Add unit and integration tests
- [ ] Add Docker Compose for local development

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and our [Code of Conduct](CODE_OF_CONDUCT.md) before opening an issue or pull request.

## License

Released under the [MIT License](LICENSE). © 2026 Caleb Ngeno.
