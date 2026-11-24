package br.edu.udesc.item.service;

import br.edu.udesc.item.model.StatusItem;
import br.edu.udesc.item.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class ItemReservationService {
    private final ItemRepository repository;

    public ItemReservationService(ItemRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean reserve(long id, UUID occurrence) {
        var item = repository.findLockedById(id).orElse(null);
        if (item == null) {
            return false;
        }
        if (item.getStatus() == StatusItem.RESERVADO) {
            return Objects.equals(occurrence, item.getReservaOcorrencia());
        }
        if (item.getStatus() != StatusItem.DISPONIVEL) {
            return false;
        }
        item.setReservaOcorrencia(occurrence);
        item.setStatus(StatusItem.RESERVADO);
        return true;
    }

    @Transactional
    public void finish(long id, UUID occurrence, StatusItem status) {
        repository.findLockedById(id).filter(item ->
                item.getStatus() == StatusItem.RESERVADO &&
                Objects.equals(occurrence, item.getReservaOcorrencia())
        ).ifPresent(item -> {
            item.setReservaOcorrencia(null);
            item.setStatus(status);
        });
    }
}
