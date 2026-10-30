package com.lantern.dispatch;

import java.util.*;

/** Original offline mobility & rail-tycoon style simulation with world city scenarios. */
public final class Game implements java.io.Serializable {
  private static final long serialVersionUID = 1L;
  public static final int W = 31, H = 21, MAX_FLEET = 12, MAX_BONDS = 3;
  public static final long BOND_FACE = 5000L;
  public static final int IDLE = 0, PICKUP = 1, RIDING = 2, CHARGING = 3;
  public static final int KIND_COMMUTER = 0, KIND_EXECUTIVE = 1, KIND_AIRPORT = 2, KIND_CARGO = 3;
  public static final int FAC_MAINT = 1, FAC_LOUNGE = 2, FAC_CARGO = 4, FAC_CHARGER = 8;
  public static final int WAIT_ANY = 0, WAIT_HALF = 1, WAIT_FULL = 2;
  public static final int DIFF_EASIEST = 0,
      DIFF_EASY = 1,
      DIFF_MEDIUM = 2,
      DIFF_HIGH = 3,
      DIFF_PROFESSIONAL = 4;
  public static final String[] DIFFICULTY_NAMES = {
    "Easiest", "Easy", "Medium", "High", "Professional"
  };
  public static final int MODE_SOLO_QUICK = 0,
      MODE_SOLO_SCENARIO = 1,
      MODE_TUTORIAL = 2,
      MODE_CITY = 3;
  public static final String[] GAME_MODE_NAMES = {
    "Solo Quick Play", "Solo Historical Scenarios", "Interactive Tutorial"
  };
  public static final int GOAL_WORTH_5CR = 0,
      GOAL_OWN_4_COMPANIES = 1,
      GOAL_ESTABLISH_2_COUNTRIES = 2,
      GOAL_CORRIDOR_HAUL = 3,
      GOAL_DEBT_FREE_IPO = 4,
      GOAL_SERVICE_CROWN = 5;
  public static final String[] QUICK_GOAL_NAMES = {
    "Survive & Reach ₹5 Crore Net Worth",
    "CEO Mogul: Own 4 Companies",
    "Cross-Border: Establish in 2+ Countries",
    "Corridor Baron: Connect Airport & Haul 120 Tons",
    "Debt-Free IPO: 0 Debt & 60,000 cr Worth",
    "5-Star Service Crown: 4.92★ Rating & 6 Fleet"
  };

  public static final class HistoricalScenarioDef {
    public final int id,
        cityId,
        startYear,
        endYear,
        targetCompanies,
        targetCountries,
        targetHubs,
        targetRides;
    public final long targetWorth;
    public final String title, era, objective;

    public HistoricalScenarioDef(
        int id,
        int cityId,
        int startYear,
        int endYear,
        String title,
        String era,
        String objective,
        long targetWorth,
        int targetCompanies,
        int targetCountries,
        int targetHubs,
        int targetRides) {
      this.id = id;
      this.cityId = cityId;
      this.startYear = startYear;
      this.endYear = endYear;
      this.title = title;
      this.era = era;
      this.objective = objective;
      this.targetWorth = targetWorth;
      this.targetCompanies = targetCompanies;
      this.targetCountries = targetCountries;
      this.targetHubs = targetHubs;
      this.targetRides = targetRides;
    }
  }

  public static final HistoricalScenarioDef[] HISTORICAL_SCENARIOS = {
    new HistoricalScenarioDef(
        0,
        0,
        2010,
        2020,
        "Silicon Plateau ₹5 Crore Rush",
        "Bengaluru 2010–2020",
        "Survive aggregator fare wars & reach ₹5 Crore (312,500 cr) Net Worth",
        312500L,
        1,
        1,
        6,
        45),
    new HistoricalScenarioDef(
        1,
        2,
        2008,
        2020,
        "The Four-Company Robber Baron",
        "New York 2008–2020",
        "CEO Takeover: Acquire all 3 rivals to own 4 companies",
        65000L,
        4,
        1,
        5,
        35),
    new HistoricalScenarioDef(
        2,
        0,
        2014,
        2024,
        "Two-Flag Indo-Pacific Bridge",
        "Bengaluru & Singapore 2014–2024",
        "Establish operations across 2 countries & complete 40 hauls",
        55000L,
        1,
        2,
        5,
        40),
    new HistoricalScenarioDef(
        3,
        1,
        2015,
        2025,
        "Global Four-Nation Syndicate",
        "London Global 2015–2025",
        "Establish international branches across 4 countries",
        80000L,
        2,
        4,
        6,
        50),
    new HistoricalScenarioDef(
        4,
        0,
        2020,
        2025,
        "2020 Lockdown Survival & Rescue",
        "Bengaluru 2020–2025",
        "Survive the 2020 lockdown shock, retire all bond debt & reach ₹2.5 Cr",
        156250L,
        1,
        1,
        5,
        30),
    new HistoricalScenarioDef(
        5,
        0,
        2011,
        2018,
        "Kempegowda Airport Express Link",
        "Bengaluru 2011–2018",
        "Connect MG Road to BLR Airport & haul 120 tons",
        48000L,
        1,
        1,
        6,
        45),
    new HistoricalScenarioDef(
        6,
        0,
        2014,
        2019,
        "2015 Indian Cab Consolidation Wave",
        "Bengaluru 2014–2019",
        "Execute buyouts to own 3 companies & operate 8 fleet units",
        60000L,
        3,
        1,
        5,
        40),
    new HistoricalScenarioDef(
        7,
        0,
        2021,
        2025,
        "2022 Namma Yatri Zero-Comm Uprising",
        "Bengaluru 2021–2025",
        "Run Zero-Commission subscription with 6 drivers & 4.92★ rating",
        52000L,
        1,
        1,
        5,
        45),
    new HistoricalScenarioDef(
        8,
        2,
        2007,
        2015,
        "Manhattan to JFK Golden Corridor",
        "New York 2007–2015",
        "Link Times Square to JFK Airport with Tier-3 corridor & 50 hauls",
        65000L,
        1,
        1,
        6,
        50),
    new HistoricalScenarioDef(
        9,
        1,
        2012,
        2018,
        "Thames Olympic Black Cab Charter",
        "London 2012–2018",
        "Open 6 London stations with 4.92★ British hospitality rating",
        55000L,
        1,
        1,
        6,
        45),
    new HistoricalScenarioDef(
        10,
        1,
        2017,
        2023,
        "2017 TfL Licensing & Safety Audit",
        "London 2017–2023",
        "Maintain 85%+ fleet condition, hire 3 managers & keep 4.93★ rating",
        58000L,
        1,
        1,
        5,
        40),
    new HistoricalScenarioDef(
        11,
        4,
        2016,
        2024,
        "Silicon Valley Autonomous Pioneer",
        "San Francisco 2016–2024",
        "Connect SOMA to SFO Airport with modern EV/Autonomous fleet",
        75000L,
        1,
        1,
        7,
        50),
    new HistoricalScenarioDef(
        12,
        4,
        2022,
        2025,
        "2023 CPUC 24/7 Robotaxi Permit Race",
        "San Francisco 2022–2025",
        "Operate late-era autonomous fleet with AI Director & reach $80k",
        80000L,
        2,
        1,
        6,
        50),
    new HistoricalScenarioDef(
        13,
        3,
        2015,
        2021,
        "2017 Tokyo JPN Taxi & Shinkansen Loop",
        "Tokyo 2015–2021",
        "Deliver 95%+ cabin cleanliness & 4.95★ Omotenashi reviews",
        70000L,
        1,
        1,
        7,
        50),
    new HistoricalScenarioDef(
        14,
        3,
        2019,
        2025,
        "2020 Tokyo GO App Unification",
        "Tokyo 2019–2025",
        "Unify Tokyo dispatch by owning 3 companies & 3 Lv2+ terminals",
        78000L,
        3,
        1,
        6,
        50),
    new HistoricalScenarioDef(
        15,
        5,
        2016,
        2022,
        "2018 Southeast Asia Super-App Merger",
        "Singapore 2016–2022",
        "Own 3 companies in Singapore & expand into a 2nd country",
        85000L,
        3,
        2,
        6,
        50),
    new HistoricalScenarioDef(
        16,
        2,
        2017,
        2021,
        "2019 Wall Street Mobility IPO Roadshow",
        "New York 2017–2021",
        "Achieve 0 debt, 40,000 cr revenue & $90,000 Net Worth for IPO",
        90000L,
        2,
        1,
        6,
        45),
    new HistoricalScenarioDef(
        17,
        1,
        2013,
        2019,
        "The Bankrupt Fleet Turnaround CEO",
        "London 2013–2019",
        "Start with 3 bonds (15,000 cr debt); retire all debt & hit £50k",
        50000L,
        1,
        1,
        5,
        35),
    new HistoricalScenarioDef(
        18,
        2,
        2023,
        2025,
        "2025 Manhattan Congestion Zone Express",
        "New York 2023–2025",
        "Haul 140 tons across Manhattan with 3 Lv3 Comfort vehicles",
        72000L,
        1,
        1,
        6,
        50),
    new HistoricalScenarioDef(
        19,
        0,
        2010,
        2025,
        "The ₹5 Crore Global Mobility Emperor",
        "Multi-Country 2010–2025",
        "Reach ₹5 Crore (312,500 cr), own 4 companies & establish in 3 countries",
        312500L,
        4,
        3,
        7,
        65)
  };

  public static final class Scenario {
    public final int id;
    public final String city, country, title, goalText, currencySymbol, currencyCode;
    public final int targetStations, targetRides;
    public final long targetWorth, baseFare, perKmRate;

    public Scenario(
        int id,
        String city,
        String country,
        String title,
        String goalText,
        int targetStations,
        int targetRides,
        long targetWorth,
        String currencySymbol,
        String currencyCode,
        long baseFare,
        long perKmRate) {
      this.id = id;
      this.city = city;
      this.country = country;
      this.title = title;
      this.goalText = goalText;
      this.targetStations = targetStations;
      this.targetRides = targetRides;
      this.targetWorth = targetWorth;
      this.currencySymbol = currencySymbol;
      this.currencyCode = currencyCode;
      this.baseFare = baseFare;
      this.perKmRate = perKmRate;
    }
  }

  public static final Scenario[] SCENARIOS = {
    new Scenario(
        0,
        "Bengaluru",
        "India",
        "Silicon Plateau Network",
        "Open 5 stations, complete 40 hauls, reach ₹35,000 net worth",
        5,
        40,
        35000L,
        "₹",
        "INR",
        50L,
        19L),
    new Scenario(
        1,
        "London",
        "United Kingdom",
        "Thames Crossrail Tycoon",
        "Bridge the Thames: open 5 stations, 50 hauls, £45,000 net worth",
        5,
        50,
        45000L,
        "£",
        "GBP",
        4L,
        2L),
    new Scenario(
        2,
        "New York",
        "United States",
        "Five Boroughs Express",
        "Connect Manhattan & boroughs: 6 stations, 60 hauls, $55,000 net worth",
        6,
        60,
        55000L,
        "$",
        "USD",
        3L,
        2L),
    new Scenario(
        3,
        "Tokyo",
        "Japan",
        "Bay Area Shinkansen Loop",
        "Master the Bay Loop: 6 stations, 70 hauls, ¥65,000 net worth",
        6,
        70,
        65000L,
        "¥",
        "JPY",
        500L,
        320L),
    new Scenario(
        4,
        "San Francisco",
        "United States",
        "Silicon Valley Robotaxi Grid",
        "Link Peninsula & Bay: 5 stations, 50 hauls, $48,000 net worth",
        5,
        50,
        48000L,
        "$",
        "USD",
        4L,
        2L),
    new Scenario(
        5,
        "Singapore",
        "Singapore",
        "Strait Autonomous Grid",
        "Port & Changi Express: 6 stations, 65 hauls, S$60,000 net worth",
        6,
        65,
        60000L,
        "S$",
        "SGD",
        4L,
        1L)
  };

  public static final class Driver implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public final int id;
    public final String name;
    public double skill = 4.6, fatigue = 0.0;
    public int level = 1, assignedTaxiId = 0;

    public Driver(int id, String name, double skill, int assignedTaxiId) {
      this.id = id;
      this.name = name;
      this.skill = skill;
      this.assignedTaxiId = assignedTaxiId;
    }
  }

  public static final class Manager implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public final int id;
    public final String role, name;
    public final long cost;
    public boolean hired = false;

    public Manager(int id, String role, String name, long cost) {
      this.id = id;
      this.role = role;
      this.name = name;
      this.cost = cost;
    }
  }

  public static final class Review implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public final int stars;
    public final String passenger, comment;

    public Review(int stars, String passenger, String comment) {
      this.stars = stars;
      this.passenger = passenger;
      this.comment = comment;
    }
  }

  public int scenarioId = 0;
  public int difficulty = DIFF_MEDIUM;
  public int gameMode = MODE_CITY;
  public int campaignStartYear = 2011, campaignEndYear = 2025;
  public final ArrayList<String> seenNews = new ArrayList<>();
  public final ArrayList<Report> reports = new ArrayList<>();
  public int chapter = 0;
  public int quickGoalType = GOAL_WORTH_5CR;
  public int historicalScenarioId = 0;
  public int tutorialStep = 0;
  public boolean scenarioWon = false;
  public String language = "en";
  public final ArrayList<District> districts = new ArrayList<>();
  public final ArrayList<Taxi> taxis = new ArrayList<>();
  public final ArrayList<Ride> requests = new ArrayList<>();
  public final ArrayList<Driver> drivers = new ArrayList<>();
  public final ArrayList<Manager> managers = new ArrayList<>();
  public final ArrayList<Review> reviews = new ArrayList<>();
  public final int[] corridorTier = new int[W * H];
  public long cash = 12000, revenue = 0, expenses = 0, rides = 0, missed = 0, cargoTons = 0;
  public double elapsed = 0,
      spawnClock = 0,
      costClock = 0,
      rating = 4.8,
      marketClock = 0,
      competitionClock = 0;
  public long marginDebt = 0, dividends = 0;
  public int historyStage = 0, marginCalls = 0, marketTick = 0;
  public int bonds = 0, restructurings = 0;
  public final ArrayList<Company> companies = new ArrayList<>();
  public int speed = 1, nextRide = 1, spawnSequence = 0;
  public boolean autoDispatch = false, milestone = false;
  public int businessModel = 0;
  public long grossFares = 0;
  public final String[] regions = {
    "Bengaluru", "London", "New York", "Tokyo", "San Francisco", "Singapore"
  };
  public final boolean[] branches = {true, false, false, false, false, false};
  public String message =
      "Build transit corridors, upgrade station facilities, and schedule multi-stop vehicle lines.";

  public static final class Report implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public String id, date, title, body, impact, url, source, category;
    public boolean read, urgent;

    public Report(
        String id,
        String date,
        String title,
        String body,
        String impact,
        String url,
        String source,
        String category) {
      this.id = id;
      this.date = date;
      this.title = title;
      this.body = body;
      this.impact = impact;
      this.url = url;
      this.source = source;
      this.category = category;
    }
  }

  public static final class District implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public final String name, landmark, specialty;
    public final int x, y, color;
    public boolean open;
    public int level = 1, facilities = 0;
    public final int[] supply = new int[] {2, 1, 0, 0};
    public final int[] demandMult = new int[] {120, 130, 140, 125};

    District(String n, String l, String s, int x, int y, int color, boolean open) {
      name = n;
      landmark = l;
      specialty = s;
      this.x = x;
      this.y = y;
      this.color = color;
      this.open = open;
    }

    public int cell() {
      return y * W + x;
    }

    public String levelName() {
      return level == 1 ? "Depot" : level == 2 ? "Station" : "Mega Terminal";
    }

    public boolean hasFacility(int flag) {
      return (facilities & flag) != 0;
    }
  }

  public static final class Ride implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public int id, from, to, kind;
    public long fare;
    public double created;

    public Ride(int id, int from, int to, long fare, double created) {
      this(id, from, to, fare, created, inferKind(from, to));
    }

    public Ride(int id, int from, int to, long fare, double created, int kind) {
      this.id = id;
      this.from = from;
      this.to = to;
      this.fare = fare;
      this.created = created;
      this.kind = kind;
    }

    public static int inferKind(int from, int to) {
      if (from == 2 || to == 2) return KIND_AIRPORT;
      if (from == 1 || to == 1) return KIND_EXECUTIVE;
      if (from == 3 || to == 3) return KIND_CARGO;
      return KIND_COMMUTER;
    }

    public String kindName() {
      return new String[] {"COMMUTERS", "EXECUTIVE", "AIRPORT", "PARCELS"}
          [Math.max(0, Math.min(3, kind))];
    }

    public int kindColor() {
      return new int[] {0xff73d6cf, 0xffffc566, 0xffb4a0e5, 0xffe89b7c}
          [Math.max(0, Math.min(3, kind))];
    }
  }

  public static final class Taxi implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public int id,
        cell,
        status = IDLE,
        at = 0,
        tier = 1,
        capacity = 2,
        waitPolicy = WAIT_ANY,
        comfortLevel = 1;
    public double progress = 0, charge = 0, condition = 100.0, cleanliness = 100.0, comfort = 80.0;
    public boolean broken = false, strictRoute = false;
    public int[] path;
    public Ride ride;
    public final ArrayList<Integer> routeStops = new ArrayList<>();
    public int routeIndex = 0;

    public Taxi(int id, int cell) {
      this.id = id;
      this.cell = cell;
      path = new int[] {cell};
      routeStops.add(0);
      routeStops.add(1);
    }

    public String tierName() {
      return tier == 1 ? "City compact" : tier == 2 ? "Hybrid shuttle" : "Electric express";
    }
  }

  public static final class Company implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public final String name;
    public final int district, color;
    public int price, owned = 0, fleet;
    public long cash = 6000, served = 0;
    public boolean merged = false;
    public final ArrayList<Integer> priceHistory = new ArrayList<>();
    public int cell, targetDistrict, at = 0;
    public double progress = 0;
    public int[] path;

    public Company(String name, int price, int fleet, int district, int color, int startCell) {
      this.name = name;
      this.price = price;
      this.fleet = fleet;
      this.district = district;
      this.color = color;
      this.cell = startCell;
      this.targetDistrict = (district + 1) % 6;
      this.path = new int[] {startCell};
      priceHistory.add(price);
    }

    public void recordPrice() {
      priceHistory.add(price);
      if (priceHistory.size() > 18) priceHistory.remove(0);
    }
  }

  public Game() {
    this(0);
  }

  public Game(int scenarioId) {
    loadScenario(scenarioId);
  }

  public Scenario currentScenario() {
    return SCENARIOS[Math.max(0, Math.min(SCENARIOS.length - 1, scenarioId))];
  }

  public HistoricalScenarioDef currentHistoricalScenario() {
    return HISTORICAL_SCENARIOS[
        Math.max(0, Math.min(HISTORICAL_SCENARIOS.length - 1, historicalScenarioId))];
  }

  public int ownedCompanyCount() {
    int n = 1;
    for (Company c : companies) if (c.merged) n++;
    return n;
  }

  public int establishedCountryCount() {
    HashSet<String> countries = new HashSet<>();
    for (int i = 0; i < branches.length; i++) if (branches[i]) countries.add(SCENARIOS[i].country);
    return countries.size();
  }

  public void startQuickPlay(int cityIdx, int goalType, int diffIdx) {
    this.gameMode = MODE_SOLO_QUICK;
    this.quickGoalType = Math.max(0, Math.min(QUICK_GOAL_NAMES.length - 1, goalType));
    setDifficulty(diffIdx);
    loadScenario(cityIdx);
    this.cash = Math.round(12000L * difficultyFareFactor());
    this.message =
        "Quick Play (" + currentScenario().city + "): " + QUICK_GOAL_NAMES[this.quickGoalType];
  }

  public void startHistoricalScenario(int histIdx) {
    this.gameMode = MODE_SOLO_SCENARIO;
    this.historicalScenarioId = Math.max(0, Math.min(HISTORICAL_SCENARIOS.length - 1, histIdx));
    HistoricalScenarioDef h = currentHistoricalScenario();
    loadScenario(h.cityId);
    campaignStartYear = h.startYear;
    campaignEndYear = h.endYear;
    if (h.id == 4) {
      this.bonds = 2;
    } else if (h.id == 17) {
      this.bonds = 3;
      this.cash = 9500;
    }
    this.message = "Scenario #" + (h.id + 1) + " [" + h.era + "]: " + h.objective;
  }

  public void startTutorial() {
    this.gameMode = MODE_TUTORIAL;
    this.tutorialStep = 1;
    setDifficulty(DIFF_EASIEST);
    loadScenario(0);
    this.cash = 18000;
    this.message = "Welcome to Fareforge Academy. Dispatch your first waiting trip.";
  }

  public boolean advanceTutorial() {
    if (this.gameMode != MODE_TUTORIAL) return false;
    if (!tutorialReady()) {
      message = "Complete the current Academy task before continuing.";
      return false;
    }
    if (this.tutorialStep < 8) {
      this.tutorialStep++;
      if (this.tutorialStep == 8) {
        this.cash += 5000;
        this.message = "Tutorial Graduated! +5,000 cr Academy Grant awarded.";
      } else {
        this.message = "Tutorial Step " + this.tutorialStep + "/8 unlocked.";
      }
      return true;
    }
    return false;
  }

  public void loadScenario(int id) {
    this.scenarioId = Math.max(0, Math.min(SCENARIOS.length - 1, id));
    this.scenarioWon = false;
    campaignStartYear = 2011;
    campaignEndYear = 2025;
    seenNews.clear();
    reports.clear();
    chapter = 0;
    districts.clear();
    taxis.clear();
    requests.clear();
    companies.clear();
    Arrays.fill(corridorTier, 0);
    cash = 12000;
    revenue = 0;
    expenses = 0;
    rides = 0;
    missed = 0;
    cargoTons = 0;
    elapsed = 0;
    spawnClock = 0;
    costClock = 0;
    rating = 4.8;
    marketClock = 0;
    competitionClock = 0;
    marginDebt = 0;
    dividends = 0;
    historyStage = 0;
    marginCalls = 0;
    marketTick = 0;
    bonds = 0;
    restructurings = 0;
    speed = 1;
    nextRide = 1;
    spawnSequence = 0;
    autoDispatch = false;
    milestone = false;
    businessModel = 0;
    grossFares = 0;
    Arrays.fill(branches, false);
    branches[scenarioId] = true;

    if (scenarioId == 1) {
      districts.add(
          new District("Westminster", "Central London", "Gov & PAX", 5, 4, 0xffffc566, true));
      districts.add(
          new District(
              "Canary Wharf",
              "Docklands Finance",
              "Financial VIP & Mail",
              15,
              4,
              0xff73d6cf,
              true));
      districts.add(
          new District(
              "Heathrow Gate", "West Air Hub", "Air Cargo & PAX", 25, 4, 0xffb4a0e5, false));
      districts.add(
          new District(
              "King's Cross",
              "North Rail Terminus",
              "Intercity Rail & Goods",
              5,
              16,
              0xffe89b7c,
              false));
      districts.add(
          new District(
              "Southwark Bank", "South Thames Hub", "Commuter PAX", 15, 16, 0xff93c5a1, false));
      districts.add(
          new District(
              "Greenwich Pier", "Thames East Port", "Maritime Freight", 25, 16, 0xff83bce4, false));
      companies.add(new Company("ThamesLine PLC", 55, 3, 3, 0xffe89b7c, districts.get(3).cell()));
      companies.add(
          new Company("Docklands Express", 70, 4, 2, 0xff83bce4, districts.get(2).cell()));
      companies.add(new Company("Albion Mobility", 45, 2, 5, 0xffb4a0e5, districts.get(5).cell()));
    } else if (scenarioId == 2) {
      districts.add(
          new District(
              "Midtown Core", "Manhattan Hub", "VIP & Mail Supply", 5, 4, 0xffffc566, true));
      districts.add(
          new District(
              "Wall Street", "Financial District", "Executive & Mail", 15, 4, 0xff73d6cf, true));
      districts.add(
          new District(
              "Queens JFK", "Airport Express", "Air Cargo & PAX", 25, 4, 0xffb4a0e5, false));
      districts.add(
          new District(
              "Jersey Terminal", "Hudson Freight", "Industrial Goods", 5, 16, 0xffe89b7c, false));
      districts.add(
          new District(
              "Brooklyn Navy", "East River Hub", "Commuter & Tech", 15, 16, 0xff93c5a1, false));
      districts.add(
          new District(
              "Bronx Concourse", "North Corridor", "Metro Commuters", 25, 16, 0xff83bce4, false));
      companies.add(
          new Company("Hudson Transit Corp", 55, 3, 3, 0xffe89b7c, districts.get(3).cell()));
      companies.add(new Company("Gotham Skyway", 70, 4, 2, 0xff83bce4, districts.get(2).cell()));
      companies.add(new Company("Tri-Borough Line", 45, 2, 5, 0xffb4a0e5, districts.get(5).cell()));
    } else if (scenarioId == 3) {
      districts.add(
          new District(
              "Shinjuku Core", "West Skyscraper Ward", "High-Volume PAX", 5, 4, 0xffffc566, true));
      districts.add(
          new District(
              "Akihabara Tech",
              "Electric Town",
              "Precision Tech Freight",
              15,
              4,
              0xff73d6cf,
              true));
      districts.add(
          new District(
              "Haneda Skyport", "Bay Airport", "Air Express & PAX", 25, 4, 0xffb4a0e5, false));
      districts.add(
          new District(
              "Shinagawa Rail",
              "Shinkansen Hub",
              "Bullet Freight & Mail",
              5,
              16,
              0xffe89b7c,
              false));
      districts.add(
          new District(
              "Shibuya Crossing",
              "Commercial Ward",
              "Retail & Commuters",
              15,
              16,
              0xff93c5a1,
              false));
      districts.add(
          new District(
              "Odaiba Waterfront",
              "Tokyo Bay Island",
              "Port & Expo Line",
              25,
              16,
              0xff83bce4,
              false));
      companies.add(new Company("Edokko Maglev", 55, 3, 3, 0xffe89b7c, districts.get(3).cell()));
      companies.add(new Company("Kanto Bay Line", 70, 4, 2, 0xff83bce4, districts.get(2).cell()));
      companies.add(new Company("Sakura Shuttle", 45, 2, 5, 0xffb4a0e5, districts.get(5).cell()));
    } else if (scenarioId == 4) {
      districts.add(
          new District("Market Street", "Downtown", "Commuters & parcels", 5, 4, 0xffffc566, true));
      districts.add(
          new District("SoMa", "Technology district", "Executive travel", 15, 4, 0xff73d6cf, true));
      districts.add(
          new District(
              "SFO Airport", "Peninsula gateway", "Airport transfers", 25, 4, 0xffb4a0e5, false));
      districts.add(
          new District(
              "Mission District",
              "Neighbourhood hub",
              "Local deliveries",
              5,
              16,
              0xffe89b7c,
              false));
      districts.add(
          new District(
              "Fisherman's Wharf",
              "Northern waterfront",
              "Visitor journeys",
              15,
              16,
              0xff93c5a1,
              false));
      districts.add(
          new District(
              "Bayview", "Southern district", "Industry & residents", 25, 16, 0xff83bce4, false));
      companies.add(
          new Company("Golden Gate Mobility", 55, 3, 3, 0xffe89b7c, districts.get(3).cell()));
      companies.add(new Company("Bayline Express", 70, 4, 2, 0xff83bce4, districts.get(2).cell()));
      companies.add(new Company("Pacific Loop", 45, 2, 5, 0xffb4a0e5, districts.get(5).cell()));
    } else if (scenarioId == 5) {
      districts.add(
          new District(
              "Marina Bay", "Financial Waterfront", "Executive & Mail", 5, 4, 0xffffc566, true));
      districts.add(
          new District(
              "One-North Biopolis",
              "Research Corridor",
              "Tech & Biotech",
              15,
              4,
              0xff73d6cf,
              true));
      districts.add(
          new District(
              "Changi Jewel", "East Air Terminal", "Global Air Cargo", 25, 4, 0xffb4a0e5, false));
      districts.add(
          new District(
              "Tuas Mega Port",
              "West Container Hub",
              "Maritime Containers",
              5,
              16,
              0xffe89b7c,
              false));
      districts.add(
          new District(
              "Orchard Central", "Retail Boulevard", "Commercial PAX", 15, 16, 0xff93c5a1, false));
      districts.add(
          new District(
              "Sentosa Causeway",
              "Southern Island",
              "Resort & Shuttle",
              25,
              16,
              0xff83bce4,
              false));
      companies.add(
          new Company("Merlion Autonomous", 55, 3, 3, 0xffe89b7c, districts.get(3).cell()));
      companies.add(new Company("Strait Skyway", 70, 4, 2, 0xff83bce4, districts.get(2).cell()));
      companies.add(
          new Company("Lion City Transit", 45, 2, 5, 0xffb4a0e5, districts.get(5).cell()));
    } else {
      districts.add(
          new District(
              "K.R. Market", "Central Bengaluru", "Commuters & parcels", 5, 4, 0xffffc566, true));
      districts.add(
          new District(
              "Indiranagar",
              "Eastern neighbourhood",
              "Executive journeys",
              15,
              4,
              0xff73d6cf,
              true));
      districts.add(
          new District(
              "Kempegowda Airport",
              "Airport corridor",
              "Airport journeys",
              25,
              4,
              0xffb4a0e5,
              false));
      districts.add(
          new District(
              "Majestic Station",
              "Railway interchange",
              "Bulk Retail Goods",
              5,
              16,
              0xffe89b7c,
              false));
      districts.add(
          new District(
              "Whitefield", "Technology corridor", "Daily commuters", 15, 16, 0xff93c5a1, false));
      districts.add(
          new District(
              "Koramangala",
              "Southern neighbourhood",
              "Local journeys",
              25,
              16,
              0xff83bce4,
              false));
      companies.add(
          new Company("Copperwheel Mobility", 55, 3, 3, 0xffe89b7c, districts.get(3).cell()));
      companies.add(
          new Company("Glasswing Transit", 70, 4, 2, 0xff83bce4, districts.get(2).cell()));
      companies.add(new Company("Violet Loop", 45, 2, 5, 0xffb4a0e5, districts.get(5).cell()));
    }

    int[] starter = route(districts.get(0).cell(), districts.get(1).cell());
    if (starter != null) for (int c : starter) corridorTier[c] = 1;
    taxis.add(new Taxi(1, districts.get(0).cell()));
    drivers.clear();
    managers.clear();
    reviews.clear();
    drivers.add(new Driver(1, "Captain #1", 4.8, 1));
    managers.add(new Manager(0, "Fleet Maintenance Chief", "Chief Engineer", 2200L));
    managers.add(new Manager(1, "Hygiene & Valet Supervisor", "Valet Lead", 1800L));
    managers.add(new Manager(2, "Driver HR & Training Director", "HR Director", 2500L));
    managers.add(new Manager(3, "AI Dispatch & Surge Director", "AI Director", 3200L));
    managers.add(new Manager(4, "Customer Relations & PR VP", "PR VP", 2800L));
    reviews.add(new Review(5, "Verified Rider", "Spotless cabin and smooth ride!"));
    for (int i = 0; i < companies.size(); i++)
      planRivalRoute(companies.get(i), (companies.get(i).district + i + 1) % districts.size());
    createRide(0, 1);
    createRide(1, 0);
    Scenario sc = currentScenario();
    message = sc.city + " Scenario (" + sc.title + "): " + sc.goalText + ".";
  }

  private void planRivalRoute(Company c, int nextDistrict) {
    c.targetDistrict = nextDistrict;
    int[] r = route(c.cell, districts.get(nextDistrict).cell());
    c.path = r != null ? r : new int[] {c.cell};
    c.at = 0;
    c.progress = 0;
  }

  public static boolean road(int x, int y) {
    return x % 5 == 0 || y % 4 == 0;
  }

  public static boolean arterial(int x, int y) {
    return x == 15 || y == 4 || y == 16;
  }

  public static boolean water(int x, int y) {
    return !road(x, y)
        && ((x >= 16 && x <= 19 && y >= 5 && y <= 7) || (x >= 6 && x <= 9 && y >= 13 && y <= 15));
  }

  public static boolean park(int x, int y) {
    return !road(x, y)
        && !water(x, y)
        && ((x >= 11 && x <= 14 && y >= 5 && y <= 7)
            || (x >= 21 && x <= 24 && y >= 9 && y <= 11)
            || (x * 13 + y * 7) % 19 == 0);
  }

  public long tileCorridorCost(int cell, int targetTier) {
    if (cell < 0 || cell >= W * H || targetTier <= corridorTier[cell] || targetTier > 3) return 0;
    int x = cell % W, y = cell / W;
    long base = arterial(x, y) ? 140L : 100L;
    return base * (targetTier - corridorTier[cell]);
  }

  public long corridorLinkCost(int fromDist, int toDist, int targetTier) {
    if (fromDist < 0
        || toDist < 0
        || fromDist >= districts.size()
        || toDist >= districts.size()
        || fromDist == toDist) return 0;
    int[] p = route(districts.get(fromDist).cell(), districts.get(toDist).cell());
    if (p == null) return 0;
    long sum = 0;
    for (int c : p) sum += tileCorridorCost(c, targetTier);
    return sum;
  }

  public boolean buildCorridor(int fromDist, int toDist, int targetTier) {
    long cost = corridorLinkCost(fromDist, toDist, targetTier);
    if (cost <= 0) {
      message = "Corridor already at Tier " + targetTier + ".";
      return false;
    }
    if (cash < cost) {
      message = "Need " + money(cost) + " to build Tier " + targetTier + " corridor.";
      return false;
    }
    int[] p = route(districts.get(fromDist).cell(), districts.get(toDist).cell());
    spend(cost);
    for (int c : p) if (corridorTier[c] < targetTier) corridorTier[c] = targetTier;
    message =
        "Built Tier "
            + targetTier
            + " corridor between "
            + districts.get(fromDist).name
            + " and "
            + districts.get(toDist).name
            + ".";
    return true;
  }

  public boolean isCorridorLinked(int fromDist, int toDist) {
    int[] p = route(districts.get(fromDist).cell(), districts.get(toDist).cell());
    if (p == null) return false;
    for (int c : p) if (corridorTier[c] <= 0) return false;
    return true;
  }

  public int corridorCount() {
    int n = 0;
    for (int t : corridorTier) if (t > 0) n++;
    return n;
  }

  public long facilityPrice(int flag) {
    if (flag == FAC_MAINT) return 1400L;
    if (flag == FAC_LOUNGE) return 1800L;
    if (flag == FAC_CARGO) return 1600L;
    return 2000L;
  }

  public boolean buildFacility(int distIndex, int flag) {
    if (distIndex < 0 || distIndex >= districts.size()) return false;
    District d = districts.get(distIndex);
    if (!d.open) {
      message = "Open " + d.name + " before adding station facilities.";
      return false;
    }
    if (d.hasFacility(flag)) {
      message = "Facility already installed at " + d.name + ".";
      return false;
    }
    long cost = facilityPrice(flag);
    if (cash < cost) {
      message = "Need " + money(cost) + " to install station facility.";
      return false;
    }
    spend(cost);
    d.facilities |= flag;
    message = "Installed station facility at " + d.name + ".";
    return true;
  }

  public int openCount() {
    int n = 0;
    for (District d : districts) if (d.open) n++;
    return n;
  }

  public int totalHubLevels() {
    int n = 0;
    for (District d : districts) if (d.open) n += (d.level - 1);
    return n;
  }

  public int idleCount() {
    int n = 0;
    for (Taxi t : taxis) if (t.status == IDLE) n++;
    return n;
  }

  public int branchCount() {
    int n = 0;
    for (boolean b : branches) if (b) n++;
    return n;
  }

  public long branchPrice(int i) {
    return 5000 + i * 1500L;
  }

  public boolean expand(int i) {
    if (i < 0 || i >= branches.length || branches[i]) return false;
    if (openCount() < 3 || rides < 20) {
      message = "Prove the model first: serve 3 districts and complete 20 rides.";
      return false;
    }
    if (cash < branchPrice(i)) {
      message = "Need " + money(branchPrice(i)) + " for this city launch.";
      return false;
    }
    spend(branchPrice(i));
    branches[i] = true;
    message = "Launched " + regions[i] + ". Global operations now contribute each quarter.";
    return true;
  }

  public String modelName() {
    return new String[] {"Commission", "Corporate commute", "Driver subscription"}[businessModel];
  }

  public boolean setModel(int value) {
    if (value < 0 || value > 2) return false;
    if (value == 1 && year() < 2015 || value == 2 && year() < 2022) {
      message = "This model is not available yet. See HISTORY for the timeline.";
      return false;
    }
    businessModel = value;
    message = "Business model changed to " + modelName() + ".";
    return true;
  }

  public int trafficPhase() {
    return ((int) (elapsed / 45)) % 3;
  }

  public boolean rushHour() {
    return trafficPhase() == 1;
  }

  public String trafficName() {
    return rushHour() ? "RUSH HOUR" : "CLEAR STREETS";
  }

  public double trafficFactor() {
    return rushHour() ? .65 : 1;
  }

  public int year() {
    return Math.min(campaignEndYear, campaignStartYear + (int) (elapsed / 60));
  }

  public double demandFactor() {
    double historical = year() == 2020 ? .22 : year() == 2021 ? .65 : year() >= 2015 ? 1.15 : 1;
    return historical
        * (businessModel == 1 ? (rushHour() ? 1.6 : .72) : businessModel == 2 ? 1.25 : 1);
  }

  public boolean bankHoliday() {
    return false;
  }

  public long taxiPrice() {
    return Math.round((2500L + (taxis.size() - 1) * 450L) * (year() >= 2015 ? .85 : 1));
  }

  public long districtPrice(int i) {
    return 3500L + i * 600L;
  }

  public long districtUpgradePrice(int i) {
    if (i < 0 || i >= districts.size()) return 0;
    return 2600L * districts.get(i).level + i * 300L;
  }

  public boolean upgradeDistrict(int i) {
    if (i < 0 || i >= districts.size()) return false;
    District d = districts.get(i);
    if (!d.open) {
      message = "Open " + d.name + " before upgrading its station.";
      return false;
    }
    if (d.level >= 3) {
      message = d.name + " is already a Level 3 Mega Terminal.";
      return false;
    }
    long cost = districtUpgradePrice(i);
    if (cash < cost) {
      message = "Need " + money(cost) + " to upgrade " + d.name + " station.";
      return false;
    }
    spend(cost);
    d.level++;
    message =
        "Upgraded "
            + d.name
            + " to Lv"
            + d.level
            + " "
            + d.levelName()
            + " (+15% fares, faster turnaround).";
    return true;
  }

  public Taxi nextUpgradeableTaxi() {
    Taxi pick = null;
    for (Taxi t : taxis) if (t.tier < 3 && (pick == null || t.tier < pick.tier)) pick = t;
    return pick;
  }

  public long taxiUpgradePrice() {
    Taxi t = nextUpgradeableTaxi();
    return t == null ? 0 : 1600L * t.tier;
  }

  public boolean upgradeTaxi() {
    Taxi t = nextUpgradeableTaxi();
    if (t == null) {
      message = "All consists in your current fleet are already Tier 3 Maglev EVs.";
      return false;
    }
    long cost = taxiUpgradePrice();
    if (cash < cost) {
      message = "Need " + money(cost) + " to upgrade Consist " + t.id + ".";
      return false;
    }
    spend(cost);
    t.tier++;
    t.capacity = 1 + t.tier * 2;
    t.condition = 100.0;
    t.broken = false;
    message =
        "Consist "
            + t.id
            + " upgraded to "
            + t.tierName()
            + " (Cap "
            + t.capacity
            + ", faster speed & bonus fare).";
    return true;
  }

  public int[] route(int start, int goal) {
    if (start < 0 || goal < 0 || start >= W * H || goal >= W * H) return null;
    int[] prev = new int[W * H];
    Arrays.fill(prev, -2);
    prev[start] = -1;
    ArrayDeque<Integer> q = new ArrayDeque<>();
    q.add(start);
    while (!q.isEmpty()) {
      int a = q.remove();
      if (a == goal) break;
      int x = a % W, y = a / W;
      for (int b :
          new int[] {
            x > 0 ? a - 1 : -1, x < W - 1 ? a + 1 : -1, y > 0 ? a - W : -1, y < H - 1 ? a + W : -1
          }) {
        if (b < 0 || !road(b % W, b / W) || prev[b] != -2) continue;
        prev[b] = a;
        q.add(b);
      }
    }
    if (prev[goal] == -2) return null;
    ArrayList<Integer> cells = new ArrayList<>();
    for (int a = goal; a != -1; a = prev[a]) cells.add(a);
    Collections.reverse(cells);
    int[] out = new int[cells.size()];
    for (int i = 0; i < out.length; i++) out[i] = cells.get(i);
    return out;
  }

  public void createRide(int from, int to) {
    if (from == to || !districts.get(from).open || !districts.get(to).open || requests.size() >= 6)
      return;
    int distance = route(districts.get(from).cell(), districts.get(to).cell()).length - 1;
    int kind = Ride.inferKind(from, to);
    District orig = districts.get(from), dest = districts.get(to);
    double hubBonus = 1.0 + (orig.level - 1) * 0.15 + (dest.level - 1) * 0.10;
    double facBonus =
        (orig.hasFacility(FAC_LOUNGE) && kind <= 1 ? 0.25 : 0)
            + (orig.hasFacility(FAC_CARGO) && kind >= 2 ? 0.25 : 0);
    double demandBonus = dest.demandMult[kind] / 120.0;
    double kindMult =
        kind == KIND_AIRPORT ? 1.20 : kind == KIND_EXECUTIVE && rushHour() ? 1.12 : 1.0;
    long fare =
        Math.round(
            (90 + distance * 15 + to * 12)
                * (rushHour() ? 1.25 : 1)
                * (hubBonus + facBonus)
                * kindMult
                * demandBonus
                * difficultyFareFactor());
    orig.supply[kind] = Math.min(12, orig.supply[kind] + 1);
    requests.add(new Ride(nextRide++, from, to, fare, elapsed, kind));
  }

  private void generateRide() {
    ArrayList<Integer> open = new ArrayList<>();
    for (int i = 0; i < districts.size(); i++) if (districts.get(i).open) open.add(i);
    int seq = spawnSequence++;
    int a = open.get((seq * 7 + seq / 3) % open.size());
    int bi = (open.indexOf(a) + 1 + (seq / open.size()) % (open.size() - 1)) % open.size();
    createRide(a, open.get(bi));
  }

  private void spend(long n) {
    cash -= n;
    expenses += n;
  }

  public boolean buyTaxi() {
    if (taxis.size() >= MAX_FLEET) {
      message = "Your 12-consist fleet is complete.";
      return false;
    }
    long price = taxiPrice();
    if (cash < price) {
      message = "Need " + money(price) + " to purchase the next vehicle consist.";
      return false;
    }
    spend(price);
    Taxi vehicle = new Taxi(taxis.size() + 1, districts.get(0).cell());
    taxis.add(vehicle);
    for (Driver driver : drivers)
      if (driver.assignedTaxiId == 0) {
        driver.assignedTaxiId = vehicle.id;
        break;
      }
    message = "New vehicle ready at " + districts.get(0).name + ".";
    return true;
  }

  public boolean unlock(int i) {
    if (i < 0 || i >= districts.size()) return false;
    District d = districts.get(i);
    if (d.open) {
      message = d.name + " is already in your network.";
      return false;
    }
    long cost = districtPrice(i);
    if (cash < cost) {
      message = "Need " + money(cost) + " to open " + d.name + ".";
      return false;
    }
    spend(cost);
    d.open = true;
    int[] p = route(districts.get(0).cell(), d.cell());
    if (p != null) for (int c : p) if (corridorTier[c] == 0) corridorTier[c] = 1;
    createRide(i, 0);
    message = "Opened " + d.name + " Station and linked corridor.";
    return true;
  }

  public boolean dispatch(int rideId) {
    Ride r = null;
    for (Ride v : requests)
      if (v.id == rideId) {
        r = v;
        break;
      }
    if (r == null) return false;
    Taxi nearest = null;
    int[] best = null;
    for (Taxi t : taxis)
      if (t.status == IDLE) {
        int[] path = route(t.cell, districts.get(r.from).cell());
        if (path != null && (best == null || path.length < best.length)) {
          nearest = t;
          best = path;
        }
      }
    if (nearest == null) {
      message = "All consists are busy. Buy another or wait for arrival.";
      return false;
    }
    return assignRide(r, nearest, best);
  }

  private boolean assignRide(Ride r, Taxi vehicle, int[] pickupPath) {
    if (vehicle == null || vehicle.status != IDLE || pickupPath == null) return false;
    Taxi nearest = vehicle;
    int[] best = pickupPath;
    nearest.ride = r;
    nearest.path = best;
    nearest.at = 0;
    nearest.progress = 0;
    nearest.status = PICKUP;
    requests.remove(r);
    District orig = districts.get(r.from);
    if (orig.supply[r.kind] > 0) orig.supply[r.kind]--;
    message = "Consist " + nearest.id + " heading to " + orig.name + " (" + r.kindName() + ").";
    if (best.length == 1) arrive(nearest);
    return true;
  }

  private void arrive(Taxi t) {
    if (t.status == PICKUP) {
      District orig = districts.get(t.ride.from);
      if (orig.hasFacility(FAC_MAINT)) {
        t.condition = 100.0;
        t.broken = false;
      }
      if (elapsed - t.ride.created > 55) rating = Math.max(1, rating - .08);
      t.status = RIDING;
      t.path = route(t.cell, districts.get(t.ride.to).cell());
      t.at = 0;
      t.progress = 0;
    } else if (t.status == RIDING) {
      District dest = districts.get(t.ride.to);
      if (dest.hasFacility(FAC_MAINT)) {
        t.condition = 100.0;
        t.broken = false;
      }
      t.cleanliness = Math.max(20.0, t.cleanliness - 8.0);
      if (managers.size() > 1 && managers.get(1).hired)
        t.cleanliness = Math.min(100.0, t.cleanliness + 12.0);
      if (managers.size() > 0 && managers.get(0).hired && t.condition < 70.0) {
        t.condition = 100.0;
        t.broken = false;
      }
      int destHubLevel = dest.level;
      long tierTip = (t.tier - 1) * 12L + (t.comfortLevel - 1) * 8L;
      Driver partner = driverFor(t);
      double serviceFactor =
          partner == null ? 1 : 1 + (partner.level - 1) * .03 - (partner.fatigue >= 85 ? .1 : 0);
      long income =
          businessModel == 2
              ? tierTip
              : Math.round(t.ride.fare * (businessModel == 1 ? .40 : .32) * serviceFactor)
                  + tierTip;
      grossFares += t.ride.fare;
      cash += income;
      revenue += income;
      rides++;
      cargoTons += t.capacity;
      int k = t.ride.kind;
      dest.demandMult[k] = Math.max(85, dest.demandMult[k] - 6);
      for (int other = 0; other < 4; other++)
        if (other != k) dest.demandMult[other] = Math.min(175, dest.demandMult[other] + 2);
      int stars = t.cleanliness >= 75 && t.condition >= 70 ? 5 : (t.cleanliness >= 50 ? 4 : 3);
      if (partner != null) {
        if (partner.fatigue >= 85) stars = Math.max(2, stars - 1);
        partner.fatigue = Math.min(100, partner.fatigue + (managers.get(2).hired ? 3 : 6));
      }
      reviews.add(
          0,
          new Review(
              stars,
              "Passenger #" + rides,
              stars == 5
                  ? "Clean cabin, smooth driver & great comfort!"
                  : "Please improve cabin cleanliness & maintenance."));
      if (reviews.size() > 12) reviews.remove(reviews.size() - 1);
      double reviewChange = stars == 5 ? .015 : stars == 4 ? -.015 : -.045;
      if (reviewChange < 0 && managers.get(4).hired) reviewChange *= .5;
      rating = Math.max(1, Math.min(5, rating + reviewChange));
      message =
          "Consist "
              + t.id
              + " delivered "
              + t.ride.kindName()
              + " to "
              + dest.name
              + ". Revenue: "
              + money(income)
              + ".";
      t.ride = null;
      t.status = CHARGING;
      double chargerCut = dest.hasFacility(FAC_CHARGER) ? 0.7 : 0.0;
      t.charge = Math.max(0.5, 3.0 - (destHubLevel - 1) * 0.7 - (t.tier - 1) * 0.4 - chargerCut);
      t.path = new int[] {t.cell};
      t.at = 0;
      t.progress = 0;
    }
  }

  public boolean serviceTaxi(int idx) {
    if (idx < 0 || idx >= taxis.size()) return false;
    long cost = managers.size() > 0 && managers.get(0).hired ? 220L : 300L;
    if (cash < cost) return false;
    spend(cost);
    Taxi t = taxis.get(idx);
    t.condition = 100.0;
    t.broken = false;
    message = "Overhauled Consist " + t.id + " mechanical condition to 100%.";
    return true;
  }

  public boolean cleanTaxi(int idx) {
    if (idx < 0 || idx >= taxis.size()) return false;
    long cost = 120L;
    if (cash < cost) return false;
    spend(cost);
    Taxi t = taxis.get(idx);
    t.cleanliness = 100.0;
    message = "Deep-cleaned & sanitized Consist " + t.id + " interior to 100%.";
    return true;
  }

  public boolean upgradeComfort(int idx) {
    if (idx < 0 || idx >= taxis.size()) return false;
    Taxi t = taxis.get(idx);
    if (t.comfortLevel >= 3) return false;
    long cost = 600L * t.comfortLevel;
    if (cash < cost) return false;
    spend(cost);
    t.comfortLevel++;
    t.comfort = Math.min(100.0, 65.0 + t.comfortLevel * 15.0);
    message = "Upgraded Consist " + t.id + " cabin comfort to Lv" + t.comfortLevel + ".";
    return true;
  }

  public boolean recruitDriver(String name) {
    if (drivers.size() >= MAX_FLEET) return false;
    long cost = 450L;
    if (cash < cost) return false;
    spend(cost);
    int id = drivers.size() + 1;
    HashSet<Integer> assigned = new HashSet<>();
    for (Driver driver : drivers) assigned.add(driver.assignedTaxiId);
    int vehicle = 0;
    for (Taxi taxi : taxis)
      if (!assigned.contains(taxi.id)) {
        vehicle = taxi.id;
        break;
      }
    drivers.add(new Driver(id, name != null ? name : ("Driver #" + id), 4.7, vehicle));
    message = "Recruited " + drivers.get(drivers.size() - 1).name + ".";
    return true;
  }

  public boolean trainDriver(int idx) {
    if (idx < 0 || idx >= drivers.size()) return false;
    Driver d = drivers.get(idx);
    if (d.level >= 3) return false;
    long cost = 350L * d.level;
    if (cash < cost) return false;
    spend(cost);
    d.level++;
    d.skill = Math.min(5.0, d.skill + 0.12);
    message =
        "Trained "
            + d.name
            + " to Lv"
            + d.level
            + " (Skill "
            + String.format(Locale.US, "%.2f", d.skill)
            + ").";
    return true;
  }

  public boolean restDriver(int idx) {
    if (idx < 0 || idx >= drivers.size()) return false;
    Driver d = drivers.get(idx);
    d.fatigue = 0.0;
    message = d.name + " rested and refreshed (0% fatigue).";
    return true;
  }

  public boolean hireManager(int idx) {
    if (idx < 0 || idx >= managers.size()) return false;
    Manager m = managers.get(idx);
    if (m.hired || cash < m.cost) return false;
    spend(m.cost);
    m.hired = true;
    message = "Recruited Executive Manager: " + m.role + " (" + m.name + ").";
    return true;
  }

  public boolean setLanguage(String lang) {
    if (lang == null || lang.isEmpty()) return false;
    this.language = lang;
    return true;
  }

  public boolean setDifficulty(int diff) {
    if (diff < DIFF_EASIEST || diff > DIFF_PROFESSIONAL) return false;
    this.difficulty = diff;
    message = "Difficulty mode set to " + difficultyName() + ".";
    return true;
  }

  public String difficultyName() {
    return DIFFICULTY_NAMES[Math.max(0, Math.min(DIFFICULTY_NAMES.length - 1, difficulty))];
  }

  public double difficultyFareFactor() {
    return new double[] {1.35, 1.15, 1.0, 0.90, 0.82}[Math.max(0, Math.min(4, difficulty))];
  }

  public double difficultyWearFactor() {
    return new double[] {0.40, 0.65, 1.0, 1.35, 1.70}[Math.max(0, Math.min(4, difficulty))];
  }

  public String formatCityMoney(long n) {
    Scenario sc = currentScenario();
    return String.format(Locale.US, "%s%,d", sc.currencySymbol, n);
  }

  public long bondDebt() {
    return bonds * BOND_FACE;
  }

  public boolean issueBond() {
    if (bonds >= MAX_BONDS) {
      message = "Bond limit reached (max 3 bonds / 15,000 cr).";
      return false;
    }
    if (bankHoliday()) {
      message = "Bank holiday: bond issuance is temporarily suspended.";
      return false;
    }
    bonds++;
    cash += BOND_FACE;
    message = "Issued " + money(BOND_FACE) + " corporate bond (175 cr quarterly interest).";
    return true;
  }

  public boolean repayBond() {
    if (bonds <= 0) {
      message = "No outstanding corporate bonds to retire.";
      return false;
    }
    if (cash < BOND_FACE) {
      message = "Need " + money(BOND_FACE) + " cash to retire a corporate bond.";
      return false;
    }
    cash -= BOND_FACE;
    bonds--;
    message = "Retired " + money(BOND_FACE) + " corporate bond. Fixed interest reduced.";
    return true;
  }

  public void checkSolvency() {
    if (cash >= -2500) return;
    restructurings++;
    rating = Math.max(1.0, rating - 0.15);
    if (bonds < MAX_BONDS) {
      bonds++;
      cash += 4500;
      expenses += 500;
      message = "Insolvency restructuring: emergency bond issued at 10% discount to cover deficit.";
    } else if (taxis.size() > 1) {
      taxis.remove(taxis.size() - 1);
      cash += 1800;
      message =
          "Receivership restructuring: 1 consist auctioned to creditors to restore liquidity.";
    } else {
      cash = -500;
      message = "Receivership restructuring: creditors reorganized operating deficit.";
    }
  }

  public long portfolioValue() {
    long n = 0;
    for (Company c : companies) if (!c.merged) n += (long) c.price * c.owned;
    return n;
  }

  public long netWorth() {
    return cash
        + portfolioValue()
        - marginDebt
        - bondDebt()
        + taxis.size() * 1500L
        + openCount() * 2000L
        + totalHubLevels() * 1800L
        + (branchCount() - 1) * 4000L;
  }

  public double equityRatio() {
    long v = portfolioValue();
    return v == 0 ? 1 : (double) (v - marginDebt) / v;
  }

  public boolean buyShares(int index, boolean margin) {
    Company c = companies.get(index);
    if (c.merged || c.owned >= 100) {
      message = "No shares available.";
      return false;
    }
    if (bankHoliday()) {
      message = "Bank holiday: share dealing is temporarily suspended.";
      return false;
    }
    long full = c.price * 10L, borrow = margin ? full / 2 : 0, cost = full - borrow;
    if (cash < cost) {
      message = "Insufficient company cash for this purchase.";
      return false;
    }
    if (margin
        && (double) (portfolioValue() + full - marginDebt - borrow) / (portfolioValue() + full)
            < .5) {
      message = "Reduce margin debt before increasing leverage.";
      return false;
    }
    cash -= cost;
    marginDebt += borrow;
    c.owned += 10;
    message =
        "Bought 10 shares of " + c.name + (margin ? " using 50% borrowed funds." : " for cash.");
    return true;
  }

  public boolean sellShares(int index) {
    Company c = companies.get(index);
    if (c.merged || c.owned < 10) {
      message = "No shares available to sell.";
      return false;
    }
    if (bankHoliday()) {
      message = "Bank holiday: share dealing is temporarily suspended.";
      return false;
    }
    long proceeds = c.price * 10L, repay = Math.min(marginDebt, proceeds);
    marginDebt -= repay;
    cash += proceeds - repay;
    c.owned -= 10;
    message = "Sold 10 shares. Proceeds repay margin debt first.";
    return true;
  }

  public void repayMargin() {
    long n = Math.min(Math.max(0, cash), marginDebt);
    cash -= n;
    marginDebt -= n;
    message =
        n > 0 ? "Repaid " + money(n) + " of margin debt." : "No cash or margin debt to repay.";
  }

  public long mergerPrice(int i) {
    Company c = companies.get(i);
    return (long) Math.ceil((100 - c.owned) * c.price * 1.15) + 1200;
  }

  public boolean merge(int i) {
    Company c = companies.get(i);
    if (c.merged || c.owned < 60) {
      message = "Acquire at least 60% of the voting shares first.";
      return false;
    }
    if (bankHoliday()) {
      message = "The bank holiday suspends merger financing.";
      return false;
    }
    if (marginDebt > 0) {
      message = "Repay all margin debt before completing a merger.";
      return false;
    }
    long cost = mergerPrice(i);
    if (cash < cost) {
      message = "Need " + money(cost) + " for the remaining shares and integration.";
      return false;
    }
    cash -= cost;
    int inherited = Math.min(c.fleet, MAX_FLEET - taxis.size());
    for (int n = 0; n < inherited; n++)
      taxis.add(new Taxi(taxis.size() + 1, districts.get(c.district).cell()));
    cash += c.cash + (c.fleet - inherited) * 1500L;
    districts.get(c.district).open = true;
    int[] p = route(districts.get(0).cell(), districts.get(c.district).cell());
    if (p != null) for (int cell : p) if (corridorTier[cell] == 0) corridorTier[cell] = 1;
    c.owned = 0;
    c.merged = true;
    message = "Merged " + c.name + ": fleet, corridors, treasury and station integrated.";
    return true;
  }

  public void maintainMargin() {
    long v = portfolioValue();
    if (marginDebt == 0) return;
    if (v == 0 || equityRatio() < .30) {
      cash += v - marginDebt;
      marginDebt = 0;
      for (Company c : companies) c.owned = 0;
      marginCalls++;
      message =
          "Margin call: broker liquidated the portfolio. Any shortfall is charged to company cash.";
    }
  }

  private void history() {
    if (historyStage == 0 && year() >= 2013) {
      historyStage = 1;
      message =
          "2013: international competition enters "
              + currentScenario().city
              + ". Upgrade corridors and station facilities.";
    }
    if (historyStage == 1 && year() >= 2015) {
      historyStage = 2;
      message =
          "2015: consolidation and commuter lines reshape transit. Vehicle procurement is now 15%"
              + " cheaper.";
    }
    if (historyStage == 2 && year() >= 2020) {
      historyStage = 3;
      for (Company c : companies)
        if (!c.merged) {
          c.price = Math.max(5, (int) Math.round(c.price * .48));
          c.recordPrice();
        }
      message =
          "2020: lockdown disrupts travel. Scenario demand drops; fictional share prices fall 52%."
              + " Check your margin.";
      maintainMargin();
    }
    if (historyStage == 3 && year() >= 2022) {
      historyStage = 4;
      message = "2022: autonomous & zero-commission pass networks unlock.";
    }
  }

  private void market() {
    marketTick++;
    long regional = Math.round((branchCount() - 1) * 220 * demandFactor());
    cash += regional;
    revenue += regional;
    if (businessModel == 2) {
      long subscription = taxis.size() * 80L;
      cash += subscription;
      revenue += subscription;
    }
    for (District d : districts)
      if (d.open) {
        for (int k = 0; k < 4; k++) d.demandMult[k] = Math.min(180, d.demandMult[k] + 3);
      }
    for (int i = 0; i < companies.size(); i++) {
      Company c = companies.get(i);
      if (c.merged) continue;
      double wave = Math.sin((marketTick + i * 3) * 1.7) * .035;
      double trend = year() == 2020 ? -.012 : .014;
      double fundamental = (c.cash + c.fleet * 1800.0) / 100;
      double valuePull = Math.max(-.025, Math.min(.025, (fundamental / c.price - 1) * .04));
      c.price = Math.max(5, (int) Math.round(c.price * (1 + trend + wave + valuePull)));
      c.recordPrice();
      long dividend = c.owned * (c.cash > 2500 ? 1 : 0);
      cash += dividend;
      dividends += dividend;
      c.cash -= dividend;
    }
    long interest = (marginDebt + 49) / 50 + bonds * 175L;
    cash -= interest;
    expenses += interest;
    maintainMargin();
    checkSolvency();
  }

  public void tick(double seconds) {
    if (speed == 0 || seconds <= 0 || !Double.isFinite(seconds)) return;
    double dt = Math.min(.25, seconds) * speed;
    elapsed += dt;
    spawnClock += dt;
    costClock += dt;
    marketClock += dt;
    competitionClock += dt;
    history();
    double interval = Math.max(2.5, 9 - openCount() - totalHubLevels() * 0.25) / demandFactor();
    while (spawnClock >= interval) {
      spawnClock -= interval;
      generateRide();
    }
    while (marketClock >= 15) {
      marketClock -= 15;
      market();
    }
    while (competitionClock >= 16) {
      competitionClock -= 16;
      for (Company c : companies)
        if (!c.merged) {
          c.cash += c.fleet * 18;
          c.served++;
          if (!requests.isEmpty()) {
            requests.remove(0);
            message = c.name + " hauled a waiting load. Expand corridors or acquire the rival.";
          }
        }
    }
    while (costClock >= 1) {
      costClock--;
      spend(taxis.size() * 2L);
    }
    for (Iterator<Ride> it = requests.iterator(); it.hasNext(); ) {
      Ride r = it.next();
      if (elapsed - r.created > 65) {
        it.remove();
        missed++;
        rating = Math.max(1, rating - .04);
      }
    }
    for (Taxi t : taxis) {
      if (t.status == CHARGING) {
        t.charge -= dt;
        if (t.charge <= 0) {
          t.status = IDLE;
          t.charge = 0;
        }
        continue;
      }
      if (t.status != PICKUP && t.status != RIDING) continue;
      t.condition =
          Math.max(10.0, t.condition - dt * (0.22 - t.tier * 0.04) * difficultyWearFactor());
      t.broken = t.condition < 25.0;
      int trackTier = Math.max(1, corridorTier[t.cell]);
      double trackBoost = trackTier == 3 ? 1.55 : trackTier == 2 ? 1.25 : 1.0;
      Driver partner = driverFor(t);
      double partnerBoost =
          partner == null ? 1 : 1 + (partner.level - 1) * .025 - (partner.fatigue >= 85 ? .1 : 0);
      double speedBoost =
          (1.0 + (t.tier - 1) * 0.22)
              * trackBoost
              * (t.broken ? 0.38 : 1.0)
              * partnerBoost
              * (managers.get(3).hired ? 1.1 : 1);
      t.progress += dt * 2.4 * trafficFactor() * speedBoost;
      while (t.progress >= 1 && (t.status == PICKUP || t.status == RIDING)) {
        t.progress--;
        t.at++;
        t.cell = t.path[t.at];
        if (t.at == t.path.length - 1) {
          arrive(t);
          break;
        }
      }
    }
    for (int i = 0; i < companies.size(); i++) {
      Company c = companies.get(i);
      if (c.merged || c.path == null) continue;
      c.progress += dt * 1.85 * trafficFactor();
      while (c.progress >= 1) {
        c.progress--;
        if (c.at < c.path.length - 1) {
          c.at++;
          c.cell = c.path[c.at];
        } else {
          int next = (c.targetDistrict + 1 + i) % districts.size();
          if (!requests.isEmpty()) next = requests.get(0).from;
          if (districts.get(next).cell() == c.cell) next = (next + 1) % districts.size();
          planRivalRoute(c, next);
          break;
        }
      }
    }
    if (autoDispatch) {
      for (Taxi vehicle : taxis)
        if (vehicle.status == IDLE) {
          Ride selected = null;
          for (Ride request : requests)
            if (vehicle.routeStops.contains(request.from)
                && vehicle.routeStops.contains(request.to)) {
              selected = request;
              break;
            }
          if (selected == null && !vehicle.strictRoute && !requests.isEmpty())
            selected = requests.get(0);
          if (selected != null)
            assignRide(selected, vehicle, route(vehicle.cell, districts.get(selected.from).cell()));
        }
    }
    Scenario sc = currentScenario();
    if (!scenarioWon && objectiveComplete()) {
      scenarioWon = true;
      cash += 8000;
      message =
          "Gold Medal in " + sc.city + " (" + sc.title + ")! +8,000 cr scenario bonus awarded.";
    }
    if (!milestone && branchCount() == 6 && taxis.size() >= 10 && rides >= 100) {
      milestone = true;
      cash += 15000;
      message = "Global network achieved! 15,000 cr growth grant received.";
    }
  }

  public static String money(long n) {
    return String.format(Locale.US, "%,d cr", n);
  }

  public boolean tutorialReady() {
    switch (tutorialStep) {
      case 1:
        return rides > 0 || taxis.get(0).status != IDLE;
      case 2:
        return rides >= 1;
      case 3:
        return taxis.size() >= 2;
      case 4:
        return openCount() >= 3;
      case 5:
        return totalHubLevels() >= 1;
      case 6:
        for (Manager m : managers) if (m.hired) return true;
        return false;
      case 7:
        return portfolioValue() > 0;
      default:
        return false;
    }
  }

  public String objectiveText() {
    if (gameMode == MODE_SOLO_QUICK) return QUICK_GOAL_NAMES[quickGoalType];
    if (gameMode == MODE_SOLO_SCENARIO) return currentHistoricalScenario().objective;
    if (gameMode == MODE_TUTORIAL) return "Academy · step " + tutorialStep + " / 8";
    return currentScenario().goalText;
  }

  public boolean objectiveComplete() {
    if (gameMode == MODE_TUTORIAL) return tutorialStep == 8;
    if (gameMode == MODE_SOLO_QUICK) {
      switch (quickGoalType) {
        case GOAL_WORTH_5CR:
          return netWorth() >= 312500;
        case GOAL_OWN_4_COMPANIES:
          return ownedCompanyCount() >= 4;
        case GOAL_ESTABLISH_2_COUNTRIES:
          return establishedCountryCount() >= 2;
        case GOAL_CORRIDOR_HAUL:
          return districts.get(2).open && cargoTons >= 120;
        case GOAL_DEBT_FREE_IPO:
          return marginDebt == 0 && bonds == 0 && netWorth() >= 60000;
        case GOAL_SERVICE_CROWN:
          return rating >= 4.92 && taxis.size() >= 6;
        default:
          return false;
      }
    }
    if (gameMode == MODE_SOLO_SCENARIO) {
      HistoricalScenarioDef h = currentHistoricalScenario();
      // Six real playable hubs are available in each city; scenarios never require a seventh.
      if (netWorth() < h.targetWorth
          || ownedCompanyCount() < h.targetCompanies
          || establishedCountryCount() < h.targetCountries
          || openCount() < Math.min(districts.size(), h.targetHubs)
          || rides < h.targetRides) return false;
      switch (h.id) {
        case 4:
        case 17:
          return bonds == 0 && marginDebt == 0;
        case 5:
          return districts.get(2).open && cargoTons >= 120;
        case 6:
          return taxis.size() >= 8;
        case 7:
          return businessModel == 2 && drivers.size() >= 6 && rating >= 4.92;
        case 8:
          return districts.get(2).open && corridorTier[districts.get(2).cell()] >= 3;
        case 9:
          return rating >= 4.92;
        case 10:
          int hired = 0;
          for (Manager m : managers) if (m.hired) hired++;
          return averageCondition() >= 85 && hired >= 3 && rating >= 4.93;
        case 11:
        case 12:
          for (Taxi t : taxis) if (t.tier < 3) return false;
          return h.id != 12 || managers.get(3).hired;
        case 13:
          for (Taxi t : taxis) if (t.cleanliness < 95) return false;
          return rating >= 4.95;
        case 14:
          int terminals = 0;
          for (District d : districts) if (d.level >= 2) terminals++;
          return terminals >= 3;
        case 16:
          return bonds == 0 && marginDebt == 0 && revenue >= 40000;
        case 18:
          int comfortable = 0;
          for (Taxi t : taxis) if (t.comfortLevel >= 3) comfortable++;
          return comfortable >= 3 && cargoTons >= 140;
        default:
          return true;
      }
    }
    Scenario sc = currentScenario();
    return openCount() >= sc.targetStations
        && rides >= sc.targetRides
        && netWorth() >= sc.targetWorth;
  }

  public double averageCondition() {
    double total = 0;
    for (Taxi t : taxis) total += t.condition;
    return total / taxis.size();
  }

  private Driver driverFor(Taxi taxi) {
    for (Driver driver : drivers) if (driver.assignedTaxiId == taxi.id) return driver;
    return null;
  }
}
