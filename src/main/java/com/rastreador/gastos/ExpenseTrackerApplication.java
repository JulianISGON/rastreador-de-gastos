package com.rastreador.gastos;

import com.rastreador.gastos.model.Expense;
import com.rastreador.gastos.service.ExpenseService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

// Punto de entrada de la aplicación de consola.
public final class ExpenseTrackerApplication {

    // Servicio central que contiene la lógica de negocio.
    private final ExpenseService expenseService;

    // Constructor privado para forzar la creación desde main.
    private ExpenseTrackerApplication(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    // Método principal que arranca la aplicación desde la terminal.
    public static void main(String[] args) {
        // Se crea la configuración por defecto usando el repositorio local.
        ExpenseService expenseService = ExpenseService.createDefault();
        // Se construye la aplicación con su servicio principal.
        ExpenseTrackerApplication application = new ExpenseTrackerApplication(expenseService);
        // Se ejecuta el flujo de comandos y se usa su código de salida.
        int exitCode = application.run(args);
        // Se finaliza el proceso con el código calculado.
        System.exit(exitCode);
    }

    // Analiza el comando recibido y decide qué operación ejecutar.
    private int run(String[] args) {
        // Si no hay argumentos, se muestra ayuda y se devuelve error.
        if (args.length == 0) {
            printHelp();
            return 1;
        }

        try {
            // Se inspecciona el primer argumento para elegir el subcomando.
            return switch (args[0].toLowerCase()) {
                case "add" -> handleAdd(args);
                case "update" -> handleUpdate(args);
                case "delete" -> handleDelete(args);
                case "list" -> handleList(args);
                case "summary" -> handleSummary(args);
                case "budget" -> handleBudget(args);
                case "export" -> handleExport(args);
                // Las variantes de ayuda salen sin error.
                case "help", "--help", "-h" -> {
                    printHelp();
                    yield 0;
                }
                // Cualquier comando desconocido se reporta al usuario.
                default -> {
                    System.out.println("Comando no reconocido: " + args[0]);
                    printHelp();
                    yield 1;
                }
            };
        } catch (IllegalArgumentException exception) {
            // Los errores de validación se muestran como mensajes claros.
            System.out.println("Error: " + exception.getMessage());
            return 1;
        }
    }

    // Ejecuta el alta de un gasto nuevo.
    private int handleAdd(String[] args) {
        // Se leen los argumentos con formato --clave valor.
        ArgumentMap arguments = ArgumentMap.from(args, 1);
        // Se extrae la descripción obligatoria.
        String description = arguments.require("description");
        // Se extrae el importe obligatorio.
        BigDecimal amount = arguments.requireAmount("amount");
        // La categoría es opcional.
        String category = arguments.optional("category");

        // Se crea el gasto y se imprime su identificador.
        Expense expense = expenseService.addExpense(description, amount, category);
        System.out.printf("Gasto añadido correctamente (ID: %d)%n", expense.id());
        // Si hay presupuesto configurado, se muestra una advertencia si corresponde.
        printBudgetWarningIfNeeded();
        return 0;
    }

    // Ejecuta la actualización de un gasto existente.
    private int handleUpdate(String[] args) {
        // Se parsean los argumentos del comando.
        ArgumentMap arguments = ArgumentMap.from(args, 1);
        // El ID es obligatorio porque identifica el gasto a modificar.
        long id = arguments.requireLong("id");
        // Los campos restantes pueden omitirse si no se desean cambiar.
        String description = arguments.optional("description");
        BigDecimal amount = arguments.optionalAmount("amount");
        String category = arguments.optional("category");

        // Se aplica la actualización y se confirma la operación.
        expenseService.updateExpense(id, description, amount, category);
        System.out.println("Gasto actualizado correctamente");
        printBudgetWarningIfNeeded();
        return 0;
    }

    // Elimina un gasto por identificador.
    private int handleDelete(String[] args) {
        // Se leen los argumentos del borrado.
        ArgumentMap arguments = ArgumentMap.from(args, 1);
        // El ID es obligatorio para localizar el registro.
        long id = arguments.requireLong("id");

        // Se elimina el gasto y se informa el resultado.
        expenseService.deleteExpense(id);
        System.out.println("Gasto eliminado correctamente");
        return 0;
    }

    // Muestra todos los gastos o solo los de una categoría.
    private int handleList(String[] args) {
        // Se procesan los argumentos opcionales.
        ArgumentMap arguments = ArgumentMap.from(args, 1);
        // La categoría sirve como filtro si se proporciona.
        String category = arguments.optional("category");
        // Se obtiene la lista ordenada desde el servicio.
        List<Expense> expenses = expenseService.listExpenses(category);

        // Si no hay datos, se informa de forma clara.
        if (expenses.isEmpty()) {
            System.out.println("No hay gastos para mostrar");
            return 0;
        }

        // Se imprime una tabla sencilla por consola.
        printExpenses(expenses);
        return 0;
    }

    // Calcula el resumen general o mensual.
    private int handleSummary(String[] args) {
        // Se extraen los filtros opcionales.
        ArgumentMap arguments = ArgumentMap.from(args, 1);
        Integer month = arguments.optionalInt("month");
        String category = arguments.optional("category");

        // El mes debe estar dentro del rango natural del calendario.
        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }

        // Se calcula el total según los filtros aplicados.
        ExpenseService.Summary summary = expenseService.getSummary(month, category);
        // Si no hay mes, se presenta un total global.
        if (month == null) {
            System.out.printf("Total de gastos: %s%n", summary.formattedTotal());
            return 0;
        }

        // Si sí hay mes, se presenta el nombre del mes en español.
        System.out.printf("Total de gastos para %s: %s%n", summary.monthName(), summary.formattedTotal());
        return 0;
    }

    // Administra el presupuesto mensual.
    private int handleBudget(String[] args) {
        // Se espera una subacción válida dentro del comando budget.
        if (args.length < 2) {
            throw new IllegalArgumentException("Uso: budget set|show|clear");
        }

        // Se decide la acción concreta a ejecutar.
        return switch (args[1].toLowerCase()) {
            case "set" -> {
                // Se leen los valores del ajuste de presupuesto.
                ArgumentMap arguments = ArgumentMap.from(args, 2);
                BigDecimal amount = arguments.requireAmount("amount");
                // Se persiste el nuevo presupuesto.
                expenseService.setMonthlyBudget(amount);
                // Se confirma el valor guardado.
                System.out.printf("Presupuesto mensual establecido en %s%n", MoneyFormatter.format(amount));
                // Se advierte si ya se superó el límite.
                printBudgetWarningIfNeeded();
                yield 0;
            }
            case "show" -> {
                // Se consulta el presupuesto actual.
                BigDecimal budget = expenseService.getMonthlyBudget();
                if (budget == null) {
                    System.out.println("No hay presupuesto mensual configurado");
                } else {
                    // Si existe, se muestra formateado.
                    System.out.printf("Presupuesto mensual actual: %s%n", MoneyFormatter.format(budget));
                }
                yield 0;
            }
            case "clear" -> {
                // Se elimina el presupuesto guardado.
                expenseService.clearMonthlyBudget();
                System.out.println("Presupuesto mensual eliminado");
                yield 0;
            }
            // Si la subacción no existe, se explica el uso correcto.
            default -> throw new IllegalArgumentException("Uso: budget set|show|clear");
        };
    }

    // Exporta los gastos a un archivo CSV.
    private int handleExport(String[] args) {
        // Se parsean los argumentos de exportación.
        ArgumentMap arguments = ArgumentMap.from(args, 1);
        // El archivo destino es obligatorio.
        Path file = arguments.requirePath("file");
        // Filtros opcionales para limitar el contenido exportado.
        Integer month = arguments.optionalInt("month");
        String category = arguments.optional("category");

        // Se genera el CSV en el destino indicado.
        expenseService.exportToCsv(file, month, category);
        System.out.printf("Gastos exportados correctamente a %s%n", file.toAbsolutePath());
        return 0;
    }

    // Reutiliza la advertencia de presupuesto cuando corresponde.
    private void printBudgetWarningIfNeeded() {
        expenseService.getBudgetWarning().ifPresent(System.out::println);
    }

    // Imprime una tabla simple con los gastos obtenidos.
    private void printExpenses(List<Expense> expenses) {
        // Cabecera de la tabla.
        System.out.printf("%-5s %-12s %-24s %-12s %-16s%n", "ID", "Fecha", "Descripción", "Importe", "Categoría");
        // Cada gasto se imprime en una fila.
        for (Expense expense : expenses) {
            System.out.printf(
                    "%-5d %-12s %-24s %-12s %-16s%n",
                    expense.id(),
                    expense.date(),
                    truncate(expense.description(), 23),
                    MoneyFormatter.format(expense.amount()),
                    truncate(expense.categoryOrDefault(), 15)
            );
        }
    }

    // Evita que textos largos rompan la alineación de la tabla.
    private String truncate(String value, int maxLength) {
        // Si el texto cabe, se devuelve tal cual.
        if (value.length() <= maxLength) {
            return value;
        }
        // Si es más largo, se recorta y se añade un indicador visual.
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    // Muestra la ayuda general de comandos y notas de uso.
    private void printHelp() {
        System.out.println("Gestor de gastos - uso:");
        System.out.println("  expense-tracker add --description \"Comida\" --amount 20 [--category comida]");
        System.out.println("  expense-tracker update --id 1 [--description \"Nuevo texto\"] [--amount 25] [--category ocio]");
        System.out.println("  expense-tracker delete --id 1");
        System.out.println("  expense-tracker list [--category comida]");
        System.out.println("  expense-tracker summary [--month 8] [--category comida]");
        System.out.println("  expense-tracker budget set --amount 500");
        System.out.println("  expense-tracker budget show");
        System.out.println("  expense-tracker budget clear");
        System.out.println("  expense-tracker export --file gastos.csv [--month 8] [--category comida]");
        System.out.println();
        System.out.println("Notas:");
        System.out.println("  - El resumen mensual usa el año en curso.");
        System.out.println("  - Los importes deben ser mayores que cero.");
        System.out.println("  - Los gastos se guardan en data/expenses.txt.");
    }
}