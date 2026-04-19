package com.desafio.reaccion;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

public class ConfigActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_NAME = "player_name";
    public static final String EXTRA_MODE        = "mode";
    public static final String EXTRA_MAX_TIME    = "max_time";
    public static final String EXTRA_ITERATIONS  = "iterations";

    public static final String MODE_TRAINING = "Entrenamiento";
    public static final String MODE_EASY     = "Facil";
    public static final String MODE_MEDIUM   = "Medio";
    public static final String MODE_HARD     = "Dificil";

    private static final String[] MODE_LABELS =
            {"Entrenamiento", "F\u00e1cil", "Medio", "Dif\u00edcil"};
    private static final String[] MODES =
            {MODE_TRAINING, MODE_EASY, MODE_MEDIUM, MODE_HARD};

    private static final int DEFAULT_ITERATIONS  = 20;
    private static final int DEFAULT_EASY_TIME   = 20;
    private static final int DEFAULT_MEDIUM_TIME = 15;
    private static final int DEFAULT_HARD_TIME   = 10;

    private static final String PREFS_NAME      = "DesafioPrefs";
    private static final String KEY_PLAYER_NAME = "last_player_name";

    private TextInputEditText   etPlayerName;
    private AutoCompleteTextView actvMode;
    private TextInputEditText   etIterations;
    private Slider              sliderTime;
    private TextView            tvTimeValue;

    private int selectedModeIndex = 1; // Fácil por defecto
    private int currentMaxTime    = DEFAULT_EASY_TIME;

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

        // Restaurar último nombre
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedName = prefs.getString(KEY_PLAYER_NAME, "");
        if (!savedName.isEmpty()) {
            etPlayerName.setText(savedName);
            etPlayerName.setSelection(savedName.length());
        }

        etIterations.setText(String.valueOf(DEFAULT_ITERATIONS));

        // Dropdown de modos
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, MODE_LABELS);
        actvMode.setAdapter(adapter);
        actvMode.setText(MODE_LABELS[selectedModeIndex], false);
        actvMode.setOnItemClickListener((parent, view, pos, id) -> {
            selectedModeIndex = pos;
            switch (MODES[pos]) {
                case MODE_MEDIUM: currentMaxTime = DEFAULT_MEDIUM_TIME; break;
                case MODE_HARD:   currentMaxTime = DEFAULT_HARD_TIME;   break;
                default:          currentMaxTime = DEFAULT_EASY_TIME;   break;
            }
            sliderTime.setValue(currentMaxTime);
            tvTimeValue.setText(currentMaxTime + "s");
        });

        // Slider de tiempo
        sliderTime.setValue(DEFAULT_EASY_TIME);
        tvTimeValue.setText(DEFAULT_EASY_TIME + "s");
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
            if (iterations < 1) iterations = DEFAULT_ITERATIONS;
        } catch (NumberFormatException e) {
            iterations = DEFAULT_ITERATIONS;
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
