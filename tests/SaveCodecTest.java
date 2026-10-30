package com.lantern.dispatch;

import java.io.*;
import java.util.*;

public final class SaveCodecTest {
  private static void check(boolean value, String why) {
    if (!value) throw new AssertionError(why);
  }

  public static void main(String[] args) throws Exception {
    for (int city = 0; city < 6; city++) {
      Game game = new Game(city);
      game.startQuickPlay(city, Game.GOAL_OWN_4_COMPANIES, Game.DIFF_HIGH);
      game.autoDispatch = true;
      game.cash = 100000;
      game.buyTaxi();
      game.unlock(2);
      game.upgradeDistrict(0);
      game.buildFacility(0, Game.FAC_MAINT);
      game.buildCorridor(0, 2, 3);
      game.buyShares(1, true);
      game.recruitDriver("Partner");
      game.trainDriver(1);
      game.hireManager(1);
      for (int i = 0; i < 40; i++) game.tick(.2);
      game.seenNews.add("real-uber-ipo");
      game.reports.add(
          new Game.Report(
              "test",
              "2019-05-09",
              "Headline",
              "Body",
              "Impact",
              "https://example.com",
              "Source",
              "Historical"));
      game.chapter = 3;
      byte[] bytes = SaveCodec.encode(game);
      Game restored = SaveCodec.decode(bytes);
      check(
          restored.scenarioId == game.scenarioId
              && restored.gameMode == game.gameMode
              && restored.cash == game.cash,
          "City and ledger roundtrip");
      check(
          restored.taxis.size() == 2
              && restored.drivers.size() == 2
              && restored.drivers.get(1).level == 2
              && restored.managers.get(1).hired,
          "Fleet and team roundtrip");
      check(
          restored.taxis.get(0).at == game.taxis.get(0).at
              && Arrays.equals(restored.taxis.get(0).path, game.taxis.get(0).path),
          "Active routes preserved");
      check(
          restored.chapter == 3
              && restored.reports.size() == 1
              && restored.seenNews.equals(game.seenNews),
          "Chapter and news archive preserved");
      check(
          restored.marginDebt == game.marginDebt
              && restored.companies.get(1).owned == 10
              && Arrays.equals(restored.corridorTier, game.corridorTier),
          "Network and shares preserved");
      game.tick(.2);
      restored.tick(.2);
      check(
          game.cash == restored.cash
              && game.rides == restored.rides
              && game.elapsed == restored.elapsed,
          "Save restores deterministic continuation");
      byte[] damaged = bytes.clone();
      damaged[damaged.length - 8] ^= 1;
      expectFailure(damaged);
      expectFailure(Arrays.copyOf(bytes, bytes.length - 1));
    }
    Game invalid = new Game();
    invalid.taxis.get(0).path = new int[] {99999};
    try {
      SaveCodec.encode(invalid);
      throw new AssertionError("Bad path accepted");
    } catch (IOException expected) {
    }
    Game nan = new Game();
    nan.elapsed = Double.NaN;
    try {
      SaveCodec.encode(nan);
      throw new AssertionError("NaN accepted");
    } catch (IOException expected) {
    }
    Game history = new Game();
    history.startHistoricalScenario(19);
    Game restored = SaveCodec.decode(SaveCodec.encode(history));
    check(
        restored.campaignStartYear == 2010 && restored.historicalScenarioId == 19,
        "Historical campaign persists");
    System.out.println(
        "PASS: six-city native save roundtrip, active routes, ledger, team, news, deterministic"
            + " continuation, checksum/truncation protection, invalid state rejection.");
  }

  private static void expectFailure(byte[] bytes) throws Exception {
    try {
      SaveCodec.decode(bytes);
      throw new AssertionError("Corrupt save accepted");
    } catch (IOException expected) {
    }
  }
}
