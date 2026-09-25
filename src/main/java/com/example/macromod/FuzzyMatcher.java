package com.example.macromod;

final class FuzzyMatcher {

    private static final int MAX_EDIT_DISTANCE = 2;

    private FuzzyMatcher() {}

    static boolean matches(String query, String name) {
        String q = normalize(query);
        String n = normalize(name);
        if (q.isEmpty() || n.isEmpty()) return false;
        if (n.equals(q)) return true;
        if (n.contains(q)) return true;
        return levenshtein(q, n) <= MAX_EDIT_DISTANCE;
    }

    static boolean matchesAny(String query, Iterable<String> names) {
        for (String name : names) {
            if (matches(query, name)) return true;
        }
        return false;
    }

    static int levenshtein(String a, String b) {
        int la = a.length();
        int lb = b.length();
        if (la == 0) return lb;
        if (lb == 0) return la;

        int[] prev = new int[lb + 1];
        int[] curr = new int[lb + 1];

        for (int j = 0; j <= lb; j++) prev[j] = j;

        for (int i = 1; i <= la; i++) {
            curr[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= lb; j++) {
                int cost = ca == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                    Math.min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[lb];
    }

    private static String normalize(String s) {
        return s.trim().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }
}
