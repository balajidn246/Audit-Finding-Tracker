import { ChangeEvent, FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, login, logout, refreshSession } from './api';
import type { AuditEvent, Evidence, Finding, FindingStatus, Severity, Stats, User } from './types';

const statuses: FindingStatus[] = ['OPEN','IN_PROGRESS','PENDING_VALIDATION','REWORK_REQUIRED','RISK_ACCEPTED','CLOSED'];
const severities: Severity[] = ['CRITICAL','HIGH','MEDIUM','LOW'];
const blankStats: Stats = { total: 0, open: 0, in_progress: 0, pending_validation: 0, rework_required: 0, risk_accepted: 0, closed: 0 };
const pretty = (value: string) => value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
const date = (value?: string) => value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value)) : 'Not set';
const utcTomorrow = () => { const d = new Date(); d.setUTCDate(d.getUTCDate() + 1); return d.toISOString().slice(0, 10); };

export default function App() {
  const [authed, setAuthed] = useState(false), [loading, setLoading] = useState(true), [error, setError] = useState('');
  const [rows, setRows] = useState<Finding[]>([]), [stats, setStats] = useState<Stats>(blankStats), [users, setUsers] = useState<User[]>([]);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [query, setQuery] = useState(''), [searchQuery, setSearchQuery] = useState(''), [filter, setFilter] = useState('ALL'), [selected, setSelected] = useState<Finding | null>(null);
  const [pageNumber, setPageNumber] = useState(0), [totalPages, setTotalPages] = useState(0);
  const [events, setEvents] = useState<AuditEvent[]>([]), [modal, setModal] = useState(false);
  const [evidence, setEvidence] = useState<Evidence[]>([]);
  const [userAdminOpen, setUserAdminOpen] = useState(false);
  const requestSequence = useRef(0);
  useEffect(() => { const timer = window.setTimeout(() => setSearchQuery(query.trim()), 250); return () => window.clearTimeout(timer); }, [query]);
  const load = useCallback(async () => {
    const requestId = ++requestSequence.current;
    try {
      const params = new URLSearchParams({ page: String(pageNumber), size: '20', sortBy: 'updatedAt', dir: 'DESC' });
      if (searchQuery) params.set('q', searchQuery);
      if (filter !== 'ALL') params.set('status', filter);
      const [page, aggregate] = await Promise.all([api.findings(params), api.stats()]);
      if (requestId === requestSequence.current) { setError(''); setRows(page.content); setTotalPages(page.totalPages); setStats(aggregate); }
    } catch (e) { if (requestId === requestSequence.current) setError(e instanceof Error ? e.message : 'Unable to load workspace'); }
  }, [searchQuery, filter, pageNumber]);
  useEffect(() => { setPageNumber(0); }, [query, filter]);
  useEffect(() => { refreshSession().then(ok => { setAuthed(ok); setLoading(false); }); }, []);
  useEffect(() => { if (authed) { load(); api.me().then(setCurrentUser).catch(() => setCurrentUser(null)); api.users().then(setUsers).catch(() => setUsers([])); } }, [authed, load]);
  const openFinding = async (f: Finding) => { setSelected(f); setEvents([]); setEvidence([]); try { const [full, history] = await Promise.all([api.finding(f.uuid), api.history(f.uuid)]); const canAccessFiles = Boolean(currentUser && (currentUser.roles.some(r => ['ROLE_ADMIN','ROLE_MANAGER'].includes(r)) || currentUser.id === full.ownerId)); const files = canAccessFiles ? await api.evidence(f.uuid) : []; setSelected(full); setEvents(history.content); setEvidence(files); } catch (e) { setError(e instanceof Error ? e.message : 'Unable to load finding'); } };
  useEffect(() => {
    if (!authed || !currentUser) return;
    const url = new URL(window.location.href);
    const findingUuid = url.searchParams.get('finding');
    if (!findingUuid) return;
    url.searchParams.delete('finding');
    window.history.replaceState({}, '', `${url.pathname}${url.search}${url.hash}`);
    void Promise.all([api.finding(findingUuid), api.history(findingUuid)])
      .then(async ([finding, history]) => {
        const canAccessFiles = currentUser.roles.some(r => ['ROLE_ADMIN','ROLE_MANAGER'].includes(r)) || currentUser.id === finding.ownerId;
        const files = canAccessFiles ? await api.evidence(findingUuid) : [];
        setSelected(finding); setEvents(history.content); setEvidence(files);
      })
      .catch(e => setError(e instanceof Error ? e.message : 'Unable to open the linked finding'));
  }, [authed, currentUser]);
  if (loading) return <div className="boot"><span className="brand-mark">V</span><p>Opening secure workspace…</p></div>;
  if (!authed) return <Login onSuccess={() => setAuthed(true)} />;
  return <div className="shell">
    <aside className="sidebar"><div className="brand"><span className="brand-mark">V</span><div><b>verity<span>ops</span></b><small>TRUST WORKSPACE</small></div></div>
      <div className="org-switch"><span className="org-icon">V</span><div><b>Organization workspace</b><small>Audit & assurance</small></div><span className="chevron">⌄</span></div>
      <div className="nav-label">WORKSPACE</div><button className="nav active"><span>▦</span> Overview</button><button className="nav" onClick={() => { setFilter('ALL'); setSelected(null); }}><span>▤</span> Findings <i>{stats.total}</i></button><button className="nav" onClick={() => setFilter('PENDING_VALIDATION')}><span>◷</span> Validation queue <i>{stats.pending_validation}</i></button>
      <div className="nav-label space">GOVERNANCE</div>{currentUser?.roles.some(r => ['ROLE_ADMIN','ROLE_MANAGER'].includes(r)) && <button className="nav" onClick={() => api.downloadReport({ q: query, status: filter }).catch(e => setError(e.message))}><span>▥</span> Export register</button>}{currentUser?.roles.includes('ROLE_ADMIN') && <button className="nav" onClick={() => setUserAdminOpen(true)}><span>♙</span> Access management</button>}<div className="sidebar-bottom"><div className="security-note"><span>◆</span><div><b>Protected workspace</b><small>Activity is recorded for review.</small></div></div><button className="profile" onClick={async () => { await logout(); setAuthed(false); }}><span className="avatar">VO</span><div><b>{currentUser?.username || 'Signed in'}</b><small>Sign out securely</small></div><span className="more">···</span></button></div>
    </aside>
    <main className="main"><header className="topbar"><div className="breadcrumb">Workspace <span>/</span> <b>Overview</b></div><div className="top-actions"><span className="env"><i/> PROTECTED SESSION</span><span className="avatar small">VO</span></div></header>
      <div className="content"><div className="page-heading"><div><div className="eyebrow">AUDIT & ASSURANCE</div><h1>Overview</h1><p>Your organization’s findings, remediation progress, and assurance posture.</p></div>{currentUser?.roles.some(r => ['ROLE_ADMIN','ROLE_MANAGER'].includes(r)) && <button className="primary" onClick={() => setModal(true)}><span>＋</span> New finding</button>}</div>
        {error && <div className="alert" role="alert"><span>!</span>{error}<button onClick={() => setError('')}>Dismiss</button></div>}
        <div className="metric-grid"><Metric label="Open findings" value={stats.open ?? 0} hint="Require attention" icon="◉" tone="blue"/><Metric label="In remediation" value={stats.in_progress ?? 0} hint="Owners are working" icon="↗" tone="violet"/><Metric label="Awaiting validation" value={stats.pending_validation ?? 0} hint="Ready for review" icon="◷" tone="amber"/><Metric label="Closed this cycle" value={stats.closed ?? 0} hint="Remediation verified" icon="✓" tone="green"/></div>
        <div className="section-heading"><div><h2>Finding register</h2><p>Prioritize issues and keep remediation moving.</p></div><div className="register-tools"><label className="search"><span>⌕</span><input placeholder="Search findings" value={query} maxLength={200} onChange={e => setQuery(e.target.value)} aria-label="Search findings"/></label><select value={filter} onChange={e => setFilter(e.target.value)} aria-label="Filter by status"><option value="ALL">All statuses</option>{statuses.map(s => <option key={s} value={s}>{pretty(s)}</option>)}</select><button className="secondary" onClick={() => void load()}>↻</button></div></div>
        <div className="table-card"><div className="table-scroll"><table><thead><tr><th>FINDING</th><th>SEVERITY</th><th>STATUS</th><th>OWNER</th><th>DUE DATE</th><th/></tr></thead><tbody>{rows.map(f => <tr key={f.uuid} onClick={() => void openFinding(f)}><td><div className="finding-title">{f.title}</div><div className="finding-meta">{f.sourceAudit || f.businessUnit || 'Audit finding'} <span>·</span> updated {date(f.updatedAt)}</div></td><td><span className={`severity ${f.severity.toLowerCase()}`}><i/>{pretty(f.severity)}</span></td><td><span className={`status ${f.status.toLowerCase()}`}>{pretty(f.status)}</span></td><td><span className="owner"><span className="avatar mini">{(users.find(u => u.id === f.ownerId)?.username || '—').slice(0,2).toUpperCase()}</span>{users.find(u => u.id === f.ownerId)?.username || 'Unassigned'}</span></td><td className={f.dueDate && f.dueDate < new Date().toISOString().slice(0,10) && !['CLOSED','RISK_ACCEPTED'].includes(f.status) ? 'overdue' : ''}>{date(f.dueDate)}</td><td className="row-arrow">›</td></tr>)}{rows.length === 0 && <tr><td colSpan={6} className="empty"><span>▤</span><b>No findings match this view</b><small>Create a finding or change the selected filters.</small><button className="text-button" onClick={() => { setFilter('ALL'); setQuery(''); }}>Clear filters</button></td></tr>}</tbody></table></div><div className="table-footer"><span>Page {totalPages ? pageNumber + 1 : 0} of {totalPages} · <b>{stats.total}</b> total findings</span><div className="pagination"><button className="secondary" disabled={pageNumber === 0} onClick={() => setPageNumber(p => Math.max(0, p - 1))}>← Previous</button><button className="secondary" disabled={totalPages === 0 || pageNumber + 1 >= totalPages} onClick={() => setPageNumber(p => p + 1)}>Next →</button></div></div></div>
        <footer className="footer"><span>VerityOps <span>·</span> Audit, risk & remediation</span><span>Security activity is logged</span></footer>
      </div>
    </main>
    {modal && <FindingModal users={users} onClose={() => setModal(false)} onCreated={async () => { setModal(false); await load(); }} onError={setError} />}
    {selected && <FindingDrawer finding={selected} events={events} evidence={evidence} users={users} currentUser={currentUser} onClose={() => setSelected(null)} onChange={async () => { await load(); await openFinding(selected); }} onError={setError}/>}
    {userAdminOpen && currentUser?.roles.includes('ROLE_ADMIN') && <UserAdminPanel currentUser={currentUser} onClose={() => setUserAdminOpen(false)} onUsersChanged={setUsers} onError={setError}/>}
  </div>;
}

function Login({ onSuccess }: { onSuccess: () => void }) {
  const [username, setUsername] = useState(''), [password, setPassword] = useState(''), [showPassword, setShowPassword] = useState(false), [error, setError] = useState(''), [busy, setBusy] = useState(false);
  const submit = async (e: FormEvent) => { e.preventDefault(); setBusy(true); setError(''); try { await login(username, password); onSuccess(); } catch (e) { setError(e instanceof Error ? e.message : 'Sign in failed'); } finally { setBusy(false); } };
  return <div className="login-page">
    <section className="login-art" aria-label="About VerityOps">
      <AmbientBackground />
      <div className="login-vignette" />
      <div className="brand"><span className="brand-mark">V</span><div><b>verity<span>ops</span></b><small>TRUST WORKSPACE</small></div></div>
      <div className="art-copy">
        <div className="eyebrow"><i className="eyebrow-line"/> ASSURANCE, WITH CLARITY</div>
        <h1>Make every finding<br/>a <em>verified outcome.</em></h1>
        <p>VerityOps gives internal audit and security teams one place to record control gaps, assign accountable owners, track remediation, and preserve proof of what changed.</p>
        <div className="login-feature-grid">
          <div tabIndex={0}><span className="feature-icon">⌁</span><b>Own the action</b><small>Clear owner, due date, and remediation path.</small></div>
          <div tabIndex={0}><span className="feature-icon">◈</span><b>Prove the fix</b><small>Scan evidence and require independent review.</small></div>
          <div tabIndex={0}><span className="feature-icon">⌑</span><b>Trust the history</b><small>Every key decision leaves an audit trail.</small></div>
        </div>
        <div className="flow-strip"><div><small>01</small><b>IDENTIFY</b></div><span/><div><small>02</small><b>REMEDIATE</b></div><span/><div><small>03</small><b>VALIDATE</b></div><span/><div><small>04</small><b>CLOSE</b></div></div>
      </div>
      <div className="login-caption">VERITYOPS <span>·</span> AUDIT, RISK & REMEDIATION <span className="caption-live"><i/> SECURE WORKSPACE</span></div>
    </section>
    <aside className="login-panel"><div className="panel-top"><span className="panel-mark">V</span><span>YOUR ASSURANCE WORKSPACE</span></div>
      <form onSubmit={submit} className="login-form"><div className="eyebrow">WELCOME BACK</div><h2>Sign in to VerityOps</h2><p>Enter your organization credentials to continue.</p>{error && <div className="alert" role="alert">{error}</div>}
        <label>Email or username<input autoComplete="username" required value={username} onChange={e => setUsername(e.target.value)} placeholder="you@company.com"/></label>
        <label>Password<span className="password-field"><input type={showPassword ? 'text' : 'password'} autoComplete="current-password" required value={password} onChange={e => setPassword(e.target.value)} placeholder="Enter your password"/><button type="button" className="password-toggle" onClick={() => setShowPassword(value => !value)} aria-label={showPassword ? 'Hide password' : 'Show password'} aria-pressed={showPassword}>{showPassword ? 'Hide' : 'Show'}</button></span></label>
        <button className="primary full" disabled={busy}>{busy ? 'Securing your session…' : 'Continue securely'} <span>→</span></button>
        <div className="login-foot"><span>⌑</span> Access is provisioned by your workspace administrator.</div>
      </form><div className="login-compliance"><i/> Protected sign-in <span>·</span> Activity logged for assurance</div>
    </aside>
  </div>;
}

function AmbientBackground() {
  const canvas = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    const element = canvas.current;
    if (!element) return;
    const context = element.getContext('2d');
    if (!context) return;
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    let width = 0, height = 0, frame = 0;
    const points = Array.from({ length: 54 }, () => ({ x: Math.random(), y: Math.random(), vx: (Math.random() - .5) * .00014, vy: (Math.random() - .5) * .00012, phase: Math.random() * 6.28 }));
    const resize = () => { const rect = element.getBoundingClientRect(); const scale = Math.min(window.devicePixelRatio || 1, 1.5); width = rect.width; height = rect.height; element.width = width * scale; element.height = height * scale; context.setTransform(scale, 0, 0, scale, 0, 0); };
    const draw = (time: number) => {
      context.clearRect(0, 0, width, height);
      const glow = context.createRadialGradient(width * .33, height * .47, 2, width * .33, height * .47, Math.max(width, height) * .8);
      glow.addColorStop(0, '#263d7620'); glow.addColorStop(.6, '#111d3a0b'); glow.addColorStop(1, '#0b102000');
      context.fillStyle = glow; context.fillRect(0, 0, width, height);
      const positions = points.map(p => ({ x: p.x * width + Math.sin(time * .00014 + p.phase) * 12, y: p.y * height + Math.cos(time * .00011 + p.phase) * 10 }));
      for (let i = 0; i < positions.length; i++) for (let j = i + 1; j < positions.length; j++) {
        const a = positions[i], b = positions[j], dx = a.x - b.x, dy = a.y - b.y, distance = Math.hypot(dx, dy);
        if (distance < 145) { context.strokeStyle = `rgba(129,157,255,${(1 - distance / 145) * .16})`; context.lineWidth = .65; context.beginPath(); context.moveTo(a.x, a.y); context.lineTo(b.x, b.y); context.stroke(); }
      }
      positions.forEach((p, i) => { const pulse = .65 + Math.sin(time * .001 + points[i].phase) * .25; context.fillStyle = `rgba(164,185,255,${pulse * .48})`; context.beginPath(); context.arc(p.x, p.y, i % 7 === 0 ? 1.7 : 1, 0, Math.PI * 2); context.fill(); });
      if (!reduced && !document.hidden) frame = requestAnimationFrame(draw);
    };
    resize(); const onVisibility = () => { cancelAnimationFrame(frame); if (!document.hidden && !reduced) frame = requestAnimationFrame(draw); };
    window.addEventListener('resize', resize); document.addEventListener('visibilitychange', onVisibility);
    frame = requestAnimationFrame(draw);
    return () => { cancelAnimationFrame(frame); window.removeEventListener('resize', resize); document.removeEventListener('visibilitychange', onVisibility); };
  }, []);
  return <canvas ref={canvas} className="ambient-canvas" aria-hidden="true"/>;
}
function Metric({ label, value, hint, icon, tone }: { label: string; value: number; hint: string; icon: string; tone: string }) { return <div className="metric"><div className={`metric-icon ${tone}`}>{icon}</div><div className="metric-label">{label}</div><div className="metric-value">{value.toLocaleString()}</div><div className="metric-hint">{hint}</div></div>; }

function FindingModal({ users, onClose, onCreated, onError }: { users: User[]; onClose: () => void; onCreated: () => Promise<void>; onError: (s: string) => void }) {
  const [busy, setBusy] = useState(false);
  const submit = async (e: FormEvent<HTMLFormElement>) => { e.preventDefault(); setBusy(true); const data = Object.fromEntries(new FormData(e.currentTarget)); const payload = { ...data, ownerId: data.ownerId ? Number(data.ownerId) : null, dueDate: data.dueDate || null }; try { await api.create(payload); await onCreated(); } catch (e) { onError(e instanceof Error ? e.message : 'Unable to create finding'); } finally { setBusy(false); } };
  return <div className="overlay" onMouseDown={e => { if (e.target === e.currentTarget) onClose(); }}><form className="modal" onSubmit={submit}><div className="modal-head"><div><div className="eyebrow">REGISTER A RISK</div><h2>New finding</h2><p>Capture the issue, accountable owner, and next action.</p></div><button type="button" className="close" onClick={onClose}>×</button></div><div className="form-grid"><label className="wide">Finding title<input name="title" maxLength={1000} required placeholder="e.g. Privileged access review not completed"/></label><label className="wide">Description<textarea name="description" rows={4} maxLength={12000} required placeholder="Describe the condition and its business impact…"/></label><label>Severity<select name="severity" required defaultValue="MEDIUM">{severities.map(s => <option key={s}>{s}</option>)}</select></label><label>Remediation owner<select name="ownerId" defaultValue=""><option value="">Unassigned</option>{users.filter(u => u.enabled).map(u => <option key={u.id} value={u.id}>{u.username} · {u.email}</option>)}</select></label><label>Source audit<input name="sourceAudit" maxLength={200} placeholder="Internal audit · Q3 2026"/></label><label>Business unit<input name="businessUnit" maxLength={160} placeholder="Finance, Engineering…"/></label><label>Target due date<input name="dueDate" type="date"/></label><label className="wide">Recommended action<textarea name="recommendation" rows={2} maxLength={12000} placeholder="Suggested remediation steps"/></label></div><div className="modal-actions"><button type="button" className="secondary" onClick={onClose}>Cancel</button><button className="primary" disabled={busy}>{busy ? 'Creating…' : 'Create finding'} <span>→</span></button></div></form></div>;
}

function UserAdminPanel({ currentUser, onClose, onUsersChanged, onError }: { currentUser: User; onClose: () => void; onUsersChanged: (users: User[]) => void; onError: (message: string) => void }) {
  const [users, setUsers] = useState<User[]>([]), [busy, setBusy] = useState(false), [createOpen, setCreateOpen] = useState(false), [localError, setLocalError] = useState('');
  const [adminEvents, setAdminEvents] = useState<AuditEvent[]>([]);
  const loadUsers = useCallback(async () => { try { const [accounts, events] = await Promise.all([api.adminUsers(), api.adminAuditEvents()]); setUsers(accounts); setAdminEvents(events.content); } catch (e) { setLocalError(e instanceof Error ? e.message : 'Unable to load access and audit activity'); } }, []);
  useEffect(() => { void loadUsers(); }, [loadUsers]);
  const toggle = async (user: User) => {
    if (user.id === currentUser.id) return;
    if (user.enabled && !window.confirm(`Deactivate ${user.username}? They will lose access immediately.`)) return;
    setBusy(true); setLocalError('');
    try { await api.setUserEnabled(user.id, !user.enabled); await loadUsers(); try { onUsersChanged(await api.users()); } catch { /* manager-only directory */ } }
    catch (e) { setLocalError(e instanceof Error ? e.message : 'Unable to update account'); }
    finally { setBusy(false); }
  };
  const created = async () => { setCreateOpen(false); await loadUsers(); try { onUsersChanged(await api.users()); } catch { /* assignee directory may be unavailable for legacy roles */ } };
  return <div className="overlay admin-overlay" onMouseDown={e => { if (e.target === e.currentTarget) onClose(); }}><section className="admin-users-panel">
    <header className="admin-head"><div><div className="eyebrow">ORGANIZATION SETTINGS</div><h2>Access management</h2><p>Provision accounts and suspend access. Role and status changes are written to the audit history.</p></div><button className="close" onClick={onClose}>×</button></header>
    {localError && <div className="alert" role="alert">{localError}</div>}
    <div className="admin-toolbar"><span><b>{users.length}</b> accounts</span><button className="primary" onClick={() => setCreateOpen(true)}>＋ Add user</button></div>
    <div className="table-card"><div className="table-scroll"><table><thead><tr><th>ACCOUNT</th><th>ROLE</th><th>STATUS</th><th>JOINED</th><th/></tr></thead><tbody>{users.map(user => <tr key={user.id}><td><div className="finding-title">{user.username}</div><div className="finding-meta">{user.email}</div></td><td>{user.roles.map(r => pretty(r.replace(/^ROLE_/, ''))).join(', ')}</td><td><span className={`user-status ${user.enabled ? 'enabled' : 'disabled'}`}>{user.enabled ? 'Active' : 'Inactive'}</span></td><td>{date(user.createdAt)}</td><td>{user.id === currentUser.id ? <span className="help">You</span> : <button className="user-action" disabled={busy} onClick={() => void toggle(user)}>{user.enabled ? 'Deactivate' : 'Reactivate'}</button>}</td></tr>)}{users.length === 0 && <tr><td colSpan={5} className="empty">No accounts found.</td></tr>}</tbody></table></div></div>
    <div className="admin-history-head"><div><h3>Administrative activity</h3><p>Recent account and notification-management events.</p></div><span>{adminEvents.length} events</span></div>
    <div className="admin-events">{adminEvents.map(event => <div className="admin-event" key={event.eventId}><span className="event-dot"/><div><b>{pretty(event.action)}</b><small>Administrator #{event.actorId ?? 'system'} · {new Date(event.createdAt).toLocaleString()}</small></div></div>)}{adminEvents.length === 0 && <p className="help">No administrative events recorded.</p>}</div>
    <footer className="admin-foot"><span>Never share passwords or grant more access than necessary.</span><button className="secondary" onClick={onClose}>Done</button></footer>
    {createOpen && <UserCreateModal onClose={() => setCreateOpen(false)} onCreated={() => void created()} onError={message => { setLocalError(message); onError(message); }}/>}
  </section></div>;
}

function UserCreateModal({ onClose, onCreated, onError }: { onClose: () => void; onCreated: () => void; onError: (message: string) => void }) {
  const [busy, setBusy] = useState(false);
  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault(); setBusy(true);
    const data = Object.fromEntries(new FormData(e.currentTarget));
    try { await api.createUser({ username: data.username, email: data.email, password: data.password, roles: [data.role] }); onCreated(); }
    catch (error) { onError(error instanceof Error ? error.message : 'Unable to create account'); }
    finally { setBusy(false); }
  };
  return <div className="overlay user-create-overlay"><form className="modal user-create-modal" onSubmit={submit}><div className="modal-head"><div><div className="eyebrow">NEW TEAM MEMBER</div><h2>Create account</h2><p>Use a unique, temporary password and share it securely.</p></div><button type="button" className="close" onClick={onClose}>×</button></div>
    <div className="form-grid"><label className="wide">Username<input name="username" autoComplete="off" minLength={3} maxLength={100} required/></label><label className="wide">Work email<input name="email" type="email" maxLength={200} required/></label><label className="wide">Temporary password<input name="password" type="password" minLength={12} maxLength={72} autoComplete="new-password" required/><small className="field-help">At least 12 characters. Use a unique password and ask the user to change it through your identity process.</small></label><label className="wide">Role<select name="role" defaultValue="VIEWER"><option value="VIEWER">Viewer — remediation owner / read access</option><option value="MANAGER">Manager — create, assign, and validate findings</option><option value="ADMIN">Administrator — manage access and workspace</option></select></label></div>
    <div className="modal-actions"><button type="button" className="secondary" onClick={onClose}>Cancel</button><button className="primary" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</button></div></form></div>;
}

function FindingDrawer({ finding, events, evidence, users, currentUser, onClose, onChange, onError }: { finding: Finding; events: AuditEvent[]; evidence: Evidence[]; users: User[]; currentUser: User | null; onClose: () => void; onChange: () => Promise<void>; onError: (s: string) => void }) {
  const [busy, setBusy] = useState(false), [nextStatus, setNextStatus] = useState(''), [reason, setReason] = useState(''), [riskExpiry, setRiskExpiry] = useState('');
  const [managementResponse, setManagementResponse] = useState(finding.managementResponse || '');
  useEffect(() => { setManagementResponse(finding.managementResponse || ''); }, [finding.uuid, finding.managementResponse]);
  const transitions = useMemo(() => { const privileged = currentUser?.roles.some(r => ['ROLE_ADMIN','ROLE_MANAGER'].includes(r)); const owner = currentUser?.id === finding.ownerId; if (privileged) return finding.status === 'OPEN' ? ['IN_PROGRESS','RISK_ACCEPTED'] : finding.status === 'IN_PROGRESS' ? ['PENDING_VALIDATION','RISK_ACCEPTED'] : finding.status === 'PENDING_VALIDATION' ? ['CLOSED','REWORK_REQUIRED','RISK_ACCEPTED'] : finding.status === 'REWORK_REQUIRED' ? ['IN_PROGRESS','RISK_ACCEPTED'] : ['OPEN']; if (!owner) return []; if (finding.status === 'OPEN') return ['IN_PROGRESS']; if (finding.status === 'IN_PROGRESS' || finding.status === 'REWORK_REQUIRED') return ['PENDING_VALIDATION']; return []; }, [finding.status, finding.ownerId, currentUser]);
  const isManager = currentUser?.roles.some(r => ['ROLE_ADMIN','ROLE_MANAGER'].includes(r)) ?? false;
  const canAccessEvidence = isManager || currentUser?.id === finding.ownerId;
  const canEditRemediation = isManager || currentUser?.id === finding.ownerId;
  const changeStatus = async () => { if (!nextStatus) return; setBusy(true); try { await api.status(finding.uuid, nextStatus, reason, riskExpiry || undefined); setNextStatus(''); setReason(''); setRiskExpiry(''); await onChange(); } catch (e) { onError(e instanceof Error ? e.message : 'Status update failed'); } finally { setBusy(false); } };
  const saveRemediation = async () => { setBusy(true); try { await api.update(finding.uuid, { managementResponse }); await onChange(); } catch (e) { onError(e instanceof Error ? e.message : 'Remediation update failed'); } finally { setBusy(false); } };
  const upload = async (e: ChangeEvent<HTMLInputElement>) => { const input = e.currentTarget; const file = input.files?.[0]; if (!file) return; setBusy(true); try { await api.uploadEvidence(finding.uuid, file); await onChange(); } catch (err) { onError(err instanceof Error ? err.message : 'Evidence upload failed'); } finally { setBusy(false); input.value = ''; } };
  return <div className="overlay drawer-overlay" onMouseDown={e => { if (e.target === e.currentTarget) onClose(); }}><section className="drawer"><header className="drawer-head"><div><div className="eyebrow">FINDING RECORD</div><small className="record-id">{finding.uuid}</small></div><button className="close" onClick={onClose}>×</button></header><div className="drawer-body"><div className="detail-badges"><span className={`severity ${finding.severity.toLowerCase()}`}><i/>{pretty(finding.severity)}</span><span className={`status ${finding.status.toLowerCase()}`}>{pretty(finding.status)}</span></div><h2>{finding.title}</h2><p className="description">{finding.description}</p><div className="detail-grid"><Detail label="Remediation owner" value={users.find(u => u.id === finding.ownerId)?.username || 'Unassigned'}/><Detail label="Due date" value={date(finding.dueDate)}/><Detail label="Source audit" value={finding.sourceAudit || '—'}/><Detail label="Business unit" value={finding.businessUnit || '—'}/></div>{finding.rootCause && <div className="detail-block"><h3>Root cause</h3><p>{finding.rootCause}</p></div>}{finding.recommendation && <div className="detail-block"><h3>Recommended action</h3><p>{finding.recommendation}</p></div>}
    <div className="detail-block"><h3>Remediation update</h3><p className="help">Record progress, blockers, or the work completed. This update becomes part of the finding history.</p>{canEditRemediation ? <><label className="reason-field"><span className="sr-only">Remediation update</span><textarea rows={3} maxLength={12000} value={managementResponse} onChange={e => setManagementResponse(e.target.value)} disabled={busy} placeholder="Describe remediation progress or remaining work…"/></label><button className="secondary" disabled={busy || managementResponse === (finding.managementResponse || "")} onClick={() => void saveRemediation()}>{busy ? "Saving…" : "Save remediation update"}</button></> : <p className="description">{finding.managementResponse || "No remediation update has been provided."}</p>}</div>
    <div className="detail-block"><div className="block-title"><h3>Evidence</h3><span>{canAccessEvidence ? `${evidence.length} verified files` : "Restricted access"}</span></div><p className="help">PDF, PNG, or JPEG up to 10 MB. Files are scanned before private storage.</p>{canAccessEvidence ? <label className="upload-button">＋ Add evidence<input type="file" accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg" onChange={upload} disabled={busy}/></label> : <p className="help">Evidence is available to the assigned owner and audit management.</p>}<div className="evidence-list">{evidence.map(file => <div className="evidence-row" key={file.id}><span className="file-icon">▧</span><div><b>{file.originalName}</b><small>{(file.sizeBytes / 1024).toFixed(0)} KB · SHA-256 {file.sha256.slice(0,12)}…</small></div>{canAccessEvidence && <button className="text-button" onClick={() => api.downloadEvidence(finding.uuid, file.id, file.originalName).catch(err => onError(err.message))}>Download</button>}</div>)}{canAccessEvidence && evidence.length === 0 && <div className="help">No evidence uploaded yet. A clean evidence file is required for closure.</div>}</div></div>
    <div className="detail-block"><div className="block-title"><h3>Remediation workflow</h3><span>Version {finding.version}</span></div><div className="workflow-actions"><select aria-label="Next status" value={nextStatus} onChange={e => setNextStatus(e.target.value)}><option value="">Move to…</option>{transitions.map(s => <option key={s} value={s}>{pretty(s)}</option>)}</select><button className="primary" disabled={!nextStatus || busy || (nextStatus === 'RISK_ACCEPTED' && (!reason.trim() || !riskExpiry))} onClick={() => void changeStatus()}>{busy ? 'Saving…' : 'Update status'}</button></div>{nextStatus === 'RISK_ACCEPTED' && <><label className="reason-field">Risk acceptance reason<textarea value={reason} onChange={e => setReason(e.target.value)} required rows={2}/></label><label className="reason-field">Acceptance expires (UTC)<input type="date" min={utcTomorrow()} value={riskExpiry} onChange={e => setRiskExpiry(e.target.value)}/></label></>}{finding.status === 'RISK_ACCEPTED' && <p className="help">Accepted through {date(finding.riskAcceptanceExpiresOn)} (UTC). It will automatically reopen after expiry.</p>}<p className="help">Closure requires malware-scanned evidence and approval by someone other than the finding owner.</p></div>
    <div className="detail-block"><div className="block-title"><h3>Activity history</h3><span>{events.length} events</span></div><div className="timeline">{events.map(ev => <div className="event" key={ev.eventId}><span className="event-dot"/><div><b>{pretty(ev.action)}</b><small>{new Date(ev.createdAt).toLocaleString()} · Actor #{ev.actorId ?? 'system'}</small></div></div>)}{events.length === 0 && <p className="help">No activity has been recorded.</p>}</div></div></div><footer className="drawer-foot"><span>Immutable activity history enabled</span><button className="secondary" onClick={onClose}>Done</button></footer></section></div>;
}
function Detail({label,value}:{label:string;value:string}) { return <div className="detail-item"><small>{label}</small><b>{value}</b></div>; }
