const qs = (s, root = document) => root.querySelector(s);

function createQuickCard(card) {
  return `
    <article class="quick-card">
      <img src="${card.image}" alt="${card.title}">
      <div class="quick-card-body">
        <h3>${card.title}</h3>
        <p>${card.subtitle}</p>
      </div>
    </article>
  `;
}

function createInfoRows(info) {
  return Object.entries(info).map(([key, value]) => `
    <div class="info-row">
      <div class="key">${key}</div>
      <div class="value">${value}</div>
    </div>
  `).join("");
}

function createNewsItem(article) {
  return `
    <article class="news-item" data-search="${[article.title, article.subtitle, article.summary, article.source].join(" ").toLowerCase()}">
      <div class="news-copy">
        <div class="news-meta">${article.source} · ${article.date}</div>
        <h3 class="news-title">${article.title}</h3>
        <p class="news-summary">${article.summary}</p>
        <div class="news-actions">
          <a class="link-pill theme ${article.accent}" href="article.html?id=${encodeURIComponent(article.id)}">Apri scheda completa</a>
          <span class="tag-pill">${article.subtitle}</span>
        </div>
      </div>
      <a class="news-thumb" href="article.html?id=${encodeURIComponent(article.id)}" aria-label="${article.title}">
        <img src="${article.image}" alt="${article.title}">
      </a>
    </article>
  `;
}

function wireFilter() {
  const input = qs("#filterInput");
  const items = [...document.querySelectorAll(".news-item")];
  input?.addEventListener("input", () => {
    const term = input.value.trim().toLowerCase();
    items.forEach(item => {
      const text = item.dataset.search || "";
      item.classList.toggle("hidden", term && !text.includes(term));
    });
  });
}

function init() {
  const summary = window.SITE_DATA?.summary;
  const articles = window.SITE_DATA?.articles || [];

  if (!summary) {
    qs("#newsList").innerHTML = `<div class="news-item"><div><h3 class="news-title">Impossibile caricare i contenuti</h3><p class="news-summary">Manca il file data.js.</p></div></div>`;
    return;
  }

  qs("#searchInput").value = summary.query;
  qs("#countryTitle").textContent = summary.query;
  qs("#countryLabel").textContent = summary.countryLabel;
  qs("#heroImage").src = summary.heroImage;
  qs("#heroImage").alt = summary.query;
  qs("#mapImage").src = summary.mapImage;
  qs("#mapImage").alt = `Mappa di ${summary.query}`;
  qs("#travelTime").textContent = summary.travelTime;
  qs("#travelSub").textContent = summary.travelSub;
  qs("#flagEmoji").textContent = summary.flagEmoji;
  qs("#flagLabel").textContent = summary.flagLabel;
  qs("#introText").textContent = summary.intro;
  qs("#quickCards").innerHTML = summary.quickCards.map(createQuickCard).join("");
  qs("#infoTable").innerHTML = createInfoRows(summary.info);
  qs("#newsList").innerHTML = articles.map(createNewsItem).join("");
  wireFilter();
}

document.addEventListener("DOMContentLoaded", init);
