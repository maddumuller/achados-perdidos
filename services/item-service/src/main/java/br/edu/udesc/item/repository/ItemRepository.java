package br.edu.udesc.item.repository;
import br.edu.udesc.item.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ItemRepository extends JpaRepository<Item, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from Item i where i.id = :id")
    java.util.Optional<Item> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
}
