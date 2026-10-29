package com.example.mapalocalcash.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.mapalocalcash.data.DBHelper;
import com.example.mapalocalcash.model.Contacto;

import java.util.ArrayList;

public class ContactoDAO implements IContacto {

    private final DBHelper dbHelper;

    public ContactoDAO(Context context) {
        dbHelper = new DBHelper(context);
    }

    @Override
    public long crear(Contacto contacto) {
        ContentValues cv = new ContentValues();
        cv.put("nombre", contacto.getNombre());
        cv.put("telefono", contacto.getTelefono());
        return dbHelper.getWritableDatabase().insert(DBHelper.TABLE_CONTACTOS, null, cv);
    }

    @Override
    public ArrayList<Contacto> listar() {
        ArrayList<Contacto> contactos = new ArrayList<>();
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT id, nombre, telefono FROM " + DBHelper.TABLE_CONTACTOS + " ORDER BY nombre", null);
        try {
            while (cursor.moveToNext()) {
                contactos.add(new Contacto(cursor.getInt(0), cursor.getString(1), cursor.getString(2)));
            }
        } finally {
            cursor.close();
        }
        return contactos;
    }

    @Override
    public boolean eliminar(int id) {
        return dbHelper.getWritableDatabase().delete(DBHelper.TABLE_CONTACTOS, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }
}
