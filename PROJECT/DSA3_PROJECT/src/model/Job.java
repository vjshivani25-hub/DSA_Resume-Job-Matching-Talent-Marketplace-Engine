package model;

import java.util.*;

public class Job {
    public int id;
    public String title;
    public String company;
    public String location;
    public String description;
    public List<String> skills = new ArrayList<>();
    public int minExperience;
    public String education;
    public int projects;
    public int certifications;

    public Job(int id, String title, String company, String location,
               String description, List<String> skills, int minExperience,
               String education, int projects, int certifications) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.description = description;
        this.skills.addAll(skills);
        this.minExperience = minExperience;
        this.education = education;
        this.projects = projects;
        this.certifications = certifications;
    }
}
