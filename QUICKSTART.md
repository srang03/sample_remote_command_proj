# 빠른 시작 가이드 (Quick Start)

이 문서는 5분 내에 시스템을 설정하고 첫 명령어를 실행하는 방법을 안내합니다.

## 전제 조건

- Java 21 설치
- Docker 설치
- Windows PC (클라이언트)

---

## 1단계: 서버 설정 (2분)

### 1.1 프로젝트 다운로드 및 데이터베이스 시작

```bash
cd sample_remote_command_proj

# PostgreSQL 시작
docker-compose up -d

# 확인
docker ps
```

### 1.2 서버 실행

```bash
./mvnw spring-boot:run
```

**확인:**
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"} 응답 확인
```

---

## 2단계: Windows 클라이언트 설정 (2분)

### PowerShell (관리자 권한)에서 실행:

```powershell
# SSH Server 설치
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0

# 시작 및 자동 시작 설정
Start-Service sshd
Set-Service -Name sshd -StartupType 'Automatic'

# 방화벽 허용
New-NetFirewallRule -Name sshd -DisplayName 'OpenSSH Server (sshd)' `
    -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22

# 현재 PC의 IP 확인
ipconfig | findstr IPv4
```

IP 주소 기록: `192.168.1.XXX`

---

## 3단계: 클라이언트 등록 (1분)

**서버에서 실행 (Linux/Mac):**

```bash
# 클라이언트 등록
curl -X POST http://localhost:8080/api/admin/clients \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "host": "192.168.1.XXX",
    "port": 22,
    "username": "YOUR_WINDOWS_USERNAME",
    "password": "YOUR_WINDOWS_PASSWORD",
    "description": "My PC"
  }' | jq '.'
```

**응답에서 `apiKey` 복사:**
```json
{
  "apiKey": "client-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

**API Key 저장:**
```bash
export API_KEY="client-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
```

---

## 4단계: 첫 명령어 실행 (1분)

### 4.1 간단한 명령어 실행

```bash
# hostname 명령어 실행
curl -X POST http://localhost:8080/api/commands \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "targetHost": "192.168.1.XXX",
    "command": "hostname"
  }' | jq '.'
```

**응답:**
```json
{
  "commandId": 1,
  "status": "PENDING",
  "message": "Command execution started..."
}
```

### 4.2 결과 확인 (2-3초 후)

```bash
curl -X GET http://localhost:8080/api/commands/1 \
  -H "X-API-Key: $API_KEY" | jq '.'
```

**성공 응답:**
```json
{
  "id": 1,
  "command": "hostname",
  "status": "SUCCESS",
  "result": "YOUR-PC-NAME\n",
  "exitCode": 0
}
```

---

## 5단계: 추가 명령어 테스트

### 시스템 정보 조회

```bash
curl -X POST http://localhost:8080/api/commands \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "targetHost": "192.168.1.XXX",
    "command": "systeminfo"
  }' | jq '.commandId'
```

### 디렉토리 조회

```bash
curl -X POST http://localhost:8080/api/commands \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "targetHost": "192.168.1.XXX",
    "command": "dir C:\\"
  }' | jq '.commandId'
```

### IP 설정 조회

```bash
curl -X POST http://localhost:8080/api/commands \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "targetHost": "192.168.1.XXX",
    "command": "ipconfig"
  }' | jq '.commandId'
```

---

## 편리한 스크립트

### 명령어 실행 헬퍼 스크립트

```bash
#!/bin/bash
# execute.sh

API_KEY="your-api-key-here"
HOST="192.168.1.XXX"
COMMAND="$1"

if [ -z "$COMMAND" ]; then
    echo "Usage: $0 <command>"
    exit 1
fi

# 명령어 실행
CMD_ID=$(curl -s -X POST http://localhost:8080/api/commands \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"targetHost\": \"$HOST\",
    \"command\": \"$COMMAND\"
  }" | jq -r '.commandId')

echo "Command ID: $CMD_ID"
echo "Waiting for result..."
sleep 3

# 결과 조회
curl -s -X GET "http://localhost:8080/api/commands/$CMD_ID" \
  -H "X-API-Key: $API_KEY" | jq '.result'
```

**사용:**
```bash
chmod +x execute.sh
./execute.sh "hostname"
./execute.sh "whoami"
./execute.sh "dir"
```

---

## 다음 단계

설정이 완료되었습니다! 이제:

1. **[README.md](README.md)** - 전체 시스템 문서
2. **[USAGE_GUIDE.md](USAGE_GUIDE.md)** - 실전 사용 예제

을 참고하여 더 많은 기능을 활용하세요.

---

## 문제 해결

### SSH 연결 실패

```powershell
# Windows에서 SSH 서비스 확인
Get-Service sshd

# 방화벽 규칙 확인
Get-NetFirewallRule -Name sshd
```

### 명령어 검증 실패

`whitelist.txt` 파일에 해당 명령어 패턴이 있는지 확인:

```bash
cat src/main/resources/whitelist.txt
```

### API Key 오류

API Key가 올바르게 저장되었는지 확인:

```bash
echo $API_KEY
```

---

**축하합니다! 시스템이 정상적으로 작동하고 있습니다.** 🎉
