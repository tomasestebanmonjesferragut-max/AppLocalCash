package com.example.mapalocalcash;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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
            nombresCuentas.add(cuenta.getNombre() + "   (" + Monto.dinero(cuenta.getSaldo()) + ")");
        }

        View vista = activity.getLayoutInflater().inflate(R.layout.dialog_movimiento, null);
        Spinner spCuenta = vista.findViewById(R.id.spCuenta);
        RadioButton rbIngreso = vista.findViewById(R.id.rbIngreso);
        EditText txtMonto = vista.findViewById(R.id.txtMonto);
        EditText txtDescripcion = vista.findViewById(R.id.txtDescripcion);
        View panelUbicacion = vista.findViewById(R.id.panelUbicacion);
        View panelLugar = vista.findViewById(R.id.panelLugar);
        TextView txtDireccion = vista.findViewById(R.id.txtDireccion);
        FrameLayout contenedorMiniMapa = vista.findViewById(R.id.contenedorMiniMapa);

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

        // Mini mapa (modo liviano: una imagen del mapa con el marcador)
        MapView miniMapa = new MapView(activity, new GoogleMapOptions().liteMode(true).mapToolbarEnabled(false));
        miniMapa.onCreate(null);
        miniMapa.onResume();
        contenedorMiniMapa.addView(miniMapa);

        // Lugar elegido (arreglo para poder cambiarlo desde los botones)
        LatLng[] lugar = {punto};
        Runnable mostrarLugar = () -> {
            boolean hayLugar = lugar[0] != null;
            panelLugar.setVisibility(hayLugar ? View.VISIBLE : View.GONE);
            panelUbicacion.setVisibility(hayLugar ? View.GONE : View.VISIBLE);
            if (hayLugar) {
                LatLng aqui = lugar[0];
                miniMapa.getMapAsync(mapa -> {
                    mapa.clear();
                    mapa.addMarker(new MarkerOptions().position(aqui));
                    mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(aqui, 16));
                });
                txtDireccion.setText("Buscando la dirección…");
                Direccion.buscar(activity, aqui, direccion -> txtDireccion.setText("📍 " + direccion));
            }
        };
        mostrarLugar.run();

        if (irAlMapa == null) {
            vista.findViewById(R.id.btnElegirMapa).setVisibility(View.GONE);
            vista.findViewById(R.id.btnCambiarLugar).setVisibility(View.GONE);
        }

        AlertDialog dialogo = new AlertDialog.Builder(activity)
                .setTitle("Nuevo movimiento")
                .setView(vista)
                .setPositiveButton("Guardar", null) // se asigna abajo para no cerrar si falta el monto
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.setOnDismissListener(d -> miniMapa.onDestroy());

        // Mi ubicacion actual: se busca al tiro y se muestra en la tarjeta
        vista.findViewById(R.id.btnMiUbicacion).setOnClickListener(v -> {
            if (!tienePermisoUbicacion(activity)) {
                ActivityCompat.requestPermissions(activity, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
                Toast.makeText(activity, "Acepta el permiso de ubicación y vuelve a tocar el botón", Toast.LENGTH_LONG).show();
                return;
            }
            panelUbicacion.setVisibility(View.GONE);
            panelLugar.setVisibility(View.VISIBLE);
            txtDireccion.setText("Buscando tu ubicación…");
            buscarMiUbicacion(activity, aqui -> {
                if (aqui == null) {
                    Toast.makeText(activity, "No se pudo obtener tu ubicación. Activa el GPS e intenta otra vez.", Toast.LENGTH_LONG).show();
                }
                lugar[0] = aqui;
                mostrarLugar.run();
            });
        });

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
        if (irAlMapa != null) {
            // Tocar el mini mapa tambien abre el mapa grande para cambiar el lugar
            miniMapa.getMapAsync(mapa -> mapa.setOnMapClickListener(p -> abrirMapa.onClick(miniMapa)));
        }
        vista.findViewById(R.id.btnQuitarLugar).setOnClickListener(v -> {
            lugar[0] = null;
            mostrarLugar.run();
        });

        dialogo.show();

        // Guardar: si falta el monto, avisa y el formulario sigue abierto
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Double monto = Monto.leer(txtMonto.getText().toString());
            if (monto == null || monto <= 0) {
                txtMonto.setError("Escribe un monto valido");
                txtMonto.requestFocus();
                return;
            }
            Movimiento movimiento = new Movimiento(
                    cuentas.get(spCuenta.getSelectedItemPosition()).getId(),
                    rbIngreso.isChecked() ? Movimiento.INGRESO : Movimiento.EGRESO,
                    monto,
                    txtDescripcion.getText().toString(),
                    lugar[0] == null ? null : lugar[0].latitude,
                    lugar[0] == null ? null : lugar[0].longitude);
            new MovimientoRepository(activity).registrar(movimiento);
            Toast.makeText(activity, movimiento.tieneUbicacion() ? "Movimiento guardado en el mapa 📍" : "Movimiento guardado", Toast.LENGTH_SHORT).show();
            dialogo.dismiss();
            alGuardar.run();
        });
    }

    // Pide la ubicacion actual al GPS. Entrega null si no se pudo obtener.
    @SuppressLint("MissingPermission")
    private static void buscarMiUbicacion(AppCompatActivity activity, Consumer<LatLng> alEncontrar) {
        LocationServices.getFusedLocationProviderClient(activity).getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> alEncontrar.accept(
                        location == null ? null : new LatLng(location.getLatitude(), location.getLongitude())))
                .addOnFailureListener(e -> alEncontrar.accept(null));
    }

    private static boolean tienePermisoUbicacion(AppCompatActivity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
