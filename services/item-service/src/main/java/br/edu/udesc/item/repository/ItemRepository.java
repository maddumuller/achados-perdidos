package br.edu.udesc.item.repository;
import br.edu.udesc.item.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ItemRepository extends JpaRepository<Item, Long> {
}
