package com.example.mapalocalcash.dao;

import com.example.mapalocalcash.model.Deuda;

import java.util.ArrayList;

public interface IDeuda {
    long crear(Deuda deuda);
    ArrayList<Deuda> listarPorCompra(int compraId);
    ArrayList<Deuda> pendientesDeContacto(int contactoId, String nombre);
    double totalPendiente();
    double totalPendienteDeContacto(int contactoId, String nombre);
    boolean marcarPagada(int id, boolean pagada);
    void eliminarPorCompra(int compraId);
    void eliminarManualesDeContacto(int contactoId);
    void desvincularContacto(int contactoId);
}
