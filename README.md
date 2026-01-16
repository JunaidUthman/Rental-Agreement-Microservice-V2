# Rental Agreement Microservice

<div align="center">

![Version](https://img.shields.io/badge/version-0.0.1--SNAPSHOT-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-green)
![License](https://img.shields.io/badge/license-MIT-brightgreen)

**A comprehensive microservice for managing rental agreements, contracts, and payments in a real estate rental platform**

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Key Features](#-key-features)
- [Technology Stack](#-technology-stack)
- [Core Components](#-core-components)
- [API Endpoints](#-api-endpoints)
- [Inter-Service Communication](#-inter-service-communication)
- [Database Schema](#-database-schema)
- [Configuration](#-configuration)
- [Getting Started](#-getting-started)
- [Deployment](#-deployment)
- [Security](#-security)

---

## 🎯 Overview

The **Rental Agreement Microservice** is a central component of a distributed real estate rental platform. It orchestrates the complete rental lifecycle from initial tenant requests to contract management, payment tracking, and dispute resolution. This microservice integrates seamlessly with other platform services including Property Management, Tenant Scoring AI, and Notification systems.

### Business Context

This microservice operates within a real estate ecosystem where:
- **Tenants** browse properties and submit rental requests
- **Landlords** review applicants and approve/reject requests
- **Smart Contracts** handle secure blockchain-based payments
- **AI Models** assess tenant risk and credibility
- **Notifications** keep all parties informed throughout the process

---

## 🏗 Architecture

### High-Level System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        WEB[Web Application]
        MOBILE[Mobile App]
    end

    subgraph "API Gateway"
        GATEWAY[API Gateway<br/>Port: 8080]
    end

    subgraph "RentalAgreement Microservice<br/>Port: 8083"
        CTRL[Controllers Layer]
        SVC[Services Layer]
        REPO[Repository Layer]
        
        subgraph "Core Components"
            RR[Rental Request<br/>Management]
            RC[Rental Contract<br/>Management]
            PAY[Payment<br/>Tracking]
            DISP[Dispute<br/>Management]
        end
    end

    subgraph "External Services"
        PROP[Property<br/>Microservice]
        AI[Tenant Scoring<br/>AI Model]
        KAFKA[Kafka<br/>Notification System]
        CONFIG[Config Server<br/>Port: 8888]
    end

    subgraph "Data Layer"
        DB[(MySQL Database<br/>rental_agreement_db)]
    end

    WEB --> GATEWAY
    MOBILE --> GATEWAY
    GATEWAY --> CTRL
    
    CTRL --> SVC
    SVC --> REPO
    SVC --> RR
    SVC --> RC
    SVC --> PAY
    SVC --> DISP
    
    REPO --> DB
    
    SVC -.->|OpenFeign| PROP
    SVC -.->|OpenFeign| AI
    SVC -.->|Kafka Producer| KAFKA
    
    CTRL -.->|Spring Cloud Config| CONFIG

    style CTRL fill:#4A90E2
    style SVC fill:#50C878
    style REPO fill:#F39C12
    style DB fill:#E74C3C
    style PROP fill:#9B59B6
    style AI fill:#1ABC9C
    style KAFKA fill:#E67E22
```

### Rental Lifecycle Flow

```mermaid
sequenceDiagram
    participant T as Tenant
    participant RA as RentalAgreement<br/>Microservice
    participant P as Property<br/>Microservice
    participant AI as Tenant Scoring<br/>AI Model
    participant K as Kafka<br/>Notifications
    participant L as Landlord
    participant BC as Blockchain<br/>Smart Contract

    Note over T,BC: Step 1: Rental Request Submission
    T->>RA: POST /rental-requests<br/>(tenantId, propertyId)
    RA->>P: GET /properties/{id}<br/>Check availability
    P-->>RA: Property available
    RA->>AI: POST /predict/score<br/>(tenant data)
    AI-->>RA: Tenant risk score
    RA->>RA: Create RentalRequest<br/>Status: PENDING
    RA->>K: Send notification event
    K-->>L: Notify landlord of new request
    RA-->>T: 201 Created

    Note over T,BC: Step 2: Landlord Review & Approval
    L->>RA: GET /rental-requests/property/{id}
    RA-->>L: List of pending requests
    L->>RA: PUT /rental-requests/{id}/status<br/>Status: ACCEPTED
    RA->>RA: Reject other pending requests
    RA->>P: Update property availability<br/>(mark as unavailable)
    RA->>K: Send acceptance notification
    K-->>T: Notify tenant of acceptance
    RA-->>L: 200 OK

    Note over T,BC: Step 3: Contract Creation & Payment
    T->>RA: POST /rental-contracts<br/>(contract terms)
    RA->>RA: Create RentalContract<br/>State: PENDING_RESERVATION
    RA->>BC: Initiate escrow payment
    BC-->>RA: Payment confirmed (txHash)
    RA->>RA: Record payment in database
    RA->>K: Send contract created notification
    RA-->>T: 201 Created

    Note over T,BC: Step 4: Key Delivery & Activation
    L->>T: Physical key delivery
    T->>RA: PUT /rental-contracts/{id}/key-delivery<br/>isKeyDelivered: true
    RA->>RA: Update contract state<br/>State: ACTIVE
    RA->>BC: Activate agreement on blockchain
    RA->>K: Send activation notification
    RA-->>T: 200 OK

    Note over T,BC: Step 5: Ongoing Rent Payments
    loop Monthly Payments
        T->>BC: Submit rent payment
        BC->>RA: Blockchain event listener<br/>(RentPaid event)
        RA->>RA: Record payment in history
        RA->>K: Send payment confirmation
    end

    Note over T,BC: Step 6: Dispute Resolution (if needed)
    alt Dispute Occurs
        T->>RA: POST /disputes<br/>(tenantId, reason)
        RA->>RA: Track dispute summary
        RA->>RA: Update contract state<br/>State: TERMINATED
        RA->>K: Send dispute notification
        RA-->>T: 200 OK
    end
```

---

## ✨ Key Features

### 🔹 Rental Request Management
- **Tenant Request Submission**: Tenants can submit rental requests for available properties
- **AI-Powered Screening**: Integration with Tenant Scoring AI to assess applicant risk
- **Landlord Review**: Landlords can view, approve, or reject rental requests
- **Automatic Status Management**: System automatically rejects competing requests when one is accepted
- **Property Availability Sync**: Real-time synchronization with Property Microservice

### 🔹 Rental Contract Management
- **Smart Contract Integration**: Blockchain-based contract creation with escrow payments
- **Multi-State Lifecycle**: Tracks contracts through PENDING_RESERVATION, ACTIVE, EXPIRED, and TERMINATED states
- **Key Delivery Confirmation**: Tenant confirms physical key receipt to activate the contract
- **Flexible Rental Terms**: Supports DAILY and MONTHLY rental periods
- **Automatic Calculation**: Computes total rental amounts based on dates and rental type

### 🔹 Payment Tracking
- **Blockchain Integration**: Records all payments from Ethereum smart contract events
- **Transaction Verification**: Each payment includes unique transaction hash (txHash) for audit trail
- **Payment History**: Complete payment history per rental contract
- **Status Tracking**: Monitors payment status (COMPLETED, PENDING, FAILED)
- **Escrow Management**: Handles initial security deposits and ongoing rent payments

### 🔹 Dispute Management
- **Dispute Tracking**: Records and tracks all tenant-landlord disputes
- **Historical Data**: Maintains dispute summaries per tenant for AI model training
- **Contract Termination**: Can terminate contracts based on dispute outcomes
- **Metrics Collection**: Provides dispute data for risk assessment and platform improvements

### 🔹 Event-Driven Notifications
- **Kafka Integration**: Publishes events to notification system for all major lifecycle events
- **Multi-Channel Support**: Supports EMAIL, SMS, and IN_APP notification channels
- **Event Types**: REQUEST_SUBMITTED, REQUEST_ACCEPTED, CONTRACT_CREATED, PAYMENT_RECEIVED, DISPUTE_FILED, etc.
- **Real-time Updates**: Keeps tenants and landlords informed throughout the process

---

## 🛠 Technology Stack

### Core Framework
- **Java 17**: Modern LTS version with latest language features
- **Spring Boot 3.4.1**: Latest Spring Boot framework for microservices
- **Spring Cloud 2024.0.0**: Cloud-native patterns and distributed system support

### Data & Persistence
- **Spring Data JPA**: Simplified data access with JPA/Hibernate
- **MySQL**: Production-ready relational database
- **H2 Database**: In-memory database for development and testing
- **Flyway/Liquibase**: Database version control (configured via external config)

### Communication & Integration
- **Spring Cloud OpenFeign**: Declarative REST client for inter-service communication
- **Apache Kafka**: Event streaming platform for asynchronous messaging
- **Spring Cloud Config**: Centralized configuration management

### Security
- **Spring Security**: Comprehensive security framework
- **JWT (jjwt 0.12.5)**: JSON Web Token for stateless authentication
- **Role-Based Access Control**: Fine-grained authorization (TENANT, LANDLORD, ADMIN)

### Development Tools
- **Lombok 1.18.30**: Reduces boilerplate code with annotations
- **MapStruct 1.5.5**: Type-safe bean mapping
- **Spring Boot DevTools**: Hot reload and enhanced development experience
- **Spring Boot Actuator**: Production-ready monitoring and management

### DevOps & Deployment
- **Docker**: Containerization with Alpine-based JRE image
- **Jenkins**: CI/CD pipeline automation (Jenkinsfile included)
- **Maven**: Dependency management and build automation

---

## 🧩 Core Components

### 📂 Package Structure

```
com.lsiproject.app.rentalagreementmicroservicev2
├── configuration/          # Application configuration classes
│   ├── FeignConfig.java   # OpenFeign client configuration
│   └── KafkaProducerConfig.java  # Kafka producer setup
│
├── controllers/            # REST API endpoints
│   ├── RentalRequestController.java
│   ├── RentalContractController.java
│   ├── PaymentController.java
│   ├── DisputeSummaryController.java
│   ├── ReportController.java
│   └── AiModelsController.java
│
├── services/               # Business logic layer
│   ├── RentalRequestService.java
│   ├── RentalContractService.java
│   ├── PaymentService.java
│   ├── PaymentReportService.java
│   ├── DisputeSummaryService.java
│   ├── NotificationService.java
│   └── TenantScoringAiModelService.java
│
├── repositories/           # Data access layer
│   ├── RentalRequestRepository.java
│   ├── RentalContractRepository.java
│   ├── PaymentRepository.java
│   ├── PaymentReportRepository.java
│   └── DisputeSummaryRepository.java
│
├── entities/               # JPA entities
│   ├── RentalRequest.java
│   ├── RentalContract.java
│   ├── Payment.java
│   ├── PaymentReport.java
│   └── DisputeSummary.java
│
├── dtos/                   # Data Transfer Objects
│   ├── RentalRequestDto.java
│   ├── RentalRequestCreationDto.java
│   ├── RentalContractDto.java
│   ├── PaymentDto.java
│   ├── NotificationEvent.java
│   └── ...
│
├── mappers/                # MapStruct mappers
│   ├── RentalRequestMapper.java
│   ├── RentalContractMapper.java
│   └── PaymentMapper.java
│
├── openFeignClients/       # External service clients
│   ├── PropertyMicroService.java
│   └── TenantScoringAiModel.java
│
├── kafka/                  # Kafka producers/consumers
│   └── NotificationProducer.java
│
├── security/               # Security components
│   ├── JwtAuthFilter.java
│   └── JwtUtil.java
│
├── enums/                  # Enumeration types
│   ├── RentalRequestStatus.java
│   ├── RentalContractState.java
│   ├── PaymentStatus.java
│   ├── EventType.java
│   └── Channel.java
│
└── exceptions/             # Exception handling
    └── GlobalExceptionHandler.java
```

### 🗄️ Core Entities

#### RentalRequest
Represents a tenant's initial request to rent a property.

**Key Fields:**
- `idRequest`: Unique identifier
- `tenantId`: ID of the requesting tenant (from Auth microservice)
- `propertyId`: ID of the desired property (from Property microservice)
- `status`: PENDING, ACCEPTED, REJECTED, EXPIRED
- `createdAt`: Timestamp of request creation

#### RentalContract
Represents the formal rental agreement between landlord and tenant.

**Key Fields:**
- `idContract`: Unique identifier
- `agreementIdOnChain`: Corresponding blockchain contract ID
- `ownerId`: Property owner ID
- `tenantId`: Tenant ID
- `propertyId`: Property ID
- `rentAmount`: Monthly/daily rent amount
- `securityDeposit`: Initial deposit amount
- `startDate` / `endDate`: Contract duration
- `state`: PENDING_RESERVATION, ACTIVE, EXPIRED, TERMINATED
- `isKeyDelivered`: Key receipt confirmation
- `isPaymentReleased`: Initial payment release status

#### Payment
Tracks all rental payments from blockchain transactions.

**Key Fields:**
- `idPayment`: Unique identifier
- `rentalContract`: Associated contract (ManyToOne relationship)
- `amount`: Payment amount
- `txHash`: Ethereum transaction hash (unique)
- `status`: COMPLETED, PENDING, FAILED
- `timestamp`: Blockchain block timestamp
- `tenantId`: Payer ID

#### DisputeSummary
Records dispute history for tenant risk assessment.

**Key Fields:**
- `id`: Unique identifier
- `tenantId`: Tenant involved in dispute
- `totalDisputes`: Count of disputes
- `lastDisputeDate`: Most recent dispute timestamp

---

## 🌐 API Endpoints

### Rental Requests

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/rentalAgreement-microservice/rental-requests` | Create new rental request | ✅ |
| `GET` | `/api/rentalAgreement-microservice/rental-requests` | Get all requests | ✅ (Admin) |
| `GET` | `/api/rentalAgreement-microservice/rental-requests/{id}` | Get request by ID | ✅ (Admin) |
| `GET` | `/api/rentalAgreement-microservice/rental-requests/property/{propertyId}` | Get requests for property | ✅ (Landlord) |
| `GET` | `/api/rentalAgreement-microservice/rental-requests/tenant/{tenantId}` | Get requests by tenant | ✅ |
| `PUT` | `/api/rentalAgreement-microservice/rental-requests/{id}/status` | Update request status | ✅ (Landlord) |
| `DELETE` | `/api/rentalAgreement-microservice/rental-requests/{id}` | Delete request | ✅ |

### Rental Contracts

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/rentalAgreement-microservice/rental-contracts` | Create new contract | ✅ (Tenant) |
| `GET` | `/api/rentalAgreement-microservice/rental-contracts/{id}` | Get contract by ID | ✅ |
| `GET` | `/api/rentalAgreement-microservice/rental-contracts/user/me` | Get user's contracts | ✅ |
| `PUT` | `/api/rentalAgreement-microservice/rental-contracts/{id}/key-delivery` | Confirm key delivery | ✅ (Tenant) |
| `PUT` | `/api/rentalAgreement-microservice/rental-contracts/{id}/dispute` | Terminate by dispute | ✅ |

### Payments

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/rentalAgreement-microservice/payments` | Record payment (blockchain listener) | ⚠️ Internal |
| `GET` | `/api/rentalAgreement-microservice/payments/{id}` | Get payment by ID | ✅ |
| `GET` | `/api/rentalAgreement-microservice/payments/contract/{contractId}` | Get payment history | ✅ |

### Disputes

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/disputes` | Create dispute record | ✅ |
| `GET` | `/api/disputes` | Get all disputes | ✅ (Admin/AI) |

---

## 🔗 Inter-Service Communication

### Integration with Property Microservice

```mermaid
graph LR
    RA[RentalAgreement<br/>Microservice] -->|OpenFeign| PM[Property<br/>Microservice]
    
    subgraph "Property Service Calls"
        PM -->|GET /properties/{id}| P1[Get Property Details]
        PM -->|GET /properties/{id}/isAvailable| P2[Check Availability]
        PM -->|GET /properties/{id}/availability| P3[Update Availability]
        PM -->|GET /properties/{id}/TypeOfRental| P4[Get Rental Type]
    end

    style RA fill:#4A90E2
    style PM fill:#9B59B6
```

**Purpose**: Validates property availability and synchronizes rental status.

**OpenFeign Client**: `PropertyMicroService.java`

**Configuration**: URL configured via `property.service.url` property

### Integration with Tenant Scoring AI Model

```mermaid
graph LR
    RA[RentalAgreement<br/>Microservice] -->|OpenFeign| AI[Tenant Scoring<br/>AI Model]
    
    AI -->|POST /predict/score| SCORE[Calculate Risk Score]
    
    subgraph "AI Request Payload"
        SCORE -->|TenantScoreRequest| TR[Tenant Data:<br/>- Income<br/>- Credit History<br/>- Past Disputes]
    end
    
    subgraph "AI Response"
        SCORE -->|TenantScoringDTO| TS[Risk Assessment:<br/>- Score<br/>- Risk Level<br/>- Recommendations]
    end

    style RA fill:#4A90E2
    style AI fill:#1ABC9C
```

**Purpose**: Assesses tenant credibility using machine learning models.

**OpenFeign Client**: `TenantScoringAiModel.java`

**Configuration**: URL configured via `tenantScoringAiModel.service.url` property

### Kafka Event Publishing

```mermaid
graph TB
    RA[RentalAgreement<br/>Microservice] -->|Kafka Producer| KAFKA[Kafka Broker]
    
    KAFKA --> TOPIC1[rental-notifications]
    
    TOPIC1 --> NS[Notification<br/>Service]
    
    subgraph "Event Types"
        E1[REQUEST_SUBMITTED]
        E2[REQUEST_ACCEPTED]
        E3[CONTRACT_CREATED]
        E4[PAYMENT_RECEIVED]
        E5[DISPUTE_FILED]
    end
    
    subgraph "Notification Channels"
        NS --> EMAIL[Email]
        NS --> SMS[SMS]
        NS --> APP[In-App]
    end

    style RA fill:#4A90E2
    style KAFKA fill:#E67E22
    style NS fill:#F39C12
```

**Purpose**: Asynchronous event-driven notifications to users.

**Producer**: `NotificationProducer.java`

**Event DTO**: `NotificationEvent.java` (serialized to byte array)

---

## 💾 Database Schema

```mermaid
erDiagram
    RENTAL_REQUESTS ||--o{ RENTAL_CONTRACTS : "accepted_request_creates"
    RENTAL_CONTRACTS ||--o{ PAYMENTS : "has_many"
    RENTAL_REQUESTS {
        bigint idRequest PK
        timestamp createdAt
        varchar status
        bigint tenantId FK
        bigint propertyId FK
    }
    
    RENTAL_CONTRACTS {
        bigint idContract PK
        bigint agreementIdOnChain UK
        bigint ownerId FK
        bigint tenantId FK
        bigint propertyId FK
        double securityDeposit
        double rentAmount
        date startDate
        date endDate
        double TotalAmountToPay
        double PayedAmount
        boolean isKeyDelivered
        boolean isPaymentReleased
        varchar state
        timestamp createdAt
    }
    
    PAYMENTS {
        bigint idPayment PK
        bigint rentalContractId FK
        double amount
        varchar txHash UK
        varchar status
        timestamp timestamp
        bigint tenantId FK
    }
    
    DISPUTE_SUMMARIES {
        bigint id PK
        bigint tenantId UK
        int totalDisputes
        timestamp lastDisputeDate
    }
    
    PAYMENT_REPORTS {
        bigint id PK
        bigint contractId FK
        text reportData
        timestamp generatedAt
    }
```

### Database Configuration

**Production**: MySQL Database
- Database name: `rental_agreement_db` (configured via Spring Cloud Config)
- Character set: UTF-8
- Timezone: UTC

**Development**: H2 In-Memory Database
- Console: `/h2-console`
- Auto-initialization with schema

---

## ⚙️ Configuration

### Application Configuration (`application.yml`)

```yaml
server:
  port: 8083

spring:
  profiles:
    active: dev
  application:
    name: RentalAgreement-microservice
  config:
    import: "optional:configserver:http://localhost:8888"

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env,refresh
  security:
    enabled: false
```

### External Configuration (via Config Server)

The following properties are managed by **Spring Cloud Config Server** (port 8888):

- **Database Configuration**: JDBC URL, credentials, JPA settings
- **Kafka Configuration**: Bootstrap servers, topics, serializers
- **Feign Client URLs**: Property service and AI model endpoints
- **JWT Secret**: Token signing key
- **Logging Levels**: Application-specific log configurations

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **MySQL 8.0+** (for production)
- **Docker** (optional, for containerized deployment)
- **Kafka** (if running locally)
- **Config Server** (running on port 8888)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd RentalAgreement-microserviceV2
   ```

2. **Start Config Server**
   Ensure your Spring Cloud Config Server is running on `http://localhost:8888`

3. **Configure external services**
   Update your Config Server with:
   ```yaml
   property:
     service:
       url: http://localhost:8082  # Property Microservice
   
   tenantScoringAiModel:
     service:
       url: http://localhost:5000  # AI Model Service
   
   spring:
     kafka:
       bootstrap-servers: localhost:9092
   ```

4. **Run with Maven**
   ```bash
   ./mvnw spring-boot:run
   ```

5. **Access the application**
   - API Base URL: `http://localhost:8083`
   - Actuator Health: `http://localhost:8083/actuator/health`
   - H2 Console (dev): `http://localhost:8083/h2-console`

### Building for Production

```bash
./mvnw clean package -DskipTests
```

The JAR file will be created in `target/RentalAgreement-microservice-0.0.1-SNAPSHOT.jar`

---

## 🐳 Deployment

### Docker Deployment

**Build Docker Image**
```bash
docker build -t rentalagreement-microservice:latest .
```

**Run Container**
```bash
docker run -p 8083:8083 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_CLOUD_CONFIG_URI=http://config-server:8888 \
  rentalagreement-microservice:latest
```

### Docker Compose Example

```yaml
version: '3.8'
services:
  rentalagreement:
    image: rentalagreement-microservice:latest
    ports:
      - "8083:8083"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_CLOUD_CONFIG_URI=http://config-server:8888
    depends_on:
      - mysql
      - kafka
      - config-server
    networks:
      - rental-network

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: rental_agreement_db
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3306:3306"
    networks:
      - rental-network

networks:
  rental-network:
    driver: bridge
```

### Jenkins CI/CD

The repository includes a `Jenkinsfile` for automated build and deployment pipelines.

---

## 🔐 Security

### Authentication & Authorization

- **JWT-based Authentication**: Stateless token-based security
- **Spring Security**: Comprehensive security framework
- **Role-Based Access Control**:
  - `ROLE_TENANT`: Can create requests, view own contracts
  - `ROLE_LANDLORD`: Can approve/reject requests, manage properties
  - `ROLE_ADMIN`: Full system access

### Security Components

- **JwtAuthFilter**: Intercepts requests and validates JWT tokens
- **JwtUtil**: Generates and validates JWT tokens
- **UserPrincipal**: Custom authentication principal with user details

### Secure Endpoints

Most endpoints require authentication via:
```
Authorization: Bearer <JWT_TOKEN>
```

---

## 📊 Monitoring & Observability

### Spring Boot Actuator Endpoints

Available at `http://localhost:8083/actuator/`:

- `/health` - Application health status
- `/info` - Application information
- `/metrics` - Application metrics
- `/prometheus` - Prometheus-formatted metrics
- `/env` - Environment properties
- `/refresh` - Refresh configuration from Config Server

---

## 🤝 Contributing

### Development Workflow

1. Create feature branch from `main`
2. Implement changes with tests
3. Ensure code style compliance (Lombok, MapStruct)
4. Submit pull request with detailed description
5. Pass CI/CD pipeline checks

### Code Style

- **Lombok**: Use annotations to reduce boilerplate
- **MapStruct**: Type-safe bean mappings
- **JavaDoc**: Document public APIs
- **Clean Code**: Follow SOLID principles

---

## 📝 License

This project is licensed under the MIT License.

---

## 👨‍💻 Maintainer

**Yassine Kamouss**
- GitHub: [@JunaidUthman](https://github.com/JunaidUthman)

---

## 🆘 Support

For issues and questions:
- Open an issue on GitHub
- Contact the development team
- Refer to the architecture documentation

---

<div align="center">

**Built with ❤️ for modern real estate rental platforms**

</div>
