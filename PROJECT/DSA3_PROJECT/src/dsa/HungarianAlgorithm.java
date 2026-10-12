package dsa;

import java.util.*;

public class HungarianAlgorithm {
    // Maximum-weight assignment using the classic O(n^3) Hungarian method.
    // The method pads rectangular matrices with zero-value dummy rows/columns.
    public Result maximize(double[][] scores) {
        int n=scores.length, m=n==0?0:scores[0].length;
        int size=Math.max(n,m);
        if(size==0) return new Result(new int[0],0);
        double max=0;
        for(double[] row:scores) for(double v:row) max=Math.max(max,v);
        double[][] cost=new double[size+1][size+1];
        for(int i=1;i<=size;i++) for(int j=1;j<=size;j++){
            double v=(i<=n&&j<=m)?scores[i-1][j-1]:0;
            cost[i][j]=max-v;
        }
        double[] u=new double[size+1], v=new double[size+1];
        int[] p=new int[size+1], way=new int[size+1];
        for(int i=1;i<=size;i++){
            p[0]=i;
            int j0=0;
            double[] minv=new double[size+1];
            Arrays.fill(minv,Double.POSITIVE_INFINITY);
            boolean[] used=new boolean[size+1];
            do{
                used[j0]=true;
                int i0=p[j0],j1=0;
                double delta=Double.POSITIVE_INFINITY;
                for(int j=1;j<=size;j++) if(!used[j]){
                    double cur=cost[i0][j]-u[i0]-v[j];
                    if(cur<minv[j]){minv[j]=cur;way[j]=j0;}
                    if(minv[j]<delta){delta=minv[j];j1=j;}
                }
                for(int j=0;j<=size;j++){
                    if(used[j]){u[p[j]]+=delta;v[j]-=delta;}
                    else minv[j]-=delta;
                }
                j0=j1;
            }while(p[j0]!=0);
            do{
                int j1=way[j0];
                p[j0]=p[j1];
                j0=j1;
            }while(j0!=0);
        }
        int[] assignment=new int[n];
        Arrays.fill(assignment,-1);
        double total=0;
        for(int j=1;j<=size;j++){
            int i=p[j];
            if(i>=1&&i<=n&&j<=m){assignment[i-1]=j-1;total+=scores[i-1][j-1];}
        }
        return new Result(assignment,total);
    }

    public static class Result{
        public int[] assignment; public double totalScore;
        public Result(int[] a,double s){assignment=a;totalScore=s;}
    }
}
