package dsa;

import model.Candidate;
import model.Job;
import java.util.*;

public class CandidateRanking {
    private final MatchEngine engine;
    public CandidateRanking(MatchEngine engine){this.engine=engine;}

    public List<ScoredCandidate> sortedCandidates(List<Candidate> candidates, Job job) {
        List<ScoredCandidate> out=new ArrayList<>();
        for(Candidate c:candidates) out.add(new ScoredCandidate(c,engine.score(c,job)));
        out.sort((a,b)->Double.compare(b.result.total,a.result.total));
        return out;
    }

    public List<ScoredCandidate> topWithHeap(List<Candidate> candidates, Job job, int k) {
        PriorityQueue<ScoredCandidate> heap = new PriorityQueue<>(
            Comparator.comparingDouble(x->x.result.total)
        );
        for(Candidate c:candidates){
            heap.offer(new ScoredCandidate(c,engine.score(c,job)));
            if(heap.size()>k) heap.poll();
        }
        List<ScoredCandidate> out=new ArrayList<>(heap);
        out.sort((a,b)->Double.compare(b.result.total,a.result.total));
        return out;
    }

    public static class ScoredCandidate {
        public Candidate candidate;
        public MatchEngine.MatchResult result;
        public ScoredCandidate(Candidate c,MatchEngine.MatchResult r){candidate=c;result=r;}
    }
}
