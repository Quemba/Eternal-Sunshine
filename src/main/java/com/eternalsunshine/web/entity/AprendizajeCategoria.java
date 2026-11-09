package com.eternalsunshine.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "aprendizaje_categoria",
        uniqueConstraints = @UniqueConstraint(columnNames = {"palabra", "categoria"}))
public class AprendizajeCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 60, nullable = false)
    private String palabra;

    @Column(length = 100, nullable = false)
    private String categoria;

    @Column(nullable = false)
    private Integer veces = 0;

    public AprendizajeCategoria() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getPalabra() { return palabra; }
    public void setPalabra(String palabra) { this.palabra = palabra; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public Integer getVeces() { return veces; }
    public void setVeces(Integer veces) { this.veces = veces; }
}