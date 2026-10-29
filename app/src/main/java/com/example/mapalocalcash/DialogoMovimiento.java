package com.example.mapalocalcash;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mapalocalcash.dao.CuentaDAO;
import com.example.mapalocalcash.model.Cuenta;
import com.example.mapalocalcash.model.Movimiento;
import com.example.mapalocalcash.repository.MovimientoRepository;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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

        CheckBox chkUbicacion = vista.findViewById(R.id.chkUbicacion);

        spCuenta.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, nombresCuentas));

        // Desde el mapa el lugar ya esta elegido; desde el panel se puede usar el GPS o ir al mapa
        if (punto != null) {
            vista.findViewById(R.id.panelUbicacion).setVisibility(View.GONE);
            vista.findViewById(R.id.txtUbicacionMapa).setVisibility(View.VISIBLE);
        }
        chkUbicacion.setOnCheckedChangeListener((boton, marcado) -> {
            if (marcado && !tienePermisoUbicacion(activity)) {
                boton.setChecked(false);
                ActivityCompat.requestPermissions(activity, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
                Toast.makeText(activity, "Acepta el permiso de ubicación y vuelve a marcar la opción", Toast.LENGTH_LONG).show();
            }
        });

        AlertDialog dialogo = new AlertDialog.Builder(activity)
                .setTitle(punto == null ? "Nuevo movimiento" : "Nuevo movimiento en el mapa")
                .setView(vista)
                .setPositiveButton("Guardar", (d, w) -> {
                    Double monto = Monto.leer(txtMonto.getText().toString());
                    if (monto == null) {
                        Toast.makeText(activity, "Escribe un monto valido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int cuentaId = cuentas.get(spCuenta.getSelectedItemPosition()).getId();
                    String tipo = rbIngreso.isChecked() ? Movimiento.INGRESO : Movimiento.EGRESO;
                    String descripcion = txtDescripcion.getText().toString();

                    if (punto != null) {
                        guardar(activity, new Movimiento(cuentaId, tipo, monto, descripcion, punto.latitude, punto.longitude), alGuardar);
                    } else if (chkUbicacion.isChecked()) {
                        guardarConMiUbicacion(activity, new Movimiento(cuentaId, tipo, monto, descripcion, null, null), alGuardar);
                    } else {
                        guardar(activity, new Movimiento(cuentaId, tipo, monto, descripcion, null, null), alGuardar);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .create();

        // Ir al mapa a elegir el lugar: alli se mantiene presionado y se abre este mismo formulario
        vista.findViewById(R.id.btnElegirMapa).setOnClickListener(v -> {
            dialogo.dismiss();
            activity.startActivity(new Intent(activity, MapaActivity.class));
            Toast.makeText(activity, "Mantén presionado el lugar del movimiento", Toast.LENGTH_LONG).show();
        });

        dialogo.show();
    }

    private static void guardar(AppCompatActivity activity, Movimiento movimiento, Runnable alGuardar) {
        new MovimientoRepository(activity).registrar(movimiento);
        Toast.makeText(activity, movimiento.tieneUbicacion() ? "Movimiento guardado en el mapa 📍" : "Movimiento guardado", Toast.LENGTH_SHORT).show();
        alGuardar.run();
    }

    // Pide la ubicacion actual al GPS y guarda el movimiento ahi (sin ubicacion si no se pudo obtener)
    @SuppressLint("MissingPermission")
    private static void guardarConMiUbicacion(AppCompatActivity activity, Movimiento sinUbicacion, Runnable alGuardar) {
        LocationServices.getFusedLocationProviderClient(activity).getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        Toast.makeText(activity, "No se pudo obtener tu ubicación (activa el GPS). Se guardó sin ubicación.", Toast.LENGTH_LONG).show();
                        guardar(activity, sinUbicacion, alGuardar);
                    } else {
                        guardar(activity, new Movimiento(sinUbicacion.getCuentaId(), sinUbicacion.getTipo(), sinUbicacion.getMonto(),
                                sinUbicacion.getDescripcion(), location.getLatitude(), location.getLongitude()), alGuardar);
                    }
                })
                .addOnFailureListener(e -> guardar(activity, sinUbicacion, alGuardar));
    }

    private static boolean tienePermisoUbicacion(AppCompatActivity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
