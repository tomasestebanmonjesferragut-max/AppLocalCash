package com.example.mapalocalcash.model;

// Persona de la agenda con la que se comparten gastos
public class Contacto {
    private int id;
    private String nombre;
    private String telefono;

    public Contacto(String nombre, String telefono) {
        this.nombre = nombre;
        this.telefono = telefono;
    }

    public Contacto(int id, String nombre, String telefono) {
        this(nombre, telefono);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono == null ? "" : telefono;
    }
}
