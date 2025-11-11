# SSH 기반 원격 명령어 실행 시스템 (POC)

Spring Boot 기반의 SSH를 통한 원격 Windows 클라이언트 명령어 실행 시스템입니다.

## 📋 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [주요 기능](#주요-기능)
3. [아키텍처](#아키텍처)
4. [기술 스택](#기술-스택)
5. [시스템 요구사항](#시스템-요구사항)
6. [설치 및 실행](#설치-및-실행)
7. [Windows 클라이언트 설정](#windows-클라이언트-설정)
8. [API 사용 가이드](#api-사용-가이드)
9. [설정 파일 상세](#설정-파일-상세)
10. [보안 고려사항](#보안-고려사항)
11. [트러블슈팅](#트러블슈팅)

---

## 프로젝트 개요

이 프로젝트는 중앙 서버에서 여러 Windows 클라이언트에 SSH를 통해 명령어를 전송하고 실행 결과를 수집하는 시스템입니다.

### 주요 사용 사례

- 여러 대의 Windows PC에 일괄 명령어 실행
- 원격 시스템 상태 모니터링
- 자동화된 관리 작업 수행
- 명령어 실행 이력 추적 및 감사

### 특징

✅ **완전한 캡슐화**: 모든 도메인 로직은 내부에서만 상태 변경  
✅ **인터페이스 기반**: 구현체 교체 가능한 유연한 설계  
✅ **비동기 처리**: 여러 명령어 동시 실행 가능  
✅ **보안 강화**: 패스워드 암호화, API Key 인증, 명령어 검증  
✅ **확장 가능**: 멀티 클라이언트, 다양한 실행 방식 지원 준비  

---

## 주요 기능

### 1. SSH 명령어 실행

- **sshj 라이브러리** 기반 SSH 연결
- **비동기 실행**: Spring `@Async`를 통한 동시 처리
- **재시도 로직**: 연결 실패 시 최대 3회 재시도 (Exponential Backoff)
- **타임아웃 제어**: 기본 60초 (설정 가능)

### 2. 보안 기능

- **패스워드 암호화**: Jasypt (AES-256) 사용
- **API Key 인증**: Stateless 인증 방식
  - Admin Key: 전체 관리 권한
  - Client Key: 클라이언트별 개별 키
- **명령어 검증**: 화이트리스트/블랙리스트 정규식 패턴
- **런타임 리로드**: 정책 파일 변경 시 자동 반영 (5초 주기)

### 3. 명령어 관리

- **실행 상태 추적**: PENDING → EXECUTING → SUCCESS/FAILED/TIMEOUT
- **이력 관리**: PostgreSQL에 모든 실행 기록 저장
- **페이징 조회**: 대량 이력 효율적 조회

### 4. 클라이언트 관리

- **자가 등록**: 클라이언트가 스스로 등록 가능
- **관리자 등록**: Admin API를 통한 중앙 관리
- **API Key 재발급**: 보안 사고 시 즉시 재발급
- **활성화/비활성화**: 클라이언트 접근 제어

---

## 아키텍처

### 시스템 구성도

```
┌─────────────────────────────────────────────────────────────┐
│                     Spring Boot Server                      │
├─────────────────────────────────────────────────────────────┤
│  Presentation Layer (REST API)                              │
│  ├─ AdminClientController (관리자 API)                      │
│  ├─ ClientController (클라이언트 자가 등록)                  │
│  └─ CommandController (명령어 실행 및 조회)                  │
├─────────────────────────────────────────────────────────────┤
│  Application Layer (비즈니스 로직)                           │
│  ├─ CommandExecutor (명령어 실행기)                         │
│  ├─ CommandValidator (명령어 검증)                          │
│  ├─ SshConnectionManager (SSH 연결 관리)                    │
│  └─ CommandPolicyLoader (정책 로더)                         │
├─────────────────────────────────────────────────────────────┤
│  Domain Layer (도메인 모델)                                 │
│  ├─ Command (명령어 엔티티)                                 │
│  ├─ ClientCredential (클라이언트 인증 정보)                 │
│  ├─ CommandService (명령어 도메인 서비스)                   │
│  └─ ClientService (클라이언트 도메인 서비스)                │
├─────────────────────────────────────────────────────────────┤
│  Infrastructure Layer (기술 구현)                           │
│  ├─ Security (API Key 인증)                                │
│  ├─ Async (비동기 설정)                                     │
│  ├─ PasswordEncryptor (암호화)                             │
│  └─ JPA Repositories                                       │
└─────────────────────────────────────────────────────────────┘
                           ↓ SSH (Port 22)
┌─────────────────────────────────────────────────────────────┐
│               Windows Client (OpenSSH Server)               │
│  ├─ OpenSSH Server 실행                                     │
│  ├─ 방화벽 설정 (Port 22 허용)                              │
│  └─ 사용자 계정 설정                                         │
└─────────────────────────────────────────────────────────────┘
```


### 디렉토리 구조

```
src/main/java/kr/urock/sample_remote_command_proj/
├── domain/                           # 도메인 계층
│   ├── command/                      # 명령어 도메인
│   │   ├── Command.java              # 명령어 엔티티 (캡슐화된 상태 관리)
│   │   ├── CommandStatus.java        # 상태 열거형
│   │   ├── CommandRepository.java    # 데이터 액세스 인터페이스
│   │   └── CommandService.java       # 도메인 서비스 (오케스트레이션)
│   └── client/                       # 클라이언트 도메인
│       ├── ClientCredential.java     # 클라이언트 엔티티
│       ├── ClientCredentialRepository.java
│       └── ClientService.java
│
├── application/                      # 애플리케이션 계층
│   ├── executor/                     # 명령어 실행
│   │   ├── CommandExecutor.java     # 실행기 인터페이스
│   │   ├── SshCommandExecutor.java  # SSH 실행 구현체
│   │   └── dto/                      # 실행 관련 DTO (불변 객체)
│   ├── validator/                    # 명령어 검증
│   │   ├── CommandValidator.java    # 검증기 인터페이스
│   │   ├── FileBasedCommandValidator.java
│   │   ├── CommandPolicyLoader.java # 정책 로더 인터페이스
│   │   └── FileBasedPolicyLoader.java
│   ├── ssh/                          # SSH 연결 관리
│   │   ├── SshConnectionManager.java # SSH 인터페이스
│   │   ├── SshjConnectionManager.java # sshj 구현체
│   │   └── dto/
│   └── cache/                        # 캐시 (추후 확장)
│       ├── CommandResultCache.java  # 캐시 인터페이스
│       └── NoOpCommandResultCache.java # POC용 NoOp 구현
│
├── infrastructure/                   # 인프라 계층
│   ├── config/                       # 설정
│   │   ├── AsyncConfig.java         # 비동기 Thread Pool 설정
│   │   ├── SecurityConfig.java      # Spring Security 설정
│   │   └── JpaConfig.java           # JPA 설정
│   ├── security/
│   │   └── ApiKeyAuthFilter.java   # API Key 인증 필터
│   └── util/
│       └── PasswordEncryptor.java   # Jasypt 암호화 유틸
│
└── presentation/                     # 프레젠테이션 계층
    └── api/                          # REST API
        ├── AdminClientController.java     # 클라이언트 관리 API
        ├── ClientController.java          # 자가 등록 API
        ├── CommandController.java         # 명령어 실행 API
        ├── GlobalExceptionHandler.java    # 통합 예외 처리
        └── dto/                            # API DTO (요청/응답)
```

---

## 기술 스택

### Backend Framework
- **Spring Boot** 3.2.5
- **Java** 21
- **Spring Security** (API Key 기반 인증)
- **Spring Data JPA** (데이터 액세스 추상화)
- **Spring Async** (비동기 처리)
- **Spring Scheduling** (정기 작업)

### Libraries
- **sshj** 0.38.0 - SSH 연결 및 명령어 실행
- **Jasypt** 3.0.5 - AES-256 암호화
- **Lombok** - 보일러플레이트 코드 제거
- **PostgreSQL Driver** - JDBC 드라이버

### Database
- **PostgreSQL** 16-alpine (Docker)

### Build & DevOps
- **Maven** 3.x - 빌드 도구
- **Docker Compose** - PostgreSQL 컨테이너 관리

---

## 시스템 요구사항

### 서버 (Spring Boot 애플리케이션)

| 항목 | 요구사항 |
|------|----------|
| **Java** | 21 이상 |
| **Maven** | 3.6 이상 |
| **Docker** | PostgreSQL 실행용 (Docker Compose) |
| **메모리** | 최소 1GB RAM (권장 2GB) |
| **디스크** | 최소 500MB (로그 및 DB 포함 1GB 권장) |
| **네트워크** | 클라이언트와 SSH 통신 가능 (Port 22) |

### Windows 클라이언트

| 항목 | 요구사항 |
|------|----------|
| **OS** | Windows 10 1809 이상 또는 Windows Server 2019 이상 |
| **OpenSSH Server** | 설치 및 실행 필요 |
| **방화벽** | SSH 포트(22) 인바운드 허용 |
| **네트워크** | 서버에서 접근 가능한 고정 IP 또는 DNS |
| **권한** | SSH 접속용 로컬 관리자 계정 |

---

## 설치 및 실행

### 1. 프로젝트 클론

```bash
git clone https://github.com/srang03/sample_remote_command_proj.git
cd sample_remote_command_proj
```

### 2. PostgreSQL 시작

```bash
# Docker Compose로 PostgreSQL 시작
docker-compose up -d

# 상태 확인
docker ps | grep remote-command-db
```

**예상 출력:**
```
CONTAINER ID   IMAGE                  STATUS         PORTS                    NAMES
abc123def456   postgres:16-alpine     Up 10 seconds  0.0.0.0:5432->5432/tcp   remote-command-db
```

### 3. 환경 변수 설정 (필수)

**보안을 위해 중요한 키값은 반드시 환경 변수로 관리하세요:**

```bash
# Linux/Mac
export ADMIN_API_KEY="your-secure-admin-key-min-32-chars"
export ENCRYPTION_KEY="your-secure-encryption-key-min-32-chars"
export DB_PASSWORD="your-db-password"

# Windows PowerShell
$env:ADMIN_API_KEY="your-secure-admin-key-min-32-chars"
$env:ENCRYPTION_KEY="your-secure-encryption-key-min-32-chars"
$env:DB_PASSWORD="your-db-password"
```

또는 `.env` 파일 생성 (Git에 커밋하지 마세요!):

```env
# .env
ADMIN_API_KEY=my-super-secret-admin-key-2025
ENCRYPTION_KEY=32-char-minimum-encryption-secret-key-here
DB_PASSWORD=secure-db-password-123
```

### 4. 애플리케이션 빌드

```bash
# Maven Wrapper 사용 (권장)
./mvnw clean package -DskipTests

# 또는 시스템 Maven 사용
mvn clean package -DskipTests
```

### 5. 애플리케이션 실행

**방법 A: Maven으로 직접 실행**
```bash
./mvnw spring-boot:run
```

**방법 B: JAR 파일로 실행**
```bash
java -jar target/sample_remote_command_proj-0.0.1-SNAPSHOT.jar
```

### 6. 애플리케이션 확인

```bash
# 헬스 체크
curl http://localhost:8080/actuator/health
```

**정상 응답:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

---

## Windows 클라이언트 설정

### 1단계: OpenSSH Server 설치

#### PowerShell (관리자 권한)에서 실행:

```powershell
# 1. OpenSSH Server 기능 추가
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0

# 2. 설치 확인
Get-WindowsCapability -Online | Where-Object Name -like 'OpenSSH.Server*'
```

**예상 출력:**
```
Name  : OpenSSH.Server~~~~0.0.1.0
State : Installed
```

### 2단계: OpenSSH Server 시작 및 자동 시작 설정

```powershell
# 서비스 시작
Start-Service sshd

# 자동 시작 설정 (재부팅 시에도 자동 실행)
Set-Service -Name sshd -StartupType 'Automatic'

# 상태 확인
Get-Service sshd
```

**예상 출력:**
```
Status   Name               DisplayName
------   ----               -----------
Running  sshd               OpenSSH SSH Server
```

### 3단계: 방화벽 규칙 추가

```powershell
# SSH 포트(22) 인바운드 허용
New-NetFirewallRule -Name sshd -DisplayName 'OpenSSH Server (sshd)' `
    -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22

# 규칙 확인
Get-NetFirewallRule -Name sshd | Format-List Name,Enabled,Direction,Action
```

### 4단계: 사용자 계정 설정

**옵션 A: 기존 사용자 사용**

현재 로그인한 사용자를 SSH로 접속 가능하게 설정 (이미 설정되어 있음)

**옵션 B: 새 사용자 생성 (권장)**

```powershell
# 새 로컬 사용자 생성
$Password = ConvertTo-SecureString "SecureP@ssw0rd123!" -AsPlainText -Force
New-LocalUser -Name "remote_admin" `
    -Password $Password `
    -FullName "Remote Admin User" `
    -Description "SSH 원격 접속용 계정"

# Administrators 그룹에 추가
Add-LocalGroupMember -Group "Administrators" -Member "remote_admin"

# 계정 확인
Get-LocalUser remote_admin
```

### 5단계: SSH 접속 테스트

**다른 PC에서 테스트:**

```bash
# Linux/Mac
ssh remote_admin@192.168.1.100

# Windows (PowerShell)
ssh remote_admin@192.168.1.100
```

**성공 시 출력:**
```
remote_admin@192.168.1.100's password:
Microsoft Windows [Version 10.0.19045.xxxx]
(c) Microsoft Corporation. All rights reserved.

remote_admin@PC-NAME C:\Users\remote_admin>
```

### 6단계: PowerShell을 기본 셸로 설정 (선택사항)

기본적으로 SSH는 cmd.exe를 사용합니다. PowerShell을 기본으로 사용하려면:

```powershell
# 레지스트리에 PowerShell 경로 등록
New-ItemProperty -Path "HKLM:\SOFTWARE\OpenSSH" -Name DefaultShell `
    -Value "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" `
    -PropertyType String -Force
```

### 7단계: SSH 서비스 재시작

```powershell
Restart-Service sshd
```


---

## API 사용 가이드

### API 인증 방식

이 시스템은 HTTP 헤더 기반 API Key 인증을 사용합니다:

| 헤더 이름 | 용도 | 권한 |
|-----------|------|------|
| `X-Admin-Key` | 관리자 API 키 | 전체 관리 권한 |
| `X-API-Key` | 클라이언트 API 키 | 클라이언트별 개별 권한 |

### 1. 클라이언트 등록

#### 방법 A: Admin API로 등록 (관리자가 중앙에서 관리)

```bash
curl -X POST http://localhost:8080/api/admin/clients \
  -H "X-Admin-Key: your-admin-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "host": "192.168.1.100",
    "port": 22,
    "username": "remote_admin",
    "password": "SecureP@ssw0rd123!",
    "description": "Development PC - John Doe"
  }'
```

**응답:**
```json
{
  "id": 1,
  "host": "192.168.1.100",
  "port": 22,
  "username": "remote_admin",
  "apiKey": "client-a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "description": "Development PC - John Doe",
  "active": true,
  "createdAt": "2025-01-15T10:30:00",
  "lastConnectedAt": null
}
```

**⚠️ 중요**: `apiKey` 값을 안전하게 보관하세요! 이 키로 명령어를 실행합니다.

#### 방법 B: 클라이언트 자가 등록

```bash
curl -X POST http://localhost:8080/api/clients/register \
  -H "Content-Type: application/json" \
  -d '{
    "host": "192.168.1.101",
    "port": 22,
    "username": "remote_admin",
    "password": "SecureP@ssw0rd123!",
    "description": "Testing PC - Jane Smith"
  }'
```

### 2. 명령어 실행

```bash
# 환경 변수로 API Key 저장
export API_KEY="client-a1b2c3d4-e5f6-7890-abcd-ef1234567890"

# 명령어 실행
curl -X POST http://localhost:8080/api/commands \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "targetHost": "192.168.1.100",
    "command": "hostname"
  }'
```

**응답:**
```json
{
  "commandId": 1,
  "status": "PENDING",
  "message": "Command execution started. Use GET /api/commands/1 to check status."
}
```

### 3. 명령어 상태 조회

```bash
curl -X GET http://localhost:8080/api/commands/1 \
  -H "X-API-Key: $API_KEY"
```

**응답 (실행 완료):**
```json
{
  "id": 1,
  "targetHost": "192.168.1.100",
  "command": "hostname",
  "status": "SUCCESS",
  "result": "DESKTOP-ABC123\n",
  "errorMessage": null,
  "exitCode": 0,
  "createdAt": "2025-01-15T10:35:00",
  "executedAt": "2025-01-15T10:35:01",
  "completedAt": "2025-01-15T10:35:03",
  "executionDurationMs": 2150
}
```

### 4. 명령어 이력 조회 (페이징)

```bash
# 최근 20개 조회
curl -X GET "http://localhost:8080/api/commands?page=0&size=20" \
  -H "X-API-Key: $API_KEY"

# 상태별 필터링
curl -X GET "http://localhost:8080/api/commands?status=SUCCESS&page=0&size=10" \
  -H "X-API-Key: $API_KEY"
```

### 5. 전체 클라이언트 조회 (Admin Only)

```bash
curl -X GET http://localhost:8080/api/admin/clients \
  -H "X-Admin-Key: your-admin-api-key"
```

### 6. API Key 재발급 (Admin Only)

보안 사고 발생 시 즉시 재발급:

```bash
curl -X POST http://localhost:8080/api/admin/clients/1/regenerate-key \
  -H "X-Admin-Key: your-admin-api-key"
```

### 7. 클라이언트 비활성화 (Admin Only)

```bash
curl -X POST http://localhost:8080/api/admin/clients/1/deactivate \
  -H "X-Admin-Key: your-admin-api-key"
```

---

## 설정 파일 상세

### application.yml 주요 설정

```yaml
app:
  security:
    # Admin API Key (환경 변수 권장)
    admin-api-key: ${ADMIN_API_KEY:admin-master-key-change-in-production}

  ssh:
    # SSH 명령어 실행 타임아웃 (초)
    timeout-seconds: 60

    # SSH 연결 타임아웃 (초)
    connect-timeout-seconds: 10

    # 재시도 설정
    retry:
      max-attempts: 3        # 최대 재시도 횟수
      backoff-ms: 1000       # 초기 대기 시간 (Exponential: 1s → 2s → 4s)

  command:
    # 화이트리스트 파일 경로
    whitelist-path: classpath:whitelist.txt

    # 블랙리스트 파일 경로
    blacklist-path: classpath:blacklist.txt

    # 정책 파일 자동 리로드 (5초마다 체크)
    policy-reload-enabled: true
    policy-check-interval-ms: 5000

  encryption:
    # 패스워드 암호화 키 (환경 변수 필수!)
    secret-key: ${ENCRYPTION_KEY:default-key-change-me}
```

### whitelist.txt (허용 명령어)

```txt
# 디렉토리 조회
^dir(\s.*)?$
^ls(\s.*)?$

# 시스템 정보
^systeminfo$
^hostname$
^whoami$
^ipconfig(\s.*)?$

# 네트워크 진단
^ping\s.*
^tracert\s.*
```

### blacklist.txt (차단 명령어)

```txt
# 위험한 삭제 명령어
.*\brm\s+-rf.*
.*\bdel\s+/[fF].*
.*\bformat\b.*

# 시스템 종료/재시작
.*\bshutdown\b.*
.*\breboot\b.*
```

---

## 보안 고려사항

### 1. API Key 관리

**❌ 절대 하지 말아야 할 것:**
- 코드에 API Key 하드코딩
- Git에 API Key 커밋
- 평문으로 API Key 전송 (HTTPS 필수)

**✅ 권장 사항:**
- 환경 변수로 관리
- Vault 등 비밀 관리 도구 사용
- 정기적으로 키 재발급 (3개월마다)
- 키 노출 시 즉시 재발급

### 2. 패스워드 보안

- 모든 SSH 패스워드는 **AES-256**으로 암호화하여 DB 저장
- 암호화 키(`ENCRYPTION_KEY`)는 환경 변수로 관리
- 최소 32자 이상의 강력한 암호화 키 사용
- 암호화 키 변경 시 모든 클라이언트 재등록 필요

### 3. 명령어 검증

**검증 우선순위:**
1. **화이트리스트 매칭** → 허용
2. **블랙리스트 매칭** → 거부
3. **둘 다 없음** → 거부 (기본 거부 정책)

**런타임 수정:**
- `whitelist.txt` 또는 `blacklist.txt` 파일 수정
- 최대 5초 내 자동 반영 (재시작 불필요)

### 4. 네트워크 보안 (운영 환경)

- ✅ HTTPS/TLS 사용 (SSL 인증서 설정)
- ✅ 방화벽 설정 (필요한 포트만 개방)
- ✅ VPN 또는 사설망 사용
- ✅ IP 화이트리스트 적용

---

## 트러블슈팅

### 1. SSH 연결 실패

**증상:**
```json
{
  "error": "Command failed: SSH connection failed"
}
```

**해결 방법:**

1. **클라이언트 SSH 서버 확인**
   ```powershell
   Get-Service sshd
   # Status가 Running이어야 함
   ```

2. **방화벽 확인**
   ```powershell
   Get-NetFirewallRule -Name sshd
   ```

3. **네트워크 연결 테스트**
   ```bash
   # 서버에서 실행
   telnet 192.168.1.100 22
   # 또는
   ssh remote_admin@192.168.1.100
   ```

### 2. 명령어 검증 실패

**증상:**
```json
{
  "message": "Command validation failed: Command not in whitelist"
}
```

**해결 방법:**

1. `whitelist.txt`에 패턴 추가:
   ```txt
   ^your-command\s.*$
   ```

2. 5초 대기 (자동 리로드)

3. 또는 애플리케이션 재시작

### 3. 타임아웃 발생

**증상:**
```json
{
  "status": "TIMEOUT"
}
```

**해결 방법:**

1. `application.yml`에서 타임아웃 증가:
   ```yaml
   app:
     ssh:
       timeout-seconds: 120  # 60초 → 120초
   ```

2. 애플리케이션 재시작

### 4. 데이터베이스 연결 실패

**증상:**
```
Failed to obtain JDBC Connection
```

**해결 방법:**

1. PostgreSQL 컨테이너 확인:
   ```bash
   docker ps | grep remote-command-db
   docker logs remote-command-db
   ```

2. 재시작:
   ```bash
   docker-compose down
   docker-compose up -d
   ```

---

## 추가 문서

- **[사용 가이드 (USAGE_GUIDE.md)](USAGE_GUIDE.md)** - 실전 사용 예제 및 스크립트

---

## 향후 개선 사항

### POC 이후 단기 목표
- [ ] HTTPS/TLS 설정
- [ ] Private Key 기반 SSH 인증
- [ ] API 문서화 (Swagger/OpenAPI)
- [ ] 단위 테스트 및 통합 테스트

### 중기 목표
- [ ] Redis 캐시 구현
- [ ] 멀티 클라이언트 동시 실행
- [ ] 명령어 스케줄링 (Cron)
- [ ] 웹 UI 대시보드

---

## 라이선스

이 프로젝트는 POC(Proof of Concept)용으로 개발되었습니다.

---

## 문의

프로젝트 관련 문의사항은 GitHub Issues를 통해 등록해 주세요.

**개발자**: Claude AI  
**프로젝트 타입**: POC (Proof of Concept)  
**개발 기간**: 2025년 1월
