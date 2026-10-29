package com.example.mapalocalcash.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.mapalocalcash.data.DBHelper;
import com.example.mapalocalcash.model.Deuda;

import java.util.ArrayList;

public class DeudaDAO implements IDeuda {

    // Deuda con el nombre de la persona (propio o del contacto) y su origen (compra o detalle)
    private static final String SELECT =
            "SELECT d.id, d.compra_id, d.contacto_id, COALESCE(d.nombre, ct.nombre, 'Persona'), d.monto, d.pagada, d.detalle, " +
            "COALESCE(co.descripcion, d.detalle, 'Deuda') " +
            "FROM " + DBHelper.TABLE_DEUDAS + " d " +
            "LEFT JOIN " + DBHelper.TABLE_CONTACTOS + " ct ON ct.id = d.contacto_id " +
            "LEFT JOIN " + DBHelper.TABLE_COMPRAS + " co ON co.id = d.compra_id ";

    // Pendientes de un contacto: las enlazadas a el y las de gastos compartidos donde aparece con su nombre
    private static final String DEL_CONTACTO =
            "WHERE d.pagada = 0 AND (d.contacto_id = ? OR (d.contacto_id IS NULL AND LOWER(d.nombre) = LOWER(?)))";

    private final DBHelper dbHelper;

    public DeudaDAO(Context context) {
        dbHelper = new DBHelper(context);
    }

    @Override
    public long crear(Deuda deuda) {
        ContentValues cv = new ContentValues();
        if (deuda.getCompraId() != null) {
            cv.put("compra_id", deuda.getCompraId());
        }
        if (deuda.getContactoId() != null) {
            cv.put("contacto_id", deuda.getContactoId());
        }
        cv.put("nombre", deuda.getNombre());
        cv.put("monto", deuda.getMonto());
        cv.put("pagada", deuda.isPagada() ? 1 : 0);
        cv.put("detalle", deuda.getDetalle());
        return dbHelper.getWritableDatabase().insert(DBHelper.TABLE_DEUDAS, null, cv);
    }

    @Override
    public ArrayList<Deuda> listarPorCompra(int compraId) {
        return consultar(SELECT + "WHERE d.compra_id = ?", new String[]{String.valueOf(compraId)});
    }

    @Override
    public ArrayList<Deuda> pendientesDeContacto(int contactoId, String nombre) {
        return consultar(SELECT + DEL_CONTACTO, new String[]{String.valueOf(contactoId), nombre});
    }

    @Override
    public double totalPendiente() {
        return sumar("SELECT IFNULL(SUM(monto), 0) FROM " + DBHelper.TABLE_DEUDAS + " WHERE pagada = 0", null);
    }

    @Override
    public double totalPendienteDeContacto(int contactoId, String nombre) {
        return sumar("SELECT IFNULL(SUM(d.monto), 0) FROM " + DBHelper.TABLE_DEUDAS + " d " + DEL_CONTACTO,
                new String[]{String.valueOf(contactoId), nombre});
    }

    @Override
    public boolean marcarPagada(int id, boolean pagada) {
        ContentValues cv = new ContentValues();
        cv.put("pagada", pagada ? 1 : 0);
        return dbHelper.getWritableDatabase().update(DBHelper.TABLE_DEUDAS, cv, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    @Override
    public void eliminarPorCompra(int compraId) {
        dbHelper.getWritableDatabase().delete(DBHelper.TABLE_DEUDAS, "compra_id = ?", new String[]{String.valueOf(compraId)});
    }

    // Deudas anotadas a mano (sin compra) de un contacto
    @Override
    public void eliminarManualesDeContacto(int contactoId) {
        dbHelper.getWritableDatabase().delete(DBHelper.TABLE_DEUDAS, "contacto_id = ? AND compra_id IS NULL",
                new String[]{String.valueOf(contactoId)});
    }

    // Las deudas de gastos compartidos quedan, pero ya sin enlace al contacto
    @Override
    public void desvincularContacto(int contactoId) {
        ContentValues cv = new ContentValues();
        cv.putNull("contacto_id");
        dbHelper.getWritableDatabase().update(DBHelper.TABLE_DEUDAS, cv, "contacto_id = ?", new String[]{String.valueOf(contactoId)});
    }

    private double sumar(String sql, String[] args) {
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(sql, args);
        try {
            return cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        } finally {
            cursor.close();
        }
    }

    private ArrayList<Deuda> consultar(String sql, String[] args) {
        ArrayList<Deuda> lista = new ArrayList<>();
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(sql, args);
        try {
            while (cursor.moveToNext()) {
                lista.add(new Deuda(
                        cursor.getInt(0),
                        cursor.isNull(1) ? null : cursor.getInt(1),
                        cursor.isNull(2) ? null : cursor.getInt(2),
                        cursor.getString(3),
                        cursor.getDouble(4),
                        cursor.getInt(5) == 1,
                        cursor.getString(6),
                        cursor.getString(7)));
            }
        } finally {
            cursor.close();
        }
        return lista;
    }
}
