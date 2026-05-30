package com.rastreador.gastos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rastreador.gastos.model.Expense;
import com.rastreador.gastos.model.ExpenseStore;
import com.rastreador.gastos.persistence.ExpenseRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Pruebas de la lógica principal del rastreador de gastos.
class ExpenseServiceTest {

    // Carpeta temporal aislada para archivos de exportación.
    @TempDir
    Path tempDir;

    // Cubre el flujo básico de crear, actualizar, resumir y borrar gastos.
    @Test
    void shouldAddUpdateDeleteAndSummarizeExpenses() {
        // Se usa un repositorio en memoria para no tocar disco.
        InMemoryExpenseRepository repository = new InMemoryExpenseRepository();
        // Se construye el servicio sobre el repositorio de pruebas.
        ExpenseService service = new ExpenseService(repository);

        // Se crea un gasto y se valida su ID secuencial.
        Expense added = service.addExpense("Lunch", new BigDecimal("20"), "food");
        assertEquals(1L, added.id());
        assertEquals(1, service.listExpenses(null).size());

        // Se actualiza el gasto y se comprueba el nuevo texto.
        Expense updated = service.updateExpense(1L, "Dinner", new BigDecimal("30"), "food");
        assertEquals("Dinner", updated.description());

        // El total debe reflejar el importe actualizado.
        ExpenseService.Summary summary = service.getSummary(null, null);
        assertEquals(new BigDecimal("30"), summary.total());

        // Tras el borrado no deben quedar elementos.
        service.deleteExpense(1L);
        assertTrue(service.listExpenses(null).isEmpty());
    }

    // Garantiza que un ID inexistente produzca un error claro.
    @Test
    void shouldRejectUnknownExpenseId() {
        InMemoryExpenseRepository repository = new InMemoryExpenseRepository();
        ExpenseService service = new ExpenseService(repository);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.deleteExpense(99L));

        assertEquals("No existe un gasto con ID 99", exception.getMessage());
    }

    // Verifica filtros por categoría, resumen mensual y advertencia de presupuesto.
    @Test
    void shouldFilterByMonthAndCategoryAndWarnOnBudget() {
        InMemoryExpenseRepository repository = new InMemoryExpenseRepository();
        ExpenseService service = new ExpenseService(repository);

        // Se usa el año actual para que el test no dependa del calendario fijo.
        int currentYear = LocalDate.now().getYear();

        // Se cargan dos gastos en categorías y meses distintos.
        repository.store.setExpenses(List.of(
            new Expense(1L, LocalDate.of(currentYear, 5, 1), "Lunch", new BigDecimal("20"), "food"),
            new Expense(2L, LocalDate.of(currentYear, 4, 1), "Book", new BigDecimal("15"), "study")
        ));
        // Se fija un presupuesto bajo para forzar la advertencia.
        repository.store.setMonthlyBudget(new BigDecimal("10"));
        // Se reconstruye el servicio para leer el estado cargado.
        service = new ExpenseService(repository);

        // El filtro por categoría debe devolver solo un gasto.
        assertEquals(1, service.listExpenses("food").size());
        // Sin filtro debe devolver ambos.
        assertEquals(2, service.listExpenses(null).size());
        // El resumen de mayo debe sumar solo el gasto de ese mes.
        assertEquals(new BigDecimal("20"), service.getSummary(5, null).total());
        // El resumen filtrado por categoría también debe sumar correctamente.
        assertEquals(new BigDecimal("20"), service.getSummary(null, "food").total());
        // Debe existir una advertencia por superar el presupuesto.
        assertTrue(service.getBudgetWarning().isPresent());
        assertTrue(service.getBudgetWarning().orElseThrow().contains("presupuesto"));

        // Al eliminar el presupuesto, la advertencia desaparece.
        service.clearMonthlyBudget();
        assertFalse(service.getBudgetWarning().isPresent());
    }

    // Verifica la exportación CSV con un archivo temporal aislado.
    @Test
    void shouldExportCsv() throws Exception {
        InMemoryExpenseRepository repository = new InMemoryExpenseRepository();
        ExpenseService service = new ExpenseService(repository);

        // Se usa el año actual para mantener coherencia con el filtro mensual.
        int currentYear = LocalDate.now().getYear();

        // Se carga un único gasto que debe aparecer en el CSV.
        repository.store.setExpenses(List.of(
            new Expense(1L, LocalDate.of(currentYear, 5, 1), "Lunch", new BigDecimal("20"), "food")
        ));
        // Se reconstruye el servicio para usar el estado preparado.
        service = new ExpenseService(repository);

        // El archivo sale en una carpeta temporal proporcionada por JUnit.
        Path tempFile = tempDir.resolve("expenses-test.csv");
        // Se exporta filtrando por mes y categoría.
        service.exportToCsv(tempFile, 5, "food");

        // Se valida que el CSV tenga encabezado y contenido esperado.
        List<String> lines = java.nio.file.Files.readAllLines(tempFile);
        assertEquals("id,date,description,amount,category", lines.get(0));
        assertTrue(lines.get(1).contains("Lunch"));
    }

    // Implementación mínima de repositorio en memoria para las pruebas.
    private static final class InMemoryExpenseRepository implements ExpenseRepository {
        // Estado interno de la prueba.
        private final ExpenseStore store = new ExpenseStore();

        // El servicio carga directamente desde este estado.
        @Override
        public ExpenseStore load() {
            return store;
        }

        // La persistencia solo copia datos al mismo contenedor de prueba.
        @Override
        public void save(ExpenseStore store) {
            this.store.setExpenses(store.expenses());
            this.store.setMonthlyBudget(store.monthlyBudget());
        }
    }
}