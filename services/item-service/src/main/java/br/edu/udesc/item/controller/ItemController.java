package br.edu.udesc.item.controller;
import br.edu.udesc.item.model.Item;
import br.edu.udesc.item.model.StatusItem;
import br.edu.udesc.item.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;


@RestController
@RequestMapping("/itens")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Item cadastrar(@Valid @RequestBody Item item) {
        return service.cadastrar(item);
    }

    @GetMapping
    public List<Item> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public Item buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PatchMapping("/{id}/status")
    public Item atualizarStatus(
            @PathVariable Long id,
            @RequestBody StatusItem novoStatus
    ) {
        return service.atualizarStatus(id, novoStatus);
    }
}