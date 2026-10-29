package com.example.mapalocalcash.dao;

import com.example.mapalocalcash.model.Movimiento;

import java.util.ArrayList;

public interface IMovimiento {
    long crear(Movimiento movimiento);
    Movimiento buscar(int id);
    ArrayList<Movimiento> listarPorCuenta(int cuentaId);
    ArrayList<Movimiento> listarConUbicacion();
    boolean eliminar(int id);
    void eliminarPorCuenta(int cuentaId);
}
