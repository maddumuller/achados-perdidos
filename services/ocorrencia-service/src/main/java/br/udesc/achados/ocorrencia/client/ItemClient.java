package br.udesc.achados.ocorrencia.client;

import br.udesc.achados.ocorrencia.exception.NotFoundException;
import br.udesc.achados.ocorrencia.exception.ServiceUnavailableException;
import br.udesc.achados.ocorrencia.web.CurrentUser;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class ItemClient {

    private final RestClient restClient;

    public ItemClient(RestClient itemServiceRestClient) {
        this.restClient = itemServiceRestClient;
    }

    public ItemResumo buscar(UUID itemId, CurrentUser user, String correlationId) {
        try {
            return restClient.get()
                    .uri("/api/itens/{id}", itemId)
                    .header("X-User-Id", user.id().toString())
                    .header("X-User-Roles", user.rolesAsHeader())
                    .header("X-Correlation-Id", correlationId != null ? correlationId : "")
                    .retrieve()
                    .onStatus(s -> s.value() == 404,
                            (rq, rs) -> { throw new NotFoundException("Item não encontrado"); })
                    .body(ItemResumo.class);
        } catch (NotFoundException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException("item-service indisponível");
        } catch (Exception e) {
            throw new ServiceUnavailableException("item-service indisponível");
        }
    }
}
