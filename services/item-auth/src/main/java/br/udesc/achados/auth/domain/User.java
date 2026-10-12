package br.udesc.achados.auth.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String nome;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private int tentativasFalhas = 0;

    @Column
    private Instant bloqueadoAte;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    public User() {
    }

    public User(String nome, String email, String senhaHash, Role role) {
        this.nome = nome;
        this.email = email != null ? email.toLowerCase().trim() : null;
        this.senhaHash = senhaHash;
        this.role = role != null ? role : Role.USER;
        this.ativo = true;
        this.tentativasFalhas = 0;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.criadoEm = now;
        this.atualizadoEm = now;
        if (this.email != null) {
            this.email = this.email.toLowerCase().trim();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = Instant.now();
        if (this.email != null) {
            this.email = this.email.toLowerCase().trim();
        }
    }

    public boolean isBloqueado(Instant agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    public void registrarTentativaFalha(Instant agora) {
        this.tentativasFalhas++;
        if (this.tentativasFalhas >= 5) {
            this.bloqueadoAte = agora.plus(15, ChronoUnit.MINUTES);
        }
    }

    public void zerarTentativasFalhas() {
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase().trim() : null;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public int getTentativasFalhas() {
        return tentativasFalhas;
    }

    public void setTentativasFalhas(int tentativasFalhas) {
        this.tentativasFalhas = tentativasFalhas;
    }

    public Instant getBloqueadoAte() {
        return bloqueadoAte;
    }

    public void setBloqueadoAte(Instant bloqueadoAte) {
        this.bloqueadoAte = bloqueadoAte;
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
