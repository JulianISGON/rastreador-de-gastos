package com.rastreador.gastos.persistence;

import com.rastreador.gastos.model.ExpenseStore;

// Contrato para cargar y guardar el estado de gastos.
public interface ExpenseRepository {
    // Recupera el estado almacenado.
    ExpenseStore load();

    // Persiste el estado actual.
    void save(ExpenseStore store);
}