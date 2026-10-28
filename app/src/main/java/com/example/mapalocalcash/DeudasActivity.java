package com.example.mapalocalcash;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DeudasActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private ListView listaDeudas;
    private TextView txtListaProductos;
    private TextView txtTotal;
    private RadioButton rbIguales;

    private ArrayList<String> productos = new ArrayList<>();
    private ArrayList<Double> precios = new ArrayList<>();
    private ArrayList<Integer> idsDeudas = new ArrayList<>();
    private ArrayList<Integer> idsComprasDeudas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deudas);

        dbHelper = new DBHelper(this);
        EditText txtProducto = findViewById(R.id.txtProducto);
        EditText txtPrecio = findViewById(R.id.txtPrecio);
        txtListaProductos = findViewById(R.id.txtListaProductos);
        txtTotal = findViewById(R.id.txtTotal);
        rbIguales = findViewById(R.id.rbIguales);
        listaDeudas = findViewById(R.id.listaDeudas);

        findViewById(R.id.btnAgregarProducto).setOnClickListener(v -> {
            String nombre = txtProducto.getText().toString();
            String precio = txtPrecio.getText().toString();
            if (nombre.isEmpty() || precio.isEmpty()) {
                Toast.makeText(this, "Falta el producto o el precio", Toast.LENGTH_SHORT).show();
                return;
            }
            productos.add(nombre);
            precios.add(Double.parseDouble(precio));
            txtProducto.setText("");
            txtPrecio.setText("");
            mostrarProductos();
        });

        findViewById(R.id.btnDividir).setOnClickListener(v -> elegirContactos());

        listaDeudas.setOnItemClickListener((parent, view, position, id) -> verDeuda(position));

        cargarDeudas();
    }

    private double total() {
        double suma = 0;
        for (double p : precios) {
            suma += p;
        }
        return suma;
    }

    private void mostrarProductos() {
        if (productos.isEmpty()) {
            txtListaProductos.setText("Sin productos");
        } else {
            StringBuilder texto = new StringBuilder();
            for (int i = 0; i < productos.size(); i++) {
                texto.append("• ").append(productos.get(i)).append("  $").append(Math.round(precios.get(i))).append("\n");
            }
            txtListaProductos.setText(texto.toString().trim());
        }
        txtTotal.setText("Total: $" + Math.round(total()));
    }

    // Paso 1: elegir con quienes se divide
    private void elegirContactos() {
        if (productos.isEmpty()) {
            Toast.makeText(this, "Agrega al menos un producto", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Integer> idsContactos = new ArrayList<>();
        ArrayList<String> nombres = new ArrayList<>();
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT id, nombre FROM contactos ORDER BY nombre", null);
        while (c.moveToNext()) {
            idsContactos.add(c.getInt(0));
            nombres.add(c.getString(1));
        }
        c.close();

        if (idsContactos.isEmpty()) {
            Toast.makeText(this, "Primero agrega contactos", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean[] marcados = new boolean[nombres.size()];
        new AlertDialog.Builder(this)
                .setTitle("Dividir con")
                .setMultiChoiceItems(nombres.toArray(new String[0]), marcados, (d, which, isChecked) -> marcados[which] = isChecked)
                .setPositiveButton("Siguiente", (d, w) -> {
                    ArrayList<Integer> elegidos = new ArrayList<>();
                    ArrayList<String> nombresElegidos = new ArrayList<>();
                    for (int i = 0; i < marcados.length; i++) {
                        if (marcados[i]) {
                            elegidos.add(idsContactos.get(i));
                            nombresElegidos.add(nombres.get(i));
                        }
                    }
                    if (elegidos.isEmpty()) {
                        Toast.makeText(this, "Elige al menos un contacto", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (rbIguales.isChecked()) {
                        // Partes iguales entre los contactos elegidos y yo
                        double parte = total() / (elegidos.size() + 1);
                        ArrayList<Double> montos = new ArrayList<>();
                        for (int i = 0; i < elegidos.size(); i++) {
                            montos.add(parte);
                        }
                        guardar(elegidos, montos);
                    } else {
                        montosPersonalizados(elegidos, nombresElegidos);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Paso 2 (personalizado): escribir cuanto debe cada uno
    private void montosPersonalizados(ArrayList<Integer> elegidos, ArrayList<String> nombresElegidos) {
        LinearLayout vista = new LinearLayout(this);
        vista.setOrientation(LinearLayout.VERTICAL);
        vista.setPadding(48, 16, 48, 0);

        ArrayList<EditText> campos = new ArrayList<>();
        for (String nombre : nombresElegidos) {
            EditText campo = new EditText(this);
            campo.setHint("Cuanto debe " + nombre);
            campo.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            vista.addView(campo);
            campos.add(campo);
        }

        new AlertDialog.Builder(this)
                .setTitle("Total $" + Math.round(total()) + " - lo que sobra lo pagas tu")
                .setView(vista)
                .setPositiveButton("Guardar", (d, w) -> {
                    ArrayList<Double> montos = new ArrayList<>();
                    double suma = 0;
                    for (EditText campo : campos) {
                        String texto = campo.getText().toString();
                        double monto = texto.isEmpty() ? 0 : Double.parseDouble(texto);
                        montos.add(monto);
                        suma += monto;
                    }
                    if (suma > total()) {
                        Toast.makeText(this, "Los montos suman mas que el total", Toast.LENGTH_LONG).show();
                        return;
                    }
                    guardar(elegidos, montos);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Guarda la compra grupal, sus productos y la deuda de cada contacto
    private void guardar(ArrayList<Integer> elegidos, ArrayList<Double> montos) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues compra = new ContentValues();
        compra.put("descripcion", String.join(", ", productos));
        compra.put("monto", total());
        compra.put("fecha", new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
        compra.put("agendada", 0);
        long compraId = db.insert("compras", null, compra);

        for (int i = 0; i < productos.size(); i++) {
            ContentValues producto = new ContentValues();
            producto.put("compra_id", compraId);
            producto.put("nombre", productos.get(i));
            producto.put("precio", precios.get(i));
            db.insert("compra_productos", null, producto);
        }

        for (int i = 0; i < elegidos.size(); i++) {
            ContentValues deuda = new ContentValues();
            deuda.put("compra_id", compraId);
            deuda.put("contacto_id", elegidos.get(i));
            deuda.put("monto", montos.get(i));
            deuda.put("pagada", 0);
            db.insert("deudas", null, deuda);
        }

        Toast.makeText(this, "Gasto compartido guardado", Toast.LENGTH_SHORT).show();
        productos.clear();
        precios.clear();
        mostrarProductos();
        cargarDeudas();
    }

    private void cargarDeudas() {
        idsDeudas.clear();
        idsComprasDeudas.clear();
        ArrayList<String> textos = new ArrayList<>();
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT d.id, ct.nombre, d.monto, co.descripcion, co.id FROM deudas d JOIN contactos ct ON ct.id = d.contacto_id JOIN compras co ON co.id = d.compra_id WHERE d.pagada = 0 ORDER BY d.id DESC", null);
        while (c.moveToNext()) {
            idsDeudas.add(c.getInt(0));
            idsComprasDeudas.add(c.getInt(4));
            textos.add(c.getString(1) + " debe $" + Math.round(c.getDouble(2)) + "  (" + c.getString(3) + ")");
        }
        c.close();
        listaDeudas.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos));
    }

    // Muestra los productos de la compra y permite marcar la deuda como pagada
    private void verDeuda(int position) {
        StringBuilder detalle = new StringBuilder("Productos:\n");
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT nombre, precio FROM compra_productos WHERE compra_id = ?",
                new String[]{String.valueOf(idsComprasDeudas.get(position))});
        while (c.moveToNext()) {
            detalle.append("• ").append(c.getString(0)).append("  $").append(Math.round(c.getDouble(1))).append("\n");
        }
        c.close();
        detalle.append("\n¿Esta deuda ya fue pagada?");

        new AlertDialog.Builder(this)
                .setTitle("Detalle")
                .setMessage(detalle.toString())
                .setPositiveButton("Si, pagada", (d, w) -> {
                    dbHelper.getWritableDatabase().execSQL("UPDATE deudas SET pagada = 1 WHERE id = ?", new Object[]{idsDeudas.get(position)});
                    cargarDeudas();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
