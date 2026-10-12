package br.edu.udesc.item.controller;
import br.edu.udesc.item.model.Item;
import br.edu.udesc.item.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
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
    public Item cadastrar(@RequestBody Item item) {
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
}