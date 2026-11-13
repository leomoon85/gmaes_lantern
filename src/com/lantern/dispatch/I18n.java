package com.lantern.dispatch;

import java.util.HashMap;
import java.util.Map;

/**
 * High-performance, offline internationalization catalog for Fareforge.
 * Supports the 8 most widely spoken global app languages:
 * English (en), Spanish (es), French (fr), German (de),
 * Japanese (ja), Simplified Chinese (zh), Portuguese (pt), and Arabic (ar).
 */
public final class I18n {

  public static final String[] SUPPORTED_CODES = {
    "en", "es", "fr", "de", "ja", "zh", "pt", "ar"
  };

  public static final String[] SUPPORTED_NAMES = {
    "English",
    "Español",
    "Français",
    "Deutsch",
    "日本語",
    "简体中文",
    "Português",
    "العربية"
  };

  private static final Map<String, Map<String, String>> DICTS = new HashMap<>();

  static {
    // 1. English (Base)
    Map<String, String> en = new HashMap<>();
    en.put("tab_city", "City");
    en.put("tab_fleet", "Fleet");
    en.put("tab_hubs", "Hubs");
    en.put("tab_team", "Team");
    en.put("tab_ledger", "Ledger");
    en.put("tab_market", "Market");
    en.put("tab_news", "News");
    en.put("menu", "Menu");
    en.put("continue", "Continue");
    en.put("quick_play", "Quick Play · Custom Entrepreneur Sandbox");
    en.put("historical_campaigns", "Historical Campaigns · 20 Scenarios");
    en.put("learn_to_play", "Learn to play · Fareforge Academy");
    en.put("daylight_map", "Switch to daylight map");
    en.put("night_map", "Switch to night map");
    en.put("music_on", "Music on · tap to mute");
    en.put("music_off", "Music off · tap to enable");
    en.put("sound_on", "Button sounds on · incident & cash chimes");
    en.put("sound_off", "Button sounds off · tap to enable");
    en.put("language_select", "Language / Idioma / 言語");
    en.put("dispatch", "Dispatch →");
    en.put("auto_on", "Auto on");
    en.put("manual", "Manual");
    en.put("fit_city", "Fit city");
    en.put("streets", "Streets");
    en.put("offline", "Offline");
    en.put("goals", "Goals");
    en.put("next_step", "Next step");
    en.put("claim_reward", "Claim reward");
    en.put("recruit_driver", "Recruit driver · 450 cr");
    en.put("train_driver", "Train");
    en.put("rest_driver", "Rest driver");
    en.put("hire_manager", "Hire manager");
    en.put("service_consist", "Overhaul mechanical condition");
    en.put("clean_consist", "Deep-clean interior");
    en.put("upgrade_comfort", "Upgrade cabin comfort");
    en.put("buy_consist", "+ Purchase Vehicle Consist");
    en.put("incorporate_venture", "+ Incorporate New Company / Spin-Off Venture");
    en.put("rename_company", "✎ Edit Entrepreneur & Company Names");
    en.put("issue_bond", "Issue bond · +5,000 cr");
    en.put("repay_bond", "Repay bond · 5,000 cr");
    en.put("repay_margin", "Repay margin borrowing");
    en.put("buy_shares", "Buy 10 shares");
    en.put("sell_shares", "Sell 10 shares");
    en.put("acquire_merge", "Acquire & Merge");
    en.put("board_vote", "Shareholder Resolution Vote");
    en.put("vote_yes", "Vote YES");
    en.put("vote_no", "Vote NO — Veto Proposal");
    en.put("rush_hour", "RUSH HOUR");
    en.put("clear_streets", "CLEAR STREETS");
    DICTS.put("en", en);

    // 2. Spanish (es)
    Map<String, String> es = new HashMap<>();
    es.put("tab_city", "Ciudad");
    es.put("tab_fleet", "Flota");
    es.put("tab_hubs", "Estaciones");
    es.put("tab_team", "Equipo");
    es.put("tab_ledger", "Libro");
    es.put("tab_market", "Mercado");
    es.put("tab_news", "Noticias");
    es.put("menu", "Menú");
    es.put("continue", "Continuar");
    es.put("quick_play", "Partida rápida · Modo libre");
    es.put("historical_campaigns", "Campañas históricas · 20 misiones");
    es.put("learn_to_play", "Aprender a jugar · Academia");
    es.put("daylight_map", "Cambiar a mapa diurno");
    es.put("night_map", "Cambiar a mapa nocturno");
    es.put("music_on", "Música activada · silenciar");
    es.put("music_off", "Música desactivada · activar");
    es.put("sound_on", "Sonidos activados · efectos y alertas");
    es.put("sound_off", "Sonidos desactivados · activar");
    es.put("language_select", "Idioma / Language (Español)");
    es.put("dispatch", "Despachar →");
    es.put("auto_on", "Auto activado");
    es.put("manual", "Manual");
    es.put("fit_city", "Centrar");
    es.put("streets", "Calles");
    es.put("offline", "Sin conexión");
    es.put("goals", "Objetivos");
    es.put("next_step", "Siguiente paso");
    es.put("claim_reward", "Reclamar recompensa");
    es.put("recruit_driver", "Contratar conductor · 450 cr");
    es.put("train_driver", "Capacitar");
    es.put("rest_driver", "Descansar conductor");
    es.put("hire_manager", "Contratar gerente");
    es.put("service_consist", "Reparar mecánica");
    es.put("clean_consist", "Limpiar cabina");
    es.put("upgrade_comfort", "Mejorar confort");
    es.put("buy_consist", "+ Comprar vehículo");
    es.put("incorporate_venture", "+ Fundar nueva empresa / filial");
    es.put("rename_company", "✎ Cambiar nombres de empresa y CEO");
    es.put("issue_bond", "Emitir bono · +5.000 cr");
    es.put("repay_bond", "Pagar bono · 5.000 cr");
    es.put("repay_margin", "Pagar deuda de margen");
    es.put("buy_shares", "Comprar 10 acciones");
    es.put("sell_shares", "Vender 10 acciones");
    es.put("acquire_merge", "Adquirir y fusionar");
    es.put("board_vote", "Votación de accionistas");
    es.put("vote_yes", "Votar SÍ");
    es.put("vote_no", "Votar NO — Vetar propuesta");
    es.put("rush_hour", "HORA PUNTA");
    es.put("clear_streets", "TRÁFICO FLUIDO");
    DICTS.put("es", es);

    // 3. French (fr)
    Map<String, String> fr = new HashMap<>();
    fr.put("tab_city", "Ville");
    fr.put("tab_fleet", "Flotte");
    fr.put("tab_hubs", "Gares");
    fr.put("tab_team", "Équipe");
    fr.put("tab_ledger", "Bilan");
    fr.put("tab_market", "Bourse");
    fr.put("tab_news", "Journal");
    fr.put("menu", "Menu");
    fr.put("continue", "Continuer");
    fr.put("quick_play", "Partie rapide · Mode libre");
    fr.put("historical_campaigns", "Campagnes historiques · 20 scénarios");
    fr.put("learn_to_play", "Apprendre à jouer · Académie");
    fr.put("daylight_map", "Passer en carte de jour");
    fr.put("night_map", "Passer en carte de nuit");
    fr.put("music_on", "Musique activée · couper");
    fr.put("music_off", "Musique coupée · activer");
    es.put("sound_on", "Sons activés · alertes");
    fr.put("sound_off", "Sons coupés · activer");
    fr.put("language_select", "Langue / Language (Français)");
    fr.put("dispatch", "Envoyer →");
    fr.put("auto_on", "Auto activé");
    fr.put("manual", "Manuel");
    fr.put("fit_city", "Recadrer");
    fr.put("streets", "Rues");
    fr.put("offline", "Hors ligne");
    fr.put("goals", "Objectifs");
    fr.put("next_step", "Étape suivante");
    fr.put("claim_reward", "Réclamer prime");
    fr.put("recruit_driver", "Recruter chauffeur · 450 cr");
    fr.put("train_driver", "Former");
    fr.put("rest_driver", "Mettre au repos");
    fr.put("hire_manager", "Engager directeur");
    fr.put("service_consist", "Révision mécanique");
    fr.put("clean_consist", "Désinfection cabine");
    fr.put("upgrade_comfort", "Améliorer confort");
    fr.put("buy_consist", "+ Acheter véhicule");
    fr.put("incorporate_venture", "+ Fonder une filiale");
    fr.put("rename_company", "✎ Modifier noms PDG & Entreprise");
    fr.put("issue_bond", "Émettre obligation · +5 000 cr");
    fr.put("repay_bond", "Rembourser obligation · 5 000 cr");
    fr.put("repay_margin", "Rembourser marge");
    fr.put("buy_shares", "Acheter 10 actions");
    fr.put("sell_shares", "Vendre 10 actions");
    fr.put("acquire_merge", "Acquérir et fusionner");
    fr.put("board_vote", "Vote des actionnaires");
    fr.put("vote_yes", "Voter OUI");
    fr.put("vote_no", "Voter NON — Rejeter");
    fr.put("rush_hour", "HEURE DE POINTE");
    fr.put("clear_streets", "TRAFIC FLUIDE");
    DICTS.put("fr", fr);

    // 4. German (de)
    Map<String, String> de = new HashMap<>();
    de.put("tab_city", "Stadt");
    de.put("tab_fleet", "Flotte");
    de.put("tab_hubs", "Bahnhöfe");
    de.put("tab_team", "Team");
    de.put("tab_ledger", "Bilanz");
    de.put("tab_market", "Markt");
    de.put("tab_news", "Zeitung");
    de.put("menu", "Menü");
    de.put("continue", "Fortsetzen");
    de.put("quick_play", "Schnellspiel · Freier Modus");
    de.put("historical_campaigns", "Historische Kampagnen · 20 Szenarien");
    de.put("learn_to_play", "Spielanleitung · Akademie");
    de.put("daylight_map", "Tagkarte aktivieren");
    de.put("night_map", "Nachtkarte aktivieren");
    de.put("music_on", "Musik an · stummschalten");
    de.put("music_off", "Musik aus · einschalten");
    de.put("sound_on", "Töne an · Signale");
    de.put("sound_off", "Töne aus · einschalten");
    de.put("language_select", "Sprache / Language (Deutsch)");
    de.put("dispatch", "Entsenden →");
    de.put("auto_on", "Auto an");
    de.put("manual", "Manuell");
    de.put("fit_city", "Zentrieren");
    de.put("streets", "Straßen");
    de.put("offline", "Offline");
    de.put("goals", "Ziele");
    de.put("next_step", "Nächster Schritt");
    de.put("claim_reward", "Belohnung einlösen");
    de.put("recruit_driver", "Fahrer einstellen · 450 cr");
    de.put("train_driver", "Schulen");
    de.put("rest_driver", "Pause gewähren");
    de.put("hire_manager", "Manager einstellen");
    de.put("service_consist", "Mechanische Wartung");
    de.put("clean_consist", "Innenraum reinigen");
    de.put("upgrade_comfort", "Komfort verbessern");
    de.put("buy_consist", "+ Fahrzeug kaufen");
    de.put("incorporate_venture", "+ Tochtergesellschaft gründen");
    de.put("rename_company", "✎ Namen von CEO & Firma anpassen");
    de.put("issue_bond", "Anleihe ausgeben · +5.000 cr");
    de.put("repay_bond", "Anleihe tilgen · 5.000 cr");
    de.put("repay_margin", "Margenschuld begleichen");
    de.put("buy_shares", "10 Aktien kaufen");
    de.put("sell_shares", "10 Aktien verkaufen");
    de.put("acquire_merge", "Übernehmen & Fusionieren");
    de.put("board_vote", "Aktionärsabstimmung");
    de.put("vote_yes", "Mit JA stimmen");
    de.put("vote_no", "Mit NEIN stimmen — Veto");
    de.put("rush_hour", "HAUPTVERKEHRSZEIT");
    de.put("clear_streets", "FREIE STRASSEN");
    DICTS.put("de", de);

    // 5. Japanese (ja)
    Map<String, String> ja = new HashMap<>();
    ja.put("tab_city", "都市");
    ja.put("tab_fleet", "車両");
    ja.put("tab_hubs", "駅・拠点");
    ja.put("tab_team", "チーム");
    ja.put("tab_ledger", "元帳");
    ja.put("tab_market", "株式市場");
    ja.put("tab_news", "新聞");
    ja.put("menu", "メニュー");
    ja.put("continue", "再開する");
    ja.put("quick_play", "クイックプレイ · サンドボックス");
    ja.put("historical_campaigns", "歴史的シナリオ · 全20編");
    ja.put("learn_to_play", "遊び方 · アカデミー");
    ja.put("daylight_map", "昼間マップに切替");
    ja.put("night_map", "夜間マップに切替");
    ja.put("music_on", "BGM オン · 消音");
    ja.put("music_off", "BGM オフ · 有効化");
    ja.put("sound_on", "効果音 オン · チャイム");
    ja.put("sound_off", "効果音 オフ · 有効化");
    ja.put("language_select", "言語選択 / Language (日本語)");
    ja.put("dispatch", "配車する →");
    ja.put("auto_on", "自動配車中");
    ja.put("manual", "手動");
    ja.put("fit_city", "全体表示");
    ja.put("streets", "街路");
    ja.put("offline", "オフライン");
    ja.put("goals", "目標");
    ja.put("next_step", "次のステップ");
    ja.put("claim_reward", "報酬を獲得");
    ja.put("recruit_driver", "運転手を雇用 · 450 cr");
    ja.put("train_driver", "訓練");
    ja.put("rest_driver", "休憩");
    ja.put("hire_manager", "幹部を雇用");
    ja.put("service_consist", "車両オーバーホール");
    ja.put("clean_consist", "車内清掃・消毒");
    ja.put("upgrade_comfort", "快適性を向上");
    ja.put("buy_consist", "+ 新車両を購入");
    ja.put("incorporate_venture", "+ 新会社・子会社を設立");
    ja.put("rename_company", "✎ CEOおよび会社名を変更");
    ja.put("issue_bond", "社債を発行 · +5,000 cr");
    ja.put("repay_bond", "社債を償還 · 5,000 cr");
    ja.put("repay_margin", "信用取引を返済");
    ja.put("buy_shares", "株式10株を購入");
    ja.put("sell_shares", "株式10株を売却");
    ja.put("acquire_merge", "買収・合併する");
    ja.put("board_vote", "株主総会決議");
    ja.put("vote_yes", "賛成票 (実行)");
    ja.put("vote_no", "反対票 (否決)");
    ja.put("rush_hour", "ラッシュアワー");
    ja.put("clear_streets", "通常運行");
    DICTS.put("ja", ja);

    // 6. Simplified Chinese (zh)
    Map<String, String> zh = new HashMap<>();
    zh.put("tab_city", "城市");
    zh.put("tab_fleet", "车队");
    zh.put("tab_hubs", "站点");
    zh.put("tab_team", "团队");
    zh.put("tab_ledger", "账本");
    zh.put("tab_market", "股市");
    zh.put("tab_news", "新闻");
    zh.put("menu", "菜单");
    zh.put("continue", "继续游戏");
    zh.put("quick_play", "快速开始 · 沙盒模式");
    zh.put("historical_campaigns", "历史战役 · 20大关卡");
    zh.put("learn_to_play", "新手教学 · 交通学院");
    zh.put("daylight_map", "切换到白天地图");
    zh.put("night_map", "切换到夜间地图");
    zh.put("music_on", "音乐开启 · 点击静音");
    zh.put("music_off", "音乐关闭 · 点击开启");
    zh.put("sound_on", "音效开启 · 提示音");
    zh.put("sound_off", "音效关闭 · 点击开启");
    zh.put("language_select", "语言选择 / Language (简体中文)");
    zh.put("dispatch", "调度出发 →");
    zh.put("auto_on", "自动调度开启");
    zh.put("manual", "手动调度");
    zh.put("fit_city", "重置视角");
    zh.put("streets", "街区");
    zh.put("offline", "离线模式");
    zh.put("goals", "目标");
    zh.put("next_step", "下一步");
    zh.put("claim_reward", "领取奖励");
    zh.put("recruit_driver", "招聘司机 · 450 cr");
    zh.put("train_driver", "培训");
    zh.put("rest_driver", "安排休息");
    zh.put("hire_manager", "聘请高管");
    zh.put("service_consist", "机械大修");
    zh.put("clean_consist", "深度清洁");
    zh.put("upgrade_comfort", "升级舒适度");
    zh.put("buy_consist", "+ 购置新车辆");
    zh.put("incorporate_venture", "+ 注册新公司 / 子公司");
    zh.put("rename_company", "✎ 修改CEO与公司名称");
    zh.put("issue_bond", "发行企业债券 · +5,000 cr");
    zh.put("repay_bond", "偿还债券 · 5,000 cr");
    zh.put("repay_margin", "偿还杠杆借款");
    zh.put("buy_shares", "买入10股");
    zh.put("sell_shares", "卖出10股");
    zh.put("acquire_merge", "并购与整合");
    zh.put("board_vote", "股东大会决议");
    zh.put("vote_yes", "投赞成票 (执行)");
    zh.put("vote_no", "投反对票 (否决)");
    zh.put("rush_hour", "高峰时段");
    zh.put("clear_streets", "交通顺畅");
    DICTS.put("zh", zh);

    // 7. Portuguese (pt)
    Map<String, String> pt = new HashMap<>();
    pt.put("tab_city", "Cidade");
    pt.put("tab_fleet", "Frota");
    pt.put("tab_hubs", "Estações");
    pt.put("tab_team", "Equipe");
    pt.put("tab_ledger", "Razão");
    pt.put("tab_market", "Mercado");
    pt.put("tab_news", "Notícias");
    pt.put("menu", "Menu");
    pt.put("continue", "Continuar");
    pt.put("quick_play", "Jogo Rápido · Modo Livre");
    pt.put("historical_campaigns", "Campanhas Históricas · 20 Cenários");
    pt.put("learn_to_play", "Como Jogar · Academia");
    pt.put("daylight_map", "Alternar para mapa diurno");
    pt.put("night_map", "Alternar para mapa noturno");
    pt.put("music_on", "Música ligada · silenciar");
    pt.put("music_off", "Música desligada · ativar");
    pt.put("sound_on", "Sons ligados · alertas e sinos");
    pt.put("sound_off", "Sons desligados · ativar");
    pt.put("language_select", "Idioma / Language (Português)");
    pt.put("dispatch", "Despachar →");
    pt.put("auto_on", "Auto ligado");
    pt.put("manual", "Manual");
    pt.put("fit_city", "Centralizar");
    pt.put("streets", "Ruas");
    pt.put("offline", "Offline");
    pt.put("goals", "Metas");
    pt.put("next_step", "Próximo passo");
    pt.put("claim_reward", "Resgatar recompensa");
    pt.put("recruit_driver", "Contratar motorista · 450 cr");
    pt.put("train_driver", "Treinar");
    pt.put("rest_driver", "Descansar motorista");
    pt.put("hire_manager", "Contratar gerente");
    pt.put("service_consist", "Revisão mecânica");
    pt.put("clean_consist", "Higienizar cabine");
    pt.put("upgrade_comfort", "Melhorar conforto");
    pt.put("buy_consist", "+ Comprar veículo");
    pt.put("incorporate_venture", "+ Fundar nova empresa / filial");
    pt.put("rename_company", "✎ Renomear empresa e CEO");
    pt.put("issue_bond", "Emitir título · +5.000 cr");
    pt.put("repay_bond", "Quitar título · 5.000 cr");
    pt.put("repay_margin", "Pagar dívida de margem");
    pt.put("buy_shares", "Comprar 10 ações");
    pt.put("sell_shares", "Vender 10 ações");
    pt.put("acquire_merge", "Adquirir e fundir");
    pt.put("board_vote", "Votação de acionistas");
    pt.put("vote_yes", "Votar SIM");
    pt.put("vote_no", "Votar NÃO — Vetar proposta");
    pt.put("rush_hour", "HORA DO RUSH");
    pt.put("clear_streets", "TRÂNSITO LIVRE");
    DICTS.put("pt", pt);

    // 8. Arabic (ar)
    Map<String, String> ar = new HashMap<>();
    ar.put("tab_city", "المدينة");
    ar.put("tab_fleet", "الأسطول");
    ar.put("tab_hubs", "المحطات");
    ar.put("tab_team", "الفريق");
    ar.put("tab_ledger", "الدفتر");
    ar.put("tab_market", "السوق");
    ar.put("tab_news", "الأخبار");
    ar.put("menu", "القائمة");
    ar.put("continue", "استمرار");
    ar.put("quick_play", "لعب سريع · وضع حر");
    ar.put("historical_campaigns", "حملات تاريخية · 20 سيناريو");
    ar.put("learn_to_play", "تعلم اللعب · الأكاديمية");
    ar.put("daylight_map", "خريطة نهارية");
    ar.put("night_map", "خريطة ليلية");
    ar.put("music_on", "الموسيقى مشغلة · كتم");
    ar.put("music_off", "الموسيقى مطفأة · تشغيل");
    ar.put("sound_on", "الأصوات مشغلة · نغمات التنبيه");
    ar.put("sound_off", "الأصوات مطفأة · تشغيل");
    ar.put("language_select", "اختيار اللغة / Language (العربية)");
    ar.put("dispatch", "إرسال الرحلة →");
    ar.put("auto_on", "إرسال تلقائي");
    ar.put("manual", "يدوي");
    ar.put("fit_city", "ملاءمة الشاشة");
    ar.put("streets", "الشوارع");
    ar.put("offline", "بدون إنترنت");
    ar.put("goals", "الأهداف");
    ar.put("next_step", "الخطوة التالية");
    ar.put("claim_reward", "استلام المكافأة");
    ar.put("recruit_driver", "توظيف سائق · 450 cr");
    ar.put("train_driver", "تدريب");
    ar.put("rest_driver", "إراحة السائق");
    ar.put("hire_manager", "توظيف مدير");
    ar.put("service_consist", "صيانة ميكانيكية");
    ar.put("clean_consist", "تنظيف المقصورة");
    ar.put("upgrade_comfort", "ترقية الراحة");
    ar.put("buy_consist", "+ شراء مركبة جديدة");
    ar.put("incorporate_venture", "+ تأسيس شركة فرعية جديدة");
    ar.put("rename_company", "✎ تعديل أسماء الشركة والمدير");
    ar.put("issue_bond", "إصدار سندات · +5,000 cr");
    ar.put("repay_bond", "سداد السندات · 5,000 cr");
    ar.put("repay_margin", "سداد الهامش");
    ar.put("buy_shares", "شراء 10 أسهم");
    ar.put("sell_shares", "بيع 10 أسهم");
    ar.put("acquire_merge", "استحواذ ودمج");
    ar.put("board_vote", "تصويت المساهمين");
    ar.put("vote_yes", "تصويت بنعم");
    ar.put("vote_no", "تصويت بلا — رفض المقترح");
    ar.put("rush_hour", "ساعة الذروة");
    ar.put("clear_streets", "شوارع سالكة");
    DICTS.put("ar", ar);
  }

  public static String get(String langCode, String key) {
    if (langCode == null) langCode = "en";
    Map<String, String> dict = DICTS.get(langCode.toLowerCase());
    if (dict == null) dict = DICTS.get("en");
    String val = dict.get(key);
    if (val != null) return val;
    // Fallback to English
    Map<String, String> en = DICTS.get("en");
    return en != null && en.containsKey(key) ? en.get(key) : key;
  }

  public static String get(String key) {
    return get("en", key);
  }

  public static boolean isSupported(String code) {
    if (code == null) return false;
    for (String c : SUPPORTED_CODES) {
      if (c.equalsIgnoreCase(code)) return true;
    }
    return false;
  }
}
