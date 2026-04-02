# Afghanistan - HTML + CSS + Java

Sito statico con server HTTP in Java, home in stile pagina risultati e pagine di approfondimento collegate.

## Struttura

- `Server.java` → server HTTP in Java puro
- `public/index.html` → home principale
- `public/article.html` → pagina dettaglio articolo
- `public/styles.css` → stile responsive
- `public/script.js` → caricamento contenuti home
- `public/article.js` → caricamento pagine articolo
- `public/assets/` → immagini SVG locali

## Avvio

```bash
cd afghanistan-demo
javac Server.java
java Server
```

Apri poi:

```text
http://localhost:8787
```
