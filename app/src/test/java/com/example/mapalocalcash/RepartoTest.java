package com.example.mapalocalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

// Pruebas de la division de un gasto compartido
public class RepartoTest {

    private static final double DELTA = 0.001;

    private static double suma(List<Double> montos) {
        double total = 0;
        for (double m : montos) {
            total += m;
        }
        return total;
    }

    @Test
    public void partesIguales_divisionExacta() {
        assertEquals(Arrays.asList(10000.0, 10000.0), Reparto.partesIguales(20000, 2));
    }

    @Test
    public void partesIguales_conResto_laUltimaPagaLaDiferencia() {
        // Error encontrado: antes cada uno quedaba en 5333,33 y en pantalla sumaban $15.999
        assertEquals(Arrays.asList(5333.0, 5333.0, 5334.0), Reparto.partesIguales(16000, 3));
    }

    @Test
    public void partesIguales_siempreSumaElTotal() {
        int[] totales = {1555, 16000, 20000, 99999, 1000001};
        for (int total : totales) {
            for (int personas = 1; personas <= 7; personas++) {
                assertEquals("total " + total + " entre " + personas,
                        total, suma(Reparto.partesIguales(total, personas)), DELTA);
            }
        }
    }

    @Test
    public void partesIguales_unaPersonaPagaTodo() {
        assertEquals(Arrays.asList(15000.0), Reparto.partesIguales(15000, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void partesIguales_ceroPersonasEsError() {
        Reparto.partesIguales(10000, 0);
    }

    @Test
    public void falta_cuandoNoAlcanza() {
        assertEquals(4000, Reparto.falta(16000, Arrays.asList(5000.0, 7000.0)), DELTA);
    }

    @Test
    public void falta_negativaCuandoSePasan() {
        assertEquals(-2000, Reparto.falta(16000, Arrays.asList(10000.0, 8000.0)), DELTA);
    }

    @Test
    public void cuadra_soloSiLaSumaEsElTotal() {
        assertTrue(Reparto.cuadra(16000, Arrays.asList(6000.0, 10000.0)));
        assertFalse(Reparto.cuadra(16000, Arrays.asList(6000.0, 9000.0)));
        assertFalse(Reparto.cuadra(16000, Arrays.asList(6000.0, 11000.0)));
    }
}
