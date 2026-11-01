const API = 'http://localhost:8080/api';
const $ = (id) => document.getElementById(id);
const fmt = (n) => Number(n || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 });
const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let currentSources = [];
let generationTouched = false;
function totalRatedCapacity() { return currentSources.reduce((sum, source) => sum + Number(source.capacityKwh || 0), 0); }
function validateGeneration(showMessage = true) {
  const generation = Number($('generationKwh').value);
  const total = totalRatedCapacity();
  const valid = Number.isFinite(generation) && Math.abs(generation - total) < 0.05;
  const message = $('formMessage');
  if (showMessage && !valid) {
    message.textContent = `Available generation must equal total rated capacity (${fmt(total)} kWh).`;
    message.className = 'form-message error';
  } else if (valid && message.classList.contains('error') && message.textContent.includes('Available generation must equal')) {
    message.textContent = ''; message.className = 'form-message';
  }
  $('generationKwh').setCustomValidity(valid ? '' : `Must equal total rated capacity (${fmt(total)} kWh).`);
  return valid;
}

async function api(path, options = {}) {
  const response = await fetch(API + path, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error || `Request failed (${response.status})`);
  return data;
}
function fillIntervals(used = []) {
  const select = $('intervalNumber');
  const current = Number(select.value) || 1;
  select.innerHTML = Array.from({length:24}, (_,i) => {
    const n = i + 1, disabled = used.includes(n);
    return `<option value="${n}" ${disabled?'disabled':''} ${n===current&&!disabled?'selected':''}>Hour ${String(n).padStart(2,'0')}${disabled?' · completed':''}</option>`;
  }).join('');
  if (select.selectedOptions.length && select.selectedOptions[0].disabled) {
    const next = Array.from(select.options).find(o => !o.disabled);
    if (next) select.value = next.value;
  }
}
function renderSources(sources) {
  currentSources = sources || [];
  const icons = {Solar:'☀',Wind:'〰'};
  $('sourceCount').textContent = `${sources.length} SOURCES`;
  $('sourceList').innerHTML = sources.map(s => `<div class="source-item">
    <div class="source-icon">${icons[s.type] || '⚡'}</div>
    <div><div class="source-name">${escapeHtml(s.name)}</div></div>
    <div class="source-capacity">${fmt(s.capacityKwh)} <small>kWh</small><div class="row-actions"><button type="button" class="text-btn" data-source-edit="${s.id}">Edit</button><button type="button" class="text-btn danger" data-source-delete="${s.id}">Delete</button></div></div>
  </div>`).join('') || '<div class="loading">No sources configured.</div>';
  const total = totalRatedCapacity();
  $('sourceCapacity').textContent = `${fmt(total)} kWh`;
  if (!generationTouched) $('generationKwh').value = total.toFixed(1);
  validateGeneration(false);
}
function renderLoads(loads) {
  const rank = {Critical:1,Essential:2,Deferrable:3};
  $('loadTable').innerHTML = loads.map(l => `<tr>
    <td>${escapeHtml(l.name)}</td><td><span class="pill ${String(l.category).toLowerCase()}">${escapeHtml(l.category)}</span></td>
    <td>${fmt(l.demandKwh)} kWh</td><td>${fmt(l.minimumServiceKwh)} kWh</td>
    <td>${l.divisible ? 'Partial allowed' : 'Minimum / full only'}</td><td><span class="priority">${l.priority || rank[l.category] || '—'}</span></td>
    <td class="row-actions"><button type="button" class="text-btn" data-load-edit="${l.id}">Edit</button><button type="button" class="text-btn danger" data-load-delete="${l.id}">Delete</button></td>
  </tr>`).join('') || '<tr><td colspan="7">No consumer loads configured.</td></tr>';
}
function renderHistory(history) {
  $('historyCount').textContent = `${history.length} records`;
  $('historyTable').innerHTML = history.length ? history.slice().reverse().map(r => `<tr>
    <td>Hour ${String(r.intervalNumber).padStart(2,'0')}</td><td>${escapeHtml(r.loadName)}</td>
    <td><span class="pill ${String(r.category).toLowerCase()}">${escapeHtml(r.category)}</span></td>
    <td>${fmt(r.requestedKwh)} kWh</td><td><b>${fmt(r.allocatedKwh)} kWh</b></td>
    <td>${fmt(r.unmetKwh)} kWh</td><td>${fmt(r.curtailedKwh)} kWh</td>
  </tr>`).join('') : '<tr><td colspan="7">No allocations yet. Run an interval to see results.</td></tr>';
  const used = [...new Set(history.map(r=>Number(r.intervalNumber)))];
  fillIntervals(used);
}
async function refresh() {
  try {
    const data = await api('/dashboard');
    $('generationMetric').textContent = fmt(data.lastGenerationKwh);
    $('allocatedMetric').textContent = fmt(data.lastAllocatedKwh);
    $('unmetMetric').textContent = fmt(data.lastUnmetKwh);
    $('intervalMetric').textContent = fmt(data.intervalCount);
    renderSources(data.sources || []);
    renderLoads(data.loads || []);
    renderHistory(data.history || []);
  } catch (err) {
    $('formMessage').textContent = `Cannot connect to backend: ${err.message}. Start the Java server and check MySQL settings.`;
    $('formMessage').className = 'form-message error';
  }
}
$('allocationForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const message = $('formMessage');
  if (!validateGeneration(true)) return;
  message.className = 'form-message';
  message.textContent = 'Calculating priority allocation…';
  try {
    const result = await api('/allocations/run', {
      method:'POST',
      body:JSON.stringify({intervalNumber:Number($('intervalNumber').value),generationKwh:Number($('generationKwh').value)})
    });
    message.textContent = `Hour ${result.intervalNumber} completed: ${fmt(result.allocatedKwh)} kWh allocated, ${fmt(result.unmetKwh)} kWh unmet, ${fmt(result.curtailedKwh)} kWh curtailed.`;
    await refresh();
  } catch (err) {
    message.textContent = err.message;
    message.className = 'form-message error';
  }
});
$('generationKwh').addEventListener('input', () => { generationTouched = true; validateGeneration(true); });
$('refreshBtn').addEventListener('click', refresh);
$('clearBtn').addEventListener('click', async () => {
  if (!confirm('Clear all allocation history? This cannot be undone.')) return;
  try { await api('/allocations', {method:'DELETE'}); await refresh(); }
  catch (err) { alert(err.message); }
});
refresh();


// Renewable source management
function resetSourceForm() {
  $('sourceForm').reset(); $('sourceId').value = ''; $('sourceFormTitle').textContent = '+ Add renewable source';
  $('sourceSaveBtn').textContent = 'Save source'; $('sourceMessage').textContent = ''; $('sourceFormWrap').open = false;
}
$('sourceForm').addEventListener('submit', async (event) => {
  event.preventDefault(); const id = $('sourceId').value;
  const body = {name:$('sourceName').value.trim(), capacityKwh:Number($('sourceCapacityInput').value)};
  try {
    await api(id ? `/sources/${id}` : '/sources', {method:id?'PUT':'POST', body:JSON.stringify(body)});
    resetSourceForm(); await refresh();
  } catch (err) { $('sourceMessage').textContent = err.message; $('sourceMessage').className = 'form-message error'; }
});
$('sourceCancelBtn').addEventListener('click', resetSourceForm);
$('sourceList').addEventListener('click', async (event) => {
  const edit = event.target.closest('[data-source-edit]'); const del = event.target.closest('[data-source-delete]');
  if (edit) {
    try { const source = (await api('/sources')).find(s => String(s.id) === edit.dataset.sourceEdit); if (!source) return;
      $('sourceId').value=source.id; $('sourceName').value=source.name; $('sourceCapacityInput').value=source.capacityKwh;
      $('sourceFormTitle').textContent='Edit renewable source'; $('sourceSaveBtn').textContent='Update source'; $('sourceFormWrap').open=true; $('sourceFormWrap').scrollIntoView({behavior:'smooth',block:'nearest'});
    } catch(err) { alert(err.message); }
  }
  if (del && confirm('Delete this renewable source?')) {
    try { await api(`/sources/${del.dataset.sourceDelete}`, {method:'DELETE'}); await refresh(); }
    catch(err) { alert(err.message); }
  }
});

// Consumer load management
function resetLoadForm() {
  $('loadForm').reset(); $('loadId').value=''; $('loadMinimum').value='0'; $('loadPriority').value='2'; $('loadDivisible').checked=true;
  $('loadFormTitle').textContent='+ Add consumer load'; $('loadSaveBtn').textContent='Save consumer'; $('loadMessage').textContent=''; $('loadFormWrap').open=false;
}
$('loadForm').addEventListener('submit', async (event) => {
  event.preventDefault(); const id=$('loadId').value; const demand=Number($('loadDemand').value); const minimum=Number($('loadMinimum').value);
  if (minimum > demand) { $('loadMessage').textContent='Minimum service cannot exceed required energy.'; $('loadMessage').className='form-message error'; return; }
  const body={name:$('loadName').value.trim(),category:$('loadCategory').value,demandKwh:demand,minimumServiceKwh:minimum,divisible:$('loadDivisible').checked,priority:Number($('loadPriority').value)};
  try { await api(id?`/loads/${id}`:'/loads',{method:id?'PUT':'POST',body:JSON.stringify(body)}); resetLoadForm(); await refresh(); }
  catch(err) { $('loadMessage').textContent=err.message; $('loadMessage').className='form-message error'; }
});
$('loadCancelBtn').addEventListener('click', resetLoadForm);
$('loadTable').addEventListener('click', async (event) => {
  const edit=event.target.closest('[data-load-edit]'); const del=event.target.closest('[data-load-delete]');
  if (edit) {
    try { const load=(await api('/loads')).find(l=>String(l.id)===edit.dataset.loadEdit); if(!load) return;
      $('loadId').value=load.id; $('loadName').value=load.name; $('loadCategory').value=load.category || 'Essential'; $('loadDemand').value=load.demandKwh;
      $('loadMinimum').value=load.minimumServiceKwh; $('loadDivisible').checked=!!load.divisible; $('loadPriority').value=load.priority || 2;
      $('loadFormTitle').textContent='Edit consumer load'; $('loadSaveBtn').textContent='Update consumer'; $('loadFormWrap').open=true; $('loadFormWrap').scrollIntoView({behavior:'smooth',block:'nearest'});
    } catch(err) { alert(err.message); }
  }
  if (del && confirm('Delete this consumer load? It will no longer participate in future allocations. Existing history remains.')) {
    try { await api(`/loads/${del.dataset.loadDelete}`,{method:'DELETE'}); await refresh(); } catch(err) { alert(err.message); }
  }
});
