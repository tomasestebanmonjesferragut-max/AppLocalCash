package com.example.mapalocalcash;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mapalocalcash.dao.CuentaDAO;
import com.example.mapalocalcash.model.Cuenta;
import com.example.mapalocalcash.model.Movimiento;
import com.example.mapalocalcash.repository.MovimientoRepository;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class DialogoMovimiento {

    // Pide confirmacion, borra el movimiento y devuelve el saldo de la cuenta a como estaba
    public static void confirmarBorrar(AppCompatActivity activity, int movimientoId, Runnable alBorrar) {
        new AlertDialog.Builder(activity)
                .setTitle("¿Borrar este movimiento?")
                .setMessage("El saldo de la cuenta vuelve a como estaba antes del movimiento.")
                .setPositiveButton("Borrar", (d, w) -> {
                    new MovimientoRepository(activity).borrar(movimientoId);
                    Toast.makeText(activity, "Movimiento borrado", Toast.LENGTH_SHORT).show();
                    alBorrar.run();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Muestra el formulario de ingreso/egreso. punto es null cuando se registra sin ubicacion.
    // alGuardar se ejecuta despues de guardar (para refrescar la pantalla).
    public static void mostrar(AppCompatActivity activity, LatLng punto, Runnable alGuardar) {
        ArrayList<Cuenta> cuentas = new CuentaDAO(activity).listar();
        if (cuentas.isEmpty()) {
            Toast.makeText(activity, "Primero crea una cuenta en 💳 Cuentas", Toast.LENGTH_LONG).show();
            return;
        }
        ArrayList<String> nombresCuentas = new ArrayList<>();
        for (Cuenta cuenta : cuentas) {
            nombresCuentas.add(cuenta.getNombre());
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
                    Movimiento movimiento = new Movimiento(
                            cuentas.get(spCuenta.getSelectedItemPosition()).getId(),
                            rbIngreso.isChecked() ? Movimiento.INGRESO : Movimiento.EGRESO,
                            monto,
                            txtDescripcion.getText().toString(),
                            punto == null ? null : punto.latitude,
                            punto == null ? null : punto.longitude);
                    new MovimientoRepository(activity).registrar(movimiento);

                    Toast.makeText(activity, "Movimiento guardado", Toast.LENGTH_SHORT).show();
                    alGuardar.run();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
