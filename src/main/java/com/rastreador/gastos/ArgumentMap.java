package com.rastreador.gastos;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// Utilidad pequeña para leer argumentos con la forma --clave valor.
final class ArgumentMap {

    // Mapa interno con los valores ya parseados.
    private final Map<String, String> values;

    // Constructor privado para obligar al uso de la fábrica from.
    private ArgumentMap(Map<String, String> values) {
        this.values = values;
    }

    // Convierte un arreglo de argumentos en pares clave-valor.
    static ArgumentMap from(String[] args, int startIndex) {
        // Se usa un mapa mutable para ir almacenando cada valor.
        Map<String, String> values = new HashMap<>();

        // Se recorre la lista de argumentos desde la posición indicada.
        for (int index = startIndex; index < args.length; index++) {
            // Cada clave debe comenzar con dos guiones.
            String token = args[index];
            if (!token.startsWith("--")) {
                throw new IllegalArgumentException("Argumento no válido: " + token);
            }

            // Se normaliza el nombre de la clave.
            String key = token.substring(2).toLowerCase();
            if (key.isBlank()) {
                throw new IllegalArgumentException("Se esperaba un nombre de argumento después de --");
            }

            // Toda clave debe ir acompañada de un valor.
            if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                throw new IllegalArgumentException("Falta el valor para --" + key);
            }

            // Se almacena el par y se avanza una posición extra.
            values.put(key, args[++index]);
        }

        // Se devuelve una instancia lista para ser consultada.
        return new ArgumentMap(values);
    }

    // Recupera un valor obligatorio.
    String require(String name) {
        // Si el valor no existe o está vacío, se falla con un mensaje claro.
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta el argumento obligatorio --" + name);
        }
        return value;
    }

    // Recupera un valor opcional.
    String optional(String name) {
        // Si no existe o está en blanco, se devuelve null.
        String value = values.get(name);
        return value == null || value.isBlank() ? null : value;
    }

    // Convierte un valor obligatorio a long.
    long requireLong(String name) {
        return parseLong(require(name), name);
    }

    // Convierte un valor opcional a Integer.
    Integer optionalInt(String name) {
        String value = optional(name);
        if (value == null) {
            return null;
        }
        return (int) parseLong(value, name);
    }

    // Convierte un valor obligatorio a BigDecimal.
    BigDecimal requireAmount(String name) {
        return parseAmount(require(name), name);
    }

    // Convierte un valor opcional a BigDecimal.
    BigDecimal optionalAmount(String name) {
        String value = optional(name);
        return value == null ? null : parseAmount(value, name);
    }

    // Convierte un argumento obligatorio en una ruta de archivo.
    Path requirePath(String name) {
        return Path.of(require(name));
    }

    // Parsea números enteros largos de forma segura.
    private long parseLong(String value, String name) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El valor de --" + name + " debe ser numérico");
        }
    }

    // Parsea importes y garantiza que sean mayores que cero.
    private BigDecimal parseAmount(String value, String name) {
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El valor de --" + name + " debe ser mayor que cero");
            }
            return amount;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El valor de --" + name + " debe ser numérico");
        }
    }
}