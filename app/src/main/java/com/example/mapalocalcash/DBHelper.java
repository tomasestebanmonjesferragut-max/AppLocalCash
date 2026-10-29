package com.example.mapalocalcash;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "localcash.db", null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE cuentas (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, saldo REAL)");
        db.execSQL("CREATE TABLE movimientos (id INTEGER PRIMARY KEY AUTOINCREMENT, cuenta_id INTEGER, tipo TEXT, monto REAL, descripcion TEXT, lat REAL, lon REAL)");
        db.execSQL("CREATE TABLE contactos (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT)");
        db.execSQL("CREATE TABLE compras (id INTEGER PRIMARY KEY AUTOINCREMENT, descripcion TEXT, monto REAL, fecha TEXT, agendada INTEGER, contacto_id INTEGER)");
        db.execSQL("CREATE TABLE deudas (id INTEGER PRIMARY KEY AUTOINCREMENT, compra_id INTEGER, contacto_id INTEGER, monto REAL, pagada INTEGER, nombre TEXT)");
        db.execSQL("CREATE TABLE compra_productos (id INTEGER PRIMARY KEY AUTOINCREMENT, compra_id INTEGER, nombre TEXT, precio REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 2: productos de cada gasto compartido (no se borran los datos anteriores)
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE compra_productos (id INTEGER PRIMARY KEY AUTOINCREMENT, compra_id INTEGER, nombre TEXT, precio REAL)");
        }
        // Version 3: nombre de cada persona en un gasto compartido (puede no estar en contactos)
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE deudas ADD COLUMN nombre TEXT");
        }
    }
}
