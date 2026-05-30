package com.rastreador.gastos.service;

import com.rastreador.gastos.MoneyFormatter;
import com.rastreador.gastos.ConsoleText;
import com.rastreador.gastos.model.Expense;
import com.rastreador.gastos.model.ExpenseStore;
import com.rastreador.gastos.persistence.ExpenseRepository;
import com.rastreador.gastos.persistence.FileExpenseRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

// Contiene la lógica de negocio central del rastreador de gastos.
public final class ExpenseService {

    // Repositorio usado para cargar y guardar el estado.
    private final ExpenseRepository repository;
    // Estado actual en memoria.
    private ExpenseStore store;

    // Construye el servicio con el repositorio indicado.
    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
        this.store = repository.load();
    }

    // Crea la configuración por defecto que usa el archivo local del proyecto.
    public static ExpenseService createDefault() {
        return new ExpenseService(new FileExpenseRepository(Path.of("data", "expenses.txt")));
    }

    // Añade un gasto nuevo y lo persiste.
    public Expense addExpense(String description, BigDecimal amount, String category) {
        // Se construye el objeto validado.
        Expense expense = new Expense(nextId(), LocalDate.now(), normalize(description), amount, normalizeCategory(category));
        // Se agrega a la lista en memoria.
        store.expenses().add(expense);
        // Se guarda el nuevo estado.
        persist();
        return expense;
    }

    // Actualiza un gasto existente por ID.
    public Expense updateExpense(long id, String description, BigDecimal amount, String category) {
        // Se localiza el índice interno del gasto.
        int index = findIndexById(id);
        // Se toma la versión anterior para conservar campos no modificados.
        Expense existing = store.expenses().get(index);

        // Se crea una nueva versión con los cambios solicitados.
        Expense updated = new Expense(
                existing.id(),
                existing.date(),
                description == null ? existing.description() : normalize(description),
                amount == null ? existing.amount() : amount,
                category == null ? existing.category() : normalizeCategory(category)
        );

        // Se reemplaza el gasto anterior por el actualizado.
        store.expenses().set(index, updated);
        // Se persiste el cambio.
        persist();
        return updated;
    }

    // Elimina un gasto por su ID.
    public void deleteExpense(long id) {
        // Se encuentra la posición exacta en la lista.
        int index = findIndexById(id);
        // Se elimina el elemento encontrado.
        store.expenses().remove(index);
        // Se guarda el nuevo estado.
        persist();
    }

    // Devuelve la lista de gastos, opcionalmente filtrada por categoría.
    public List<Expense> listExpenses(String category) {
        return store.expenses().stream()
                .filter(expense -> category == null || matchesCategory(expense, category))
                .sorted(Comparator.comparing(Expense::date).thenComparingLong(Expense::id))
                .toList();
    }

    // Calcula un resumen total global o por mes y categoría.
    public Summary getSummary(Integer month, String category) {
        // Se usa el año actual para los resúmenes mensuales.
        LocalDate today = LocalDate.now();
        int year = today.getYear();

        // Se aplica el filtrado solicitado por mes y categoría.
        List<Expense> filtered = store.expenses().stream()
                .filter(expense -> month == null || (expense.date().getYear() == year && expense.date().getMonthValue() == month))
                .filter(expense -> category == null || matchesCategory(expense, category))
                .toList();

        // Se suman todos los importes del subconjunto filtrado.
        BigDecimal total = filtered.stream()
                .map(Expense::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Se devuelve el total y el nombre del mes si aplica.
        return new Summary(total, month == null ? null : ConsoleText.ascii(Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, Locale.of("es", "ES"))));
    }

    // Asigna un presupuesto mensual nuevo.
    public void setMonthlyBudget(BigDecimal amount) {
        store.setMonthlyBudget(amount);
        persist();
    }

    // Borra el presupuesto mensual.
    public void clearMonthlyBudget() {
        store.setMonthlyBudget(null);
        persist();
    }

    // Devuelve el presupuesto actual si existe.
    public BigDecimal getMonthlyBudget() {
        return store.monthlyBudget();
    }

    // Construye una advertencia si el gasto total supera el presupuesto.
    public Optional<String> getBudgetWarning() {
        BigDecimal budget = store.monthlyBudget();
        if (budget == null) {
            return Optional.empty();
        }

        // El total se calcula sobre todos los gastos actuales.
        BigDecimal total = getSummary(null, null).total();
        if (total.compareTo(budget) <= 0) {
            return Optional.empty();
        }

        // El mensaje explica claramente la diferencia.
        return Optional.of("Advertencia: has superado tu presupuesto mensual. Total actual: " + MoneyFormatter.format(total)
                + ", presupuesto: " + MoneyFormatter.format(budget));
    }

    // Exporta gastos a un archivo CSV opcionalmente filtrado.
    public void exportToCsv(Path outputFile, Integer month, String category) {
        // Se reutiliza el filtrado de listado y luego se limita por mes si es necesario.
        List<Expense> expenses = listExpenses(category).stream()
                .filter(expense -> month == null || (expense.date().getMonthValue() == month && expense.date().getYear() == LocalDate.now().getYear()))
                .toList();

        // Cada gasto se convierte en una línea CSV.
        List<String> lines = expenses.stream()
                .map(expense -> String.join(",",
                        String.valueOf(expense.id()),
                        expense.date().toString(),
                        csvEscape(expense.description()),
                        expense.amount().toPlainString(),
                        csvEscape(expense.categoryOrDefault())))
                .collect(Collectors.toList());

        // Se prepara el contenido completo con encabezado.
        List<String> content = new java.util.ArrayList<>();
        content.add("id,date,description,amount,category");
        content.addAll(lines);

        try {
            // Se crea la carpeta destino si hace falta.
            Files.createDirectories(Optional.ofNullable(outputFile.getParent()).orElse(Path.of(".")));
            // Se escribe el archivo final.
            Files.write(outputFile, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo exportar el archivo CSV", exception);
        }
    }

    // Escapa un valor para que sea seguro dentro de CSV.
    private String csvEscape(String value) {
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }

    // Persiste el estado actual usando el repositorio configurado.
    private void persist() {
        repository.save(store);
    }

    // Calcula el siguiente ID secuencial disponible.
    private long nextId() {
        return store.expenses().stream()
                .mapToLong(Expense::id)
                .max()
                .orElse(0L) + 1L;
    }

    // Busca el índice interno de un gasto por su ID.
    private int findIndexById(long id) {
        // Se recorre la lista hasta encontrar una coincidencia.
        for (int index = 0; index < store.expenses().size(); index++) {
            if (store.expenses().get(index).id() == id) {
                return index;
            }
        }

        // Si no existe, se informa con un error explícito.
        throw new IllegalArgumentException("No existe un gasto con ID " + id);
    }

    // Compara categorías ignorando mayúsculas y espacios irrelevantes.
    private boolean matchesCategory(Expense expense, String category) {
        return normalizeCategory(category).equalsIgnoreCase(normalizeCategory(expense.category()));
    }

    // Limpia y valida una descripción.
    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La descripción no puede estar vacía");
        }
        return value.trim();
    }

    // Limpia una categoría opcional, devolviendo null si no se indicó.
    private String normalizeCategory(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Resultado estructurado del resumen.
    public record Summary(BigDecimal total, String monthName) {
        // Devuelve el total ya formateado para consola.
        public String formattedTotal() {
            return MoneyFormatter.format(total);
        }
    }
}