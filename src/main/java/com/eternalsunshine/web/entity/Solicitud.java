package com.eternalsunshine.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes")
public class Solicitud extends EntidadDeTienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private AccionSolicitud accion;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private String codigo;
    private String nombre;
    private String categoria;
    private Integer precio;
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado;

    @ManyToOne
    @JoinColumn(name = "usuario_solicitante_id")
    private Usuario usuarioSolicitante;

    private LocalDateTime fechaSolicitud;

    private Boolean vistaPorSolicitante;

    public Solicitud() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public AccionSolicitud getAccion() { return accion; }
    public void setAccion(AccionSolicitud accion) { this.accion = accion; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public Integer getPrecio() { return precio; }
    public void setPrecio(Integer precio) { this.precio = precio; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }

    public Usuario getUsuarioSolicitante() { return usuarioSolicitante; }
    public void setUsuarioSolicitante(Usuario usuarioSolicitante) { this.usuarioSolicitante = usuarioSolicitante; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public Boolean getVistaPorSolicitante() { return vistaPorSolicitante; }
    public void setVistaPorSolicitante(Boolean vistaPorSolicitante) { this.vistaPorSolicitante = vistaPorSolicitante; }
}