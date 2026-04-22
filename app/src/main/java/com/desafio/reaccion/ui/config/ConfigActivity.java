package com.desafio.reaccion.ui.config;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import com.desafio.reaccion.R;
import com.desafio.reaccion.ui.game.GameActivity;
import com.desafio.reaccion.utils.GameConfig;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

public class ConfigActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_NAME = "player_name";
    public static final String EXTRA_MODE        = "mode";
    public static final String EXTRA_MAX_TIME    = "max_time";
    public static final String EXTRA_ITERATIONS  = "iterations";

    private static final String PREFS_NAME      = "DesafioPrefs";
    private static final String KEY_PLAYER_NAME = "last_player_name";

    private static final String[] MODE_LABELS =
            {"Entrenamiento", "F\u00e1cil", "Medio", "Dif\u00edcil"};
    private static final String[] MODES = {
            GameConfig.MODE_TRAINING, GameConfig.MODE_EASY,
            GameConfig.MODE_MEDIUM,   GameConfig.MODE_HARD
    };

    private TextInputEditText    etPlayerName;
    private AutoCompleteTextView actvMode;
    private TextInputEditText    etIterations;
    private Slider               sliderTime;
    private TextView             tvTimeValue;

    private int selectedModeIndex = 1;
    private int currentMaxTime    = GameConfig.DEFAULT_EASY_TIME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        etPlayerName = findViewById(R.id.et_player_name);
        actvMode     = findViewById(R.id.actv_mode);
        etIterations = findViewById(R.id.et_iterations);
        sliderTime   = findViewById(R.id.slider_time);
        tvTimeValue  = findViewById(R.id.tv_time_value);
        findViewById(R.id.btn_start).setOnClickListener(v -> startGame());

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedName = prefs.getString(KEY_PLAYER_NAME, "");
        if (!savedName.isEmpty()) {
            etPlayerName.setText(savedName);
            etPlayerName.setSelection(savedName.length());
        }

        etIterations.setText(String.valueOf(GameConfig.DEFAULT_ITERATIONS));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, MODE_LABELS);
        actvMode.setAdapter(adapter);
        actvMode.setText(MODE_LABELS[selectedModeIndex], false);
        actvMode.setOnItemClickListener((parent, view, pos, id) -> {
            selectedModeIndex = pos;
            if (GameConfig.MODE_MEDIUM.equals(MODES[pos]))
                currentMaxTime = GameConfig.DEFAULT_MEDIUM_TIME;
            else if (GameConfig.MODE_HARD.equals(MODES[pos]))
                currentMaxTime = GameConfig.DEFAULT_HARD_TIME;
            else
                currentMaxTime = GameConfig.DEFAULT_EASY_TIME;
            sliderTime.setValue(currentMaxTime);
            tvTimeValue.setText(currentMaxTime + "s");
        });

        sliderTime.setValue(GameConfig.DEFAULT_EASY_TIME);
        tvTimeValue.setText(GameConfig.DEFAULT_EASY_TIME + "s");
        sliderTime.addOnChangeListener((slider, value, fromUser) -> {
            currentMaxTime = (int) value;
            tvTimeValue.setText(currentMaxTime + "s");
        });
    }

    private void startGame() {
        String playerName = etPlayerName.getText() != null
                ? etPlayerName.getText().toString().trim() : "";
        if (playerName.isEmpty()) {
            Toast.makeText(this, "Ingresa tu nombre", Toast.LENGTH_SHORT).show();
            return;
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putString(KEY_PLAYER_NAME, playerName).apply();

        int iterations;
        try {
            String raw = etIterations.getText() != null
                    ? etIterations.getText().toString().trim() : "";
            iterations = Integer.parseInt(raw);
            if (iterations < 1) iterations = GameConfig.DEFAULT_ITERATIONS;
        } catch (NumberFormatException e) {
            iterations = GameConfig.DEFAULT_ITERATIONS;
        }

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(EXTRA_PLAYER_NAME, playerName);
        intent.putExtra(EXTRA_MODE,        MODES[selectedModeIndex]);
        intent.putExtra(EXTRA_MAX_TIME,    currentMaxTime);
        intent.putExtra(EXTRA_ITERATIONS,  iterations);

        ActivityOptionsCompat opts = ActivityOptionsCompat.makeCustomAnimation(
                this, R.anim.slide_in_right, R.anim.slide_out_left);
        startActivity(intent, opts.toBundle());
    }
}
