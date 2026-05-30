package com.rastreador.gastos.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rastreador.gastos.model.Expense;
import com.rastreador.gastos.model.ExpenseStore;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Pruebas de lectura y escritura del archivo de almacenamiento.
class FileExpenseRepositoryTest {

    // Carpeta temporal aislada para no ensuciar el repositorio.
    @TempDir
    Path tempDir;

    // Verifica que guardar y volver a cargar preserve los datos.
    @Test
    void shouldSaveAndLoadExpenses() {
        // Se define el archivo temporal de persistencia.
        Path file = tempDir.resolve("expenses.txt");
        // Se crea el repositorio real apuntando al archivo temporal.
        FileExpenseRepository repository = new FileExpenseRepository(file);

        // Se arma un estado mínimo para guardar.
        ExpenseStore store = new ExpenseStore();
        store.setMonthlyBudget(new BigDecimal("100"));
        store.setExpenses(List.of(new Expense(1L, LocalDate.of(2026, 5, 30), "Lunch", new BigDecimal("20"), "food")));

        // Se escribe el estado en disco.
        repository.save(store);

        // Se carga de nuevo desde el mismo archivo.
        ExpenseStore loaded = repository.load();
        // Se comprueba que el presupuesto siga presente.
        assertEquals(new BigDecimal("100"), loaded.monthlyBudget());
        // Se verifica que el gasto siga existiendo.
        assertEquals(1, loaded.expenses().size());
        assertEquals("Lunch", loaded.expenses().get(0).description());
        // El archivo debe existir físicamente tras el guardado.
        assertTrue(Files.exists(file));
    }
}