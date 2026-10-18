package br.udesc.achados.ocorrencia.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "ocorrencia_historico")
public class OcorrenciaHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ocorrencia_id", nullable = false)
    private Ocorrencia ocorrencia;

    @Enumerated(EnumType.STRING)
    @Column
    private StatusOcorrencia de;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOcorrencia para;

    @Column(length = 500)
    private String motivo;

    @Column(nullable = false, length = 100)
    private String autor;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    public OcorrenciaHistorico() {
    }

    public OcorrenciaHistorico(Ocorrencia ocorrencia, StatusOcorrencia de, StatusOcorrencia para,
                                String motivo, String autor) {
        this.ocorrencia = ocorrencia;
        this.de = de;
        this.para = para;
        this.motivo = motivo;
        this.autor = autor;
    }

    @PrePersist
    public void prePersist() {
        this.criadoEm = Instant.now();
    }

    // Getters

    public Long getId() {
        return id;
    }

    public Ocorrencia getOcorrencia() {
        return ocorrencia;
    }

    public StatusOcorrencia getDe() {
        return de;
    }

    public StatusOcorrencia getPara() {
        return para;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getAutor() {
        return autor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
