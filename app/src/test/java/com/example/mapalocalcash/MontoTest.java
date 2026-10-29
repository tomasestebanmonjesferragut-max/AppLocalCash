package com.example.mapalocalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

// Pruebas de la lectura y el formato de montos
public class MontoTest {

    private static final double DELTA = 0.001;

    @Test
    public void leer_numeroSimple() {
        assertEquals(110000, Monto.leer("110000"), DELTA);
    }

    @Test
    public void leer_conPuntosDeMiles() {
        assertEquals(110000, Monto.leer("110.000"), DELTA);
        assertEquals(1250000, Monto.leer("1.250.000"), DELTA);
    }

    @Test
    public void leer_conSignoPesoYEspacios() {
        assertEquals(20000, Monto.leer(" $ 20.000 "), DELTA);
    }

    @Test
    public void leer_conComaDecimal() {
        assertEquals(1500.5, Monto.leer("1.500,5"), DELTA);
    }

    @Test
    public void leer_textoInvalidoDevuelveNull() {
        assertNull(Monto.leer(""));
        assertNull(Monto.leer("."));
        assertNull(Monto.leer("abc"));
    }

    @Test
    public void miles_ponePuntos() {
        assertEquals("0", Monto.miles(0));
        assertEquals("999", Monto.miles(999));
        assertEquals("1.000", Monto.miles(1000));
        assertEquals("110.000", Monto.miles(110000));
        assertEquals("1.000.000.000", Monto.miles(1000000000));
    }

    @Test
    public void miles_redondeaAPesos() {
        assertEquals("5.333", Monto.miles(5333.33));
        assertEquals("5.334", Monto.miles(5333.5));
    }

    @Test
    public void dinero_positivoYNegativo() {
        assertEquals("$110.000", Monto.dinero(110000));
        assertEquals("-$5.000", Monto.dinero(-5000));
    }

    @Test
    public void leerLoQueMuestraMiles_devuelveElMismoMonto() {
        // Lo que se ve en pantalla se puede volver a leer sin cambiar el valor
        assertEquals(1250000, Monto.leer(Monto.miles(1250000)), DELTA);
    }
}
