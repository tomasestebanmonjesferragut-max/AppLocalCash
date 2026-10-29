package com.example.mapalocalcash;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class DeudasActivity extends AppCompatActivity {

    private DBHelper dbHelper;

    private TextView txtListaProductos;
    private TextView txtTotal;
    private RadioButton rbIguales;
    private LinearLayout panelIguales;
    private LinearLayout panelPersonalizado;
    private EditText txtPersonas;
    private TextView txtCadaUno;
    private AutoCompleteTextView txtNombrePersona;
    private EditText txtMontoPersona;
    private LinearLayout listaPersonas;
    private TextView txtEstadoReparto;
    private RadioButton rbPendientes;
    private RadioButton rbCompletadas;
    private LinearLayout listaOperaciones;
    private LinearLayout filaProducto;
    private View btnAgregarProducto;
    private View btnContinuar;
    private LinearLayout tarjetaDividir;
    private TextView txtTotalDividir;

    private ArrayList<String> productos = new ArrayList<>();
    private ArrayList<Double> precios = new ArrayList<>();
    private ArrayList<String> personas = new ArrayList<>();
    private ArrayList<Double> montosPersonas = new ArrayList<>();
    // Nombre del contacto (en minusculas) -> id, para enlazar con la agenda
    private HashMap<String, Integer> contactos = new HashMap<>();

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
        panelIguales = findViewById(R.id.panelIguales);
        panelPersonalizado = findViewById(R.id.panelPersonalizado);
        txtPersonas = findViewById(R.id.txtPersonas);
        txtCadaUno = findViewById(R.id.txtCadaUno);
        txtNombrePersona = findViewById(R.id.txtNombrePersona);
        txtMontoPersona = findViewById(R.id.txtMontoPersona);
        listaPersonas = findViewById(R.id.listaPersonas);
        txtEstadoReparto = findViewById(R.id.txtEstadoReparto);
        rbPendientes = findViewById(R.id.rbPendientes);
        rbCompletadas = findViewById(R.id.rbCompletadas);
        listaOperaciones = findViewById(R.id.listaOperaciones);
        filaProducto = findViewById(R.id.filaProducto);
        btnAgregarProducto = findViewById(R.id.btnAgregarProducto);
        btnContinuar = findViewById(R.id.btnContinuar);
        tarjetaDividir = findViewById(R.id.tarjetaDividir);
        txtTotalDividir = findViewById(R.id.txtTotalDividir);

        // Sugerir nombres de la agenda de contactos
        ArrayList<String> nombres = new ArrayList<>();
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT id, nombre FROM contactos ORDER BY nombre", null);
        while (c.moveToNext()) {
            contactos.put(c.getString(1).trim().toLowerCase(), c.getInt(0));
            nombres.add(c.getString(1));
        }
        c.close();
        txtNombrePersona.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, nombres));

        btnAgregarProducto.setOnClickListener(v -> {
            String nombre = txtProducto.getText().toString().trim();
            Double precio = Monto.leer(txtPrecio.getText().toString());
            if (nombre.isEmpty() || precio == null) {
                Toast.makeText(this, "Falta el producto o un precio valido", Toast.LENGTH_SHORT).show();
                return;
            }
            productos.add(nombre);
            precios.add(precio);
            txtProducto.setText("");
            txtPrecio.setText("");
            mostrarProductos();
        });

        ((android.widget.RadioGroup) findViewById(R.id.rgReparto)).setOnCheckedChangeListener((g, id) -> {
            panelIguales.setVisibility(rbIguales.isChecked() ? View.VISIBLE : View.GONE);
            panelPersonalizado.setVisibility(rbIguales.isChecked() ? View.GONE : View.VISIBLE);
            actualizarReparto();
        });

        txtPersonas.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                actualizarReparto();
            }
        });

        findViewById(R.id.btnAgregarPersona).setOnClickListener(v -> {
            String nombre = txtNombrePersona.getText().toString().trim();
            Double monto = Monto.leer(txtMontoPersona.getText().toString());
            if (nombre.isEmpty() || monto == null) {
                Toast.makeText(this, "Falta el nombre o un monto valido", Toast.LENGTH_SHORT).show();
                return;
            }
            personas.add(nombre);
            montosPersonas.add(monto);
            txtNombrePersona.setText("");
            txtMontoPersona.setText("");
            mostrarPersonas();
        });

        // Paso 1 -> paso 2: terminar de agregar productos y elegir como dividir
        btnContinuar.setOnClickListener(v -> mostrarPasoDividir(true));
        findViewById(R.id.btnEditarProductos).setOnClickListener(v -> mostrarPasoDividir(false));

        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());

        ((android.widget.RadioGroup) findViewById(R.id.rgFiltro)).setOnCheckedChangeListener((g, id) -> cargarOperaciones());

        mostrarProductos();
        cargarOperaciones();
    }

    private double total() {
        double suma = 0;
        for (double p : precios) {
            suma += p;
        }
        return suma;
    }

    private double sumaPersonas() {
        double suma = 0;
        for (double m : montosPersonas) {
            suma += m;
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
        btnContinuar.setVisibility(!productos.isEmpty() && tarjetaDividir.getVisibility() != View.VISIBLE ? View.VISIBLE : View.GONE);
        actualizarReparto();
    }

    // Paso 1 (productos) o paso 2 (como dividir). En el paso 2 no se pueden cambiar los productos.
    private void mostrarPasoDividir(boolean dividir) {
        tarjetaDividir.setVisibility(dividir ? View.VISIBLE : View.GONE);
        filaProducto.setVisibility(dividir ? View.GONE : View.VISIBLE);
        btnAgregarProducto.setVisibility(dividir ? View.GONE : View.VISIBLE);
        txtTotalDividir.setText("Total a dividir: $" + Math.round(total()));
        mostrarProductos();
    }

    // Lista de personas del reparto personalizado, cada una con un boton para quitarla
    private void mostrarPersonas() {
        listaPersonas.removeAllViews();
        for (int i = 0; i < personas.size(); i++) {
            final int posicion = i;
            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView texto = new TextView(this);
            texto.setText("👤 " + personas.get(i) + "   $" + Math.round(montosPersonas.get(i)));
            texto.setTextColor(ContextCompat.getColor(this, R.color.texto));
            texto.setTextSize(15);
            fila.addView(texto, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            Button quitar = new Button(this, null, android.R.attr.borderlessButtonStyle);
            quitar.setText("✕");
            quitar.setTextColor(ContextCompat.getColor(this, R.color.rojo));
            quitar.setOnClickListener(v -> {
                personas.remove(posicion);
                montosPersonas.remove(posicion);
                mostrarPersonas();
            });
            fila.addView(quitar);

            listaPersonas.addView(fila);
        }
        actualizarReparto();
    }

    // Muestra cuanto paga cada uno (iguales) o si los montos cuadran con el total (personalizado)
    private void actualizarReparto() {
        double total = total();
        if (rbIguales.isChecked()) {
            Integer n = cantidadPersonas();
            if (total > 0 && n != null) {
                txtCadaUno.setText("Cada persona paga $" + Math.round(total / n));
            } else {
                txtCadaUno.setText("");
            }
            txtEstadoReparto.setText("");
            return;
        }

        double falta = total - sumaPersonas();
        if (personas.isEmpty()) {
            txtEstadoReparto.setText("");
        } else if (Math.abs(falta) < 0.5) {
            txtEstadoReparto.setTextColor(ContextCompat.getColor(this, R.color.verde_oscuro));
            txtEstadoReparto.setText("✔ Los montos cuadran con el total");
        } else if (falta > 0) {
            txtEstadoReparto.setTextColor(ContextCompat.getColor(this, R.color.rojo));
            txtEstadoReparto.setText("⚠ No alcanza: falta asignar $" + Math.round(falta));
        } else {
            txtEstadoReparto.setTextColor(ContextCompat.getColor(this, R.color.rojo));
            txtEstadoReparto.setText("⚠ Te pasaste del total por $" + Math.round(-falta));
        }
    }

    // Numero de personas escrito (al menos 1), o null si no es valido
    private Integer cantidadPersonas() {
        try {
            int n = Integer.parseInt(txtPersonas.getText().toString().trim());
            return n >= 1 ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void guardar() {
        if (productos.isEmpty()) {
            Toast.makeText(this, "Agrega al menos un producto", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> nombres = new ArrayList<>();
        ArrayList<Double> montos = new ArrayList<>();

        if (rbIguales.isChecked()) {
            Integer n = cantidadPersonas();
            if (n == null) {
                Toast.makeText(this, "Escribe cuántas personas son", Toast.LENGTH_SHORT).show();
                return;
            }
            for (int i = 1; i <= n; i++) {
                nombres.add("Persona " + i);
                montos.add(total() / n);
            }
        } else {
            if (personas.isEmpty()) {
                Toast.makeText(this, "Agrega al menos una persona", Toast.LENGTH_SHORT).show();
                return;
            }
            double falta = total() - sumaPersonas();
            if (falta >= 0.5) {
                Toast.makeText(this, "No alcanza el total: falta asignar $" + Math.round(falta), Toast.LENGTH_LONG).show();
                return;
            }
            if (falta <= -0.5) {
                Toast.makeText(this, "Los montos pasan el total por $" + Math.round(-falta), Toast.LENGTH_LONG).show();
                return;
            }
            nombres.addAll(personas);
            montos.addAll(montosPersonas);
        }

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

        for (int i = 0; i < nombres.size(); i++) {
            ContentValues deuda = new ContentValues();
            deuda.put("compra_id", compraId);
            deuda.put("nombre", nombres.get(i));
            deuda.put("monto", montos.get(i));
            deuda.put("pagada", 0);
            Integer contactoId = contactos.get(nombres.get(i).toLowerCase());
            if (contactoId != null) {
                deuda.put("contacto_id", contactoId);
            }
            db.insert("deudas", null, deuda);
        }

        Toast.makeText(this, "Gasto compartido guardado", Toast.LENGTH_SHORT).show();
        productos.clear();
        precios.clear();
        personas.clear();
        montosPersonas.clear();
        txtPersonas.setText("");
        mostrarPersonas();
        mostrarPasoDividir(false);
        cargarOperaciones();
    }

    // Operaciones hechas, con filtro: todas, pendientes (falta que alguien pague) o completas
    private void cargarOperaciones() {
        listaOperaciones.removeAllViews();

        String filtro = "";
        if (rbPendientes.isChecked()) {
            filtro = " HAVING SUM(d.pagada) < COUNT(d.id)";
        } else if (rbCompletadas.isChecked()) {
            filtro = " HAVING SUM(d.pagada) = COUNT(d.id)";
        }

        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT co.id, co.descripcion, co.monto, co.fecha, COUNT(d.id), SUM(d.pagada), " +
                        "SUM(CASE WHEN d.pagada = 0 THEN d.monto ELSE 0 END) " +
                        "FROM compras co JOIN deudas d ON d.compra_id = co.id " +
                        "GROUP BY co.id" + filtro + " ORDER BY co.id DESC", null);

        while (c.moveToNext()) {
            int compraId = c.getInt(0);
            String descripcion = c.getString(1);
            int cantidad = c.getInt(4);
            int pagaron = c.getInt(5);
            boolean completa = pagaron == cantidad;

            TextView fila = new TextView(this);
            String estado = completa ? "✅ Completa" : "⏳ Pendiente";
            String detalle = completa
                    ? "Pagaron todos (" + cantidad + ")"
                    : "Pagaron " + pagaron + " de " + cantidad + " · falta cobrar $" + Math.round(c.getDouble(6));
            fila.setText(estado + "\n" + descripcion + "\nTotal $" + Math.round(c.getDouble(2)) + " · " + c.getString(3) + "\n" + detalle);
            fila.setTextColor(ContextCompat.getColor(this, completa ? R.color.verde_oscuro : R.color.rojo));
            fila.setTextSize(15);
            fila.setBackgroundResource(R.drawable.bg_tarjeta);
            fila.setBackgroundTintList(ContextCompat.getColorStateList(this, completa ? R.color.verde_claro : R.color.rojo_claro));
            int p = dp(12);
            fila.setPadding(p, p, p, p);
            fila.setOnClickListener(v -> marcarPagos(compraId, descripcion));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(8);
            listaOperaciones.addView(fila, params);
        }
        c.close();

        if (listaOperaciones.getChildCount() == 0) {
            TextView vacio = new TextView(this);
            vacio.setText("No hay operaciones aquí todavía.");
            vacio.setTextColor(ContextCompat.getColor(this, R.color.texto_suave));
            vacio.setPadding(0, dp(8), 0, 0);
            listaOperaciones.addView(vacio);
        }
    }

    // Marcar quien ya pago su parte de una operacion
    private void marcarPagos(int compraId, String descripcion) {
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<String> textos = new ArrayList<>();
        ArrayList<Boolean> pagadas = new ArrayList<>();
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT d.id, COALESCE(d.nombre, ct.nombre, 'Persona'), d.monto, d.pagada " +
                        "FROM deudas d LEFT JOIN contactos ct ON ct.id = d.contacto_id WHERE d.compra_id = ?",
                new String[]{String.valueOf(compraId)});
        while (c.moveToNext()) {
            ids.add(c.getInt(0));
            textos.add(c.getString(1) + "   $" + Math.round(c.getDouble(2)));
            pagadas.add(c.getInt(3) == 1);
        }
        c.close();

        boolean[] marcados = new boolean[pagadas.size()];
        for (int i = 0; i < marcados.length; i++) {
            marcados[i] = pagadas.get(i);
        }

        new AlertDialog.Builder(this)
                .setTitle("¿Quién ya pagó? (" + descripcion + ")")
                .setMultiChoiceItems(textos.toArray(new String[0]), marcados, (d, which, isChecked) -> marcados[which] = isChecked)
                .setPositiveButton("Guardar", (d, w) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    for (int i = 0; i < ids.size(); i++) {
                        db.execSQL("UPDATE deudas SET pagada = ? WHERE id = ?", new Object[]{marcados[i] ? 1 : 0, ids.get(i)});
                    }
                    cargarOperaciones();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private int dp(int valor) {
        return (int) (valor * getResources().getDisplayMetrics().density);
    }
}
