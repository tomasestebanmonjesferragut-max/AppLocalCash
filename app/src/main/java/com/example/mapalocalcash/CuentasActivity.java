package com.example.mapalocalcash;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class CuentasActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private ListView lista;
    private ArrayList<Integer> ids = new ArrayList<>();

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
            String saldo = txtSaldo.getText().toString();

            ContentValues valores = new ContentValues();
            valores.put("nombre", nombre);
            valores.put("saldo", saldo.isEmpty() ? 0 : Double.parseDouble(saldo));
            dbHelper.getWritableDatabase().insert("cuentas", null, valores);

            txtNombre.setText("");
            txtSaldo.setText("");
            cargar();
        });

        lista.setOnItemClickListener((parent, view, position, id) -> verMovimientos(ids.get(position)));

        cargar();
    }

    private void cargar() {
        ids.clear();
        ArrayList<CharSequence> textos = new ArrayList<>();
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT id, nombre, saldo FROM cuentas", null);
        while (c.moveToNext()) {
            ids.add(c.getInt(0));
            double saldo = c.getDouble(2);
            // Saldo en verde si es positivo, en rojo si es negativo
            textos.add(conColor(c.getString(1) + "   ", "$" + Math.round(saldo), saldo >= 0));
        }
        c.close();
        lista.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos));
    }

    private void verMovimientos(int cuentaId) {
        ArrayList<CharSequence> textos = new ArrayList<>();
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT tipo, monto, descripcion, lat FROM movimientos WHERE cuenta_id = ? ORDER BY id DESC",
                new String[]{String.valueOf(cuentaId)});
        while (c.moveToNext()) {
            boolean ingreso = c.getString(0).equals("Ingreso");
            String ubicacion = c.isNull(3) ? "" : " 📍";
            // Ingreso en verde con +, egreso en rojo con −
            String monto = (ingreso ? "+ $" : "− $") + Math.round(c.getDouble(1));
            textos.add(TextUtils.concat(conColor("", monto, ingreso), "  " + c.getString(2) + ubicacion));
        }
        c.close();

        if (textos.isEmpty()) {
            textos.add("Sin movimientos");
        }

        new AlertDialog.Builder(this)
                .setTitle("Movimientos")
                .setItems(textos.toArray(new CharSequence[0]), null)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    // Texto normal seguido de un monto en verde (suma) o rojo (resta)
    private CharSequence conColor(String antes, String monto, boolean verde) {
        SpannableString texto = new SpannableString(antes + monto);
        int color = ContextCompat.getColor(this, verde ? R.color.verde_oscuro : R.color.rojo);
        texto.setSpan(new ForegroundColorSpan(color), antes.length(), texto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        texto.setSpan(new StyleSpan(Typeface.BOLD), antes.length(), texto.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return texto;
    }
}
