package kr.hhplus.be.server.queue.infrastructure.web;

import kr.hhplus.be.server.queue.application.port.in.*;
import kr.hhplus.be.server.queue.infrastructure.web.dto.IssueTokenRequest;
import kr.hhplus.be.server.queue.infrastructure.web.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueTokenController {

    private final IssueTokenUseCase issueTokenUseCase;
    private final ValidateTokenUseCase validateTokenUseCase;

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> issueToken(@RequestBody IssueTokenRequest request) {
        IssueTokenCommand command = new IssueTokenCommand(request.userId(), request.concertId());
        TokenResult result = issueTokenUseCase.issue(command);
        return ResponseEntity.ok(TokenResponse.from(result));
    }

    @GetMapping("/token/{token}")
    public ResponseEntity<TokenResponse> validateToken(@PathVariable String token) {
        TokenResult result = validateTokenUseCase.validate(token);
        return ResponseEntity.ok(TokenResponse.from(result));
    }
}
