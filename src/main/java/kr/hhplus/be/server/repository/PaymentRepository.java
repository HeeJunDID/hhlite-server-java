package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
