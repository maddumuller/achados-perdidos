package br.edu.udesc.gateway.web;

import br.edu.udesc.gateway.service.RouteAdminService;
import br.edu.udesc.gateway.web.dto.RouteRequest;
import br.edu.udesc.gateway.web.dto.RouteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gateway/admin/routes")
public class RouteAdminController {

    private final RouteAdminService service;

    public RouteAdminController(RouteAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<RouteResponse> listar() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<RouteResponse> criar(@Valid @RequestBody RouteRequest request) {
        RouteResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public RouteResponse buscarPorId(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public RouteResponse atualizar(@PathVariable Long id, @Valid @RequestBody RouteRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.delete(id);
    }
}
