package com.example.mapalocalcash;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mapalocalcash.dao.CuentaDAO;
import com.example.mapalocalcash.dao.DeudaDAO;

// Panel principal: resumen y acciones. El mapa se abre con el boton de abajo.
public class MainActivity extends AppCompatActivity {

    private CuentaDAO cuentaDAO;
    private DeudaDAO deudaDAO;
    private TextView txtSaldoTotal;
    private TextView txtTeDeben;

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

        findViewById(R.id.btnMovimiento).setOnClickListener(v -> DialogoMovimiento.mostrar(this, null, this::cargarResumen));
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

    // Saldo de todas las cuentas y total que te deben
    private void cargarResumen() {
        double saldo = cuentaDAO.saldoTotal();
        double teDeben = deudaDAO.totalPendiente();

        txtSaldoTotal.setText("$" + Math.round(saldo));
        txtSaldoTotal.setTextColor(ContextCompat.getColor(this, saldo >= 0 ? R.color.verde_oscuro : R.color.rojo));
        txtTeDeben.setText("+ $" + Math.round(teDeben));
    }
}
