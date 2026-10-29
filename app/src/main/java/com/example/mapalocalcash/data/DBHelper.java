package com.example.mapalocalcash.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// Crea y actualiza la base de datos local de LocalCash
public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "localcash.db";
    // Version 4: telefono en contactos y detalle en deudas
    private static final int DATABASE_VERSION = 4;

    public static final String TABLE_CUENTAS = "cuentas";
    public static final String TABLE_MOVIMIENTOS = "movimientos";
    public static final String TABLE_CONTACTOS = "contactos";
    public static final String TABLE_COMPRAS = "compras";
    public static final String TABLE_PRODUCTOS = "compra_productos";
    public static final String TABLE_DEUDAS = "deudas";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CUENTAS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, saldo REAL)");
        db.execSQL("CREATE TABLE " + TABLE_MOVIMIENTOS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, cuenta_id INTEGER, tipo TEXT, monto REAL, descripcion TEXT, lat REAL, lon REAL)");
        db.execSQL("CREATE TABLE " + TABLE_CONTACTOS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, telefono TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_COMPRAS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, descripcion TEXT, monto REAL, fecha TEXT, agendada INTEGER, contacto_id INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_DEUDAS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, compra_id INTEGER, contacto_id INTEGER, monto REAL, pagada INTEGER, nombre TEXT, detalle TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_PRODUCTOS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, compra_id INTEGER, nombre TEXT, precio REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 2: productos de cada gasto compartido (no se borran los datos anteriores)
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE " + TABLE_PRODUCTOS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, compra_id INTEGER, nombre TEXT, precio REAL)");
        }
        // Version 3: nombre de cada persona en un gasto compartido (puede no estar en contactos)
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_DEUDAS + " ADD COLUMN nombre TEXT");
        }
        // Version 4: telefono del contacto y motivo de una deuda anotada a mano (sin compra)
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_CONTACTOS + " ADD COLUMN telefono TEXT");
            db.execSQL("ALTER TABLE " + TABLE_DEUDAS + " ADD COLUMN detalle TEXT");
        }
    }
}
