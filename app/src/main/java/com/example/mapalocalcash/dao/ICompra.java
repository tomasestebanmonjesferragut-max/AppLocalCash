package com.example.mapalocalcash.dao;

import com.example.mapalocalcash.model.Compra;
import com.example.mapalocalcash.model.GastoCompartido;

import java.util.ArrayList;

public interface ICompra {
    int TODAS = 0;
    int PENDIENTES = 1;
    int COMPLETAS = 2;

    long crear(Compra compra);
    Compra buscar(int id);
    ArrayList<Compra> listar();
    boolean actualizar(int id, String descripcion, double monto);
    boolean marcarRealizada(int id);
    boolean eliminar(int id);
    ArrayList<GastoCompartido> listarGastosCompartidos(int filtro);
}
