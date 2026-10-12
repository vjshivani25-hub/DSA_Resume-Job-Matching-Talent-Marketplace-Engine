package dsa;

import java.util.*;

public class SimilarityEngine {
    private final SkillNormalizer normalizer;

    public SimilarityEngine(SkillNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public double skillSimilarity(String a, String b) {
        String na = normalizer.normalize(a);
        String nb = normalizer.normalize(b);
        if (na.equalsIgnoreCase(nb)) return 1.0;
        if (normalizer.relatedSkills(na).contains(nb)) return 0.75;

        Set<String> ta = tokens(na);
        Set<String> tb = tokens(nb);
        if (ta.isEmpty() || tb.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(ta);
        intersection.retainAll(tb);
        Set<String> union = new HashSet<>(ta);
        union.addAll(tb);
        double jaccard = union.isEmpty() ? 0 : (double) intersection.size() / union.size();
        double lev = normalizedLevenshtein(na.toLowerCase(), nb.toLowerCase());
        return Math.max(jaccard, lev);
    }

    public double setMatch(Collection<String> candidateSkills, Collection<String> requiredSkills) {
        Set<String> c = normalizer.normalizeAll(candidateSkills);
        Set<String> r = normalizer.normalizeAll(requiredSkills);
        if (r.isEmpty()) return 1;
        double total = 0;
        for (String req : r) {
            double best = 0;
            for (String cs : c) best = Math.max(best, skillSimilarity(cs, req));
            total += best;
        }
        return total / r.size();
    }

    private Set<String> tokens(String s) {
        return new HashSet<>(Arrays.asList(s.toLowerCase().split("[\\s./+#-]+")));
    }

    public double normalizedLevenshtein(String a, String b) {
        int d = levenshtein(a, b);
        return 1.0 - (double)d / Math.max(1, Math.max(a.length(), b.length()));
    }

    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length()+1], cur = new int[b.length()+1];
        for (int j=0;j<=b.length();j++) prev[j]=j;
        for (int i=1;i<=a.length();i++) {
            cur[0]=i;
            for (int j=1;j<=b.length();j++) {
                int cost = a.charAt(i-1)==b.charAt(j-1)?0:1;
                cur[j]=Math.min(Math.min(cur[j-1]+1, prev[j]+1), prev[j-1]+cost);
            }
            int[] t=prev; prev=cur; cur=t;
        }
        return prev[b.length()];
    }
}
