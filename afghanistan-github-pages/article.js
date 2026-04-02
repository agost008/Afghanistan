const qs = (s, root = document) => root.querySelector(s);

function renderArticle(article) {
  const root = qs("#articleRoot");
  document.title = `${article.title} — Afghanistan`;
  root.innerHTML = `
    <a class="back-link" href="index.html">← Torna alla panoramica</a>
    <section class="card article-header-card ${article.accent}">
      <img class="article-cover" src="${article.image}" alt="${article.title}">
      <div class="article-copy">
        <div class="meta-strip">
          <span class="theme-badge">${article.tag}</span>
          <span class="theme-badge">${article.source}</span>
          <span class="theme-badge">${article.date}</span>
        </div>
        <h1>${article.title}</h1>
        <p class="article-subtitle">${article.subtitle}</p>
        <p class="news-summary">${article.summary}</p>
      </div>
    </section>
    <section class="article-grid">
      <article class="card article-main">
        ${article.paragraphs.map(p => `<p>${p}</p>`).join("")}
        <div class="accent-box">
          <h3>Punti essenziali</h3>
          <ul class="highlight-list">
            ${article.highlights.map(item => `<li>${item}</li>`).join("")}
          </ul>
        </div>
      </article>
      <aside class="card article-side">
        <div class="evidence-card">
          <img src="${article.evidenceImage || article.image}" alt="${article.title}">
        </div>
        <h3>Cronologia</h3>
        <ul class="timeline-list">
          ${article.timeline.map(item => `<li><strong>${item.when}</strong> — ${item.what}</li>`).join("")}
        </ul>
        <h3>Fonti</h3>
        <ul class="source-list">
          ${article.sources.map(item => `<li><a href="${item.url}" target="_blank" rel="noreferrer">${item.label}</a></li>`).join("")}
        </ul>
      </aside>
    </section>
  `;
}

function init() {
  const params = new URLSearchParams(location.search);
  const id = params.get("id");
  const root = qs("#articleRoot");
  const article = id ? window.SITE_DATA?.details?.[id] : null;

  if (!id) {
    root.innerHTML = `<section class="card article-main"><h1>Articolo non selezionato</h1><p class="news-summary">Apri una notizia dalla pagina principale.</p></section>`;
    return;
  }

  if (!article) {
    root.innerHTML = `<section class="card article-main"><h1>Impossibile caricare l'articolo</h1><p class="news-summary">Controlla che il file data.js sia presente e che l'id della pagina sia corretto.</p></section>`;
    return;
  }

  renderArticle(article);
}

document.addEventListener("DOMContentLoaded", init);
