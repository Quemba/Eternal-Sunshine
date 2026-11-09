package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.AprendizajeCategoria;
import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.repository.AprendizajeCategoriaRepository;
import com.eternalsunshine.web.repository.ProductoBusquedaRepository;
import com.eternalsunshine.web.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class CategoriaService {

    private static final Logger log = LoggerFactory.getLogger(CategoriaService.class);

    public static final String VALOR_OTRA = "__otro__";

    private static final List<String> BASE = Arrays.asList(
            "Bebidas",
            "Lácteos",
            "Panadería",
            "Salsas y condimentos",
            "Granos y cereales",
            "Carnes y embutidos",
            "Frutas y verduras",
            "Snacks y dulces",
            "Enlatados y conservas",
            "Aseo del hogar",
            "Cuidado personal",
            "Mascotas",
            "Varios");

    private static final Set<String> CONECTORES = new HashSet<>(Arrays.asList(
            "y", "e", "de", "del", "la", "el", "los", "las", "para", "con", "en"));

    private static final Map<String, Set<String>> PALABRAS = new LinkedHashMap<>();

    private static final Map<String, Set<String>> PALABRAS_RAIZ = new LinkedHashMap<>();

    private static final Set<String> GENERICAS = new HashSet<>(Arrays.asList(
            "del", "las", "los", "con", "sin", "para", "por", "una", "uno", "unos", "unas",
            "tipo", "original", "classic", "clasico", "clasica", "light", "zero", "premium",
            "natural", "familiar", "grande", "grandes", "pequeno", "mini", "extra", "super",
            "nuevo", "nueva", "sabor", "paquete", "pack", "unidades", "unidad", "litro", "litros",
            "gramos", "kilo", "kilos", "pet", "referencia"));

    static {
        palabras("Bebidas", "gaseosa", "cola", "coca", "pepsi", "sprite", "fanta", "postobon", "jugo",
                "agua", "cerveza", "aguardiente", "ron", "whisky", "vino", "energizante", "malta",
                "tampico", "hit", "gatorade", "bebida", "refresco", "limonada", "soda");

        palabras("Lácteos", "leche", "yogurt", "yogur", "queso", "mantequilla", "kumis", "huevo",
                "kefir", "lacteo");

        palabras("Panadería", "pan", "tajado", "arepa", "torta", "ponque", "croissant", "mogolla",
                "bizcocho", "pandebono", "bunuelo", "tostada", "almojabana");

        palabras("Salsas y condimentos", "salsa", "mayonesa", "ketchup", "mostaza", "vinagre", "aji",
                "sal", "sazon", "condimento", "ajo", "pimienta", "comino", "caldo", "consome",
                "aceite", "adobo", "oregano", "refisal", "tartara");

        palabras("Granos y cereales", "arroz", "frijol", "lenteja", "arveja", "garbanzo", "maiz",
                "avena", "cereal", "harina", "pasta", "espagueti", "fideo", "azucar", "panela", "cafe");

        palabras("Carnes y embutidos", "carne", "pollo", "cerdo", "res", "chorizo", "salchicha",
                "jamon", "salchichon", "tocino", "mortadela", "pescado", "costilla", "chuleta",
                "pechuga");

        palabras("Frutas y verduras", "manzana", "pera", "banano", "platano", "naranja", "limon",
                "tomate", "cebolla", "papa", "zanahoria", "lechuga", "aguacate", "fruta", "verdura",
                "mora", "fresa", "mango", "papaya", "pina", "uva", "cilantro");

        palabras("Snacks y dulces", "galleta", "chocolate", "dulce", "caramelo", "chicle", "bombon",
                "gomita", "mani", "snack", "chocolatina", "helado", "paleta", "chocorramo",
                "detodito", "doritos", "cheetos", "margarita", "chitos", "papas", "fritas", "barra");

        palabras("Enlatados y conservas", "atun", "sardina", "enlatado", "conserva", "champinon",
                "mermelada", "durazno", "lata");

        palabras("Aseo del hogar", "detergente", "suavizante", "cloro", "limpiador", "desinfectante",
                "lavaplatos", "fabuloso", "escoba", "trapero", "esponja", "blanqueador", "ambientador",
                "insecticida", "jabon", "basura", "limpia", "limpiavidrios");

        palabras("Cuidado personal", "shampoo", "champu", "acondicionador", "desodorante", "dental",
                "cepillo", "higienico", "panal", "protector", "afeitar", "rasuradora", "colonia",
                "perfume", "toalla", "tocador", "talco", "enjuague");

        palabras("Mascotas", "perro", "gato", "mascota", "purina", "dogui", "gatarina", "concentrado");

        palabras("Varios", "pila", "bateria", "bombillo", "vela", "fosforo", "cuaderno", "lapiz",
                "esfero", "papeleria", "cinta", "bolsa");

        for (Map.Entry<String, Set<String>> entrada : PALABRAS.entrySet()) {

            Set<String> raices = new HashSet<>();

            for (String palabra : entrada.getValue()) {
                raices.add(raiz(palabra));
            }

            PALABRAS_RAIZ.put(entrada.getKey(), raices);
        }
    }

    private static void palabras(String categoria, String... lista) {
        PALABRAS.put(categoria, new HashSet<>(Arrays.asList(lista)));
    }

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoBusquedaRepository busquedaRepository;

    @Autowired
    private AprendizajeCategoriaRepository aprendizajeRepository;

    public static class Sugerencia {

        private final String categoria;
        private final String motivo;

        public Sugerencia(String categoria, String motivo) {
            this.categoria = categoria;
            this.motivo = motivo;
        }

        public String getCategoria() { return categoria; }
        public String getMotivo() { return motivo; }
    }

    private static class FilaProducto {

        private final Producto producto;
        private final Set<String> palabras;

        private FilaProducto(Producto producto, Set<String> palabras) {
            this.producto = producto;
            this.palabras = palabras;
        }
    }

    private static String clave(String texto) {

        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
    }

    private static String raiz(String palabra) {

        String t = clave(palabra);

        if (t.length() > 3 && t.endsWith("s")) {
            return t.substring(0, t.length() - 1);
        }

        return t;
    }

    private static String claveFlexible(String texto) {

        String base = clave(texto).replaceAll("[^a-z0-9 ]", " ").trim().replaceAll("\\s+", " ");

        if (base.isEmpty()) {
            return "";
        }

        List<String> partes = new ArrayList<>();

        for (String parte : base.split(" ")) {

            if (CONECTORES.contains(parte)) {
                continue;
            }

            partes.add(raiz(parte));
        }

        return partes.isEmpty() ? base : String.join(" ", partes);
    }

    private List<String> tokens(String texto) {

        String limpio = clave(texto).replaceAll("[^a-z0-9]+", " ").trim();
        List<String> resultado = new ArrayList<>();

        if (limpio.isEmpty()) {
            return resultado;
        }

        for (String t : limpio.split(" ")) {

            if (t.length() < 3 || GENERICAS.contains(t) || t.matches(".*\\d.*")) {
                continue;
            }

            if (!resultado.contains(t)) {
                resultado.add(t);
            }
        }

        return resultado;
    }

    public List<String> opciones() {

        Map<String, String> mapa = new LinkedHashMap<>();

        for (String base : BASE) {
            mapa.putIfAbsent(claveFlexible(base), base);
        }

        Map<String, String> extras = new TreeMap<>();

        for (String categoria : busquedaRepository.categoriasDistintas()) {

            String limpia = limpiar(categoria);

            if (limpia.isEmpty()) {
                continue;
            }

            String k = claveFlexible(limpia);

            if (!mapa.containsKey(k)) {
                extras.putIfAbsent(k, limpia);
            }
        }

        for (Map.Entry<String, String> extra : extras.entrySet()) {
            mapa.putIfAbsent(extra.getKey(), extra.getValue());
        }

        return new ArrayList<>(mapa.values());
    }

    private Map<String, String> mapaOpciones() {

        Map<String, String> mapa = new LinkedHashMap<>();

        for (String opcion : opciones()) {
            mapa.putIfAbsent(claveFlexible(opcion), opcion);
        }

        return mapa;
    }

    public String existente(String texto) {

        String limpio = limpiar(texto);

        if (limpio.isEmpty()) {
            return null;
        }

        return mapaOpciones().get(claveFlexible(limpio));
    }

    public String canonica(String texto) {

        String limpio = limpiar(texto);

        if (limpio.isEmpty()) {
            return "";
        }

        String encontrada = existente(limpio);

        if (encontrada != null) {
            return encontrada;
        }

        return limpio.substring(0, 1).toUpperCase(Locale.ROOT) + limpio.substring(1);
    }

    public Sugerencia sugerir(String nombre) {
        return sugerir(nombre, null);
    }

    public Sugerencia sugerir(String nombre, Integer excluirId) {

        List<String> nuevos = tokens(nombre);

        if (nuevos.isEmpty()) {
            return null;
        }

        Map<String, String> validas = mapaOpciones();

        List<FilaProducto> filas = new ArrayList<>();
        Map<String, Integer> frecuencia = new HashMap<>();

        for (Producto p : productoRepository.findAll()) {

            if (excluirId != null && excluirId.equals(p.getId())) {
                continue;
            }

            Set<String> set = new LinkedHashSet<>(tokens(p.getNombre()));
            filas.add(new FilaProducto(p, set));

            for (String t : set) {
                frecuencia.merge(t, 1, Integer::sum);
            }
        }

        Producto mejor = null;
        int mejorPuntaje = 0;

        for (FilaProducto fila : filas) {

            if (limpiar(fila.producto.getCategoria()).isEmpty()) {
                continue;
            }

            int puntaje = 0;

            for (String t : nuevos) {
                if (fila.palabras.contains(t) && t.length() >= 4 && frecuencia.getOrDefault(t, 0) <= 2) {
                    puntaje++;
                }
            }

            if (puntaje > mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = fila.producto;
            }
        }

        if (mejor != null) {
            return new Sugerencia(canonica(mejor.getCategoria()),
                    "se parece a «" + mejor.getNombre() + "»");
        }

        Set<String> raices = new LinkedHashSet<>();

        for (String t : nuevos) {
            raices.add(raiz(t));
        }

        Map<String, List<AprendizajeCategoria>> aprendido = new HashMap<>();

        try {
            for (AprendizajeCategoria a : aprendizajeRepository.findByPalabraIn(raices)) {
                aprendido.computeIfAbsent(a.getPalabra(), k -> new ArrayList<>()).add(a);
            }
        } catch (Exception e) {
            log.warn("No se pudo leer lo aprendido; se usan solo las palabras clave: {}", e.toString());
            aprendido.clear();
        }

        Map<String, Map<String, double[]>> aportes = new HashMap<>();

        for (String t : nuevos) {

            String r = raiz(t);

            List<AprendizajeCategoria> lista = aprendido.getOrDefault(r, Collections.emptyList());

            int suma = 0;

            for (AprendizajeCategoria a : lista) {
                suma += nz(a.getVeces());
            }

            for (AprendizajeCategoria a : lista) {

                String categoria = validas.get(claveFlexible(a.getCategoria()));

                if (categoria == null || suma <= 0) {
                    continue;
                }

                int veces = nz(a.getVeces());
                double peso = Math.min(1.5, 0.7 * veces) * ((double) veces / suma);

                aporte(aportes, categoria, t)[0] += peso;
            }

            for (Map.Entry<String, Set<String>> base : PALABRAS_RAIZ.entrySet()) {

                if (base.getValue().contains(r)) {

                    String categoria = validas.get(claveFlexible(base.getKey()));

                    if (categoria != null) {
                        aporte(aportes, categoria, t)[1] += 1.0;
                    }
                }
            }
        }

        String mejorCategoria = null;
        String mejorPalabra = null;
        boolean mejorEsAprendida = false;
        double mejorTotal = 0;
        double segundoTotal = 0;

        for (Map.Entry<String, Map<String, double[]>> entrada : aportes.entrySet()) {

            double total = 0;
            double maximo = 0;
            String palabraTop = null;
            boolean aprendidaTop = false;

            for (Map.Entry<String, double[]> palabra : entrada.getValue().entrySet()) {

                double suma = palabra.getValue()[0] + palabra.getValue()[1];
                total += suma;

                if (suma > maximo) {
                    maximo = suma;
                    palabraTop = palabra.getKey();
                    aprendidaTop = palabra.getValue()[0] >= palabra.getValue()[1];
                }
            }

            if (total > mejorTotal) {
                segundoTotal = mejorTotal;
                mejorTotal = total;
                mejorCategoria = entrada.getKey();
                mejorPalabra = palabraTop;
                mejorEsAprendida = aprendidaTop;

            } else if (total > segundoTotal) {
                segundoTotal = total;
            }
        }

        if (mejorCategoria != null && mejorTotal >= 0.65 && (mejorTotal - segundoTotal) >= 0.25) {

            String motivo = mejorEsAprendida
                    ? "según tus productos anteriores con «" + mejorPalabra + "»"
                    : "por la palabra «" + mejorPalabra + "»";

            return new Sugerencia(mejorCategoria, motivo);
        }

        return null;
    }

    private int nz(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private double[] aporte(Map<String, Map<String, double[]>> aportes, String categoria, String palabra) {
        return aportes
                .computeIfAbsent(categoria, k -> new HashMap<>())
                .computeIfAbsent(palabra, k -> new double[2]);
    }

    @Transactional
    public void aprender(String nombre, String categoria, Integer productoId) {

        String elegida = canonica(categoria);

        if (elegida.isEmpty()) {
            return;
        }

        Sugerencia previa = sugerir(nombre, productoId);

        boolean acierto = previa != null
                && claveFlexible(previa.getCategoria()).equals(claveFlexible(elegida));

        registrarVotos(nombre, elegida, acierto ? 1 : 2);
    }

    private void registrarVotos(String nombre, String categoria, int peso) {

        Set<String> raices = new LinkedHashSet<>();

        for (String t : tokens(nombre)) {

            String r = raiz(t);

            if (r.length() >= 3 && r.length() <= 60) {
                raices.add(r);
            }
        }

        for (String r : raices) {

            AprendizajeCategoria fila = aprendizajeRepository.findByPalabraAndCategoria(r, categoria)
                    .orElseGet(() -> {
                        AprendizajeCategoria nueva = new AprendizajeCategoria();
                        nueva.setPalabra(r);
                        nueva.setCategoria(categoria);
                        nueva.setVeces(0);
                        return nueva;
                    });

            fila.setVeces(nz(fila.getVeces()) + peso);
            aprendizajeRepository.save(fila);
        }
    }

    @Transactional
    public int sembrarAprendizajeSiVacio() {

        if (aprendizajeRepository.count() > 0) {
            return 0;
        }

        int total = 0;

        for (Producto p : productoRepository.findAll()) {

            String categoria = canonica(p.getCategoria());

            if (categoria.isEmpty()) {
                continue;
            }

            registrarVotos(p.getNombre(), categoria, 1);
            total++;
        }

        return total;
    }

    @Transactional
    public int unificarExistentes() {

        List<Producto> cambiados = new ArrayList<>();

        for (Producto p : productoRepository.findAll()) {

            String actual = limpiar(p.getCategoria());

            if (actual.isEmpty()) {
                continue;
            }

            String buscada = claveFlexible(actual);

            for (String base : BASE) {
                if (claveFlexible(base).equals(buscada)) {

                    if (!base.equals(p.getCategoria())) {
                        p.setCategoria(base);
                        cambiados.add(p);
                    }

                    break;
                }
            }
        }

        productoRepository.saveAll(cambiados);

        return cambiados.size();
    }
}