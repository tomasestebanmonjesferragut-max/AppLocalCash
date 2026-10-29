package com.example.mapalocalcash.repository;

import android.content.Context;

import com.example.mapalocalcash.dao.ContactoDAO;
import com.example.mapalocalcash.dao.DeudaDAO;
import com.example.mapalocalcash.model.Contacto;
import com.example.mapalocalcash.model.Deuda;

// Contactos y lo que deben
public class ContactoRepository {

    private final ContactoDAO contactoDAO;
    private final DeudaDAO deudaDAO;

    public ContactoRepository(Context context) {
        contactoDAO = new ContactoDAO(context);
        deudaDAO = new DeudaDAO(context);
    }

    // Crea el contacto y, si se indico, lo que ya debe
    public void crear(Contacto contacto, Double meDebe, String motivo) {
        int id = (int) contactoDAO.crear(contacto);
        if (meDebe != null && meDebe > 0) {
            anotarDeuda(id, contacto.getNombre(), meDebe, motivo);
        }
    }

    // Deuda anotada a mano (no viene de un gasto compartido)
    public void anotarDeuda(int contactoId, String nombre, double monto, String motivo) {
        deudaDAO.crear(new Deuda(null, contactoId, nombre, monto, false, motivo.isEmpty() ? "Préstamo" : motivo));
    }

    // Borra el contacto y sus deudas anotadas a mano. Lo de gastos compartidos se mantiene con su nombre.
    public void borrar(int contactoId) {
        deudaDAO.eliminarManualesDeContacto(contactoId);
        deudaDAO.desvincularContacto(contactoId);
        contactoDAO.eliminar(contactoId);
    }
}
