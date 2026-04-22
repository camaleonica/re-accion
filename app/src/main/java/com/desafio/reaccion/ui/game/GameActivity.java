package com.desafio.reaccion.ui.game;

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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import com.desafio.reaccion.R;
import com.desafio.reaccion.ui.config.ConfigActivity;
import com.desafio.reaccion.ui.result.ResultActivity;
import com.desafio.reaccion.utils.GameConfig;
import com.desafio.reaccion.utils.StimulusData;
import com.desafio.reaccion.utils.StimulusGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    // Views
    private TextView                tvPillStatus, tvPointsInfo, tvIterationInfo;
    private TextView                tvStimulus, tvInstruction, tvRule;
    private FrameLayout             timeBarContainer;
    private View                    viewTimeBar;
    private MaterialCardView        stimulusCard;
    private FrameLayout             overlayStatus;
    private TextView                tvOverlayPill, tvOverlaySubtitle;
    private MaterialButton          btnOptionA, btnOptionB;
    private LinearProgressIndicator progressBar;

    // State
    private String  playerName, mode;
    private int     maxTimeSeconds, iterationsPerLevel;
    private int     currentLevel     = 1;
    private int     currentIteration = 0;
    private int     totalPoints      = 0;
    private boolean gameCompleted    = false;
    private boolean correctIsA       = true;
    private int     optionAColor     = 0; // 0 = sin color personalizado
    private int     optionBColor     = 0;
    private final List<Long> reactionTimes   = new ArrayList<>();
    private long             minReactionTime = Long.MAX_VALUE;
    private long             maxReactionTime = 0;

    private final Handler  handler = new Handler(Looper.getMainLooper());
    private CountDownTimer countDownTimer;
    private long           stimulusShownAt;
    private boolean        reacted  = false;
    private boolean        gameOver = false;
    private final Random   random   = new Random();

    private int timeBarTotalWidth = 0;

    private ToneGenerator    toneGen;
    private Vibrator         vibrator;
    private StimulusGenerator generator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        tvPointsInfo      = findViewById(R.id.tv_points_info);
        tvPillStatus      = findViewById(R.id.tv_pill_status);
        tvIterationInfo   = findViewById(R.id.tv_iteration_info);
        tvStimulus        = findViewById(R.id.tv_stimulus);
        tvInstruction     = findViewById(R.id.tv_instruction);
        tvRule            = findViewById(R.id.tv_rule);
        timeBarContainer  = findViewById(R.id.time_bar_container);
        viewTimeBar       = findViewById(R.id.view_time_bar);
        stimulusCard      = findViewById(R.id.stimulus_card);
        btnOptionA        = findViewById(R.id.btn_option_a);
        btnOptionB        = findViewById(R.id.btn_option_b);
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
        vibrator  = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        generator = new StimulusGenerator();
        generator.setLevel(1);

        btnOptionA.setOnClickListener(v -> handleAnswer(btnOptionA, correctIsA));
        btnOptionB.setOnClickListener(v -> handleAnswer(btnOptionB, !correctIsA));

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

    private void setStimulusState(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8)  & 0xFF;
        int b = color         & 0xFF;
        stimulusCard.setStrokeColor(color);
        stimulusCard.setCardBackgroundColor(Color.argb(20, r, g, b));
        tvStimulus.setTextColor(color);
        tvInstruction.setTextColor(Color.argb(128, r, g, b));
    }

    private void resetButtonColors() {
        optionAColor = 0;
        optionBColor = 0;
        ColorStateList gold = ColorStateList.valueOf(0xFFD4FF00);
        btnOptionA.setStrokeColor(gold);
        btnOptionA.setTextColor(0xFFD4FF00);
        btnOptionB.setStrokeColor(gold);
        btnOptionB.setTextColor(0xFFD4FF00);
    }

    private void setButtonsEnabled(boolean enabled, float alpha) {
        btnOptionA.setEnabled(enabled);
        btnOptionA.setAlpha(alpha);
        btnOptionB.setEnabled(enabled);
        btnOptionB.setAlpha(alpha);
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

        int barColor, btnColor;
        if (pct > 0.30f) {
            barColor = Color.parseColor("#4ADE80");
            btnColor = Color.parseColor("#D4FF00");
        } else if (pct > 0.15f) {
            barColor = Color.parseColor("#FB923C");
            btnColor = Color.parseColor("#FB923C");
        } else {
            barColor = Color.parseColor("#FF4D6D");
            btnColor = Color.parseColor("#FF4D6D");
        }
        viewTimeBar.setBackgroundColor(barColor);

        // Solo actualizar color de botón si no tiene color personalizado (reglas de color)
        if (optionAColor == 0) {
            btnOptionA.setStrokeColor(ColorStateList.valueOf(btnColor));
            btnOptionA.setTextColor(btnColor);
        }
        if (optionBColor == 0) {
            btnOptionB.setStrokeColor(ColorStateList.valueOf(btnColor));
            btnOptionB.setTextColor(btnColor);
        }
    }

    // ─── Regla activa ─────────────────────────────────────────────────────

    private void displayRule(StimulusData data) {
        SpannableString ss = new SpannableString(data.ruleText);
        if (data.isNegativeRule) {
            int idx = data.ruleText.indexOf("NO");
            if (idx >= 0) {
                ss.setSpan(new ForegroundColorSpan(0xFFFB923C),
                        idx, idx + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new StyleSpan(Typeface.BOLD),
                        idx, idx + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        tvRule.setText(ss);
        if (data.ruleChanged) {
            tvRule.setAlpha(0f);
            tvRule.animate().alpha(0.7f).setDuration(300).start();
        }
    }

    // ─── Overlay de estado ────────────────────────────────────────────────

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

    private void hideOverlay() { overlayStatus.setVisibility(View.GONE); }

    // ─── HUD pill ─────────────────────────────────────────────────────────

    private void setPillStatus(int drawableRes, int color, String text) {
        tvPillStatus.setBackgroundResource(drawableRes);
        tvPillStatus.setTextColor(color);
        tvPillStatus.setText(text);
    }

    // ─── Cuenta regresiva inicial ─────────────────────────────────────────

    private void startInitialCountdown() {
        stimulusCard.setStrokeColor(0xFF2E2A22);
        stimulusCard.setCardBackgroundColor(Color.TRANSPARENT);
        tvInstruction.setText("PREPÁRATE");
        tvInstruction.setTextColor(Color.argb(80, 0xF5, 0xEE, 0xD8));
        tvRule.setText("");
        btnOptionA.setText("");
        btnOptionB.setText("");
        setButtonsEnabled(false, 0.4f);
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
            tvStimulus.setText("¡YA!");
            playLevelUp();
            vibrateMedium();
            handler.postDelayed(() -> { tvStimulus.setText(""); startRound(); }, 650);
        }
    }

    // ─── Flujo de ronda ───────────────────────────────────────────────────

    private void updateHUD() {
        boolean isTraining = GameConfig.MODE_TRAINING.equals(mode);
        tvPointsInfo.setText(isTraining ? "ENTRENAMIENTO"
                : String.format(Locale.getDefault(), "%d pts", totalPoints));
        setPillStatus(R.drawable.pill_hud, 0xFFA855F7,
                String.format(Locale.getDefault(), "▸ NVL %02d/%02d",
                        currentLevel, GameConfig.MAX_LEVELS));
        tvIterationInfo.setText(String.format(Locale.getDefault(),
                "RONDA %02d / %02d", currentIteration + 1, iterationsPerLevel));
    }

    private void startRound() {
        if (gameOver) return;
        hideOverlay();
        reacted = false;
        updateHUD();
        showStimulus();
    }

    private void showStimulus() {
        if (gameOver) return;
        stimulusShownAt = System.currentTimeMillis();

        StimulusData data = generator.next();

        displayRule(data);
        setStimulusState(data.stimulusColor);
        tvStimulus.setText(data.stimulusText);
        tvInstruction.setText("");

        correctIsA = random.nextBoolean();
        btnOptionA.setText(correctIsA ? data.correctOption   : data.incorrectOption);
        btnOptionB.setText(correctIsA ? data.incorrectOption : data.correctOption);

        resetButtonColors();
        setButtonsEnabled(true, 1.0f);

        // Aplicar colores específicos de la opción (reglas de color)
        int aOptColor = correctIsA ? data.correctOptionColor : data.incorrectOptionColor;
        int bOptColor = correctIsA ? data.incorrectOptionColor : data.correctOptionColor;
        if (aOptColor != 0) {
            optionAColor = aOptColor;
            btnOptionA.setTextColor(aOptColor);
            btnOptionA.setStrokeColor(ColorStateList.valueOf(aOptColor));
        }
        if (bOptColor != 0) {
            optionBColor = bOptColor;
            btnOptionB.setTextColor(bOptColor);
            btnOptionB.setStrokeColor(ColorStateList.valueOf(bOptColor));
        }

        if (timeBarTotalWidth <= 0) timeBarTotalWidth = timeBarContainer.getWidth();
        ViewGroup.LayoutParams p = viewTimeBar.getLayoutParams();
        p.width = timeBarTotalWidth > 0 ? timeBarTotalWidth : ViewGroup.LayoutParams.MATCH_PARENT;
        viewTimeBar.setLayoutParams(p);
        viewTimeBar.setBackgroundColor(0xFF4ADE80);
        progressBar.setProgress(0);

        final long maxTimeMs = GameConfig.effectiveTime(maxTimeSeconds, currentLevel) * 1000L;
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

    // ─── Respuesta del jugador ────────────────────────────────────────────

    private void handleAnswer(MaterialButton tappedBtn, boolean isCorrect) {
        if (reacted || gameOver) return;
        reacted = true;
        if (countDownTimer != null) countDownTimer.cancel();
        setButtonsEnabled(false, 1.0f);

        long reactionMs = System.currentTimeMillis() - stimulusShownAt;

        if (isCorrect) {
            tappedBtn.setStrokeColor(ColorStateList.valueOf(0xFF4ADE80));
            tappedBtn.setTextColor(0xFF4ADE80);

            reactionTimes.add(reactionMs);
            if (reactionMs < minReactionTime) minReactionTime = reactionMs;
            if (reactionMs > maxReactionTime) maxReactionTime = reactionMs;

            if (!GameConfig.MODE_TRAINING.equals(mode)) {
                long maxMs = GameConfig.effectiveTime(maxTimeSeconds, currentLevel) * 1000L;
                int pts = (int) ((maxMs - reactionMs) / (float) maxMs * 100) * currentLevel;
                totalPoints += Math.max(pts, 0);
            }

            playCorrect();
            vibrateShort();
            progressBar.setProgress(100);
            currentIteration++;
            updateHUD();

            handler.postDelayed(() -> {
                if (gameOver) return;
                setPillStatus(R.drawable.pill_ok, 0xFF4ADE80, "✓ OK");
                showOverlay(R.drawable.pill_ok, 0xFF4ADE80, "✓ OK", null);

                handler.postDelayed(() -> {
                    if (gameOver) return;
                    if (currentIteration >= iterationsPerLevel) {
                        if (currentLevel >= GameConfig.MAX_LEVELS) {
                            gameCompleted = true;
                            playLevelUp();
                            vibrateMedium();
                            setPillStatus(R.drawable.pill_ganaste, 0xFFD4FF00, "★ GANASTE");
                            showTransition(R.drawable.pill_ganaste, 0xFFD4FF00,
                                    "★ GANASTE", "COMPLETASTE TODOS LOS NIVELES",
                                    this::finishGame, 2000);
                        } else {
                            currentLevel++;
                            currentIteration = 0;
                            generator.setLevel(currentLevel);
                            playLevelUp();
                            vibrateMedium();
                            updateHUD();
                            showTransition(R.drawable.pill_hud, 0xFFA855F7,
                                    String.format(Locale.getDefault(),
                                            "▸ NVL %02d/03", currentLevel),
                                    "TIEMPO REDUCIDO: "
                                            + GameConfig.effectiveTime(maxTimeSeconds, currentLevel) + "s",
                                    this::startRound, 1800);
                        }
                    } else {
                        startRound();
                    }
                }, 700);
            }, 200);

        } else {
            tappedBtn.setStrokeColor(ColorStateList.valueOf(0xFFFF4D6D));
            tappedBtn.setTextColor(0xFFFF4D6D);
            handler.postDelayed(() -> gameOver("Respuesta incorrecta"), 200);
        }
    }

    private void showTransition(int drawableRes, int color, String text, String subtitle,
                                Runnable next, long delay) {
        showOverlay(drawableRes, color, text, subtitle);
        progressBar.setProgress(0);
        tvInstruction.setText("");
        handler.postDelayed(next, delay);
    }

    // ─── Game Over ────────────────────────────────────────────────────────

    private void gameOver(String reason) {
        if (gameOver) return;
        if (countDownTimer != null) countDownTimer.cancel();
        handler.removeCallbacksAndMessages(null);
        setButtonsEnabled(false, 0.2f);
        progressBar.setProgress(0);
        tvInstruction.setText("");
        playError();
        vibrateError();

        if (GameConfig.MODE_TRAINING.equals(mode)) {
            reacted = true;
            String subtitle = reason.toUpperCase(Locale.getDefault())
                    + " — ¡INTENTA DE NUEVO!";
            showOverlay(R.drawable.pill_hud, 0xFFA855F7, "▸ INTENTO", subtitle);
            handler.postDelayed(this::startRound, 1800);
            return;
        }

        gameOver = true;
        setPillStatus(R.drawable.pill_gameover, 0xFFFB923C, "✕ GAME OVER");
        String subtitle = "Tiempo agotado".equals(reason) ? "TIEMPO AGOTADO" : "RESPUESTA INCORRECTA";
        showOverlay(R.drawable.pill_gameover, 0xFFFB923C, "✕ GAME OVER", subtitle);
        handler.postDelayed(this::finishGame, 2200);
    }

    // ─── Fin de partida ───────────────────────────────────────────────────

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

    // ─── Diálogo de salida ────────────────────────────────────────────────

    private void showExitDialog() {
        handler.removeCallbacksAndMessages(null);
        if (countDownTimer != null) countDownTimer.cancel();

        new AlertDialog.Builder(this)
                .setTitle("Abandonar partida")
                .setMessage("Se perderá el progreso. ¿Salir?")
                .setPositiveButton("Salir",     (d, w) -> finish())
                .setNegativeButton("Continuar", (d, w) -> { reacted = false; startRound(); })
                .setCancelable(false)
                .show();
    }

    // ─── Sonido ───────────────────────────────────────────────────────────

    private void playTick()    { if (toneGen != null) toneGen.startTone(ToneGenerator.TONE_PROP_BEEP,  80);  }
    private void playCorrect() { if (toneGen != null) toneGen.startTone(ToneGenerator.TONE_PROP_ACK,   60);  }
    private void playLevelUp() { if (toneGen != null) toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 150); }
    private void playError()   { if (toneGen != null) toneGen.startTone(ToneGenerator.TONE_PROP_NACK,  300); }

    // ─── Vibración ────────────────────────────────────────────────────────

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
