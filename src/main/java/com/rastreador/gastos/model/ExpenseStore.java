package com.rastreador.gastos.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Contenedor en memoria con los gastos y el presupuesto mensual.
public final class ExpenseStore {

    // Lista editable de gastos cargados o creados.
    private List<Expense> expenses = new ArrayList<>();
    // Presupuesto mensual opcional.
    private BigDecimal monthlyBudget;

    // Devuelve la lista actual de gastos.
    public List<Expense> expenses() {
        return expenses;
    }

    // Sustituye la lista interna por una copia defensiva.
    public void setExpenses(List<Expense> expenses) {
        this.expenses = new ArrayList<>(expenses);
    }

    // Devuelve el presupuesto mensual si existe.
    public BigDecimal monthlyBudget() {
        return monthlyBudget;
    }

    // Asigna un nuevo presupuesto mensual.
    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }
}