package br.edu.udesc.item.service;

import br.edu.udesc.item.model.Item;
import br.edu.udesc.item.model.StatusItem;
import br.edu.udesc.item.repository.ItemRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemReservationServiceTest {
    @Test
    void onlyTheOwningOccurrenceCanReleaseTheReservation() {
        var repository = mock(ItemRepository.class);
        var service = new ItemReservationService(repository);
        var item = new Item();
        item.setStatus(StatusItem.DISPONIVEL);
        when(repository.findLockedById(7L)).thenReturn(Optional.of(item));

        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertTrue(service.reserve(7L, first));
        assertTrue(service.reserve(7L, first));
        assertFalse(service.reserve(7L, second));

        service.finish(7L, second, StatusItem.DISPONIVEL);
        assertEquals(StatusItem.RESERVADO, item.getStatus());

        service.finish(7L, first, StatusItem.DISPONIVEL);
        assertEquals(StatusItem.DISPONIVEL, item.getStatus());
        assertNull(item.getReservaOcorrencia());
    }

    @Test
    void resolvesExistingItemIdAndItsOccurrenceReference() {
        assertEquals(42L, ItemReference.numericId("42"));
        assertEquals(42L, ItemReference.numericId(new UUID(0, 42).toString()));
    }
}
