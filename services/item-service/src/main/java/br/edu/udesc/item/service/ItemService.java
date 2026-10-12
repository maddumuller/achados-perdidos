package br.edu.udesc.item.service;
import br.edu.udesc.item.model.Item;
import br.edu.udesc.item.model.StatusItem;
import br.edu.udesc.item.repository.ItemRepository;
import org.springframework.stereotype.Service;
import java.util.List;



@Service
public class ItemService {

    private final ItemRepository repository;

    public ItemService(ItemRepository repository) {
        this.repository = repository;
    }

    public Item cadastrar(Item item) {
        item.setId(null);
        item.setStatus(StatusItem.DISPONIVEL);
        return repository.save(item);
    }

    public List<Item> listarTodos() {
        return repository.findAll();
    }

    public Item buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item não encontrado: " + id
                        )
                );
    }

    public Item atualizarStatus(Long id, StatusItem novoStatus) {
        Item item = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item não encontrado: " + id
                ));

        item.setStatus(novoStatus);

        return repository.save(item);
    }
}
