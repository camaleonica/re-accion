package com.desafio.reaccion.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.desafio.reaccion.data.db.AppDatabase;
import com.desafio.reaccion.data.db.ResultadoDao;
import com.desafio.reaccion.data.model.ResultadoPartida;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultadoRepository {

    public interface Callback<T> {
        void onResult(T result);
    }

    private final ResultadoDao    dao;
    private final ExecutorService executor    = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    public ResultadoRepository(Context context) {
        this.dao = AppDatabase.getInstance(context).resultadoDao();
    }

    /** Guarda la partida y devuelve en el hilo principal si es nuevo récord personal. */
    public void guardarPartida(ResultadoPartida r, Callback<Boolean> onComplete) {
        executor.execute(() -> {
            Integer prevBest = dao.getMejorPuntaje(r.jugador, r.modo);
            boolean isRecord = prevBest != null && r.puntos > prevBest;
            dao.insertar(r);
            if (onComplete != null) mainHandler.post(() -> onComplete.onResult(isRecord));
        });
    }

    /** Devuelve en el hilo principal el mejor resultado por jugador (leaderboard). */
    public void obtenerMejores(Callback<List<ResultadoPartida>> callback) {
        executor.execute(() -> {
            List<ResultadoPartida> list = dao.obtenerMejoresPorJugador();
            mainHandler.post(() -> callback.onResult(list));
        });
    }

    /** Devuelve en el hilo principal todas las partidas de un jugador. */
    public void obtenerPorJugador(String nombre, Callback<List<ResultadoPartida>> callback) {
        executor.execute(() -> {
            List<ResultadoPartida> list = dao.obtenerPorJugador(nombre);
            mainHandler.post(() -> callback.onResult(list));
        });
    }

    /** Elimina todos los registros y notifica en el hilo principal. */
    public void eliminarTodos(Runnable onComplete) {
        executor.execute(() -> {
            dao.eliminarTodos();
            if (onComplete != null) mainHandler.post(onComplete);
        });
    }
}
