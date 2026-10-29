package com.example.mapalocalcash;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mapalocalcash.dao.CuentaDAO;
import com.example.mapalocalcash.dao.MovimientoDAO;
import com.example.mapalocalcash.model.Cuenta;
import com.example.mapalocalcash.model.Movimiento;
import com.example.mapalocalcash.repository.MovimientoRepository;

import java.util.ArrayList;

public class CuentasActivity extends AppCompatActivity {

    private CuentaDAO cuentaDAO;
    private MovimientoDAO movimientoDAO;
    private ListView lista;
    private ArrayList<Cuenta> cuentas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cuentas);

        cuentaDAO = new CuentaDAO(this);
        movimientoDAO = new MovimientoDAO(this);
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

            cuentaDAO.crear(new Cuenta(nombre, saldo));

            txtNombre.setText("");
            txtSaldo.setText("");
            cargar();
        });

        lista.setOnItemClickListener((parent, view, position, id) -> opciones(cuentas.get(position)));

        cargar();
    }

    private void cargar() {
        cuentas = cuentaDAO.listar();
        ArrayList<CharSequence> textos = new ArrayList<>();
        for (Cuenta cuenta : cuentas) {
            // Saldo en verde si es positivo, en rojo si es negativo
            textos.add(Monto.conColor(this, cuenta.getNombre() + "   ", Monto.dinero(cuenta.getSaldo()), cuenta.getSaldo() >= 0));
        }
        lista.setAdapter(new ArrayAdapter<>(this, R.layout.item_lista, textos));
    }

    private void opciones(Cuenta cuenta) {
        String[] opciones = {"📋 Ver movimientos", "🗑️ Borrar cuenta"};
        new AlertDialog.Builder(this)
                .setTitle(cuenta.getNombre())
                .setItems(opciones, (d, which) -> {
                    if (which == 0) {
                        verMovimientos(cuenta.getId());
                    } else {
                        borrarCuenta(cuenta);
                    }
                })
                .show();
    }

    private void borrarCuenta(Cuenta cuenta) {
        new AlertDialog.Builder(this)
                .setTitle("¿Borrar la cuenta " + cuenta.getNombre() + "?")
                .setMessage("Se borran también todos sus movimientos (y sus marcadores del mapa). No se puede deshacer.")
                .setPositiveButton("Borrar", (d, w) -> {
                    new MovimientoRepository(this).borrarCuenta(cuenta.getId());
                    Toast.makeText(this, "Cuenta borrada", Toast.LENGTH_SHORT).show();
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Movimientos de la cuenta. Tocar uno permite borrarlo (el saldo se devuelve).
    private void verMovimientos(int cuentaId) {
        ArrayList<Movimiento> movimientos = movimientoDAO.listarPorCuenta(cuentaId);
        if (movimientos.isEmpty()) {
            Toast.makeText(this, "Esta cuenta no tiene movimientos", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<CharSequence> textos = new ArrayList<>();
        for (Movimiento m : movimientos) {
            String ubicacion = m.tieneUbicacion() ? " 📍" : "";
            // Ingreso en verde con +, egreso en rojo con −
            String monto = (m.esIngreso() ? "+ $" : "− $") + Monto.miles(m.getMonto());
            textos.add(TextUtils.concat(Monto.conColor(this, "", monto, m.esIngreso()), "  " + m.getDescripcion() + ubicacion));
        }

        new AlertDialog.Builder(this)
                .setTitle("Movimientos (toca uno para borrarlo)")
                .setItems(textos.toArray(new CharSequence[0]), (d, which) ->
                        DialogoMovimiento.confirmarBorrar(this, movimientos.get(which).getId(), this::cargar))
                .setPositiveButton("Cerrar", null)
                .show();
    }
}
