package com.example.mapalocalcash;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mapalocalcash.dao.ContactoDAO;
import com.example.mapalocalcash.dao.DeudaDAO;
import com.example.mapalocalcash.model.Contacto;
import com.example.mapalocalcash.model.Deuda;
import com.example.mapalocalcash.repository.ContactoRepository;

import java.util.ArrayList;

public class ContactosActivity extends AppCompatActivity {

    private ContactoDAO contactoDAO;
    private DeudaDAO deudaDAO;
    private ContactoRepository contactoRepository;
    private ListView lista;
    private ArrayList<Contacto> contactos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactos);

        contactoDAO = new ContactoDAO(this);
        deudaDAO = new DeudaDAO(this);
        contactoRepository = new ContactoRepository(this);
        EditText txtNombre = findViewById(R.id.txtNombre);
        EditText txtTelefono = findViewById(R.id.txtTelefono);
        EditText txtMeDebe = findViewById(R.id.txtMeDebe);
        EditText txtMotivo = findViewById(R.id.txtMotivo);
        lista = findViewById(R.id.lista);

        findViewById(R.id.btnAgregar).setOnClickListener(v -> {
            String nombre = txtNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                Toast.makeText(this, "Escribe un nombre", Toast.LENGTH_SHORT).show();
                return;
            }
            String textoDebe = txtMeDebe.getText().toString().trim();
            Double meDebe = textoDebe.isEmpty() ? null : Monto.leer(textoDebe);
            if (!textoDebe.isEmpty() && meDebe == null) {
                Toast.makeText(this, "Escribe un monto valido", Toast.LENGTH_SHORT).show();
                return;
            }

            contactoRepository.crear(new Contacto(nombre, txtTelefono.getText().toString().trim()),
                    meDebe, txtMotivo.getText().toString().trim());

            txtNombre.setText("");
            txtTelefono.setText("");
            txtMeDebe.setText("");
            txtMotivo.setText("");
            cargar();
        });

        lista.setOnItemClickListener((parent, view, position, id) -> opciones(contactos.get(position)));

        cargar();
    }

    private void cargar() {
        contactos = contactoDAO.listar();
        ArrayList<CharSequence> textos = new ArrayList<>();
        for (Contacto contacto : contactos) {
            String telefono = contacto.getTelefono();
            String antes = "👤 " + contacto.getNombre() + (telefono.isEmpty() ? "" : "\n📞 " + telefono) + "\n";
            double debe = deudaDAO.totalPendienteDeContacto(contacto.getId(), contacto.getNombre());
            textos.add(debe > 0
                    ? Monto.conColor(this, antes, "Te debe + $" + Math.round(debe), true)
                    : antes + "✔ Al día");
        }
        lista.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos));
    }

    private void opciones(Contacto contacto) {
        String[] opciones = {"📞 Llamar", "➕ Anotar monto que me debe", "✅ Ver deudas y marcar pagos", "🗑️ Borrar contacto"};
        new AlertDialog.Builder(this)
                .setTitle(contacto.getNombre())
                .setItems(opciones, (d, which) -> {
                    if (which == 0) {
                        if (contacto.getTelefono().isEmpty()) {
                            Toast.makeText(this, "Este contacto no tiene número", Toast.LENGTH_SHORT).show();
                        } else {
                            // Abre el marcador del telefono con el numero (no llama solo)
                            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contacto.getTelefono())));
                        }
                    } else if (which == 1) {
                        anotarDeuda(contacto);
                    } else if (which == 2) {
                        verDeudas(contacto);
                    } else {
                        borrar(contacto);
                    }
                })
                .show();
    }

    private void anotarDeuda(Contacto contacto) {
        LinearLayout vista = new LinearLayout(this);
        vista.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        vista.setPadding(p, p / 2, p, 0);

        EditText monto = new EditText(this);
        monto.setHint("Monto $");
        monto.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        vista.addView(monto);

        EditText motivo = new EditText(this);
        motivo.setHint("Motivo (ej: préstamo, entrada)");
        vista.addView(motivo);

        new AlertDialog.Builder(this)
                .setTitle("¿Cuánto te debe " + contacto.getNombre() + "?")
                .setView(vista)
                .setPositiveButton("Guardar", (d, w) -> {
                    Double valor = Monto.leer(monto.getText().toString());
                    if (valor == null || valor <= 0) {
                        Toast.makeText(this, "Escribe un monto valido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    contactoRepository.anotarDeuda(contacto.getId(), contacto.getNombre(), valor, motivo.getText().toString().trim());
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Lista lo que debe (anotado a mano y de gastos compartidos) para marcar lo que ya pago
    private void verDeudas(Contacto contacto) {
        ArrayList<Deuda> deudas = deudaDAO.pendientesDeContacto(contacto.getId(), contacto.getNombre());
        if (deudas.isEmpty()) {
            Toast.makeText(this, contacto.getNombre() + " no te debe nada", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<CharSequence> textos = new ArrayList<>();
        for (Deuda deuda : deudas) {
            textos.add(TextUtils.concat(Monto.conColor(this, "", "+ $" + Math.round(deuda.getMonto()), true), "  " + deuda.getOrigen()));
        }

        boolean[] pagadas = new boolean[deudas.size()];
        new AlertDialog.Builder(this)
                .setTitle("Marca lo que " + contacto.getNombre() + " ya pagó")
                .setMultiChoiceItems(textos.toArray(new CharSequence[0]), pagadas, (d, which, isChecked) -> pagadas[which] = isChecked)
                .setPositiveButton("Guardar", (d, w) -> {
                    for (int i = 0; i < deudas.size(); i++) {
                        if (pagadas[i]) {
                            deudaDAO.marcarPagada(deudas.get(i).getId(), true);
                        }
                    }
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void borrar(Contacto contacto) {
        new AlertDialog.Builder(this)
                .setTitle("¿Borrar a " + contacto.getNombre() + "?")
                .setMessage("Se borran también las deudas que anotaste a mano. Lo de los gastos compartidos se mantiene con su nombre.")
                .setPositiveButton("Borrar", (d, w) -> {
                    contactoRepository.borrar(contacto.getId());
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
