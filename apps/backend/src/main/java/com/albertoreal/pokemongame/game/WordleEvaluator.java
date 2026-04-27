package com.albertoreal.pokemongame.game;

public final class WordleEvaluator {

    private WordleEvaluator() {}

    /**
     * Computes per-letter Wordle feedback for {@code guess} against {@code answer}.
     * Both inputs should be normalized (lowercase, no diacritics). The returned
     * string has the same length as {@code guess}; each char is 'H', 'P' or 'M'.
     */
    public static String evaluate(String guess, String answer) {
        if (guess == null || guess.isEmpty()) return "";
        if (answer == null) answer = "";

        char[] g = guess.toCharArray();
        char[] a = answer.toCharArray();
        char[] out = new char[g.length];
        boolean[] usedInAnswer = new boolean[a.length];

        for (int i = 0; i < g.length; i++) {
            if (i < a.length && g[i] == a[i]) {
                out[i] = 'H';
                usedInAnswer[i] = true;
            } else {
                out[i] = 0;
            }
        }
        for (int i = 0; i < g.length; i++) {
            if (out[i] != 0) continue;
            int present = -1;
            for (int j = 0; j < a.length; j++) {
                if (!usedInAnswer[j] && a[j] == g[i]) {
                    present = j;
                    break;
                }
            }
            if (present >= 0) {
                out[i] = 'P';
                usedInAnswer[present] = true;
            } else {
                out[i] = 'M';
            }
        }
        return new String(out);
    }
}
