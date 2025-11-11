# 사용 가이드 및 실습 예제

이 문서는 SSH 기반 원격 명령어 실행 시스템을 실제로 사용하는 방법을 단계별로 안내합니다.

## 📖 목차

1. [빠른 시작 (Quick Start)](#빠른-시작-quick-start)
2. [실전 사용 시나리오](#실전-사용-시나리오)
3. [PowerShell 스크립트 예제](#powershell-스크립트-예제)
4. [고급 사용법](#고급-사용법)
5. [모니터링 및 관리](#모니터링-및-관리)

---

## 빠른 시작 (Quick Start)

### 1단계: 환경 준비 (5분)

#### 서버 측

```bash
# 1. PostgreSQL 시작
docker-compose up -d

# 2. 서버 실행
./mvnw spring-boot:run
```

#### Windows 클라이언트 측

```powershell
# PowerShell (관리자 권한)

# 1. OpenSSH Server 설치
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0

# 2. 서비스 시작
Start-Service sshd
Set-Service -Name sshd -StartupType 'Automatic'

# 3. 방화벽 규칙 추가
New-NetFirewallRule -Name sshd -DisplayName 'OpenSSH Server (sshd)' `
    -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22

# 4. 현재 사용자로 SSH 접속 가능하도록 설정 완료
```

### 2단계: 클라이언트 등록 (2분)

**서버에서 실행:**

```bash
# 클라이언트 등록
curl -X POST http://localhost:8080/api/admin/clients \
  -H "X-Admin-Key: admin-master-key-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "host": "192.168.1.100",
    "port": 22,
    "username": "your_windows_username",
    "password": "your_windows_password",
    "description": "My Windows PC"
  }'
```

**응답에서 API Key 복사:**
```json
{
  "apiKey": "client-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

### 3단계: 첫 명령어 실행 (1분)

```bash
# API Key를 환경 변수로 저장
export API_KEY="client-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"

# 간단한 명령어 실행
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
  "message": "Command execution started..."
}
```

### 4단계: 결과 확인

```bash
# 결과 조회
curl -X GET http://localhost:8080/api/commands/1 \
  -H "X-API-Key: $API_KEY"
```

**성공 시 응답:**
```json
{
  "id": 1,
  "command": "hostname",
  "status": "SUCCESS",
  "result": "MY-PC-NAME\n",
  "exitCode": 0,
  "executionDurationMs": 1250
}
```

---

## 실전 사용 시나리오

### 시나리오 1: 여러 PC의 시스템 정보 수집

#### 목표
10대의 Windows PC에서 시스템 정보를 수집하여 보고서 작성

#### 단계별 실행

**1. 모든 PC 등록 (bash 스크립트)**

```bash
#!/bin/bash
# register_clients.sh

ADMIN_KEY="admin-master-key-change-in-production"
SERVER="http://localhost:8080"

# PC 목록 (IP, username, password, description)
clients=(
  "192.168.1.101:admin:Pass123:Development-PC-1"
  "192.168.1.102:admin:Pass123:Development-PC-2"
  "192.168.1.103:admin:Pass123:Testing-PC-1"
  # ... 더 추가
)

for client in "${clients[@]}"; do
  IFS=':' read -r ip user pass desc <<< "$client"

  echo "Registering $desc ($ip)..."

  curl -X POST "$SERVER/api/admin/clients" \
    -H "X-Admin-Key: $ADMIN_KEY" \
    -H "Content-Type: application/json" \
    -d "{
      \"host\": \"$ip\",
      \"port\": 22,
      \"username\": \"$user\",
      \"password\": \"$pass\",
      \"description\": \"$desc\"
    }" | jq '.'

  sleep 1
done
```

**2. 모든 PC에 시스템 정보 명령어 실행**

```bash
#!/bin/bash
# collect_system_info.sh

ADMIN_KEY="admin-master-key-change-in-production"
SERVER="http://localhost:8080"

# 모든 클라이언트 조회
clients=$(curl -s -X GET "$SERVER/api/admin/clients" \
  -H "X-Admin-Key: $ADMIN_KEY" | jq -r '.[] | .host')

# 각 클라이언트에 명령어 실행
for host in $clients; do
  echo "Collecting info from $host..."

  curl -X POST "$SERVER/api/commands" \
    -H "X-Admin-Key: $ADMIN_KEY" \
    -H "Content-Type: application/json" \
    -d "{
      \"targetHost\": \"$host\",
      \"command\": \"systeminfo\"
    }" | jq '.commandId' >> command_ids.txt

  sleep 0.5
done

echo "All commands submitted. Check results after a few seconds."
```

**3. 결과 수집**

```bash
#!/bin/bash
# collect_results.sh

ADMIN_KEY="admin-master-key-change-in-production"
SERVER="http://localhost:8080"

while IFS= read -r cmd_id; do
  echo "Fetching result for command $cmd_id..."

  curl -s -X GET "$SERVER/api/commands/$cmd_id" \
    -H "X-Admin-Key: $ADMIN_KEY" | jq '.' >> results.json

done < command_ids.txt

echo "Results saved to results.json"
```

### 시나리오 2: 디스크 용량 모니터링

#### PowerShell 스크립트 (서버 측)

```powershell
# monitor_disk_space.ps1

$SERVER = "http://localhost:8080"
$API_KEY = "admin-master-key-change-in-production"

# 디스크 용량 확인 명령어
$command = "Get-PSDrive C | Select-Object Used,Free"

# 모든 클라이언트 조회
$clients = Invoke-RestMethod -Uri "$SERVER/api/admin/clients" `
    -Headers @{"X-Admin-Key"=$API_KEY} -Method Get

foreach ($client in $clients) {
    Write-Host "Checking disk space on $($client.host)..."

    # 명령어 실행
    $response = Invoke-RestMethod -Uri "$SERVER/api/commands" `
        -Headers @{
            "X-Admin-Key"=$API_KEY
            "Content-Type"="application/json"
        } `
        -Method Post `
        -Body (@{
            targetHost = $client.host
            command = $command
        } | ConvertTo-Json)

    # 결과 대기 (3초)
    Start-Sleep -Seconds 3

    # 결과 조회
    $result = Invoke-RestMethod -Uri "$SERVER/api/commands/$($response.commandId)" `
        -Headers @{"X-Admin-Key"=$API_KEY} -Method Get

    # 결과 출력
    Write-Host "Host: $($client.host)"
    Write-Host "Status: $($result.status)"
    if ($result.status -eq "SUCCESS") {
        Write-Host "Result:`n$($result.result)"
    }
    Write-Host "---"
}
```

### 시나리오 3: 실시간 프로세스 모니터링

```python
# monitor_processes.py

import requests
import time
import json

SERVER = "http://localhost:8080"
ADMIN_KEY = "admin-master-key-change-in-production"

headers_admin = {"X-Admin-Key": ADMIN_KEY}
headers_json = {"Content-Type": "application/json"}

def get_all_clients():
    response = requests.get(f"{SERVER}/api/admin/clients", headers=headers_admin)
    return response.json()

def execute_command(host, command):
    data = {
        "targetHost": host,
        "command": command
    }
    response = requests.post(
        f"{SERVER}/api/commands",
        headers={**headers_admin, **headers_json},
        json=data
    )
    return response.json()["commandId"]

def get_command_result(command_id):
    response = requests.get(
        f"{SERVER}/api/commands/{command_id}",
        headers=headers_admin
    )
    return response.json()

def monitor_processes():
    clients = get_all_clients()

    while True:
        print("\n" + "="*50)
        print(f"Monitoring at {time.strftime('%Y-%m-%d %H:%M:%S')}")
        print("="*50)

        for client in clients:
            host = client["host"]
            print(f"\n[{host}]")

            # tasklist 명령어 실행
            cmd_id = execute_command(host, "tasklist")

            # 결과 대기
            time.sleep(2)

            # 결과 조회
            result = get_command_result(cmd_id)

            if result["status"] == "SUCCESS":
                # CPU 사용률 높은 프로세스만 필터링 (실제로는 파싱 필요)
                print(result["result"][:500])  # 처음 500자만 출력
            else:
                print(f"Failed: {result.get('errorMessage', 'Unknown error')}")

        # 30초 대기
        time.sleep(30)

if __name__ == "__main__":
    try:
        monitor_processes()
    except KeyboardInterrupt:
        print("\nMonitoring stopped.")
```

---

## PowerShell 스크립트 예제

### 1. 배치 명령어 실행

```powershell
# batch_execute.ps1

param(
    [Parameter(Mandatory=$true)]
    [string]$CommandFile,  # 명령어가 적힌 파일

    [Parameter(Mandatory=$true)]
    [string]$TargetHost
)

$SERVER = "http://localhost:8080"
$API_KEY = "your-api-key-here"

# 명령어 파일 읽기
$commands = Get-Content $CommandFile

foreach ($cmd in $commands) {
    if ([string]::IsNullOrWhiteSpace($cmd) -or $cmd.StartsWith("#")) {
        continue  # 빈 줄이나 주석 무시
    }

    Write-Host "Executing: $cmd"

    # 명령어 실행
    $response = Invoke-RestMethod -Uri "$SERVER/api/commands" `
        -Headers @{
            "X-API-Key"=$API_KEY
            "Content-Type"="application/json"
        } `
        -Method Post `
        -Body (@{
            targetHost = $TargetHost
            command = $cmd
        } | ConvertTo-Json)

    Write-Host "Command ID: $($response.commandId)"

    # 결과 대기
    Start-Sleep -Seconds 2

    # 결과 조회
    $result = Invoke-RestMethod -Uri "$SERVER/api/commands/$($response.commandId)" `
        -Headers @{"X-API-Key"=$API_KEY} -Method Get

    Write-Host "Status: $($result.status)"
    if ($result.status -eq "SUCCESS") {
        Write-Host "Output:`n$($result.result)`n"
    } else {
        Write-Host "Error: $($result.errorMessage)`n"
    }
}
```

**사용법:**

```powershell
# commands.txt 파일 생성
@"
hostname
ipconfig
systeminfo
"@ | Out-File commands.txt

# 실행
.\batch_execute.ps1 -CommandFile commands.txt -TargetHost "192.168.1.100"
```

### 2. CSV 파일 기반 대량 처리

```powershell
# process_from_csv.ps1

param(
    [Parameter(Mandatory=$true)]
    [string]$CsvFile
)

$SERVER = "http://localhost:8080"
$ADMIN_KEY = "admin-master-key-change-in-production"

# CSV 읽기 (host,command 형식)
$tasks = Import-Csv $CsvFile

$results = @()

foreach ($task in $tasks) {
    Write-Host "Executing on $($task.host): $($task.command)"

    # 명령어 실행
    $response = Invoke-RestMethod -Uri "$SERVER/api/commands" `
        -Headers @{
            "X-Admin-Key"=$ADMIN_KEY
            "Content-Type"="application/json"
        } `
        -Method Post `
        -Body (@{
            targetHost = $task.host
            command = $task.command
        } | ConvertTo-Json)

    $results += [PSCustomObject]@{
        Host = $task.host
        Command = $task.command
        CommandId = $response.commandId
    }
}

# 결과 대기 (모든 명령어가 완료될 때까지)
Start-Sleep -Seconds 5

# 결과 수집
$finalResults = @()

foreach ($r in $results) {
    $result = Invoke-RestMethod -Uri "$SERVER/api/commands/$($r.CommandId)" `
        -Headers @{"X-Admin-Key"=$ADMIN_KEY} -Method Get

    $finalResults += [PSCustomObject]@{
        Host = $r.Host
        Command = $r.Command
        Status = $result.status
        ExitCode = $result.exitCode
        Duration = $result.executionDurationMs
        Result = $result.result
    }
}

# CSV로 저장
$finalResults | Export-Csv -Path "results_$(Get-Date -Format 'yyyyMMdd_HHmmss').csv" -NoTypeInformation

Write-Host "Results saved to results_*.csv"
```

**tasks.csv 예제:**

```csv
host,command
192.168.1.101,hostname
192.168.1.101,ipconfig
192.168.1.102,hostname
192.168.1.102,systeminfo
```

---

## 고급 사용법

### 1. 명령어 템플릿 시스템

```bash
# templates.json
{
  "disk_check": "Get-PSDrive C | Select-Object @{Name='UsedGB';Expression={[math]::Round($_.Used/1GB,2)}},@{Name='FreeGB';Expression={[math]::Round($_.Free/1GB,2)}}",
  "top_processes": "Get-Process | Sort-Object CPU -Descending | Select-Object -First 5 Name, CPU, WorkingSet",
  "network_info": "Get-NetAdapter | Select-Object Name, Status, LinkSpeed"
}
```

```bash
#!/bin/bash
# execute_template.sh

TEMPLATE_NAME=$1
TARGET_HOST=$2

# 템플릿에서 명령어 가져오기
COMMAND=$(jq -r ".$TEMPLATE_NAME" templates.json)

if [ "$COMMAND" == "null" ]; then
    echo "Template not found: $TEMPLATE_NAME"
    exit 1
fi

# 명령어 실행
curl -X POST http://localhost:8080/api/commands \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"targetHost\": \"$TARGET_HOST\",
    \"command\": \"$COMMAND\"
  }"
```

**사용:**
```bash
./execute_template.sh disk_check 192.168.1.100
```

### 2. 주기적 모니터링 (Cron)

```bash
# 매 시간마다 디스크 용량 확인
0 * * * * /path/to/monitor_disk_space.sh >> /var/log/disk_monitor.log 2>&1

# 매일 오전 9시에 시스템 정보 수집
0 9 * * * /path/to/collect_system_info.sh >> /var/log/system_info.log 2>&1
```

### 3. 조건부 실행

```python
# conditional_execution.py

def execute_if_disk_low(host, threshold_gb=10):
    """디스크 여유 공간이 임계값 이하일 때만 정리 명령어 실행"""

    # 1. 디스크 용량 확인
    check_cmd = "Get-PSDrive C | Select-Object -ExpandProperty Free"
    cmd_id = execute_command(host, check_cmd)
    time.sleep(2)

    result = get_command_result(cmd_id)

    if result["status"] == "SUCCESS":
        free_bytes = int(result["result"].strip())
        free_gb = free_bytes / (1024**3)

        print(f"{host}: Free space = {free_gb:.2f} GB")

        # 2. 임계값 이하면 정리 명령어 실행
        if free_gb < threshold_gb:
            print(f"Low disk space detected on {host}. Cleaning...")
            cleanup_cmd = "Remove-Item C:\\Temp\\* -Recurse -Force"
            execute_command(host, cleanup_cmd)
        else:
            print(f"{host}: Disk space OK")
```

---

## 모니터링 및 관리

### 1. 실행 이력 분석

```bash
# 최근 100개 명령어 조회 (페이징)
curl -X GET "http://localhost:8080/api/commands?page=0&size=100" \
  -H "X-Admin-Key: $ADMIN_KEY" | jq '.content[] | {id, command, status, duration: .executionDurationMs}'
```

### 2. 실패한 명령어 조회

```bash
# 실패한 명령어만 필터링
curl -X GET "http://localhost:8080/api/commands?status=FAILED&size=50" \
  -H "X-Admin-Key: $ADMIN_KEY" | jq '.content[] | {id, command, host: .targetHost, error: .errorMessage}'
```

### 3. 성능 통계

```python
# performance_stats.py

import requests
import statistics

def get_performance_stats():
    response = requests.get(
        "http://localhost:8080/api/commands?size=1000",
        headers={"X-Admin-Key": "admin-key"}
    )

    commands = response.json()["content"]

    # 성공한 명령어만 필터링
    success_commands = [c for c in commands if c["status"] == "SUCCESS"]

    if not success_commands:
        print("No successful commands found")
        return

    durations = [c["executionDurationMs"] for c in success_commands]

    print(f"Total commands: {len(commands)}")
    print(f"Successful: {len(success_commands)}")
    print(f"Failed: {len([c for c in commands if c['status'] == 'FAILED'])}")
    print(f"\nExecution time statistics (ms):")
    print(f"  Min: {min(durations)}")
    print(f"  Max: {max(durations)}")
    print(f"  Average: {statistics.mean(durations):.2f}")
    print(f"  Median: {statistics.median(durations):.2f}")

if __name__ == "__main__":
    get_performance_stats()
```

### 4. 클라이언트 상태 대시보드

```bash
#!/bin/bash
# dashboard.sh

clear

while true; do
    clear
    echo "=== Remote Command System Dashboard ==="
    echo "Updated: $(date)"
    echo ""

    # 클라이언트 상태
    echo "=== Clients ==="
    curl -s -X GET "http://localhost:8080/api/admin/clients" \
      -H "X-Admin-Key: $ADMIN_KEY" | jq -r '.[] | "\(.host): \(if .active then "ACTIVE" else "INACTIVE" end) (Last connected: \(.lastConnectedAt // "Never"))"'

    echo ""
    echo "=== Recent Commands ==="
    curl -s -X GET "http://localhost:8080/api/commands?size=5" \
      -H "X-Admin-Key: $ADMIN_KEY" | jq -r '.content[] | "\(.id): \(.command) on \(.targetHost) - \(.status)"'

    echo ""
    echo "Press Ctrl+C to exit"
    sleep 10
done
```

---

## 보안 체크리스트

실제 사용 전 반드시 확인:

- [ ] Admin API Key 변경 (`ADMIN_API_KEY` 환경 변수)
- [ ] 암호화 키 변경 (`ENCRYPTION_KEY` 환경 변수, 32자 이상)
- [ ] HTTPS 설정 (운영 환경)
- [ ] 방화벽 설정 (필요한 포트만 개방)
- [ ] 화이트리스트 검토 (최소 권한 원칙)
- [ ] 블랙리스트 검토 (위험 명령어 차단)
- [ ] 정기적 API Key 재발급 계획
- [ ] 로그 모니터링 설정

---

## 참고 자료

- [메인 README](README.md) - 전체 시스템 개요
- [OpenSSH for Windows 공식 문서](https://docs.microsoft.com/en-us/windows-server/administration/openssh/openssh_install_firstuse)
- [Spring Security 공식 문서](https://spring.io/projects/spring-security)
