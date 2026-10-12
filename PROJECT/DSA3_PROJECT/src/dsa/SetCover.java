package dsa;

import model.Candidate;
import java.util.*;

public class SetCover {
    public Result greedy(List<Candidate> candidates, Set<String> required, SkillNormalizer normalizer) {
        Set<String> uncovered=new HashSet<>(normalizer.normalizeAll(required));
        List<Candidate> chosen=new ArrayList<>();
        Set<Integer> used=new HashSet<>();
        while(!uncovered.isEmpty()){
            Candidate best=null; Set<String> bestGain=Collections.emptySet();
            for(Candidate c:candidates) if(!used.contains(c.id)){
                Set<String> skills=normalizer.normalizeAll(c.skills);
                Set<String> gain=new HashSet<>(skills); gain.retainAll(uncovered);
                if(gain.size()>bestGain.size()) {best=c;bestGain=gain;}
            }
            if(best==null||bestGain.isEmpty()) break;
            chosen.add(best);used.add(best.id);uncovered.removeAll(bestGain);
        }
        return new Result(chosen,uncovered);
    }

    public static class Result{
        public List<Candidate> chosen; public Set<String> uncovered;
        public Result(List<Candidate> c,Set<String> u){chosen=c;uncovered=u;}
        public boolean complete(){return uncovered.isEmpty();}
    }
}
