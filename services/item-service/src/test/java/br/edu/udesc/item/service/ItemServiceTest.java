package br.edu.udesc.item.service;

import br.edu.udesc.item.model.Item;
import br.edu.udesc.item.repository.ItemRepository;
import br.edu.udesc.item.model.StatusItem;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository repository;

    @InjectMocks
    private ItemService service;

    @Test
    void deveListarTodosOsItens() {
        Item item = new Item();
        item.setNome("Garrafa azul");

        when(repository.findAll()).thenReturn(List.of(item));

        List<Item> resultado = service.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals("Garrafa azul", resultado.get(0).getNome());

        verify(repository).findAll();
    }
    @Test
    void deveCadastrarItemComoDisponivel() {
        Item item = new Item();
        item.setNome("Carteira");
        item.setDescricao("Encontrada no corredor");

        when(repository.save(any(Item.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Item resultado = service.cadastrar(item);

        assertEquals(StatusItem.DISPONIVEL, resultado.getStatus());
        assertNull(resultado.getId());

        verify(repository).save(item);
    }

    @Test
    void deveBuscarItemPorId() {
        Item item = new Item();
        item.setId(1L);
        item.setNome("Garrafa azul");

        when(repository.findById(1L))
                .thenReturn(Optional.of(item));

        Item resultado = service.buscarPorId(1L);

        assertEquals("Garrafa azul", resultado.getNome());
        verify(repository).findById(1L);
    }

    @Test
    void deveLancarExcecaoQuandoItemNaoExiste() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.buscarPorId(99L)
        );
    }

    @Test
    void deveAtualizarStatusDoItem() {
        Item item = new Item();
        item.setId(1L);
        item.setStatus(StatusItem.DISPONIVEL);

        when(repository.findById(1L))
                .thenReturn(Optional.of(item));

        when(repository.save(any(Item.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Item resultado = service.atualizarStatus(
                1L, StatusItem.RESERVADO
        );

        assertEquals(StatusItem.RESERVADO, resultado.getStatus());
        verify(repository).save(item);
    }
}
