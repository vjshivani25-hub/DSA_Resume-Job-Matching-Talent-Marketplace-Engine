package dsa;

import java.util.*;

public class SkillNormalizer {
    private final Map<String, String> aliasToCanonical = new HashMap<>();
    private final Map<String, Set<String>> related = new HashMap<>();

    public SkillNormalizer() {
        addAliases("Java", "java", "java programming", "core java", "java se", "jdk");
        addAliases("JavaScript", "javascript", "js", "ecmascript");
        addAliases("React", "react", "reactjs", "react.js", "react js");
        addAliases("Node.js", "node", "nodejs", "node.js");
        addAliases("Spring Boot", "spring", "spring boot", "springboot");
        addAliases("SQL", "sql", "mysql", "postgresql", "database sql");
        addAliases("HTML", "html", "html5");
        addAliases("CSS", "css", "css3");
        addAliases("Python", "python", "py");
        addAliases("DSA", "dsa", "data structures", "data structures and algorithms", "algorithms");
        addAliases("Git", "git", "github", "gitlab");
        addAliases("REST API", "rest", "rest api", "restful", "api development");
        addAliases("Machine Learning", "machine learning", "ml", "machine-learning");
        addAliases("AWS", "aws", "amazon web services");
        addAliases("Docker", "docker", "containerization");
        addAliases("C++", "c++", "cpp", "cplusplus");
        addAliases("C", "c", "c programming");
        addAliases("Figma", "figma", "ui ux", "ui/ux");
        addAliases("Communication", "communication", "communication skills", "verbal communication");

        connect("React", "JavaScript");
        connect("Node.js", "JavaScript");
        connect("Spring Boot", "Java");
        connect("REST API", "Node.js");
        connect("REST API", "Spring Boot");
        connect("SQL", "Java");
        connect("Docker", "AWS");
        connect("Git", "Java");
        connect("DSA", "Java");
    }

    private void addAliases(String canonical, String... aliases) {
        for (String a : aliases) aliasToCanonical.put(clean(a), canonical);
    }

    private void connect(String a, String b) {
        related.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        related.computeIfAbsent(b, k -> new HashSet<>()).add(a);
    }

    private String clean(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#.]+", " ").trim();
    }

    public String normalize(String skill) {
        String c = clean(skill);
        if (aliasToCanonical.containsKey(c)) return aliasToCanonical.get(c);
        for (Map.Entry<String,String> e : aliasToCanonical.entrySet()) {
            if (c.equals(e.getKey()) || c.replace(".", "").equals(e.getKey().replace(".", ""))) return e.getValue();
        }
        return titleCase(c);
    }

    public Set<String> normalizeAll(Collection<String> skills) {
        Set<String> out = new HashSet<>();
        for (String s : skills) if (s != null && !s.isBlank()) out.add(normalize(s));
        return out;
    }

    public Set<String> relatedSkills(String canonical) {
        return related.getOrDefault(canonical, Collections.emptySet());
    }

    public Map<String,String> aliases() { return Collections.unmodifiableMap(aliasToCanonical); }

    private String titleCase(String x) {
        if (x.isEmpty()) return x;
        StringBuilder b = new StringBuilder();
        for (String p : x.split(" ")) {
            if (!p.isEmpty()) b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return b.toString().trim();
    }
}
