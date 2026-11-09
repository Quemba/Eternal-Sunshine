package com.eternalsunshine.web.service;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class PaginaVista<T> {

    public static class ItemPagina {

        private final int numero;
        private final boolean puntos;
        private final boolean actual;

        public ItemPagina(int numero, boolean puntos, boolean actual) {
            this.numero = numero;
            this.puntos = puntos;
            this.actual = actual;
        }

        public int getNumero() { return numero; }
        public boolean isPuntos() { return puntos; }
        public boolean isActual() { return actual; }
    }

    private final List<T> filas;
    private final int pagina;
    private final int totalPaginas;
    private final long total;
    private final int desdeRegistro;
    private final int hastaRegistro;
    private final List<ItemPagina> items;

    public PaginaVista(List<T> filas, int pagina, int tamano, int totalPaginas, long total) {
        this.filas = filas;
        this.pagina = pagina;
        this.totalPaginas = totalPaginas;
        this.total = total;
        this.desdeRegistro = filas.isEmpty() ? 0 : (pagina - 1) * tamano + 1;
        this.hastaRegistro = filas.isEmpty() ? 0 : desdeRegistro + filas.size() - 1;
        this.items = totalPaginas > 0 ? construirItems(pagina, totalPaginas) : new ArrayList<>();
    }

    private static List<ItemPagina> construirItems(int actual, int total) {

        TreeSet<Integer> visibles = new TreeSet<>();

        visibles.add(1);
        visibles.add(total);

        for (int n = actual - 2; n <= actual + 2; n++) {
            if (n >= 1 && n <= total) {
                visibles.add(n);
            }
        }

        List<ItemPagina> items = new ArrayList<>();
        int anterior = 0;

        for (Integer numero : visibles) {
            if (anterior != 0 && numero - anterior > 1) {
                items.add(new ItemPagina(0, true, false));
            }

            items.add(new ItemPagina(numero, false, numero == actual));
            anterior = numero;
        }

        return items;
    }

    public List<T> getFilas() { return filas; }
    public int getPagina() { return pagina; }
    public int getTotalPaginas() { return totalPaginas; }
    public long getTotal() { return total; }
    public int getDesdeRegistro() { return desdeRegistro; }
    public int getHastaRegistro() { return hastaRegistro; }
    public List<ItemPagina> getItems() { return items; }
}