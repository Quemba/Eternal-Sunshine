package com.eternalsunshine.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "contadores_documento",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tipo", "periodo"}))
public class ContadorDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 20, nullable = false)
    private String tipo;

    @Column(length = 7, nullable = false)
    private String periodo;

    @Column(nullable = false)
    private Integer ultimo = 0;

    public ContadorDocumento() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public Integer getUltimo() { return ultimo; }
    public void setUltimo(Integer ultimo) { this.ultimo = ultimo; }
}