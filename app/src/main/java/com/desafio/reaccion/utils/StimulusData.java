package com.desafio.reaccion.utils;

public class StimulusData {
    public final String  ruleText;
    public final boolean isNegativeRule;
    public final String  stimulusText;
    public final int     stimulusColor;
    public final String  correctOption;
    public final String  incorrectOption;
    public final int     correctOptionColor;   // 0 = usar color por defecto
    public final int     incorrectOptionColor; // 0 = usar color por defecto
    public final boolean ruleChanged;

    /** Constructor sin colores de botón (números, textos genéricos). */
    public StimulusData(String ruleText, boolean isNegativeRule,
                        String stimulusText, int stimulusColor,
                        String correctOption, String incorrectOption,
                        boolean ruleChanged) {
        this(ruleText, isNegativeRule, stimulusText, stimulusColor,
             correctOption, incorrectOption, 0, 0, ruleChanged);
    }

    /** Constructor con colores individuales por botón (reglas de color). */
    public StimulusData(String ruleText, boolean isNegativeRule,
                        String stimulusText, int stimulusColor,
                        String correctOption, String incorrectOption,
                        int correctOptionColor, int incorrectOptionColor,
                        boolean ruleChanged) {
        this.ruleText             = ruleText;
        this.isNegativeRule       = isNegativeRule;
        this.stimulusText         = stimulusText;
        this.stimulusColor        = stimulusColor;
        this.correctOption        = correctOption;
        this.incorrectOption      = incorrectOption;
        this.correctOptionColor   = correctOptionColor;
        this.incorrectOptionColor = incorrectOptionColor;
        this.ruleChanged          = ruleChanged;
    }
}
