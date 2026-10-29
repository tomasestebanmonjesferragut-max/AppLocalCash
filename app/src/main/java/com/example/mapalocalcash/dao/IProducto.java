package com.example.mapalocalcash.dao;

import com.example.mapalocalcash.model.Producto;

import java.util.ArrayList;

public interface IProducto {
    long crear(int compraId, Producto producto);
    ArrayList<Producto> listarPorCompra(int compraId);
    void eliminarPorCompra(int compraId);
}
