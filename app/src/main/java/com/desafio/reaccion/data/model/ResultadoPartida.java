package com.desafio.reaccion.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "resultados")
public class ResultadoPartida {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String jugador;
    public int    puntos;
    public long   tiempoPromedio;
    public String modo;
    public int    nivelAlcanzado;
    public long   fecha;
}
