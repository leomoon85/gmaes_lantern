/* Fictional newspaper reports sourced from the running simulation, never live news. */
let pressActive = null;
let pressReturnFocus = null;
let pressLastClosed = -Infinity;
const pressInertState=new Map();
function pressInert(enabled) {
  if(enabled) {
    for(const el of document.body.children) if(el.id!=='pressOverlay' && el.tagName!=='SCRIPT') { if(!pressInertState.has(el))pressInertState.set(el,el.inert);el.inert=true; }
  } else { for(const [el,value] of pressInertState) el.inert=value;pressInertState.clear(); }
}
function resetPress() {
  pressInert(false);
  pressActive=null;pressReturnFocus=null;pressLastClosed=-Infinity;
  document.getElementById('pressOverlay').hidden=true;
}
function bulletinOpen() { return pressActive !== null; }
function pressData() {
  if (!state.press || !Array.isArray(state.press.stories)) state.press={stories:[],queue:[],seen:[]};
  return state.press;
}
function publishBulletin(story) {
  const data=pressData();
  if(data.seen.includes(story.id)) return;
  data.seen.push(story.id);
  if(data.seen.length>160) { const old=data.seen.findIndex(id=>id.startsWith('market-'));if(old>=0)data.seen.splice(old,1); }
  const entry={...story,year:story.year||year(),time:state.elapsed,read:!!story.archiveOnly};
  data.stories.unshift(entry); data.stories=data.stories.filter((s,i)=>s.url||i<40).slice(0,80);
  if(!story.archiveOnly) data.queue.push(entry.id);
  data.queue=data.queue.slice(-8);
  saveState();
}
function pressText(id,text) { document.getElementById(id).textContent=text; }
function openBulletin(story) {
  if(bulletinOpen()) return;
  pressReturnFocus=document.activeElement;
  pressActive=story;
  story.read=true;
  const data=pressData(); data.queue=data.queue.filter(id=>id!==story.id);
  pressText('pressEdition',currentScenario().city+' · '+(story.date||story.year)+' · '+story.category);
  pressText('pressHeadline',story.title);
  pressText('pressCopy',story.body);
  pressText('pressImpact',story.impact);
  const source=document.getElementById('pressSource');
  source.hidden=!story.url;source.textContent=story.source||'';
  if(story.url) source.href=story.url;else source.removeAttribute('href');
  pressText('pressNote',story.url?'Historical record · Event date '+story.date+(story.published?' · Report published '+story.published:'')+'. Gameplay interpretation appears in the shaded box.':'Company simulation · This report describes your fictional campaign, not a real-world company or stock quotation.');
  document.getElementById('pressArticle').hidden=false;
  document.getElementById('pressArchive').hidden=true;
  const action=document.getElementById('pressAction');
  action.hidden=story.tab===undefined;
  action.textContent=story.action||'Review company';
  document.getElementById('pressOverlay').hidden=false;
  pressInert(true);
  document.getElementById('pressClose').focus();
  saveState();
}
function closeBulletin(tab) {
  document.getElementById('pressOverlay').hidden=true;
  pressActive=null; pressLastClosed=state.elapsed;
  pressInert(false);
  if(tab!==undefined) selectTab(tab);
  else if(pressReturnFocus && pressReturnFocus.isConnected) pressReturnFocus.focus();
  saveState();
}
function openPressArchive() {
  if(bulletinOpen()) return;
  pressReturnFocus=document.activeElement;
  pressActive={archive:true};
  pressText('pressEdition',currentScenario().city+' · Company archive');
  const list=document.getElementById('pressArchive'); list.replaceChildren();
  const stories=pressData().stories;
  if(!stories.length) { const p=document.createElement('p');p.textContent='Your first edition arrives shortly. City developments and market reports will be collected here.';list.append(p); }
  for(const story of stories) {
    const button=document.createElement('button');button.className='press-story';
    const meta=document.createElement('small');meta.textContent=story.year+' · '+story.category+(story.read?'':' · NEW');
    button.append(meta,document.createTextNode(story.title));
    button.onclick=()=>{const origin=pressReturnFocus;pressActive=null;openBulletin(story);pressReturnFocus=origin;}; list.append(button);
  }
  list.hidden=false;document.getElementById('pressArticle').hidden=true;
  document.getElementById('pressAction').hidden=true;
  document.getElementById('pressOverlay').hidden=false;
  pressInert(true);
  document.getElementById('pressClose').focus();
}
function updatePress() {
  const data=pressData();
  updateHistoricalPress();
  if(state.elapsed>=8 && !data.seen.includes('opening')) publishBulletin({id:'opening',category:'City desk',title:'A new name on the city streets',body:'Lantern has entered '+currentScenario().city+' with a small fleet and a large ambition. Established operators are already competing for the same passengers.',impact:'Your vehicles are serving real requests in this simulation. Expand capacity, keep vehicles in good condition, and watch rival share prices as your company grows.',tab:1,action:'Inspect your fleet'});
  const unread=data.stories.filter(s=>!s.read).length;
  pressText('pressButton','▤ News'+(unread?' · '+unread:''));
  if(bulletinOpen() || activeTab!==0) return;
  const story=data.queue.map(id=>data.stories.find(s=>s.id===id)).find(s=>s && (s.urgent || state.elapsed-pressLastClosed>=20));
  if(story) openBulletin(story);
}
document.addEventListener('keydown',event=>{
  if(!bulletinOpen()) return;
  if(event.key==='Escape') { event.preventDefault();closeBulletin(); }
  if(event.key==='Tab') {
    const buttons=[...document.getElementById('pressOverlay').querySelectorAll('button,a[href]')].filter(b=>!b.hidden && b.getClientRects().length);
    const first=buttons[0],last=buttons[buttons.length-1];
    if(event.shiftKey && document.activeElement===first) { event.preventDefault();last.focus(); }
    else if(!event.shiftKey && document.activeElement===last) { event.preventDefault();first.focus(); }
  }
});
