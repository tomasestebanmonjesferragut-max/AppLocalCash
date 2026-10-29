package com.example.mapalocalcash.model;

// Producto o servicio de un gasto compartido
public class Producto {
    private int id;
    private int compraId;
    private String nombre;
    private double precio;

    public Producto(String nombre, double precio) {
        this.nombre = nombre;
        this.precio = precio;
    }

    public Producto(int id, int compraId, String nombre, double precio) {
        this(nombre, precio);
        this.id = id;
        this.compraId = compraId;
    }

    public int getId() {
        return id;
    }

    public int getCompraId() {
        return compraId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }
}
