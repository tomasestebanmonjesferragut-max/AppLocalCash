package com.example.mapalocalcash.model;

// Lo que una persona debe: su parte de un gasto compartido (compraId) o un monto anotado a mano (detalle)
public class Deuda {
    private int id;
    private Integer compraId;
    private Integer contactoId;
    private String nombre;
    private double monto;
    private boolean pagada;
    private String detalle;
    private String origen; // solo para mostrar: descripcion de la compra o el detalle

    public Deuda(Integer compraId, Integer contactoId, String nombre, double monto, boolean pagada, String detalle) {
        this.compraId = compraId;
        this.contactoId = contactoId;
        this.nombre = nombre;
        this.monto = monto;
        this.pagada = pagada;
        this.detalle = detalle;
    }

    public Deuda(int id, Integer compraId, Integer contactoId, String nombre, double monto, boolean pagada, String detalle, String origen) {
        this(compraId, contactoId, nombre, monto, pagada, detalle);
        this.id = id;
        this.origen = origen;
    }

    public int getId() {
        return id;
    }

    public Integer getCompraId() {
        return compraId;
    }

    public Integer getContactoId() {
        return contactoId;
    }

    public String getNombre() {
        return nombre;
    }

    public double getMonto() {
        return monto;
    }

    public boolean isPagada() {
        return pagada;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getOrigen() {
        return origen;
    }
}
