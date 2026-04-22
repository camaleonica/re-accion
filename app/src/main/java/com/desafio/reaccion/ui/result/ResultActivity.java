package com.desafio.reaccion.ui.result;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import com.desafio.reaccion.R;
import com.desafio.reaccion.data.model.ResultadoPartida;
import com.desafio.reaccion.repository.ResultadoRepository;
import com.desafio.reaccion.ui.config.ConfigActivity;
import com.desafio.reaccion.ui.stats.StatsActivity;
import com.desafio.reaccion.utils.GameConfig;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.Date;
import java.util.Locale;

public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_NAME   = "result_player";
    public static final String EXTRA_MODE          = "result_mode";
    public static final String EXTRA_POINTS        = "result_points";
    public static final String EXTRA_AVG_TIME      = "result_avg_time";
    public static final String EXTRA_MIN_TIME      = "result_min_time";
    public static final String EXTRA_MAX_TIME      = "result_max_time";
    public static final String EXTRA_LEVEL         = "result_level";
    public static final String EXTRA_COMPLETED     = "result_completed";
    public static final String EXTRA_CORRECT_COUNT = "result_correct_count";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        String  playerName   = getIntent().getStringExtra(EXTRA_PLAYER_NAME);
        String  mode         = getIntent().getStringExtra(EXTRA_MODE);
        int     points       = getIntent().getIntExtra(EXTRA_POINTS, 0);
        long    avgTime      = getIntent().getLongExtra(EXTRA_AVG_TIME, 0);
        long    minTime      = getIntent().getLongExtra(EXTRA_MIN_TIME, 0);
        long    maxTime      = getIntent().getLongExtra(EXTRA_MAX_TIME, 0);
        int     level        = getIntent().getIntExtra(EXTRA_LEVEL, 1);
        boolean completed    = getIntent().getBooleanExtra(EXTRA_COMPLETED, false);
        int     correctCount = getIntent().getIntExtra(EXTRA_CORRECT_COUNT, 0);
        boolean isTraining   = GameConfig.MODE_TRAINING.equals(mode);

        TextView tvTitle  = findViewById(R.id.tv_result_title);
        TextView tvStatus = findViewById(R.id.tv_result_status);
        if (completed) {
            tvTitle.setBackgroundResource(R.drawable.pill_ganaste);
            tvTitle.setTextColor(0xFFD4FF00);
            tvTitle.setText("\u2605 GANASTE");
            tvStatus.setText("\u00a1COM-\nPLETADO!");
        } else if (isTraining) {
            tvTitle.setBackgroundResource(R.drawable.pill_nivel);
            tvTitle.setTextColor(0xFFA855F7);
            tvTitle.setText("SESI\u00d3N FINALIZADA");
            tvStatus.setText("ENTRE-\nNAMIENTO");
        } else {
            tvTitle.setBackgroundResource(R.drawable.pill_gameover);
            tvTitle.setTextColor(0xFFFB923C);
            tvTitle.setText("\u2715 GAME OVER");
            tvStatus.setText("GAME\nOVER");
        }

        ((TextView) findViewById(R.id.tv_result_player)).setText(playerName);
        ((TextView) findViewById(R.id.tv_result_mode)).setText(mode.toUpperCase(Locale.getDefault()));

        TextView tvPoints      = findViewById(R.id.tv_result_points);
        TextView tvPuntosLabel = findViewById(R.id.tv_puntos_label);
        if (isTraining) {
            tvPoints.setText("\u2014");
            tvPuntosLabel.setVisibility(View.GONE);
        } else {
            tvPoints.setText(String.valueOf(points));
        }

        TextView tvAvg = findViewById(R.id.tv_stat_avg);
        tvAvg.setText(avgTime > 0
                ? String.format(Locale.getDefault(), "%d ms", avgTime)
                : "\u2014 ms");

        ((TextView) findViewById(R.id.tv_stat_level)).setText(level + " / 3");
        ((TextView) findViewById(R.id.tv_stat_correct)).setText(String.valueOf(correctCount));

        TextView tvTimes = findViewById(R.id.tv_stat_times);
        if (correctCount > 0) {
            tvTimes.setText(String.format(Locale.getDefault(),
                    "min %d ms\nmax %d ms", minTime, maxTime));
        } else {
            tvTimes.setText("");
        }

        MaterialCardView cardRecord = findViewById(R.id.card_record);

        if (!isTraining) {
            ResultadoPartida r = new ResultadoPartida();
            r.jugador        = playerName;
            r.modo           = mode;
            r.puntos         = points;
            r.tiempoPromedio = avgTime;
            r.nivelAlcanzado = level;
            r.fecha          = new Date().getTime();

            new ResultadoRepository(this).guardarPartida(r, isRecord -> {
                if (isRecord) cardRecord.setVisibility(View.VISIBLE);
            });
        }

        MaterialButton btnRestart = findViewById(R.id.btn_restart);
        MaterialButton btnStats   = findViewById(R.id.btn_stats);

        btnRestart.setOnClickListener(v -> {
            Intent i = new Intent(this, ConfigActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ActivityOptionsCompat opts = ActivityOptionsCompat.makeCustomAnimation(
                    this, android.R.anim.fade_in, android.R.anim.fade_out);
            startActivity(i, opts.toBundle());
            finish();
        });

        btnStats.setOnClickListener(v -> {
            Intent i = new Intent(this, StatsActivity.class);
            i.putExtra(StatsActivity.EXTRA_HIGHLIGHT_PLAYER, playerName);
            ActivityOptionsCompat opts = ActivityOptionsCompat.makeCustomAnimation(
                    this, R.anim.slide_in_right, R.anim.slide_out_left);
            startActivity(i, opts.toBundle());
        });
    }
}
