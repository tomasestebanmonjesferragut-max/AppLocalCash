package com.example.mapalocalcash.repository;

import android.content.Context;

import com.example.mapalocalcash.dao.CuentaDAO;
import com.example.mapalocalcash.dao.MovimientoDAO;
import com.example.mapalocalcash.model.Movimiento;

// Movimientos y saldos: cada cambio en un movimiento ajusta el saldo de su cuenta
public class MovimientoRepository {

    private final MovimientoDAO movimientoDAO;
    private final CuentaDAO cuentaDAO;

    public MovimientoRepository(Context context) {
        movimientoDAO = new MovimientoDAO(context);
        cuentaDAO = new CuentaDAO(context);
    }

    // Guarda el movimiento y suma (ingreso) o resta (egreso) el monto al saldo
    public void registrar(Movimiento movimiento) {
        movimientoDAO.crear(movimiento);
        cuentaDAO.cambiarSaldo(movimiento.getCuentaId(), movimiento.efectoEnSaldo());
    }

    // Borra el movimiento y deja el saldo como estaba antes de el
    public void borrar(int movimientoId) {
        Movimiento movimiento = movimientoDAO.buscar(movimientoId);
        if (movimiento == null) {
            return;
        }
        cuentaDAO.cambiarSaldo(movimiento.getCuentaId(), -movimiento.efectoEnSaldo());
        movimientoDAO.eliminar(movimientoId);
    }

    // Borra la cuenta con todos sus movimientos
    public void borrarCuenta(int cuentaId) {
        movimientoDAO.eliminarPorCuenta(cuentaId);
        cuentaDAO.eliminar(cuentaId);
    }
}
