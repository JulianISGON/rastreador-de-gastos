package com.rastreador.gastos.model;

import java.math.BigDecimal;
import java.time.LocalDate;

// Representa un gasto individual dentro del sistema.
public record Expense(
        long id,
        LocalDate date,
        String description,
        BigDecimal amount,
        String category
) {
    // Valida automáticamente los datos al crear una instancia.
    public Expense {
        // La descripción no puede ser vacía porque es la referencia visual del gasto.
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("La descripción no puede estar vacía");
        }
        // El importe debe ser positivo para evitar registros inválidos.
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El importe debe ser mayor que cero");
        }
        // La fecha siempre debe existir para poder resumir por periodos.
        if (date == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
    }

    // Devuelve una etiqueta legible cuando no se definió categoría.
    public String categoryOrDefault() {
        return category == null || category.isBlank() ? "Sin categoría" : category;
    }
}