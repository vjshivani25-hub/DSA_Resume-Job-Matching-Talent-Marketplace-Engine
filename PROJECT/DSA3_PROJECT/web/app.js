const $ = s => document.querySelector(s);
let candidates = [], jobs = [], applications = [];
let currentRole = null, currentEmail = null, pendingApplyJobId = null, liveTimer = null;
const DEMO_USERS = {
  candidate: { email: "alex@example.com", pass: "demo123", name: "Alex Johnson" },
  recruiter: { email: "recruiter@example.com", pass: "demo123", name: "Recruiter Demo" },
  admin: { email: "admin@example.com", pass: "admin123", name: "Admin Demo" }
};

async function api(url, options = {}) {
  const r = await fetch(url, options);
  let data = {};
  try { data = await r.json(); } catch (_) {}
  if (!r.ok || data.error) throw new Error(data.error || `Request failed (${r.status})`);
  return data;
}
function formBody(obj) { return new URLSearchParams(obj).toString(); }
function toast(msg) {
  const t = $("#toast"); if (!t) return;
  t.textContent = msg; t.classList.add("show");
  clearTimeout(window.__toastTimer);
  window.__toastTimer = setTimeout(() => t.classList.remove("show"), 2600);
}
function escapeHtml(v) { return String(v ?? "").replace(/[&<>\"]/g, m => ({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;"}[m])); }
function initials(name) { return String(name || "TM").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase(); }
function tags(arr = []) { return arr.slice(0, 8).map(x => `<span class="tag">${escapeHtml(x)}</span>`).join(""); }
function scrollToId(id) { const el = document.getElementById(id); if (el) el.scrollIntoView({ behavior: "smooth", block: "start" }); }
function goHomeSection(id) { backToHome(); setTimeout(() => scrollToId(id), 60); return false; }
function toggleNav() { $("#publicNav")?.classList.toggle("mobile-open"); }
function setAppMode(active) {
  document.body.classList.toggle("app-mode", active);
  $("#publicApp")?.classList.toggle("public-hidden", active);
  $("#publicNav")?.classList.toggle("hidden", active);
  $("#navbar")?.classList.toggle("app-navbar", active);
  $("footer")?.classList.toggle("footer-hidden", active);
}
function showPage(id) {
  setAppMode(true);
  document.querySelectorAll(".app-page").forEach(x => x.classList.remove("page-active"));
  document.getElementById(id)?.classList.add("page-active");
  window.scrollTo(0, 0);
}
function backToHome() {
  setAppMode(false);
  document.querySelectorAll(".app-page").forEach(x => x.classList.remove("page-active"));
  window.scrollTo(0, 0);
  if (currentRole) updateNavForRole();
}
function openLogin(role = "candidate") {
  showPage("login");
  const btn = [...document.querySelectorAll(".role-tabs button")].find(b => b.textContent.toLowerCase() === role) || document.querySelector(".role-tabs button");
  selectRole(btn, role);
}
function logout() {
  stopLiveTracking();
  currentRole = null; currentEmail = null; pendingApplyJobId = null;
  localStorage.removeItem("talentmatchSession");
  $("#navUser").textContent = ""; $("#navLogin").textContent = "Login"; $("#navLogin").onclick = openLogin;
  backToHome(); toast("Logged out successfully ✓");
}
function updateNavForRole() {
  if (!currentRole) return;
  const user = currentRole === "candidate" ? (getCurrentCandidate() || DEMO_USERS.candidate) : DEMO_USERS[currentRole];
  $("#navUser").textContent = `${user.name} · ${currentRole}`;
  $("#navLogin").textContent = "Dashboard";
  $("#navLogin").onclick = () => openDashboard(currentRole);
}
function getCurrentCandidate() {
  if (currentRole !== "candidate") return null;
  return candidates.find(c => c.email.toLowerCase() === String(currentEmail || "").toLowerCase()) || null;
}
function currentCandidateId() { return getCurrentCandidate()?.id || null; }
function requireCandidate(action, jobId = null) {
  if (currentRole === "candidate" && currentCandidateId()) { action(); return; }
  if (jobId) pendingApplyJobId = jobId;
  openLogin("candidate");
  toast("Please login as a candidate to continue");
}
function requireRecruiter(action) {
  if (currentRole === "recruiter" || currentRole === "admin") { action(); return; }
  openLogin("recruiter");
  toast("Please login as a recruiter to post jobs");
}

async function init() {
  try {
    const [c, j, a, d] = await Promise.all([api("/api/candidates"), api("/api/jobs"), api("/api/applications"), api("/api/dashboard")]);
    candidates = c; jobs = j; applications = a;
    $("#statCandidates").textContent = d.candidates;
    $("#statJobs").textContent = d.jobs;
    $("#statApps").textContent = d.applications;
    $("#statSkills").textContent = d.skills;
    fillSelects(); renderCandidates(); renderJobs(); await renderTalent(); renderAnalytics();
    if (currentRole && document.getElementById("dashboard")?.classList.contains("page-active")) openDashboard(currentRole, false);
  } catch (e) { console.error(e); toast("Java server is not connected. Run the Java server first."); }
}
function fillSelects() {
  $("#matchCandidate").innerHTML = candidates.map(c => `<option value="${c.id}">${escapeHtml(c.name)} — ${escapeHtml(c.title)}</option>`).join("");
  $("#matchJob").innerHTML = jobs.map(j => `<option value="${j.id}">${escapeHtml(j.title)} — ${escapeHtml(j.company)}</option>`).join("");
  $("#talentJob").innerHTML = jobs.map(j => `<option value="${j.id}">${escapeHtml(j.title)}</option>`).join("");
}
function renderCandidates() {
  $("#candidateGrid").innerHTML = candidates.map(c => `<article class="candidate-card"><div class="profile-top"><div class="avatar">${initials(c.name)}</div><span class="online-dot">Available</span></div><h3>${escapeHtml(c.name)}</h3><div class="muted">${escapeHtml(c.title)} • ${escapeHtml(c.location)}</div><div class="card-tags">${tags(c.skills)}</div><div class="muted">${c.experience} yrs experience • ${c.projects} projects • ${c.certifications} certifications</div><div class="card-actions"><button class="smallbtn" onclick="openProfile(${c.id})">View Profile</button><button class="smallbtn apply" onclick="prepareMatch(${c.id})">Match</button></div></article>`).join("");
}
function jobCard(j, dashboard = false) {
  const applied = currentRole === "candidate" && applications.some(a => a.candidateId === currentCandidateId() && a.jobId === j.id);
  return `<article class="job-card"><div class="job-company"><span class="company-logo">${initials(j.company)}</span><div><div class="eyebrow">${escapeHtml(j.company)}</div><h3>${escapeHtml(j.title)}</h3></div></div><div class="muted">📍 ${escapeHtml(j.location)} • ${j.minExperience}+ yrs</div><p class="job-description">${escapeHtml(j.description || "Opportunity available on TalentMatch.")}</p><div class="card-tags">${tags(j.skills)}</div><div class="job-meta"><span>Projects ${j.projects}+</span><span>Certifications ${j.certifications}+</span></div><div class="card-actions"><button class="smallbtn" onclick="jobMatch(${j.id})">Why match?</button>${currentRole === "recruiter" || currentRole === "admin" ? `<button class="smallbtn apply" onclick="showRecruiterTalent()">View talent →</button>` : `<button class="smallbtn ${applied ? "disabled-btn" : "apply"}" ${applied ? "disabled" : ""} onclick="applyJob(${j.id})">${applied ? "Applied ✓" : "Apply now →"}</button>`}</div></article>`;
}
function renderJobs() {
  const q = ($("#jobSearch")?.value || "").toLowerCase().trim();
  const filtered = jobs.filter(j => (`${j.title} ${j.company} ${j.skills.join(" ")} ${j.location}`).toLowerCase().includes(q));
  $("#jobGrid").innerHTML = filtered.length ? filtered.map(j => jobCard(j)).join("") : `<div class="empty-state"><h3>No jobs found</h3><p>Try another keyword or wait for a recruiter to publish a role.</p></div>`;
}
async function renderTalent() {
  const id = Number($("#talentJob")?.value || jobs[0]?.id); if (!id || !jobs.length) return;
  const job = jobs.find(j => j.id === id);
  try {
    const scores = await Promise.all(candidates.map(async c => ({ c, r: await api("/api/match", { method: "POST", headers: {"Content-Type":"application/x-www-form-urlencoded"}, body: formBody({candidateId:c.id, jobId:job.id}) }) })));
    scores.sort((a,b) => b.r.score - a.r.score);
    $("#talentList").innerHTML = scores.map((x,i) => `<div class="rank-row"><div class="rank-no">#${i+1}</div><div class="rank-person"><div class="avatar mini">${initials(x.c.name)}</div><div><b>${escapeHtml(x.c.name)}</b><div class="muted">${escapeHtml(x.c.title)} • ${escapeHtml(x.c.location)}</div></div></div><div class="rank-score">${x.r.score}%<small>fit</small></div><button class="smallbtn" onclick="showMatch(${x.c.id},${job.id})">Details</button></div>`).join("");
  } catch (e) { console.error(e); }
}
async function runMatch() { await showMatch(Number($("#matchCandidate").value), Number($("#matchJob").value)); }
async function showMatch(c,j) {
  try {
    const r = await api("/api/match", {method:"POST", headers:{"Content-Type":"application/x-www-form-urlencoded"}, body:formBody({candidateId:c,jobId:j})});
    $("#matchResult").classList.remove("empty");
    $("#matchResult").innerHTML = `<div class="match-result-head"><div><div class="muted">FIT SCORE</div><div class="score-big">${r.score}%</div><b>${escapeHtml(r.candidate.name)}</b> → ${escapeHtml(r.job.title)}</div><button class="btn primary" onclick="applyJob(${r.job.id})">Apply for this job</button></div><div class="score-grid"><div class="score-mini"><b>Skills 50%</b>${r.skillScore}%</div><div class="score-mini"><b>Experience 20%</b>${r.experienceScore}%</div><div class="score-mini"><b>Education 10%</b>${r.educationScore}%</div><div class="score-mini"><b>Projects 10%</b>${r.projectScore}%</div><div class="score-mini"><b>Certifications 10%</b>${r.certificationScore}%</div></div><div class="skill-block"><b>Matched skills</b><div>${r.matched.map(x=>`<span class="skill-good">✓ ${escapeHtml(x)}</span>`).join("") || "None"}</div><b>Skill gaps</b><div>${r.missing.map(x=>`<span class="skill-missing">△ ${escapeHtml(x)}</span>`).join("") || "No major skill gaps"}</div></div>`;
    scrollToId("matching");
  } catch(e) { toast(e.message); }
}
function setMatchCandidate(id) { $("#matchCandidate").value = id; runMatch(); }
function prepareMatch(id) { setMatchCandidate(id); scrollToId("matching"); }
function jobMatch(id) { $("#matchJob").value = id; runMatch(); }

async function applyJob(jobId) {
  if (currentRole !== "candidate" || !currentCandidateId()) { pendingApplyJobId = jobId; openLogin("candidate"); toast("Login as candidate to apply for this job"); return; }
  try {
    const existing = applications.find(a => a.candidateId === currentCandidateId() && a.jobId === jobId);
    if (existing) { toast("You already applied for this job ✓"); openDashboard("candidate"); return; }
    const a = await api("/api/applications", {method:"POST", headers:{"Content-Type":"application/x-www-form-urlencoded"}, body:formBody({candidateId:currentCandidateId(),jobId})});
    applications.push(a); $("#statApps").textContent = applications.length;
    toast("Application submitted successfully ✓"); openDashboard("candidate");
  } catch(e) { toast(e.message || "Could not apply"); }
}
async function runAssignment() {
  try { const r = await api("/api/assignment"); $("#assignmentResult").classList.remove("empty"); $("#assignmentResult").innerHTML = `<div><div class="muted">MAXIMUM TOTAL FIT</div><div class="score-big">${r.totalScore}</div><p class="muted">Hungarian algorithm created ${r.pairs.length} optimal assignments.</p></div>${r.pairs.map(x=>`<div class="assignment-row"><div><b>${escapeHtml(x.candidate.name)}</b><small class="muted">${escapeHtml(x.candidate.title)}</small></div><div class="arrow">→</div><div><b>${escapeHtml(x.job.title)}</b><small class="muted">${escapeHtml(x.job.company)}</small></div><strong>${x.score}%</strong></div>`).join("")}`; scrollToId("assignment"); toast("Hungarian optimization complete ✓"); } catch(e) { toast(e.message); }
}
async function runTeam() {
  try { const r = await api("/api/team", {method:"POST", headers:{"Content-Type":"application/x-www-form-urlencoded"}, body:formBody({skills:$("#teamSkills").value})}); $("#teamResult").classList.remove("empty"); $("#teamResult").innerHTML = `<div class="team-summary"><div><div class="muted">TEAM SIZE</div><div class="score-big">${r.team.length}</div></div><div><b>${r.complete ? "✓ 100% skill coverage" : "△ Partial coverage"}</b><p class="muted">Required: ${r.required.join(", ")}</p></div></div>${r.team.map(c=>`<div class="assignment-row"><div><b>${escapeHtml(c.name)}</b><small class="muted">${escapeHtml(c.title)}</small></div><div class="arrow">+</div><div>${tags(c.skills)}</div><strong>Selected</strong></div>`).join("")}${r.uncovered.length?`<p class="skill-missing">Uncovered: ${r.uncovered.join(", ")}</p>`:""}`; scrollToId("team"); toast("Set Cover team generated ✓"); } catch(e) { toast(e.message); }
}
function renderAnalytics() {
  if (!jobs.length || !candidates.length) return;
  Promise.all(candidates.map(c=>api("/api/match",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:formBody({candidateId:c.id,jobId:jobs[0].id})}))).then(rs=>{ const avg=rs.reduce((a,b)=>a+b.score,0)/rs.length; $("#avgScore").textContent=avg.toFixed(1)+"%"; $("#avgBar").style.width=avg+"%"; const best=[...rs].sort((a,b)=>b.score-a.score)[0]; $("#bestTalent").textContent=best.candidate.name; const freq={}; candidates.flatMap(c=>c.skills).forEach(s=>freq[s]=(freq[s]||0)+1); $("#topSkill").textContent=Object.entries(freq).sort((a,b)=>b[1]-a[1])[0]?.[0]||"—"; }).catch(()=>{});
}

function openCandidateModal() {
  $("#modalContent").innerHTML = `<div class="eyebrow">CANDIDATE PORTAL</div><h2>Create candidate profile</h2><p class="muted">The profile is added instantly to the Java in-memory engine.</p><form class="form-grid" onsubmit="saveCandidate(event)"><label>Name<input name="name" required></label><label>Email<input name="email" type="email" required></label><label>Job title<input name="title" value="Software Engineer"></label><label>Experience<input name="experience" type="number" min="0" value="2"></label><label>Education<input name="education" value="B.Tech Computer Science"></label><label>Location<input name="location" value="Hyderabad"></label><label>Projects<input name="projects" type="number" min="0" value="2"></label><label>Certifications<input name="certifications" type="number" min="0" value="1"></label><label class="full">Skills<input name="skills" required placeholder="Java, React, SQL, Git"></label><label class="full">Bio<textarea name="bio" placeholder="Tell recruiters about your strengths..."></textarea></label><button class="btn primary full" type="submit">Create Candidate Profile</button></form>`;
  $("#modal").classList.remove("hidden");
}
async function saveCandidate(e) { e.preventDefault(); try { const o=Object.fromEntries(new FormData(e.target)); const created=await api("/api/candidates",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:formBody(o)}); closeModal(); await init(); toast(`Candidate ${created.name} created ✓ Login with ${created.email} / demo123`); } catch(err) { toast(err.message || "Could not create candidate"); } }
function openJobModal() {
  $("#modalContent").innerHTML = `<div class="eyebrow">RECRUITER PORTAL</div><h2>Publish a new job</h2><p class="muted">The vacancy becomes available immediately in the job marketplace.</p><form class="form-grid" onsubmit="saveJob(event)"><label>Job title<input name="title" required></label><label>Company<input name="company" required></label><label>Location<input name="location" value="Hyderabad"></label><label>Min experience<input name="minExperience" type="number" min="0" value="1"></label><label>Education<input name="education" value="Bachelor"></label><label>Projects<input name="projects" type="number" min="0" value="2"></label><label>Certifications<input name="certifications" type="number" min="0" value="1"></label><label>Skills<input name="skills" required placeholder="Java, SQL, React"></label><label class="full">Description<textarea name="description" placeholder="Role responsibilities and opportunity details..."></textarea></label><button class="btn primary full" type="submit">Publish Job</button></form>`;
  $("#modal").classList.remove("hidden");
}
async function saveJob(e) { e.preventDefault(); try { const o=Object.fromEntries(new FormData(e.target)); await api("/api/jobs",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:formBody(o)}); closeModal(); await init(); toast("Job published successfully ✓"); if(currentRole) openDashboard(currentRole); } catch(err) { toast(err.message || "Could not publish job"); } }
function openProfile(id) { const c=candidates.find(x=>x.id===id); if(!c)return; $("#modalContent").innerHTML=`<div class="eyebrow">CANDIDATE PROFILE</div><div class="profile-modal-head"><div class="avatar large">${initials(c.name)}</div><div><h2>${escapeHtml(c.name)}</h2><p class="muted">${escapeHtml(c.title)} • ${escapeHtml(c.location)}</p></div></div><p class="profile-bio">${escapeHtml(c.bio || "Professional candidate profile.")}</p><div class="card-tags">${tags(c.skills)}</div><p class="muted">${c.experience} years experience • ${escapeHtml(c.education)} • ${c.projects} projects • ${c.certifications} certifications</p><button class="btn primary" style="margin-top:20px" onclick="closeModal();setMatchCandidate(${c.id})">Run Match →</button>`; $("#modal").classList.remove("hidden"); }
function closeModal() { $("#modal").classList.add("hidden"); }
function selectRole(btn, role) { if(!btn)return; document.querySelectorAll(".role-tabs button").forEach(x=>x.classList.remove("active")); btn.classList.add("active"); $("#loginEmail").value=DEMO_USERS[role].email; $("#loginPass").value=DEMO_USERS[role].pass; }
async function demoLogin() {
  const email=$("#loginEmail").value.trim().toLowerCase(), pass=$("#loginPass").value;
  let role = Object.keys(DEMO_USERS).find(r=>DEMO_USERS[r].email===email && DEMO_USERS[r].pass===pass);
  if (!role && pass === "demo123" && candidates.some(c=>c.email.toLowerCase()===email)) role = "candidate";
  if (!role) { toast("Invalid email or password ✕"); return; }
  currentRole=role; currentEmail=email;
  localStorage.setItem("talentmatchSession", JSON.stringify({role,email}));
  updateNavForRole(); openDashboard(role);
  if (pendingApplyJobId && role === "candidate") { const jobId=pendingApplyJobId; pendingApplyJobId=null; setTimeout(()=>applyJob(jobId),180); }
}
async function refreshLiveApplications(){
  try {
    applications = await api("/api/applications");
    $("#statApps").textContent = applications.length;
    if((currentRole === "recruiter" || currentRole === "admin") && document.getElementById("dashboard")?.classList.contains("page-active")){
      const content=$("#dashboardContent");
      if(currentRole === "recruiter") renderRecruiterDashboard(content);
      else renderAdminDashboard(content);
    }
  } catch(e) {}
}
function startLiveTracking(){
  clearInterval(liveTimer);
  if(currentRole === "recruiter" || currentRole === "admin") liveTimer=setInterval(refreshLiveApplications,3500);
}
function stopLiveTracking(){ clearInterval(liveTimer); liveTimer=null; }
function openDashboard(role=currentRole, move=true) {
  if (!role) return openLogin();
  if (move) showPage("dashboard"); else setAppMode(true);
  currentRole=role; updateNavForRole();
  if(role === "recruiter" || role === "admin") startLiveTracking(); else stopLiveTracking();
  const u = role === "candidate" ? (getCurrentCandidate() || DEMO_USERS.candidate) : DEMO_USERS[role];
  $("#dashRole").textContent=role.toUpperCase()+" PORTAL";
  $("#dashTitle").textContent="Welcome, "+u.name;
  $("#dashSubtitle").textContent=role==="candidate"?"Your personal workspace for jobs, applications and fit scores.":role==="recruiter"?"Your hiring workspace for jobs, talent and applications.":"Platform overview, marketplace activity and optimization controls.";
  const content=$("#dashboardContent");
  if(role==="candidate") renderCandidateDashboard(content);
  else if(role==="recruiter") renderRecruiterDashboard(content);
  else renderAdminDashboard(content);
}
function renderCandidateDashboard(content) {
  const c=getCurrentCandidate() || candidates[0]; if(!c){content.innerHTML='<div class="empty-state"><h3>No candidate profile found</h3></div>';return;}
  const mine=applications.filter(a=>a.candidateId===c.id);
  content.innerHTML=`<div class="dash-stats"><div><span>PROFILE</span><b>${c.experience} yrs</b><small>experience</small></div><div><span>OPEN JOBS</span><b>${jobs.length}</b><small>opportunities</small></div><div><span>APPLICATIONS</span><b>${mine.length}</b><small>submitted</small></div><div><span>SKILLS</span><b>${c.skills.length}</b><small>profile skills</small></div></div><div class="dashboard-grid wide-grid"><div class="dash-card highlight"><div class="profile-top"><div class="avatar large">${initials(c.name)}</div><span class="status-pill">Profile active</span></div><h3>${escapeHtml(c.name)}</h3><p>${escapeHtml(c.title)} • ${escapeHtml(c.location)}</p><div class="card-tags">${tags(c.skills)}</div><button class="smallbtn apply" onclick="openProfile(${c.id})">View My Profile</button></div><div class="dash-card"><div class="card-title-row"><div><span>BEST OPPORTUNITIES</span><h3>Recommended for you</h3></div></div>${jobs.slice(0,3).map(j=>`<div class="mini-job"><div><b>${escapeHtml(j.title)}</b><small>${escapeHtml(j.company)} · ${escapeHtml(j.location)}</small></div><button class="smallbtn apply" onclick="applyJob(${j.id})">Apply</button></div>`).join("")}</div></div><div class="dash-card full-card"><div class="card-title-row"><div><span>JOB MARKETPLACE</span><h3>Find your next opportunity</h3></div><input id="dashJobSearch" class="search dash-search" placeholder="Search jobs..." oninput="renderCandidateDashboardJobs()"></div><div class="dashboard-job-grid" id="dashboardJobGrid">${jobs.map(j=>jobCard(j,true)).join("")}</div></div><div class="dash-card full-card"><div class="card-title-row"><div><span>MY APPLICATIONS</span><h3>Application tracking</h3></div></div>${renderApplicationRows(mine,"candidate")}</div>`;
}
function renderCandidateDashboardJobs() { const q=( $("#dashJobSearch")?.value || "").toLowerCase().trim(); const list=jobs.filter(j=>(j.title+" "+j.company+" "+j.skills.join(" ")).toLowerCase().includes(q)); $("#dashboardJobGrid").innerHTML=list.map(j=>jobCard(j,true)).join("") || '<div class="empty-state"><h3>No matching jobs</h3></div>'; }
function renderRecruiterDashboard(content) {
  const liveApps = applications.slice().reverse().slice(0,6);
  content.innerHTML=`<div class="dash-stats"><div><span>LIVE JOBS</span><b>${jobs.length}</b><small>published</small></div><div><span>TALENT POOL</span><b>${candidates.length}</b><small>candidates</small></div><div><span>APPLICATIONS</span><b>${applications.length}</b><small>received</small></div><div><span>ACTIVE PIPELINE</span><b>${applications.filter(a=>!['Rejected','Accepted'].includes(a.status)).length}</b><small>in progress</small></div></div><div class="live-panel"><div class="live-panel-head"><div><span class="eyebrow">LIVE HIRING TRACKER</span><h3>Application activity is updating automatically</h3></div><span class="live-indicator"><i></i> LIVE</span></div><div>${liveApps.length?liveApps.map(a=>{const c=candidates.find(x=>x.id===a.candidateId),j=jobs.find(x=>x.id===a.jobId);return `<div class="live-event"><i class="live-event-dot"></i><div><b>${escapeHtml(c?.name||'Candidate')}</b> · ${escapeHtml(a.status)} · ${escapeHtml(j?.title||'Job')}</div><small>${escapeHtml(a.updatedAt||a.appliedAt||'')}</small></div>`}).join(''):'<div class="muted">No live application activity yet.</div>'}</div></div><div class="dashboard-grid wide-grid"><div class="dash-card highlight"><span>RECRUITER WORKSPACE</span><h3>Build your hiring pipeline</h3><p>Publish roles, discover talent and manage candidate applications from this page.</p><div class="dash-actions"><button class="btn primary" onclick="openJobModal()">+ Post New Job</button><button class="btn ghost" onclick="showRecruiterTalent()">Discover Talent</button></div></div><div class="dash-card"><span>OPTIMIZATION</span><h3>Hungarian assignment</h3><p>Find the best one-to-one candidate allocation across your open roles.</p><button class="smallbtn apply" onclick="backToHome();setTimeout(runAssignment,80)">Run Assignment →</button></div></div><div class="dash-card full-card"><div class="card-title-row"><div><span>YOUR JOBS</span><h3>Published opportunities</h3></div><button class="btn primary" onclick="openJobModal()">+ Post Job</button></div><div class="dashboard-job-grid">${jobs.map(j=>jobCard(j,true)).join('')}</div></div><div class="dash-card full-card"><div class="card-title-row"><div><span>APPLICATION PIPELINE</span><h3>Open an application to review the candidate</h3></div><span class="live-indicator"><i></i> LIVE</span></div>${renderApplicationRows(applications,'recruiter')}</div>`;
}
function showRecruiterTalent(){ backToHome(); setTimeout(()=>scrollToId('talent'),80); }
function renderAdminDashboard(content) {
  const accepted=applications.filter(a=>a.status==='Accepted').length, rejected=applications.filter(a=>a.status==='Rejected').length;
  content.innerHTML=`<div class="dash-stats"><div><span>CANDIDATES</span><b>${candidates.length}</b><small>profiles</small></div><div><span>JOBS</span><b>${jobs.length}</b><small>opportunities</small></div><div><span>APPLICATIONS</span><b>${applications.length}</b><small>activity</small></div><div><span>DECISIONS</span><b>${accepted+rejected}</b><small>${accepted} accepted · ${rejected} rejected</small></div></div><div class="live-panel"><div class="live-panel-head"><div><span class="eyebrow">ADMIN LIVE MONITOR</span><h3>Platform activity & recruitment decisions</h3></div><span class="live-indicator"><i></i> LIVE MONITORING</span></div><p class="muted">New applications and recruiter decisions are reflected here automatically every few seconds.</p></div><div class="dashboard-grid wide-grid"><div class="dash-card highlight"><span>PLATFORM CONTROL</span><h3>Marketplace overview</h3><p>Monitor candidate growth, job supply, application activity and hiring decisions.</p><div class="dash-actions"><button class="btn primary" onclick="backToHome();setTimeout(()=>scrollToId('analytics'),80)">Open Analytics</button><button class="btn ghost" onclick="backToHome();setTimeout(runAssignment,80)">Run Optimizer</button></div></div><div class="dash-card"><span>TEAM FORMATION</span><h3>Skill coverage</h3><p>Build the smallest team covering your selected technology requirements.</p><button class="smallbtn apply" onclick="backToHome();setTimeout(runTeam,80)">Build Team →</button></div></div><div class="dash-card full-card"><div class="card-title-row"><div><span>LIVE APPLICATION ACTIVITY</span><h3>Platform-wide application tracker</h3></div></div>${renderApplicationRows(applications,'admin')}</div>`;
}
function renderApplicationRows(list, mode) {
  if(!list.length) return `<div class="empty-state compact"><h3>No applications yet</h3><p>Activity will appear here as candidates apply for jobs.</p></div>`;
  return `<div class="application-table"><div class="application-head"><span>Candidate</span><span>Job</span><span>Status</span><span>Action</span></div>${list.slice().reverse().slice(0,15).map(a=>{const c=candidates.find(x=>x.id===a.candidateId),j=jobs.find(x=>x.id===a.jobId);const action=mode==='recruiter'?`<div class="app-actions"><button class="smallbtn apply" onclick="openApplicationDetails(${a.id})">Open</button><button class="smallbtn" onclick="updateApplication(${a.id},'Accepted')">Accept</button><button class="smallbtn danger" onclick="updateApplication(${a.id},'Rejected')">Reject</button></div>`:mode==='admin'?`<div class="app-actions"><button class="smallbtn" onclick="openApplicationDetails(${a.id})">View</button></div>`:`<div class="app-actions"><button class="smallbtn" onclick="openApplicationDetails(${a.id})">Track</button></div>`;return `<div class="application-row"><div class="app-candidate" onclick="openApplicationDetails(${a.id})"><b>${escapeHtml(c?.name||'Unknown')}</b><small>${escapeHtml(c?.title||'')}</small></div><div class="app-job" onclick="openApplicationDetails(${a.id})"><b>${escapeHtml(j?.title||'Unknown job')}</b><small>${escapeHtml(j?.company||'')}</small></div><span class="status-badge ${String(a.status).toLowerCase()}">${escapeHtml(a.status)}</span>${action}</div>`;}).join('')}</div>`;
}
function applicationStatusClass(status){return String(status||'Applied').toLowerCase();}
function openApplicationDetails(id){
  const a=applications.find(x=>x.id===id); if(!a)return;
  const c=candidates.find(x=>x.id===a.candidateId), j=jobs.find(x=>x.id===a.jobId); if(!c||!j)return;
  const steps=['Applied','Shortlisted','Accepted']; const current=a.status==='Rejected'?'Rejected':a.status;
  const timeline=steps.map((s,i)=>`<span class="status-step ${current===s || (steps.indexOf(current)>i)?'active':''}">${s}</span>${i<2?'<span class="status-line"></span>':''}`).join('');
  const decision=current==='Rejected'?'<span class="status-step active">Rejected</span>':'';
  const actions=current==='Accepted'||current==='Rejected'?`<button class="btn ghost" onclick="closeModal()">Close</button>`:`${current!=='Accepted'?`<button class="btn primary" onclick="updateApplication(${a.id},'Accepted');closeModal()">Accept Candidate</button>`:''}<button class="btn ghost" onclick="updateApplication(${a.id},'Rejected');closeModal()">Reject Candidate</button>`;
  const matchBtn=`<button class="smallbtn" onclick="closeModal();showMatch(${c.id},${j.id})">View Fit Score</button>`;
  const canDecide=current!=='Accepted'&&current!=='Rejected'&&currentRole==='recruiter';
  const finalActions=canDecide?actions:`<button class="btn ghost" onclick="closeModal()">Close</button>`;
  $('#modalContent').innerHTML=`<div class="eyebrow">APPLICATION #${a.id}</div><div class="profile-modal-head"><div class="avatar large">${initials(c.name)}</div><div><h2>${escapeHtml(c.name)}</h2><p class="muted">${escapeHtml(c.title)} · ${escapeHtml(c.location)} · ${c.experience} years experience</p></div></div><div class="status-timeline">${timeline}${decision}</div><div class="app-detail-grid"><div class="app-detail-item"><span>APPLIED FOR</span><b>${escapeHtml(j.title)}</b><small>${escapeHtml(j.company)} · ${escapeHtml(j.location)}</small></div><div class="app-detail-item"><span>EMAIL</span><b>${escapeHtml(c.email)}</b></div><div class="app-detail-item"><span>EDUCATION</span><b>${escapeHtml(c.education)}</b></div><div class="app-detail-item"><span>EXPERIENCE</span><b>${c.experience} years · ${c.projects} projects</b></div></div><p class="profile-bio">${escapeHtml(c.bio||'No bio provided.')}</p><div class="card-tags">${tags(c.skills)}</div><p class="muted">Applied: ${escapeHtml(a.appliedAt||'')}<br>Last updated: ${escapeHtml(a.updatedAt||a.appliedAt||'')}</p><div class="app-detail-actions">${matchBtn}${finalActions}</div>`;
  $('#modal').classList.remove('hidden');
}
async function updateApplication(id,status){ try { await api('/api/applications/status',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:formBody({id,status})}); const a=applications.find(x=>x.id===id); if(a){a.status=status;a.updatedAt=new Date().toString();} await refreshLiveApplications(); toast(`Application ${status.toLowerCase()} ✓`); } catch(e){toast(e.message);} }

window.addEventListener("load", async()=>{
  const saved=JSON.parse(localStorage.getItem("talentmatchSession")||"null");
  if(saved?.role){currentRole=saved.role;currentEmail=saved.email;}
  await init();
  if(currentRole && ((currentRole!=="candidate") || getCurrentCandidate())) { updateNavForRole(); openDashboard(currentRole); }
});
