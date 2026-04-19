package com.desafio.reaccion;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private static final int MAX_LEVELS = 3;

    private static final String[] STIMULUS_COLOR_NAMES = {
            "VERDE", "AZUL", "ROJO", "AMARILLO", "CIAN", "MAGENTA"
    };
    private static final int[] STIMULUS_COLOR_VALUES = {
            0xFF4ADE80, // VERDE
            0xFF60A5FA, // AZUL
            0xFFFF4D6D, // ROJO
            0xFFD4FF00, // AMARILLO
            0xFF22D3EE, // CIAN
            0xFFE879F9  // MAGENTA
    };

    private static final String[] STIMULUS_WORDS = {"YA!", "TAP!", "AHORA!", "PULSA!"};
    private static final int TYPE_COLOR  = 0;
    private static final int TYPE_NUMBER = 1;
    private static final int TYPE_WORD   = 2;

    private static final int COLOR_NUMBER = 0xFFF5EED8;
    private static final int COLOR_WORD   = 0xFFA855F7;

    // Views
    private TextView               tvPillStatus, tvPointsInfo, tvIterationInfo;
    private TextView               tvStimulus, tvInstruction;
    private FrameLayout            timeBarContainer;
    private View                   viewTimeBar;
    private MaterialCardView       stimulusCard;
    private FrameLayout            overlayStatus;
    private TextView               tvOverlayPill, tvOverlaySubtitle;
    private MaterialButton         btnReact;
    private LinearProgressIndicator progressBar;

    // State
    private String playerName, mode;
    private int    maxTimeSeconds, iterationsPerLevel;
    private int    currentLevel     = 1;
    private int    currentIteration = 0;
    private int    totalPoints      = 0;
    private boolean gameCompleted   = false;
    private final List<Long> reactionTimes   = new ArrayList<>();
    private long             minReactionTime = Long.MAX_VALUE;
    private long             maxReactionTime = 0;

    private final Handler  handler = new Handler(Looper.getMainLooper());
    private CountDownTimer countDownTimer;
    private long           stimulusShownAt;
    private boolean        stimulusVisible = false;
    private boolean        reacted         = false;
    private boolean        gameOver        = false;
    private final Random   random          = new Random();

    private int  timeBarTotalWidth = 0;

    private ToneGenerator toneGen;
    private Vibrator      vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        tvPointsInfo      = findViewById(R.id.tv_points_info);
        tvPillStatus      = findViewById(R.id.tv_pill_status);
        tvIterationInfo   = findViewById(R.id.tv_iteration_info);
        tvStimulus        = findViewById(R.id.tv_stimulus);
        tvInstruction     = findViewById(R.id.tv_instruction);
        timeBarContainer  = findViewById(R.id.time_bar_container);
        viewTimeBar       = findViewById(R.id.view_time_bar);
        stimulusCard      = findViewById(R.id.stimulus_card);
        btnReact          = findViewById(R.id.btn_react);
        progressBar       = findViewById(R.id.progress_bar);
        overlayStatus     = findViewById(R.id.overlay_status);
        tvOverlayPill     = findViewById(R.id.tv_overlay_pill);
        tvOverlaySubtitle = findViewById(R.id.tv_overlay_subtitle);

        playerName         = getIntent().getStringExtra(ConfigActivity.EXTRA_PLAYER_NAME);
        mode               = getIntent().getStringExtra(ConfigActivity.EXTRA_MODE);
        maxTimeSeconds     = getIntent().getIntExtra(ConfigActivity.EXTRA_MAX_TIME, 20);
        iterationsPerLevel = getIntent().getIntExtra(ConfigActivity.EXTRA_ITERATIONS, 20);

        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 80); }
        catch (RuntimeException e) { toneGen = null; }
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Fijar colores del botón para que no cambien con el estado Material
        ColorStateList fixedGold = ColorStateList.valueOf(0xFFD4FF00);
        btnReact.setStrokeColor(fixedGold);
        btnReact.setTextColor(0xFFD4FF00);
        btnReact.setOnClickListener(v -> handleReaction());

        stimulusCard.setOnClickListener(v -> {
            if (!stimulusVisible && !reacted && !gameOver) gameOver("Salida en falso");
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (gameOver) finish();
                else showExitDialog();
            }
        });

        updateHUD();
        startInitialCountdown();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (countDownTimer != null) countDownTimer.cancel();
        if (toneGen != null) { toneGen.release(); toneGen = null; }
    }

    // ─── Estados visuales ────────────────────────────────────────────────

    private void setWaitingState() {
        // Barra de tiempo: restaurar ancho completo
        if (timeBarTotalWidth <= 0) timeBarTotalWidth = timeBarContainer.getWidth();
        ViewGroup.LayoutParams p = viewTimeBar.getLayoutParams();
        p.width = timeBarTotalWidth > 0 ? timeBarTotalWidth : ViewGroup.LayoutParams.MATCH_PARENT;
        viewTimeBar.setLayoutParams(p);
        viewTimeBar.setBackgroundColor(0xFF4ADE80);

        // Recuadro: neutro
        stimulusCard.setStrokeColor(0xFF2E2A22);
        stimulusCard.setCardBackgroundColor(Color.TRANSPARENT);

        // Texto estímulo: "ESPERA..." en crema con 12% de opacidad
        tvStimulus.setTextColor(Color.argb(31, 0xF5, 0xEE, 0xD8));
        tvStimulus.setText("ESPERA...");
        tvInstruction.setText("");
        tvInstruction.setTextColor(Color.TRANSPARENT);

        // Botón: visible al 40%, sin ocultar
        btnReact.setAlpha(0.4f);
        btnReact.setStrokeColor(ColorStateList.valueOf(0xFFD4FF00));
        btnReact.setTextColor(0xFFD4FF00);

        progressBar.setProgress(0);
    }

    private void setStimulusState(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Recuadro: borde y fondo tintado al 8%
        stimulusCard.setStrokeColor(color);
        stimulusCard.setCardBackgroundColor(Color.argb(20, r, g, b));

        // Texto: color pleno para estímulo, 50% para instrucción
        tvStimulus.setTextColor(color);
        tvInstruction.setTextColor(Color.argb(128, r, g, b));

        // Botón: opacidad completa al activar el estímulo
        btnReact.setAlpha(1.0f);
        btnReact.setStrokeColor(ColorStateList.valueOf(0xFFD4FF00));
        btnReact.setTextColor(0xFFD4FF00);
    }

    private void updateTimeBar(long left, long maxMs) {
        if (timeBarTotalWidth <= 0) {
            timeBarTotalWidth = timeBarContainer.getWidth();
            if (timeBarTotalWidth <= 0) return;
        }
        float pct = (float) left / maxMs;

        ViewGroup.LayoutParams p = viewTimeBar.getLayoutParams();
        p.width = (int) (pct * timeBarTotalWidth);
        viewTimeBar.setLayoutParams(p);

        // Color de la barra y botón según porcentaje restante
        if (pct > 0.30f) {
            viewTimeBar.setBackgroundColor(Color.parseColor("#4ADE80"));
            btnReact.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#D4FF00")));
            btnReact.setTextColor(Color.parseColor("#D4FF00"));
        } else if (pct > 0.15f) {
            viewTimeBar.setBackgroundColor(Color.parseColor("#FB923C"));
            btnReact.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FB923C")));
            btnReact.setTextColor(Color.parseColor("#FB923C"));
        } else {
            viewTimeBar.setBackgroundColor(Color.parseColor("#FF4D6D"));
            btnReact.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FF4D6D")));
            btnReact.setTextColor(Color.parseColor("#FF4D6D"));
        }
    }

    // ─── Overlay de estado (ok / game over / ganaste) ────────────────────

    private void showOverlay(int drawableRes, int color, String text, String subtitle) {
        tvOverlayPill.setBackgroundResource(drawableRes);
        tvOverlayPill.setTextColor(color);
        tvOverlayPill.setText(text);
        if (subtitle != null && !subtitle.isEmpty()) {
            tvOverlaySubtitle.setText(subtitle);
            tvOverlaySubtitle.setTextColor(color);
            tvOverlaySubtitle.setVisibility(View.VISIBLE);
        } else {
            tvOverlaySubtitle.setVisibility(View.GONE);
        }
        overlayStatus.setVisibility(View.VISIBLE);
    }

    private void hideOverlay() {
        overlayStatus.setVisibility(View.GONE);
    }

    // ─── HUD pill ────────────────────────────────────────────────────────

    private void setPillStatus(int drawableRes, int color, String text) {
        tvPillStatus.setBackgroundResource(drawableRes);
        tvPillStatus.setTextColor(color);
        tvPillStatus.setText(text);
    }

    // ─── Cuenta regresiva inicial 3–2–1–¡YA! ────────────────────────────

    private void startInitialCountdown() {
        stimulusCard.setStrokeColor(0xFF2E2A22);
        stimulusCard.setCardBackgroundColor(Color.TRANSPARENT);
        tvInstruction.setText("PREP\u00c1RATE \u2014 NO TOQUES AUN");
        tvInstruction.setTextColor(Color.argb(80, 0xF5, 0xEE, 0xD8));
        btnReact.setAlpha(0.4f);
        showCountdownTick(3);
    }

    private void showCountdownTick(int n) {
        if (gameOver) return;
        hideOverlay();
        stimulusCard.setStrokeColor(0xFF2E2A22);
        stimulusCard.setCardBackgroundColor(Color.TRANSPARENT);
        tvStimulus.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        progressBar.setProgress(0);

        if (n > 0) {
            tvStimulus.setText(String.valueOf(n));
            playTick();
            vibrateShort();
            handler.postDelayed(() -> showCountdownTick(n - 1), 850);
        } else {
            tvStimulus.setText("\u00a1YA!");
            playLevelUp();
            vibrateMedium();
            handler.postDelayed(() -> {
                tvStimulus.setText("");
                startRound();
            }, 650);
        }
    }

    // ─── Flujo de ronda ──────────────────────────────────────────────────

    private void updateHUD() {
        boolean isTraining = ConfigActivity.MODE_TRAINING.equals(mode);
        tvPointsInfo.setText(isTraining ? "ENTRENAMIENTO"
                : String.format(Locale.getDefault(), "%d pts", totalPoints));
        setPillStatus(R.drawable.pill_hud, 0xFFA855F7,
                String.format(Locale.getDefault(), "\u25b8 NVL %02d/%02d", currentLevel, MAX_LEVELS));
        tvIterationInfo.setText(String.format(Locale.getDefault(),
                "RONDA %02d / %02d", currentIteration + 1, iterationsPerLevel));
    }

    private void startRound() {
        if (gameOver) return;
        hideOverlay();
        reacted         = false;
        stimulusVisible = false;
        setWaitingState();
        updateHUD();

        long delay = 1000 + random.nextInt(2001);
        handler.postDelayed(this::showStimulus, delay);
    }

    private int getEffectiveMaxTime() {
        switch (currentLevel) {
            case 2: return Math.max(3, maxTimeSeconds * 7 / 10);
            case 3: return Math.max(2, maxTimeSeconds / 2);
            default: return maxTimeSeconds;
        }
    }

    private void showStimulus() {
        if (reacted || gameOver) return;
        stimulusVisible = true;
        stimulusShownAt = System.currentTimeMillis();

        int type = random.nextInt(3);
        int stimulusColor;
        String text;

        if (type == TYPE_COLOR) {
            int idx = random.nextInt(STIMULUS_COLOR_NAMES.length);
            stimulusColor = STIMULUS_COLOR_VALUES[idx];
            text = STIMULUS_COLOR_NAMES[idx];
        } else if (type == TYPE_NUMBER) {
            stimulusColor = COLOR_NUMBER;
            text = String.valueOf(random.nextInt(9) + 1);
        } else {
            stimulusColor = COLOR_WORD;
            text = STIMULUS_WORDS[random.nextInt(STIMULUS_WORDS.length)];
        }

        setStimulusState(stimulusColor);
        tvStimulus.setText(text);
        tvInstruction.setText("\u00a1REACCIONA!");

        final long maxTimeMs = getEffectiveMaxTime() * 1000L;
        countDownTimer = new CountDownTimer(maxTimeMs, 50) {
            @Override public void onTick(long left) {
                if (reacted || gameOver) { cancel(); return; }
                updateTimeBar(left, maxTimeMs);
                progressBar.setProgress((int) ((maxTimeMs - left) / (float) maxTimeMs * 100f));
            }
            @Override public void onFinish() {
                if (!reacted && !gameOver) gameOver("Tiempo agotado");
            }
        }.start();
    }

    // ─── Reacción correcta ───────────────────────────────────────────────

    private void handleReaction() {
        if (reacted || gameOver) return;

        if (!stimulusVisible) {
            gameOver("Salida en falso");
            return;
        }

        reacted = true;
        long reactionMs = System.currentTimeMillis() - stimulusShownAt;

        if (countDownTimer != null) countDownTimer.cancel();
        reactionTimes.add(reactionMs);
        if (reactionMs < minReactionTime) minReactionTime = reactionMs;
        if (reactionMs > maxReactionTime) maxReactionTime = reactionMs;

        if (!ConfigActivity.MODE_TRAINING.equals(mode)) {
            long maxMs = getEffectiveMaxTime() * 1000L;
            int pts = (int) ((maxMs - reactionMs) / (float) maxMs * 100) * currentLevel;
            totalPoints += Math.max(pts, 0);
        }

        playCorrect();
        vibrateShort();

        progressBar.setProgress(100);
        tvInstruction.setText("");
        updateHUD();
        setPillStatus(R.drawable.pill_ok, 0xFF4ADE80, "\u2713 OK");
        showOverlay(R.drawable.pill_ok, 0xFF4ADE80, "\u2713 OK", null);

        currentIteration++;

        handler.postDelayed(() -> {
            if (gameOver) return;
            if (currentIteration >= iterationsPerLevel) {
                if (currentLevel >= MAX_LEVELS) {
                    gameCompleted = true;
                    playLevelUp();
                    vibrateMedium();
                    setPillStatus(R.drawable.pill_ganaste, 0xFFD4FF00, "\u2605 GANASTE");
                    showTransition(R.drawable.pill_ganaste, 0xFFD4FF00,
                            "\u2605 GANASTE", "COMPLETASTE TODOS LOS NIVELES",
                            this::finishGame, 2000);
                } else {
                    currentLevel++;
                    currentIteration = 0;
                    playLevelUp();
                    vibrateMedium();
                    updateHUD();
                    showTransition(R.drawable.pill_hud, 0xFFA855F7,
                            String.format(Locale.getDefault(), "\u25b8 NVL %02d/03", currentLevel),
                            "TIEMPO REDUCIDO: " + getEffectiveMaxTime() + "s",
                            this::startRound, 1800);
                }
            } else {
                startRound();
            }
        }, 900);
    }

    private void showTransition(int drawableRes, int color, String text, String subtitle,
                                Runnable next, long delay) {
        showOverlay(drawableRes, color, text, subtitle);
        progressBar.setProgress(0);
        tvInstruction.setText("");
        handler.postDelayed(next, delay);
    }

    // ─── Game Over ───────────────────────────────────────────────────────

    private void gameOver(String reason) {
        if (gameOver) return;
        if (countDownTimer != null) countDownTimer.cancel();
        handler.removeCallbacksAndMessages(null);
        btnReact.setEnabled(false);
        btnReact.setAlpha(0.2f);
        progressBar.setProgress(0);
        tvInstruction.setText("");
        playError();
        vibrateError();

        if (ConfigActivity.MODE_TRAINING.equals(mode)) {
            reacted = true;
            String subtitle = reason.toUpperCase(Locale.getDefault())
                    + " \u2014 \u00a1INTENTA DE NUEVO!";
            showOverlay(R.drawable.pill_hud, 0xFFA855F7, "\u25b8 INTENTO", subtitle);
            handler.postDelayed(this::startRound, 1800);
            return;
        }

        gameOver = true;
        setPillStatus(R.drawable.pill_gameover, 0xFFFB923C, "\u2715 GAME OVER");
        String subtitle = "Salida en falso".equals(reason) ? "SALIDA EN FALSO" : "TIEMPO AGOTADO";
        showOverlay(R.drawable.pill_gameover, 0xFFFB923C, "\u2715 GAME OVER", subtitle);
        handler.postDelayed(this::finishGame, 2200);
    }

    // ─── Fin de partida ──────────────────────────────────────────────────

    private void finishGame() {
        long avgTime = 0;
        if (!reactionTimes.isEmpty()) {
            long sum = 0;
            for (long t : reactionTimes) sum += t;
            avgTime = sum / reactionTimes.size();
        }
        long min = reactionTimes.isEmpty() ? 0 : minReactionTime;
        long max = reactionTimes.isEmpty() ? 0 : maxReactionTime;

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(ResultActivity.EXTRA_PLAYER_NAME,   playerName);
        intent.putExtra(ResultActivity.EXTRA_MODE,          mode);
        intent.putExtra(ResultActivity.EXTRA_POINTS,        totalPoints);
        intent.putExtra(ResultActivity.EXTRA_AVG_TIME,      avgTime);
        intent.putExtra(ResultActivity.EXTRA_MIN_TIME,      min);
        intent.putExtra(ResultActivity.EXTRA_MAX_TIME,      max);
        intent.putExtra(ResultActivity.EXTRA_LEVEL,         currentLevel);
        intent.putExtra(ResultActivity.EXTRA_COMPLETED,     gameCompleted);
        intent.putExtra(ResultActivity.EXTRA_CORRECT_COUNT, reactionTimes.size());

        ActivityOptionsCompat opts = ActivityOptionsCompat.makeCustomAnimation(
                this, android.R.anim.fade_in, android.R.anim.fade_out);
        startActivity(intent, opts.toBundle());
        finish();
    }

    // ─── Diálogo de salida ───────────────────────────────────────────────

    private void showExitDialog() {
        handler.removeCallbacksAndMessages(null);
        if (countDownTimer != null) countDownTimer.cancel();

        new AlertDialog.Builder(this)
                .setTitle("Abandonar partida")
                .setMessage("Se perder\u00e1 el progreso. \u00bfSalir?")
                .setPositiveButton("Salir", (d, w) -> finish())
                .setNegativeButton("Continuar", (d, w) -> {
                    stimulusVisible = false;
                    reacted = false;
                    startRound();
                })
                .setCancelable(false)
                .show();
    }

    // ─── Sonido ──────────────────────────────────────────────────────────

    private void playTick()    { if (toneGen != null) toneGen.startTone(ToneGenerator.TONE_PROP_BEEP,  80);  }
    private void playCorrect() { if (toneGen != null) toneGen.startTone(ToneGenerator.TONE_PROP_ACK,   60);  }
    private void playLevelUp() { if (toneGen != null) toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 150); }
    private void playError()   { if (toneGen != null) toneGen.startTone(ToneGenerator.TONE_PROP_NACK,  300); }

    // ─── Vibración ───────────────────────────────────────────────────────

    private void vibrateShort() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(40);
        } catch (SecurityException ignored) {}
    }

    private void vibrateMedium() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(120);
        } catch (SecurityException ignored) {}
    }

    private void vibrateError() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        try {
            long[] pattern = {0, 80, 60, 180};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            else vibrator.vibrate(pattern, -1);
        } catch (SecurityException ignored) {}
    }
}
