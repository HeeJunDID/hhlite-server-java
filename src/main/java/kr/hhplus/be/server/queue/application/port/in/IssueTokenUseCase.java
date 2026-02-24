package kr.hhplus.be.server.queue.application.port.in;

public interface IssueTokenUseCase {
    TokenResult issue(IssueTokenCommand command);
}
