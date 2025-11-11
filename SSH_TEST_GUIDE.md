# SSH 연결 테스트 가이드

## 📋 목차
- [SSH 인증 방식 설명](#ssh-인증-방식-설명)
- [PC 1대로 테스트하는 방법](#pc-1대로-테스트하는-방법)
- [Docker 기반 SSH 테스트 환경](#docker-기반-ssh-테스트-환경)

---

## SSH 인증 방식 설명

### ⚠️ 중요: SSH 접속은 반드시 비밀번호가 필요합니다

README의 SSH 접속 테스트 섹션에서:

```bash
ssh remote_admin@192.168.1.100
```

이 명령어는 **SSH 접속을 시도**하는 명령어이며, 실행하면:

1. 명령어 입력 후 Enter
2. **비밀번호 입력 프롬프트 출력**: `remote_admin@192.168.1.100's password:`
3. 비밀번호 입력 (화면에 표시되지 않음)
4. 인증 성공 시 원격 셸 접속

### 인증서 기반 인증 (선택사항)

비밀번호 없이 접속하려면 SSH 키 페어를 생성하고 공개키를 서버에 등록해야 합니다:

```bash
# 1. 키 페어 생성
ssh-keygen -t rsa -b 4096

# 2. 공개키를 원격 서버에 복사
ssh-copy-id remote_admin@192.168.1.100

# 3. 이후부터는 비밀번호 없이 접속 가능
ssh remote_admin@192.168.1.100
```

**현재 프로젝트는 패스워드 기반 인증만 지원합니다.**

---

## PC 1대로 테스트하는 방법

### 방법 1: 로컬호스트 테스트 (Windows)

Windows PC에서 SSH 서버를 설치하고 **localhost로 자기 자신에게 SSH 접속**:

```powershell
# 1. OpenSSH Server 설치 (README 참조)
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0

# 2. SSH 서비스 시작
Start-Service sshd

# 3. 로컬에서 자기 자신에게 SSH 접속 테스트
ssh localhost
# 또는
ssh 127.0.0.1
# 또는
ssh your_username@localhost
```

**장점:**
- 별도 PC나 가상환경 불필요
- 실제 Windows SSH 서버 동작 확인 가능

**단점:**
- 실제 네트워크 환경과 다름
- 방화벽 설정 테스트 불가

---

### 방법 2: WSL (Windows Subsystem for Linux)

Windows에서 WSL을 사용하여 Linux 환경에서 Windows SSH 서버로 접속:

```bash
# WSL에서 Windows 호스트로 SSH 접속
# Windows IP는 /etc/resolv.conf에서 확인 가능
cat /etc/resolv.conf | grep nameserver
# nameserver 172.x.x.x (이 IP가 Windows 호스트 IP)

ssh remote_admin@172.x.x.x
```

**장점:**
- 실제 네트워크를 통한 테스트
- Linux 환경에서 테스트 가능

**단점:**
- WSL 설치 필요
- Windows 방화벽 설정 필요

---

## Docker 기반 SSH 테스트 환경

### ✅ 권장 방법: Docker로 SSH 클라이언트 시뮬레이션

**장점:**
- PC 1대로 완전한 SSH 서버-클라이언트 환경 구축
- 실제 네트워크 격리 테스트 가능
- 쉽게 초기화 및 재생성 가능

---

### 옵션 A: Linux SSH 서버 컨테이너 (가장 간단)

Spring Boot 애플리케이션에서 Docker 컨테이너 내부의 SSH 서버로 명령어 전송 테스트:

#### 1. SSH 서버 Docker 이미지 생성

`docker-compose.test.yml` 파일 생성:

```yaml
version: '3.8'

services:
  # PostgreSQL (기존)
  postgres:
    image: postgres:16-alpine
    container_name: remote_command_postgres
    environment:
      POSTGRES_DB: remote_command
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U admin -d remote_command"]
      interval: 10s
      timeout: 5s
      retries: 5

  # SSH 테스트 서버 (Linux)
  ssh-test-server:
    image: linuxserver/openssh-server:latest
    container_name: ssh_test_server
    environment:
      - PUID=1000
      - PGID=1000
      - TZ=Asia/Seoul
      - PASSWORD_ACCESS=true
      - USER_PASSWORD=test1234
      - USER_NAME=testuser
    ports:
      - "2222:2222"  # SSH 포트
    volumes:
      - ./ssh-test-data:/config
    restart: unless-stopped

volumes:
  postgres_data:
```

#### 2. 컨테이너 시작

```bash
docker-compose -f docker-compose.test.yml up -d
```

#### 3. SSH 접속 테스트

```bash
# 호스트에서 SSH 테스트
ssh testuser@localhost -p 2222
# 비밀번호: test1234

# 명령어 실행 테스트
ssh testuser@localhost -p 2222 "echo 'Hello from SSH'"
ssh testuser@localhost -p 2222 "ls -la"
ssh testuser@localhost -p 2222 "whoami"
```

#### 4. Spring Boot 애플리케이션에서 클라이언트 등록

```bash
# 1. 클라이언트 등록 API 호출
curl -X POST http://localhost:8080/api/clients/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "SSH Test Server",
    "host": "localhost",
    "port": 2222,
    "username": "testuser",
    "password": "test1234"
  }'

# 응답에서 API Key 확인
# 예: {"apiKey": "abc123...", "clientId": 1}
```

#### 5. 명령어 실행 테스트

```bash
# Admin API Key로 명령어 실행
curl -X POST http://localhost:8080/api/admin/commands/execute \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "command": "echo Hello from Spring Boot"
  }'

# 명령어 상태 조회
curl -X GET http://localhost:8080/api/admin/commands/1 \
  -H "X-Admin-Key: admin-master-key-change-in-production"
```

---

### 옵션 B: Windows SSH 서버 Docker (고급)

Windows 컨테이너를 사용하여 실제 Windows SSH 환경 시뮬레이션:

**주의:** Windows 컨테이너는 Windows 호스트에서만 실행 가능

```dockerfile
# Dockerfile.windows-ssh
FROM mcr.microsoft.com/windows/servercore:ltsc2022

# OpenSSH 설치
RUN powershell -Command \
    Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0; \
    Start-Service sshd; \
    Set-Service -Name sshd -StartupType 'Automatic'

# 사용자 생성
RUN net user remote_admin "SecurePass123!" /add
RUN net localgroup Administrators remote_admin /add

EXPOSE 22

CMD ["powershell", "-NoExit"]
```

**실행:**

```bash
# 이미지 빌드
docker build -f Dockerfile.windows-ssh -t windows-ssh-server .

# 컨테이너 실행
docker run -d -p 2222:22 --name windows-ssh windows-ssh-server
```

---

## 🔍 테스트 시나리오

### 1. 기본 연결 테스트

```bash
# SSH 연결 확인
ssh testuser@localhost -p 2222
```

**예상 결과:**
```
testuser@localhost's password: [test1234 입력]
Welcome to OpenSSH Server
testuser@ssh-server:~$
```

### 2. 명령어 실행 테스트

```bash
# 단순 명령어
ssh testuser@localhost -p 2222 "echo test"

# 파일 시스템 탐색
ssh testuser@localhost -p 2222 "ls -la /home"

# 시스템 정보
ssh testuser@localhost -p 2222 "uname -a"
```

### 3. 화이트리스트/블랙리스트 테스트

**화이트리스트에 등록된 명령어만 실행 가능:**

```bash
# 성공 (화이트리스트에 echo가 있다면)
curl -X POST http://localhost:8080/api/admin/commands/execute \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{"clientId": 1, "command": "echo test"}'

# 실패 (블랙리스트에 rm이 있다면)
curl -X POST http://localhost:8080/api/admin/commands/execute \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{"clientId": 1, "command": "rm -rf /"}'
```

### 4. 타임아웃 테스트

```bash
# 긴 실행 시간 명령어 (60초 타임아웃 테스트)
curl -X POST http://localhost:8080/api/admin/commands/execute \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{"clientId": 1, "command": "sleep 70"}'

# 상태 확인 (TIMEOUT 예상)
curl -X GET http://localhost:8080/api/admin/commands/{commandId} \
  -H "X-Admin-Key: admin-master-key-change-in-production"
```

---

## 🧹 정리 (Cleanup)

### Docker 컨테이너 중지 및 삭제

```bash
# 컨테이너 중지
docker-compose -f docker-compose.test.yml down

# 볼륨까지 삭제
docker-compose -f docker-compose.test.yml down -v

# SSH 테스트 데이터 삭제
rm -rf ./ssh-test-data
```

---

## 📊 비교 요약

| 방법 | 장점 | 단점 | 권장도 |
|------|------|------|--------|
| **로컬호스트 (localhost)** | 가장 간단, 추가 설치 불필요 | 실제 네트워크 환경과 다름 | ⭐⭐⭐ |
| **WSL** | Windows-Linux 간 테스트 가능 | WSL 설치 필요, 설정 복잡 | ⭐⭐⭐ |
| **Docker (Linux SSH)** | 격리된 환경, 쉬운 초기화 | Docker 필요, Linux 명령어만 테스트 | ⭐⭐⭐⭐⭐ |
| **Docker (Windows SSH)** | 실제 Windows 환경 시뮬레이션 | Windows 호스트 필요, 복잡함 | ⭐⭐ |

---

## 🎯 권장 테스트 순서

1. **로컬호스트 테스트** → SSH 기본 연결 확인
2. **Docker Linux SSH 서버** → 명령어 실행 및 정책 테스트
3. **실제 Windows PC** → 프로덕션 환경 시뮬레이션

---

## ❓ FAQ

### Q1: Docker 컨테이너에서 Spring Boot 앱이 접속할 수 없어요

**A:** Docker 네트워크 설정 확인:

```bash
# Spring Boot 앱도 같은 Docker 네트워크에 있어야 함
# 또는 host.docker.internal 사용 (Docker Desktop)

# 호스트에서 컨테이너로 접속 시:
ssh testuser@localhost -p 2222

# Spring Boot 앱이 호스트에서 실행 중이면:
# application.yml에서 host를 localhost, port를 2222로 설정
```

### Q2: "Permission denied (publickey)" 오류가 발생해요

**A:** 비밀번호 인증이 비활성화되어 있을 수 있습니다:

```bash
# SSH 서버 설정 확인 (/etc/ssh/sshd_config)
PasswordAuthentication yes
PubkeyAuthentication no  # 또는 yes (둘 다 가능)

# 설정 변경 후 SSH 서비스 재시작
sudo systemctl restart sshd
```

### Q3: 화이트리스트/블랙리스트 파일은 어디에 있나요?

**A:** 프로젝트 루트의 다음 경로:

```
src/main/resources/whitelist.txt
src/main/resources/blacklist.txt
```

파일을 수정하면 5초 이내에 자동으로 리로드됩니다 (파일 변경 감지).
