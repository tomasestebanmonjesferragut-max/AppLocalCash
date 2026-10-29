package com.example.mapalocalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.mapalocalcash.model.Compra;
import com.example.mapalocalcash.model.Contacto;
import com.example.mapalocalcash.model.GastoCompartido;
import com.example.mapalocalcash.model.Movimiento;

import org.junit.Test;

// Pruebas de las reglas que viven en los modelos
public class ModeloTest {

    private static final double DELTA = 0.001;

    @Test
    public void movimiento_ingresoSumaAlSaldo() {
        Movimiento m = new Movimiento(1, Movimiento.INGRESO, 5000, "sueldo", null, null);
        assertTrue(m.esIngreso());
        assertEquals(5000, m.efectoEnSaldo(), DELTA);
    }

    @Test
    public void movimiento_egresoRestaAlSaldo() {
        Movimiento m = new Movimiento(1, Movimiento.EGRESO, 1600, "desayuno", null, null);
        assertFalse(m.esIngreso());
        assertEquals(-1600, m.efectoEnSaldo(), DELTA);
    }

    @Test
    public void movimiento_ubicacionSoloConLatitudYLongitud() {
        assertTrue(new Movimiento(1, Movimiento.EGRESO, 100, "", -33.49, -70.61).tieneUbicacion());
        assertFalse(new Movimiento(1, Movimiento.EGRESO, 100, "", null, null).tieneUbicacion());
        assertFalse(new Movimiento(1, Movimiento.EGRESO, 100, "", -33.49, null).tieneUbicacion());
    }

    @Test
    public void gastoCompartido_completoSoloSiPagaronTodos() {
        assertTrue(new GastoCompartido(1, "pizza", 20000, "29-10-2026", 2, 2, 0).estaCompleto());
        assertFalse(new GastoCompartido(1, "pizza", 20000, "29-10-2026", 2, 1, 10000).estaCompleto());
    }

    @Test
    public void contacto_sinTelefonoDevuelveTextoVacio() {
        assertEquals("", new Contacto(1, "Ana", null).getTelefono());
    }

    @Test
    public void compra_sinFechaDevuelveTextoVacio() {
        assertEquals("", new Compra(1, "mochila", 19990, null, false, null, null).getFecha());
    }
}
