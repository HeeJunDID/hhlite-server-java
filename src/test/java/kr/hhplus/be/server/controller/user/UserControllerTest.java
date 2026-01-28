package kr.hhplus.be.server.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.common.exception.GlobalExceptionHandler;
import kr.hhplus.be.server.controller.user.dto.ChargePointRequest;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Green: 유저를 조회한다")
    void getUser_success() throws Exception {
        // given
        Long userId = 1L;
        User user = User.builder()
                .userId(userId)
                .balance(1000L)
                .password("password")
                .build();

        given(userService.getUser(userId)).willReturn(user);

        // when & then
        mockMvc.perform(get("/api/users/{userId}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.balance").value(1000L));
    }

    @Test
    @DisplayName("Red: 존재하지 않는 유저 조회 시 404 반환")
    void getUser_notFound() throws Exception {
        // given
        Long userId = 999L;
        given(userService.getUser(userId))
                .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/users/{userId}", userId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("U001"));
    }

    @Test
    @DisplayName("Green: 잔액을 조회한다")
    void getBalance_success() throws Exception {
        // given
        Long userId = 1L;
        long balance = 5000L;

        given(userService.getBalance(userId)).willReturn(balance);

        // when & then
        mockMvc.perform(get("/api/users/{userId}/balance", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.balance").value(balance));
    }

    @Test
    @DisplayName("Green: 포인트를 충전한다")
    void chargePoint_success() throws Exception {
        // given
        Long userId = 1L;
        long chargeAmount = 5000L;
        long newBalance = 6000L;

        User user = User.builder()
                .userId(userId)
                .balance(newBalance)
                .password("password")
                .build();

        ChargePointRequest request = new ChargePointRequest(chargeAmount);

        given(userService.chargePoint(userId, chargeAmount)).willReturn(user);

        // when & then
        mockMvc.perform(patch("/api/users/{userId}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.balance").value(newBalance));
    }

    @Test
    @DisplayName("Red: 존재하지 않는 유저에게 포인트 충전 시 404 반환")
    void chargePoint_userNotFound() throws Exception {
        // given
        Long userId = 999L;
        long chargeAmount = 5000L;

        ChargePointRequest request = new ChargePointRequest(chargeAmount);

        given(userService.chargePoint(userId, chargeAmount))
                .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/api/users/{userId}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("U001"));
    }

    @Test
    @DisplayName("Red: 0 이하 금액 충전 시 400 반환")
    void chargePoint_invalidAmount() throws Exception {
        // given
        Long userId = 1L;
        long chargeAmount = 0L;

        ChargePointRequest request = new ChargePointRequest(chargeAmount);

        given(userService.chargePoint(userId, chargeAmount))
                .willThrow(new BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT));

        // when & then
        mockMvc.perform(patch("/api/users/{userId}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("U002"));
    }
}
