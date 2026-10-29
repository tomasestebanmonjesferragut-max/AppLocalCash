package com.example.mapalocalcash.repository;

import android.content.Context;

import com.example.mapalocalcash.dao.CompraDAO;
import com.example.mapalocalcash.dao.DeudaDAO;
import com.example.mapalocalcash.dao.ProductoDAO;
import com.example.mapalocalcash.model.Compra;
import com.example.mapalocalcash.model.Deuda;
import com.example.mapalocalcash.model.Producto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Compras y gastos compartidos: una compra con sus productos y lo que debe cada persona
public class CompraRepository {

    private final CompraDAO compraDAO;
    private final ProductoDAO productoDAO;
    private final DeudaDAO deudaDAO;

    public CompraRepository(Context context) {
        compraDAO = new CompraDAO(context);
        productoDAO = new ProductoDAO(context);
        deudaDAO = new DeudaDAO(context);
    }

    // Guarda un gasto compartido nuevo (compraId null) o reemplaza uno existente.
    // contactos: nombre en minusculas -> id del contacto, para enlazar a quienes estan en la agenda.
    public void guardarGastoCompartido(Integer compraId, List<Producto> productos, List<String> nombres,
                                       List<Double> montos, Map<String, Integer> contactos) {
        ArrayList<String> nombresProductos = new ArrayList<>();
        double total = 0;
        for (Producto p : productos) {
            nombresProductos.add(p.getNombre());
            total += p.getPrecio();
        }
        String descripcion = String.join(", ", nombresProductos);

        // Al editar, quien ya habia pagado (por nombre) sigue como pagado
        HashMap<String, Boolean> yaPagaron = new HashMap<>();
        int id;
        if (compraId == null) {
            String fecha = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            id = (int) compraDAO.crear(new Compra(descripcion, total, fecha, false, null));
        } else {
            id = compraId;
            for (Deuda d : deudaDAO.listarPorCompra(id)) {
                yaPagaron.put(d.getNombre().toLowerCase(), d.isPagada());
            }
            compraDAO.actualizar(id, descripcion, total);
            productoDAO.eliminarPorCompra(id);
            deudaDAO.eliminarPorCompra(id);
        }

        for (Producto p : productos) {
            productoDAO.crear(id, p);
        }
        for (int i = 0; i < nombres.size(); i++) {
            String clave = nombres.get(i).toLowerCase();
            Boolean pagada = yaPagaron.get(clave);
            deudaDAO.crear(new Deuda(id, contactos.get(clave), nombres.get(i), montos.get(i), pagada != null && pagada, null));
        }
    }

    // Productos de un gasto compartido. Las compras antiguas sin productos usan su descripcion y total.
    public ArrayList<Producto> productosDe(int compraId) {
        ArrayList<Producto> productos = productoDAO.listarPorCompra(compraId);
        if (productos.isEmpty()) {
            Compra compra = compraDAO.buscar(compraId);
            if (compra != null) {
                productos.add(new Producto(compra.getDescripcion(), compra.getMonto()));
            }
        }
        return productos;
    }

    // Borra la compra; si era un gasto compartido, tambien sus productos y deudas
    public void borrar(int compraId) {
        deudaDAO.eliminarPorCompra(compraId);
        productoDAO.eliminarPorCompra(compraId);
        compraDAO.eliminar(compraId);
    }
}
