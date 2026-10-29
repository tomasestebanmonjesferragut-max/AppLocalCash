package com.example.mapalocalcash;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ContactosActivity extends AppCompatActivity {

    // Deudas pendientes de un contacto: las anotadas a mano (contacto_id) y las de gastos
    // compartidos donde aparece con su nombre
    private static final String DEUDAS_DEL_CONTACTO =
            " FROM deudas d LEFT JOIN compras co ON co.id = d.compra_id" +
            " WHERE d.pagada = 0 AND (d.contacto_id = ? OR (d.contacto_id IS NULL AND LOWER(d.nombre) = LOWER(?)))";

    private DBHelper dbHelper;
    private ListView lista;
    private ArrayList<Integer> ids = new ArrayList<>();
    private ArrayList<String> nombres = new ArrayList<>();
    private ArrayList<String> telefonos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactos);

        dbHelper = new DBHelper(this);
        EditText txtNombre = findViewById(R.id.txtNombre);
        EditText txtTelefono = findViewById(R.id.txtTelefono);
        EditText txtMeDebe = findViewById(R.id.txtMeDebe);
        EditText txtMotivo = findViewById(R.id.txtMotivo);
        lista = findViewById(R.id.lista);

        findViewById(R.id.btnAgregar).setOnClickListener(v -> {
            String nombre = txtNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                Toast.makeText(this, "Escribe un nombre", Toast.LENGTH_SHORT).show();
                return;
            }
            String textoDebe = txtMeDebe.getText().toString().trim();
            Double meDebe = textoDebe.isEmpty() ? null : Monto.leer(textoDebe);
            if (!textoDebe.isEmpty() && meDebe == null) {
                Toast.makeText(this, "Escribe un monto valido", Toast.LENGTH_SHORT).show();
                return;
            }

            ContentValues valores = new ContentValues();
            valores.put("nombre", nombre);
            valores.put("telefono", txtTelefono.getText().toString().trim());
            long id = dbHelper.getWritableDatabase().insert("contactos", null, valores);

            if (meDebe != null && meDebe > 0) {
                agregarDeuda((int) id, nombre, meDebe, txtMotivo.getText().toString().trim());
            }

            txtNombre.setText("");
            txtTelefono.setText("");
            txtMeDebe.setText("");
            txtMotivo.setText("");
            cargar();
        });

        lista.setOnItemClickListener((parent, view, position, id) -> opciones(position));

        cargar();
    }

    private void cargar() {
        ids.clear();
        nombres.clear();
        telefonos.clear();
        ArrayList<CharSequence> textos = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, nombre, telefono FROM contactos ORDER BY nombre", null);
        while (c.moveToNext()) {
            int id = c.getInt(0);
            String nombre = c.getString(1);
            String telefono = c.isNull(2) ? "" : c.getString(2);
            ids.add(id);
            nombres.add(nombre);
            telefonos.add(telefono);

            String antes = "👤 " + nombre + (telefono.isEmpty() ? "" : "\n📞 " + telefono) + "\n";
            double debe = totalQueDebe(id, nombre);
            textos.add(debe > 0
                    ? Monto.conColor(this, antes, "Te debe + $" + Math.round(debe), true)
                    : antes + "✔ Al día");
        }
        c.close();
        lista.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos));
    }

    private double totalQueDebe(int id, String nombre) {
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT IFNULL(SUM(d.monto), 0)" + DEUDAS_DEL_CONTACTO,
                new String[]{String.valueOf(id), nombre});
        c.moveToFirst();
        double total = c.getDouble(0);
        c.close();
        return total;
    }

    // Deuda anotada a mano (no viene de un gasto compartido)
    private void agregarDeuda(int contactoId, String nombre, double monto, String motivo) {
        ContentValues deuda = new ContentValues();
        deuda.put("contacto_id", contactoId);
        deuda.put("nombre", nombre);
        deuda.put("monto", monto);
        deuda.put("pagada", 0);
        deuda.put("detalle", motivo.isEmpty() ? "Préstamo" : motivo);
        dbHelper.getWritableDatabase().insert("deudas", null, deuda);
    }

    private void opciones(int posicion) {
        int id = ids.get(posicion);
        String nombre = nombres.get(posicion);
        String telefono = telefonos.get(posicion);

        String[] opciones = {"📞 Llamar", "➕ Anotar monto que me debe", "✅ Ver deudas y marcar pagos", "🗑️ Borrar contacto"};
        new AlertDialog.Builder(this)
                .setTitle(nombre)
                .setItems(opciones, (d, which) -> {
                    if (which == 0) {
                        if (telefono.isEmpty()) {
                            Toast.makeText(this, "Este contacto no tiene número", Toast.LENGTH_SHORT).show();
                        } else {
                            // Abre el marcador del telefono con el numero (no llama solo)
                            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + telefono)));
                        }
                    } else if (which == 1) {
                        anotarDeuda(id, nombre);
                    } else if (which == 2) {
                        verDeudas(id, nombre);
                    } else {
                        borrar(id, nombre);
                    }
                })
                .show();
    }

    private void anotarDeuda(int id, String nombre) {
        LinearLayout vista = new LinearLayout(this);
        vista.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        vista.setPadding(p, p / 2, p, 0);

        EditText monto = new EditText(this);
        monto.setHint("Monto $");
        monto.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        vista.addView(monto);

        EditText motivo = new EditText(this);
        motivo.setHint("Motivo (ej: préstamo, entrada)");
        vista.addView(motivo);

        new AlertDialog.Builder(this)
                .setTitle("¿Cuánto te debe " + nombre + "?")
                .setView(vista)
                .setPositiveButton("Guardar", (d, w) -> {
                    Double valor = Monto.leer(monto.getText().toString());
                    if (valor == null || valor <= 0) {
                        Toast.makeText(this, "Escribe un monto valido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    agregarDeuda(id, nombre, valor, motivo.getText().toString().trim());
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Lista lo que debe (anotado a mano y de gastos compartidos) para marcar lo que ya pago
    private void verDeudas(int id, String nombre) {
        ArrayList<Integer> idsDeudas = new ArrayList<>();
        ArrayList<CharSequence> textos = new ArrayList<>();
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT d.id, d.monto, COALESCE(co.descripcion, d.detalle, 'Deuda')" + DEUDAS_DEL_CONTACTO,
                new String[]{String.valueOf(id), nombre});
        while (c.moveToNext()) {
            idsDeudas.add(c.getInt(0));
            textos.add(TextUtils.concat(Monto.conColor(this, "", "+ $" + Math.round(c.getDouble(1)), true), "  " + c.getString(2)));
        }
        c.close();

        if (idsDeudas.isEmpty()) {
            Toast.makeText(this, nombre + " no te debe nada", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean[] pagadas = new boolean[idsDeudas.size()];
        new AlertDialog.Builder(this)
                .setTitle("Marca lo que " + nombre + " ya pagó")
                .setMultiChoiceItems(textos.toArray(new CharSequence[0]), pagadas, (d, which, isChecked) -> pagadas[which] = isChecked)
                .setPositiveButton("Guardar", (d, w) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    for (int i = 0; i < idsDeudas.size(); i++) {
                        if (pagadas[i]) {
                            db.execSQL("UPDATE deudas SET pagada = 1 WHERE id = ?", new Object[]{idsDeudas.get(i)});
                        }
                    }
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void borrar(int id, String nombre) {
        new AlertDialog.Builder(this)
                .setTitle("¿Borrar a " + nombre + "?")
                .setMessage("Se borran también las deudas que anotaste a mano. Lo de los gastos compartidos se mantiene con su nombre.")
                .setPositiveButton("Borrar", (d, w) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    String[] arg = {String.valueOf(id)};
                    db.delete("deudas", "contacto_id = ? AND compra_id IS NULL", arg);
                    db.execSQL("UPDATE deudas SET contacto_id = NULL WHERE contacto_id = ?", new Object[]{id});
                    db.delete("contactos", "id = ?", arg);
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
