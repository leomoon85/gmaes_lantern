# Historical context and invented simulation effects

## Current WebView newspaper dataset

The current implementation uses `assets/history-data.js` as the source of truth for 14 sourced historical event records and 11 selected historical share-price observations. Each record includes its event/observation date and a source URL. Publication dates are recorded separately for retrospective reports. The newspaper unlocks records by campaign date; the city timeline uses this dataset instead of its older embedded headlines.

Real companies may be named in historical reporting. The playable rivals remain fictional. Historical quotes are reference observations in USD, not tradable rival prices, not live quotes, and not a full daily series. The dataset includes Lyft's US$72 IPO offer and Uber's US$45 IPO offer, eight Lyft quarter-end closes through March 2021, and Uber's December 2020 quarter-end close. Quote types remain distinct. The bank pricing supplements reproduce historical market tables and can reflect corporate-action adjustments; SEC hosting is not an endorsement or independent validation by the SEC.

Examples of corrections: New York's congestion-zone toll launch is dated 5 January 2025; the November 2022 Namma Yatri launch record does not claim a ₹25/day subscription launch fee; the game's one-time 52% fictional rival price reduction is explicitly separated from real share returns. Legacy vehicle/tariff descriptions have not been comprehensively verified.

The original Java prototype notes below describe the earlier implementation and should not be read as a specification of the current WebView dataset.

The campaign draws on public historical facts, paraphrased here. The playable rivals are fictional and do not stand for particular real businesses. Historical company names and logos do not appear in the game. Source websites necessarily identify their own subjects; those references do not imply endorsement or a licence to use their branding.

| Period | Documented context | Invented campaign interpretation |
|---|---|---|
| 2011 | Starting year chosen for the fictional business | One partner, two local service districts, 12,000 credits |
| 2013 | An international app-based cab service tested in Bengaluru in August and formally announced launch in October | Competitive entry milestone; no assertion that all Indian mobility firms began in Bengaluru |
| 2015 | Bike-taxi and employee-carpooling businesses emerged; the cab market saw a significant acquisition | Corporate-commute model unlock and 15% reduction in fictional driver onboarding costs |
| 2020 | Nationwide lockdown was announced on 24 March | Demand multiplier 0.22 in 2020, 0.65 in 2021; one-time 52% reduction in fictional rival share prices |
| 2022 | A driver-direct auto-booking platform launched in Bengaluru in November | Driver-subscription business model unlock |
| 2025 | Campaign calendar stops advancing; sandbox continues | Not a claim about present-day market conditions |

Sources for historical context:

- [August 2013 Bengaluru test announcement](https://www.uber.com/in/en/blog/hello-india-secret-ubers-have-arrived-in-bangalore/).
- [October 2013 formal Bengaluru launch announcement](https://www.uber.com/in/en/newsroom/namma-bengaluru-namma-uber-were-officially-in-bangalore/).
- [Bike-taxi company's own account of its 2015 inception](https://www.rapido.bike/About).
- [Carpooling company's account of its beginnings in 2015](https://quickride.in/about-us.php).
- [Contemporaneous March 2015 acquisition report](https://www.business-standard.com/article/reuters/ola-buys-rival-taxiforsure-for-200-million-115030200466_1.html).
- [Government announcement of nationwide lockdown, 24 March 2020](https://www.pib.gov.in/PressReleasePage.aspx?PRID=1608009&lang=2&reg=48).
- [Contemporaneous report of the November 2022 driver-direct launch](https://www.news9live.com/state/karnataka/namma-yatri-auto-union-from-bengaluru-enters-ride-hailing-biz-with-new-app-205343).

Game consequences are design choices, not quantitative findings about these events. No actual company valuations, private funding terms, investment returns, stock-price histories or merger outcomes are represented. Fictional exchange access is deliberately available from the start; it is not a claim that the historical companies were publicly tradable.

The prototype abstracts city expansion into branch launches. It does not yet reconstruct each real operator's state-by-state growth, legal proceedings, licensing history, workforce arrangements or funding rounds. Those would require additional sourced research and design work. It also does not model current transport regulations.
