package com.example.mapalocalcash;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mapalocalcash.dao.MovimientoDAO;
import com.example.mapalocalcash.model.Movimiento;
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

public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Modo "elegir lugar": se abre desde el formulario de movimiento y devuelve el punto tocado
    public static final String EXTRA_ELEGIR = "elegir";
    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LON = "lon";

    private boolean modoElegir;
    private Button btnAccionPunto;
    private GoogleMap mMap = null;
    private MovimientoDAO movimientoDAO;
    private FusedLocationProviderClient ubicacion;
    private Marker puntoMarcado = null;
    // Lugar tocado (se guarda aparte para que no se pierda al redibujar el mapa)
    private LatLng puntoElegido = null;
    private LatLng yo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mapa);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        movimientoDAO = new MovimientoDAO(this);
        ubicacion = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        modoElegir = getIntent().getBooleanExtra(EXTRA_ELEGIR, false);
        btnAccionPunto = findViewById(R.id.btnAccionPunto);
        Button btnVolver = findViewById(R.id.btnVolver);
        if (modoElegir) {
            ((TextView) findViewById(R.id.txtTituloMapa)).setText("📍 Elige el lugar");
            ((TextView) findViewById(R.id.txtAyudaMapa)).setText("Toca el mapa donde fue el gasto o ingreso y presiona Guardar dirección.");
            btnAccionPunto.setText("📍 Guardar dirección");
            btnVolver.setText("Cancelar y volver al formulario");
            if (getIntent().hasExtra(EXTRA_LAT)) {
                puntoElegido = new LatLng(getIntent().getDoubleExtra(EXTRA_LAT, 0), getIntent().getDoubleExtra(EXTRA_LON, 0));
            }
        }

        btnVolver.setOnClickListener(v -> finish());
        btnAccionPunto.setOnClickListener(v -> usarPuntoElegido());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Centrado en el lugar ya elegido, o en la zona de inicio
        LatLng starPoin = new LatLng(-33.498895, -70.616617);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(puntoElegido != null ? puntoElegido : starPoin, 18));

        // Tocar el mapa marca el lugar (solo uno) y muestra el boton de abajo
        mMap.setOnMapClickListener(punto -> {
            puntoElegido = punto;
            dibujarPuntoElegido();
        });

        // Tocar el cartel: quita el punto marcado, o borra el movimiento (devolviendo el saldo)
        mMap.setOnInfoWindowClickListener(marker -> {
            if (marker.equals(puntoMarcado)) {
                puntoElegido = null;
                dibujarPuntoElegido();
                Toast.makeText(this, "Punto quitado", Toast.LENGTH_SHORT).show();
            } else if (marker.getTag() instanceof Integer) {
                DialogoMovimiento.confirmarBorrar(this, (Integer) marker.getTag(), this::cargarMapa);
            }
        });

        cargarMapa();
        activarUbicacion();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!modoElegir) {
            DialogoMovimiento.descartarBorrador();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            cargarMapa();
        }
    }

    // Pone (o quita) el marcador amarillo del lugar tocado y muestra u oculta el boton de abajo
    private void dibujarPuntoElegido() {
        if (puntoMarcado != null) {
            puntoMarcado.remove();
            puntoMarcado = null;
        }
        if (puntoElegido != null) {
            puntoMarcado = mMap.addMarker(new MarkerOptions()
                    .position(puntoElegido)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    .title("Buscando la dirección…")
                    .snippet("Toca este cartel para quitarlo"));
            Marker marcador = puntoMarcado;
            Direccion.buscar(this, puntoElegido, direccion -> {
                if (marcador != null && marcador.equals(puntoMarcado)) {
                    marcador.setTitle(direccion);
                    marcador.showInfoWindow();
                }
            });
        }
        btnAccionPunto.setVisibility(puntoElegido == null ? View.GONE : View.VISIBLE);
    }

    // Boton de abajo: devolver el lugar al formulario, o registrar un movimiento en el
    private void usarPuntoElegido() {
        if (puntoElegido == null) {
            return;
        }
        if (modoElegir) {
            Intent datos = new Intent();
            datos.putExtra(EXTRA_LAT, puntoElegido.latitude);
            datos.putExtra(EXTRA_LON, puntoElegido.longitude);
            setResult(RESULT_OK, datos);
            finish();
        } else {
            DialogoMovimiento.mostrar(this, puntoElegido, () -> {
                puntoElegido = null;
                cargarMapa();
            }, lugarActual -> Toast.makeText(this, "Toca otro lugar y presiona Registrar movimiento aquí", Toast.LENGTH_LONG).show());
        }
    }

    // Dibuja mi ubicacion, un marcador por cada movimiento con ubicacion y el lugar tocado
    private void cargarMapa() {
        mMap.clear();
        puntoMarcado = null;

        if (yo != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(yo)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title("Estoy aqui"));
        }

        // Movimientos guardados en SQLite: ingreso en verde (suma), egreso en rojo (resta)
        for (Movimiento m : movimientoDAO.listarConUbicacion()) {
            boolean ingreso = m.esIngreso();
            Marker marcador = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(m.getLat(), m.getLon()))
                    .icon(BitmapDescriptorFactory.defaultMarker(ingreso ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED))
                    .title(m.getDescripcion())
                    .snippet((ingreso ? "+ $" : "− $") + Math.round(m.getMonto()) + "  " + m.getTipo() + " · " + m.getCuentaNombre() + "  (toca para borrar)"));
            if (marcador != null) {
                marcador.setTag(m.getId());
            }
        }
        dibujarPuntoElegido();
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
                if (puntoElegido == null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(yo, 17));
                }
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
}
