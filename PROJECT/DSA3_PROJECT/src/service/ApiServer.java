package service;

import com.sun.net.httpserver.*;
import dsa.*;
import model.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ApiServer {
    private final DataStore store;
    private final SkillNormalizer normalizer=new SkillNormalizer();
    private final MatchEngine matcher=new MatchEngine(normalizer);
    private final CandidateRanking ranking=new CandidateRanking(matcher);
    private final HungarianAlgorithm hungarian=new HungarianAlgorithm();
    private final SetCover setCover=new SetCover();

    public ApiServer(DataStore store){this.store=store;}

    public void start(int port) throws IOException {
        HttpServer server=HttpServer.create(new InetSocketAddress(port),0);
        server.createContext("/",this::staticFile);
        server.createContext("/api/dashboard",this::dashboard);
        server.createContext("/api/candidates",this::candidates);
        server.createContext("/api/jobs",this::jobs);
        server.createContext("/api/match",this::match);
        server.createContext("/api/assignment",this::assignment);
        server.createContext("/api/team",this::team);
        server.createContext("/api/algorithms",this::algorithms);
        server.createContext("/api/applications",this::applications);
        server.createContext("/api/applications/status",this::applicationStatus);
        server.setExecutor(null);
        server.start();
        System.out.println("TalentMatch running at http://localhost:"+port);
    }

    private void staticFile(HttpExchange ex) throws IOException {
        String path=ex.getRequestURI().getPath();
        if(path.equals("/")) path="/index.html";
        if(path.contains("..")) {send(ex,403,"Forbidden","text/plain");return;}
        File f=new File("web",path.substring(1));
        if(!f.exists()||f.isDirectory()){send(ex,404,"Not Found","text/plain");return;}
        byte[] data=java.nio.file.Files.readAllBytes(f.toPath());
        String ct=path.endsWith(".css")?"text/css":path.endsWith(".js")?"application/javascript":"text/html";
        ex.getResponseHeaders().set("Content-Type",ct+"; charset=UTF-8");
        ex.sendResponseHeaders(200,data.length);
        try(OutputStream o=ex.getResponseBody()){o.write(data);}
    }

    private void dashboard(HttpExchange ex)throws IOException{
        String json="{\"candidates\":"+store.candidates.size()+",\"jobs\":"+store.jobs.size()+
            ",\"applications\":"+store.applications.size()+",\"skills\":"+normalizer.aliases().size()+
            ",\"algorithmCount\":7}";
        sendJson(ex,json);
    }

    private void candidates(HttpExchange ex)throws IOException{
        if("POST".equalsIgnoreCase(ex.getRequestMethod())){
            Map<String,String> p=parseBody(ex);
            Candidate c=store.addCandidate(p.getOrDefault("name","New Candidate"),p.getOrDefault("email",""),
                p.getOrDefault("title","Software Engineer"),integer(p,"experience",0),p.getOrDefault("education","Bachelor"),
                list(p.get("skills")),integer(p,"projects",0),integer(p,"certifications",0),
                p.getOrDefault("location","Remote"),p.getOrDefault("bio",""));
            sendJson(ex,candidateJson(c));
            return;
        }
        StringBuilder b=new StringBuilder("[");
        for(int i=0;i<store.candidates.size();i++){if(i>0)b.append(",");b.append(candidateJson(store.candidates.get(i)));}
        sendJson(ex,b.append("]").toString());
    }

    private void jobs(HttpExchange ex)throws IOException{
        if("POST".equalsIgnoreCase(ex.getRequestMethod())){
            Map<String,String> p=parseBody(ex);
            Job j=store.addJob(p.getOrDefault("title","Software Engineer"),p.getOrDefault("company","New Company"),
                p.getOrDefault("location","Remote"),p.getOrDefault("description",""),
                list(p.get("skills")),integer(p,"minExperience",0),p.getOrDefault("education","Bachelor"),
                integer(p,"projects",0),integer(p,"certifications",0));
            sendJson(ex,jobJson(j));
            return;
        }
        StringBuilder b=new StringBuilder("[");
        for(int i=0;i<store.jobs.size();i++){if(i>0)b.append(",");b.append(jobJson(store.jobs.get(i)));}
        sendJson(ex,b.append("]").toString());
    }

    private void match(HttpExchange ex)throws IOException{
        Map<String,String> p=parseBody(ex);
        Candidate c=store.candidate(integer(p,"candidateId",1));
        Job j=store.job(integer(p,"jobId",1));
        if(c==null||j==null){sendJson(ex,"{\"error\":\"Candidate or job not found\"}");return;}
        MatchEngine.MatchResult r=matcher.score(c,j);
        sendJson(ex,"{\"candidate\":"+candidateJson(c)+",\"job\":"+jobJson(j)+",\"score\":"+Json.number(r.total)+
            ",\"skillScore\":"+Json.number(r.skill)+",\"experienceScore\":"+Json.number(r.experience)+
            ",\"educationScore\":"+Json.number(r.education)+",\"projectScore\":"+Json.number(r.projects)+
            ",\"certificationScore\":"+Json.number(r.certifications)+",\"matched\":"+Json.arr(r.matchedSkills)+
            ",\"missing\":"+Json.arr(r.missingSkills)+"}");
    }

    private void assignment(HttpExchange ex)throws IOException{
        double[][] s=new double[store.candidates.size()][store.jobs.size()];
        for(int i=0;i<s.length;i++)for(int j=0;j<s[i].length;j++)s[i][j]=matcher.score(store.candidates.get(i),store.jobs.get(j)).total;
        HungarianAlgorithm.Result r=hungarian.maximize(s);
        StringBuilder b=new StringBuilder("{\"totalScore\":").append(Json.number(r.totalScore)).append(",\"pairs\":[");
        boolean first=true;
        for(int i=0;i<r.assignment.length;i++){
            int j=r.assignment[i];
            if(j>=0&&j<store.jobs.size()){
                if(!first)b.append(",");first=false;
                b.append("{\"candidate\":").append(candidateJson(store.candidates.get(i)))
                 .append(",\"job\":").append(jobJson(store.jobs.get(j)))
                 .append(",\"score\":").append(Json.number(s[i][j])).append("}");
            }
        }
        sendJson(ex,b.append("]}").toString());
    }

    private void team(HttpExchange ex)throws IOException{
        Map<String,String> p=parseBody(ex);
        Set<String> req=new LinkedHashSet<>(list(p.get("skills")));
        if(req.isEmpty()) req.addAll(Arrays.asList("Java","SQL","React","Git","DSA"));
        SetCover.Result r=setCover.greedy(store.candidates,req,normalizer);
        StringBuilder b=new StringBuilder("{\"complete\":").append(r.complete()).append(",\"required\":").append(Json.arr(normalizer.normalizeAll(req)))
            .append(",\"uncovered\":").append(Json.arr(r.uncovered.stream().toList())).append(",\"team\":[");
        for(int i=0;i<r.chosen.size();i++){if(i>0)b.append(",");b.append(candidateJson(r.chosen.get(i)));}
        sendJson(ex,b.append("]}").toString());
    }

    private void algorithms(HttpExchange ex)throws IOException{
        Map<String,String> p=parseBody(ex);
        String type=p.getOrDefault("type","all");
        if(type.equals("hashmap")) {
            sendJson(ex,"{\"name\":\"HashMap / HashSet\",\"result\":\"ReactJS → React, Java Programming → Java, SQL duplicates removed\",\"complexity\":\"Average O(1) lookup\"}");
        } else if(type.equals("fuzzy")) {
            SimilarityEngine sim=new SimilarityEngine(normalizer);
            double a=sim.skillSimilarity("ReactJS","React"), b=sim.skillSimilarity("Java Programming","Java");
            sendJson(ex,"{\"name\":\"Fuzzy Skill Matching\",\"result\":\"ReactJS ↔ React = "+Json.number(a*100)+"%, Java Programming ↔ Java = "+Json.number(b*100)+"%\",\"complexity\":\"O(L1×L2) per comparison\"}");
        } else if(type.equals("fit")) {
            Candidate c=store.candidates.get(0); Job j=store.jobs.get(0); MatchEngine.MatchResult r=matcher.score(c,j);
            sendJson(ex,"{\"name\":\"Weighted Fit Score\",\"result\":\"Alex Johnson ↔ Java Full Stack = "+Json.number(r.total)+"%\",\"formula\":\"0.50 Skill + 0.20 Experience + 0.10 Education + 0.10 Projects + 0.10 Certification\"}");
        } else if(type.equals("heap")) {
            Job j=store.jobs.get(0); List<CandidateRanking.ScoredCandidate> top=ranking.topWithHeap(store.candidates,j,3);
            StringBuilder b=new StringBuilder("{\"name\":\"PriorityQueue / Max Heap\",\"top\":[");
            for(int i=0;i<top.size();i++){if(i>0)b.append(",");b.append("{\"name\":").append(Json.quote(top.get(i).candidate.name)).append(",\"score\":").append(Json.number(top.get(i).result.total)).append("}");}
            sendJson(ex,b.append("],\"complexity\":\"O(n log k)\"}").toString());
        } else if(type.equals("hungarian")) {
            sendJson(ex,assignmentResult());
        } else if(type.equals("setcover")) {
            SetCover.Result r=setCover.greedy(store.candidates,new HashSet<>(Arrays.asList("Java","SQL","React","Git","DSA")),normalizer);
            sendJson(ex,"{\"name\":\"Set Cover\",\"teamSize\":"+r.chosen.size()+",\"complete\":"+r.complete()+",\"complexity\":\"Greedy approximation\"}");
        } else {
            sendJson(ex,"{\"name\":\"DSA Algorithm Lab\",\"result\":\"7 algorithms ready\",\"algorithms\":[\"HashMap / HashSet\",\"Fuzzy Matching\",\"Weighted Fit Score\",\"Sorting\",\"PriorityQueue / Max Heap\",\"Hungarian Algorithm\",\"Set Cover\"]}");
        }
    }

    private String assignmentResult(){
        double[][] s=new double[store.candidates.size()][store.jobs.size()];
        for(int i=0;i<s.length;i++)for(int j=0;j<s[i].length;j++)s[i][j]=matcher.score(store.candidates.get(i),store.jobs.get(j)).total;
        HungarianAlgorithm.Result r=hungarian.maximize(s);
        return "{\"name\":\"Hungarian Algorithm\",\"totalScore\":"+Json.number(r.totalScore)+",\"assignments\":"+r.assignment.length+"}";
    }

    private void applications(HttpExchange ex)throws IOException{
        if("POST".equalsIgnoreCase(ex.getRequestMethod())){
            Map<String,String> p=parseBody(ex);
            int cid=integer(p,"candidateId",1), jid=integer(p,"jobId",1);
            if(store.candidate(cid)==null || store.job(jid)==null){
                send(ex,400,"{\"error\":\"Candidate or job not found\"}","application/json");
                return;
            }
            Application a=store.addApplication(cid,jid);
            sendJson(ex,applicationJson(a));return;
        }
        StringBuilder b=new StringBuilder("[");
        for(int i=0;i<store.applications.size();i++){if(i>0)b.append(",");b.append(applicationJson(store.applications.get(i)));}
        sendJson(ex,b.append("]").toString());
    }

    private void applicationStatus(HttpExchange ex)throws IOException{
        if(!"POST".equalsIgnoreCase(ex.getRequestMethod())){
            send(ex,405,"{\"error\":\"POST required\"}","application/json");
            return;
        }
        Map<String,String> p=parseBody(ex);
        int id=integer(p,"id",-1);
        String status=p.getOrDefault("status","Applied");
        Application found=null;
        for(Application a:store.applications) if(a.id==id){found=a;break;}
        if(found==null){send(ex,404,"{\"error\":\"Application not found\"}","application/json");return;}
        found.status=status;
        found.updatedAt=new java.util.Date().toString();
        sendJson(ex,applicationJson(found));
    }

    private String candidateJson(Candidate c){
        return "{\"id\":"+c.id+",\"name\":"+Json.quote(c.name)+",\"email\":"+Json.quote(c.email)+",\"title\":"+Json.quote(c.title)+
            ",\"experience\":"+c.experience+",\"education\":"+Json.quote(c.education)+",\"skills\":"+Json.arr(c.skills)+
            ",\"projects\":"+c.projects+",\"certifications\":"+c.certifications+",\"location\":"+Json.quote(c.location)+
            ",\"bio\":"+Json.quote(c.bio)+"}";
    }
    private String jobJson(Job j){
        return "{\"id\":"+j.id+",\"title\":"+Json.quote(j.title)+",\"company\":"+Json.quote(j.company)+",\"location\":"+Json.quote(j.location)+
            ",\"description\":"+Json.quote(j.description)+",\"skills\":"+Json.arr(j.skills)+",\"minExperience\":"+j.minExperience+
            ",\"education\":"+Json.quote(j.education)+",\"projects\":"+j.projects+",\"certifications\":"+j.certifications+"}";
    }
    private String applicationJson(Application a){
        return "{\"id\":"+a.id+",\"candidateId\":"+a.candidateId+",\"jobId\":"+a.jobId+
            ",\"status\":"+Json.quote(a.status)+",\"appliedAt\":"+Json.quote(a.appliedAt)+",\"updatedAt\":"+Json.quote(a.updatedAt)+"}";
    }

    private Map<String,String> parseBody(HttpExchange ex)throws IOException{
        String body=new String(ex.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
        Map<String,String> map=new HashMap<>();
        for(String pair:body.split("&")){
            if(pair.isBlank())continue;
            String[] kv=pair.split("=",2);
            String k=URLDecoder.decode(kv[0],StandardCharsets.UTF_8);
            String v=kv.length>1?URLDecoder.decode(kv[1],StandardCharsets.UTF_8):"";
            map.put(k,v);
        }
        return map;
    }
    private List<String> list(String x){
        if(x==null||x.isBlank())return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(x.split(",")));
    }
    private int integer(Map<String,String> p,String k,int d){try{return Integer.parseInt(p.getOrDefault(k,""+d));}catch(Exception e){return d;}}
    private void sendJson(HttpExchange ex,String body)throws IOException{send(ex,200,body,"application/json");}
    private void send(HttpExchange ex,int code,String body,String type)throws IOException{
        byte[] d=body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type",type+"; charset=UTF-8");
        ex.getResponseHeaders().set("Cache-Control","no-store");
        ex.sendResponseHeaders(code,d.length);
        try(OutputStream o=ex.getResponseBody()){o.write(d);}
    }
}
