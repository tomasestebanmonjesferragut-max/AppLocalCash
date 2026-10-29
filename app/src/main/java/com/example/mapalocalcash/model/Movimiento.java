package com.example.mapalocalcash.model;

// Ingreso o egreso de una cuenta. lat y lon son null cuando no tiene ubicacion.
public class Movimiento {
    public static final String INGRESO = "Ingreso";
    public static final String EGRESO = "Egreso";

    private int id;
    private int cuentaId;
    private String tipo;
    private double monto;
    private String descripcion;
    private Double lat;
    private Double lon;
    private String cuentaNombre; // solo para mostrar

    public Movimiento(int cuentaId, String tipo, double monto, String descripcion, Double lat, Double lon) {
        this.cuentaId = cuentaId;
        this.tipo = tipo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.lat = lat;
        this.lon = lon;
    }

    public Movimiento(int id, int cuentaId, String tipo, double monto, String descripcion, Double lat, Double lon, String cuentaNombre) {
        this(cuentaId, tipo, monto, descripcion, lat, lon);
        this.id = id;
        this.cuentaNombre = cuentaNombre;
    }

    public boolean esIngreso() {
        return INGRESO.equals(tipo);
    }

    // Lo que suma (ingreso) o resta (egreso) al saldo de la cuenta
    public double efectoEnSaldo() {
        return esIngreso() ? monto : -monto;
    }

    public boolean tieneUbicacion() {
        return lat != null && lon != null;
    }

    public int getId() {
        return id;
    }

    public int getCuentaId() {
        return cuentaId;
    }

    public String getTipo() {
        return tipo;
    }

    public double getMonto() {
        return monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public String getCuentaNombre() {
        return cuentaNombre;
    }
}
