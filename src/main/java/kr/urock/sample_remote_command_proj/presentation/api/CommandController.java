package kr.urock.sample_remote_command_proj.presentation.api;

import jakarta.validation.Valid;
import kr.urock.sample_remote_command_proj.domain.client.ClientCredential;
import kr.urock.sample_remote_command_proj.domain.client.ClientService;
import kr.urock.sample_remote_command_proj.domain.command.Command;
import kr.urock.sample_remote_command_proj.domain.command.CommandService;
import kr.urock.sample_remote_command_proj.domain.command.CommandStatus;
import kr.urock.sample_remote_command_proj.presentation.api.dto.CommandResponse;
import kr.urock.sample_remote_command_proj.presentation.api.dto.ExecuteCommandRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Command API - 명령어 실행 및 조회
 *
 * 명령어 실행 요청 및 이력 조회 API
 */
@Slf4j
@RestController
@RequestMapping("/api/commands")
@RequiredArgsConstructor
public class CommandController {

    private final CommandService commandService;
    private final ClientService clientService;

    /**
     * 명령어 실행 요청
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> executeCommand(
        @Valid @RequestBody ExecuteCommandRequest request,
        Authentication authentication
    ) {
        String apiKey = authentication.getName();

        // 명령어 실행
        Long commandId = commandService.executeCommand(
            request.getTargetHost(),
            request.getCommand(),
            apiKey
        );

        // 응답
        Map<String, Object> response = new HashMap<>();
        response.put("commandId", commandId);
        response.put("status", "PENDING");
        response.put("message", "Command execution started. Use GET /api/commands/" + commandId + " to check status.");

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * 명령어 상태 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommandResponse> getCommand(@PathVariable Long id) {
        Command command = commandService.getCommand(id);
        return ResponseEntity.ok(CommandResponse.from(command));
    }

    /**
     * 명령어 이력 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @param status 상태 필터 (선택)
     * @param clientId 클라이언트 ID 필터 (선택, Admin만 사용 가능)
     * @param targetHost 대상 호스트 필터 (선택, Admin만 사용 가능)
     * @param authentication 인증 정보
     */
    @GetMapping
    public ResponseEntity<Page<CommandResponse>> getCommands(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestParam(required = false) CommandStatus status,
        @RequestParam(required = false) Long clientId,
        @RequestParam(required = false) String targetHost,
        Authentication authentication
    ) {
        // clientId 또는 targetHost 파라미터는 Admin만 사용 가능
        boolean isAdminOnlyParamUsed = (clientId != null || targetHost != null);
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdminOnlyParamUsed && !isAdmin) {
            throw new AccessDeniedException(
                "Admin role required to use 'clientId' or 'targetHost' parameters"
            );
        }

        Page<Command> commands;

        if (isAdmin) {
            commands = getCommandsForAdmin(pageable, status, clientId, targetHost);
        } else {
            commands = getCommandsForClient(pageable, authentication.getName());
        }

        Page<CommandResponse> response = commands.map(CommandResponse::from);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin용 명령어 조회 (내부 메서드)
     */
    private Page<Command> getCommandsForAdmin(
        Pageable pageable,
        CommandStatus status,
        Long clientId,
        String targetHost
    ) {
        // clientId와 targetHost를 동시에 제공하면 오류 (mutual exclusivity)
        if (clientId != null && targetHost != null) {
            throw new IllegalArgumentException(
                "Cannot specify both 'clientId' and 'targetHost' parameters. Use only one."
            );
        }

        // clientId가 제공된 경우, 해당 클라이언트의 targetHost로 변환
        if (clientId != null) {
            ClientCredential client = clientService.getClient(clientId);
            targetHost = client.getHost();
        }

        if (targetHost != null && status != null) {
            return commandService.getCommandsByTargetHostAndStatus(targetHost, status, pageable);
        } else if (targetHost != null) {
            return commandService.getCommandsByTargetHost(targetHost, pageable);
        } else if (status != null) {
            return commandService.getCommandsByStatus(status, pageable);
        } else {
            return commandService.getCommands(pageable);
        }
    }

    /**
     * Client용 명령어 조회 (내부 메서드)
     */
    private Page<Command> getCommandsForClient(Pageable pageable, String apiKey) {
        return commandService.getCommandsByApiKey(apiKey, pageable);
    }
}
