package com.rastreador.gastos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

// Pruebas del parser simple de argumentos de la CLI.
class ArgumentMapTest {

    // Verifica que las claves y valores se interpreten correctamente.
    @Test
    void shouldParseRequiredValues() {
        ArgumentMap map = ArgumentMap.from(new String[] {"add", "--description", "Lunch", "--amount", "20", "--category", "food"}, 1);

        assertEquals("Lunch", map.require("description"));
        assertEquals(new BigDecimal("20"), map.requireAmount("amount"));
        assertEquals("food", map.optional("category"));
    }

    // Verifica que un importe inválido genere un mensaje claro.
    @Test
    void shouldFailWhenAmountIsInvalid() {
        ArgumentMap map = ArgumentMap.from(new String[] {"add", "--description", "Lunch", "--amount", "-10"}, 1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> map.requireAmount("amount"));

        assertEquals("El valor de --amount debe ser mayor que cero", exception.getMessage());
    }
}