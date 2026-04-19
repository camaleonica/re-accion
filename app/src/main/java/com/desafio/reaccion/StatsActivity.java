package com.desafio.reaccion;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.desafio.reaccion.adapter.ResultadoAdapter;
import com.desafio.reaccion.db.AppDatabase;
import com.desafio.reaccion.db.ResultadoPartida;
import java.util.List;
import java.util.concurrent.Executors;

public class StatsActivity extends AppCompatActivity {

    public static final String EXTRA_HIGHLIGHT_PLAYER = "highlight_player";

    private RecyclerView recyclerView;
    private TextView     tvEmpty;
    private Button       btnClear, btnFilterAll, btnFilterMe;
    private String       highlightPlayer;
    private boolean      showingAll = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        Toolbar toolbar = findViewById(R.id.toolbar_stats);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Estad\u00edsticas");
        }

        highlightPlayer = getIntent().getStringExtra(EXTRA_HIGHLIGHT_PLAYER);

        recyclerView   = findViewById(R.id.recycler_stats);
        tvEmpty        = findViewById(R.id.tv_empty);
        btnClear       = findViewById(R.id.btn_clear_stats);
        btnFilterAll   = findViewById(R.id.btn_filter_all);
        btnFilterMe    = findViewById(R.id.btn_filter_me);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Mostrar filtro "Solo yo" solo si sabemos quién es el jugador
        if (highlightPlayer == null || highlightPlayer.isEmpty()) {
            btnFilterMe.setVisibility(View.GONE);
            btnFilterAll.setVisibility(View.GONE);
        }

        btnFilterAll.setOnClickListener(v -> {
            showingAll = true;
            updateFilterButtons();
            loadStats();
        });
        btnFilterMe.setOnClickListener(v -> {
            showingAll = false;
            updateFilterButtons();
            loadStats();
        });
        btnClear.setOnClickListener(v -> confirmClear());

        updateFilterButtons();
        loadStats();
    }

    private void updateFilterButtons() {
        btnFilterAll.setAlpha(showingAll ? 1f : 0.45f);
        btnFilterMe.setAlpha(showingAll ? 0.45f : 1f);
    }

    private void loadStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ResultadoPartida> list;
            if (!showingAll && highlightPlayer != null) {
                list = AppDatabase.getInstance(this)
                        .resultadoDAO().obtenerPorJugador(highlightPlayer);
            } else {
                list = AppDatabase.getInstance(this).resultadoDAO().obtenerMejoresPorJugador();
            }
            runOnUiThread(() -> {
                if (list.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    btnClear.setEnabled(false);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    btnClear.setEnabled(true);
                    recyclerView.setAdapter(
                            new ResultadoAdapter(list, highlightPlayer));
                }
            });
        });
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("Limpiar estad\u00edsticas")
                .setMessage("Se borrar\u00e1n todos los resultados guardados. \u00bfContinuar?")
                .setPositiveButton("Borrar todo", (d, w) -> clearStats())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void clearStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(this).resultadoDAO().eliminarTodos();
            runOnUiThread(this::loadStats);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
