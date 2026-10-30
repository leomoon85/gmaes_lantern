package com.lantern.dispatch;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Offline, attributed historical records. Economic effects are labelled separately. */
final class NativeNews {
  private final JSONArray events, quotes;

  NativeNews(Context context) throws IOException, JSONException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (InputStream input = context.getAssets().open("history.json")) {
      byte[] buffer = new byte[4096];
      int n;
      while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
    }
    JSONObject data = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
    events = data.getJSONArray("events");
    quotes = data.getJSONArray("quotes");
  }

  static String date(Game game) {
    int y = game.year();
    GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
    calendar.clear();
    calendar.set(y, Calendar.JANUARY, 1);
    int days = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);
    int day =
        game.campaignStartYear + (int) (game.elapsed / 60) > game.campaignEndYear
            ? days - 1
            : (int) ((game.elapsed % 60) / 60 * days);
    calendar.add(Calendar.DAY_OF_YEAR, day);
    return String.format(
        Locale.US,
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH));
  }

  void collect(Game game) {
    String today = date(game);
    try {
      for (int i = 0; i < events.length(); i++) {
        JSONObject event = events.getJSONObject(i);
        String published = event.optString("published", event.getString("date"));
        if (published.compareTo(today) > 0 || !matches(event.opt("cities"), game.scenarioId))
          continue;
        add(
            game,
            new Game.Report(
                "real-" + event.getString("id"),
                event.getString("date"),
                event.getString("title"),
                event.getString("body"),
                event.getString("angle"),
                event.getString("url"),
                event.getString("source")
                    + (event.has("published") ? " · Report published " + published : ""),
                "Historical record"));
      }
      for (int i = 0; i < quotes.length(); i++) {
        JSONObject quote = quotes.getJSONObject(i);
        String date = quote.getString("date");
        if (date.compareTo(today) > 0) continue;
        String symbol = quote.getString("symbol"),
            price = String.format(Locale.US, "US$%.2f", quote.getDouble("price"));
        add(
            game,
            new Game.Report(
                "quote-" + symbol + "-" + date,
                date,
                symbol + " · " + price,
                quote.getString("exchange")
                    + ":"
                    + symbol
                    + " · "
                    + quote.getString("type")
                    + "\n\nRecorded price: "
                    + price
                    + " per share.",
                "Historical USD observation. IPO offering prices differ from market closes."
                    + " Fictional rival shares use a separate simulation economy.",
                quote.getString("url"),
                quote.getString("source"),
                "Historical share price"));
      }
    } catch (JSONException e) {
      game.message = "One historical record could not be read.";
    }
    if (game.elapsed >= 8)
      add(
          game,
          new Game.Report(
              "opening",
              today,
              "A new name on the city streets",
              "Lantern has entered "
                  + game.currentScenario().city
                  + ". A single vehicle is the beginning of a network.",
              "Serve your first trips, improve the service, then connect another hub. Your"
                  + " company’s journey is yours to shape.",
              "",
              "Lantern company desk",
              "Your campaign"));
    if (game.marginCalls > 0)
      financial(
          game,
          "margin-" + game.marginCalls,
          "Broker liquidates Lantern’s shares",
          "Your portfolio fell below the margin equity requirement. The shares were sold to settle"
              + " borrowing.");
    if (game.restructurings > 0)
      financial(
          game,
          "restructure-" + game.restructurings,
          "Lantern enters emergency restructuring",
          "Creditors intervened after your operating cash fell too far. Review costs and debt"
              + " before expanding.");
    for (Game.Company company : game.companies)
      if (company.merged)
        financial(
            game,
            "merger-" + company.name,
            "Lantern acquires " + company.name,
            "The rival’s available fleet, treasury, and hub have been integrated into your"
                + " company.");
    if (game.scenarioWon)
      financial(
          game,
          "victory",
          "A company worth celebrating",
          "Your campaign objective is complete. An 8,000 cr award has been added to your balance."
              + " You can continue playing.");
    Collections.sort(game.reports, (a, b) -> b.date.compareTo(a.date));
  }

  private void financial(Game g, String id, String title, String body) {
    Game.Report report =
        new Game.Report(
            id,
            date(g),
            title,
            body,
            "Cash: "
                + Game.money(g.cash)
                + " · Margin debt: "
                + Game.money(g.marginDebt)
                + " · Bonds: "
                + Game.money(g.bondDebt()),
            "",
            "Company ledger",
            "Your campaign");
    report.urgent = true;
    add(g, report);
  }

  private void add(Game game, Game.Report report) {
    if (game.seenNews.contains(report.id)) return;
    if (!report.url.isEmpty() && report.date.compareTo(game.campaignStartYear + "-01-01") < 0)
      report.read = true;
    game.seenNews.add(report.id);
    if (game.seenNews.size() > 150) {
      for (Iterator<String> it = game.seenNews.iterator(); it.hasNext(); ) {
        String id = it.next();
        if (id.startsWith("margin-") || id.startsWith("restructure-")) {
          it.remove();
          break;
        }
      }
    }
    game.reports.add(report);
    if (game.reports.size() > 80) {
      int oldest = 0;
      for (int i = 0; i < game.reports.size(); i++)
        if (game.reports.get(i).date.compareTo(game.reports.get(oldest).date) < 0) oldest = i;
      game.reports.remove(oldest);
    }
  }

  private boolean matches(Object cities, int id) throws JSONException {
    if ("all".equals(cities)) return true;
    if (cities instanceof JSONArray) {
      JSONArray list = (JSONArray) cities;
      for (int i = 0; i < list.length(); i++) if (list.getInt(i) == id) return true;
    }
    return false;
  }
}
