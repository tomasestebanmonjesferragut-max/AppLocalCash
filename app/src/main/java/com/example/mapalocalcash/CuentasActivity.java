package com.example.mapalocalcash;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class CuentasActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private ListView lista;
    private ArrayList<Integer> ids = new ArrayList<>();
    private ArrayList<String> nombres = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cuentas);

        dbHelper = new DBHelper(this);
        EditText txtNombre = findViewById(R.id.txtNombre);
        EditText txtSaldo = findViewById(R.id.txtSaldo);
        lista = findViewById(R.id.lista);

        findViewById(R.id.btnAgregar).setOnClickListener(v -> {
            String nombre = txtNombre.getText().toString();
            if (nombre.isEmpty()) {
                Toast.makeText(this, "Escribe un nombre (ej: Efectivo)", Toast.LENGTH_SHORT).show();
                return;
            }
            String textoSaldo = txtSaldo.getText().toString().trim();
            Double saldo = textoSaldo.isEmpty() ? Double.valueOf(0) : Monto.leer(textoSaldo);
            if (saldo == null) {
                Toast.makeText(this, "Escribe un saldo valido", Toast.LENGTH_SHORT).show();
                return;
            }

            ContentValues valores = new ContentValues();
            valores.put("nombre", nombre);
            valores.put("saldo", saldo);
            dbHelper.getWritableDatabase().insert("cuentas", null, valores);

            txtNombre.setText("");
            txtSaldo.setText("");
            cargar();
        });

        lista.setOnItemClickListener((parent, view, position, id) -> opciones(ids.get(position), nombres.get(position)));

        cargar();
    }

    private void cargar() {
        ids.clear();
        nombres.clear();
        ArrayList<CharSequence> textos = new ArrayList<>();
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT id, nombre, saldo FROM cuentas", null);
        while (c.moveToNext()) {
            ids.add(c.getInt(0));
            nombres.add(c.getString(1));
            double saldo = c.getDouble(2);
            // Saldo en verde si es positivo, en rojo si es negativo
            textos.add(Monto.conColor(this, c.getString(1) + "   ", "$" + Math.round(saldo), saldo >= 0));
        }
        c.close();
        lista.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos));
    }

    private void opciones(int cuentaId, String nombre) {
        String[] opciones = {"📋 Ver movimientos", "🗑️ Borrar cuenta"};
        new AlertDialog.Builder(this)
                .setTitle(nombre)
                .setItems(opciones, (d, which) -> {
                    if (which == 0) {
                        verMovimientos(cuentaId);
                    } else {
                        borrarCuenta(cuentaId, nombre);
                    }
                })
                .show();
    }

    private void borrarCuenta(int cuentaId, String nombre) {
        new AlertDialog.Builder(this)
                .setTitle("¿Borrar la cuenta " + nombre + "?")
                .setMessage("Se borran también todos sus movimientos (y sus marcadores del mapa). No se puede deshacer.")
                .setPositiveButton("Borrar", (d, w) -> {
                    String[] arg = {String.valueOf(cuentaId)};
                    dbHelper.getWritableDatabase().delete("movimientos", "cuenta_id = ?", arg);
                    dbHelper.getWritableDatabase().delete("cuentas", "id = ?", arg);
                    Toast.makeText(this, "Cuenta borrada", Toast.LENGTH_SHORT).show();
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Movimientos de la cuenta. Tocar uno permite borrarlo (el saldo se devuelve).
    private void verMovimientos(int cuentaId) {
        ArrayList<Integer> idsMovimientos = new ArrayList<>();
        ArrayList<CharSequence> textos = new ArrayList<>();
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT tipo, monto, descripcion, lat, id FROM movimientos WHERE cuenta_id = ? ORDER BY id DESC",
                new String[]{String.valueOf(cuentaId)});
        while (c.moveToNext()) {
            idsMovimientos.add(c.getInt(4));
            boolean ingreso = c.getString(0).equals("Ingreso");
            String ubicacion = c.isNull(3) ? "" : " 📍";
            // Ingreso en verde con +, egreso en rojo con −
            String monto = (ingreso ? "+ $" : "− $") + Math.round(c.getDouble(1));
            textos.add(TextUtils.concat(Monto.conColor(this, "", monto, ingreso), "  " + c.getString(2) + ubicacion));
        }
        c.close();

        if (textos.isEmpty()) {
            Toast.makeText(this, "Esta cuenta no tiene movimientos", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Movimientos (toca uno para borrarlo)")
                .setItems(textos.toArray(new CharSequence[0]), (d, which) ->
                        DialogoMovimiento.confirmarBorrar(this, dbHelper, idsMovimientos.get(which), this::cargar))
                .setPositiveButton("Cerrar", null)
                .show();
    }
}
