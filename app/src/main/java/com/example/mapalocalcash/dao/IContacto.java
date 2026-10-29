package com.example.mapalocalcash.dao;

import com.example.mapalocalcash.model.Contacto;

import java.util.ArrayList;

public interface IContacto {
    long crear(Contacto contacto);
    ArrayList<Contacto> listar();
    boolean eliminar(int id);
}
