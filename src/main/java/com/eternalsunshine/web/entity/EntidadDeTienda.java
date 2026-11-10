package com.eternalsunshine.web.entity;

import com.eternalsunshine.web.config.AccesoEntreTiendasException;
import com.eternalsunshine.web.config.TiendaContexto;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.TenantId;

/**
 * Base de todas las tablas que pertenecen a una tienda.
 *
 * Hibernate agrega solo, a cada consulta, la condición "tienda_id = tienda de la sesión"
 * y llena el campo al guardar. Como las búsquedas directas por id no siempre pasan por ese
 * filtro, además se comprueba al cargar cada registro que sea de la tienda actual.
 */
@MappedSuperclass
public abstract class EntidadDeTienda {

    @TenantId
    @Column(name = "tienda_id", updatable = false)
    private Integer tiendaId;

    public Integer getTiendaId() {
        return tiendaId;
    }

    @PostLoad
    void verificarTiendaAlCargar() {

        Integer actual = TiendaContexto.actual();

        if (tiendaId == null || !tiendaId.equals(actual)) {
            throw new AccesoEntreTiendasException(
                    "Un registro de otra tienda fue solicitado desde la tienda " + actual + ".");
        }
    }

    @PrePersist
    void exigirTiendaAlGuardar() {

        if (TiendaContexto.SIN_TIENDA.equals(TiendaContexto.actual())) {
            throw new IllegalStateException("No se puede guardar información sin una tienda activa.");
        }
    }
}
