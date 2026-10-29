package com.example.mapalocalcash;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class DialogoMovimiento {

    // Pide confirmacion, borra el movimiento y devuelve el saldo de la cuenta a como estaba
    public static void confirmarBorrar(AppCompatActivity activity, DBHelper dbHelper, int movimientoId, Runnable alBorrar) {
        new AlertDialog.Builder(activity)
                .setTitle("¿Borrar este movimiento?")
                .setMessage("El saldo de la cuenta vuelve a como estaba antes del movimiento.")
                .setPositiveButton("Borrar", (d, w) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    Cursor c = db.rawQuery("SELECT cuenta_id, tipo, monto FROM movimientos WHERE id = ?",
                            new String[]{String.valueOf(movimientoId)});
                    if (c.moveToFirst()) {
                        double monto = c.getDouble(2);
                        double revertir = c.getString(1).equals("Ingreso") ? -monto : monto;
                        db.execSQL("UPDATE cuentas SET saldo = saldo + ? WHERE id = ?", new Object[]{revertir, c.getInt(0)});
                        db.delete("movimientos", "id = ?", new String[]{String.valueOf(movimientoId)});
                    }
                    c.close();
                    Toast.makeText(activity, "Movimiento borrado", Toast.LENGTH_SHORT).show();
                    alBorrar.run();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Muestra el formulario de ingreso/egreso. punto es null cuando se registra sin ubicacion.
    // alGuardar se ejecuta despues de guardar (para refrescar la pantalla).
    public static void mostrar(AppCompatActivity activity, DBHelper dbHelper, LatLng punto, Runnable alGuardar) {
        ArrayList<Integer> idsCuentas = new ArrayList<>();
        ArrayList<String> nombresCuentas = new ArrayList<>();
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT id, nombre FROM cuentas", null);
        while (c.moveToNext()) {
            idsCuentas.add(c.getInt(0));
            nombresCuentas.add(c.getString(1));
        }
        c.close();

        if (idsCuentas.isEmpty()) {
            Toast.makeText(activity, "Primero crea una cuenta en 💳 Cuentas", Toast.LENGTH_LONG).show();
            return;
        }

        View vista = activity.getLayoutInflater().inflate(R.layout.dialog_movimiento, null);
        Spinner spCuenta = vista.findViewById(R.id.spCuenta);
        RadioButton rbIngreso = vista.findViewById(R.id.rbIngreso);
        EditText txtMonto = vista.findViewById(R.id.txtMonto);
        EditText txtDescripcion = vista.findViewById(R.id.txtDescripcion);

        spCuenta.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, nombresCuentas));

        new AlertDialog.Builder(activity)
                .setTitle(punto == null ? "Nuevo movimiento" : "Nuevo movimiento en el mapa")
                .setView(vista)
                .setPositiveButton("Guardar", (d, w) -> {
                    Double monto = Monto.leer(txtMonto.getText().toString());
                    if (monto == null) {
                        Toast.makeText(activity, "Escribe un monto valido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String tipo = rbIngreso.isChecked() ? "Ingreso" : "Egreso";
                    int cuentaId = idsCuentas.get(spCuenta.getSelectedItemPosition());

                    ContentValues valores = new ContentValues();
                    valores.put("cuenta_id", cuentaId);
                    valores.put("tipo", tipo);
                    valores.put("monto", monto);
                    valores.put("descripcion", txtDescripcion.getText().toString());
                    if (punto != null) {
                        valores.put("lat", punto.latitude);
                        valores.put("lon", punto.longitude);
                    }

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.insert("movimientos", null, valores);
                    double cambio = tipo.equals("Ingreso") ? monto : -monto;
                    db.execSQL("UPDATE cuentas SET saldo = saldo + ? WHERE id = ?", new Object[]{cambio, cuentaId});

                    Toast.makeText(activity, "Movimiento guardado", Toast.LENGTH_SHORT).show();
                    alGuardar.run();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
