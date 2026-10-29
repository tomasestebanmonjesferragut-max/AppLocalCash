package com.example.mapalocalcash.model;

// Compra hecha o agendada. Un gasto compartido tambien es una compra (con productos y deudas).
public class Compra {
    private int id;
    private String descripcion;
    private double monto;
    private String fecha;
    private boolean agendada;
    private Integer contactoId;
    private String contactoNombre; // solo para mostrar

    public Compra(String descripcion, double monto, String fecha, boolean agendada, Integer contactoId) {
        this.descripcion = descripcion;
        this.monto = monto;
        this.fecha = fecha;
        this.agendada = agendada;
        this.contactoId = contactoId;
    }

    public Compra(int id, String descripcion, double monto, String fecha, boolean agendada, Integer contactoId, String contactoNombre) {
        this(descripcion, monto, fecha, agendada, contactoId);
        this.id = id;
        this.contactoNombre = contactoNombre;
    }

    public int getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public double getMonto() {
        return monto;
    }

    public String getFecha() {
        return fecha == null ? "" : fecha;
    }

    public boolean isAgendada() {
        return agendada;
    }

    public Integer getContactoId() {
        return contactoId;
    }

    public String getContactoNombre() {
        return contactoNombre;
    }
}
