package com.eternalsunshine.web.entity;

public enum TipoDocumento {

    VENTA("VEN"),
    ENTRADA("ENT"),
    CONTEO("CON");

    private final String prefijo;

    TipoDocumento(String prefijo) {
        this.prefijo = prefijo;
    }

    public String getPrefijo() {
        return prefijo;
    }

    public String codigo(String periodo, Integer consecutivo) {

        if (periodo == null || consecutivo == null) {
            return null;
        }

        return prefijo + "-" + periodo + "-" + String.format("%04d", consecutivo);
    }
}