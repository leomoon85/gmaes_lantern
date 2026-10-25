const { chromium } = require('C:/Users/RAJES/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const assert = require('node:assert/strict');
const path = require('node:path');
(async () => {
  const browser = await chromium.launch({headless:true, channel:'msedge'});
  const page = await browser.newPage({viewport:{width:1280,height:800},reducedMotion:'reduce'});
  const errors=[]; page.on('pageerror', e=>errors.push(e.message));
  // Keep verification deterministic and independent of external map services.
  await page.route('https://**/*', route=>route.abort());
  await page.goto('file:///'+path.resolve(__dirname,'../index.html').replaceAll('\\','/'));
  await page.evaluate(()=>{state.speed=0;updateHUD();});
  assert.match(await page.locator('#journeyTitle').innerText(), /first regulars/);
  await page.evaluate(()=>{state.rides=5;updateHUD();});
  const cash=await page.evaluate(()=>state.cash);
  await page.locator('#journeyAction').click();
  assert.equal(await page.evaluate(()=>state.cash),cash+1200);
  assert.equal(await page.evaluate(()=>state.journeyChapter),1);
  await page.reload();
  assert.equal(await page.evaluate(()=>state.journeyChapter),1);
  assert.equal(await page.evaluate(()=>state.cash),cash+1200);
  await page.locator('#journeyAction').click();
  assert.equal(await page.evaluate(()=>activeTab),1);
  assert.equal(await page.locator('#journey').isVisible(),false);
  await page.locator('.drawer-close').click();
  await page.locator('#journeyToggle').click();
  assert.equal(await page.locator('#journeyAction').isVisible(),false);
  await page.locator('#journeyToggle').click();
  await page.locator('#optionsToggle').click();
  assert.equal(await page.locator('#btnDiff').isVisible(),true);
  await page.locator('#optionsToggle').click();
  assert.equal(await page.locator('#btnDiff').isVisible(),false);
  await page.screenshot({path:path.resolve(__dirname,'../preview-journey-desktop.png')});
  for(const size of [{width:844,height:390},{width:390,height:844}]) {
    await page.setViewportSize(size);
    await page.screenshot({path:path.resolve(__dirname,`../preview-journey-${size.width}.png`)});
    const fits=await page.locator('#journey').evaluate(el=>{const r=el.getBoundingClientRect();return r.left>=0&&r.right<=innerWidth&&r.bottom<innerHeight-70;});
    assert.equal(fits,true,'Goal fits viewport');
  }
  // Finish remaining chapters and verify rewards cannot be claimed again.
  await page.evaluate(()=>{state.taxis.push({...state.taxis[0],id:99});state.districts.forEach(d=>d.open=true);state.districts[0].level=2;state.rides=75;});
  const before=await page.evaluate(()=>state.cash);
  await page.evaluate(()=>{for(let i=0;i<5;i++)journeyAction();});
  assert.equal(await page.evaluate(()=>state.cash),before+14500);
  await page.evaluate(()=>journeyAction());
  assert.equal(await page.evaluate(()=>state.cash),before+14500);
  assert.deepEqual(errors,[]);
  await browser.close();
  console.log('Journey: rewards, save/reload, navigation, settings, viewport bounds, and one-time completion passed.');
})().catch(e=>{console.error(e);process.exit(1);});
