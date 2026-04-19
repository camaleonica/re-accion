package com.desafio.reaccion.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "resultados")
public class ResultadoPartida {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String jugador;
    public int    puntos;
    public long   tiempoPromedio; // milliseconds
    public String modo;
    public int    nivelAlcanzado;
    public long   fecha;          // epoch milliseconds
}
