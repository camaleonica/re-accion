package com.desafio.reaccion.utils;

public final class GameConfig {

    private GameConfig() {}

    // ── Modos de juego ────────────────────────────────────────────────────
    public static final String MODE_TRAINING = "Entrenamiento";
    public static final String MODE_EASY     = "Facil";
    public static final String MODE_MEDIUM   = "Medio";
    public static final String MODE_HARD     = "Dificil";

    // ── Configuración general ─────────────────────────────────────────────
    public static final int MAX_LEVELS         = 3;
    public static final int MAX_SECONDS        = 30;
    public static final int DEFAULT_ITERATIONS = 20;

    // ── Tiempos por defecto según modo ────────────────────────────────────
    public static final int DEFAULT_EASY_TIME   = 20;
    public static final int DEFAULT_MEDIUM_TIME = 15;
    public static final int DEFAULT_HARD_TIME   = 10;

    // ── Factores de reducción de tiempo por nivel ─────────────────────────
    // Nivel 1 → 100%, Nivel 2 → 70%, Nivel 3 → 50%
    public static int effectiveTime(int maxSeconds, int level) {
        switch (level) {
            case 2:  return Math.max(3, maxSeconds * 7 / 10);
            case 3:  return Math.max(2, maxSeconds / 2);
            default: return maxSeconds;
        }
    }
}
