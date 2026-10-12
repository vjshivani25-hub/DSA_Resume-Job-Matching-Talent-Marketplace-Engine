package dsa;

import model.Candidate;
import model.Job;
import java.util.*;

public class MatchEngine {
    private final SkillNormalizer normalizer;
    private final SimilarityEngine similarity;

    public MatchEngine(SkillNormalizer normalizer) {
        this.normalizer = normalizer;
        this.similarity = new SimilarityEngine(normalizer);
    }

    public MatchResult score(Candidate c, Job j) {
        double skill = similarity.setMatch(c.skills, j.skills);
        double experience = c.experience >= j.minExperience ? 1.0 :
                Math.max(0, 1.0 - (j.minExperience - c.experience) * 0.25);
        double education = educationScore(c.education, j.education);
        double projects = j.projects <= 0 ? 1.0 : Math.min(1.0, (double)c.projects / j.projects);
        double cert = j.certifications <= 0 ? 1.0 : Math.min(1.0, (double)c.certifications / j.certifications);
        double total = 0.50*skill + 0.20*experience + 0.10*education + 0.10*projects + 0.10*cert;

        Set<String> cs = normalizer.normalizeAll(c.skills);
        Set<String> js = normalizer.normalizeAll(j.skills);
        Set<String> matched = new LinkedHashSet<>();
        Set<String> missing = new LinkedHashSet<>();
        for (String req : js) {
            boolean ok=false;
            for(String have:cs) if(similarity.skillSimilarity(have,req)>=0.70){ok=true;break;}
            if(ok) matched.add(req); else missing.add(req);
        }
        return new MatchResult(total*100, skill*100, experience*100, education*100,
                projects*100, cert*100, matched, missing);
    }

    private double educationScore(String candidate, String required) {
        if (required == null || required.isBlank()) return 1;
        String a=canonicalEdu(candidate), b=canonicalEdu(required);
        if (a.equals(b)) return 1;
        if (a.contains(b) || b.contains(a)) return .85;
        return .60;
    }

    private String canonicalEdu(String s) {
        if (s==null) return "";
        s=s.toLowerCase();
        if(s.contains("phd")) return "phd";
        if(s.contains("master")||s.contains("m.tech")||s.contains("mca")||s.contains("msc")) return "master";
        if(s.contains("bachelor")||s.contains("b.tech")||s.contains("b.e")||s.contains("bsc")||s.contains("bca")) return "bachelor";
        return s;
    }

    public static class MatchResult {
        public double total, skill, experience, education, projects, certifications;
        public Set<String> matchedSkills, missingSkills;
        public MatchResult(double total,double skill,double experience,double education,double projects,
                           double certifications,Set<String> matchedSkills,Set<String> missingSkills){
            this.total=total;this.skill=skill;this.experience=experience;this.education=education;
            this.projects=projects;this.certifications=certifications;
            this.matchedSkills=matchedSkills;this.missingSkills=missingSkills;
        }
    }
}
