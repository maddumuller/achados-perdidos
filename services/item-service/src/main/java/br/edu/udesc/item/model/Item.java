package br.edu.udesc.item.model;
import jakarta.persistence.*;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

    @Entity
    @Table(name = "itens")
    public class Item {
        public StatusItem getStatus() {
            return status;
        }

        public void setStatus(StatusItem status) {
            this.status = status;
        }

        public LocalDate getDataRegistro() {
            return dataRegistro;
        }

        public void setDataRegistro(LocalDate dataRegistro) {
            this.dataRegistro = dataRegistro;
        }

        public String getLocalizacao() {
            return localizacao;
        }

        public void setLocalizacao(String localizacao) {
            this.localizacao = localizacao;
        }

        public String getCategoria() {
            return categoria;
        }

        public void setCategoria(String categoria) {
            this.categoria = categoria;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }


        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        @Column(nullable = false)
        private String nome;

        @NotBlank(message = "A descrição é obrigatória")
        @Column(nullable = false)
        private String descricao;


        private String categoria;

        private String localizacao;

        private LocalDate dataRegistro;

        @Enumerated(EnumType.STRING)
        private StatusItem status;

        public Item() {
        }


    }

