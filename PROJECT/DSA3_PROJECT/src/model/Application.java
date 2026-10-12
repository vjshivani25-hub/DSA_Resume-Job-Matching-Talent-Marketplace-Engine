package model;

public class Application {
    public int id;
    public int candidateId;
    public int jobId;
    public String status;
    public String appliedAt;
    public String updatedAt;

    public Application(int id, int candidateId, int jobId, String status, String appliedAt) {
        this.id = id;
        this.candidateId = candidateId;
        this.jobId = jobId;
        this.status = status;
        this.appliedAt = appliedAt;
        this.updatedAt = appliedAt;
    }
}
