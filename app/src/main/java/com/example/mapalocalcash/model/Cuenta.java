package com.example.mapalocalcash.model;

// Cuenta de dinero (efectivo, ahorros, tarjeta) con su saldo actual
public class Cuenta {
    private int id;
    private String nombre;
    private double saldo;

    public Cuenta(String nombre, double saldo) {
        this.nombre = nombre;
        this.saldo = saldo;
    }

    public Cuenta(int id, String nombre, double saldo) {
        this(nombre, saldo);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public double getSaldo() {
        return saldo;
    }
}
