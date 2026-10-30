package com.lantern.dispatch;

import java.io.*;
import java.util.zip.CRC32;

/** Bounded, checksummed native save format. Only local game value types may be restored. */
public final class SaveCodec {
  private static final int MAGIC = 0x4c4e5431, VERSION = 1, MAX_SIZE = 512 * 1024;

  private SaveCodec() {}

  public static byte[] encode(Game game) throws IOException {
    validate(game);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(game);
    }
    byte[] payload = bytes.toByteArray();
    if (payload.length > MAX_SIZE) throw new IOException("Save exceeds size limit");
    CRC32 crc = new CRC32();
    crc.update(payload);
    ByteArrayOutputStream envelope = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(envelope);
    out.writeInt(MAGIC);
    out.writeInt(VERSION);
    out.writeInt(payload.length);
    out.writeLong(crc.getValue());
    out.write(payload);
    out.flush();
    return envelope.toByteArray();
  }

  public static Game decode(byte[] bytes) throws IOException {
    if (bytes.length > MAX_SIZE + 20) throw new IOException("Invalid save size");
    DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
    if (input.readInt() != MAGIC || input.readInt() != VERSION)
      throw new IOException("Unsupported save version");
    int length = input.readInt();
    long checksum = input.readLong();
    if (length < 1 || length > MAX_SIZE || length != input.available())
      throw new IOException("Truncated save");
    byte[] payload = new byte[length];
    input.readFully(payload);
    CRC32 crc = new CRC32();
    crc.update(payload);
    if (crc.getValue() != checksum) throw new IOException("Save checksum mismatch");
    try (ObjectInputStream in =
        new ObjectInputStream(new ByteArrayInputStream(payload)) {
          @Override
          protected Class<?> resolveClass(ObjectStreamClass desc)
              throws IOException, ClassNotFoundException {
            String name = desc.getName();
            if (!(name.equals("com.lantern.dispatch.Game")
                || name.startsWith("com.lantern.dispatch.Game$")
                || name.equals("java.util.ArrayList")
                || name.equals("java.lang.Integer")
                || name.equals("java.lang.Number")
                || name.equals("[I")
                || name.equals("[Z")
                || name.equals("[Ljava.lang.String;")))
              throw new InvalidClassException("Unexpected save class", name);
            return super.resolveClass(desc);
          }

          @Override
          protected Class<?> resolveProxyClass(String[] interfaces) throws IOException {
            throw new InvalidClassException("Proxy objects are not permitted");
          }
        }) {
      Object object = in.readObject();
      if (!(object instanceof Game)) throw new IOException("Not a game save");
      Game game = (Game) object;
      validate(game);
      return game;
    } catch (ClassNotFoundException | RuntimeException e) {
      throw new IOException("Invalid save data", e);
    }
  }

  public static void validate(Game g) throws IOException {
    require(g != null && g.scenarioId >= 0 && g.scenarioId < Game.SCENARIOS.length, "Invalid city");
    require(
        g.gameMode >= 0
            && g.gameMode <= Game.MODE_CITY
            && g.quickGoalType >= 0
            && g.quickGoalType < Game.QUICK_GOAL_NAMES.length
            && g.historicalScenarioId >= 0
            && g.historicalScenarioId < Game.HISTORICAL_SCENARIOS.length,
        "Invalid mode");
    require(
        g.difficulty >= 0
            && g.difficulty <= 4
            && (g.speed == 0 || g.speed == 1 || g.speed == 2 || g.speed == 4),
        "Invalid speed or difficulty");
    require(
        Double.isFinite(g.elapsed)
            && g.elapsed >= 0
            && g.elapsed < 1e10
            && Double.isFinite(g.rating)
            && g.rating >= 1
            && g.rating <= 5,
        "Invalid clock or rating");
    require(
        g.campaignStartYear >= 2000
            && g.campaignEndYear >= g.campaignStartYear
            && g.campaignEndYear <= 2030,
        "Invalid campaign years");
    require(
        g.districts != null
            && g.districts.size() == 6
            && g.taxis != null
            && g.taxis.size() >= 1
            && g.taxis.size() <= Game.MAX_FLEET
            && g.requests != null
            && g.requests.size() <= 6,
        "Invalid fleet or hubs");
    require(
        g.companies != null
            && g.companies.size() == 3
            && g.drivers != null
            && g.drivers.size() >= 1
            && g.drivers.size() <= Game.MAX_FLEET
            && g.managers != null
            && g.managers.size() == 5,
        "Invalid companies or team");
    require(
        g.corridorTier != null
            && g.corridorTier.length == Game.W * Game.H
            && g.branches != null
            && g.branches.length == 6
            && g.seenNews != null
            && g.seenNews.size() <= 160,
        "Invalid network data");
    require(g.marginDebt >= 0 && g.bonds >= 0 && g.bonds <= Game.MAX_BONDS, "Invalid debt");
    require(
        g.chapter >= 0 && g.chapter <= 6 && g.tutorialStep >= 0 && g.tutorialStep <= 8,
        "Invalid progression");
    require(
        g.reports != null && g.reports.size() <= 80 && g.reviews != null && g.reviews.size() <= 12,
        "Invalid archive");
    for (Game.Report report : g.reports)
      require(
          report != null
              && report.id != null
              && report.title != null
              && report.body != null
              && report.date != null
              && report.impact != null
              && report.source != null
              && report.url != null
              && report.category != null,
          "Invalid report");
    for (Game.Driver driver : g.drivers)
      require(
          driver != null
              && Double.isFinite(driver.skill)
              && Double.isFinite(driver.fatigue)
              && driver.skill >= 0
              && driver.skill <= 5
              && driver.fatigue >= 0
              && driver.fatigue <= 100,
          "Invalid partner");
    for (int tier : g.corridorTier) require(tier >= 0 && tier <= 3, "Invalid corridor");
    for (Game.District d : g.districts)
      require(
          d != null && d.cell() >= 0 && d.cell() < Game.W * Game.H && d.level >= 1 && d.level <= 3,
          "Invalid district");
    for (Game.Taxi t : g.taxis) {
      require(
          t != null
              && t.path != null
              && t.path.length >= 1
              && t.path.length <= Game.W * Game.H
              && t.at >= 0
              && t.at < t.path.length
              && t.status >= 0
              && t.status <= 3
              && t.cell >= 0
              && t.cell < Game.W * Game.H,
          "Invalid vehicle route");
      require(
          Double.isFinite(t.progress)
              && t.progress >= 0
              && t.progress < 1
              && Double.isFinite(t.charge)
              && Double.isFinite(t.condition)
              && t.condition >= 0
              && t.condition <= 100,
          "Invalid vehicle state");
      require(
          t.tier >= 1 && t.tier <= 3 && t.routeStops != null && t.routeStops.size() <= 6,
          "Invalid vehicle tier");
      for (int cell : t.path)
        require(
            cell >= 0 && cell < Game.W * Game.H && Game.road(cell % Game.W, cell / Game.W),
            "Invalid road cell");
      for (int stop : t.routeStops) require(stop >= 0 && stop < 6, "Invalid route stop");
      if (t.status == Game.PICKUP || t.status == Game.RIDING)
        require(t.ride != null, "Missing active trip");
      if (t.ride != null) validateRide(t.ride);
    }
    for (Game.Ride r : g.requests) validateRide(r);
    for (Game.Company c : g.companies)
      require(
          c != null
              && c.price >= 5
              && c.owned >= 0
              && c.owned <= 100
              && c.priceHistory != null
              && c.priceHistory.size() <= 18,
          "Invalid shares");
  }

  private static void validateRide(Game.Ride r) throws IOException {
    require(
        r != null
            && r.from >= 0
            && r.from < 6
            && r.to >= 0
            && r.to < 6
            && r.from != r.to
            && r.fare >= 0
            && Double.isFinite(r.created)
            && r.kind >= 0
            && r.kind <= 3,
        "Invalid request");
  }

  private static void require(boolean condition, String message) throws IOException {
    if (!condition) throw new IOException(message);
  }
}
