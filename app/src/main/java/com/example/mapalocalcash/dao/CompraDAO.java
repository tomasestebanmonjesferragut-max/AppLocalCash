package com.example.mapalocalcash.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.mapalocalcash.data.DBHelper;
import com.example.mapalocalcash.model.Compra;
import com.example.mapalocalcash.model.GastoCompartido;

import java.util.ArrayList;

public class CompraDAO implements ICompra {

    // Compra con el nombre del contacto con quien se compartio
    private static final String SELECT =
            "SELECT co.id, co.descripcion, co.monto, co.fecha, co.agendada, co.contacto_id, ct.nombre " +
            "FROM " + DBHelper.TABLE_COMPRAS + " co LEFT JOIN " + DBHelper.TABLE_CONTACTOS + " ct ON ct.id = co.contacto_id ";

    private final DBHelper dbHelper;

    public CompraDAO(Context context) {
        dbHelper = new DBHelper(context);
    }

    @Override
    public long crear(Compra compra) {
        ContentValues cv = new ContentValues();
        cv.put("descripcion", compra.getDescripcion());
        cv.put("monto", compra.getMonto());
        cv.put("fecha", compra.getFecha());
        cv.put("agendada", compra.isAgendada() ? 1 : 0);
        if (compra.getContactoId() != null) {
            cv.put("contacto_id", compra.getContactoId());
        }
        return dbHelper.getWritableDatabase().insert(DBHelper.TABLE_COMPRAS, null, cv);
    }

    @Override
    public Compra buscar(int id) {
        ArrayList<Compra> lista = consultar(SELECT + "WHERE co.id = ?", new String[]{String.valueOf(id)});
        return lista.isEmpty() ? null : lista.get(0);
    }

    // Primero las agendadas, despues las mas nuevas
    @Override
    public ArrayList<Compra> listar() {
        return consultar(SELECT + "ORDER BY co.agendada DESC, co.id DESC", null);
    }

    @Override
    public boolean actualizar(int id, String descripcion, double monto) {
        ContentValues cv = new ContentValues();
        cv.put("descripcion", descripcion);
        cv.put("monto", monto);
        return dbHelper.getWritableDatabase().update(DBHelper.TABLE_COMPRAS, cv, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    @Override
    public boolean marcarRealizada(int id) {
        ContentValues cv = new ContentValues();
        cv.put("agendada", 0);
        return dbHelper.getWritableDatabase().update(DBHelper.TABLE_COMPRAS, cv, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    @Override
    public boolean eliminar(int id) {
        return dbHelper.getWritableDatabase().delete(DBHelper.TABLE_COMPRAS, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // Compras que tienen deudas = gastos compartidos. Pendiente: falta que alguien pague.
    @Override
    public ArrayList<GastoCompartido> listarGastosCompartidos(int filtro) {
        String having = "";
        if (filtro == PENDIENTES) {
            having = " HAVING SUM(d.pagada) < COUNT(d.id)";
        } else if (filtro == COMPLETAS) {
            having = " HAVING SUM(d.pagada) = COUNT(d.id)";
        }

        ArrayList<GastoCompartido> lista = new ArrayList<>();
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT co.id, co.descripcion, co.monto, co.fecha, COUNT(d.id), SUM(d.pagada), " +
                        "SUM(CASE WHEN d.pagada = 0 THEN d.monto ELSE 0 END) " +
                        "FROM " + DBHelper.TABLE_COMPRAS + " co JOIN " + DBHelper.TABLE_DEUDAS + " d ON d.compra_id = co.id " +
                        "GROUP BY co.id" + having + " ORDER BY co.id DESC", null);
        try {
            while (cursor.moveToNext()) {
                lista.add(new GastoCompartido(cursor.getInt(0), cursor.getString(1), cursor.getDouble(2),
                        cursor.getString(3), cursor.getInt(4), cursor.getInt(5), cursor.getDouble(6)));
            }
        } finally {
            cursor.close();
        }
        return lista;
    }

    private ArrayList<Compra> consultar(String sql, String[] args) {
        ArrayList<Compra> lista = new ArrayList<>();
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(sql, args);
        try {
            while (cursor.moveToNext()) {
                lista.add(new Compra(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getDouble(2),
                        cursor.getString(3),
                        cursor.getInt(4) == 1,
                        cursor.isNull(5) ? null : cursor.getInt(5),
                        cursor.getString(6)));
            }
        } finally {
            cursor.close();
        }
        return lista;
    }
}
