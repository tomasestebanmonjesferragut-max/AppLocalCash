package com.example.mapalocalcash;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ComprasActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private ListView lista;
    private TextView txtTotalAgendado;
    private ArrayList<Integer> idsCompras = new ArrayList<>();
    private ArrayList<Boolean> agendadas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compras);

        dbHelper = new DBHelper(this);
        EditText txtDescripcion = findViewById(R.id.txtDescripcion);
        EditText txtMonto = findViewById(R.id.txtMonto);
        EditText txtFecha = findViewById(R.id.txtFecha);
        CheckBox chkAgendada = findViewById(R.id.chkAgendada);
        Spinner spContacto = findViewById(R.id.spContacto);
        lista = findViewById(R.id.lista);
        txtTotalAgendado = findViewById(R.id.txtTotalAgendado);

        // Contactos para compartir la compra (el primero es "sin contacto")
        ArrayList<Integer> idsContactos = new ArrayList<>();
        ArrayList<String> nombres = new ArrayList<>();
        idsContactos.add(-1);
        nombres.add("Sin contacto");
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT id, nombre FROM contactos ORDER BY nombre", null);
        while (c.moveToNext()) {
            idsContactos.add(c.getInt(0));
            nombres.add(c.getString(1));
        }
        c.close();
        spContacto.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres));

        findViewById(R.id.btnGuardar).setOnClickListener(v -> {
            String descripcion = txtDescripcion.getText().toString();
            Double monto = Monto.leer(txtMonto.getText().toString());
            if (descripcion.isEmpty() || monto == null) {
                Toast.makeText(this, "Falta la descripcion o un monto valido", Toast.LENGTH_SHORT).show();
                return;
            }

            ContentValues valores = new ContentValues();
            valores.put("descripcion", descripcion);
            valores.put("monto", monto);
            valores.put("fecha", txtFecha.getText().toString());
            valores.put("agendada", chkAgendada.isChecked() ? 1 : 0);
            int contactoId = idsContactos.get(spContacto.getSelectedItemPosition());
            if (contactoId != -1) {
                valores.put("contacto_id", contactoId);
            }
            dbHelper.getWritableDatabase().insert("compras", null, valores);

            txtDescripcion.setText("");
            txtMonto.setText("");
            txtFecha.setText("");
            chkAgendada.setChecked(false);
            spContacto.setSelection(0);
            cargar();
        });

        // Al tocar una compra agendada se puede marcar como realizada
        lista.setOnItemClickListener((parent, view, position, id) -> {
            if (!agendadas.get(position)) {
                return;
            }
            new AlertDialog.Builder(this)
                    .setMessage("¿Marcar esta compra como realizada?")
                    .setPositiveButton("Si", (d, w) -> {
                        dbHelper.getWritableDatabase().execSQL("UPDATE compras SET agendada = 0 WHERE id = ?", new Object[]{idsCompras.get(position)});
                        cargar();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        cargar();
    }

    private void cargar() {
        idsCompras.clear();
        agendadas.clear();
        ArrayList<CharSequence> textos = new ArrayList<>();
        double totalAgendado = 0;

        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT co.id, co.descripcion, co.monto, co.fecha, co.agendada, ct.nombre FROM compras co LEFT JOIN contactos ct ON ct.id = co.contacto_id ORDER BY co.agendada DESC, co.id DESC", null);
        while (c.moveToNext()) {
            boolean agendada = c.getInt(4) == 1;
            // Las compras son gastos: monto en rojo
            String antes = (agendada ? "📅 Agendada · " : "") + c.getString(3) + " " + c.getString(1) + "  ";
            CharSequence texto = Monto.conColor(this, antes, "− $" + Math.round(c.getDouble(2)), false);
            if (!c.isNull(5)) {
                texto = TextUtils.concat(texto, "  (con " + c.getString(5) + ")");
            }
            if (agendada) {
                totalAgendado += c.getDouble(2);
            }
            idsCompras.add(c.getInt(0));
            agendadas.add(agendada);
            textos.add(texto);
        }
        c.close();

        txtTotalAgendado.setText("Monto a apartar para compras agendadas: $" + Math.round(totalAgendado));
        lista.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos));
    }
}
