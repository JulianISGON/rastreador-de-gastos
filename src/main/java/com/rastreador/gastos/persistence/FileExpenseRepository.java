package com.rastreador.gastos.persistence;

import com.rastreador.gastos.model.Expense;
import com.rastreador.gastos.model.ExpenseStore;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Implementación de persistencia basada en un archivo de texto local.
public final class FileExpenseRepository implements ExpenseRepository {

    // Separador lógico de cada campo serializado.
    private static final String SEPARATOR = "|";
    // Ruta del archivo donde se guarda el estado.
    private final Path storeFile;

    // Recibe la ubicación del archivo a usar.
    public FileExpenseRepository(Path storeFile) {
        this.storeFile = storeFile;
    }

    // Carga los datos desde disco si el archivo existe.
    @Override
    public ExpenseStore load() {
        // Se crea un contenedor vacío por defecto.
        ExpenseStore store = new ExpenseStore();

        // Si el archivo no existe, se devuelve el estado vacío.
        if (!Files.exists(storeFile)) {
            return store;
        }

        try {
            // Se leen todas las líneas del archivo.
            List<String> lines = Files.readAllLines(storeFile, StandardCharsets.UTF_8);
            // Se acumulan los gastos ya decodificados.
            List<Expense> expenses = new ArrayList<>();

            // Cada línea puede ser presupuesto o un gasto.
            for (String line : lines) {
                // Las líneas vacías se ignoran.
                if (line.isBlank()) {
                    continue;
                }

                // La línea de presupuesto se detecta por prefijo.
                if (line.startsWith("budget=")) {
                    String budgetValue = line.substring("budget=".length()).trim();
                    if (!budgetValue.isBlank()) {
                        store.setMonthlyBudget(new BigDecimal(budgetValue));
                    }
                    continue;
                }

                // El resto de líneas representan un gasto serializado.
                expenses.add(parseExpense(line));
            }

            // Se fija la lista final antes de devolver el estado.
            store.setExpenses(expenses);
            return store;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el archivo de gastos", exception);
        }
    }

    // Guarda el estado actual en disco.
    @Override
    public void save(ExpenseStore store) {
        try {
            // Se crea la carpeta destino si no existía.
            Files.createDirectories(Optional.ofNullable(storeFile.getParent()).orElse(Path.of(".")));

            // Se preparan las líneas a escribir.
            List<String> lines = new ArrayList<>();
            if (store.monthlyBudget() != null) {
                // El presupuesto se guarda en una línea especial.
                lines.add("budget=" + store.monthlyBudget().toPlainString());
            }

            // Cada gasto se serializa a una línea independiente.
            for (Expense expense : store.expenses()) {
                lines.add(serializeExpense(expense));
            }

            // Se sobrescribe el archivo con el estado actual.
            Files.write(storeFile, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo guardar el archivo de gastos", exception);
        }
    }

    // Convierte una línea de texto en un gasto real.
    private Expense parseExpense(String line) {
        // Se separan los campos serializados.
        String[] parts = line.split("\\|", -1);
        if (parts.length != 5) {
            throw new IllegalStateException("Formato inválido en el archivo de gastos");
        }

        // Se reconstruye el gasto con sus valores originales.
        return new Expense(
                Long.parseLong(parts[0]),
                LocalDate.parse(parts[1]),
                decodeText(parts[2]),
                new BigDecimal(parts[3]),
                decodeNullableText(parts[4])
        );
    }

    // Serializa un gasto para guardarlo en una sola línea.
    private String serializeExpense(Expense expense) {
        return expense.id()
                + SEPARATOR + expense.date()
                + SEPARATOR + encodeText(expense.description())
                + SEPARATOR + expense.amount().toPlainString()
                + SEPARATOR + encodeNullableText(expense.category());
    }

    // Codifica texto seguro para almacenamiento como base64 URL-safe.
    private String encodeText(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    // Codifica texto opcional, dejando vacío si no existe.
    private String encodeNullableText(String value) {
        return value == null ? "" : encodeText(value);
    }

    // Decodifica texto opcional almacenado en base64.
    private String decodeNullableText(String value) {
        if (value.isBlank()) {
            return null;
        }

        return decodeText(value);
    }

    // Intenta decodificar base64 y, si no puede, devuelve el texto tal cual.
    private String decodeText(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return value;
        }
    }
}