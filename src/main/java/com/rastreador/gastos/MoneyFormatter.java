package com.rastreador.gastos;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Formatea importes para mostrarlos de forma legible en consola.
public final class MoneyFormatter {

    // Constructor privado porque esta clase solo ofrece funciones estáticas.
    private MoneyFormatter() {
    }

    // Convierte el importe a una representación con símbolo monetario.
    public static String format(BigDecimal amount) {
        // Se normaliza a dos decimales para asegurar una salida uniforme.
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        // Si el scale resulta negativo, se fuerza a cero para evitar formatos raros.
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0, RoundingMode.UNNECESSARY);
        }
        // Se antepone el símbolo de moneda.
        return "$" + normalized.toPlainString();
    }
}