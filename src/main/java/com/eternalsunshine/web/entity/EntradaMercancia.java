package com.eternalsunshine.web.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "entradas_mercancia")
public class EntradaMercancia extends EntidadDeTienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private Integer cantidad;

    private Integer stockAntes;

    private Integer stockDespues;

    @Column(length = 200)
    private String nota;

    @Column(length = 100)
    private String proveedor;

    @Column(length = 50)
    private String numeroFactura;

    private LocalDate fechaFactura;

    @Column(length = 7)
    private String periodo;

    private Integer consecutivo;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private LocalDateTime fecha;

    private Boolean anulada = false;

    @ManyToOne
    @JoinColumn(name = "usuario_anula_id")
    private Usuario usuarioAnula;

    private LocalDateTime fechaAnulacion;

    public EntradaMercancia() {
    }

    public String getNumero() {

        String codigo = TipoDocumento.ENTRADA.codigo(periodo, consecutivo);

        return codigo != null ? codigo : "-";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public Integer getStockAntes() { return stockAntes; }
    public void setStockAntes(Integer stockAntes) { this.stockAntes = stockAntes; }

    public Integer getStockDespues() { return stockDespues; }
    public void setStockDespues(Integer stockDespues) { this.stockDespues = stockDespues; }

    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public LocalDate getFechaFactura() { return fechaFactura; }
    public void setFechaFactura(LocalDate fechaFactura) { this.fechaFactura = fechaFactura; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public Integer getConsecutivo() { return consecutivo; }
    public void setConsecutivo(Integer consecutivo) { this.consecutivo = consecutivo; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Boolean getAnulada() { return anulada; }
    public void setAnulada(Boolean anulada) { this.anulada = anulada; }

    public Usuario getUsuarioAnula() { return usuarioAnula; }
    public void setUsuarioAnula(Usuario usuarioAnula) { this.usuarioAnula = usuarioAnula; }

    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }
}