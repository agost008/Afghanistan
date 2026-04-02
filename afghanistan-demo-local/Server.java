import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 8787;
    private static final Path PUBLIC_DIR = Path.of("public").toAbsolutePath().normalize();
    private static final List<Article> ARTICLES = buildArticles();
    private static final Map<String, String> COUNTRY_INFO = buildCountryInfo();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/summary", Server::handleSummary);
        server.createContext("/api/articles", Server::handleArticles);
        server.createContext("/assets/", new StaticFileHandler(PUBLIC_DIR));
        server.createContext("/styles.css", new StaticFileHandler(PUBLIC_DIR));
        server.createContext("/script.js", new StaticFileHandler(PUBLIC_DIR));
        server.createContext("/article.js", new StaticFileHandler(PUBLIC_DIR));
        server.createContext("/article.html", new StaticFileHandler(PUBLIC_DIR));
        server.createContext("/", Server::handleRoot);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Server avviato su http://localhost:" + PORT);
        System.out.println("Apri il browser e visita la home.");
    }

    private static void handleRoot(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.equals("/index.html")) {
            Path file = PUBLIC_DIR.resolve("index.html");
            writeBytes(exchange, 200, Files.readAllBytes(file), contentType(file));
            return;
        }
        Path resolved = PUBLIC_DIR.resolve(path.substring(1)).normalize();
        if (!resolved.startsWith(PUBLIC_DIR) || !Files.exists(resolved) || Files.isDirectory(resolved)) {
            writeText(exchange, 404, "404 - Risorsa non trovata", "text/plain; charset=utf-8");
            return;
        }
        writeBytes(exchange, 200, Files.readAllBytes(resolved), contentType(resolved));
    }

    private static void handleSummary(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            writeText(exchange, 405, "Metodo non consentito", "text/plain; charset=utf-8");
            return;
        }
        String json = buildSummaryJson();
        writeText(exchange, 200, json, "application/json; charset=utf-8");
    }

    private static void handleArticles(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            writeText(exchange, 405, "Metodo non consentito", "text/plain; charset=utf-8");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/api/articles")) {
            writeText(exchange, 200, buildArticlesListJson(), "application/json; charset=utf-8");
            return;
        }

        if (path.startsWith("/api/articles/")) {
            String id = URLDecoder.decode(path.substring("/api/articles/".length()), StandardCharsets.UTF_8);
            Article match = ARTICLES.stream().filter(a -> a.id.equals(id)).findFirst().orElse(null);
            if (match == null) {
                writeText(exchange, 404, "{\"error\":\"Articolo non trovato\"}", "application/json; charset=utf-8");
                return;
            }
            writeText(exchange, 200, buildArticleJson(match), "application/json; charset=utf-8");
            return;
        }

        writeText(exchange, 404, "{\"error\":\"Endpoint non trovato\"}", "application/json; charset=utf-8");
    }

    private static String buildSummaryJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(jsonPair("query", "Afghanistan")).append(",");
        sb.append(jsonPair("countryLabel", "Paese")).append(",");
        sb.append(jsonPair("heroImage", "assets/hero-afghanistan.svg")).append(",");
        sb.append(jsonPair("mapImage", "assets/map-afghanistan.svg")).append(",");
        sb.append(jsonPair("flagLabel", "Afghanistan")).append(",");
        sb.append(jsonPair("flagEmoji", "🇦🇫")).append(",");
        sb.append(jsonPair("travelTime", "Quadro generale")).append(",");
        sb.append(jsonPair("travelSub", "Panoramica sintetica del Paese e delle principali restrizioni")).append(",");
        sb.append(jsonPair("intro", "Afghanistan. La pagina riunisce una scheda sintetica sul Paese e una sezione di notizie dedicate alle principali restrizioni imposte o consolidate sotto il regime talebano, con particolare attenzione alla legge del 2024 su virtù e vizio.")).append(",");
        sb.append("\"quickCards\":[");
        sb.append("{\"title\":\"Legge 2024\",\"subtitle\":\"Articoli 13 e 17\",\"image\":\"assets/cover-law-2024.svg\"},");
        sb.append("{\"title\":\"Scuola femminile\",\"subtitle\":\"Oltre la sesta classe\",\"image\":\"assets/cover-school-ban.svg\"},");
        sb.append("{\"title\":\"Media\",\"subtitle\":\"Censura e immagini\",\"image\":\"assets/cover-media.svg\"},");
        sb.append("{\"title\":\"Lavoro e università\",\"subtitle\":\"Divieti e restrizioni\",\"image\":\"assets/cover-work-university.svg\"}");
        sb.append("],");
        sb.append("\"info\":{");
        int i = 0;
        for (Map.Entry<String, String> entry : COUNTRY_INFO.entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append(jsonPair(entry.getKey(), entry.getValue()));
        }
        sb.append("}");
        sb.append("}");
        return sb.toString();
    }

    private static String buildArticlesListJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < ARTICLES.size(); i++) {
            if (i > 0) sb.append(",");
            Article a = ARTICLES.get(i);
            sb.append("{")
              .append(jsonPair("id", a.id)).append(",")
              .append(jsonPair("title", a.title)).append(",")
              .append(jsonPair("subtitle", a.subtitle)).append(",")
              .append(jsonPair("date", a.date)).append(",")
              .append(jsonPair("source", a.source)).append(",")
              .append(jsonPair("summary", a.summary)).append(",")
              .append(jsonPair("image", a.image)).append(",")
              .append(jsonPair("accent", a.accent))
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String buildArticleJson(Article a) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append(jsonPair("id", a.id)).append(",")
          .append(jsonPair("title", a.title)).append(",")
          .append(jsonPair("subtitle", a.subtitle)).append(",")
          .append(jsonPair("date", a.date)).append(",")
          .append(jsonPair("source", a.source)).append(",")
          .append(jsonPair("summary", a.summary)).append(",")
          .append(jsonPair("image", a.image)).append(",")
          .append(jsonPair("evidenceImage", a.evidenceImage)).append(",")
          .append(jsonPair("accent", a.accent)).append(",")
          .append(jsonPair("tag", a.tag)).append(",")
          .append("\"paragraphs\":[");
        for (int i = 0; i < a.paragraphs.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(jsonString(a.paragraphs.get(i)));
        }
        sb.append("],\"highlights\":[");
        for (int i = 0; i < a.highlights.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(jsonString(a.highlights.get(i)));
        }
        sb.append("],\"timeline\":[");
        for (int i = 0; i < a.timeline.size(); i++) {
            if (i > 0) sb.append(",");
            TimelineItem t = a.timeline.get(i);
            sb.append("{").append(jsonPair("when", t.when)).append(",").append(jsonPair("what", t.what)).append("}");
        }
        sb.append("],\"sources\":[");
        for (int i = 0; i < a.sources.size(); i++) {
            if (i > 0) sb.append(",");
            SourceLink s = a.sources.get(i);
            sb.append("{").append(jsonPair("label", s.label)).append(",").append(jsonPair("url", s.url)).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String jsonPair(String key, String value) {
        return jsonString(key) + ":" + jsonString(value == null ? "" : value);
    }

    private static String jsonString(String value) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static void writeText(HttpExchange exchange, int status, String text, String contentType) throws IOException {
        writeBytes(exchange, status, text.getBytes(StandardCharsets.UTF_8), contentType);
    }

    private static void writeBytes(HttpExchange exchange, int status, byte[] bytes, String contentType) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    private static Map<String, String> buildCountryInfo() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("Capitale", "Kabul");
        info.put("Governo", "Emirato islamico de facto / non riconosciuto internazionalmente");
        info.put("Lingue ufficiali", "Pashto, Dari");
        info.put("Valuta", "Afghani (AFN)");
        info.put("Continente", "Asia");
        info.put("Popolazione", "circa 43 milioni (dato 2024, fonte Banca Mondiale)");
        info.put("PIL", "circa 17,15 miliardi USD (dato 2023, fonte Banca Mondiale)");
        return info;
    }

    private static List<Article> buildArticles() {
        List<Article> list = new ArrayList<>();

        list.add(new Article(
                "legge-2024",
                "Legge del 2024: i punti principali",
                "Articoli 13 e 17, restrizioni su donne e media",
                "ONU / OHCHR / UNAMA",
                "2024–2025",
                "La legge del 2024 su virtù e vizio è uno dei testi più citati per spiegare il rafforzamento del controllo sociale in Afghanistan. I passaggi più discussi riguardano l'articolo 13 su corpo, volto e voce delle donne e l'articolo 17 sui media e sulle immagini di esseri viventi.",
                "assets/cover-law-2024.svg",
                "assets/detail-law-2024.svg",
                "blue",
                "Legge 2024",
                List.of(
                        "Il relatore speciale dell'ONU descrive la legge come un testo che codifica e consolida molte restrizioni già imposte dopo il ritorno dei Talebani al potere. Per questo il provvedimento viene spesso considerato un passaggio chiave nella formalizzazione del controllo sociale.",
                        "Per le donne, il punto più discusso è l'articolo 13: secondo i report ONU, quando una donna esce di casa deve coprire corpo e volto davanti a uomini non parenti stretti e non dovrebbe alzare la voce in pubblico.",
                        "Per i media, il riferimento centrale è l'articolo 17: la norma vieta contenuti giudicati contrari alla sharia e richiama il divieto di pubblicare immagini di esseri viventi, con effetti su televisione, giornalismo e comunicazione visiva.",
                        "I report internazionali sottolineano anche il potere molto ampio lasciato alle autorità incaricate di far rispettare la norma, con possibilità di fermo, sanzione e rinvio ai tribunali."
                ),
                List.of(
                        "Articolo 13: corpo e volto coperti davanti a uomini non parenti stretti.",
                        "Articolo 13: forti limitazioni alla voce femminile in pubblico.",
                        "Articolo 17: divieto di immagini di esseri viventi e controllo sui media.",
                        "Ampio margine di applicazione alle autorità di controllo."
                ),
                List.of(
                        new TimelineItem("21 ago 2024", "Pubblicazione della legge su virtù e vizio."),
                        new TimelineItem("ago–set 2024", "Partono critiche ONU e ONG per l'impatto su donne, media e libertà personali."),
                        new TimelineItem("feb 2025", "Il relatore speciale ONU pubblica uno studio dettagliato sulla legge.")
                ),
                List.of(
                        new SourceLink("OHCHR – Study on the PVPV law", "https://www.ohchr.org/sites/default/files/2025-02/a-hrc-58-74-pvpv-study.pdf"),
                        new SourceLink("UNAMA – Report on implementation and effects", "https://unama.unmissions.org/sites/default/files/unama_pvpv_report_10_april_2025_english.pdf")
                )
        ));

        list.add(new Article(
                "media-articolo-17",
                "Media e articolo 17: il divieto delle immagini di esseri viventi",
                "TV e informazione colpite direttamente dalla legge",
                "UNAMA / AP / Reuters",
                "2024–2025",
                "Secondo i report ONU, la legge vieta immagini o video di esseri viventi e impedisce ai media di pubblicare contenuti ritenuti contrari alla sharia. In alcune province questo ha portato a stop delle trasmissioni televisive o a palinsesti senza immagini.",
                "assets/cover-media.svg",
                "assets/detail-media.svg",
                "slate",
                "Media",
                List.of(
                        "Il tema dei media mostra che la legge del 2024 non riguarda solo l'abbigliamento femminile, ma anche la circolazione delle informazioni. Il rapporto ONU collega l'articolo 17 a un forte restringimento della libertà di espressione.",
                        "I report internazionali descrivono una norma che incide su giornalismo, televisione, fotografia e comunicazione visiva, ampliando il controllo sul contenuto diffuso al pubblico.",
                        "UNAMA segnala che in alcune province varie emittenti hanno smesso di mostrare immagini di persone. Agenzie come AP e Reuters hanno poi raccontato casi di media costretti a fermarsi o a cambiare completamente formato per adeguarsi alla norma.",
                        "L'articolo 17 è diventato uno dei riferimenti principali per spiegare come la censura visiva si sia estesa oltre le singole redazioni e abbia inciso sulla presenza stessa delle immagini nello spazio pubblico."
                ),
                List.of(
                        "Divieto di immagini o video di esseri viventi.",
                        "Possibili stop o sospensioni per emittenti televisive in alcune province.",
                        "La norma colpisce giornalismo, fotografia, arte e pubblicità.",
                        "Secondo l'ONU, i testi sono vaghi e lasciano molto arbitrio nell'applicazione."
                ),
                List.of(
                        new TimelineItem("ago 2024", "La legge formalizza il divieto sulle immagini di esseri viventi."),
                        new TimelineItem("ott 2024", "AP riferisce che alcune aree iniziano a bloccare media con immagini di persone."),
                        new TimelineItem("dic 2024", "UNAMA segnala che in province come Kandahar alcune TV chiudono o sospendono il video."),
                        new TimelineItem("apr 2025", "Nuovo report UNAMA descrive l'impatto concreto della norma sui media.")
                ),
                List.of(
                        new SourceLink("UNAMA – Media Freedom in Afghanistan", "https://unama.unmissions.org/sites/default/files/unama_report_on_media_freedom_in_afghanistan.pdf"),
                        new SourceLink("UNAMA – PVPV report 2025", "https://unama.unmissions.org/sites/default/files/unama_pvpv_report_10_april_2025_english.pdf"),
                        new SourceLink("AP – province bans media from showing images", "https://apnews.com/article/afghanistan-taliban-media-morality-laws-living-things-b39d42fdceb5317f62a145b7092c9384"),
                        new SourceLink("Reuters – morality laws codified in 2024", "https://www.reuters.com/world/asia-pacific/taliban-codify-morality-laws-requiring-afghan-women-cover-faces-men-grow-beards-2024-08-23/")
                )
        ));

        list.add(new Article(
                "scuola-femminile",
                "Scuola secondaria vietata alle ragazze oltre la sesta classe",
                "Nel 2025 l'ONU parla del quarto anno consecutivo di esclusione",
                "UNICEF / UNAMA",
                "26 marzo 2025",
                "UNAMA e UNICEF hanno dichiarato che il nuovo anno scolastico in Afghanistan è iniziato ancora una volta senza le ragazze nelle classi secondarie. Nel 2025 il divieto è entrato nel quarto anno consecutivo.",
                "assets/cover-school-ban.svg",
                "assets/detail-school-ban.svg",
                "amber",
                "Istruzione",
                List.of(
                        "Questo è uno dei dati più evidenti perché mostra un effetto concreto: le ragazze non possono proseguire gli studi oltre la sesta classe, quindi la scuola secondaria resta di fatto chiusa per loro.",
                        "Nel comunicato del 26 marzo 2025, UNAMA parla chiaramente del quarto anno consecutivo di esclusione, sottolineando la continuità della misura.",
                        "La restrizione non colpisce solo il percorso scolastico immediato: incide anche su prospettive professionali, autonomia economica, salute e partecipazione pubblica delle ragazze afghane.",
                        "Per questo la chiusura della scuola secondaria è spesso presentata come uno dei simboli più evidenti del sistema di discriminazione imposto alle donne in Afghanistan."
                ),
                List.of(
                        "Divieto oltre la sesta classe.",
                        "Nel 2025: quarto anno consecutivo di esclusione.",
                        "Secondo l'ONU la misura aggrava crisi umanitaria, economica e dei diritti umani.",
                        "È uno dei simboli più evidenti del sistema di discriminazione imposto alle donne."
                ),
                List.of(
                        new TimelineItem("set 2021", "Le ragazze restano escluse dalla scuola secondaria nella nuova fase talebana."),
                        new TimelineItem("giu 2024", "UNICEF parla di 1.000 giorni di apprendimento perduto."),
                        new TimelineItem("mar 2025", "UNAMA e UNICEF: il divieto entra nel quarto anno consecutivo.")
                ),
                List.of(
                        new SourceLink("UNICEF / UNAMA – fourth year statement", "https://www.unicef.org/rosa/press-releases/unama-expresses-deep-disappointment-ban-girls-secondary-education-goes-fourth-year"),
                        new SourceLink("UNICEF – 1,000 days of education lost", "https://www.unicef.org/rosa/press-releases/1000-days-education-equivalent-three-billion-learning-hours-lost-afghan-girls"),
                        new SourceLink("UNICEF – Education Situation Report 2025", "https://www.unicef.org/afghanistan/media/12691/file/Report_AFG_Education_PRINT_final-.pdf.pdf")
                )
        ));

        list.add(new Article(
                "universita-ong-lavoro",
                "Università, ONG e lavoro femminile: esclusione sistematica",
                "Non solo scuole: le restrizioni colpiscono anche studi e occupazione",
                "CEDAW / OHCHR",
                "2022–2025",
                "Le donne sono state escluse dalle università nel dicembre 2022 e un ordine dello stesso periodo ha vietato alle donne di lavorare nelle ONG. Il Comitato ONU CEDAW ha poi richiamato anche l'estensione del divieto al lavoro con le Nazioni Unite e altre gravi limitazioni all'occupazione femminile.",
                "assets/cover-work-university.svg",
                "assets/detail-work.svg",
                "rose",
                "Lavoro e studio",
                List.of(
                        "Quando si parla di Afghanistan spesso si cita solo il velo o la scuola. In realtà i report ONU spiegano che il blocco è molto più largo: l'accesso all'università è stato fermato, il lavoro nelle ONG è stato vietato e molte donne sono state allontanate dal lavoro pubblico o tenute fuori dal mercato formale.",
                        "Il Comitato CEDAW del 2025 usa toni molto netti e riassume diversi provvedimenti: ordine del 24 dicembre 2022 contro il lavoro femminile nelle ONG, estensione del 4 aprile 2023 alle donne afghane che lavorano per l'ONU, e reiterazione del provvedimento nel dicembre 2024.",
                        "Il focus di questa pagina è economico e professionale: reddito, indipendenza e possibilità di costruirsi un futuro vengono ridotti insieme quando si sommano esclusione dagli studi e limitazioni al lavoro.",
                        "La discriminazione non è solo culturale o simbolica: diventa materiale perché colpisce studio, professioni, stipendi, mobilità e autonomia personale."
                ),
                List.of(
                        "Università femminili bloccate dal dicembre 2022.",
                        "Divieto di lavoro nelle ONG dal dicembre 2022.",
                        "Estensione del divieto a lavoratrici afghane ONU dal 2023.",
                        "Calo drastico della presenza femminile nel mercato del lavoro formale."
                ),
                List.of(
                        new TimelineItem("dic 2022", "Esclusione delle donne dalle università e ordine contro il lavoro nelle ONG."),
                        new TimelineItem("apr 2023", "Il divieto viene esteso alle donne afghane che lavorano per le Nazioni Unite."),
                        new TimelineItem("dic 2024", "Ulteriori restrizioni colpiscono anche istituti medici e percorsi di formazione sanitaria."),
                        new TimelineItem("lug 2025", "CEDAW condanna formalmente l'insieme delle misure discriminatorie.")
                ),
                List.of(
                        new SourceLink("CEDAW – Concluding observations 2025", "https://docstore.ohchr.org/SelfServices/FilesHandler.ashx?enc=7rvSuptDjae05a7My%2BbmbrS0JFJsl6XfLP01zEohEnNTHs8fYd6Xz%2Ff6IEZAvhKk%2FNjpLa5piSTZ4zrs2SnmSA%3D%3D"),
                        new SourceLink("OHCHR – report on Afghanistan situation", "https://www.ohchr.org/sites/default/files/documents/hrbodies/hrcouncil/sessions-regular/session58/advance-version/a-hrc-58-74-aev.pdf")
                )
        ));

        list.add(new Article(
                "spazi-pubblici",
                "Bagni pubblici, parchi e palestre: spazi negati alle donne",
                "Le limitazioni vanno oltre scuola e lavoro",
                "OHCHR / UNAMA",
                "2023–2025",
                "Esperti ONU e report UNAMA hanno confermato restrizioni o divieti per donne e ragazze in parchi, palestre, sport club e bagni pubblici. Il risultato è un'esclusione crescente dagli spazi sociali e ricreativi.",
                "assets/cover-public-spaces.svg",
                "assets/detail-public.svg",
                "green",
                "Spazi pubblici",
                List.of(
                        "Questa parte è importante perché mostra che il controllo non riguarda solo grandi temi come istruzione e lavoro, ma anche la vita quotidiana: uscire, fare attività fisica, frequentare luoghi pubblici e curare l'igiene personale.",
                        "Nel 2023 gli esperti ONU hanno già denunciato che donne e ragazze erano state bandite da parchi pubblici, bagni, palestre e club sportivi. UNAMA ha poi confermato il tema nelle sue analisi successive sull'Afghanistan.",
                        "Queste restrizioni si collegano al tema più generale della libertà di movimento, del confinamento nella sfera privata e della segregazione di genere.",
                        "Le restrizioni colpiscono anche il tempo libero e la presenza nello spazio pubblico, contribuendo a rendere donne e ragazze sempre più invisibili nella vita sociale."
                ),
                List.of(
                        "Divieti o forti restrizioni per parchi, palestre, bagni pubblici e sport club.",
                        "Riduzione della libertà di movimento e della vita sociale.",
                        "Le misure si sommano a obblighi su abbigliamento e presenza di un mahram in alcuni contesti.",
                        "Secondo esperti ONU, la situazione delle donne afghane è la più grave al mondo."
                ),
                List.of(
                        new TimelineItem("nov 2022", "Vengono riportati divieti per parchi e palestre."),
                        new TimelineItem("mar 2023", "Esperti ONU denunciano l'esclusione da bagni pubblici, palestre e parchi."),
                        new TimelineItem("2024–2025", "UNAMA continua a riportare restrizioni in spazi ricreativi e pubblici.")
                ),
                List.of(
                        new SourceLink("OHCHR – UN experts say progress erased", "https://www.ohchr.org/en/press-releases/2023/03/afghanistan-un-experts-say-20-years-progress-women-and-girls-rights-erased"),
                        new SourceLink("UNAMA – moral oversight report", "https://unama.unmissions.org/sites/default/files/moral_oversight_report_english_final.pdf"),
                        new SourceLink("UNAMA – 2025 human rights update", "https://unama.unmissions.org/sites/default/files/english_-_unama_hrs_update_on_human_rights_in_afghanistan_april-june_2025_final.pdf")
                )
        ));

        list.add(new Article(
                "contesto-paese",
                "Afghanistan oggi: quadro generale",
                "Dati essenziali e contesto politico",
                "Banca Mondiale / OHCHR",
                String.valueOf(LocalDate.now().getYear()),
                "La home riporta una scheda sintetica sul Paese: capitale Kabul, lingue ufficiali pashto e dari, governo come Emirato islamico de facto non riconosciuto internazionalmente, dati di popolazione e PIL, più collegamenti alle notizie principali sulle restrizioni.",
                "assets/cover-context.svg",
                "assets/detail-context.svg",
                "gold",
                "Panoramica",
                List.of(
                        "Questa pagina introduttiva unisce dati geografici e demografici a un contesto politico molto particolare: l'Emirato islamico de facto guidato dai Talebani non è riconosciuto a livello internazionale.",
                        "La pagina iniziale raccoglie immagine principale, mappa, mini-box laterali e collegamenti alle notizie principali, così da offrire una panoramica immediata.",
                        "Da qui è possibile aprire gli approfondimenti collegati: legge 2024, scuola femminile, media, lavoro, università e spazi pubblici.",
                        "La scheda introduttiva collega i principali temi politici e sociali sviluppati nelle pagine di approfondimento."
                ),
                List.of(
                        "Capitale: Kabul.",
                        "Lingue ufficiali: pashto e dari.",
                        "Governo: Emirato islamico de facto non riconosciuto internazionalmente.",
                        "Scheda introduttiva collegata a tutte le pagine di approfondimento."
                ),
                List.of(
                        new TimelineItem("ago 2021", "I Talebani riprendono il controllo del Paese."),
                        new TimelineItem("2021–2025", "Si accumulano decreti, ordini e direttive che restringono libertà e diritti."),
                        new TimelineItem("2024–2025", "La legge su virtù e vizio consolida molte restrizioni in un testo unico.")
                ),
                List.of(
                        new SourceLink("World Bank – Afghanistan data profile", "https://data.worldbank.org/country/afghanistan"),
                        new SourceLink("OHCHR – Study on the PVPV law", "https://www.ohchr.org/sites/default/files/2025-02/a-hrc-58-74-pvpv-study.pdf")
                )
        ));

        return list;
    }

    private record TimelineItem(String when, String what) {}
    private record SourceLink(String label, String url) {}

    private static class Article {
        String id;
        String title;
        String subtitle;
        String source;
        String date;
        String summary;
        String image;
        String evidenceImage;
        String accent;
        String tag;
        List<String> paragraphs;
        List<String> highlights;
        List<TimelineItem> timeline;
        List<SourceLink> sources;

        Article(String id, String title, String subtitle, String source, String date, String summary,
                String image, String evidenceImage, String accent, String tag, List<String> paragraphs,
                List<String> highlights, List<TimelineItem> timeline, List<SourceLink> sources) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.source = source;
            this.date = date;
            this.summary = summary;
            this.image = image;
            this.evidenceImage = evidenceImage;
            this.accent = accent;
            this.tag = tag;
            this.paragraphs = paragraphs;
            this.highlights = highlights;
            this.timeline = timeline;
            this.sources = sources;
        }
    }

    private static class StaticFileHandler implements HttpHandler {
        private final Path publicDir;

        StaticFileHandler(Path publicDir) {
            this.publicDir = publicDir;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String pathString = exchange.getRequestURI().getPath();
            if (pathString.startsWith("/")) pathString = pathString.substring(1);
            Path file = publicDir.resolve(pathString).normalize();
            if (!file.startsWith(publicDir) || !Files.exists(file) || Files.isDirectory(file)) {
                writeText(exchange, 404, "404 - Risorsa non trovata", "text/plain; charset=utf-8");
                return;
            }
            writeBytes(exchange, 200, Files.readAllBytes(file), contentType(file));
        }
    }
}
