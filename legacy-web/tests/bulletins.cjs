const {chromium}=require('C:/Users/RAJES/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const assert=require('node:assert/strict');
const path=require('node:path');
(async()=>{
 const browser=await chromium.launch({headless:true,channel:'msedge'});
 try {
  const page=await browser.newPage({viewport:{width:1280,height:800},reducedMotion:'reduce'});
  await page.addInitScript(()=>{window.setDate=date=>{const y=Number(date.slice(0,4));state.elapsed=(y-currentScenario().startYear+(Date.parse(date)-Date.UTC(y,0,1))/(Date.UTC(y+1,0,1)-Date.UTC(y,0,1)))*50+.000001;};});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.route('https://**/*',r=>r.abort());
  await page.goto('file:///'+path.resolve(__dirname,'../index.html').replaceAll('\\','/'));
  await page.evaluate(()=>{state.speed=0;state.elapsed=8;updateHUD();});
  assert.equal(await page.locator('#pressOverlay').isVisible(),true);
  const before=await page.evaluate(()=>{state.speed=4;const before=[state.elapsed,state.cash];tickSimulation(.25);return before;});
  assert.deepEqual(await page.evaluate(()=>[state.elapsed,state.cash]),before);
  await page.locator('#pressClose').click();
  assert.equal(await page.evaluate(()=>state.speed),4);
  await page.evaluate(()=>{tickSimulation(.1);state.speed=0;});
  assert.ok(await page.evaluate(()=>state.elapsed>8));
  assert.equal(await page.locator('#mapCanvas').evaluate(e=>e.inert),false);
  // Exact historical dates, region filtering, and no premature share prices.
  await page.evaluate(()=>{
   createScenarioState(0);state.speed=0;
   window.setDate=date=>{const y=Number(date.slice(0,4));state.elapsed=(y-currentScenario().startYear+(Date.parse(date)-Date.UTC(y,0,1))/(Date.UTC(y+1,0,1)-Date.UTC(y,0,1)))*50+.000001;};
   setDate('2013-10-01');updateHistoricalPress();
  });
  assert.equal(await page.evaluate(()=>pressData().seen.includes('real-blr-uber')),false);
  await page.evaluate(()=>{setDate('2013-10-02');updateHUD();});
  assert.equal(await page.locator('#pressHeadline').innerText(),'Uber formally launches in Bengaluru');
  assert.match(await page.locator('#pressSource').getAttribute('href'),/uber.com/);
  await page.screenshot({path:path.resolve(__dirname,'../preview-historical-news.png')});
  await page.locator('#pressAction').click();
  assert.equal(await page.evaluate(()=>activeTab),3);
  await page.evaluate(()=>{closeSheet();setDate('2019-03-27');});
  assert.equal(await page.evaluate(()=>historicalQuoteRows().length),0);
  await page.evaluate(()=>setDate('2019-03-28'));
  assert.equal(await page.evaluate(()=>historicalQuoteRows()[0].price),72);
  assert.equal(await page.evaluate(()=>historicalQuoteRows()[0].type),'IPO offer');
  await page.evaluate(()=>{setDate('2021-04-01');selectTab(4);updateHUD();});
  assert.equal(await page.evaluate(()=>historicalQuoteRows().length),11);
  assert.match(await page.locator('#historicalMarket').innerText(),/US\$51.00/);
  assert.match(await page.locator('#historicalMarket').innerText(),/Quarter-end close/);
  assert.equal(await page.evaluate(()=>historicalForCity().some(e=>e.id==='tokyo-go')),false);
  // Reading from the archive must restore the background and original focus.
  await page.evaluate(()=>{closeSheet();resetPress();pressData().queue=[];pressLastClosed=state.elapsed;});
  await page.locator('#pressButton').click();
  await page.locator('.press-story').first().click();
  await page.keyboard.press('Escape');
  assert.equal(await page.locator('#pressButton').evaluate(e=>e.inert),false);
  assert.equal(await page.locator('#pressButton').evaluate(e=>e===document.activeElement),true);
  await page.evaluate(()=>{saveState();window.savedCount=pressData().stories.filter(s=>s.id==='real-blr-uber').length;updateHistoricalPress();});
  assert.equal(await page.evaluate(()=>pressData().stories.filter(s=>s.id==='real-blr-uber').length),1);
  await page.reload();
  assert.equal(await page.evaluate(()=>pressData().stories.filter(s=>s.id==='real-blr-uber').length),1);
  await page.evaluate(()=>{resetPress();state.speed=0;openBulletin(pressData().stories.find(s=>s.id==='real-blr-uber'));});
  for(const size of [{width:844,height:390},{width:390,height:844}]) {
   await page.setViewportSize(size);
   assert.equal(await page.locator('.press-paper').evaluate(el=>{const r=el.getBoundingClientRect();return r.left>=0&&r.right<=innerWidth&&r.top>=0&&r.bottom<=innerHeight;}),true);
   await page.locator('#pressClose').scrollIntoViewIfNeeded();
   await page.screenshot({path:path.resolve(__dirname,`../preview-news-${size.width}.png`)});
  }
  await page.evaluate(()=>{resetPress();createScenarioState(4);state.speed=0;setDate('2020-09-02');updateHistoricalPress();});
  assert.equal(await page.evaluate(()=>pressData().seen.includes('real-tokyo-go')),true);
  assert.equal(await page.evaluate(()=>pressData().seen.includes('real-blr-uber')),false);
  // A margin call publishes an urgent report only once for the same event.
  await page.evaluate(()=>{resetPress();createScenarioState(0);state.marginDebt=1000;maintainMargin();});
  assert.equal(await page.evaluate(()=>pressData().stories.some(s=>s.id==='margin-1'&&s.urgent)),true);
  assert.deepEqual(errors,[]);
  console.log('PASS: historical dates, regional filtering, 11 sourced quotes, modal pause/resume, archive persistence, deduplication, focus restoration, viewport bounds, and financial alerts.');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
