package com.example.mapalocalcash;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap = null;
    private DBHelper dbHelper;
    private FusedLocationProviderClient ubicacion;
    private Marker puntoMarcado = null;
    private LatLng yo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DBHelper(this);
        ubicacion = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        findViewById(R.id.btnMovimiento).setOnClickListener(v -> nuevoMovimiento(null));
        findViewById(R.id.btnCuentas).setOnClickListener(v -> startActivity(new Intent(this, CuentasActivity.class)));
        findViewById(R.id.btnContactos).setOnClickListener(v -> startActivity(new Intent(this, ContactosActivity.class)));
        findViewById(R.id.btnCompras).setOnClickListener(v -> startActivity(new Intent(this, ComprasActivity.class)));
        findViewById(R.id.btnDeudas).setOnClickListener(v -> startActivity(new Intent(this, DeudasActivity.class)));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng starPoin = new LatLng(-33.498895, -70.616617);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(starPoin, 18));

        // Marca punto en la vista: solo uno, si se marca otro se quita el anterior
        mMap.setOnMapClickListener(punto -> {
            if (puntoMarcado != null) {
                puntoMarcado.remove();
            }
            puntoMarcado = mMap.addMarker(new MarkerOptions()
                    .position(punto)
                    .title("Punto marcado")
                    .snippet("Toca este cartel para quitarlo"));
        });

        // Quitar el punto marcado: tocar el marcador y luego su cartel
        mMap.setOnInfoWindowClickListener(marker -> {
            if (marker.equals(puntoMarcado)) {
                marker.remove();
                puntoMarcado = null;
                Toast.makeText(this, "Punto quitado", Toast.LENGTH_SHORT).show();
            }
        });

        // Mantener presionado: registrar un movimiento en ese lugar
        mMap.setOnMapLongClickListener(this::nuevoMovimiento);

        cargarMapa();
        activarUbicacion();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            cargarMapa();
        }
    }

    // Dibuja los puntos fijos, mi ubicacion y un marcador por cada movimiento con ubicacion
    private void cargarMapa() {
        mMap.clear();
        puntoMarcado = null;

        // Punto en el mapa
        LatLng starPoin = new LatLng(-33.498895, -70.616617);

        mMap.addMarker(new MarkerOptions()
                .position(starPoin)
                .title("Hola ")
                .snippet("Repartidor cerca"));

        if (yo != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(yo)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title("Estoy aqui"));
        }

        // Movimientos guardados en SQLite (verdes)
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT m.tipo, m.monto, m.descripcion, m.lat, m.lon, c.nombre FROM movimientos m JOIN cuentas c ON c.id = m.cuenta_id WHERE m.lat IS NOT NULL", null);
        while (c.moveToNext()) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(c.getDouble(3), c.getDouble(4)))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .title(c.getString(2))
                    .snippet(c.getString(0) + " $" + Math.round(c.getDouble(1)) + " - " + c.getString(5)));
        }
        c.close();
    }

    // Geolocalizacion
    @SuppressLint("MissingPermission")
    private void activarUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        mMap.setMyLocationEnabled(true);
        ubicacion.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                yo = new LatLng(location.getLatitude(), location.getLongitude());
                cargarMapa();
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(yo, 17));
            } else {
                Toast.makeText(this, "Activa el GPS para ver tu ubicacion", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            activarUbicacion();
        } else {
            Toast.makeText(this, "Sin permiso de ubicacion", Toast.LENGTH_SHORT).show();
        }
    }

    // punto es null cuando el movimiento se registra sin ubicacion
    private void nuevoMovimiento(LatLng punto) {
        ArrayList<Integer> idsCuentas = new ArrayList<>();
        ArrayList<String> nombresCuentas = new ArrayList<>();
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT id, nombre FROM cuentas", null);
        while (c.moveToNext()) {
            idsCuentas.add(c.getInt(0));
            nombresCuentas.add(c.getString(1));
        }
        c.close();

        if (idsCuentas.isEmpty()) {
            Toast.makeText(this, "Primero crea una cuenta", Toast.LENGTH_SHORT).show();
            return;
        }

        View vista = getLayoutInflater().inflate(R.layout.dialog_movimiento, null);
        Spinner spCuenta = vista.findViewById(R.id.spCuenta);
        Spinner spTipo = vista.findViewById(R.id.spTipo);
        EditText txtMonto = vista.findViewById(R.id.txtMonto);
        EditText txtDescripcion = vista.findViewById(R.id.txtDescripcion);

        spCuenta.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombresCuentas));
        spTipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Egreso", "Ingreso"}));

        new AlertDialog.Builder(this)
                .setTitle(punto == null ? "Nuevo movimiento" : "Nuevo movimiento en el mapa")
                .setView(vista)
                .setPositiveButton("Guardar", (d, w) -> {
                    String textoMonto = txtMonto.getText().toString();
                    if (textoMonto.isEmpty()) {
                        Toast.makeText(this, "Falta el monto", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double monto = Double.parseDouble(textoMonto);
                    String tipo = spTipo.getSelectedItem().toString();
                    int cuentaId = idsCuentas.get(spCuenta.getSelectedItemPosition());

                    ContentValues valores = new ContentValues();
                    valores.put("cuenta_id", cuentaId);
                    valores.put("tipo", tipo);
                    valores.put("monto", monto);
                    valores.put("descripcion", txtDescripcion.getText().toString());
                    if (punto != null) {
                        valores.put("lat", punto.latitude);
                        valores.put("lon", punto.longitude);
                    }

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.insert("movimientos", null, valores);
                    double cambio = tipo.equals("Ingreso") ? monto : -monto;
                    db.execSQL("UPDATE cuentas SET saldo = saldo + ? WHERE id = ?", new Object[]{cambio, cuentaId});

                    Toast.makeText(this, "Movimiento guardado", Toast.LENGTH_SHORT).show();
                    cargarMapa();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
