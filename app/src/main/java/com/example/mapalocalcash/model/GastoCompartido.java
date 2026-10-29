package com.example.mapalocalcash.model;

// Resumen de un gasto compartido para la lista de operaciones
public class GastoCompartido {
    private int compraId;
    private String descripcion;
    private double total;
    private String fecha;
    private int personas;
    private int pagaron;
    private double faltaCobrar;

    public GastoCompartido(int compraId, String descripcion, double total, String fecha, int personas, int pagaron, double faltaCobrar) {
        this.compraId = compraId;
        this.descripcion = descripcion;
        this.total = total;
        this.fecha = fecha;
        this.personas = personas;
        this.pagaron = pagaron;
        this.faltaCobrar = faltaCobrar;
    }

    public boolean estaCompleto() {
        return pagaron == personas;
    }

    public int getCompraId() {
        return compraId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public double getTotal() {
        return total;
    }

    public String getFecha() {
        return fecha == null ? "" : fecha;
    }

    public int getPersonas() {
        return personas;
    }

    public int getPagaron() {
        return pagaron;
    }

    public double getFaltaCobrar() {
        return faltaCobrar;
    }
}
