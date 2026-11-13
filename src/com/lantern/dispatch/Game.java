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
        6,
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
        6,
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
        6,
        65),
    new HistoricalScenarioDef(
        20,
        1,
        2013,
        2022,
        "London Thames Triple-Rival Dogfight",
        "London 2013–2022",
        "Multi-AI War: Acquire 2 AI rivals fielding 4 fleet units & reach £65,000",
        65000L,
        3,
        1,
        6,
        50),
    new HistoricalScenarioDef(
        21,
        3,
        2015,
        2025,
        "Tokyo 4-Way Megacity AI Turf War",
        "Tokyo 2015–2025",
        "Multi-AI Megacorp Hegemony: Outlast 3 AI rivals (5 fleet each) & reach ¥75,000",
        75000L,
        4,
        1,
        6,
        55)
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

  public static final int CUE_NONE = 0,
      CUE_CASH = 1,
      CUE_INCIDENT = 2,
      CUE_FANFARE = 3,
      CUE_NEWS = 4,
      CUE_DISPATCH = 5;

  public int scenarioId = 0;
  public int difficulty = DIFF_MEDIUM;
  public int gameMode = MODE_CITY;
  public int campaignStartYear = 2011, campaignEndYear = 2025;
  public String founderName = "Founder";
  public String companyName = "Fareforge Mobility";
  public int founderEquityPct = 80;
  public long founderCash = 2500L;
  public long founderSalaryRate = 350L;
  public long founderTotalSalary = 0L;
  public long founderTotalDividends = 0L;
  public int founderCeoTarget = -1;
  public String flagshipHiredCeoName = "Vikram Rao";
  public int pendingSoundCue = 0;
  public int founderXp = 0;
  public int founderLevel = 1;
  public long unclaimedCareerRewards = 0L;
  public boolean[] medals = new boolean[12];
  public int dailyStreak = 1;
  public long lastCheckInEpochDay = 0L;
  public long lastActiveTimestampMs = 0L;

  public static final String[] RANK_TITLES = {
    "Street Dispatcher",
    "Depot Supervisor",
    "Fleet Captain",
    "Corridor Architect",
    "Regional Director",
    "Transit Mogul",
    "Boardroom Baron",
    "Continental Tycoon",
    "Global Chairman",
    "Mobility Emperor"
  };

  public static final String[] MEDAL_NAMES = {
    "First Dispatch",
    "Regulars' Choice",
    "Century Express",
    "Fleet Commander",
    "Full Grid Network",
    "Mega Terminal Architect",
    "Maglev Pioneer",
    "Executive Cabinet",
    "Five-Star Crown",
    "Wall Street Raider",
    "Serial Entrepreneur",
    "Global Empire"
  };

  public static final String[] MEDAL_DESCS = {
    "Complete your 1st passenger or parcel trip",
    "Complete 25 total trips across your network",
    "Complete 100 total trips across your network",
    "Expand your fleet to 4 or more vehicles",
    "Open all 6 district stations in a city",
    "Upgrade any station to a Level 3 Mega Terminal",
    "Upgrade a vehicle or corridor to Tier 3",
    "Hire 3 or more executive managers",
    "Achieve 4.90★+ service rating with 10+ completed trips",
    "Acquire and merge a rival transport company",
    "Own 3 or more companies/ventures in your portfolio",
    "Operate active transit branches in 3+ world cities"
  };

  public static final long[] MEDAL_REWARDS = {
    250L, 600L, 1500L, 800L, 1200L, 1000L, 1100L, 1200L, 1400L, 2000L, 2200L, 3000L
  };
  public final ArrayList<String> seenNews = new ArrayList<>();
  public final ArrayList<Report> reports = new ArrayList<>();
  public final ArrayList<Venture> ventures = new ArrayList<>();
  public final ArrayList<CityBranchState> cityStates = new ArrayList<>();
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
  public int lastAnnualMeetingYear = 0;
  public long annualRevenue = 0L, annualExpenses = 0L;
  public int annualRides = 0;
  public long lastYearRevenue = 0L, lastYearExpenses = 0L, lastYearProfit = 0L;
  public int lastYearRides = 0;
  public int investorApprovalRating = 85;
  public boolean annualMeetingPending = false;
  public long corporateDividendRate = 400L;
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
    public int tripsOriginating = 0;
    public long revenueOriginating = 0L;
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
    public int tripsCompleted = 0, distanceTraveled = 0;
    public long totalFareEarned = 0L;
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
    public String name;
    public final int district, color;
    public int price, owned = 0, fleet;
    public int hubsOpen = 2, corridorLevel = 1, strategy = 0;
    public String lastAction = "Operating regional line";
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
      this.strategy = Math.abs(name.hashCode()) % 3;
      this.path = new int[] {startCell};
      priceHistory.add(price);
    }

    public String strategyName() {
      return strategy == 0
          ? "Fare Undercutter"
          : strategy == 1 ? "Corridor Builder" : "Fleet Expansionist";
    }

    public void recordPrice() {
      priceHistory.add(price);
      if (priceHistory.size() > 18) priceHistory.remove(0);
    }
  }

  public static final class CityBranchState implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public int cityId;
    public final ArrayList<District> districts = new ArrayList<>();
    public final ArrayList<Taxi> taxis = new ArrayList<>();
    public final ArrayList<Driver> drivers = new ArrayList<>();
    public final ArrayList<Company> companies = new ArrayList<>();
    public final ArrayList<Ride> requests = new ArrayList<>();
    public final int[] corridorTier = new int[W * H];

    public CityBranchState(int cityId) {
      this.cityId = cityId;
    }
  }

  public static final String[] HIRED_CEO_ROSTER = {
    "Ananya Sen",
    "Kenji Sato",
    "Marcus Vance",
    "Elena Rostova",
    "Arjun Mehta",
    "Clara Sterling"
  };

  public static final class Venture implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public String name;
    public String ceoName = "Ananya Sen";
    public long ceoSalary = 240L;
    public int cityId, hubA, hubB, founderEquityPct;
    public int fleet = 2;
    public long cash = 3200L, valuation = 8000L, revenue = 0L;
    public boolean merged = false, sold = false, pendingVote = false;
    public String voteType = "";
    public long voteOfferAmount = 0L;
    public String voteSummary = "";
    public int cell = 0, at = 0;
    public double progress = 0;
    public int[] path;

    public Venture(String name, int cityId, int hubA, int hubB, int founderEquityPct) {
      this.name = name;
      this.cityId = cityId;
      this.hubA = hubA;
      this.hubB = hubB;
      this.founderEquityPct = Math.max(20, Math.min(100, founderEquityPct));
    }
  }

  public static String hiredCeoNameFor(int index) {
    return HIRED_CEO_ROSTER[Math.abs(index) % HIRED_CEO_ROSTER.length];
  }

  public boolean isFounderFlagshipCeo() {
    return founderCeoTarget == -1;
  }

  public String flagshipCeoName() {
    return founderCeoTarget == -1 ? founderName : flagshipHiredCeoName;
  }

  public String activeFounderCeoCompanyName() {
    if (founderCeoTarget >= 0 && founderCeoTarget < ventures.size()) {
      Venture v = ventures.get(founderCeoTarget);
      if (!v.merged && !v.sold) return v.name;
    }
    return companyName;
  }

  public boolean assignFounderAsCeo(int targetIndex) {
    if (targetIndex < -1 || targetIndex >= ventures.size()) return false;
    if (targetIndex >= 0) {
      Venture target = ventures.get(targetIndex);
      if (target.merged || target.sold) return false;
    }
    // Restore hired professional CEO to whichever company the entrepreneur is leaving
    if (founderCeoTarget >= 0 && founderCeoTarget < ventures.size()) {
      ventures.get(founderCeoTarget).ceoName = hiredCeoNameFor(founderCeoTarget);
    }
    founderCeoTarget = targetIndex;
    if (targetIndex == -1) {
      pendingSoundCue = CUE_CASH;
      message =
          founderName
              + " assumed CEO command of "
              + companyName
              + " (1 CEO per company rule enforced).";
    } else {
      Venture v = ventures.get(targetIndex);
      v.ceoName = founderName;
      pendingSoundCue = CUE_CASH;
      message =
          founderName
              + " is now CEO of "
              + v.name
              + ". "
              + flagshipHiredCeoName
              + " appointed Acting CEO of "
              + companyName
              + ".";
    }
    return true;
  }

  public boolean setFounderSalary(long rate) {
    founderSalaryRate = Math.max(100L, Math.min(1000L, rate));
    message =
        "Entrepreneur CEO salary set to "
            + money(founderSalaryRate)
            + " / quarter (paid by "
            + activeFounderCeoCompanyName()
            + ").";
    return true;
  }

  public boolean declareCorporateDividend(long totalDistribution) {
    if (totalDistribution <= 0 || cash < totalDistribution + 1500L) {
      message =
          "Need at least "
              + money(totalDistribution + 1500L)
              + " in corporate treasury to declare this dividend.";
      return false;
    }
    cash -= totalDistribution;
    long founderShare = totalDistribution * founderEquityPct / 100L;
    founderCash += founderShare;
    founderTotalDividends += founderShare;
    dividends += founderShare;
    pendingSoundCue = CUE_CASH;
    message =
        companyName
            + " declared "
            + money(totalDistribution)
            + " dividend — "
            + founderName
            + " received "
            + money(founderShare)
            + " ("
            + founderEquityPct
            + "% stake).";
    return true;
  }

  public boolean investPersonalCashIntoCompany(long amount) {
    if (amount <= 0 || founderCash < amount) {
      message = "Insufficient Entrepreneur personal cash (" + money(founderCash) + ").";
      return false;
    }
    founderCash -= amount;
    cash += amount;
    pendingSoundCue = CUE_CASH;
    message =
        founderName
            + " injected "
            + money(amount)
            + " personal capital into "
            + companyName
            + " treasury.";
    return true;
  }

  public long equitySlicePrice() {
    return Math.max(1800L, companyNetWorth() / 10L);
  }

  public boolean buybackFounderEquity(boolean usePersonalCash) {
    if (founderEquityPct >= 100) {
      message = founderName + " already holds 100% ownership of " + companyName + ".";
      return false;
    }
    long cost = equitySlicePrice();
    if (usePersonalCash) {
      if (founderCash < cost) {
        message = "Need " + money(cost) + " in Entrepreneur personal cash to buy 10% equity.";
        return false;
      }
      founderCash -= cost;
    } else {
      if (cash < cost) {
        message = "Need " + money(cost) + " in corporate cash to repurchase 10% equity.";
        return false;
      }
      spend(cost);
    }
    founderEquityPct = Math.min(100, founderEquityPct + 10);
    pendingSoundCue = CUE_FANFARE;
    message =
        "Repurchased 10% equity in "
            + companyName
            + " ("
            + founderName
            + " now owns "
            + founderEquityPct
            + "%).";
    return true;
  }

  public boolean issueSecondaryEquity() {
    if (founderEquityPct <= 30) {
      message = "Minimum 30% founder voting stake reached in " + companyName + ".";
      return false;
    }
    long proceeds = equitySlicePrice();
    founderEquityPct -= 10;
    cash += proceeds;
    pendingSoundCue = CUE_CASH;
    message =
        "Issued 10% secondary equity in "
            + companyName
            + " for +"
            + money(proceeds)
            + " corporate capital ("
            + founderEquityPct
            + "% Founder stake).";
    return true;
  }

  public int firstOpenHubIndex() {
    for (int i = 0; i < districts.size(); i++) if (districts.get(i).open) return i;
    return 0;
  }

  public int pollSoundCue() {
    int c = pendingSoundCue;
    pendingSoundCue = CUE_NONE;
    return c;
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
    for (CityBranchState b : cityStates)
      if (b.cityId != scenarioId) {
        for (Company c : b.companies) if (c.merged) n++;
      }
    for (Venture v : ventures) if (!v.sold) n++;
    return n;
  }

  public void normalizeCareerState() {
    if (medals == null || medals.length != 12) medals = new boolean[12];
    if (founderLevel < 1) founderLevel = 1;
    if (founderXp < 0) founderXp = 0;
    if (unclaimedCareerRewards < 0) unclaimedCareerRewards = 0;
    if (dailyStreak < 1) dailyStreak = 1;
  }

  public String founderRankTitle() {
    return RANK_TITLES[Math.max(0, Math.min(RANK_TITLES.length - 1, founderLevel - 1))];
  }

  public int xpForNextLevel() {
    return Math.max(150, founderLevel * 150);
  }

  public int xpProgressPct() {
    return Math.max(0, Math.min(100, (int) ((founderXp * 100L) / Math.max(1, xpForNextLevel()))));
  }

  public int hiredManagerCount() {
    int count = 0;
    for (Manager m : managers) if (m.hired) count++;
    return count;
  }

  public int unlockedMedalCount() {
    if (medals == null) return 0;
    int count = 0;
    for (boolean m : medals) if (m) count++;
    return count;
  }

  private double medalCompletionRatio(int idx) {
    switch (idx) {
      case 0:
        return Math.min(1.0, rides);
      case 1:
        return Math.min(1.0, rides / 25.0);
      case 2:
        return Math.min(1.0, rides / 100.0);
      case 3:
        return Math.min(1.0, globalFleetCount() / 4.0);
      case 4:
        return Math.min(1.0, openCount() / 6.0);
      case 5:
        {
          int maxLv = 1;
          for (District d : districts) if (d.level > maxLv) maxLv = d.level;
          return maxLv >= 3 ? 1.0 : (maxLv - 1) / 2.0;
        }
      case 6:
        {
          int maxT = 1;
          for (Taxi t : taxis) if (t.tier > maxT) maxT = t.tier;
          for (int ct : corridorTier) if (ct > maxT) maxT = ct;
          return maxT >= 3 ? 1.0 : (maxT - 1) / 2.0;
        }
      case 7:
        return Math.min(1.0, hiredManagerCount() / 3.0);
      case 8:
        return rides >= 10 && rating >= 4.90
            ? 1.0
            : Math.min(0.95, (rides / 10.0) * 0.5 + Math.max(0, (rating - 4.5) / 0.4) * 0.5);
      case 9:
        {
          int maxOwn = 0;
          for (Company c : companies) {
            if (c.merged) return 1.0;
            if (c.owned > maxOwn) maxOwn = c.owned;
          }
          return Math.min(0.95, maxOwn / 60.0);
        }
      case 10:
        return Math.min(1.0, ownedCompanyCount() / 3.0);
      case 11:
        return Math.min(1.0, branchCount() / 3.0);
      default:
        return 0.0;
    }
  }

  public int nextLockedMedalIndex() {
    normalizeCareerState();
    int best = -1;
    double bestRatio = -1.0;
    for (int i = 0; i < medals.length; i++) {
      if (medals[i]) continue;
      double r = medalCompletionRatio(i);
      if (r > bestRatio) {
        bestRatio = r;
        best = i;
      }
    }
    return best;
  }

  public String medalProgressText(int idx) {
    switch (idx) {
      case 0:
        return Math.min(1, rides) + " / 1 trip";
      case 1:
        return Math.min(25, rides) + " / 25 trips";
      case 2:
        return Math.min(100, rides) + " / 100 trips";
      case 3:
        return Math.min(4, globalFleetCount()) + " / 4 vehicles";
      case 4:
        return Math.min(6, openCount()) + " / 6 hubs";
      case 5:
        {
          int maxLv = 1;
          for (District d : districts) if (d.level > maxLv) maxLv = d.level;
          return "Max Station Lv" + maxLv + " / 3";
        }
      case 6:
        {
          int maxT = 1;
          for (Taxi t : taxis) if (t.tier > maxT) maxT = t.tier;
          for (int ct : corridorTier) if (ct > maxT) maxT = ct;
          return "Max Tier " + maxT + " / 3";
        }
      case 7:
        return Math.min(3, hiredManagerCount()) + " / 3 managers";
      case 8:
        return String.format(Locale.US, "%.2f★ / 4.90★ (%d/10 trips)", rating, Math.min(10, rides));
      case 9:
        {
          boolean any = false;
          int maxOwn = 0;
          for (Company c : companies) {
            if (c.merged) any = true;
            if (c.owned > maxOwn) maxOwn = c.owned;
          }
          return any ? "Merged!" : (maxOwn + "% / 60% buyout threshold");
        }
      case 10:
        return Math.min(3, ownedCompanyCount()) + " / 3 companies";
      case 11:
        return Math.min(3, branchCount()) + " / 3 cities";
      default:
        return "";
    }
  }

  public void awardXp(int amount) {
    normalizeCareerState();
    if (amount <= 0) return;
    founderXp += amount;
    while (founderXp >= xpForNextLevel()) {
      founderXp -= xpForNextLevel();
      founderLevel++;
      unclaimedCareerRewards += founderLevel * 400L;
      pendingSoundCue = CUE_FANFARE;
    }
    evaluateMedals();
  }

  public int evaluateMedals() {
    normalizeCareerState();
    boolean hasLv3Hub = false;
    for (District d : districts) if (d.level >= 3) hasLv3Hub = true;
    boolean hasTier3 = false;
    for (Taxi t : taxis) if (t.tier >= 3) hasTier3 = true;
    if (!hasTier3) {
      for (int ct : corridorTier) {
        if (ct >= 3) {
          hasTier3 = true;
          break;
        }
      }
    }
    boolean hasMergedRival = false;
    for (Company c : companies) if (c.merged) hasMergedRival = true;
    boolean[] met = {
      rides >= 1,
      rides >= 25,
      rides >= 100,
      globalFleetCount() >= 4,
      openCount() >= 6,
      hasLv3Hub,
      hasTier3,
      hiredManagerCount() >= 3,
      rides >= 10 && rating >= 4.90,
      hasMergedRival,
      ownedCompanyCount() >= 3,
      branchCount() >= 3
    };
    int unlockedNow = 0;
    for (int i = 0; i < medals.length && i < met.length; i++) {
      if (!medals[i] && met[i]) {
        medals[i] = true;
        unclaimedCareerRewards += MEDAL_REWARDS[i];
        unlockedNow++;
        pendingSoundCue = CUE_FANFARE;
      }
    }
    return unlockedNow;
  }

  public boolean claimCareerRewards() {
    evaluateMedals();
    if (unclaimedCareerRewards <= 0) return false;
    long grant = unclaimedCareerRewards;
    unclaimedCareerRewards = 0L;
    cash += grant;
    long personalBonus = Math.max(100L, grant / 4L);
    founderCash += personalBonus;
    pendingSoundCue = CUE_FANFARE;
    message =
        "🏅 Claimed +"
            + money(grant)
            + " grant (+"
            + money(personalBonus)
            + " personal) at Lv"
            + founderLevel
            + " "
            + founderRankTitle()
            + "!";
    return true;
  }

  public long evaluateDailyAndOfflineComeback(long nowEpochMs) {
    normalizeCareerState();
    if (nowEpochMs <= 0L) return 0L;
    if (lastActiveTimestampMs > 0L && nowEpochMs < lastActiveTimestampMs) {
      return 0L;
    }
    long before = unclaimedCareerRewards;
    long epochDay = nowEpochMs / 86_400_000L;
    if (lastCheckInEpochDay == 0L) {
      lastCheckInEpochDay = epochDay;
      dailyStreak = 1;
    } else if (epochDay == lastCheckInEpochDay + 1L) {
      dailyStreak = Math.min(30, dailyStreak + 1);
      lastCheckInEpochDay = epochDay;
      unclaimedCareerRewards += 300L + dailyStreak * 150L;
      awardXp(50);
    } else if (epochDay > lastCheckInEpochDay + 1L) {
      dailyStreak = 1;
      lastCheckInEpochDay = epochDay;
      unclaimedCareerRewards += 300L;
      awardXp(25);
    }
    if (lastActiveTimestampMs > 0L) {
      long elapsedSec = Math.min(8L * 3600L, (nowEpochMs - lastActiveTimestampMs) / 1000L);
      if (elapsedSec >= 300L) {
        long idleGrant =
            Math.min(
                5000L,
                Math.max(120L, (elapsedSec / 60L) * (taxis.size() * 6L + openCount() * 4L)));
        unclaimedCareerRewards += idleGrant;
      }
    }
    lastActiveTimestampMs = nowEpochMs;
    return unclaimedCareerRewards - before;
  }

  public void configureFounderCompany(
      String founder, String compName, int equityPct, int startHubA, int startHubB) {
    if (founder != null && !founder.trim().isEmpty()) this.founderName = founder.trim();
    if (compName != null && !compName.trim().isEmpty()) this.companyName = compName.trim();
    this.founderEquityPct = Math.max(20, Math.min(100, equityPct));
    this.founderCeoTarget = -1;
    int outsideSoldPct = 100 - this.founderEquityPct;
    long seedCapitalBonus = (outsideSoldPct / 10) * 1600L;
    this.cash += seedCapitalBonus;

    if (startHubA >= 0
        && startHubB >= 0
        && startHubA < districts.size()
        && startHubB < districts.size()
        && startHubA != startHubB) {
      for (int i = 0; i < districts.size(); i++)
        districts.get(i).open = (i == startHubA || i == startHubB);
      Arrays.fill(corridorTier, 0);
      int[] p = route(districts.get(startHubA).cell(), districts.get(startHubB).cell());
      if (p != null) for (int c : p) corridorTier[c] = 1;
      if (!taxis.isEmpty()) {
        Taxi t0 = taxis.get(0);
        t0.cell = districts.get(startHubA).cell();
        t0.path = new int[] {t0.cell};
        t0.at = 0;
        t0.routeStops.clear();
        t0.routeStops.add(startHubA);
        t0.routeStops.add(startHubB);
      }
      requests.clear();
      createRide(startHubA, startHubB);
      createRide(startHubB, startHubA);
    }
    this.pendingSoundCue = CUE_FANFARE;
    this.message =
        this.companyName
            + " incorporated by CEO "
            + this.founderName
            + " ("
            + this.founderEquityPct
            + "% Founder Equity"
            + (seedCapitalBonus > 0 ? ", +" + money(seedCapitalBonus) + " Investor Capital" : "")
            + ").";
  }

  public long ventureStartupCost(int cityIdx, int equityPct) {
    long base = 5500L + (branches[cityIdx] ? 0L : branchPrice(cityIdx) / 2);
    int pct = Math.max(20, Math.min(100, equityPct));
    return Math.max(1200L, base * pct / 100L);
  }

  public boolean foundVenture(String vName, int cityIdx, int hubA, int hubB, int equityPct) {
    return foundVenture(vName, cityIdx, hubA, hubB, equityPct, false);
  }

  public boolean foundVenture(
      String vName, int cityIdx, int hubA, int hubB, int equityPct, boolean usePersonalCash) {
    if (ventures.size() >= 6) {
      message = "Maximum of 6 subsidiary ventures reached.";
      return false;
    }
    if (cityIdx < 0 || cityIdx >= SCENARIOS.length) return false;
    if (hubA == hubB) hubB = (hubA + 1) % 6;
    int pct = Math.max(20, Math.min(100, equityPct));
    long founderCost = ventureStartupCost(cityIdx, pct);
    if (usePersonalCash) {
      if (founderCash < founderCost) {
        message =
            "Need "
                + money(founderCost)
                + " in Entrepreneur personal wealth for your "
                + pct
                + "% stake.";
        return false;
      }
      founderCash -= founderCost;
    } else {
      if (cash < founderCost) {
        message = "Need " + money(founderCost) + " for your " + pct + "% founder equity stake.";
        return false;
      }
      spend(founderCost);
    }
    String cleanName =
        (vName == null || vName.trim().isEmpty())
            ? (regions[cityIdx] + " Express #" + (ventures.size() + 2))
            : vName.trim();
    if (!branches[cityIdx]) {
      branches[cityIdx] = true;
      ensureCityBranchState(cityIdx);
    }
    int startCell = 0;
    int[] initialPath = null;
    if (cityIdx == scenarioId
        && hubA >= 0
        && hubB >= 0
        && hubA < districts.size()
        && hubB < districts.size()) {
      districts.get(hubA).open = true;
      districts.get(hubB).open = true;
      startCell = districts.get(hubA).cell();
      initialPath = route(districts.get(hubA).cell(), districts.get(hubB).cell());
      if (initialPath != null)
        for (int cell : initialPath) if (corridorTier[cell] == 0) corridorTier[cell] = 1;
    } else {
      CityBranchState bs = ensureCityBranchState(cityIdx);
      if (hubA >= 0 && hubB >= 0 && hubA < bs.districts.size() && hubB < bs.districts.size()) {
        bs.districts.get(hubA).open = true;
        bs.districts.get(hubB).open = true;
        startCell = bs.districts.get(hubA).cell();
        initialPath = route(bs.districts.get(hubA).cell(), bs.districts.get(hubB).cell());
        if (initialPath != null)
          for (int cell : initialPath) if (bs.corridorTier[cell] == 0) bs.corridorTier[cell] = 1;
      }
    }
    Venture v = new Venture(cleanName, cityIdx, hubA, hubB, pct);
    v.ceoName = hiredCeoNameFor(ventures.size());
    v.cell = startCell;
    v.path = initialPath != null ? initialPath : new int[] {startCell};
    v.at = 0;
    v.progress = 0;
    ventures.add(v);
    pendingSoundCue = CUE_FANFARE;
    awardXp(150);
    message =
        "Founded "
            + cleanName
            + " in "
            + regions[cityIdx]
            + " ("
            + pct
            + "% Founder Stake · CEO: "
            + v.ceoName
            + ").";
    return true;
  }

  public boolean callVentureVote(int index, boolean sellProposal) {
    if (index < 0 || index >= ventures.size()) return false;
    Venture v = ventures.get(index);
    if (v.merged || v.sold) return false;
    v.pendingVote = true;
    v.voteType = sellProposal ? "SELL" : "MERGE";
    v.voteOfferAmount =
        sellProposal
            ? Math.round((v.valuation + v.cash) * 1.25)
            : Math.round((100 - v.founderEquityPct) * (v.valuation / 100.0));
    v.voteSummary =
        sellProposal
            ? "Acquirer offers "
                + money(v.voteOfferAmount)
                + " to buy 100% of "
                + v.name
                + " (Entrepreneur's "
                + v.founderEquityPct
                + "% payout: "
                + money(v.voteOfferAmount * v.founderEquityPct / 100L)
                + ")."
            : "Consolidate "
                + v.name
                + " fleet & hubs into "
                + companyName
                + " (Minority buyout cost: "
                + money(v.voteOfferAmount)
                + ").";
    pendingSoundCue = CUE_NEWS;
    message = "Board vote scheduled for " + v.name + ".";
    return true;
  }

  public boolean resolveVentureVote(int index, boolean founderVotesYes) {
    if (index < 0 || index >= ventures.size()) return false;
    Venture v = ventures.get(index);
    if (v.merged || v.sold || !v.pendingVote) return false;
    int outsideStake = 100 - v.founderEquityPct;
    int outsideYes =
        "SELL".equals(v.voteType)
            ? (int) Math.round(outsideStake * 0.75)
            : (int) Math.round(outsideStake * (rating >= 4.7 ? 0.70 : 0.45));
    int totalYes = (founderVotesYes ? v.founderEquityPct : 0) + outsideYes;
    v.pendingVote = false;
    if (!founderVotesYes || totalYes < 51) {
      message =
          "Shareholder Vote ("
              + totalYes
              + "% YES): Proposal for "
              + v.name
              + " declined by board.";
      return true;
    }
    if (founderCeoTarget == index) {
      founderCeoTarget = -1;
    }
    if ("SELL".equals(v.voteType)) {
      long payout = v.voteOfferAmount * v.founderEquityPct / 100L;
      founderCash += payout;
      founderTotalDividends += payout;
      cash += payout;
      revenue += payout;
      v.sold = true;
      pendingSoundCue = CUE_FANFARE;
      message =
          "Shareholder Vote Passed ("
              + totalYes
              + "% YES)! Sold "
              + v.name
              + " — "
              + founderName
              + " received "
              + money(payout)
              + ".";
      return true;
    } else {
      long buyoutCost = v.voteOfferAmount;
      if (cash < buyoutCost) {
        message = "Need " + money(buyoutCost) + " cash to buy out minority shareholders for merger.";
        return false;
      }
      spend(buyoutCost);
      v.merged = true;
      cash += v.cash;
      if (v.cityId == scenarioId) {
        int addFleet = Math.min(v.fleet, MAX_FLEET - taxis.size());
        for (int i = 0; i < addFleet; i++)
          taxis.add(new Taxi(taxis.size() + 1, districts.get(v.hubA % districts.size()).cell()));
        assignAvailableDrivers();
      } else {
        CityBranchState bs = ensureCityBranchState(v.cityId);
        int addFleet = Math.min(v.fleet, MAX_FLEET - bs.taxis.size());
        int spawnCell =
            bs.districts.isEmpty() ? 0 : bs.districts.get(v.hubA % bs.districts.size()).cell();
        for (int i = 0; i < addFleet; i++) bs.taxis.add(new Taxi(bs.taxis.size() + 1, spawnCell));
      }
      pendingSoundCue = CUE_FANFARE;
      message =
          "Shareholder Vote Passed ("
              + totalYes
              + "% YES)! Merged "
              + v.name
              + " into "
              + companyName
              + ".";
      return true;
    }
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
    syncHistoryStageToStartYear();
    if (h.id == 4) {
      this.bonds = 2;
    } else if (h.id == 17) {
      this.bonds = 3;
      this.cash = 9500;
    } else if (h.id == 20) {
      for (Company c : companies) {
        c.fleet = 4;
        c.cash = 9500;
      }
    } else if (h.id == 21) {
      for (Company c : companies) {
        c.fleet = 5;
        c.cash = 12500;
      }
    }
    this.message = "Scenario #" + (h.id + 1) + " [" + h.era + "]: " + h.objective;
  }

  private void syncHistoryStageToStartYear() {
    if (campaignStartYear >= 2022) historyStage = 4;
    else if (campaignStartYear >= 2020) historyStage = 3;
    else if (campaignStartYear >= 2015) historyStage = 2;
    else if (campaignStartYear >= 2013) historyStage = 1;
    else historyStage = 0;
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
    ventures.clear();
    cityStates.clear();
    chapter = 0;
    districts.clear();
    taxis.clear();
    requests.clear();
    companies.clear();
    Arrays.fill(corridorTier, 0);
    cash = 12000;
    founderCash = 2500L;
    founderSalaryRate = 350L;
    founderTotalSalary = 0L;
    founderTotalDividends = 0L;
    founderCeoTarget = -1;
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

    populateCityTemplate(scenarioId, districts, companies);

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
    activeCurrencySymbol = sc.currencySymbol;
    lastAnnualMeetingYear = 0;
    annualRevenue = 0L;
    annualExpenses = 0L;
    annualRides = 0;
    lastYearRevenue = 0L;
    lastYearExpenses = 0L;
    lastYearProfit = 0L;
    lastYearRides = 0;
    investorApprovalRating = 85;
    annualMeetingPending = false;
    corporateDividendRate = 400L;
    message = sc.city + " Scenario (" + sc.title + "): " + sc.goalText + ".";
  }

  private void populateCityTemplate(int cityId, List<District> distOut, List<Company> compOut) {
    distOut.clear();
    compOut.clear();
    if (cityId == 1) {
      distOut.add(
          new District("Westminster", "Central London", "Gov & PAX", 5, 4, 0xffffc566, true));
      distOut.add(
          new District(
              "Canary Wharf",
              "Docklands Finance",
              "Financial VIP & Mail",
              15,
              4,
              0xff73d6cf,
              true));
      distOut.add(
          new District(
              "Heathrow Gate", "West Air Hub", "Air Cargo & PAX", 25, 4, 0xffb4a0e5, false));
      distOut.add(
          new District(
              "King's Cross",
              "North Rail Terminus",
              "Intercity Rail & Goods",
              5,
              16,
              0xffe89b7c,
              false));
      distOut.add(
          new District(
              "Southwark Bank", "South Thames Hub", "Commuter PAX", 15, 16, 0xff93c5a1, false));
      distOut.add(
          new District(
              "Greenwich Pier", "Thames East Port", "Maritime Freight", 25, 16, 0xff83bce4, false));
      compOut.add(new Company("ThamesLine PLC", 55, 3, 3, 0xffe89b7c, distOut.get(3).cell()));
      compOut.add(new Company("Docklands Express", 70, 4, 2, 0xff83bce4, distOut.get(2).cell()));
      compOut.add(new Company("Albion Mobility", 45, 2, 5, 0xffb4a0e5, distOut.get(5).cell()));
    } else if (cityId == 2) {
      distOut.add(
          new District(
              "Midtown Core", "Manhattan Hub", "VIP & Mail Supply", 5, 4, 0xffffc566, true));
      distOut.add(
          new District(
              "Wall Street", "Financial District", "Executive & Mail", 15, 4, 0xff73d6cf, true));
      distOut.add(
          new District(
              "Queens JFK", "Airport Express", "Air Cargo & PAX", 25, 4, 0xffb4a0e5, false));
      distOut.add(
          new District(
              "Jersey Terminal", "Hudson Freight", "Industrial Goods", 5, 16, 0xffe89b7c, false));
      distOut.add(
          new District(
              "Brooklyn Navy", "East River Hub", "Commuter & Tech", 15, 16, 0xff93c5a1, false));
      distOut.add(
          new District(
              "Bronx Concourse", "North Corridor", "Metro Commuters", 25, 16, 0xff83bce4, false));
      compOut.add(new Company("Hudson Transit Corp", 55, 3, 3, 0xffe89b7c, distOut.get(3).cell()));
      compOut.add(new Company("Gotham Skyway", 70, 4, 2, 0xff83bce4, distOut.get(2).cell()));
      compOut.add(new Company("Tri-Borough Line", 45, 2, 5, 0xffb4a0e5, distOut.get(5).cell()));
    } else if (cityId == 3) {
      distOut.add(
          new District(
              "Shinjuku Core", "West Skyscraper Ward", "High-Volume PAX", 5, 4, 0xffffc566, true));
      distOut.add(
          new District(
              "Akihabara Tech",
              "Electric Town",
              "Precision Tech Freight",
              15,
              4,
              0xff73d6cf,
              true));
      distOut.add(
          new District(
              "Haneda Skyport", "Bay Airport", "Air Express & PAX", 25, 4, 0xffb4a0e5, false));
      distOut.add(
          new District(
              "Shinagawa Rail",
              "Shinkansen Hub",
              "Bullet Freight & Mail",
              5,
              16,
              0xffe89b7c,
              false));
      distOut.add(
          new District(
              "Shibuya Crossing",
              "Commercial Ward",
              "Retail & Commuters",
              15,
              16,
              0xff93c5a1,
              false));
      distOut.add(
          new District(
              "Odaiba Waterfront",
              "Tokyo Bay Island",
              "Port & Expo Line",
              25,
              16,
              0xff83bce4,
              false));
      compOut.add(new Company("Edokko Maglev", 55, 3, 3, 0xffe89b7c, distOut.get(3).cell()));
      compOut.add(new Company("Kanto Bay Line", 70, 4, 2, 0xff83bce4, distOut.get(2).cell()));
      compOut.add(new Company("Sakura Shuttle", 45, 2, 5, 0xffb4a0e5, distOut.get(5).cell()));
    } else if (cityId == 4) {
      distOut.add(
          new District("Market Street", "Downtown", "Commuters & parcels", 5, 4, 0xffffc566, true));
      distOut.add(
          new District("SoMa", "Technology district", "Executive travel", 15, 4, 0xff73d6cf, true));
      distOut.add(
          new District(
              "SFO Airport", "Peninsula gateway", "Airport transfers", 25, 4, 0xffb4a0e5, false));
      distOut.add(
          new District(
              "Mission District",
              "Neighbourhood hub",
              "Local deliveries",
              5,
              16,
              0xffe89b7c,
              false));
      distOut.add(
          new District(
              "Fisherman's Wharf",
              "Northern waterfront",
              "Visitor journeys",
              15,
              16,
              0xff93c5a1,
              false));
      distOut.add(
          new District(
              "Bayview", "Southern district", "Industry & residents", 25, 16, 0xff83bce4, false));
      compOut.add(new Company("Golden Gate Mobility", 55, 3, 3, 0xffe89b7c, distOut.get(3).cell()));
      compOut.add(new Company("Bayline Express", 70, 4, 2, 0xff83bce4, distOut.get(2).cell()));
      compOut.add(new Company("Pacific Loop", 45, 2, 5, 0xffb4a0e5, distOut.get(5).cell()));
    } else if (cityId == 5) {
      distOut.add(
          new District(
              "Marina Bay", "Financial Waterfront", "Executive & Mail", 5, 4, 0xffffc566, true));
      distOut.add(
          new District(
              "One-North Biopolis",
              "Research Corridor",
              "Tech & Biotech",
              15,
              4,
              0xff73d6cf,
              true));
      distOut.add(
          new District(
              "Changi Jewel", "East Air Terminal", "Global Air Cargo", 25, 4, 0xffb4a0e5, false));
      distOut.add(
          new District(
              "Tuas Mega Port",
              "West Container Hub",
              "Maritime Containers",
              5,
              16,
              0xffe89b7c,
              false));
      distOut.add(
          new District(
              "Orchard Central", "Retail Boulevard", "Commercial PAX", 15, 16, 0xff93c5a1, false));
      distOut.add(
          new District(
              "Sentosa Causeway",
              "Southern Island",
              "Resort & Shuttle",
              25,
              16,
              0xff83bce4,
              false));
      compOut.add(new Company("Merlion Autonomous", 55, 3, 3, 0xffe89b7c, distOut.get(3).cell()));
      compOut.add(new Company("Strait Skyway", 70, 4, 2, 0xff83bce4, distOut.get(2).cell()));
      compOut.add(new Company("Lion City Transit", 45, 2, 5, 0xffb4a0e5, distOut.get(5).cell()));
    } else {
      distOut.add(
          new District(
              "K.R. Market", "Central Bengaluru", "Commuters & parcels", 5, 4, 0xffffc566, true));
      distOut.add(
          new District(
              "Indiranagar",
              "Eastern neighbourhood",
              "Executive journeys",
              15,
              4,
              0xff73d6cf,
              true));
      distOut.add(
          new District(
              "Kempegowda Airport",
              "Airport corridor",
              "Airport journeys",
              25,
              4,
              0xffb4a0e5,
              false));
      distOut.add(
          new District(
              "Majestic Station",
              "Railway interchange",
              "Bulk Retail Goods",
              5,
              16,
              0xffe89b7c,
              false));
      distOut.add(
          new District(
              "Whitefield", "Technology corridor", "Daily commuters", 15, 16, 0xff93c5a1, false));
      distOut.add(
          new District(
              "Koramangala",
              "Southern neighbourhood",
              "Local journeys",
              25,
              16,
              0xff83bce4,
              false));
      compOut.add(
          new Company("Copperwheel Mobility", 55, 3, 3, 0xffe89b7c, distOut.get(3).cell()));
      compOut.add(new Company("Glasswing Transit", 70, 4, 2, 0xff83bce4, distOut.get(2).cell()));
      compOut.add(new Company("Violet Loop", 45, 2, 5, 0xffb4a0e5, distOut.get(5).cell()));
    }
  }

  private CityBranchState ensureCityBranchState(int cityId) {
    for (CityBranchState s : cityStates) if (s.cityId == cityId) return s;
    CityBranchState s = new CityBranchState(cityId);
    populateCityTemplate(cityId, s.districts, s.companies);
    int[] starter = route(s.districts.get(0).cell(), s.districts.get(1).cell());
    if (starter != null) for (int c : starter) s.corridorTier[c] = 1;
    s.taxis.add(new Taxi(1, s.districts.get(0).cell()));
    s.drivers.add(new Driver(1, "Captain #" + (cityId + 1), 4.8, 1));
    for (int i = 0; i < s.companies.size(); i++) {
      Company c = s.companies.get(i);
      int nextDist = (c.district + i + 1) % s.districts.size();
      c.targetDistrict = nextDist;
      int[] r = route(c.cell, s.districts.get(nextDist).cell());
      c.path = r != null ? r : new int[] {c.cell};
    }
    cityStates.add(s);
    return s;
  }

  private void saveCurrentCityState() {
    CityBranchState current = ensureCityBranchState(scenarioId);
    current.districts.clear();
    current.districts.addAll(districts);
    current.taxis.clear();
    current.taxis.addAll(taxis);
    current.drivers.clear();
    current.drivers.addAll(drivers);
    current.companies.clear();
    current.companies.addAll(companies);
    current.requests.clear();
    current.requests.addAll(requests);
    System.arraycopy(corridorTier, 0, current.corridorTier, 0, corridorTier.length);
  }

  /** Switch active management view between unlocked city branches (e.g. Bengaluru <-> London). */
  public boolean switchCity(int targetCity) {
    if (targetCity < 0 || targetCity >= SCENARIOS.length) return false;
    if (!branches[targetCity]) {
      message = "Launch the " + regions[targetCity] + " branch first before managing it.";
      return false;
    }
    if (targetCity == scenarioId) {
      message = "Currently managing " + regions[targetCity] + " operations.";
      return true;
    }
    saveCurrentCityState();
    CityBranchState next = ensureCityBranchState(targetCity);
    this.scenarioId = targetCity;
    districts.clear();
    districts.addAll(next.districts);
    taxis.clear();
    taxis.addAll(next.taxis);
    drivers.clear();
    drivers.addAll(next.drivers);
    companies.clear();
    companies.addAll(next.companies);
    requests.clear();
    requests.addAll(next.requests);
    System.arraycopy(next.corridorTier, 0, corridorTier, 0, corridorTier.length);
    assignAvailableDrivers();
    if (requests.isEmpty()) {
      createRide(0, 1);
      createRide(1, 0);
    }
    pendingSoundCue = CUE_DISPATCH;
    message = "Switched active city management to " + regions[targetCity] + ".";
    return true;
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
    awardXp(50);
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
    awardXp(80);
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
    ensureCityBranchState(i);
    pendingSoundCue = CUE_FANFARE;
    awardXp(150);
    message =
        "Launched "
            + regions[i]
            + "! Tap 'Switch to "
            + regions[i]
            + "' in Hubs to manage local operations.";
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
    awardXp(80);
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
    awardXp(60);
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
    annualExpenses += n;
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
    int hq = firstOpenHubIndex();
    Taxi vehicle = new Taxi(taxis.size() + 1, districts.get(hq).cell());
    taxis.add(vehicle);
    assignAvailableDrivers();
    awardXp(60);
    message = "New vehicle ready at " + districts.get(hq).name + ".";
    return true;
  }

  public void assignAvailableDrivers() {
    HashSet<Integer> validTaxis = new HashSet<>();
    for (Taxi taxi : taxis) validTaxis.add(taxi.id);
    HashSet<Integer> assignedTaxis = new HashSet<>();
    for (Driver driver : drivers) {
      if (driver.assignedTaxiId != 0 && !validTaxis.contains(driver.assignedTaxiId)) {
        driver.assignedTaxiId = 0;
      }
      if (driver.assignedTaxiId > 0) {
        if (!assignedTaxis.add(driver.assignedTaxiId)) {
          driver.assignedTaxiId = 0;
        }
      }
    }
    for (Driver driver : drivers) {
      if (driver.assignedTaxiId == 0) {
        for (Taxi taxi : taxis) {
          if (!assignedTaxis.contains(taxi.id)) {
            driver.assignedTaxiId = taxi.id;
            assignedTaxis.add(taxi.id);
            break;
          }
        }
      }
    }
  }

  public boolean isStaffed(Taxi taxi) {
    if (taxi == null) return false;
    return driverFor(taxi) != null || (taxi.tier >= 3 && year() >= 2022);
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
    int hq = firstOpenHubIndex();
    d.open = true;
    int[] p = route(districts.get(hq).cell(), d.cell());
    if (p != null) for (int c : p) if (corridorTier[c] == 0) corridorTier[c] = 1;
    createRide(i, hq);
    awardXp(80);
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
    boolean unstaffedIdle = false;
    for (Taxi t : taxis)
      if (t.status == IDLE) {
        if (!isStaffed(t)) {
          unstaffedIdle = true;
          continue;
        }
        int[] path = route(t.cell, districts.get(r.from).cell());
        if (path != null && (best == null || path.length < best.length)) {
          nearest = t;
          best = path;
        }
      }
    if (nearest == null) {
      message =
          unstaffedIdle
              ? "Idle vehicles need an assigned driver. Recruit a driver in Fleet & Team."
              : "All consists are busy. Buy another or wait for arrival.";
      return false;
    }
    return assignRide(r, nearest, best);
  }

  private boolean assignRide(Ride r, Taxi vehicle, int[] pickupPath) {
    if (vehicle == null || vehicle.status != IDLE || !isStaffed(vehicle) || pickupPath == null)
      return false;
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

  public long estimatedIncome(Ride r) {
    if (r == null) return 0;
    Taxi ref = taxis.isEmpty() ? null : taxis.get(0);
    for (Taxi t : taxis)
      if (t.status == IDLE && isStaffed(t)) {
        ref = t;
        break;
      }
    long tierTip = ref == null ? 0 : (ref.tier - 1) * 12L + (ref.comfortLevel - 1) * 8L;
    Driver partner = ref == null ? null : driverFor(ref);
    double ceoBoost = isFounderFlagshipCeo() ? 1.10 : 1.0;
    double serviceFactor =
        (partner == null ? 1 : 1 + (partner.level - 1) * .03 - (partner.fatigue >= 85 ? .1 : 0))
            * ceoBoost;
    return businessModel == 2
        ? tierTip
        : Math.round(r.fare * (businessModel == 1 ? .40 : .32) * serviceFactor) + tierTip;
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
      District orig = districts.get(t.ride.from);
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
      double ceoBoost = isFounderFlagshipCeo() ? 1.10 : 1.0;
      double serviceFactor =
          (partner == null ? 1 : 1 + (partner.level - 1) * .03 - (partner.fatigue >= 85 ? .1 : 0))
              * ceoBoost;
      long income =
          businessModel == 2
              ? tierTip
              : Math.round(t.ride.fare * (businessModel == 1 ? .40 : .32) * serviceFactor)
                  + tierTip;
      grossFares += t.ride.fare;
      cash += income;
      revenue += income;
      rides++;
      annualRevenue += income;
      annualRides++;
      t.tripsCompleted++;
      t.totalFareEarned += income;
      t.distanceTraveled += (t.path != null ? t.path.length : 1);
      orig.tripsOriginating++;
      orig.revenueOriginating += income;
      awardXp(25);
      if (t.ride.kind == KIND_CARGO
          || orig.hasFacility(FAC_CARGO)
          || dest.hasFacility(FAC_CARGO)) {
        cargoTons += t.capacity;
      }
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

  public boolean serviceAllTaxis() {
    long unitCost = managers.size() > 0 && managers.get(0).hired ? 220L : 300L;
    int count = 0;
    boolean needed = false;
    for (int i = 0; i < taxis.size(); i++) {
      Taxi t = taxis.get(i);
      if (t.broken || t.condition < 99.5) {
        needed = true;
        if (cash >= unitCost) {
          spend(unitCost);
          t.condition = 100.0;
          t.broken = false;
          count++;
        }
      }
    }
    if (!needed) {
      message = "All vehicles are already at 100% mechanical condition.";
      return false;
    }
    if (count == 0) {
      message = "Need " + money(unitCost) + " to service fleet.";
      return false;
    }
    message = "Serviced " + count + " vehicle" + (count == 1 ? "" : "s") + " (" + money(count * unitCost) + ").";
    return true;
  }

  public boolean cleanAllTaxis() {
    long unitCost = 120L;
    int count = 0;
    boolean needed = false;
    for (int i = 0; i < taxis.size(); i++) {
      Taxi t = taxis.get(i);
      if (t.cleanliness < 99.5) {
        needed = true;
        if (cash >= unitCost) {
          spend(unitCost);
          t.cleanliness = 100.0;
          count++;
        }
      }
    }
    if (!needed) {
      message = "All vehicle cabins are already spotless.";
      return false;
    }
    if (count == 0) {
      message = "Need " + money(unitCost) + " to clean fleet.";
      return false;
    }
    message = "Deep-cleaned " + count + " vehicle" + (count == 1 ? "" : "s") + " (" + money(count * unitCost) + ").";
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
    drivers.add(new Driver(id, name != null ? name : ("Driver #" + id), 4.7, 0));
    assignAvailableDrivers();
    awardXp(75);
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
    awardXp(80);
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
    awardXp(75);
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
      assignAvailableDrivers();
      cash += 1800;
      message =
          "Receivership restructuring: 1 consist auctioned to creditors to restore liquidity.";
    } else {
      cash = -500;
      message = "Receivership restructuring: creditors reorganized operating deficit.";
    }
  }

  public int cityFleetCount(int cityIdx) {
    if (cityIdx == scenarioId) return taxis.size();
    for (CityBranchState b : cityStates) if (b.cityId == cityIdx) return b.taxis.size();
    return branches[cityIdx] ? 1 : 0;
  }

  public int globalFleetCount() {
    int total = taxis.size();
    for (CityBranchState b : cityStates) if (b.cityId != scenarioId) total += b.taxis.size();
    return total;
  }

  public int globalOpenHubCount() {
    int total = openCount();
    for (CityBranchState b : cityStates)
      if (b.cityId != scenarioId) {
        for (District d : b.districts) if (d.open) total++;
      }
    return total;
  }

  public int globalHubLevelCount() {
    int total = totalHubLevels();
    for (CityBranchState b : cityStates)
      if (b.cityId != scenarioId) {
        for (District d : b.districts) if (d.open) total += (d.level - 1);
      }
    return total;
  }

  public long portfolioValue() {
    long n = 0;
    for (Company c : companies) if (!c.merged) n += (long) c.price * c.owned;
    for (CityBranchState b : cityStates)
      if (b.cityId != scenarioId) {
        for (Company c : b.companies) if (!c.merged) n += (long) c.price * c.owned;
      }
    return n;
  }

  public long ventureEquityValue() {
    long total = 0;
    for (Venture v : ventures)
      if (!v.merged && !v.sold) {
        total += (v.valuation + v.cash) * v.founderEquityPct / 100L;
      }
    return total;
  }

  public long companyNetWorth() {
    return cash
        + portfolioValue()
        - marginDebt
        - bondDebt()
        + globalFleetCount() * 1500L
        + globalOpenHubCount() * 2000L
        + globalHubLevelCount() * 1800L
        + (branchCount() - 1) * 4000L;
  }

  public long entrepreneurNetWorth() {
    long flagshipEquity = Math.max(0L, companyNetWorth()) * founderEquityPct / 100L;
    return founderCash + flagshipEquity + ventureEquityValue();
  }

  public long netWorth() {
    return companyNetWorth() + ventureEquityValue();
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
    assignAvailableDrivers();
    cash += c.cash + (c.fleet - inherited) * 1500L;
    districts.get(c.district).open = true;
    int hq = firstOpenHubIndex();
    int[] p = route(districts.get(hq).cell(), districts.get(c.district).cell());
    if (p != null) for (int cell : p) if (corridorTier[cell] == 0) corridorTier[cell] = 1;
    c.owned = 0;
    c.merged = true;
    pendingSoundCue = CUE_FANFARE;
    awardXp(150);
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
      for (CityBranchState b : cityStates) for (Company c : b.companies) c.owned = 0;
      marginCalls++;
      pendingSoundCue = CUE_INCIDENT;
      message =
          "Margin call: broker liquidated the portfolio. Any shortfall is charged to company cash.";
    }
  }

  private void history() {
    if (historyStage == 0 && year() >= 2013) {
      historyStage = 1;
      pendingSoundCue = CUE_NEWS;
      message =
          "2013: international competition enters "
              + currentScenario().city
              + ". Upgrade corridors and station facilities.";
    }
    if (historyStage == 1 && year() >= 2015) {
      historyStage = 2;
      pendingSoundCue = CUE_NEWS;
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
      pendingSoundCue = CUE_INCIDENT;
      message =
          "2020: lockdown disrupts travel. Scenario demand drops; fictional share prices fall 52%."
              + " Check your margin.";
      maintainMargin();
    }
    if (historyStage == 3 && year() >= 2022) {
      historyStage = 4;
      pendingSoundCue = CUE_FANFARE;
      message = "2022: autonomous & zero-commission pass networks unlock.";
    }
  }

  private void market() {
    marketTick++;
    // 1. Background City Branch Earnings (scaled by actual fleet & hubs in each saved city)
    long regional = 0L;
    for (int cIdx = 0; cIdx < branches.length; cIdx++) {
      if (branches[cIdx] && cIdx != scenarioId) {
        CityBranchState bs = ensureCityBranchState(cIdx);
        int bOpen = 0;
        for (District d : bs.districts) if (d.open) bOpen++;
        regional += Math.round((140L + bs.taxis.size() * 90L + bOpen * 45L) * demandFactor());
        for (int i = 0; i < bs.companies.size(); i++) {
          Company c = bs.companies.get(i);
          if (c.merged) continue;
          double wave = Math.sin((marketTick + i * 3) * 1.7) * .035;
          double trend = year() == 2020 ? -.012 : .014;
          c.price = Math.max(5, (int) Math.round(c.price * (1 + trend + wave)));
          c.recordPrice();
          long div = c.owned * (c.cash > 2500 ? 1 : 0);
          cash += div;
          dividends += div;
        }
      }
    }
    cash += regional;
    revenue += regional;
    if (businessModel == 2) {
      long subscription = taxis.size() * 80L;
      cash += subscription;
      revenue += subscription;
    }

    // 2. CEO Salary Payroll (1 Person = 1 Company CEO Rule)
    if (founderCeoTarget == -1) {
      // Entrepreneur is CEO of Flagship Company
      if (cash >= founderSalaryRate) {
        spend(founderSalaryRate);
        founderCash += founderSalaryRate;
        founderTotalSalary += founderSalaryRate;
      }
    } else {
      // Hired Professional CEO runs Flagship Company
      long hiredCeoCost = 250L;
      if (cash >= hiredCeoCost) spend(hiredCeoCost);
    }

    // 3. Quarterly Flagship Dividend when cash-rich
    if (cash > 11000L && corporateDividendRate > 0) {
      long autoDiv = corporateDividendRate;
      cash -= autoDiv;
      long founderCut = autoDiv * founderEquityPct / 100L;
      founderCash += founderCut;
      founderTotalDividends += founderCut;
      dividends += founderCut;
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
      double fundamental = (c.cash + c.fleet * 1800.0 + c.hubsOpen * 600.0) / 100;
      double valuePull = Math.max(-.025, Math.min(.025, (fundamental / c.price - 1) * .04));
      c.price = Math.max(5, (int) Math.round(c.price * (1 + trend + wave + valuePull)));
      c.recordPrice();
      long dividend = c.owned * (c.cash > 2500 ? 1 : 0);
      cash += dividend;
      dividends += dividend;
      c.cash -= dividend;
    }
    for (int i = 0; i < ventures.size(); i++) {
      Venture v = ventures.get(i);
      if (v.merged || v.sold) continue;
      double ceoMult = (founderCeoTarget == i) ? 1.25 : 1.0;
      long vRev = Math.round((v.fleet * 220L) * demandFactor() * ceoMult);
      v.cash += vRev;
      v.revenue += vRev;
      // Pay Venture's CEO salary (either Entrepreneur if assigned here, or Hired CEO)
      if (founderCeoTarget == i) {
        long sal = Math.min(v.cash, founderSalaryRate);
        v.cash -= sal;
        founderCash += sal;
        founderTotalSalary += sal;
      } else {
        long sal = Math.min(v.cash, v.ceoSalary);
        v.cash -= sal;
      }
      v.valuation = Math.max(4000L, v.valuation + vRev / 2);
      long founderDiv = (vRev * v.founderEquityPct) / 250L;
      if (founderDiv > 0 && v.cash >= founderDiv) {
        v.cash -= founderDiv;
        founderCash += founderDiv;
        founderTotalDividends += founderDiv;
        cash += founderDiv;
        dividends += founderDiv;
      }
      if (v.cash >= 2600 && v.fleet < 8) {
        v.cash -= 1900;
        v.fleet++;
        v.valuation += 1600;
      }
      if (!v.pendingVote && marketTick % 3 == (i % 3) && v.revenue >= 600) {
        boolean buyout = (marketTick + i) % 2 == 0;
        callVentureVote(i, buyout);
      }
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
    int curYear = year();
    if (lastAnnualMeetingYear == 0) {
      lastAnnualMeetingYear = curYear;
    } else if (curYear > lastAnnualMeetingYear && gameMode != MODE_TUTORIAL) {
      lastYearRevenue = annualRevenue;
      lastYearExpenses = annualExpenses;
      lastYearRides = annualRides;
      lastYearProfit = annualRevenue - annualExpenses;

      long netGrowth = lastYearProfit;
      int baseApproval = (int) Math.round((rating / 5.0) * 80.0);
      if (netGrowth > 2500) baseApproval += 18;
      else if (netGrowth > 0) baseApproval += 8;
      else baseApproval -= 24;
      investorApprovalRating = Math.max(30, Math.min(99, baseApproval));

      annualRevenue = 0L;
      annualExpenses = 0L;
      annualRides = 0;
      lastAnnualMeetingYear = curYear;
      annualMeetingPending = true;
      pendingSoundCue = CUE_FANFARE;
      message = "Annual Investors Meeting convened for Fiscal Year " + (curYear - 1) + "!";
    }
    double interval = Math.max(2.5, 9 - openCount() - totalHubLevels() * 0.25) / demandFactor();
    while (spawnClock >= interval) {
      spawnClock -= interval;
      generateRide();
    }
    while (marketClock >= 15) {
      marketClock -= 15;
      market();
    }
    while (competitionClock >= 14) {
      competitionClock -= 14;
      for (int i = 0; i < companies.size(); i++) {
        Company c = companies.get(i);
        if (!c.merged) {
          c.cash += c.fleet * 24L + c.hubsOpen * 30L;
          c.served++;
          // Check if AI Rival launches a Hostile Takeover Bid on a player Venture in this city
          Venture hostileTarget = null;
          for (Venture v : ventures) {
            if (!v.merged && !v.sold && !v.pendingVote && v.cityId == scenarioId) {
              hostileTarget = v;
              break;
            }
          }
          if (hostileTarget != null && c.cash >= 3400) {
            c.cash -= 600;
            hostileTarget.pendingVote = true;
            hostileTarget.voteType = "SELL";
            hostileTarget.voteOfferAmount =
                Math.round((hostileTarget.valuation + hostileTarget.cash) * 1.35);
            hostileTarget.voteSummary =
                "HOSTILE TAKEOVER BID: AI Rival "
                    + c.name
                    + " offers "
                    + money(hostileTarget.voteOfferAmount)
                    + " to acquire "
                    + hostileTarget.name
                    + " (Entrepreneur's "
                    + hostileTarget.founderEquityPct
                    + "% payout: "
                    + money(
                        hostileTarget.voteOfferAmount * hostileTarget.founderEquityPct / 100L)
                    + ").";
            c.lastAction = "Launched hostile bid on " + hostileTarget.name;
            pendingSoundCue = CUE_INCIDENT;
            message =
                "HOSTILE BID! AI Rival "
                    + c.name
                    + " tabled a takeover offer for "
                    + hostileTarget.name
                    + "!";
          } else if (c.cash >= 2200 && c.fleet < 9 && (c.strategy == 2 || c.fleet < 4)) {
            c.cash -= 1800;
            c.fleet++;
            c.lastAction = "Expanded fleet to " + c.fleet + " vehicles";
          } else if (c.cash >= 2400 && c.hubsOpen < 6 && c.strategy == 1) {
            c.cash -= 1900;
            c.hubsOpen++;
            c.corridorLevel = Math.min(3, c.corridorLevel + 1);
            c.lastAction =
                "Opened Hub #" + c.hubsOpen + " & upgraded Tier " + c.corridorLevel + " track";
          } else if (c.strategy == 0 && c.cash >= 2600 && !districts.isEmpty()) {
            c.cash -= 900;
            District targetD = districts.get(c.district % districts.size());
            for (int k = 0; k < 4; k++)
              targetD.demandMult[k] = Math.max(90, targetD.demandMult[k] - 8);
            c.lastAction = "Launched 20% discount fare war at " + targetD.name;
            pendingSoundCue = CUE_INCIDENT;
            message =
                "FARE WAR! AI Rival "
                    + c.name
                    + " slashed fares at "
                    + targetD.name
                    + "!";
          } else if (!requests.isEmpty()) {
            Ride poached = requests.remove(0);
            c.cash += poached.fare / 2;
            c.served++;
            String hubName = districts.get(poached.from).name;
            c.lastAction = "Poached waiting load at " + hubName;
            pendingSoundCue = CUE_INCIDENT;
            message =
                "AI Rival "
                    + c.name
                    + " ("
                    + c.fleet
                    + " fleet) poached a load at "
                    + hubName
                    + "!";
          }
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
    // Move active Subsidiary Venture vehicles along their hubA <-> hubB corridor in the current city
    for (Venture v : ventures) {
      if (v.merged || v.sold || v.cityId != scenarioId || districts.size() < 2) continue;
      int cellA = districts.get(Math.max(0, Math.min(districts.size() - 1, v.hubA))).cell();
      int cellB = districts.get(Math.max(0, Math.min(districts.size() - 1, v.hubB))).cell();
      if (v.path == null || v.path.length == 0) {
        v.cell = cellA;
        int[] r = route(cellA, cellB);
        v.path = r != null ? r : new int[] {cellA};
        v.at = 0;
      }
      v.progress += dt * 2.1 * trafficFactor();
      while (v.progress >= 1) {
        v.progress--;
        if (v.at < v.path.length - 1) {
          v.at++;
          v.cell = v.path[v.at];
        } else {
          int targetCell = (v.cell == cellA) ? cellB : cellA;
          int[] r = route(v.cell, targetCell);
          v.path = r != null ? r : new int[] {v.cell};
          v.at = 0;
          break;
        }
      }
    }
    if (autoDispatch) {
      for (Taxi vehicle : taxis)
        if (vehicle.status == IDLE && isStaffed(vehicle)) {
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
      pendingSoundCue = CUE_FANFARE;
      message =
          "Gold Medal in " + sc.city + " (" + sc.title + ")! +" + money(8000) + " scenario bonus awarded.";
    }
    if (!milestone && branchCount() == 6 && taxis.size() >= 10 && rides >= 100) {
      milestone = true;
      cash += 15000;
      pendingSoundCue = CUE_FANFARE;
      message = "Global network achieved! " + money(15000) + " growth grant received.";
    }
  }

  public static volatile String activeCurrencySymbol = "₹";

  public static String money(long n) {
    return activeCurrencySymbol + String.format(Locale.US, "%,d", n);
  }

  public static String quickGoalNameForCity(int goalType, int cityIdx) {
    Scenario sc = SCENARIOS[Math.max(0, Math.min(SCENARIOS.length - 1, cityIdx))];
    switch (goalType) {
      case GOAL_WORTH_5CR:
        return "Survive & Reach " + sc.currencySymbol + "312,500 Net Worth";
      case GOAL_DEBT_FREE_IPO:
        return "Debt-Free IPO: 0 Debt & " + sc.currencySymbol + "60,000 Net Worth";
      default:
        return QUICK_GOAL_NAMES[Math.max(0, Math.min(QUICK_GOAL_NAMES.length - 1, goalType))];
    }
  }

  public boolean adjustCeoSalary(long delta) {
    founderSalaryRate = Math.max(100L, Math.min(2500L, founderSalaryRate + delta));
    message = "CEO Salary adjusted to " + money(founderSalaryRate) + " / quarter.";
    return true;
  }

  public boolean adjustCorporateDividend(long delta) {
    corporateDividendRate = Math.max(0L, Math.min(2000L, corporateDividendRate + delta));
    message = "Corporate Dividend adjusted to " + money(corporateDividendRate) + " per distribution.";
    return true;
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
    if (gameMode == MODE_SOLO_QUICK) return quickGoalNameForCity(quickGoalType, scenarioId);
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

  public Driver driverFor(Taxi taxi) {
    for (Driver driver : drivers) if (driver.assignedTaxiId == taxi.id) return driver;
    return null;
  }
}
