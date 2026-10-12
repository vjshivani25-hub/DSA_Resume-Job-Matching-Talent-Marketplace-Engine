# TalentMatch — Resume–Job Matching & Talent-Marketplace Engine

Java + DSA semester project with an in-memory backend and a role-based web interface.

## Roles
- Candidate: profile, job search, apply, application tracking, application status timeline.
- Recruiter: post jobs, discover talent, open applications, review candidate profiles, accept/reject candidates, live application pipeline.
- Admin: platform overview, live application monitoring, hiring decision counts, analytics and optimization controls.

## Core DSA
1. HashMap / HashSet skill normalization
2. Fuzzy / semantic-style skill similarity
3. Weighted Fit Score (50/20/10/10/10)
4. Sorting
5. PriorityQueue / Max Heap
6. Hungarian Algorithm
7. Greedy Set Cover

## Run in PowerShell
```powershell
javac -d out (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName })
java -cp out Main
```
Open `http://localhost:8080`.

Keep the Java terminal open while using the website.

## Demo accounts
- Candidate: `alex@example.com` / `demo123`
- Recruiter: `recruiter@example.com` / `demo123`
- Admin: `admin@example.com` / `admin123`

New candidates created from the Candidate portal can sign in with their email and `demo123`.

## Live application workflow
1. Candidate logs in and applies for a job.
2. Recruiter dashboard receives the application in the pipeline.
3. Recruiter clicks **Open** to inspect the candidate, job, skills and application timeline.
4. Recruiter can **Accept Candidate** or **Reject Candidate**.
5. Candidate can track the decision from their dashboard.
6. Admin dashboard automatically refreshes application activity and decision counts.

No database is used; the Java `DataStore` keeps data in memory for the demo.
