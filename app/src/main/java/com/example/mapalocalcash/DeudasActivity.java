package com.example.mapalocalcash;

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

import com.example.mapalocalcash.dao.CompraDAO;
import com.example.mapalocalcash.dao.ContactoDAO;
import com.example.mapalocalcash.dao.DeudaDAO;
import com.example.mapalocalcash.dao.ICompra;
import com.example.mapalocalcash.model.Contacto;
import com.example.mapalocalcash.model.Deuda;
import com.example.mapalocalcash.model.GastoCompartido;
import com.example.mapalocalcash.model.Producto;
import com.example.mapalocalcash.repository.CompraRepository;

import java.util.ArrayList;
import java.util.HashMap;

public class DeudasActivity extends AppCompatActivity {

    private CompraDAO compraDAO;
    private DeudaDAO deudaDAO;
    private CompraRepository compraRepository;

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
    private LinearLayout listaProductos;
    private LinearLayout panelEditando;
    private TextView txtEditando;
    private Button btnGuardar;
    private android.widget.ScrollView scroll;
    // id de la operacion que se esta editando (null = operacion nueva)
    private Integer editandoId = null;

    private ArrayList<Producto> productos = new ArrayList<>();
    private ArrayList<String> personas = new ArrayList<>();
    private ArrayList<Double> montosPersonas = new ArrayList<>();
    // Nombre del contacto (en minusculas) -> id, para enlazar con la agenda
    private HashMap<String, Integer> contactos = new HashMap<>();
    private ArrayList<String> nombresContactos = new ArrayList<>();
    // Contactos elegidos para partes iguales (el resto se guarda como "Persona N")
    private ArrayList<String> nombresIguales = new ArrayList<>();
    private TextView txtNombresIguales;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deudas);

        compraDAO = new CompraDAO(this);
        deudaDAO = new DeudaDAO(this);
        compraRepository = new CompraRepository(this);
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
        listaProductos = findViewById(R.id.listaProductos);
        panelEditando = findViewById(R.id.panelEditando);
        txtEditando = findViewById(R.id.txtEditando);
        btnGuardar = findViewById(R.id.btnGuardar);
        scroll = findViewById(R.id.scroll);
        txtNombresIguales = findViewById(R.id.txtNombresIguales);
        findViewById(R.id.btnElegirContactos).setOnClickListener(v -> elegirContactosIguales());

        // Sugerir nombres de la agenda de contactos
        ArrayList<String> nombres = new ArrayList<>();
        for (Contacto contacto : new ContactoDAO(this).listar()) {
            contactos.put(contacto.getNombre().trim().toLowerCase(), contacto.getId());
            nombres.add(contacto.getNombre());
            nombresContactos.add(contacto.getNombre());
        }
        txtNombrePersona.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, nombres));

        btnAgregarProducto.setOnClickListener(v -> {
            String nombre = txtProducto.getText().toString().trim();
            Double precio = Monto.leer(txtPrecio.getText().toString());
            if (nombre.isEmpty() || precio == null) {
                Toast.makeText(this, "Falta el producto o un precio valido", Toast.LENGTH_SHORT).show();
                return;
            }
            productos.add(new Producto(nombre, precio));
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

        btnGuardar.setOnClickListener(v -> guardar());
        findViewById(R.id.btnCancelarEdicion).setOnClickListener(v -> terminarEdicion());

        ((android.widget.RadioGroup) findViewById(R.id.rgFiltro)).setOnCheckedChangeListener((g, id) -> cargarOperaciones());

        mostrarProductos();
        cargarOperaciones();
    }

    private double total() {
        double suma = 0;
        for (Producto p : productos) {
            suma += p.getPrecio();
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

    // Lista de productos: en el paso 1 se puede tocar uno para cambiar nombre/precio o quitarlo con ✕
    private void mostrarProductos() {
        boolean dividiendo = tarjetaDividir.getVisibility() == View.VISIBLE;
        txtListaProductos.setVisibility(productos.isEmpty() ? View.VISIBLE : View.GONE);
        listaProductos.removeAllViews();
        for (int i = 0; i < productos.size(); i++) {
            final int posicion = i;
            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView texto = new TextView(this);
            texto.setText("• " + productos.get(i).getNombre() + "   $" + Monto.miles(productos.get(i).getPrecio()) + (dividiendo ? "" : "   ✏️"));
            texto.setTextColor(ContextCompat.getColor(this, R.color.texto));
            texto.setTextSize(15);
            texto.setPadding(0, dp(6), 0, dp(6));
            if (!dividiendo) {
                texto.setOnClickListener(v -> editarProducto(posicion));
            }
            fila.addView(texto, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            if (!dividiendo) {
                Button quitar = new Button(this, null, android.R.attr.borderlessButtonStyle);
                quitar.setText("✕");
                quitar.setTextColor(ContextCompat.getColor(this, R.color.rojo));
                quitar.setOnClickListener(v -> {
                    productos.remove(posicion);
                    mostrarProductos();
                });
                fila.addView(quitar);
            }
            listaProductos.addView(fila);
        }
        txtTotal.setText("Total: $" + Monto.miles(total()));
        btnContinuar.setVisibility(!productos.isEmpty() && tarjetaDividir.getVisibility() != View.VISIBLE ? View.VISIBLE : View.GONE);
        actualizarReparto();
    }

    // Paso 1 (productos) o paso 2 (como dividir). En el paso 2 no se pueden cambiar los productos.
    private void mostrarPasoDividir(boolean dividir) {
        tarjetaDividir.setVisibility(dividir ? View.VISIBLE : View.GONE);
        filaProducto.setVisibility(dividir ? View.GONE : View.VISIBLE);
        btnAgregarProducto.setVisibility(dividir ? View.GONE : View.VISIBLE);
        txtTotalDividir.setText("Total a dividir: $" + Monto.miles(total()));
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
            texto.setText("👤 " + personas.get(i) + "   $" + Monto.miles(montosPersonas.get(i)));
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
                txtCadaUno.setText("Cada persona paga $" + Monto.miles(total / n));
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
            txtEstadoReparto.setText("⚠ No alcanza: falta asignar $" + Monto.miles(falta));
        } else {
            txtEstadoReparto.setTextColor(ContextCompat.getColor(this, R.color.rojo));
            txtEstadoReparto.setText("⚠ Te pasaste del total por $" + Monto.miles(-falta));
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
                nombres.add(i <= nombresIguales.size() ? nombresIguales.get(i - 1) : "Persona " + i);
                montos.add(total() / n);
            }
        } else {
            if (personas.isEmpty()) {
                Toast.makeText(this, "Agrega al menos una persona", Toast.LENGTH_SHORT).show();
                return;
            }
            double falta = total() - sumaPersonas();
            if (falta >= 0.5) {
                Toast.makeText(this, "No alcanza el total: falta asignar $" + Monto.miles(falta), Toast.LENGTH_LONG).show();
                return;
            }
            if (falta <= -0.5) {
                Toast.makeText(this, "Los montos pasan el total por $" + Monto.miles(-falta), Toast.LENGTH_LONG).show();
                return;
            }
            nombres.addAll(personas);
            montos.addAll(montosPersonas);
        }

        compraRepository.guardarGastoCompartido(editandoId, productos, nombres, montos, contactos);

        Toast.makeText(this, editandoId == null ? "Gasto compartido guardado" : "Cambios guardados", Toast.LENGTH_SHORT).show();
        terminarEdicion();
        cargarOperaciones();
    }

    // Deja el formulario vacio, en el paso 1, listo para una operacion nueva
    private void terminarEdicion() {
        editandoId = null;
        productos.clear();
        personas.clear();
        montosPersonas.clear();
        txtPersonas.setText("");
        nombresIguales.clear();
        txtNombresIguales.setText("");
        rbIguales.setChecked(true);
        panelEditando.setVisibility(View.GONE);
        btnGuardar.setText("Guardar gasto compartido");
        mostrarPersonas();
        mostrarPasoDividir(false);
    }

    // Partes iguales: elegir contactos de la agenda. La cantidad de personas parte en los elegidos
    // y se puede subir (los que sobran quedan como "Persona N").
    private void elegirContactosIguales() {
        if (nombresContactos.isEmpty()) {
            Toast.makeText(this, "No tienes contactos. Agrégalos en 👥 Contactos", Toast.LENGTH_LONG).show();
            return;
        }
        boolean[] marcados = new boolean[nombresContactos.size()];
        for (int i = 0; i < marcados.length; i++) {
            marcados[i] = nombresIguales.contains(nombresContactos.get(i));
        }
        new AlertDialog.Builder(this)
                .setTitle("¿Con quiénes divides?")
                .setMultiChoiceItems(nombresContactos.toArray(new String[0]), marcados, (d, which, isChecked) -> marcados[which] = isChecked)
                .setPositiveButton("Listo", (d, w) -> {
                    nombresIguales.clear();
                    for (int i = 0; i < marcados.length; i++) {
                        if (marcados[i]) {
                            nombresIguales.add(nombresContactos.get(i));
                        }
                    }
                    txtNombresIguales.setText(nombresIguales.isEmpty() ? "" : "👥 " + String.join(", ", nombresIguales));
                    Integer n = cantidadPersonas();
                    if (n == null || n < nombresIguales.size()) {
                        txtPersonas.setText(String.valueOf(nombresIguales.size()));
                    }
                    actualizarReparto();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Cambiar nombre o precio de un producto de la lista
    private void editarProducto(int posicion) {
        LinearLayout vista = new LinearLayout(this);
        vista.setOrientation(LinearLayout.VERTICAL);
        vista.setPadding(dp(20), dp(8), dp(20), 0);

        EditText nombre = new EditText(this);
        nombre.setHint("Producto");
        nombre.setText(productos.get(posicion).getNombre());
        vista.addView(nombre);

        EditText precio = new EditText(this);
        precio.setHint("Precio $");
        precio.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        precio.setText(String.valueOf(Math.round(productos.get(posicion).getPrecio())));
        vista.addView(precio);

        new AlertDialog.Builder(this)
                .setTitle("Editar producto")
                .setView(vista)
                .setPositiveButton("Guardar", (d, w) -> {
                    Double nuevoPrecio = Monto.leer(precio.getText().toString());
                    String nuevoNombre = nombre.getText().toString().trim();
                    if (nuevoNombre.isEmpty() || nuevoPrecio == null) {
                        Toast.makeText(this, "Falta el producto o un precio valido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    productos.get(posicion).setNombre(nuevoNombre);
                    productos.get(posicion).setPrecio(nuevoPrecio);
                    mostrarProductos();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Menu al tocar una operacion guardada
    private void opcionesOperacion(int compraId, String descripcion) {
        String[] opciones = {"✅ Marcar quién pagó", "✏️ Editar (productos, precios, división)", "🗑️ Borrar"};
        new AlertDialog.Builder(this)
                .setTitle(descripcion)
                .setItems(opciones, (d, which) -> {
                    if (which == 0) {
                        marcarPagos(compraId, descripcion);
                    } else if (which == 1) {
                        editarOperacion(compraId, descripcion);
                    } else {
                        borrarOperacion(compraId, descripcion);
                    }
                })
                .show();
    }

    private void borrarOperacion(int compraId, String descripcion) {
        new AlertDialog.Builder(this)
                .setTitle("¿Borrar esta operación?")
                .setMessage(descripcion + "\n\nSe borran sus productos y lo que cada persona debía. No se puede deshacer.")
                .setPositiveButton("Borrar", (d, w) -> {
                    compraRepository.borrar(compraId);
                    if (editandoId != null && editandoId == compraId) {
                        terminarEdicion();
                    }
                    Toast.makeText(this, "Operación borrada", Toast.LENGTH_SHORT).show();
                    cargarOperaciones();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Carga una operacion guardada en el formulario para cambiarla
    private void editarOperacion(int compraId, String descripcion) {
        terminarEdicion();
        productos.addAll(compraRepository.productosDe(compraId));

        ArrayList<String> nombres = new ArrayList<>();
        ArrayList<Double> montos = new ArrayList<>();
        for (Deuda deuda : deudaDAO.listarPorCompra(compraId)) {
            nombres.add(deuda.getNombre());
            montos.add(deuda.getMonto());
        }

        // Si todos son "Persona N" con el mismo monto, era partes iguales
        boolean iguales = !nombres.isEmpty();
        for (int i = 0; i < nombres.size(); i++) {
            if (!nombres.get(i).matches("Persona \\d+") || Math.abs(montos.get(i) - montos.get(0)) > 0.01) {
                iguales = false;
            }
        }
        if (iguales) {
            rbIguales.setChecked(true);
            txtPersonas.setText(String.valueOf(nombres.size()));
        } else {
            ((RadioButton) findViewById(R.id.rbPersonalizado)).setChecked(true);
            personas.addAll(nombres);
            montosPersonas.addAll(montos);
        }

        editandoId = compraId;
        txtEditando.setText("✏️ Editando: " + descripcion);
        panelEditando.setVisibility(View.VISIBLE);
        btnGuardar.setText("Guardar cambios");
        mostrarPersonas();
        mostrarPasoDividir(false);
        scroll.smoothScrollTo(0, 0);
    }

    // Operaciones hechas, con filtro: todas, pendientes (falta que alguien pague) o completas
    private void cargarOperaciones() {
        listaOperaciones.removeAllViews();

        int filtro = ICompra.TODAS;
        if (rbPendientes.isChecked()) {
            filtro = ICompra.PENDIENTES;
        } else if (rbCompletadas.isChecked()) {
            filtro = ICompra.COMPLETAS;
        }

        for (GastoCompartido gasto : compraDAO.listarGastosCompartidos(filtro)) {
            int compraId = gasto.getCompraId();
            String descripcion = gasto.getDescripcion();
            boolean completa = gasto.estaCompleto();

            TextView fila = new TextView(this);
            String estado = completa ? "✅ Completa" : "⏳ Pendiente";
            String detalle = completa
                    ? "Pagaron todos (" + gasto.getPersonas() + ")"
                    : "Pagaron " + gasto.getPagaron() + " de " + gasto.getPersonas() + " · falta cobrar $" + Monto.miles(gasto.getFaltaCobrar());
            fila.setText(estado + "\n" + descripcion + "\nTotal $" + Monto.miles(gasto.getTotal()) + " · " + gasto.getFecha() + "\n" + detalle);
            fila.setTextColor(ContextCompat.getColor(this, completa ? R.color.verde_oscuro : R.color.rojo));
            fila.setTextSize(15);
            fila.setBackgroundResource(R.drawable.bg_tarjeta);
            fila.setBackgroundTintList(ContextCompat.getColorStateList(this, completa ? R.color.verde_claro : R.color.rojo_claro));
            int p = dp(12);
            fila.setPadding(p, p, p, p);
            fila.setOnClickListener(v -> opcionesOperacion(compraId, descripcion));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(8);
            listaOperaciones.addView(fila, params);
        }

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
        ArrayList<Deuda> deudas = deudaDAO.listarPorCompra(compraId);
        ArrayList<String> textos = new ArrayList<>();
        boolean[] marcados = new boolean[deudas.size()];
        for (int i = 0; i < deudas.size(); i++) {
            textos.add(deudas.get(i).getNombre() + "   $" + Monto.miles(deudas.get(i).getMonto()));
            marcados[i] = deudas.get(i).isPagada();
        }

        new AlertDialog.Builder(this)
                .setTitle("¿Quién ya pagó? (" + descripcion + ")")
                .setMultiChoiceItems(textos.toArray(new String[0]), marcados, (d, which, isChecked) -> marcados[which] = isChecked)
                .setPositiveButton("Guardar", (d, w) -> {
                    for (int i = 0; i < deudas.size(); i++) {
                        deudaDAO.marcarPagada(deudas.get(i).getId(), marcados[i]);
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
