package kr.urock.sample_remote_command_proj.presentation.api;

import jakarta.validation.Valid;
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
     */
    @GetMapping
    public ResponseEntity<Page<CommandResponse>> getCommands(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestParam(required = false) CommandStatus status,
        Authentication authentication
    ) {
        Page<Command> commands;

        // Admin인 경우 전체 조회, Client인 경우 자신의 것만 조회
        if (authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            // Admin: 전체 조회
            if (status != null) {
                commands = commandService.getCommandsByStatus(status, pageable);
            } else {
                commands = commandService.getCommands(pageable);
            }
        } else {
            // Client: 자신의 API Key로 조회
            String apiKey = authentication.getName();
            commands = commandService.getCommandsByApiKey(apiKey, pageable);
        }

        Page<CommandResponse> response = commands.map(CommandResponse::from);
        return ResponseEntity.ok(response);
    }
}
