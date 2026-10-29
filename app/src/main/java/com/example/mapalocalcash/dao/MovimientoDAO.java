package com.example.mapalocalcash.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.mapalocalcash.data.DBHelper;
import com.example.mapalocalcash.model.Movimiento;

import java.util.ArrayList;

public class MovimientoDAO implements IMovimiento {

    // Movimiento con el nombre de su cuenta
    private static final String SELECT =
            "SELECT m.id, m.cuenta_id, m.tipo, m.monto, m.descripcion, m.lat, m.lon, c.nombre " +
            "FROM " + DBHelper.TABLE_MOVIMIENTOS + " m LEFT JOIN " + DBHelper.TABLE_CUENTAS + " c ON c.id = m.cuenta_id ";

    private final DBHelper dbHelper;

    public MovimientoDAO(Context context) {
        dbHelper = new DBHelper(context);
    }

    @Override
    public long crear(Movimiento m) {
        ContentValues cv = new ContentValues();
        cv.put("cuenta_id", m.getCuentaId());
        cv.put("tipo", m.getTipo());
        cv.put("monto", m.getMonto());
        cv.put("descripcion", m.getDescripcion());
        if (m.tieneUbicacion()) {
            cv.put("lat", m.getLat());
            cv.put("lon", m.getLon());
        }
        return dbHelper.getWritableDatabase().insert(DBHelper.TABLE_MOVIMIENTOS, null, cv);
    }

    @Override
    public Movimiento buscar(int id) {
        ArrayList<Movimiento> lista = consultar(SELECT + "WHERE m.id = ?", new String[]{String.valueOf(id)});
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public ArrayList<Movimiento> listarPorCuenta(int cuentaId) {
        return consultar(SELECT + "WHERE m.cuenta_id = ? ORDER BY m.id DESC", new String[]{String.valueOf(cuentaId)});
    }

    @Override
    public ArrayList<Movimiento> listarConUbicacion() {
        return consultar(SELECT + "WHERE m.lat IS NOT NULL", null);
    }

    @Override
    public boolean eliminar(int id) {
        return dbHelper.getWritableDatabase().delete(DBHelper.TABLE_MOVIMIENTOS, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    @Override
    public void eliminarPorCuenta(int cuentaId) {
        dbHelper.getWritableDatabase().delete(DBHelper.TABLE_MOVIMIENTOS, "cuenta_id = ?", new String[]{String.valueOf(cuentaId)});
    }

    private ArrayList<Movimiento> consultar(String sql, String[] args) {
        ArrayList<Movimiento> lista = new ArrayList<>();
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(sql, args);
        try {
            while (cursor.moveToNext()) {
                lista.add(new Movimiento(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getDouble(3),
                        cursor.getString(4),
                        cursor.isNull(5) ? null : cursor.getDouble(5),
                        cursor.isNull(6) ? null : cursor.getDouble(6),
                        cursor.getString(7)));
            }
        } finally {
            cursor.close();
        }
        return lista;
    }
}
