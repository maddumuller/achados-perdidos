package br.udesc.achados.ocorrencia.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ocorrencias")
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID itemId;

    @Column(nullable = false, length = 200)
    private String itemTitulo;

    @Column(nullable = false, length = 30)
    private String itemTipo;

    @Column(nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 1000)
    private String justificativa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOcorrencia status;

    @Column(length = 500)
    private String observacaoAdmin;

    @Column(length = 500)
    private String motivoFinal;

    private UUID analisadoPor;

    @Version
    private long versao;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    public Ocorrencia() {
    }

    public Ocorrencia(UUID itemId, String itemTitulo, String itemTipo, UUID usuarioId, String justificativa) {
        this.itemId = itemId;
        this.itemTitulo = itemTitulo;
        this.itemTipo = itemTipo;
        this.usuarioId = usuarioId;
        this.justificativa = justificativa;
        this.status = StatusOcorrencia.AGUARDANDO_RESERVA;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.criadoEm = now;
        this.atualizadoEm = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = Instant.now();
    }

    // Getters e Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public String getItemTitulo() {
        return itemTitulo;
    }

    public void setItemTitulo(String itemTitulo) {
        this.itemTitulo = itemTitulo;
    }

    public String getItemTipo() {
        return itemTipo;
    }

    public void setItemTipo(String itemTipo) {
        this.itemTipo = itemTipo;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public StatusOcorrencia getStatus() {
        return status;
    }

    public void setStatus(StatusOcorrencia status) {
        this.status = status;
    }

    public String getObservacaoAdmin() {
        return observacaoAdmin;
    }

    public void setObservacaoAdmin(String observacaoAdmin) {
        this.observacaoAdmin = observacaoAdmin;
    }

    public String getMotivoFinal() {
        return motivoFinal;
    }

    public void setMotivoFinal(String motivoFinal) {
        this.motivoFinal = motivoFinal;
    }

    public UUID getAnalisadoPor() {
        return analisadoPor;
    }

    public void setAnalisadoPor(UUID analisadoPor) {
        this.analisadoPor = analisadoPor;
    }

    public long getVersao() {
        return versao;
    }

    public void setVersao(long versao) {
        this.versao = versao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
