package com.desafio.reaccion.utils;

import java.util.Random;

/**
 * Genera estímulos con reglas por nivel.
 *
 * Mecánica:
 *   - La regla define el criterio de selección.
 *   - El recuadro central muestra un valor concreto (número o color) relacionado
 *     con las opciones de los botones; puede o no coincidir con la respuesta correcta.
 *   - Los dos botones muestran valores concretos del mismo tipo que el estímulo.
 *   - El jugador aplica la regla a los botones, no al estímulo del recuadro.
 *
 * Nivel 1 – estímulos de 1 cifra / colores.
 * Nivel 2 – números de 2 cifras, reglas intermedias + negativa.
 * Nivel 3 – números de 2-3 cifras, reglas difíciles.
 */
public class StimulusGenerator {

    // ── Identificadores de regla ──────────────────────────────────────────
    private static final int RULE_SEL_PAR    = 0;
    private static final int RULE_SEL_IMPAR  = 1;
    private static final int RULE_SEL_CALIDO = 2;
    private static final int RULE_SEL_FRIO   = 3;
    private static final int RULE_MULT_3     = 4;
    private static final int RULE_NO_PAR     = 5;
    private static final int RULE_MAYOR      = 6;
    private static final int RULE_PRIMO      = 7;
    private static final int RULE_MULT_7     = 8;
    private static final int RULE_MENOR      = 9;

    private static final int[][] RULES_PER_LEVEL = {
        {},
        { RULE_SEL_PAR, RULE_SEL_IMPAR, RULE_SEL_CALIDO, RULE_SEL_FRIO },
        { RULE_MULT_3, RULE_NO_PAR, RULE_MAYOR },
        { RULE_PRIMO, RULE_MULT_7, RULE_MENOR }
    };

    // ── Paleta de colores ─────────────────────────────────────────────────
    private static final String[] WARM_NAMES  = { "ROJO", "AMARILLO", "NARANJA" };
    private static final int[]    WARM_VALUES = { 0xFFFF4D6D, 0xFFD4FF00, 0xFFFF9933 };
    private static final String[] COOL_NAMES  = { "AZUL", "CIAN", "VERDE" };
    private static final int[]    COOL_VALUES = { 0xFF60A5FA, 0xFF22D3EE, 0xFF4ADE80 };

    // ── Números primos de 2 cifras ────────────────────────────────────────
    private static final int[] TWO_DIGIT_PRIMES =
        { 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97 };

    private static final int COLOR_CREAM = 0xFFF5EED8;

    // ── Estado interno ────────────────────────────────────────────────────
    private final Random random = new Random();
    private int currentLevel = 1;
    private int currentRule  = -1;
    private int turnsLeft    = 0;

    public void setLevel(int level) {
        currentLevel = Math.min(Math.max(level, 1), 3);
        currentRule  = -1;
        turnsLeft    = 0;
    }

    public StimulusData next() {
        boolean changed = false;
        if (turnsLeft <= 0) {
            pickNewRule();
            changed = true;
        }
        turnsLeft--;
        return generate(changed);
    }

    private void pickNewRule() {
        int[] available = RULES_PER_LEVEL[currentLevel];
        if (available.length == 1) {
            currentRule = available[0];
        } else {
            int next;
            do { next = available[random.nextInt(available.length)]; }
            while (next == currentRule);
            currentRule = next;
        }
        turnsLeft = 1 + random.nextInt(3);
    }

    private StimulusData generate(boolean changed) {
        switch (currentRule) {
            case RULE_SEL_PAR:    return genSelPar(changed);
            case RULE_SEL_IMPAR:  return genSelImpar(changed);
            case RULE_SEL_CALIDO: return genSelCalido(changed);
            case RULE_SEL_FRIO:   return genSelFrio(changed);
            case RULE_MULT_3:     return genMult3(changed);
            case RULE_NO_PAR:     return genNoPar(changed);
            case RULE_MAYOR:      return genMayor(changed);
            case RULE_PRIMO:      return genPrimo(changed);
            case RULE_MULT_7:     return genMult7(changed);
            case RULE_MENOR:      return genMenor(changed);
            default:              return genSelPar(changed);
        }
    }

    // ── Nivel 1 ───────────────────────────────────────────────────────────

    /**
     * Regla: "SELECCIONÁ EL NÚMERO PAR"
     * Botones: un par y un impar de 1 cifra.
     * Estímulo: aleatoriamente el valor del botón correcto o incorrecto.
     */
    private StimulusData genSelPar(boolean changed) {
        int[] evens = { 2, 4, 6, 8 };
        int[] odds  = { 1, 3, 5, 7, 9 };
        int correct   = evens[random.nextInt(evens.length)];
        int incorrect = odds[random.nextInt(odds.length)];
        int stimulus  = random.nextBoolean() ? correct : incorrect;
        return new StimulusData(
            "SELECCIONÁ EL NÚMERO PAR", false,
            String.valueOf(stimulus), COLOR_CREAM,
            String.valueOf(correct), String.valueOf(incorrect),
            changed);
    }

    /**
     * Regla: "SELECCIONÁ EL NÚMERO IMPAR"
     * Botones: un impar y un par de 1 cifra.
     * Estímulo: aleatoriamente el valor del botón correcto o incorrecto.
     */
    private StimulusData genSelImpar(boolean changed) {
        int[] odds  = { 1, 3, 5, 7, 9 };
        int[] evens = { 2, 4, 6, 8 };
        int correct   = odds[random.nextInt(odds.length)];
        int incorrect = evens[random.nextInt(evens.length)];
        int stimulus  = random.nextBoolean() ? correct : incorrect;
        return new StimulusData(
            "SELECCIONÁ EL NÚMERO IMPAR", false,
            String.valueOf(stimulus), COLOR_CREAM,
            String.valueOf(correct), String.valueOf(incorrect),
            changed);
    }

    /**
     * Regla: "SELECCIONÁ EL COLOR CÁLIDO"
     * Botones: un color cálido (correcto) y uno frío, cada uno en su color.
     * Estímulo: uno de los mismos colores de los botones, al azar.
     */
    private StimulusData genSelCalido(boolean changed) {
        int wi = random.nextInt(WARM_NAMES.length);
        int ci = random.nextInt(COOL_NAMES.length);
        boolean stimulusIsCorrect = random.nextBoolean();
        String stimText  = stimulusIsCorrect ? WARM_NAMES[wi] : COOL_NAMES[ci];
        int    stimColor = stimulusIsCorrect ? WARM_VALUES[wi] : COOL_VALUES[ci];
        return new StimulusData(
            "SELECCIONÁ EL COLOR CÁLIDO", false,
            stimText, stimColor,
            WARM_NAMES[wi], COOL_NAMES[ci],
            WARM_VALUES[wi], COOL_VALUES[ci],
            changed);
    }

    /**
     * Regla: "SELECCIONÁ EL COLOR FRÍO"
     * Botones: un color frío (correcto) y uno cálido, cada uno en su color.
     * Estímulo: uno de los mismos colores de los botones, al azar.
     */
    private StimulusData genSelFrio(boolean changed) {
        int ci = random.nextInt(COOL_NAMES.length);
        int wi = random.nextInt(WARM_NAMES.length);
        boolean stimulusIsCorrect = random.nextBoolean();
        String stimText  = stimulusIsCorrect ? COOL_NAMES[ci] : WARM_NAMES[wi];
        int    stimColor = stimulusIsCorrect ? COOL_VALUES[ci] : WARM_VALUES[wi];
        return new StimulusData(
            "SELECCIONÁ EL COLOR FRÍO", false,
            stimText, stimColor,
            COOL_NAMES[ci], WARM_NAMES[wi],
            COOL_VALUES[ci], WARM_VALUES[wi],
            changed);
    }

    // ── Nivel 2 ───────────────────────────────────────────────────────────

    /**
     * Regla: "SELECCIONÁ EL MÚLTIPLO DE 3"
     * Botones: un múltiplo de 3 y un no-múltiplo, ambos de 2 cifras.
     * Estímulo: uno de los valores de los botones, al azar.
     */
    private StimulusData genMult3(boolean changed) {
        int[] pool = multiplesOf(3, 10, 99);
        int correct = pool[random.nextInt(pool.length)];
        int incorrect;
        do { incorrect = random.nextInt(90) + 10; } while (incorrect % 3 == 0);
        int stimulus = random.nextBoolean() ? correct : incorrect;
        return new StimulusData(
            "SELECCIONÁ EL MÚLTIPLO DE 3", false,
            String.valueOf(stimulus), COLOR_CREAM,
            String.valueOf(correct), String.valueOf(incorrect),
            changed);
    }

    /**
     * Regla: "NO SELECCIONÉS EL NÚMERO PAR" (negativa)
     * Correcto = impar (la regla prohíbe el par).
     * Botones: un impar y un par de 2 cifras.
     * Estímulo: uno de los valores de los botones, al azar.
     */
    private StimulusData genNoPar(boolean changed) {
        int[] odds2  = buildOdds2Digit();
        int[] evens2 = buildEvens2Digit();
        int correct   = odds2[random.nextInt(odds2.length)];
        int incorrect = evens2[random.nextInt(evens2.length)];
        int stimulus  = random.nextBoolean() ? correct : incorrect;
        return new StimulusData(
            "NO SELECCIONÉS EL NÚMERO PAR", true,
            String.valueOf(stimulus), COLOR_CREAM,
            String.valueOf(correct), String.valueOf(incorrect),
            changed);
    }

    /**
     * Regla: "SELECCIONÁ EL MAYOR"
     * Botones: el mayor y el menor de dos números de 2 cifras.
     * Estímulo: uno de los dos números, al azar.
     */
    private StimulusData genMayor(boolean changed) {
        int a, b;
        do { a = random.nextInt(90) + 10; b = random.nextInt(90) + 10; } while (a == b);
        int correct   = Math.max(a, b);
        int incorrect = Math.min(a, b);
        int stimulus  = random.nextBoolean() ? correct : incorrect;
        return new StimulusData(
            "SELECCIONÁ EL MAYOR", false,
            String.valueOf(stimulus), COLOR_CREAM,
            String.valueOf(correct), String.valueOf(incorrect),
            changed);
    }

    // ── Nivel 3 ───────────────────────────────────────────────────────────

    /**
     * Regla: "SELECCIONÁ EL NÚMERO PRIMO"
     * Botones: un primo y un compuesto de 2 cifras.
     * Estímulo: uno de los valores de los botones, al azar.
     */
    private StimulusData genPrimo(boolean changed) {
        int correct = TWO_DIGIT_PRIMES[random.nextInt(TWO_DIGIT_PRIMES.length)];
        int incorrect;
        do { incorrect = random.nextInt(90) + 10; } while (isPrime(incorrect));
        int stimulus = random.nextBoolean() ? correct : incorrect;
        return new StimulusData(
            "SELECCIONÁ EL NÚMERO PRIMO", false,
            String.valueOf(stimulus), COLOR_CREAM,
            String.valueOf(correct), String.valueOf(incorrect),
            changed);
    }

    /**
     * Regla: "SELECCIONÁ EL MÚLTIPLO DE 7"
     * Botones: un múltiplo de 7 y un no-múltiplo, ambos de 2 cifras.
     * Estímulo: uno de los valores de los botones, al azar.
     */
    private StimulusData genMult7(boolean changed) {
        int[] pool = multiplesOf(7, 14, 98);
        int correct = pool[random.nextInt(pool.length)];
        int incorrect;
        do { incorrect = random.nextInt(90) + 10; } while (incorrect % 7 == 0);
        int stimulus = random.nextBoolean() ? correct : incorrect;
        return new StimulusData(
            "SELECCIONÁ EL MÚLTIPLO DE 7", false,
            String.valueOf(stimulus), COLOR_CREAM,
            String.valueOf(correct), String.valueOf(incorrect),
            changed);
    }

    /**
     * Regla: "SELECCIONÁ EL MENOR"
     * Botones: el menor y el mayor de dos números de 3 cifras.
     * Estímulo: uno de los dos números, al azar.
     */
    private StimulusData genMenor(boolean changed) {
        int a, b;
        do { a = random.nextInt(900) + 100; b = random.nextInt(900) + 100; } while (a == b);
        int correct   = Math.min(a, b);
        int incorrect = Math.max(a, b);
        int stimulus  = random.nextBoolean() ? correct : incorrect;
        return new StimulusData(
            "SELECCIONÁ EL MENOR", false,
            String.valueOf(stimulus), COLOR_CREAM,
            String.valueOf(correct), String.valueOf(incorrect),
            changed);
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    private int[] multiplesOf(int div, int min, int max) {
        int count = 0;
        for (int i = min; i <= max; i++) if (i % div == 0) count++;
        int[] result = new int[count];
        int idx = 0;
        for (int i = min; i <= max; i++) if (i % div == 0) result[idx++] = i;
        return result;
    }

    private int[] buildEvens2Digit() {
        int[] arr = new int[45];
        int idx = 0;
        for (int i = 10; i <= 98; i += 2) arr[idx++] = i;
        return arr;
    }

    private int[] buildOdds2Digit() {
        int[] arr = new int[45];
        int idx = 0;
        for (int i = 11; i <= 99; i += 2) arr[idx++] = i;
        return arr;
    }

    private boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) if (n % i == 0) return false;
        return true;
    }
}
