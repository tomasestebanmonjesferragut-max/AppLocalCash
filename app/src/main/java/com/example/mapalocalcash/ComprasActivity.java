package com.example.mapalocalcash;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mapalocalcash.dao.CompraDAO;
import com.example.mapalocalcash.dao.ContactoDAO;
import com.example.mapalocalcash.model.Compra;
import com.example.mapalocalcash.model.Contacto;
import com.example.mapalocalcash.repository.CompraRepository;

import java.util.ArrayList;

public class ComprasActivity extends AppCompatActivity {

    private CompraDAO compraDAO;
    private ListView lista;
    private TextView txtTotalAgendado;
    private ArrayList<Compra> compras = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compras);

        compraDAO = new CompraDAO(this);
        EditText txtDescripcion = findViewById(R.id.txtDescripcion);
        EditText txtMonto = findViewById(R.id.txtMonto);
        EditText txtFecha = findViewById(R.id.txtFecha);
        CheckBox chkAgendada = findViewById(R.id.chkAgendada);
        Spinner spContacto = findViewById(R.id.spContacto);
        lista = findViewById(R.id.lista);
        txtTotalAgendado = findViewById(R.id.txtTotalAgendado);

        // Contactos para compartir la compra (el primero es "sin contacto")
        ArrayList<Contacto> contactos = new ContactoDAO(this).listar();
        ArrayList<String> nombres = new ArrayList<>();
        nombres.add("Sin contacto");
        for (Contacto contacto : contactos) {
            nombres.add(contacto.getNombre());
        }
        spContacto.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres));

        findViewById(R.id.btnGuardar).setOnClickListener(v -> {
            String descripcion = txtDescripcion.getText().toString();
            Double monto = Monto.leer(txtMonto.getText().toString());
            if (descripcion.isEmpty() || monto == null) {
                Toast.makeText(this, "Falta la descripcion o un monto valido", Toast.LENGTH_SHORT).show();
                return;
            }

            int posicion = spContacto.getSelectedItemPosition();
            Integer contactoId = posicion == 0 ? null : contactos.get(posicion - 1).getId();
            compraDAO.crear(new Compra(descripcion, monto, txtFecha.getText().toString(), chkAgendada.isChecked(), contactoId));

            txtDescripcion.setText("");
            txtMonto.setText("");
            txtFecha.setText("");
            chkAgendada.setChecked(false);
            spContacto.setSelection(0);
            cargar();
        });

        // Al tocar una compra: marcarla como realizada (si esta agendada) o borrarla
        lista.setOnItemClickListener((parent, view, position, id) -> {
            Compra compra = compras.get(position);
            String[] opciones = compra.isAgendada()
                    ? new String[]{"✅ Marcar como realizada", "🗑️ Borrar"}
                    : new String[]{"🗑️ Borrar"};
            new AlertDialog.Builder(this)
                    .setItems(opciones, (d, which) -> {
                        if (compra.isAgendada() && which == 0) {
                            compraDAO.marcarRealizada(compra.getId());
                            cargar();
                        } else {
                            borrar(compra.getId());
                        }
                    })
                    .show();
        });

        cargar();
    }

    // Borra la compra; si era un gasto compartido, tambien sus productos y deudas
    private void borrar(int compraId) {
        new AlertDialog.Builder(this)
                .setTitle("¿Borrar esta compra?")
                .setMessage("Si es un gasto compartido, se borran también sus productos y lo que cada persona debía.")
                .setPositiveButton("Borrar", (d, w) -> {
                    new CompraRepository(this).borrar(compraId);
                    Toast.makeText(this, "Compra borrada", Toast.LENGTH_SHORT).show();
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cargar() {
        compras = compraDAO.listar();
        ArrayList<CharSequence> textos = new ArrayList<>();
        double totalAgendado = 0;

        for (Compra compra : compras) {
            // Las compras son gastos: monto en rojo
            String antes = (compra.isAgendada() ? "📅 Agendada · " : "") + compra.getFecha() + " " + compra.getDescripcion() + "  ";
            CharSequence texto = Monto.conColor(this, antes, "− $" + Math.round(compra.getMonto()), false);
            if (compra.getContactoNombre() != null) {
                texto = TextUtils.concat(texto, "  (con " + compra.getContactoNombre() + ")");
            }
            if (compra.isAgendada()) {
                totalAgendado += compra.getMonto();
            }
            textos.add(texto);
        }

        txtTotalAgendado.setText("Monto a apartar para compras agendadas: $" + Math.round(totalAgendado));
        lista.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos));
    }
}
