package com.desafio.reaccion.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.desafio.reaccion.data.model.ResultadoPartida;
import java.util.List;

@Dao
public interface ResultadoDao {

    @Insert
    void insertar(ResultadoPartida resultado);

    @Query("SELECT * FROM (SELECT * FROM resultados ORDER BY puntos DESC) GROUP BY jugador ORDER BY puntos DESC")
    List<ResultadoPartida> obtenerMejoresPorJugador();

    @Query("SELECT * FROM resultados WHERE jugador = :nombre ORDER BY puntos DESC")
    List<ResultadoPartida> obtenerPorJugador(String nombre);

    @Query("DELETE FROM resultados")
    void eliminarTodos();

    @Query("SELECT MAX(puntos) FROM resultados WHERE jugador = :jugador AND modo = :modo")
    Integer getMejorPuntaje(String jugador, String modo);
}
