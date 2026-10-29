package com.example.mapalocalcash.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.mapalocalcash.data.DBHelper;
import com.example.mapalocalcash.model.Producto;

import java.util.ArrayList;

public class ProductoDAO implements IProducto {

    private final DBHelper dbHelper;

    public ProductoDAO(Context context) {
        dbHelper = new DBHelper(context);
    }

    @Override
    public long crear(int compraId, Producto producto) {
        ContentValues cv = new ContentValues();
        cv.put("compra_id", compraId);
        cv.put("nombre", producto.getNombre());
        cv.put("precio", producto.getPrecio());
        return dbHelper.getWritableDatabase().insert(DBHelper.TABLE_PRODUCTOS, null, cv);
    }

    @Override
    public ArrayList<Producto> listarPorCompra(int compraId) {
        ArrayList<Producto> productos = new ArrayList<>();
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT id, compra_id, nombre, precio FROM " + DBHelper.TABLE_PRODUCTOS + " WHERE compra_id = ?",
                new String[]{String.valueOf(compraId)});
        try {
            while (cursor.moveToNext()) {
                productos.add(new Producto(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getDouble(3)));
            }
        } finally {
            cursor.close();
        }
        return productos;
    }

    @Override
    public void eliminarPorCompra(int compraId) {
        dbHelper.getWritableDatabase().delete(DBHelper.TABLE_PRODUCTOS, "compra_id = ?", new String[]{String.valueOf(compraId)});
    }
}
