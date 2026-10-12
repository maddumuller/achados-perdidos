package br.edu.udesc.gateway.repository;

import br.edu.udesc.gateway.domain.RequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    Page<RequestLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
