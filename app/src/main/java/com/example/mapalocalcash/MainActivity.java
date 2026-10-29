package com.example.mapalocalcash;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mapalocalcash.dao.CuentaDAO;
import com.example.mapalocalcash.dao.DeudaDAO;
import com.google.android.gms.maps.model.LatLng;

// Panel principal: resumen y acciones. El mapa se abre con el boton de abajo.
public class MainActivity extends AppCompatActivity {

    private CuentaDAO cuentaDAO;
    private DeudaDAO deudaDAO;
    private TextView txtSaldoTotal;
    private TextView txtTeDeben;
    // Lugar que tenia el formulario al ir al mapa (para volver igual si se cancela)
    private LatLng lugarAntesDelMapa;

    // Abre el mapa para elegir el lugar del movimiento y, al volver, reabre el formulario
    private final ActivityResultLauncher<Intent> elegirLugar = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                    Intent datos = resultado.getData();
                    abrirFormulario(new LatLng(
                            datos.getDoubleExtra(MapaActivity.EXTRA_LAT, 0),
                            datos.getDoubleExtra(MapaActivity.EXTRA_LON, 0)));
                } else if (DialogoMovimiento.hayBorrador()) {
                    abrirFormulario(lugarAntesDelMapa);
                }
            });

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

        cuentaDAO = new CuentaDAO(this);
        deudaDAO = new DeudaDAO(this);
        txtSaldoTotal = findViewById(R.id.txtSaldoTotal);
        txtTeDeben = findViewById(R.id.txtTeDeben);

        findViewById(R.id.btnMovimiento).setOnClickListener(v -> abrirFormulario(null));
        findViewById(R.id.btnCuentas).setOnClickListener(v -> startActivity(new Intent(this, CuentasActivity.class)));
        findViewById(R.id.btnContactos).setOnClickListener(v -> startActivity(new Intent(this, ContactosActivity.class)));
        findViewById(R.id.btnCompras).setOnClickListener(v -> startActivity(new Intent(this, ComprasActivity.class)));
        findViewById(R.id.btnDeudas).setOnClickListener(v -> startActivity(new Intent(this, DeudasActivity.class)));
        findViewById(R.id.btnVerMapa).setOnClickListener(v -> startActivity(new Intent(this, MapaActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarResumen();
    }

    private void abrirFormulario(LatLng lugar) {
        DialogoMovimiento.mostrar(this, lugar, this::cargarResumen, this::elegirEnMapa);
    }

    // Va al mapa en modo "elegir lugar", centrado en el lugar actual si ya habia uno
    private void elegirEnMapa(LatLng lugarActual) {
        lugarAntesDelMapa = lugarActual;
        Intent intent = new Intent(this, MapaActivity.class);
        intent.putExtra(MapaActivity.EXTRA_ELEGIR, true);
        if (lugarActual != null) {
            intent.putExtra(MapaActivity.EXTRA_LAT, lugarActual.latitude);
            intent.putExtra(MapaActivity.EXTRA_LON, lugarActual.longitude);
        }
        elegirLugar.launch(intent);
    }

    // Saldo de todas las cuentas y total que te deben
    private void cargarResumen() {
        double saldo = cuentaDAO.saldoTotal();
        double teDeben = deudaDAO.totalPendiente();

        txtSaldoTotal.setText(Monto.dinero(saldo));
        txtSaldoTotal.setTextColor(ContextCompat.getColor(this, saldo >= 0 ? R.color.verde_oscuro : R.color.rojo));
        txtTeDeben.setText("+ $" + Monto.miles(teDeben));
    }
}
