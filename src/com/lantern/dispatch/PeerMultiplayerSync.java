package com.lantern.dispatch;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * PeerSplit-style serverless multiplayer sync manager and encrypted cloud backup broker.
 *
 * Encapsulates:
 * 1. Encrypted Save Export & Import (Zero-Server Backup & Restore using PeerSplit E2EE).
 * 2. Room Code Multiplayer Exchange: Generates/Consumes E2EE Peer Company Envelopes
 *    (import rival players as live competitors in your world).
 * 3. Nostr Relay event formatting (NIP-04/NIP-44 encrypted events for WebSocket sync).
 */
public final class PeerMultiplayerSync {

  /** Metadata for public Nostr relays used in decentralized P2P pass-through. */
  public static final String[] DEFAULT_NOSTR_RELAYS = {
    "wss://relay.damus.io",
    "wss://nos.lol",
    "wss://relay.primal.net"
  };

  /** Summary of an incoming rival company imported from a multiplayer peer. */
  public static final class PeerRivalProfile {
    public final String founderName;
    public final String companyName;
    public final String city;
    public final long netWorth;
    public final long cash;
    public final int fleetCount;
    public final double rating;
    public final int founderEquityPct;

    public PeerRivalProfile(
        String founderName,
        String companyName,
        String city,
        long netWorth,
        long cash,
        int fleetCount,
        double rating,
        int founderEquityPct) {
      this.founderName = founderName;
      this.companyName = companyName;
      this.city = city;
      this.netWorth = netWorth;
      this.cash = cash;
      this.fleetCount = fleetCount;
      this.rating = rating;
      this.founderEquityPct = founderEquityPct;
    }
  }

  private PeerMultiplayerSync() {}

  /**
   * Creates an End-to-End Encrypted (AES-GCM-256) portable backup envelope of the game.
   * This string can be stored anywhere (private Nostr event, user notes, pasteboard)
   * with zero central server involvement.
   */
  public static String exportEncryptedBackup(Game game, String passphrase) throws Exception {
    byte[] rawSave = SaveCodec.encode(game);
    byte[] key = PeerSyncCrypto.deriveKey(passphrase);
    byte[] encrypted = PeerSyncCrypto.encrypt(rawSave, key);
    return PeerSyncCrypto.toArmor("ENCRYPTED BACKUP", encrypted);
  }

  /**
   * Restores a game from an E2EE PeerSplit-style backup envelope.
   */
  public static Game importEncryptedBackup(String armoredBackup, String passphrase) throws Exception {
    byte[] encrypted = PeerSyncCrypto.fromArmor(armoredBackup);
    byte[] key = PeerSyncCrypto.deriveKey(passphrase);
    byte[] rawSave = PeerSyncCrypto.decrypt(encrypted, key);
    return SaveCodec.decode(rawSave);
  }

  /**
   * Exports an encrypted "Peer Company Snapshot" for multiplayer matchmaking.
   * Player 1 shares their generated Room Code and this snapshot.
   * Contains the player's flagship company, fleet size, treasury, valuation, and rating.
   */
  public static String exportMultiplayerRoomEnvelope(Game game, String roomCode) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    out.writeInt(0x50454552); // "PEER" magic
    out.writeInt(1); // Version
    out.writeUTF(game.founderName);
    out.writeUTF(game.companyName);
    out.writeUTF(game.currentScenario().city);
    out.writeLong(game.companyNetWorth());
    out.writeLong(game.cash);
    out.writeInt(game.taxis.size());
    out.writeDouble(game.rating);
    out.writeInt(game.founderEquityPct);
    out.flush();

    byte[] key = PeerSyncCrypto.deriveKey(roomCode);
    byte[] encrypted = PeerSyncCrypto.encrypt(baos.toByteArray(), key);
    return PeerSyncCrypto.toArmor("MULTIPLAYER ROOM", encrypted);
  }

  /**
   * Decrypts and inspects a peer company from another player's Room Envelope.
   */
  public static PeerRivalProfile parsePeerRival(String armoredRoom, String roomCode) throws Exception {
    byte[] encrypted = PeerSyncCrypto.fromArmor(armoredRoom);
    byte[] key = PeerSyncCrypto.deriveKey(roomCode);
    byte[] raw = PeerSyncCrypto.decrypt(encrypted, key);

    DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw));
    int magic = in.readInt();
    if (magic != 0x50454552) {
      throw new IOException("Not a valid Fareforge peer envelope");
    }
    int version = in.readInt();
    String founder = in.readUTF();
    String company = in.readUTF();
    String city = in.readUTF();
    long netWorth = in.readLong();
    long cash = in.readLong();
    int fleet = in.readInt();
    double rating = in.readDouble();
    int equity = in.readInt();

    return new PeerRivalProfile(founder, company, city, netWorth, cash, fleet, rating, equity);
  }

  /**
   * Injects the rival human player's company into the current game's Market tab,
   * replacing one of the AI rival companies with the actual peer company!
   */
  public static boolean injectRivalIntoGame(Game game, PeerRivalProfile rival) {
    if (game == null || rival == null || game.companies.isEmpty()) return false;
    // Replace rival #1 or #2 with the peer
    Game.Company slot = game.companies.get(0);
    slot.name = rival.companyName + " (👤 " + rival.founderName + ")";
    slot.price = (int) Math.max(15, Math.min(180, rival.netWorth / 250L));
    slot.fleet = Math.max(1, Math.min(Game.MAX_FLEET, rival.fleetCount));
    slot.cash = rival.cash;
    slot.owned = 0;
    slot.merged = false;

    game.pendingSoundCue = Game.CUE_FANFARE;
    game.message = "Multiplayer: 👤 " + rival.founderName + "'s " + rival.companyName + " entered the market!";
    return true;
  }

  /**
   * Formats a Nostr NIP-01/NIP-04 WebSocket event JSON string to publish to relays.
   */
  public static String createNostrPublishEventJson(String topicTag, String encryptedPayloadHex) {
    long nowSec = System.currentTimeMillis() / 1000L;
    // Nostr event kind 4 (direct message) or ephemeral kind 20000
    return "[\"EVENT\",{"
        + "\"kind\":20000,"
        + "\"created_at\":" + nowSec + ","
        + "\"tags\":[[\"t\",\"" + topicTag + "\"],[\"app\",\"fareforge-peersplit\"]],"
        + "\"content\":\"" + encryptedPayloadHex + "\","
        + "\"pubkey\":\"0000000000000000000000000000000000000000000000000000000000000000\","
        + "\"id\":\"0000000000000000000000000000000000000000000000000000000000000000\","
        + "\"sig\":\"00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000\""
        + "}]";
  }

  /**
   * Formats a Nostr REQ filter JSON string to subscribe to a room's events over WebSockets.
   */
  public static String createNostrSubscriptionJson(String subscriptionId, String topicTag) {
    return "[\"REQ\",\"" + subscriptionId + "\",{\"kinds\":[20000],\"#t\":[\"" + topicTag + "\"],\"limit\":10}]";
  }
}
