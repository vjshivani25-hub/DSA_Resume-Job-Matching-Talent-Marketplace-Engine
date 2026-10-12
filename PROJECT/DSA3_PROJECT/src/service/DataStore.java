package service;

import model.*;
import java.util.*;

public class DataStore {
    public final List<Candidate> candidates=new ArrayList<>();
    public final List<Job> jobs=new ArrayList<>();
    public final List<Application> applications=new ArrayList<>();
    private int candidateSeq=100, jobSeq=200, appSeq=1000;

    public DataStore(){
        seed();
    }

    private void seed(){
        candidates.add(new Candidate(1,"Alex Johnson","alex@example.com","Full Stack Developer",3,
            "B.Tech Computer Science",Arrays.asList("Java","Spring Boot","ReactJS","SQL","Git","DSA"),5,2,"Hyderabad",
            "Full-stack engineer focused on scalable web applications."));
        candidates.add(new Candidate(2,"Priya Sharma","priya@example.com","Frontend Engineer",2,
            "B.Tech Information Technology",Arrays.asList("React.js","JavaScript","HTML","CSS","Figma","Git"),4,1,"Bengaluru",
            "Frontend developer building accessible and responsive interfaces."));
        candidates.add(new Candidate(3,"Rahul Verma","rahul@example.com","Backend Engineer",4,
            "M.Tech Computer Science",Arrays.asList("Java Programming","Spring","REST API","SQL","Docker","AWS"),7,3,"Pune",
            "Backend engineer experienced in APIs, cloud deployment and microservices."));
        candidates.add(new Candidate(4,"Sneha Reddy","sneha@example.com","Data & ML Engineer",3,
            "M.Tech Data Science",Arrays.asList("Python","Machine Learning","SQL","Docker","Git"),6,2,"Hyderabad",
            "Data scientist working on machine learning pipelines and analytics."));
        candidates.add(new Candidate(5,"Karthik Rao","karthik@example.com","Software Engineer",2,
            "BCA",Arrays.asList("Java","C++","DSA","SQL","Git","REST API"),3,1,"Chennai",
            "Software engineer with strong programming and algorithmic fundamentals."));

        jobs.add(new Job(1,"Java Full Stack Developer","TechNova","Hyderabad",
            "Build modern enterprise applications.",Arrays.asList("Java","Spring Boot","React","SQL","Git","REST API"),2,
            "Bachelor",3,1));
        jobs.add(new Job(2,"Frontend React Engineer","CloudSpark","Bengaluru",
            "Create polished web experiences.",Arrays.asList("React","JavaScript","HTML","CSS","Git"),1,
            "Bachelor",2,1));
        jobs.add(new Job(3,"Backend Cloud Engineer","DataBridge","Pune",
            "Design APIs and cloud-ready backend services.",Arrays.asList("Java","Spring Boot","REST API","Docker","AWS","SQL"),3,
            "Bachelor",4,2));
        jobs.add(new Job(4,"ML Engineer","InsightAI","Hyderabad",
            "Develop machine learning solutions.",Arrays.asList("Python","Machine Learning","SQL","Docker","Git"),2,
            "Master",4,1));
        jobs.add(new Job(5,"Software Engineer","InnovateLabs","Remote",
            "Solve engineering problems using strong DSA.",Arrays.asList("Java","DSA","SQL","Git","REST API"),1,
            "Bachelor",2,1));
    }

    public synchronized Candidate addCandidate(String name,String email,String title,int experience,String education,
                                                List<String> skills,int projects,int certs,String location,String bio){
        Candidate c=new Candidate(++candidateSeq,name,email,title,experience,education,skills,projects,certs,location,bio);
        candidates.add(c);return c;
    }

    public synchronized Job addJob(String title,String company,String location,String description,List<String> skills,
                                    int minExp,String education,int projects,int certs){
        Job j=new Job(++jobSeq,title,company,location,description,skills,minExp,education,projects,certs);
        jobs.add(j);return j;
    }

    public synchronized Application addApplication(int cid,int jid){
        for(Application a:applications) if(a.candidateId==cid&&a.jobId==jid) return a;
        Application a=new Application(++appSeq,cid,jid,"Applied",new Date().toString());
        applications.add(a);return a;
    }

    public Candidate candidate(int id){return candidates.stream().filter(c->c.id==id).findFirst().orElse(null);}
    public Job job(int id){return jobs.stream().filter(j->j.id==id).findFirst().orElse(null);}
}
