package kr.hhplus.be.server.queue.application.port.in;

public interface ExpireTokenUseCase {
    void expireToken(String token);
    int expireAllExpiredTokens();
}
