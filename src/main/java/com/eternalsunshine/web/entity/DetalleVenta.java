package com.eternalsunshine.web.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "detalle_ventas")
public class DetalleVenta extends EntidadDeTienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private Integer cantidad;

    private Integer precioUnitario;

    private Integer subtotal;

    public DetalleVenta() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public Integer getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Integer precioUnitario) { this.precioUnitario = precioUnitario; }

    public Integer getSubtotal() { return subtotal; }
    public void setSubtotal(Integer subtotal) { this.subtotal = subtotal; }
}