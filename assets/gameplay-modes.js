/*
 * LANTERN DISPATCH — GAMEPLAY MODES ENGINE
 * Inspired by Railroad Tycoon / Railroad Tycoon II:
 *   1. SOLO — Quick Play (Random Scenario Generator + Selectable Victory Goal)
 *   2. SOLO — 20 Historical Timeline Scenarios (Bronze / Silver / Gold Medals, ₹5 Cr, Own 4 Companies, 2+ Countries, Lockdown Survival, Debt Turnaround)
 *   3. INTERACTIVE TUTORIAL (8-Step Hands-On Dispatcher & Tycoon Academy)
 */

const COUNTRY_BRANCHES = [
  { code: "IN", flag: "🇮🇳", name: "India (Bengaluru / ONDC Grid)", cost: 8500, quarterlyYield: 420 },
  { code: "US", flag: "🇺🇸", name: "United States (NYC & Bay Area)", cost: 11000, quarterlyYield: 560 },
  { code: "GB", flag: "🇬🇧", name: "United Kingdom (London TfL)", cost: 10500, quarterlyYield: 520 },
  { code: "JP", flag: "🇯🇵", name: "Japan (Tokyo Shinkansen Loop)", cost: 11500, quarterlyYield: 590 },
  { code: "SG", flag: "🇸🇬", name: "Singapore (Strait Autonomous)", cost: 9800, quarterlyYield: 490 },
  { code: "FR", flag: "🇫🇷", name: "France (Paris Euro-Terminal)", cost: 10200, quarterlyYield: 510 }
];

const SCENARIO_HOME_COUNTRY = ["IN", "US", "GB", "US", "JP", "SG"];

function ensureModeState() {
  if (!state) return;
  if (!state.gameMode) state.gameMode = "standard"; // "standard" | "quickplay" | "historical" | "tutorial"
  const homeCode = SCENARIO_HOME_COUNTRY[state.scenarioId || 0] || "IN";
  if (!Array.isArray(state.countries) || state.countries.length === 0) {
    state.countries = [homeCode];
  } else if (!state.countries.includes(homeCode)) {
    state.countries.unshift(homeCode);
  }
  if (typeof state.quickGoalId !== "number") state.quickGoalId = 0;
  if (typeof state.histScenarioId !== "number") state.histScenarioId = 0;
  if (typeof state.tutorialStep !== "number") state.tutorialStep = 0;
  if (!state.medalsEarned) state.medalsEarned = {};
}

function ownedCompaniesCount() {
  if (!state || !Array.isArray(state.companies)) return 1;
  return 1 + state.companies.filter(c => c.merged).length;
}

function countriesCount() {
  ensureModeState();
  return (state.countries || ["IN"]).length;
}

function establishCountryBranch(code) {
  ensureModeState();
  if (state.countries.includes(code)) {
    state.message = "Country franchise already active!";
    refreshUI();
    return false;
  }
  const branch = COUNTRY_BRANCHES.find(b => b.code === code);
  if (!branch) return false;
  if (state.cash < branch.cost) {
    state.message = `Need ${money(branch.cost)} to charter international subsidiary in ${branch.flag} ${branch.name}.`;
    refreshUI();
    return false;
  }
  state.cash -= branch.cost;
  state.expenses += branch.cost;
  state.countries.push(code);
  state.message = `Chartered ${branch.flag} ${branch.name} branch! Active in ${countriesCount()} countries (+${money(branch.quarterlyYield)}/quarter).`;
  playTone(860, 0.16);
  saveState();
  refreshUI();
  renderModeModal();
  return true;
}

/*
 * 1A. SOLO QUICK PLAY — SELECTABLE CUSTOM GOALS
 */
const QUICK_PLAY_GOALS = [
  {
    id: 0,
    icon: "💰",
    name: "Survive & Reach ₹5 Crore Net Worth",
    shortDesc: "Build an empire worth ₹5 Crore (312,500 cr / $312.5k equivalent) without insolvency.",
    check: () => netWorth() >= 312500,
    progressText: () => `Net Worth: ${money(netWorth())} / ${money(312500)} (₹5 Cr Target)`
  },
  {
    id: 1,
    icon: "🏛️",
    name: "CEO Mogul: Own 4 Companies",
    shortDesc: "Railroad Tycoon M&A Goal: Acquire & merge all 3 rival operators so you own 4 companies total.",
    check: () => ownedCompaniesCount() >= 4,
    progressText: () => `Companies Owned: ${ownedCompaniesCount()} / 4 (1 Flagship + ${ownedCompaniesCount() - 1}/3 Rivals Merged)`
  },
  {
    id: 2,
    icon: "🌏",
    name: "Cross-Border Pioneer: Establish in 2 Countries",
    shortDesc: "Expand internationally into at least 2 countries and complete 35 passenger/cargo hauls.",
    check: () => countriesCount() >= 2 && state.rides >= 35,
    progressText: () => `Countries: ${countriesCount()} / 2 · Hauls: ${state.rides} / 35`
  },
  {
    id: 3,
    icon: "🌐",
    name: "Global Mobility Syndicate: Establish in 4 Countries",
    shortDesc: "Charter international branches across 4 countries and reach 80,000 cr Net Worth.",
    check: () => countriesCount() >= 4 && netWorth() >= 80000,
    progressText: () => `Countries: ${countriesCount()} / 4 · Worth: ${money(netWorth())} / ${money(80000)}`
  },
  {
    id: 4,
    icon: "🛤️",
    name: "Airport Express & 120-Ton Haulage Baron",
    shortDesc: "Open the Airport Hub (#8), link it via upgraded corridor, and haul 120 tons of passengers/freight.",
    check: () => state.districts[7] && state.districts[7].open && (state.cargoTons || 0) >= 120,
    progressText: () => `Airport Hub: ${state.districts[7]?.open ? "OPEN ✓" : "Locked"} · Tonnage: ${state.cargoTons || 0} / 120t`
  },
  {
    id: 5,
    icon: "🔔",
    name: "Debt-Free Wall Street IPO (0 Debt & 65,000 cr Worth)",
    shortDesc: "Retire all Corporate Bonds & Margin Debt while reaching 65,000 cr Net Worth.",
    check: () => state.bonds === 0 && state.marginDebt === 0 && netWorth() >= 65000 && state.rides >= 25,
    progressText: () => `Worth: ${money(netWorth())}/${money(65000)} · Debt: ${money(state.marginDebt + bondDebt())} · Hauls: ${state.rides}/25`
  },
  {
    id: 6,
    icon: "⭐",
    name: "5-Star Executive Fleet Crown (4.92★ & 6 Vehicles)",
    shortDesc: "Operate at least 6 spotless vehicles, complete 40 trips, and hold a 4.92★+ passenger review score.",
    check: () => state.taxis.length >= 6 && state.rides >= 40 && state.rating >= 4.92,
    progressText: () => `Fleet: ${state.taxis.length}/6 · Rating: ${state.rating.toFixed(2)}★/4.92★ · Hauls: ${state.rides}/40`
  }
];

/*
 * 1B. SOLO SCENARIOS — 20 RAILROAD-TYCOON STYLE HISTORICAL TIMELINE SCENARIOS
 */
const RAILROAD_SCENARIOS = [
  {
    id: 0, cityId: 0, startYear: 2010, endYear: 2020, category: "Wealth & Survival",
    title: "1. Silicon Plateau ₹5 Crore Gold Rush",
    era: "🇮🇳 Bengaluru · 2010–2020",
    briefing: "Bengaluru's smartphone cab boom is igniting. Survive the 2013 aggregator fare wars and build a company worth ₹5 Crore (312,500 cr)!",
    setup: s => { s.cash = 16000; },
    bronzeText: "Reach ₹2 Crore (125,000 cr) Net Worth",
    silverText: "Reach ₹3.5 Crore (218,750 cr) & 5 Hubs",
    goldText: "Survive & Reach ₹5 Crore (312,500 cr) Net Worth + 6 Hubs",
    checkBronze: () => netWorth() >= 125000,
    checkSilver: () => netWorth() >= 218750 && openCount() >= 5,
    checkGold: () => netWorth() >= 312500 && openCount() >= 6
  },
  {
    id: 1, cityId: 1, startYear: 2008, endYear: 2020, category: "CEO & M&A",
    title: "2. The Four-Company Robber Baron",
    era: "🇺🇸 New York City · 2008–2020",
    briefing: "Classic Railroad Tycoon Hostile Takeover: Use Wall Street share purchases and mergers to own all 4 companies (Your Flagship + 3 Rivals)!",
    setup: s => { s.cash = 22000; },
    bronzeText: "Own 2 Companies (Acquire 1 Rival)",
    silverText: "Own 3 Companies (Acquire 2 Rivals)",
    goldText: "CEO Monopoly: Own All 4 Companies + 0 Margin Debt",
    checkBronze: () => ownedCompaniesCount() >= 2,
    checkSilver: () => ownedCompaniesCount() >= 3,
    checkGold: () => ownedCompaniesCount() >= 4 && state.marginDebt === 0
  },
  {
    id: 2, cityId: 0, startYear: 2014, endYear: 2024, category: "Multi-Country",
    title: "3. Two-Flag Indo-Pacific Bridge",
    era: "🇮🇳 India & 🇸🇬 Singapore · 2014–2024",
    briefing: "Prove your dispatch network in Bengaluru, then charter an international branch to establish operations in 2 countries!",
    setup: s => { s.elapsed = (2014 - s.startYear) * 50; s.cash = 15500; },
    bronzeText: "Open 5 Local Hubs & 20 Hauls",
    silverText: "Establish Operations in 2 Countries",
    goldText: "Establish in 2 Countries + 45 Hauls + 60,000 cr Worth",
    checkBronze: () => openCount() >= 5 && state.rides >= 20,
    checkSilver: () => countriesCount() >= 2,
    checkGold: () => countriesCount() >= 2 && state.rides >= 45 && netWorth() >= 60000
  },
  {
    id: 3, cityId: 2, startYear: 2015, endYear: 2025, category: "Multi-Country",
    title: "4. Global Four-Nation Syndicate",
    era: "🇬🇧 London HQ → 4 Countries · 2015–2025",
    briefing: "From Westminster & Canary Wharf, build an international mobility holding company operating across 4 countries!",
    setup: s => { s.elapsed = (2015 - 2008) * 50; s.cash = 19000; },
    bronzeText: "Establish in 2 Countries",
    silverText: "Establish in 3 Countries + 30 Hauls",
    goldText: "Establish in 4 Countries + Own 2 Companies + 80,000 cr Worth",
    checkBronze: () => countriesCount() >= 2,
    checkSilver: () => countriesCount() >= 3 && state.rides >= 30,
    checkGold: () => countriesCount() >= 4 && ownedCompaniesCount() >= 2 && netWorth() >= 80000
  },
  {
    id: 4, cityId: 0, startYear: 2020, endYear: 2025, category: "Crisis & Turnaround",
    title: "5. 2020 Lockdown Survival & Rescue",
    era: "🇮🇳 Bengaluru · March 2020–2025",
    briefing: "You take over in 2020 as nationwide lockdowns hit. You start with 2 Corporate Bonds (10,000 cr debt). Survive, retire all debt, and reach ₹2.5 Crore!",
    setup: s => { s.elapsed = (2020 - 2010) * 50; s.bonds = 2; s.cash = 11000; },
    bronzeText: "Survive to 2022 with Positive Cash & 15 Hauls",
    silverText: "Retire All Corporate Bonds (0 Bonds) + 30 Hauls",
    goldText: "0 Bonds + ₹2.5 Crore (156,250 cr) Net Worth",
    checkBronze: () => year() >= 2022 && state.cash > 0 && state.rides >= 15,
    checkSilver: () => state.bonds === 0 && state.rides >= 30,
    checkGold: () => state.bonds === 0 && netWorth() >= 156250
  },
  {
    id: 5, cityId: 0, startYear: 2011, endYear: 2018, category: "Corridors & Haulage",
    title: "6. Kempegowda Airport Express Link",
    era: "🇮🇳 Bengaluru · 2011–2018",
    briefing: "Railroad Tycoon Connection Scenario: Link MG Road (#1) through Hebbal (#7) to Kempegowda Airport (#8) and haul 120 tons of passengers & air cargo!",
    setup: s => { s.elapsed = (2011 - 2010) * 50; s.cash = 15000; },
    bronzeText: "Unlock Kempegowda Airport Hub (#8)",
    silverText: "Airport Hub Open + 80 Tons Hauled",
    goldText: "Tier-2+ Airport Corridor + 120 Tons Hauled",
    checkBronze: () => state.districts[7]?.open,
    checkSilver: () => state.districts[7]?.open && (state.cargoTons || 0) >= 80,
    checkGold: () => state.districts[7]?.open && corridorLinkMinTier(0, 7) >= 2 && (state.cargoTons || 0) >= 120
  },
  {
    id: 6, cityId: 0, startYear: 2014, endYear: 2019, category: "CEO & M&A",
    title: "7. 2015 Indian Cab Consolidation Wave",
    era: "🇮🇳 Bengaluru · 2014–2019",
    briefing: "Inspired by the 2015 Indian cab market consolidation: acquire 2 rival cab companies and scale your active fleet to 8 vehicles!",
    setup: s => { s.elapsed = (2014 - 2010) * 50; s.cash = 18500; },
    bronzeText: "Own 2 Companies (Acquire 1 Rival)",
    silverText: "Own 3 Companies (Acquire 2 Rivals)",
    goldText: "Own 3 Companies + 8 Active Fleet Vehicles",
    checkBronze: () => ownedCompaniesCount() >= 2,
    checkSilver: () => ownedCompaniesCount() >= 3,
    checkGold: () => ownedCompaniesCount() >= 3 && state.taxis.length >= 8
  },
  {
    id: 7, cityId: 0, startYear: 2021, endYear: 2025, category: "Service & Fleet",
    title: "8. 2022 Namma Yatri Zero-Commission Uprising",
    era: "🇮🇳 Bengaluru · 2021–2025",
    briefing: "Switch to the Zero-Commission Driver Subscription model (0% ride cut), recruit 6 licensed drivers, and maintain a 4.92★ rating!",
    setup: s => { s.elapsed = (2021 - 2010) * 50; s.businessModel = 2; s.cash = 14500; },
    bronzeText: "Zero-Comm Model Active + 25 Hauls",
    silverText: "5 Licensed Drivers + 4.88★ Rating",
    goldText: "Zero-Comm Model + 6 Drivers + 4.92★ Rating + 45 Hauls",
    checkBronze: () => state.businessModel === 2 && state.rides >= 25,
    checkSilver: () => (state.drivers?.length || 0) >= 5 && state.rating >= 4.88,
    checkGold: () => state.businessModel === 2 && (state.drivers?.length || 0) >= 6 && state.rating >= 4.92 && state.rides >= 45
  },
  {
    id: 8, cityId: 1, startYear: 2007, endYear: 2015, category: "Corridors & Haulage",
    title: "9. Manhattan to JFK Golden Corridor",
    era: "🇺🇸 New York City · 2007–2015",
    briefing: "Connect Times Square through Queens to JFK Int'l Airport (#8), upgrade the airport express corridor, and complete 50 hauls!",
    setup: s => { s.cash = 16500; },
    bronzeText: "Open JFK Int'l Airport Hub (#8)",
    silverText: "JFK Open + 35 Hauls + $45,000 Worth",
    goldText: "Tier-2+ JFK Corridor + 50 Hauls + $65,000 Worth",
    checkBronze: () => state.districts[7]?.open,
    checkSilver: () => state.districts[7]?.open && state.rides >= 35 && netWorth() >= 45000,
    checkGold: () => state.districts[7]?.open && corridorLinkMinTier(0, 7) >= 2 && state.rides >= 50 && netWorth() >= 65000
  },
  {
    id: 9, cityId: 2, startYear: 2012, endYear: 2018, category: "Service & Fleet",
    title: "10. Thames Olympic & Black Cab Charter",
    era: "🇬🇧 London · 2012–2018",
    briefing: "London's 2012 Olympic summer demands world-class reliability. Open 6 London stations while keeping passenger reviews above 4.92★!",
    setup: s => { s.elapsed = (2012 - 2008) * 50; s.cash = 16000; },
    bronzeText: "Open 5 London Stations",
    silverText: "Open 6 London Stations + 4.88★ Rating",
    goldText: "Open 6 Stations + 4.92★ Rating + 45 Hauls",
    checkBronze: () => openCount() >= 5,
    checkSilver: () => openCount() >= 6 && state.rating >= 4.88,
    checkGold: () => openCount() >= 6 && state.rating >= 4.92 && state.rides >= 45
  },
  {
    id: 10, cityId: 2, startYear: 2017, endYear: 2023, category: "Service & Fleet",
    title: "11. 2017 TfL Licensing & Safety Audit",
    era: "🇬🇧 London · 2017–2023",
    briefing: "Transport for London's September 2017 licensing scrutiny requires impeccable standards. Recruit 3 Executive Managers and hold 4.93★!",
    setup: s => { s.elapsed = (2017 - 2008) * 50; s.cash = 17500; },
    bronzeText: "Recruit 2 Executive Managers + 20 Hauls",
    silverText: "Recruit 3 Managers + 4.90★ Rating",
    goldText: "3 Managers + 4.93★ Rating + 40 Hauls + No Broken Units",
    checkBronze: () => (state.managers?.filter(m => m.hired).length || 0) >= 2 && state.rides >= 20,
    checkSilver: () => (state.managers?.filter(m => m.hired).length || 0) >= 3 && state.rating >= 4.90,
    checkGold: () => (state.managers?.filter(m => m.hired).length || 0) >= 3 && state.rating >= 4.93 && state.rides >= 40 && state.taxis.every(t => !t.broken)
  },
  {
    id: 11, cityId: 3, startYear: 2016, endYear: 2024, category: "Corridors & Haulage",
    title: "12. Silicon Valley Autonomous Pioneer",
    era: "🇺🇸 San Francisco · 2016–2024",
    briefing: "Connect Market St / SOMA down the Peninsula to Palo Alto and SFO Airport (#8) while building a 6-vehicle high-tech fleet!",
    setup: s => { s.elapsed = (2016 - 2009) * 50; s.cash = 17000; },
    bronzeText: "Open SFO Airport Hub + 4 Vehicles",
    silverText: "6 Hubs Open + 5 Vehicles + $55,000 Worth",
    goldText: "7 Hubs Open + 6 Vehicles + $75,000 Net Worth",
    checkBronze: () => state.districts[7]?.open && state.taxis.length >= 4,
    checkSilver: () => openCount() >= 6 && state.taxis.length >= 5 && netWorth() >= 55000,
    checkGold: () => openCount() >= 7 && state.taxis.length >= 6 && netWorth() >= 75000
  },
  {
    id: 12, cityId: 3, startYear: 2022, endYear: 2025, category: "Service & Fleet",
    title: "13. 2023 CPUC 24/7 Robotaxi Permit Race",
    era: "🇺🇸 San Francisco · 2022–2025",
    briefing: "California regulators are granting 24/7 driverless fare permits in SF. Hire the AI Surge & Route Director and reach $80,000 Net Worth!",
    setup: s => { s.elapsed = (2022 - 2009) * 50; s.cash = 19500; },
    bronzeText: "Hire AI Surge Director + 25 Hauls",
    silverText: "$60,000 Net Worth + 35 Hauls",
    goldText: "AI Director Hired + $80,000 Worth + 50 Hauls",
    checkBronze: () => hasManager(3) && state.rides >= 25,
    checkSilver: () => netWorth() >= 60000 && state.rides >= 35,
    checkGold: () => hasManager(3) && netWorth() >= 80000 && state.rides >= 50
  },
  {
    id: 13, cityId: 4, startYear: 2015, endYear: 2021, category: "Service & Fleet",
    title: "14. 2017 Tokyo JPN Taxi & Shinkansen Loop",
    era: "🇯🇵 Tokyo · 2015–2021",
    briefing: "Prepare Tokyo's rail-feeder network for the 2017 JPN Taxi launch. Maintain 4.95★ Omotenashi hospitality across 7 Tokyo hubs!",
    setup: s => { s.elapsed = (2015 - 2009) * 50; s.cash = 17500; },
    bronzeText: "Open 5 Tokyo Hubs + 25 Hauls",
    silverText: "Open 6 Tokyo Hubs + 4.90★ Rating",
    goldText: "Open 7 Tokyo Hubs + 4.95★ Rating + 50 Hauls",
    checkBronze: () => openCount() >= 5 && state.rides >= 25,
    checkSilver: () => openCount() >= 6 && state.rating >= 4.90,
    checkGold: () => openCount() >= 7 && state.rating >= 4.95 && state.rides >= 50
  },
  {
    id: 14, cityId: 4, startYear: 2019, endYear: 2025, category: "CEO & M&A",
    title: "15. 2020 Tokyo GO App Unification",
    era: "🇯🇵 Tokyo · 2019–2025",
    briefing: "Inspired by the September 2020 unification of JapanTaxi and MOV into GO: consolidate 3 companies in Tokyo and upgrade 3 Express Hubs!",
    setup: s => { s.elapsed = (2019 - 2009) * 50; s.cash = 20000; },
    bronzeText: "Own 2 Companies in Tokyo",
    silverText: "Own 3 Companies + 2 Lv2+ Hubs",
    goldText: "Own 3 Companies + 3 Lv2+ Hubs + 50 Hauls",
    checkBronze: () => ownedCompaniesCount() >= 2,
    checkSilver: () => ownedCompaniesCount() >= 3 && totalHubLevels() >= 2,
    checkGold: () => ownedCompaniesCount() >= 3 && totalHubLevels() >= 3 && state.rides >= 50
  },
  {
    id: 15, cityId: 5, startYear: 2016, endYear: 2022, category: "Multi-Country",
    title: "16. 2018 Southeast Asia Super-App Merger",
    era: "🇸🇬 Singapore · 2016–2022",
    briefing: "Compete along Marina Bay and Changi Airport, acquire 2 rivals in Singapore, and expand into a 2nd country!",
    setup: s => { s.elapsed = (2016 - 2011) * 50; s.cash = 19000; },
    bronzeText: "Open 5 Singapore Hubs + 25 Hauls",
    silverText: "Own 2 Companies + Establish in 2 Countries",
    goldText: "Own 3 Companies + 2 Countries + 75,000 cr Worth",
    checkBronze: () => openCount() >= 5 && state.rides >= 25,
    checkSilver: () => ownedCompaniesCount() >= 2 && countriesCount() >= 2,
    checkGold: () => ownedCompaniesCount() >= 3 && countriesCount() >= 2 && netWorth() >= 75000
  },
  {
    id: 16, cityId: 1, startYear: 2017, endYear: 2021, category: "Wealth & Survival",
    title: "17. 2019 Wall Street Mobility IPO Roadshow",
    era: "🇺🇸 New York City · 2017–2021",
    briefing: "2019 is the year of historic ride-hailing IPOs on NYSE & Nasdaq. Build a clean balance sheet (0 Debt) and reach $90,000 Net Worth!",
    setup: s => { s.elapsed = (2017 - 2007) * 50; s.cash = 18000; },
    bronzeText: "Reach $55,000 Net Worth",
    silverText: "Reach $72,000 Net Worth + 0 Debt",
    goldText: "Reach $90,000 Net Worth + 0 Debt + 35,000 cr Revenue",
    checkBronze: () => netWorth() >= 55000,
    checkSilver: () => netWorth() >= 72000 && state.bonds === 0 && state.marginDebt === 0,
    checkGold: () => netWorth() >= 90000 && state.bonds === 0 && state.marginDebt === 0 && state.revenue >= 35000
  },
  {
    id: 17, cityId: 2, startYear: 2013, endYear: 2019, category: "Crisis & Turnaround",
    title: "18. The Bankrupt Fleet Turnaround CEO",
    era: "🇬🇧 London · 2013–2019",
    briefing: "Classic Railroad Tycoon Receivership Challenge: You inherit 3 maxed-out Corporate Bonds (15,000 cr debt) and worn vehicles. Retire every bond and hit £55,000 Worth!",
    setup: s => {
      s.elapsed = (2013 - 2008) * 50;
      s.bonds = 3;
      s.cash = 9500;
      if (s.taxis[0]) { s.taxis[0].condition = 52; s.taxis[0].cleanliness = 58; }
    },
    bronzeText: "Pay Down Debt to 1 Corporate Bond",
    silverText: "Retire All 3 Corporate Bonds (0 Bonds)",
    goldText: "0 Bonds + 0 Margin Debt + 55,000 cr Worth + 35 Hauls",
    checkBronze: () => state.bonds <= 1,
    checkSilver: () => state.bonds === 0,
    checkGold: () => state.bonds === 0 && state.marginDebt === 0 && netWorth() >= 55000 && state.rides >= 35
  },
  {
    id: 18, cityId: 1, startYear: 2023, endYear: 2025, category: "Corridors & Haulage",
    title: "19. 2025 Manhattan Congestion Zone Express",
    era: "🇺🇸 New York City · 2023–2025",
    briefing: "Manhattan's 5 January 2025 Congestion Relief Zone rewards high-capacity executive shuttles. Upgrade 3 vehicles to Comfort Lv3 and haul 140 tons!",
    setup: s => { s.elapsed = (2023 - 2007) * 50; s.cash = 19000; },
    bronzeText: "Haul 70 Tons + 1 Comfort Lv3 Vehicle",
    silverText: "Haul 105 Tons + 2 Comfort Lv3 Vehicles",
    goldText: "Haul 140 Tons + 3 Comfort Lv3 Vehicles",
    checkBronze: () => (state.cargoTons || 0) >= 70 && state.taxis.filter(t => (t.comfortLevel || 1) >= 3).length >= 1,
    checkSilver: () => (state.cargoTons || 0) >= 105 && state.taxis.filter(t => (t.comfortLevel || 1) >= 3).length >= 2,
    checkGold: () => (state.cargoTons || 0) >= 140 && state.taxis.filter(t => (t.comfortLevel || 1) >= 3).length >= 3
  },
  {
    id: 19, cityId: 0, startYear: 2010, endYear: 2025, category: "Grand Campaign",
    title: "20. The ₹5 Crore Global Mobility Emperor",
    era: "🌍 Multi-Country Grand Campaign · 2010–2025",
    briefing: "The ultimate Railroad Tycoon Grand Campaign: Reach ₹5 Crore (312,500 cr) Net Worth, own all 4 companies, and establish branches across 3+ countries!",
    setup: s => { s.cash = 20000; },
    bronzeText: "Reach ₹2 Crore (125,000 cr) + 2 Countries",
    silverText: "Reach ₹3.5 Crore (218,750 cr) + Own 3 Companies + 2 Countries",
    goldText: "₹5 Crore (312,500 cr) + Own All 4 Companies + 3 Countries",
    checkBronze: () => netWorth() >= 125000 && countriesCount() >= 2,
    checkSilver: () => netWorth() >= 218750 && ownedCompaniesCount() >= 3 && countriesCount() >= 2,
    checkGold: () => netWorth() >= 312500 && ownedCompaniesCount() >= 4 && countriesCount() >= 3
  }
];

/*
 * 2. INTERACTIVE 8-STEP TUTORIAL ("LANTERN TYCOON ACADEMY")
 */
const TUTORIAL_STEPS = [
  {
    step: 1,
    title: "Welcome! Inspect a City Station Hub",
    copy: "Pan/zoom the real street map or tap any station hub on the map (or click 'Highlight Hub #1' below) to inspect its passenger demand and corridor options.",
    cta: "Highlight Hub #1",
    trigger: () => { selectedHub = 0; renderHubPopover(); },
    done: () => selectedHub >= 0 || state.rides >= 1
  },
  {
    step: 2,
    title: "Dispatch Your First Passenger Fare",
    copy: "Tap a waiting Ride Ticket at the bottom of the screen (or switch AUTO: ON) to send Unit #1 on its first paid city haul!",
    cta: "Dispatch Waiting Fare",
    trigger: () => {
      if (state.requests.length === 0) createRide(0, 1);
      if (state.requests[0]) dispatchRide(state.requests[0].id);
    },
    done: () => state.taxis.some(t => t.status !== IDLE) || state.rides >= 1
  },
  {
    step: 3,
    title: "Master Speed & Auto-Line Controls",
    copy: "Like Railroad Tycoon, you can run automated scheduled corridors (AUTO: ON) and accelerate time (1× / 2× / 4×). Try setting speed to 2× or 4×!",
    cta: "Set Speed 2×",
    trigger: () => { state.speed = 2; state.autoDispatch = true; updateHUD(); },
    done: () => state.speed >= 2 || state.rides >= 2
  },
  {
    step: 4,
    title: "Open 🚖 FLEET & Expand or Upgrade",
    copy: "Open the 🚖 FLEET tab in the bottom dock to commission a second vehicle, wash cabin interiors, or upgrade Cabin Comfort for +14% higher fares.",
    cta: "Open 🚖 FLEET Tab",
    trigger: () => { selectTab(1); },
    done: () => activeTab === 1 || state.taxis.length >= 2 || state.taxis.some(t => (t.comfortLevel || 1) >= 2)
  },
  {
    step: 5,
    title: "Open 👨‍✈️ STAFF: Drivers, Managers & Reviews",
    copy: "Open 👨‍✈️ STAFF to recruit/train drivers, rest fatigued captains, hire Executive Managers (auto-maintenance/valet/AI), and read live 1★–5★ rider reviews.",
    cta: "Open 👨‍✈️ STAFF Tab",
    trigger: () => { selectTab(2); },
    done: () => activeTab === 2 || (state.drivers && state.drivers.length >= 2) || (state.managers && state.managers.some(m => m.hired))
  },
  {
    step: 6,
    title: "Open 🛤️ HUBS: Unlock Districts & Express Corridors",
    copy: "Open 🛤️ HUBS to unlock a 4th neighborhood hub or upgrade a station-to-station corridor to Tier 2 Express for faster vehicle speeds!",
    cta: "Open 🛤️ HUBS Tab",
    trigger: () => { selectTab(3); },
    done: () => activeTab === 3 || openCount() >= 4 || corridorCount() > 3
  },
  {
    step: 7,
    title: "Open 📈 FINANCE: Rivals, Takeovers & Bonds",
    copy: "Open 📈 FINANCE to buy 10% shares in a rival operator. Owning 60%+ lets you execute a hostile takeover to own multiple companies!",
    cta: "Open 📈 FINANCE Tab",
    trigger: () => { selectTab(4); },
    done: () => activeTab === 4 || state.companies.some(c => c.owned > 0 || c.merged)
  },
  {
    step: 8,
    title: "Graduate from Lantern Tycoon Academy! 🎓",
    copy: "You know how to run corridors, manage fleet hygiene/drivers, acquire rivals, and expand across countries. Claim your +5,000 cr Graduation Bonus!",
    cta: "Claim +5,000 cr & Graduate 🎓",
    trigger: () => { finishTutorial(); },
    done: () => false
  }
];

/*
 * MODE LAUNCHERS & MODAL UI CONTROLLER
 */
let activeModeTab = "scenarios"; // "scenarios" | "quickplay" | "tutorial" | "countries"
let scenarioFilter = "ALL";
let qpConfig = { cityId: 0, diffId: 2, goalId: 0 };

function openModeModal(tab) {
  ensureModeState();
  if (tab) activeModeTab = tab;
  const mask = document.getElementById("modeModalMask");
  if (!mask) return;
  renderModeModal();
  mask.hidden = false;
}

function closeModeModal() {
  const mask = document.getElementById("modeModalMask");
  if (mask) mask.hidden = true;
}

function randomizeQuickPlayConfig() {
  qpConfig.cityId = Math.floor(Math.random() * WORLD_SCENARIOS.length);
  qpConfig.diffId = Math.floor(Math.random() * DIFFICULTY_MODES.length);
  qpConfig.goalId = Math.floor(Math.random() * QUICK_PLAY_GOALS.length);
  renderModeModal();
}

function startSoloQuickPlay(randomAll) {
  if (randomAll) {
    qpConfig.cityId = Math.floor(Math.random() * WORLD_SCENARIOS.length);
    qpConfig.goalId = Math.floor(Math.random() * QUICK_PLAY_GOALS.length);
  }
  createScenarioState(qpConfig.cityId, qpConfig.diffId);
  state.gameMode = "quickplay";
  state.quickGoalId = qpConfig.goalId;
  state.scenarioWon = false;
  ensureModeState();
  createRide(0, 1);
  createRide(1, 2);
  const g = QUICK_PLAY_GOALS[qpConfig.goalId] || QUICK_PLAY_GOALS[0];
  state.message = `⚡ Quick Play (${currentScenario().city} · ${curDiff().name}): ${g.name}`;
  closeModeModal();
  closeSheet();
  resetCamera();
  saveState();
  refreshUI();
  playTone(790, 0.16);
}

function startHistoricalScenarioById(scenId) {
  const hs = RAILROAD_SCENARIOS.find(x => x.id === scenId) || RAILROAD_SCENARIOS[0];
  createScenarioState(hs.cityId, state.difficulty ?? 2);
  state.gameMode = "historical";
  state.histScenarioId = hs.id;
  state.scenarioWon = false;
  state.medalsEarned = {};
  ensureModeState();
  if (typeof hs.setup === "function") hs.setup(state);
  state.historyStage = currentScenario().chronicle.filter(e => e.year <= year()).length;
  state.pandemicMarketApplied = year() >= 2020;
  createRide(0, 1);
  createRide(1, 2);
  state.message = `📜 ${hs.title} (${hs.era}) — 🥇 Goal: ${hs.goldText}`;
  closeModeModal();
  closeSheet();
  resetCamera();
  saveState();
  refreshUI();
  playTone(840, 0.18);
}

function startInteractiveTutorial() {
  createScenarioState(0, 0); // Bengaluru on Easiest for smooth learning
  state.gameMode = "tutorial";
  state.tutorialStep = 1;
  state.cash = 22000;
  state.autoDispatch = false;
  ensureModeState();
  createRide(0, 1);
  createRide(1, 2);
  state.message = "🎓 Lantern Tycoon Academy started! Follow the step-by-step coach on the right.";
  closeModeModal();
  closeSheet();
  resetCamera();
  saveState();
  refreshUI();
  playTone(720, 0.15);
}

function advanceTutorialStep() {
  ensureModeState();
  if (state.gameMode !== "tutorial") return;
  if (state.tutorialStep < 8) {
    state.tutorialStep++;
    playTone(760, 0.1);
    saveState();
    updateTutorialBanner();
  } else {
    finishTutorial();
  }
}

function finishTutorial() {
  ensureModeState();
  state.cash += 5000;
  state.gameMode = "standard";
  state.tutorialStep = 0;
  state.autoDispatch = true;
  state.message = "🎓 Tutorial Graduated! +5,000 cr Academy Grant credited. Open 🎮 MODES anytime to play Quick Play or 20 Scenarios!";
  playTone(940, 0.22);
  saveState();
  refreshUI();
}

function exitTutorialMode() {
  ensureModeState();
  state.gameMode = "standard";
  state.tutorialStep = 0;
  saveState();
  updateTutorialBanner();
}

function updateTutorialBanner() {
  ensureModeState();
  const banner = document.getElementById("tutorialBanner");
  if (!banner) return;
  if (state.gameMode !== "tutorial" || !state.tutorialStep) {
    banner.hidden = true;
    return;
  }
  const stepObj = TUTORIAL_STEPS[Math.max(0, Math.min(TUTORIAL_STEPS.length - 1, state.tutorialStep - 1))];
  if (stepObj.done() && state.tutorialStep < 8) {
    state.tutorialStep++;
    saveState();
  }
  const cur = TUTORIAL_STEPS[Math.max(0, Math.min(TUTORIAL_STEPS.length - 1, state.tutorialStep - 1))];
  banner.hidden = false;
  banner.innerHTML = `
    <div class="flex-between">
      <span class="tut-step-tag">🎓 TUTORIAL STEP ${cur.step} / 8</span>
      <button class="drawer-close" style="width:22px;height:22px;font-size:10px" onclick="exitTutorialMode()" title="Exit Tutorial">✕</button>
    </div>
    <div class="tut-title">${cur.title}</div>
    <div class="tut-copy">${cur.copy}</div>
    <div class="tut-actions">
      <button class="cyber-btn gold" onclick="TUTORIAL_STEPS[${cur.step - 1}].trigger()">${cur.cta}</button>
      ${cur.step < 8 ? `<button class="cyber-btn" onclick="advanceTutorialStep()">Next Step ▸</button>` : ""}
      <button class="cyber-btn dim" onclick="openModeModal('scenarios')">Switch Mode</button>
    </div>
  `;
}

function evaluateActiveModeGoals() {
  ensureModeState();
  // Quarterly international country branch dividends
  if (state.countries && state.countries.length > 1 && state._lastCountryTick !== state.marketTick) {
    state._lastCountryTick = state.marketTick;
    let intlBonus = 0;
    state.countries.slice(1).forEach(code => {
      const b = COUNTRY_BRANCHES.find(x => x.code === code);
      if (b) intlBonus += b.quarterlyYield;
    });
    if (intlBonus > 0) {
      state.cash += intlBonus;
      state.revenue += intlBonus;
    }
  }

  if (state.gameMode === "quickplay") {
    const g = QUICK_PLAY_GOALS[state.quickGoalId || 0] || QUICK_PLAY_GOALS[0];
    if (!state.scenarioWon && g.check()) {
      state.scenarioWon = true;
      state.cash += 10000;
      state.message = `🏆 QUICK PLAY VICTORY: ${g.name}! +${money(10000)} bonus awarded!`;
      playTone(960, 0.25);
      saveState();
    }
  } else if (state.gameMode === "historical") {
    const hs = RAILROAD_SCENARIOS[state.histScenarioId || 0] || RAILROAD_SCENARIOS[0];
    if (!state.medalsEarned) state.medalsEarned = {};
    if (!state.medalsEarned.bronze && hs.checkBronze()) {
      state.medalsEarned.bronze = true;
      state.cash += 2500;
      state.message = `🥉 BRONZE MEDAL in "${hs.title}"! (+${money(2500)} bonus)`;
      playTone(740, 0.16);
    }
    if (!state.medalsEarned.silver && hs.checkSilver()) {
      state.medalsEarned.silver = true;
      state.cash += 5000;
      state.message = `🥈 SILVER MEDAL in "${hs.title}"! (+${money(5000)} bonus)`;
      playTone(840, 0.2);
    }
    if (!state.medalsEarned.gold && hs.checkGold()) {
      state.medalsEarned.gold = true;
      state.scenarioWon = true;
      state.cash += 10000;
      state.message = `🥇 GOLD MEDAL VICTORY in "${hs.title}"! (+${money(10000)} grand prize)`;
      playTone(980, 0.28);
    }
  }
}

function syncModeBannerInJourney() {
  ensureModeState();
  updateTutorialBanner();
  const modePill = document.getElementById("modeStatusLabel");
  if (modePill) {
    if (state.gameMode === "quickplay") modePill.textContent = "SOLO: QUICK";
    else if (state.gameMode === "historical") modePill.textContent = `SCEN #${(state.histScenarioId || 0) + 1}`;
    else if (state.gameMode === "tutorial") modePill.textContent = `TUTORIAL ${state.tutorialStep}/8`;
    else modePill.textContent = "MODES";
  }

  // If in Quick Play or Historical Scenario mode, enrich the #journey summary with live mode goal status
  const sumEl = document.getElementById("journeySummary");
  if (!sumEl) return;
  if (state.gameMode === "quickplay") {
    const g = QUICK_PLAY_GOALS[state.quickGoalId || 0] || QUICK_PLAY_GOALS[0];
    sumEl.innerHTML = `<strong style="color:var(--gold)">${g.icon} Quick Play Goal:</strong> ${g.name}<br/><span>${g.progressText()}</span>`;
  } else if (state.gameMode === "historical") {
    const hs = RAILROAD_SCENARIOS[state.histScenarioId || 0] || RAILROAD_SCENARIOS[0];
    const m = state.medalsEarned || {};
    const medals = `${m.bronze ? "🥉" : "⚪"} ${m.silver ? "🥈" : "⚪"} ${m.gold ? "🥇" : "⚪"}`;
    sumEl.innerHTML = `<strong style="color:var(--gold)">${medals} ${hs.title}</strong><br/><span>🥇 ${hs.goldText} · Co: ${ownedCompaniesCount()}/4 · Countries: ${countriesCount()}</span>`;
  }
}

function renderModeModal() {
  ensureModeState();
  const body = document.getElementById("modeModalBody");
  if (!body) return;

  ["scenarios", "quickplay", "tutorial", "countries"].forEach(t => {
    const btn = document.getElementById("modeTab_" + t);
    if (btn) btn.classList.toggle("active", activeModeTab === t);
  });

  if (activeModeTab === "quickplay") {
    const cityOpts = WORLD_SCENARIOS.map(c =>
      `<button class="cyber-btn ${qpConfig.cityId === c.id ? 'gold' : 'dim'}" onclick="qpConfig.cityId=${c.id};renderModeModal()">${c.flag} ${c.city}</button>`
    ).join("");
    const diffOpts = DIFFICULTY_MODES.map(d =>
      `<button class="cyber-btn ${qpConfig.diffId === d.id ? 'gold' : 'dim'}" onclick="qpConfig.diffId=${d.id};renderModeModal()">${d.badge}</button>`
    ).join("");
    const goalCards = QUICK_PLAY_GOALS.map(g => {
      const sel = qpConfig.goalId === g.id;
      return `<div class="scen-card ${sel ? 'active' : ''}" onclick="qpConfig.goalId=${g.id};renderModeModal()" style="cursor:pointer">
        <div>
          <div class="scen-title">${g.icon} ${g.name}</div>
          <div class="scen-desc" style="margin-top:4px">${g.shortDesc}</div>
        </div>
        <div class="flex-between" style="margin-top:6px">
          <span class="fleet-badge" style="color:${sel ? 'var(--gold)' : 'var(--text-dim)'}">${sel ? "SELECTED GOAL ✓" : "Tap to Select"}</span>
        </div>
      </div>`;
    }).join("");

    body.innerHTML = `
      <div class="holo-card" style="border-color:var(--gold)">
        <div class="flex-between">
          <div>
            <div class="h-title">⚡ Solo Quick Play — Custom or Random Scenario Generator</div>
            <div class="h-desc">Pick any world city, difficulty, and custom victory condition—or hit 🎲 Randomize Everything for an instant challenge!</div>
          </div>
          <div style="display:flex;gap:8px">
            <button class="cyber-btn" onclick="randomizeQuickPlayConfig()">🎲 Randomize Setup</button>
            <button class="cyber-btn gold" onclick="startSoloQuickPlay(false)">▶ Launch Quick Play</button>
          </div>
        </div>
      </div>
      <div class="holo-card">
        <div class="h-title">1. Select Starting World City</div>
        <div class="btn-row">${cityOpts}</div>
      </div>
      <div class="holo-card">
        <div class="h-title">2. Select Difficulty Mode</div>
        <div class="btn-row">${diffOpts}</div>
      </div>
      <div class="holo-card">
        <div class="h-title">3. Select Victory Goal (or Randomize)</div>
        <div class="scen-grid" style="margin-top:8px">${goalCards}</div>
      </div>
    `;
  } else if (activeModeTab === "scenarios") {
    const categories = ["ALL", "Wealth & Survival", "CEO & M&A", "Multi-Country", "Corridors & Haulage", "Service & Fleet", "Crisis & Turnaround", "Grand Campaign"];
    const filterBtns = categories.map(cat =>
      `<button class="cyber-btn ${scenarioFilter === cat ? 'gold' : 'dim'}" onclick="scenarioFilter='${cat}';renderModeModal()">${cat}</button>`
    ).join("");

    const filtered = RAILROAD_SCENARIOS.filter(s => scenarioFilter === "ALL" || s.category === scenarioFilter);
    const cards = filtered.map(s => {
      const isCur = state.gameMode === "historical" && state.histScenarioId === s.id;
      const city = WORLD_SCENARIOS[s.cityId] || WORLD_SCENARIOS[0];
      return `<div class="scen-card ${isCur ? 'active' : ''}">
        <div>
          <div class="flex-between">
            <span class="scen-era">${s.era}</span>
            <span class="fleet-badge">${s.category}</span>
          </div>
          <div class="scen-title">${s.title}</div>
          <div class="scen-desc" style="margin-top:4px">${s.briefing}</div>
          <div class="medal-strip" style="margin-top:8px">
            <div>🥉 <strong>Bronze:</strong> ${s.bronzeText}</div>
            <div>🥈 <strong>Silver:</strong> ${s.silverText}</div>
            <div>🥇 <strong style="color:var(--gold)">Gold:</strong> ${s.goldText}</div>
          </div>
        </div>
        <div class="flex-between" style="margin-top:8px">
          <span style="font-size:10.5px;color:var(--text-dim)">${city.currencySymbol} ${city.currencyCode} · ${s.startYear}–${s.endYear}</span>
          <button class="cyber-btn ${isCur ? 'gold' : ''}" onclick="startHistoricalScenarioById(${s.id})">
            ${isCur ? "Restart Scenario" : "Play Scenario ▸"}
          </button>
        </div>
      </div>`;
    }).join("");

    body.innerHTML = `
      <div class="holo-card">
        <div class="flex-between">
          <div>
            <div class="h-title">📜 20 Railroad-Tycoon Historical Timeline Scenarios</div>
            <div class="h-desc">Compete for 🥉 Bronze, 🥈 Silver, and 🥇 Gold Medals across real mobility history timelines—including Survive & Reach ₹5 Crore, Own 4 Companies, Establish in 2–4 Countries, 2020 Lockdown Rescue, and Debt Turnaround!</div>
          </div>
        </div>
        <div class="btn-row" style="margin-top:8px">${filterBtns}</div>
      </div>
      <div class="scen-grid">${cards}</div>
    `;
  } else if (activeModeTab === "tutorial") {
    const stepList = TUTORIAL_STEPS.map(st => {
      const cur = state.gameMode === "tutorial" && state.tutorialStep === st.step;
      const done = state.gameMode === "tutorial" && state.tutorialStep > st.step;
      return `<div class="holo-card" style="border-color:${cur ? 'var(--gold)' : done ? 'var(--mint)' : 'rgba(255,255,255,0.1)'}">
        <div class="flex-between">
          <div>
            <div class="h-title">${done ? "✅" : cur ? "👉" : "🎓"} Step ${st.step}: ${st.title}</div>
            <div class="h-desc">${st.copy}</div>
          </div>
          <span class="fleet-badge">${done ? "DONE" : cur ? "ACTIVE" : "STEP " + st.step}</span>
        </div>
      </div>`;
    }).join("");

    body.innerHTML = `
      <div class="holo-card" style="border-color:var(--gold)">
        <div class="flex-between">
          <div>
            <div class="h-title">🎓 Lantern Tycoon Academy — Interactive 8-Step Tutorial</div>
            <div class="h-desc">Learn real-map navigation, corridor upgrades, fleet hygiene & maintenance, driver fatigue, M&A share takeovers, and multi-country expansion step by step (+5,000 cr Graduation Bonus).</div>
          </div>
          <button class="cyber-btn gold" onclick="startInteractiveTutorial()">
            ${state.gameMode === "tutorial" ? "Restart Tutorial ▸" : "Start Interactive Tutorial ▸"}
          </button>
        </div>
      </div>
      ${stepList}
    `;
  } else if (activeModeTab === "countries") {
    const countryCards = COUNTRY_BRANCHES.map(b => {
      const active = (state.countries || []).includes(b.code);
      return `<div class="holo-card" style="border-color:${active ? 'var(--mint)' : 'rgba(255,255,255,0.1)'}">
        <div class="flex-between">
          <div>
            <div class="h-title">${b.flag} ${b.name}</div>
            <div class="h-desc">Quarterly International Franchise Dividend: <strong style="color:var(--mint)">+${money(b.quarterlyYield)} / quarter</strong></div>
          </div>
          <button class="cyber-btn ${active ? 'dim' : 'gold'}" onclick="establishCountryBranch('${b.code}')">
            ${active ? "ESTABLISHED ✓" : "Charter Branch (" + money(b.cost) + ")"}
          </button>
        </div>
      </div>`;
    }).join("");

    body.innerHTML = `
      <div class="holo-card" style="border-color:var(--cyan)">
        <div class="h-title">🌐 Multi-Country Franchise & Subsidiary Desk (${countriesCount()} / ${COUNTRY_BRANCHES.length} Countries Active)</div>
        <div class="h-desc">
          Companies Owned: <strong style="color:var(--gold)">${ownedCompaniesCount()} / 4</strong> ·
          Countries Established: <strong style="color:var(--mint)">${countriesCount()} / 6</strong> ·
          Net Worth: <strong style="color:var(--cyan)">${money(netWorth())}</strong><br/>
          Charter international subsidiaries to earn quarterly global dividends and fulfill Multi-Country & Grand Campaign goals!
        </div>
      </div>
      ${countryCards}
    `;
  }
}
