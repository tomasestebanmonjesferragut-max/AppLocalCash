package com.example.mapalocalcash.dao;

import com.example.mapalocalcash.model.Cuenta;

import java.util.ArrayList;

public interface ICuenta {
    long crear(Cuenta cuenta);
    ArrayList<Cuenta> listar();
    boolean eliminar(int id);
    void cambiarSaldo(int id, double diferencia);
    double saldoTotal();
}
