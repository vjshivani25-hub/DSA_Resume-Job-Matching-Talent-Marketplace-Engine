package model;

import java.util.*;

public class Candidate {
    public int id;
    public String name;
    public String email;
    public String title;
    public int experience;
    public String education;
    public List<String> skills = new ArrayList<>();
    public int projects;
    public int certifications;
    public String location;
    public String bio;

    public Candidate(int id, String name, String email, String title, int experience,
                     String education, List<String> skills, int projects,
                     int certifications, String location, String bio) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.title = title;
        this.experience = experience;
        this.education = education;
        this.skills.addAll(skills);
        this.projects = projects;
        this.certifications = certifications;
        this.location = location;
        this.bio = bio;
    }

    public String toString() { return name + " (" + title + ")"; }
}
