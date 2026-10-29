package com.example.mapalocalcash;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
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
import java.util.function.Consumer;

public class DialogoMovimiento {

    // Lo escrito en el formulario mientras el usuario va al mapa a elegir (o cambiar) el lugar
    private static int borradorCuenta = -1;
    private static boolean borradorIngreso;
    private static String borradorMonto;
    private static String borradorDescripcion;

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

    // Hay datos guardados de un formulario que se dejo para ir al mapa
    public static boolean hayBorrador() {
        return borradorCuenta != -1;
    }

    public static void descartarBorrador() {
        borradorCuenta = -1;
        borradorMonto = null;
        borradorDescripcion = null;
    }

    // Muestra el formulario de ingreso/egreso.
    // punto: lugar ya elegido (null = sin ubicacion). alGuardar: refresca la pantalla al guardar.
    // irAlMapa: abre el mapa para elegir o cambiar el lugar (recibe el lugar actual); null = sin esa opcion.
    public static void mostrar(AppCompatActivity activity, LatLng punto, Runnable alGuardar, Consumer<LatLng> irAlMapa) {
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
        View panelUbicacion = vista.findViewById(R.id.panelUbicacion);
        View panelLugar = vista.findViewById(R.id.panelLugar);
        TextView txtDireccion = vista.findViewById(R.id.txtDireccion);

        spCuenta.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, nombresCuentas));

        // Volver de elegir el lugar: se recupera lo que ya estaba escrito
        if (hayBorrador()) {
            if (borradorCuenta < cuentas.size()) {
                spCuenta.setSelection(borradorCuenta);
            }
            rbIngreso.setChecked(borradorIngreso);
            txtMonto.setText(borradorMonto);
            txtDescripcion.setText(borradorDescripcion);
            descartarBorrador();
        }

        // Lugar elegido (arreglo para poder cambiarlo desde los botones)
        LatLng[] lugar = {punto};
        Runnable mostrarLugar = () -> {
            boolean hayLugar = lugar[0] != null;
            panelLugar.setVisibility(hayLugar ? View.VISIBLE : View.GONE);
            panelUbicacion.setVisibility(hayLugar ? View.GONE : View.VISIBLE);
            if (hayLugar) {
                txtDireccion.setText("Buscando la dirección…");
                Direccion.buscar(activity, lugar[0], txtDireccion::setText);
            }
        };
        mostrarLugar.run();

        if (irAlMapa == null) {
            vista.findViewById(R.id.btnElegirMapa).setVisibility(View.GONE);
            vista.findViewById(R.id.btnCambiarLugar).setVisibility(View.GONE);
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
                .setTitle("Nuevo movimiento")
                .setView(vista)
                .setPositiveButton("Guardar", null) // se asigna abajo para no cerrar si falta el monto
                .setNegativeButton("Cancelar", null)
                .create();

        // Ir al mapa: se guarda lo escrito para recuperarlo al volver
        View.OnClickListener abrirMapa = v -> {
            borradorCuenta = spCuenta.getSelectedItemPosition();
            borradorIngreso = rbIngreso.isChecked();
            borradorMonto = txtMonto.getText().toString();
            borradorDescripcion = txtDescripcion.getText().toString();
            dialogo.dismiss();
            irAlMapa.accept(lugar[0]);
        };
        vista.findViewById(R.id.btnElegirMapa).setOnClickListener(abrirMapa);
        vista.findViewById(R.id.btnCambiarLugar).setOnClickListener(abrirMapa);
        vista.findViewById(R.id.btnQuitarLugar).setOnClickListener(v -> {
            lugar[0] = null;
            mostrarLugar.run();
        });

        dialogo.show();

        // Guardar: si falta el monto, avisa y el formulario sigue abierto
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Double monto = Monto.leer(txtMonto.getText().toString());
            if (monto == null) {
                txtMonto.setError("Escribe un monto valido");
                txtMonto.requestFocus();
                return;
            }
            int cuentaId = cuentas.get(spCuenta.getSelectedItemPosition()).getId();
            String tipo = rbIngreso.isChecked() ? Movimiento.INGRESO : Movimiento.EGRESO;
            String descripcion = txtDescripcion.getText().toString();
            dialogo.dismiss();

            if (lugar[0] != null) {
                guardar(activity, new Movimiento(cuentaId, tipo, monto, descripcion, lugar[0].latitude, lugar[0].longitude), alGuardar);
            } else if (chkUbicacion.isChecked()) {
                guardarConMiUbicacion(activity, new Movimiento(cuentaId, tipo, monto, descripcion, null, null), alGuardar);
            } else {
                guardar(activity, new Movimiento(cuentaId, tipo, monto, descripcion, null, null), alGuardar);
            }
        });
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
