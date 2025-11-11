# 호스트 PC SSH 서버 테스트 가이드

## 📌 목적

**로컬 Windows PC에 설치한 OpenSSH Server가 정상 작동하는지 확인**
- Docker 컨테이너에서 → 호스트 PC의 SSH 서버로 접속 테스트
- 접속 성공 시 → Spring Boot API를 통한 실제 원격 명령 실행 테스트

---

## 🎯 테스트 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    호스트 PC (Windows)                   │
│                                                          │
│  ┌──────────────────────┐      ┌─────────────────────┐ │
│  │  OpenSSH Server      │      │  Spring Boot App    │ │
│  │  (Port 22)           │◄─────│  (Port 8080)        │ │
│  └──────────────────────┘      └─────────────────────┘ │
│            ▲                             │              │
│            │                             │              │
│            │                             ▼              │
│  ┌─────────┴──────────┐      ┌─────────────────────┐  │
│  │ Docker Container   │      │  PostgreSQL         │  │
│  │ (SSH Client Test)  │      │  (Docker)           │  │
│  └────────────────────┘      └─────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

**테스트 흐름:**
1. Docker 컨테이너 → 호스트 SSH 서버 (연결 테스트)
2. Spring Boot → 호스트 SSH 서버 (API 통한 명령 실행)

---

## 📋 사전 준비 (Prerequisites)

### 1. Windows PC에 OpenSSH Server 설치

README.md의 "Windows 클라이언트 설정 가이드" 참조하여 설치:

```powershell
# 관리자 권한 PowerShell에서 실행
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0
Start-Service sshd
Set-Service -Name sshd -StartupType 'Automatic'

# 방화벽 규칙 확인
Get-NetFirewallRule -Name *ssh*

# SSH 서비스 상태 확인
Get-Service sshd
```

**예상 출력:**
```
Status   Name               DisplayName
------   ----               -----------
Running  sshd               OpenSSH SSH Server
```

### 2. SSH 접속용 사용자 계정 확인

```powershell
# 현재 사용자 확인
whoami
# 예: DESKTOP-ABC123\username

# 또는 로컬 사용자 목록
Get-LocalUser
```

### 3. 호스트 PC IP 주소 확인

```powershell
# IPv4 주소 확인
ipconfig

# 예상 출력에서 IPv4 주소 확인:
# IPv4 Address. . . . . . . . . . . : 192.168.1.100
```

---

## 🚀 1단계: Docker 컨테이너 시작

### 컨테이너 시작

```bash
# PostgreSQL + SSH 클라이언트 테스트 컨테이너 시작
docker-compose up -d

# 컨테이너 로그 확인 (사용 방법 안내 출력됨)
docker-compose logs ssh-client-test
```

**예상 출력:**
```
=== SSH Client Test Container Started ===
Installing OpenSSH client...
✓ SSH client installed!

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 호스트 PC SSH 서버 테스트 방법
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1️⃣ 컨테이너 내부로 접속:
   docker exec -it ssh-client-test sh
...
```

### 컨테이너 상태 확인

```bash
docker ps

# 예상 출력:
# CONTAINER ID   IMAGE           STATUS         NAMES
# abc123...      alpine:latest   Up 10 seconds  ssh-client-test
# def456...      postgres:16     Up 10 seconds  remote-command-db
```

---

## 🔍 2단계: Docker 컨테이너에서 호스트 SSH 접속 테스트

### 컨테이너 내부로 접속

```bash
docker exec -it ssh-client-test sh
```

**프롬프트 변경 확인:**
```
/ #  (컨테이너 내부)
```

### 방법 A: host.docker.internal 사용 (Docker Desktop)

**Docker Desktop (Windows/Mac)을 사용하는 경우 권장:**

```bash
# 1. 호스트 연결 테스트 (ping)
ping -c 3 host.docker.internal

# 2. SSH 포트 확인
nc -zv host.docker.internal 22
# 또는
telnet host.docker.internal 22

# 3. SSH 접속 시도
ssh your_username@host.docker.internal
```

**예상 출력:**
```
The authenticity of host 'host.docker.internal (192.168.65.2)' can't be established.
ECDSA key fingerprint is SHA256:...
Are you sure you want to continue connecting (yes/no)? yes

your_username@host.docker.internal's password: [비밀번호 입력]

Microsoft Windows [Version 10.0.19045.xxxx]
(c) Microsoft Corporation. All rights reserved.

C:\Users\your_username>
```

### 방법 B: 실제 IP 주소 사용

**호스트 PC의 실제 IP 주소를 확인한 경우:**

```bash
# 예: 호스트 IP가 192.168.1.100인 경우
ping -c 3 192.168.1.100
nc -zv 192.168.1.100 22
ssh your_username@192.168.1.100
```

### SSH 접속 성공 시

**Windows 명령어 실행 테스트:**

```bash
# PowerShell 명령어는 작동하지 않을 수 있음 (기본 셸이 cmd.exe)
# CMD 명령어 사용

# 1. 현재 사용자 확인
ssh your_username@host.docker.internal "whoami"

# 2. 디렉토리 목록
ssh your_username@host.docker.internal "dir C:\"

# 3. 시스템 정보
ssh your_username@host.docker.internal "systeminfo | findstr /C:\"OS Name\""

# 4. 환경 변수 확인
ssh your_username@host.docker.internal "echo %USERNAME%"

# 5. 네트워크 정보
ssh your_username@host.docker.internal "ipconfig"
```

### PowerShell 명령어를 사용하려면

**README의 6단계 참조하여 PowerShell을 기본 셸로 설정:**

```powershell
# 호스트 PC에서 실행 (관리자 권한 PowerShell)
New-ItemProperty -Path "HKLM:\SOFTWARE\OpenSSH" -Name DefaultShell `
    -Value "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" `
    -PropertyType String -Force

# SSH 서비스 재시작
Restart-Service sshd
```

**설정 후 PowerShell 명령어 실행:**

```bash
# Docker 컨테이너에서
ssh your_username@host.docker.internal "Get-Process | Select-Object -First 5"
ssh your_username@host.docker.internal "Get-Date"
ssh your_username@host.docker.internal "Test-NetConnection -ComputerName google.com -Port 80"
```

### 컨테이너에서 빠져나오기

```bash
exit
# 또는 Ctrl+D
```

---

## ⚠️ 문제 해결 (Troubleshooting)

### 1. "Connection refused" 오류

**원인:** SSH 서비스가 실행되지 않음

**해결:**
```powershell
# 호스트 PC에서
Start-Service sshd
Get-Service sshd
```

### 2. "No route to host" 오류

**원인:** Docker와 호스트 간 네트워크 문제

**해결:**
```bash
# 컨테이너에서 호스트 IP 확인
# Docker Desktop 사용 시:
getent hosts host.docker.internal

# 호스트 PC에서 방화벽 확인
Get-NetFirewallRule -Name *ssh* | Select-Object Name, Enabled
```

### 3. "Permission denied" 오류

**원인:** 비밀번호 인증이 비활성화되어 있을 수 있음

**해결:**
```powershell
# 호스트 PC에서 SSH 설정 파일 확인
notepad C:\ProgramData\ssh\sshd_config

# 다음 설정 확인 및 수정:
PasswordAuthentication yes
PubkeyAuthentication yes

# 변경 후 SSH 재시작
Restart-Service sshd
```

### 4. host.docker.internal이 작동하지 않음

**원인:** Docker Desktop이 아닌 Docker Engine 사용 중

**해결:**
```bash
# docker-compose.yml에 extra_hosts 추가 (현재 주석 처리됨)
# 주석 해제:
extra_hosts:
  - "host.docker.internal:host-gateway"

# 재시작
docker-compose down
docker-compose up -d
```

---

## 🎮 3단계: Spring Boot API로 실제 명령 실행 테스트

### 1. Spring Boot 애플리케이션 시작

```bash
# 호스트 PC에서
./mvnw spring-boot:run

# 또는 이미 실행 중이면 건너뛰기
```

**서버 시작 확인:**
```
Started SampleRemoteCommandProjApplication in X.XXX seconds
```

### 2. 클라이언트 등록 API 호출

**호스트 PC 정보를 클라이언트로 등록:**

```bash
# host.docker.internal 사용 시
curl -X POST http://localhost:8080/api/clients/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Windows PC",
    "host": "host.docker.internal",
    "port": 22,
    "username": "your_username",
    "password": "your_password"
  }'

# 실제 IP 주소 사용 시 (예: 192.168.1.100)
curl -X POST http://localhost:8080/api/clients/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Windows PC",
    "host": "192.168.1.100",
    "port": 22,
    "username": "your_username",
    "password": "your_password"
  }'
```

**예상 응답:**
```json
{
  "id": 1,
  "name": "My Windows PC",
  "host": "host.docker.internal",
  "port": 22,
  "username": "your_username",
  "apiKey": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "active": true,
  "createdAt": "2025-11-11T10:00:00",
  "lastConnectedAt": null
}
```

**⚠️ 중요: API Key 저장!**
```
apiKey: a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

### 3. 명령어 실행 API 호출

#### 3-1. 간단한 명령어 테스트 (echo)

```bash
curl -X POST http://localhost:8080/api/admin/commands/execute \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "command": "echo Hello from Spring Boot"
  }'
```

**예상 응답:**
```json
{
  "id": 1,
  "clientId": 1,
  "command": "echo Hello from Spring Boot",
  "status": "PENDING",
  "createdAt": "2025-11-11T10:05:00",
  "executedAt": null,
  "output": null,
  "errorMessage": null
}
```

#### 3-2. 명령어 실행 상태 조회

```bash
# 몇 초 대기 후 실행 (비동기 처리)
curl -X GET http://localhost:8080/api/admin/commands/1 \
  -H "X-Admin-Key: admin-master-key-change-in-production"
```

**성공 시 응답:**
```json
{
  "id": 1,
  "clientId": 1,
  "command": "echo Hello from Spring Boot",
  "status": "SUCCESS",
  "output": "Hello from Spring Boot\n",
  "errorMessage": null,
  "createdAt": "2025-11-11T10:05:00",
  "executedAt": "2025-11-11T10:05:03",
  "completedAt": "2025-11-11T10:05:03"
}
```

#### 3-3. 시스템 정보 조회 (Windows 명령어)

```bash
# 현재 사용자 조회
curl -X POST http://localhost:8080/api/admin/commands/execute \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "command": "whoami"
  }'

# 디렉토리 목록
curl -X POST http://localhost:8080/api/admin/commands/execute \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "command": "dir C:\\"
  }'

# 시스템 정보
curl -X POST http://localhost:8080/api/admin/commands/execute \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "command": "systeminfo | findstr /C:\"OS Name\" /C:\"OS Version\""
  }'
```

#### 3-4. PowerShell 명령어 (기본 셸이 PowerShell인 경우)

```bash
curl -X POST http://localhost:8080/api/admin/commands/execute \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "command": "Get-Process | Select-Object -First 5 | Format-Table"
  }'

curl -X POST http://localhost:8080/api/admin/commands/execute \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "command": "Get-ComputerInfo | Select-Object CsName, WindowsVersion"
  }'
```

### 4. 화이트리스트/블랙리스트 테스트

#### 4-1. 화이트리스트에 없는 명령어 (실패 예상)

```bash
curl -X POST http://localhost:8080/api/admin/commands/execute \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "command": "dangerous-command"
  }'
```

**예상 응답:**
```json
{
  "error": "Command validation failed: Command not in whitelist"
}
```

#### 4-2. 블랙리스트에 있는 명령어 (실패 예상)

```bash
curl -X POST http://localhost:8080/api/admin/commands/execute \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "command": "rm -rf /"
  }'
```

**예상 응답:**
```json
{
  "error": "Command validation failed: Command in blacklist"
}
```

### 5. Swagger UI에서 테스트

**브라우저에서:**
```
http://localhost:8080/swagger-ui.html
```

1. "Authorize" 버튼 클릭
2. Admin API Key 입력: `admin-master-key-change-in-production`
3. "admin-controller" 섹션 확장
4. "POST /api/admin/commands/execute" 선택
5. "Try it out" 클릭
6. Request body 입력:
```json
{
  "clientId": 1,
  "command": "echo Test from Swagger"
}
```
7. "Execute" 클릭
8. 응답 확인

---

## 📊 전체 테스트 플로우 요약

### ✅ 성공 시나리오

```
1. Docker 컨테이너 시작
   ↓
2. 컨테이너에서 호스트 SSH 접속 성공
   ↓
3. Spring Boot 애플리케이션 시작
   ↓
4. 클라이언트 등록 API 호출
   ↓
5. 명령어 실행 API 호출
   ↓
6. 명령어 상태 조회 → SUCCESS
   ↓
7. 출력 결과 확인
```

### 🔍 확인 포인트

- [ ] Docker 컨테이너가 정상 시작됨
- [ ] 컨테이너에서 호스트로 ping 성공
- [ ] 컨테이너에서 호스트 SSH 22번 포트 접속 가능
- [ ] SSH 로그인 성공 (비밀번호 인증)
- [ ] SSH로 명령어 실행 가능 (`whoami`, `dir` 등)
- [ ] Spring Boot 클라이언트 등록 성공 (API Key 발급)
- [ ] 명령어 실행 API 호출 성공
- [ ] 명령어 상태가 PENDING → EXECUTING → SUCCESS로 변경
- [ ] 출력 결과가 정상적으로 반환됨
- [ ] 화이트리스트/블랙리스트 검증 작동

---

## 🧹 테스트 완료 후 정리

```bash
# 컨테이너 중지 및 삭제
docker-compose down

# 볼륨까지 삭제 (데이터베이스 초기화)
docker-compose down -v

# 이미지 삭제 (선택사항)
docker rmi alpine:latest postgres:16-alpine
```

---

## 📝 추가 참고사항

### Docker Desktop vs Docker Engine

- **Docker Desktop (Windows/Mac)**: `host.docker.internal` 자동 지원
- **Docker Engine (Linux)**: `extra_hosts` 설정 필요

### Windows 방화벽 규칙 추가

```powershell
# SSH 포트 (22) 인바운드 규칙 추가
New-NetFirewallRule -Name sshd -DisplayName 'OpenSSH Server (sshd)' `
    -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22
```

### SSH 디버그 모드로 접속

```bash
# 컨테이너에서
ssh -v your_username@host.docker.internal
# 또는
ssh -vvv your_username@host.docker.internal  # 매우 상세한 로그
```

### 호스트 PC에서 SSH 로그 확인

```powershell
# Windows Event Viewer에서 확인
eventvwr.msc
# Applications and Services Logs → OpenSSH → Operational

# 또는 PowerShell로
Get-WinEvent -LogName 'OpenSSH/Operational' -MaxEvents 10
```

---

## ❓ FAQ

### Q1: host.docker.internal이 해석되지 않아요

**A:** Docker Desktop을 사용 중인지 확인하세요. Docker Engine만 사용하는 경우:

```yaml
# docker-compose.yml에서 주석 해제
extra_hosts:
  - "host.docker.internal:host-gateway"
```

### Q2: SSH 접속 시 "Connection timed out"

**A:**
1. 호스트 PC에서 SSH 서비스 실행 확인: `Get-Service sshd`
2. 방화벽 규칙 확인: `Get-NetFirewallRule -Name *ssh*`
3. 22번 포트 리스닝 확인: `netstat -an | findstr :22`

### Q3: Spring Boot에서 명령어 실행 시 타임아웃

**A:** application.yml에서 타임아웃 설정 확인:
```yaml
app:
  ssh:
    timeout-seconds: 60  # 필요시 증가
```

### Q4: 비밀번호가 맞는데 로그인이 안 돼요

**A:** SSH 설정 확인:
```powershell
# sshd_config 확인
notepad C:\ProgramData\ssh\sshd_config

# 다음 설정 확인:
PasswordAuthentication yes
```

---

## 🎯 다음 단계

1. ✅ Docker 컨테이너로 호스트 SSH 접속 확인
2. ✅ Spring Boot API로 명령어 실행 확인
3. 🔜 실제 프로덕션 환경에서 테스트
4. 🔜 여러 클라이언트 등록 및 관리
5. 🔜 명령어 실행 이력 분석

모든 테스트가 성공하면 SSH 기반 원격 명령 실행 시스템이 정상 작동하는 것입니다!
