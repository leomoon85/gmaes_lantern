const {chromium}=require('C:/Users/RAJES/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const assert=require('node:assert/strict');
const path=require('node:path');
const pixel=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=','base64');
(async()=>{
 const browser=await chromium.launch({headless:true,channel:'msedge'});
 try{
  let page;
  const errors=[];
  let mode='primary';
  async function openFreshPage() {
   if(page)await page.close();
   page=await browser.newPage({viewport:{width:1280,height:800}});
   page.on('pageerror',e=>errors.push(e.message));
   await page.route('https://**/*',async route=>{
   const host=new URL(route.request().url()).host;
   if((mode==='primary'&&host==='mt1.google.com')||(mode==='fallback'&&host==='basemaps.cartocdn.com'))await route.fulfill({contentType:'image/png',headers:{'cache-control':'no-store'},body:pixel});
   else await route.abort();
   });
   await page.goto('file:///'+path.resolve(__dirname,'../index.html').replaceAll('\\','/'));
   await page.evaluate(()=>state.speed=0);
  }
  await openFreshPage();
  await page.waitForFunction(()=>visibleMapTiles.length&&visibleMapTiles.every(t=>t&&t.loaded));
  assert.equal(await page.evaluate(()=>mapStyleIdx),1);
  await page.waitForFunction(()=>document.getElementById('mapCredits').textContent.includes('Google Maps'));
  assert.match(await page.locator('#mapCredits').innerText(),/Google Maps/);
  // Failure of Google switches to the real street-map backup, not a cached blank.
  mode='fallback';await openFreshPage();
  await page.waitForFunction(()=>visibleMapTiles.length&&visibleMapTiles.every(t=>t&&t.loaded&&t.fallback));
  await page.waitForFunction(()=>document.getElementById('mapHealth').textContent.includes('Backup'));
  assert.match(await page.locator('#mapCredits').innerText(),/OpenStreetMap/);
  // Failure of every provider remains recoverable through the visible retry button.
  mode='none';await openFreshPage();
  await page.waitForFunction(()=>visibleMapTiles.length&&visibleMapTiles.every(t=>t&&t.status==='failed'));
  await page.waitForFunction(()=>document.getElementById('mapHealth').textContent.includes('unavailable'));
  mode='primary';await page.locator('#mapRetry').click();
  await page.waitForFunction(()=>visibleMapTiles.length&&visibleMapTiles.every(t=>t&&t.loaded&&!t.fallback));
  await page.evaluate(()=>{mapStyleIdx=3;persistMapStyle();});
  await page.reload();assert.equal(await page.evaluate(()=>mapStyleIdx),3);
  await page.evaluate(()=>state.speed=0);
  await page.waitForFunction(()=>visibleMapTiles.length&&visibleMapTiles.every(t=>t&&t.loaded));
  await page.context().setOffline(true);
  await page.waitForFunction(()=>document.getElementById('mapHealth').textContent.includes('Offline'));
  await page.context().setOffline(false);
  assert.deepEqual(errors,[]);
  console.log('PASS: Google default, successful tiles, primary failure/backup, full failure, manual retry/recovery, saved style, offline indicator, and no browser exceptions.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
