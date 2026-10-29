package com.example.mapalocalcash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.example.mapalocalcash.dao.CompraDAO;
import com.example.mapalocalcash.dao.ContactoDAO;
import com.example.mapalocalcash.dao.CuentaDAO;
import com.example.mapalocalcash.dao.DeudaDAO;
import com.example.mapalocalcash.dao.ICompra;
import com.example.mapalocalcash.dao.MovimientoDAO;
import com.example.mapalocalcash.dao.ProductoDAO;
import com.example.mapalocalcash.model.Compra;
import com.example.mapalocalcash.model.Contacto;
import com.example.mapalocalcash.model.Cuenta;
import com.example.mapalocalcash.model.Deuda;
import com.example.mapalocalcash.model.GastoCompartido;
import com.example.mapalocalcash.model.Movimiento;
import com.example.mapalocalcash.model.Producto;
import com.example.mapalocalcash.repository.CompraRepository;
import com.example.mapalocalcash.repository.ContactoRepository;
import com.example.mapalocalcash.repository.MovimientoRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// Pruebas de los DAO y repositorios contra una base SQLite real (vacia en cada prueba)
@RunWith(RobolectricTestRunner.class)
public class BaseDeDatosTest {

    private static final double DELTA = 0.001;

    private Context context;
    private CuentaDAO cuentaDAO;
    private MovimientoDAO movimientoDAO;
    private CompraDAO compraDAO;
    private DeudaDAO deudaDAO;

    @Before
    public void preparar() {
        context = ApplicationProvider.getApplicationContext();
        cuentaDAO = new CuentaDAO(context);
        movimientoDAO = new MovimientoDAO(context);
        compraDAO = new CompraDAO(context);
        deudaDAO = new DeudaDAO(context);
    }

    // ---------- Cuentas ----------

    @Test
    public void cuenta_crearYListar() {
        cuentaDAO.crear(new Cuenta("Efectivo", 50000));
        cuentaDAO.crear(new Cuenta("Ahorros", 100000));

        List<Cuenta> cuentas = cuentaDAO.listar();
        assertEquals(2, cuentas.size());
        assertEquals("Efectivo", cuentas.get(0).getNombre());
        assertEquals(150000, cuentaDAO.saldoTotal(), DELTA);
    }

    @Test
    public void cuenta_saldoTotalSinCuentasEsCero() {
        assertEquals(0, cuentaDAO.saldoTotal(), DELTA);
    }

    // ---------- Movimientos y saldo ----------

    @Test
    public void movimiento_ingresoSumaYEgresoRestaAlSaldo() {
        int cuenta = (int) cuentaDAO.crear(new Cuenta("Efectivo", 10000));
        MovimientoRepository repo = new MovimientoRepository(context);

        repo.registrar(new Movimiento(cuenta, Movimiento.INGRESO, 5000, "sueldo", null, null));
        repo.registrar(new Movimiento(cuenta, Movimiento.EGRESO, 1600, "desayuno", null, null));

        assertEquals(13400, cuentaDAO.saldoTotal(), DELTA);
        assertEquals(2, movimientoDAO.listarPorCuenta(cuenta).size());
    }

    @Test
    public void movimiento_borrarDevuelveElSaldo() {
        int cuenta = (int) cuentaDAO.crear(new Cuenta("Efectivo", 10000));
        MovimientoRepository repo = new MovimientoRepository(context);
        repo.registrar(new Movimiento(cuenta, Movimiento.EGRESO, 3000, "almuerzo", null, null));
        int id = movimientoDAO.listarPorCuenta(cuenta).get(0).getId();

        repo.borrar(id);

        assertEquals(10000, cuentaDAO.saldoTotal(), DELTA);
        assertNull(movimientoDAO.buscar(id));
    }

    @Test
    public void movimiento_conUbicacionApareceEnElMapa() {
        int cuenta = (int) cuentaDAO.crear(new Cuenta("Efectivo", 0));
        MovimientoRepository repo = new MovimientoRepository(context);
        repo.registrar(new Movimiento(cuenta, Movimiento.EGRESO, 1000, "con lugar", -33.4989, -70.6166));
        repo.registrar(new Movimiento(cuenta, Movimiento.EGRESO, 1000, "sin lugar", null, null));

        List<Movimiento> enMapa = movimientoDAO.listarConUbicacion();
        assertEquals(1, enMapa.size());
        assertEquals("con lugar", enMapa.get(0).getDescripcion());
        assertEquals(-33.4989, enMapa.get(0).getLat(), 0.00001);
        assertEquals("Efectivo", enMapa.get(0).getCuentaNombre());
    }

    @Test
    public void cuenta_borrarTambienBorraSusMovimientos() {
        int cuenta = (int) cuentaDAO.crear(new Cuenta("Tarjeta", 0));
        new MovimientoRepository(context).registrar(new Movimiento(cuenta, Movimiento.EGRESO, 500, "x", null, null));

        new MovimientoRepository(context).borrarCuenta(cuenta);

        assertTrue(cuentaDAO.listar().isEmpty());
        assertTrue(movimientoDAO.listarPorCuenta(cuenta).isEmpty());
    }

    // ---------- Compras ----------

    @Test
    public void compra_agendadaYMarcarRealizada() {
        int id = (int) compraDAO.crear(new Compra("Zapatillas", 40000, "15-12-2026", true, null));
        assertTrue(compraDAO.buscar(id).isAgendada());

        compraDAO.marcarRealizada(id);

        assertFalse(compraDAO.buscar(id).isAgendada());
    }

    @Test
    public void compra_agendadasAparecenPrimero() {
        compraDAO.crear(new Compra("Pan", 2000, "01-10-2026", false, null));
        compraDAO.crear(new Compra("Bicicleta", 300000, "01-12-2026", true, null));

        assertEquals("Bicicleta", compraDAO.listar().get(0).getDescripcion());
    }

    // ---------- Gastos compartidos ----------

    private HashMap<String, Integer> sinContactos() {
        return new HashMap<>();
    }

    @Test
    public void gasto_guardarCreaCompraProductosYDeudas() {
        CompraRepository repo = new CompraRepository(context);
        List<Producto> productos = Arrays.asList(new Producto("pizza", 15000), new Producto("bebida", 1000));

        repo.guardarGastoCompartido(null, productos, Arrays.asList("Persona 1", "Persona 2"),
                Reparto.partesIguales(16000, 2), sinContactos());

        List<GastoCompartido> gastos = compraDAO.listarGastosCompartidos(ICompra.TODAS);
        assertEquals(1, gastos.size());
        GastoCompartido gasto = gastos.get(0);
        assertEquals("pizza, bebida", gasto.getDescripcion());
        assertEquals(16000, gasto.getTotal(), DELTA);
        assertEquals(2, gasto.getPersonas());
        assertEquals(16000, gasto.getFaltaCobrar(), DELTA);
        assertEquals(2, new ProductoDAO(context).listarPorCompra(gasto.getCompraId()).size());
    }

    @Test
    public void gasto_filtroPendientesYCompletas() {
        CompraRepository repo = new CompraRepository(context);
        repo.guardarGastoCompartido(null, Arrays.asList(new Producto("cena", 20000)),
                Arrays.asList("Ana", "Luis"), Reparto.partesIguales(20000, 2), sinContactos());
        int compraId = compraDAO.listarGastosCompartidos(ICompra.TODAS).get(0).getCompraId();

        assertEquals(1, compraDAO.listarGastosCompartidos(ICompra.PENDIENTES).size());
        assertEquals(0, compraDAO.listarGastosCompartidos(ICompra.COMPLETAS).size());

        for (Deuda d : deudaDAO.listarPorCompra(compraId)) {
            deudaDAO.marcarPagada(d.getId(), true);
        }

        assertEquals(0, compraDAO.listarGastosCompartidos(ICompra.PENDIENTES).size());
        assertEquals(1, compraDAO.listarGastosCompartidos(ICompra.COMPLETAS).size());
        assertEquals(0, deudaDAO.totalPendiente(), DELTA);
    }

    @Test
    public void gasto_editarMantieneAQuienesYaPagaron() {
        CompraRepository repo = new CompraRepository(context);
        repo.guardarGastoCompartido(null, Arrays.asList(new Producto("cena", 20000)),
                Arrays.asList("Ana", "Luis"), Reparto.partesIguales(20000, 2), sinContactos());
        int compraId = compraDAO.listarGastosCompartidos(ICompra.TODAS).get(0).getCompraId();
        for (Deuda d : deudaDAO.listarPorCompra(compraId)) {
            if (d.getNombre().equals("Ana")) {
                deudaDAO.marcarPagada(d.getId(), true);
            }
        }

        // Se cambia el precio: la cena costaba 30.000
        repo.guardarGastoCompartido(compraId, Arrays.asList(new Producto("cena", 30000)),
                Arrays.asList("Ana", "Luis"), Reparto.partesIguales(30000, 2), sinContactos());

        assertEquals(1, compraDAO.listarGastosCompartidos(ICompra.TODAS).size());
        for (Deuda d : deudaDAO.listarPorCompra(compraId)) {
            assertEquals(15000, d.getMonto(), DELTA);
            assertEquals(d.getNombre().equals("Ana"), d.isPagada());
        }
    }

    @Test
    public void gasto_borrarEliminaProductosYDeudas() {
        CompraRepository repo = new CompraRepository(context);
        repo.guardarGastoCompartido(null, Arrays.asList(new Producto("cena", 20000)),
                Arrays.asList("Ana", "Luis"), Reparto.partesIguales(20000, 2), sinContactos());
        int compraId = compraDAO.listarGastosCompartidos(ICompra.TODAS).get(0).getCompraId();

        repo.borrar(compraId);

        assertTrue(compraDAO.listarGastosCompartidos(ICompra.TODAS).isEmpty());
        assertTrue(new ProductoDAO(context).listarPorCompra(compraId).isEmpty());
        assertTrue(deudaDAO.listarPorCompra(compraId).isEmpty());
        assertNull(compraDAO.buscar(compraId));
    }

    // ---------- Contactos y deudas ----------

    @Test
    public void contacto_conDeudaAnotadaAMano() {
        ContactoRepository repo = new ContactoRepository(context);
        repo.crear(new Contacto("Ana", "912345678"), 5000.0, "préstamo");
        Contacto ana = new ContactoDAO(context).listar().get(0);

        assertEquals("912345678", ana.getTelefono());
        assertEquals(5000, deudaDAO.totalPendienteDeContacto(ana.getId(), "Ana"), DELTA);
        assertEquals("préstamo", deudaDAO.pendientesDeContacto(ana.getId(), "Ana").get(0).getOrigen());
    }

    @Test
    public void contacto_sumaLoQueDebeEnGastosCompartidos() {
        new ContactoRepository(context).crear(new Contacto("Ana", ""), null, "");
        Contacto ana = new ContactoDAO(context).listar().get(0);
        HashMap<String, Integer> contactos = new HashMap<>();
        contactos.put("ana", ana.getId());

        new CompraRepository(context).guardarGastoCompartido(null, Arrays.asList(new Producto("cena", 20000)),
                Arrays.asList("Ana", "Luis"), Reparto.partesIguales(20000, 2), contactos);

        assertEquals(10000, deudaDAO.totalPendienteDeContacto(ana.getId(), "Ana"), DELTA);
    }

    @Test
    public void contacto_borrarQuitaSusDeudasAManoPeroNoLasDeGastos() {
        ContactoRepository repo = new ContactoRepository(context);
        repo.crear(new Contacto("Ana", ""), 5000.0, "");
        Contacto ana = new ContactoDAO(context).listar().get(0);
        HashMap<String, Integer> contactos = new HashMap<>();
        contactos.put("ana", ana.getId());
        new CompraRepository(context).guardarGastoCompartido(null, Arrays.asList(new Producto("cena", 20000)),
                Arrays.asList("Ana", "Luis"), Reparto.partesIguales(20000, 2), contactos);

        repo.borrar(ana.getId());

        assertTrue(new ContactoDAO(context).listar().isEmpty());
        // Quedan las dos deudas de la cena (10.000 de Ana y 10.000 de Luis); la de 5.000 a mano se borro
        assertEquals(20000, deudaDAO.totalPendiente(), DELTA);
        int compraId = compraDAO.listarGastosCompartidos(ICompra.TODAS).get(0).getCompraId();
        List<String> nombres = new ArrayList<>();
        for (Deuda d : deudaDAO.listarPorCompra(compraId)) {
            nombres.add(d.getNombre());
            assertNull(d.getContactoId());
        }
        assertTrue(nombres.contains("Ana"));
    }
}
