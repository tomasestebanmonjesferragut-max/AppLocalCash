package com.example.mapalocalcash.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mapalocalcash.data.DBHelper;
import com.example.mapalocalcash.model.Cuenta;

import java.util.ArrayList;

public class CuentaDAO implements ICuenta {

    private final DBHelper dbHelper;

    public CuentaDAO(Context context) {
        dbHelper = new DBHelper(context);
    }

    @Override
    public long crear(Cuenta cuenta) {
        ContentValues cv = new ContentValues();
        cv.put("nombre", cuenta.getNombre());
        cv.put("saldo", cuenta.getSaldo());
        return dbHelper.getWritableDatabase().insert(DBHelper.TABLE_CUENTAS, null, cv);
    }

    @Override
    public ArrayList<Cuenta> listar() {
        ArrayList<Cuenta> cuentas = new ArrayList<>();
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery("SELECT id, nombre, saldo FROM " + DBHelper.TABLE_CUENTAS, null);
        try {
            while (cursor.moveToNext()) {
                cuentas.add(new Cuenta(cursor.getInt(0), cursor.getString(1), cursor.getDouble(2)));
            }
        } finally {
            cursor.close();
        }
        return cuentas;
    }

    @Override
    public boolean eliminar(int id) {
        return dbHelper.getWritableDatabase().delete(DBHelper.TABLE_CUENTAS, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // Suma (o resta, si es negativa) la diferencia al saldo de la cuenta
    @Override
    public void cambiarSaldo(int id, double diferencia) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE " + DBHelper.TABLE_CUENTAS + " SET saldo = saldo + ? WHERE id = ?", new Object[]{diferencia, id});
    }

    @Override
    public double saldoTotal() {
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery("SELECT IFNULL(SUM(saldo), 0) FROM " + DBHelper.TABLE_CUENTAS, null);
        try {
            return cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        } finally {
            cursor.close();
        }
    }
}
