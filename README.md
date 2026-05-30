# Rastreador de gastos

Aplicación de consola desarrollada en Java 21 con Gradle para administrar gastos personales de forma simple, clara y extensible.

## Descripción

Este proyecto permite registrar gastos desde la terminal, listarlos, actualizarlos, eliminarlos y consultar resúmenes generales o mensuales.
También incluye soporte para categorías, presupuesto mensual y exportación a CSV.

La información se guarda en un archivo local dentro de `data/expenses.txt`, por lo que la aplicación no requiere base de datos ni servicios externos.

## Requisitos

- Java 21
- Gradle Wrapper incluido en el proyecto

## Estructura general

- `src/main/java`: código principal de la aplicación
- `src/test/java`: pruebas unitarias
- `gradle/wrapper`: configuración del wrapper de Gradle
- `data/`: almacenamiento local de gastos

## Ejecución

La forma recomendada de ejecutar el proyecto es mediante el wrapper incluido:

```bash
./gradlew.bat run --args="add --description \"Lunch\" --amount 20"
```

En sistemas Unix o macOS:

```bash
./gradlew run --args="add --description \"Lunch\" --amount 20"
```

## Comandos disponibles

### Crear un gasto

```bash
./gradlew.bat run --args="add --description \"Lunch\" --amount 20"
```

### Actualizar un gasto

```bash
./gradlew.bat run --args="update --id 1 --description \"Dinner\" --amount 25"
```

### Eliminar un gasto

```bash
./gradlew.bat run --args="delete --id 1"
```

### Ver todos los gastos

```bash
./gradlew.bat run --args="list"
```

### Ver resumen general

```bash
./gradlew.bat run --args="summary"
```

### Ver resumen por mes

```bash
./gradlew.bat run --args="summary --month 8"
```

### Configurar presupuesto mensual

```bash
./gradlew.bat run --args="budget set --amount 500"
```

### Ver presupuesto mensual

```bash
./gradlew.bat run --args="budget show"
```

### Exportar a CSV

```bash
./gradlew.bat run --args="export --file gastos.csv"
```

## Pruebas

Para ejecutar la suite de pruebas:

```bash
./gradlew.bat test
```

## Persistencia

Los gastos y el presupuesto se almacenan en `data/expenses.txt`.
El archivo se crea automáticamente la primera vez que agregas o modificas un gasto.

## Notas de uso

- Los importes deben ser mayores que cero.
- El resumen mensual usa siempre el año en curso.
- Si superas el presupuesto mensual configurado, la aplicación mostrará una advertencia.

## Proyecto extraído de roadmapsh

- https://roadmap.sh/projects/expense-tracker