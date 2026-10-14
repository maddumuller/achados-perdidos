package br.udesc.achados.ocorrencia.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemResumo(
        String id,
        String titulo,
        String nome,
        String tipo,
        String categoria,
        String status,
        String usuarioId
) {
    public String getTituloEfetivo() {
        if (titulo != null && !titulo.isBlank()) {
            return titulo;
        }
        if (nome != null && !nome.isBlank()) {
            return nome;
        }
        return "Item " + (id != null ? id : "");
    }

    public String getTipoEfetivo() {
        if (tipo != null && !tipo.isBlank()) {
            return tipo;
        }
        if (categoria != null && !categoria.isBlank()) {
            return categoria;
        }
        return "ACHADO";
    }

    public boolean isAberto() {
        return "ABERTO".equalsIgnoreCase(status);
    }
}
