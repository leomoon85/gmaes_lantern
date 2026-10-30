// Each cache entry can fail, time out, retry, or switch providers independently.
// Request callbacks are generation checked so evicted/late requests cannot revive tiles.
let visibleMapTiles=[];
function mapStorage(key) { try{return localStorage.getItem(key);}catch{return null;} }
function initialiseMapStyle() {
  const saved=mapStorage('lantern_map_style');
  const index=saved===null?1:Number(saved);
  return Number.isInteger(index)&&index>=0&&index<4?index:1;
}
function persistMapStyle() { try{localStorage.setItem('lantern_map_style',String(mapStyleIdx));}catch{} }
function loadRecoverableTile(styleIdx,z,x,y) {
  const size=2**z,wrappedX=((x%size)+size)%size;
  if(y<0||y>=size)return null;
  const key=styleIdx+':'+z+':'+wrappedX+':'+y;
  let tile=tileCache.get(key);
  if(tile) {
    if(tile.status==='failed'&&Date.now()>=tile.retryAt) startTileRequest(tile,styleIdx,z,wrappedX,y);
    return tile;
  }
  tile=new Image();tile.loaded=false;tile.status='loading';tile.generation=0;tile.failures=0;
  tileCache.set(key,tile);
  startTileRequest(tile,styleIdx,z,wrappedX,y);
  if(tileCache.size>260) {
    const oldest=tileCache.keys().next().value,entry=tileCache.get(oldest);
    clearTimeout(entry.deadline);entry.generation++;entry.onload=null;entry.onerror=null;
    tileCache.delete(oldest);
  }
  return tile;
}
function startTileRequest(tile,styleIdx,z,x,y) {
  tile.loaded=false;tile.status='loading';tile.startedAt=Date.now();
  const primary=MAP_STYLES[styleIdx];
  const providers=[{name:primary.name,url:primary.url(z,x,y),filter:primary.filter,credit:styleIdx===0?'carto':'google'},
    {name:'Backup street map',url:'https://basemaps.cartocdn.com/light_all/'+z+'/'+x+'/'+y+'.png',filter:'brightness(.72) saturate(.65)',credit:'carto'},
    {name:'OpenStreetMap',url:'https://tile.openstreetmap.org/'+z+'/'+x+'/'+y+'.png',filter:'brightness(.72) saturate(.65)',credit:'osm'}];
  let attempt=0;
  function next() {
    clearTimeout(tile.deadline);
    const provider=providers[attempt++],generation=++tile.generation;
    if(!provider) {
      tile.status='failed';tile.failures++;
      tile.retryAt=Date.now()+Math.min(60000,15000*2**Math.min(2,tile.failures-1));
      return;
    }
    tile.provider=provider;tile.fallback=attempt>1;
    tile.onload=()=>{
      if(generation!==tile.generation)return;
      clearTimeout(tile.deadline);tile.loaded=true;tile.status='ready';tile.failures=0;
    };
    tile.onerror=()=>{if(generation===tile.generation)next();};
    tile.deadline=setTimeout(()=>{if(generation===tile.generation)next();},6000);
    tile.src=provider.url;
  }
  next();
}
function retryMapTiles() {
  for(const [key,tile] of tileCache) if(!tile.loaded||tile.fallback) {
    clearTimeout(tile.deadline);tile.generation++;tile.onload=null;tile.onerror=null;tileCache.delete(key);
  }
  updateMapFeedback();
}
function updateMapFeedback() {
  const tiles=visibleMapTiles.filter(Boolean),loaded=tiles.filter(t=>t.loaded);
  const failed=tiles.some(t=>t.status==='failed');
  const fallback=loaded.some(t=>t.fallback);
  const slow=tiles.some(t=>!t.loaded&&Date.now()-t.startedAt>6500);
  const offline=navigator.onLine===false;
  const label=offline?'Offline · city network available':!tiles.length?'Loading street map…':!loaded.length?(failed?'Street map unavailable · city network available':slow?'Primary map unavailable · trying backup…':'Loading street map…'):fallback?'Backup street map active':loaded.length<tiles.length?'Street map loading · '+loaded.length+'/'+tiles.length:'Street map ready';
  document.getElementById('mapHealth').textContent=label;
  document.getElementById('mapRetry').hidden=!(offline||failed||fallback||slow);
  const credits=document.getElementById('mapCredits');credits.replaceChildren();
  const kinds=new Set(loaded.map(t=>t.provider.credit));
  function credit(text,url) {const a=document.createElement('a');a.textContent=text;a.href=url;a.target='_blank';a.rel='noopener noreferrer';if(credits.childNodes.length)credits.append(' · ');credits.append(a);}
  if(kinds.has('google'))credit('Google Maps','https://maps.google.com/');
  if(kinds.has('carto')||kinds.has('osm'))credit('© OpenStreetMap contributors','https://www.openstreetmap.org/copyright');
  if(kinds.has('carto'))credit('© CARTO','https://carto.com/attributions');
  if(!loaded.length)credits.textContent='Pan and zoom remain available';
}
window.addEventListener('online',retryMapTiles);
