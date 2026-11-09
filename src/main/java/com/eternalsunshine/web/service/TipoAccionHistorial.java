package com.eternalsunshine.web.service;

import java.util.Locale;

public enum TipoAccionHistorial {

    VENTA("venta", "Venta", "ti-receipt",
            "VENTA", "ANULÓ VENTA"),

    ENTRADA("entrada", "Entrada de mercancía", "ti-package-import",
            "ENTRADA", "ANULÓ ENTRADA"),

    STOCK("stock", "Corrección de stock", "ti-adjustments",
            "CORRIGIÓ STOCK"),

    PRODUCTO("producto", "Producto", "ti-package",
            "CREÓ PRODUCTO", "EDITÓ PRODUCTO", "ELIMINÓ PRODUCTO",
            "Agregó producto", "Editó producto", "Eliminó producto",
            "Actualizó producto", "Modificó producto"),

    USUARIO("usuario", "Usuario", "ti-user",
            "CREÓ USUARIO", "EDITÓ USUARIO", "ELIMINÓ USUARIO"),

    SOLICITUD("solicitud", "Solicitud", "ti-clipboard-check",
            "SOLICITÓ", "APROBÓ SOLICITUD", "RECHAZÓ SOLICITUD"),

    CONTEO("conteo", "Conteo de inventario", "ti-clipboard-list",
            "INICIÓ CONTEO", "ENVIÓ CONTEO", "REVISÓ CONTEO", "CANCELÓ CONTEO"),

    OTRO("otro", "Otro", "ti-point");

    private final String clave;
    private final String etiqueta;
    private final String icono;
    private final String[] prefijos;

    TipoAccionHistorial(String clave, String etiqueta, String icono, String... prefijos) {
        this.clave = clave;
        this.etiqueta = etiqueta;
        this.icono = icono;
        this.prefijos = prefijos;
    }

    public String getClave() { return clave; }
    public String getEtiqueta() { return etiqueta; }
    public String getIcono() { return icono; }
    public String[] getPrefijos() { return prefijos; }

    public static TipoAccionHistorial detectar(String accion) {

        if (accion == null) {
            return OTRO;
        }

        String texto = accion.trim().toUpperCase(Locale.ROOT);

        for (TipoAccionHistorial tipo : values()) {
            for (String prefijo : tipo.prefijos) {
                if (texto.startsWith(prefijo.toUpperCase(Locale.ROOT))) {
                    return tipo;
                }
            }
        }

        return OTRO;
    }

    public static TipoAccionHistorial porClave(String clave) {

        if (clave == null || clave.isEmpty()) {
            return null;
        }

        for (TipoAccionHistorial tipo : values()) {
            if (tipo.clave.equals(clave)) {
                return tipo;
            }
        }

        return null;
    }
}