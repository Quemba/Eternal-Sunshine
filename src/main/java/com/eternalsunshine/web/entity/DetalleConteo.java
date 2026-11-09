package com.eternalsunshine.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "detalles_conteo")
public class DetalleConteo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conteo_id")
    private ConteoInventario conteo;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private Integer cantidadSistema;

    private Integer cantidadContada;

    private LocalDateTime fechaPrimerConteo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoLineaConteo estado = EstadoLineaConteo.PENDIENTE;

    private Integer stockAntesAjuste;

    private Integer stockDespuesAjuste;

    private LocalDateTime fechaAplicacion;

    public DetalleConteo() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public ConteoInventario getConteo() { return conteo; }
    public void setConteo(ConteoInventario conteo) { this.conteo = conteo; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Integer getCantidadSistema() { return cantidadSistema; }
    public void setCantidadSistema(Integer cantidadSistema) { this.cantidadSistema = cantidadSistema; }

    public Integer getCantidadContada() { return cantidadContada; }
    public void setCantidadContada(Integer cantidadContada) { this.cantidadContada = cantidadContada; }

    public LocalDateTime getFechaPrimerConteo() { return fechaPrimerConteo; }
    public void setFechaPrimerConteo(LocalDateTime fechaPrimerConteo) { this.fechaPrimerConteo = fechaPrimerConteo; }

    public EstadoLineaConteo getEstado() { return estado; }
    public void setEstado(EstadoLineaConteo estado) { this.estado = estado; }

    public Integer getStockAntesAjuste() { return stockAntesAjuste; }
    public void setStockAntesAjuste(Integer stockAntesAjuste) { this.stockAntesAjuste = stockAntesAjuste; }

    public Integer getStockDespuesAjuste() { return stockDespuesAjuste; }
    public void setStockDespuesAjuste(Integer stockDespuesAjuste) { this.stockDespuesAjuste = stockDespuesAjuste; }

    public LocalDateTime getFechaAplicacion() { return fechaAplicacion; }
    public void setFechaAplicacion(LocalDateTime fechaAplicacion) { this.fechaAplicacion = fechaAplicacion; }
}