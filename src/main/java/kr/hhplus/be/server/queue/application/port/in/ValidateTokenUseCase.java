package kr.hhplus.be.server.queue.application.port.in;

public interface ValidateTokenUseCase {
    TokenResult validate(String token);
}
