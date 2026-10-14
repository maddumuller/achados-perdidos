package br.udesc.achados.ocorrencia.domain;

public enum StatusOcorrencia {
    AGUARDANDO_RESERVA, EM_ANALISE, APROVADA, DEVOLVIDA, REJEITADA, CANCELADA;

    public boolean podeIrPara(StatusOcorrencia destino) {
        return switch (this) {
            case AGUARDANDO_RESERVA -> destino == EM_ANALISE || destino == CANCELADA;
            case EM_ANALISE -> destino == APROVADA || destino == REJEITADA || destino == CANCELADA;
            case APROVADA -> destino == DEVOLVIDA || destino == CANCELADA;
            case DEVOLVIDA, REJEITADA, CANCELADA -> false;
        };
    }

    public boolean ativa() {
        return this == AGUARDANDO_RESERVA || this == EM_ANALISE || this == APROVADA;
    }
}
