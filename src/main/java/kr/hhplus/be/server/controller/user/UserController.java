package kr.hhplus.be.server.controller.user;

import kr.hhplus.be.server.controller.user.dto.BalanceResponse;
import kr.hhplus.be.server.controller.user.dto.ChargePointRequest;
import kr.hhplus.be.server.controller.user.dto.UserResponse;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        User user = userService.getUser(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long userId) {
        long balance = userService.getBalance(userId);
        return ResponseEntity.ok(BalanceResponse.of(userId, balance));
    }

    @PatchMapping("/{userId}/charge")
    public ResponseEntity<UserResponse> chargePoint(
            @PathVariable Long userId,
            @RequestBody ChargePointRequest request) {
        User user = userService.chargePoint(userId, request.getAmount());
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
