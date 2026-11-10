package com.eternalsunshine.web.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conteos_inventario",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tienda_id", "periodo", "consecutivo"}))
public class ConteoInventario extends EntidadDeTienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EstadoConteo estado = EstadoConteo.EN_CURSO;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private LocalDateTime fechaInicio;

    private LocalDateTime fechaEnvio;

    private LocalDateTime fechaRevision;

    @ManyToOne
    @JoinColumn(name = "usuario_revisor_id")
    private Usuario usuarioRevisor;

    @Column(length = 7)
    private String periodo;

    private Integer consecutivo;

    private LocalDateTime fechaCancelacion;

    @ManyToOne
    @JoinColumn(name = "usuario_cancela_id")
    private Usuario usuarioCancela;

    @OneToMany(mappedBy = "conteo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetalleConteo> lineas = new ArrayList<>();

    public ConteoInventario() {
    }

    public String getNumero() {
        return TipoDocumento.CONTEO.codigo(periodo, consecutivo);
    }

    public String getEstadoTexto() {

        if (estado == null) {
            return "";
        }

        switch (estado) {
            case EN_CURSO:
                return "En curso";
            case PENDIENTE_REVISION:
                return "Por revisar";
            case CANCELADO:
                return "Cancelado";
            default:
                return "Revisado";
        }
    }

    public String getEstadoClase() {

        if (estado == null) {
            return "";
        }

        switch (estado) {
            case EN_CURSO:
                return "ec-curso";
            case PENDIENTE_REVISION:
                return "ec-revision";
            case CANCELADO:
                return "ec-cancelado";
            default:
                return "ec-revisado";
        }
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public EstadoConteo getEstado() { return estado; }
    public void setEstado(EstadoConteo estado) { this.estado = estado; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }

    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime fechaRevision) { this.fechaRevision = fechaRevision; }

    public Usuario getUsuarioRevisor() { return usuarioRevisor; }
    public void setUsuarioRevisor(Usuario usuarioRevisor) { this.usuarioRevisor = usuarioRevisor; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public Integer getConsecutivo() { return consecutivo; }
    public void setConsecutivo(Integer consecutivo) { this.consecutivo = consecutivo; }

    public LocalDateTime getFechaCancelacion() { return fechaCancelacion; }
    public void setFechaCancelacion(LocalDateTime fechaCancelacion) { this.fechaCancelacion = fechaCancelacion; }

    public Usuario getUsuarioCancela() { return usuarioCancela; }
    public void setUsuarioCancela(Usuario usuarioCancela) { this.usuarioCancela = usuarioCancela; }

    public List<DetalleConteo> getLineas() { return lineas; }
    public void setLineas(List<DetalleConteo> lineas) { this.lineas = lineas; }
}