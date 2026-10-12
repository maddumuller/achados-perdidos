package br.edu.udesc.gateway.web;

import br.edu.udesc.gateway.repository.RequestLogRepository;
import br.edu.udesc.gateway.web.dto.RequestLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gateway/admin/logs")
public class LogController {

    private final RequestLogRepository repository;

    public LogController(RequestLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Page<RequestLogResponse> listarLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findAll(pageable).map(RequestLogResponse::fromEntity);
    }
}
