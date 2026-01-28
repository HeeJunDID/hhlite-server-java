package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
