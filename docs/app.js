(function () {
  'use strict';

  var API_URL = 'https://api.github.com/repos/christianrvdv/Sumador/releases/latest';
  var FALLBACK_URL = 'https://github.com/christianrvdv/Sumador/releases/latest';
  var DOWNLOAD_PREFIX = 'https://github.com/christianrvdv/Sumador/releases/download/';
  var CACHE_KEY = 'sumador-release-v1';
  var CACHE_TTL = 10 * 60 * 1000;
  var THEME_KEY = 'sumador-theme';

  function queryAll(selector) {
    return Array.prototype.slice.call(document.querySelectorAll(selector));
  }

  function setText(nodes, value) {
    nodes.forEach(function (node) {
      node.textContent = value;
    });
  }

  /* ---------- Tema ---------- */

  function themeMeta(theme) {
    var meta = document.getElementById('meta-theme-color');
    if (meta) {
      meta.setAttribute('content', theme === 'dark' ? '#0f172a' : '#faf8ff');
    }
    var toggle = document.getElementById('theme-toggle');
    if (toggle) {
      var label = theme === 'dark' ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro';
      toggle.setAttribute('aria-label', label);
      toggle.setAttribute('title', label);
    }
    document.documentElement.style.colorScheme = theme;
  }

  function storedTheme() {
    try {
      var value = localStorage.getItem(THEME_KEY);
      return value === 'light' || value === 'dark' ? value : null;
    } catch (error) {
      return null;
    }
  }

  function initTheme() {
    var current = document.documentElement.getAttribute('data-theme');
    themeMeta(current === 'dark' ? 'dark' : 'light');

    var toggle = document.getElementById('theme-toggle');
    if (toggle) {
      toggle.addEventListener('click', function () {
        var next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        themeMeta(next);
        try {
          localStorage.setItem(THEME_KEY, next);
        } catch (error) {
          /* almacenamiento no disponible */
        }
      });
    }

    if (window.matchMedia) {
      var media = window.matchMedia('(prefers-color-scheme: dark)');
      var followSystem = function (event) {
        if (storedTheme()) {
          return;
        }
        var next = event.matches ? 'dark' : 'light';
        document.documentElement.setAttribute('data-theme', next);
        themeMeta(next);
      };
      if (typeof media.addEventListener === 'function') {
        media.addEventListener('change', followSystem);
      } else if (typeof media.addListener === 'function') {
        media.addListener(followSystem);
      }
    }
  }

  /* ---------- Datos de la release ---------- */

  function pickAsset(release) {
    if (!release || !Array.isArray(release.assets)) {
      return null;
    }
    for (var index = 0; index < release.assets.length; index += 1) {
      var asset = release.assets[index];
      if (
        asset &&
        typeof asset.name === 'string' &&
        /\.apk$/i.test(asset.name) &&
        typeof asset.browser_download_url === 'string' &&
        asset.browser_download_url.indexOf(DOWNLOAD_PREFIX) === 0
      ) {
        return asset;
      }
    }
    return null;
  }

  function normaliseRelease(release) {
    if (!release || typeof release.tag_name !== 'string' || !release.tag_name.trim()) {
      return null;
    }
    var published = new Date(release.published_at);
    if (isNaN(published.getTime())) {
      return null;
    }
    var asset = pickAsset(release);
    if (!asset) {
      return null;
    }
    return {
      tag: release.tag_name.trim(),
      publishedAt: release.published_at,
      assetName: asset.name,
      size: Number(asset.size) || 0,
      downloadUrl: asset.browser_download_url,
      digest: typeof asset.digest === 'string' ? asset.digest : ''
    };
  }

  function readCache() {
    try {
      var raw = sessionStorage.getItem(CACHE_KEY);
      if (!raw) {
        return null;
      }
      var entry = JSON.parse(raw);
      if (
        !entry ||
        typeof entry.timestamp !== 'number' ||
        !entry.payload ||
        typeof entry.payload.tag !== 'string' ||
        typeof entry.payload.assetName !== 'string' ||
        typeof entry.payload.downloadUrl !== 'string' ||
        entry.payload.downloadUrl.indexOf(DOWNLOAD_PREFIX) !== 0 ||
        isNaN(new Date(entry.payload.publishedAt).getTime()) ||
        Date.now() - entry.timestamp > CACHE_TTL
      ) {
        return null;
      }
      return entry.payload;
    } catch (error) {
      return null;
    }
  }

  function writeCache(data) {
    try {
      sessionStorage.setItem(CACHE_KEY, JSON.stringify({ timestamp: Date.now(), payload: data }));
    } catch (error) {
      /* almacenamiento no disponible */
    }
  }

  function formatSize(bytes) {
    if (!isFinite(bytes) || bytes <= 0) {
      return '—';
    }
    var value = bytes / 1048576;
    try {
      return new Intl.NumberFormat('es', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }).format(value) + ' MB';
    } catch (error) {
      return value.toFixed(2) + ' MB';
    }
  }

  function formatDate(iso) {
    var date = new Date(iso);
    if (isNaN(date.getTime())) {
      return '—';
    }
    var diff = Date.now() - date.getTime();
    var hour = 3600000;
    var day = 86400000;

    if (diff >= 0 && diff < hour) {
      return 'Hoy';
    }
    if (diff >= hour && diff < day) {
      var hours = Math.floor(diff / hour);
      return 'Hace ' + hours + (hours === 1 ? ' hora' : ' horas');
    }
    var days = Math.floor(diff / day);
    if (days >= 1 && days < 30) {
      return 'Hace ' + days + (days === 1 ? ' día' : ' días');
    }
    try {
      return new Intl.DateTimeFormat('es', { dateStyle: 'medium' }).format(date);
    } catch (error) {
      return date.toLocaleDateString('es');
    }
  }

  function formatDigest(digest) {
    if (typeof digest !== 'string') {
      return 'SHA256: —';
    }
    var value = digest.replace(/^sha256:/i, '');
    if (!/^[a-f0-9]{64}$/i.test(value)) {
      return 'SHA256: —';
    }
    return 'SHA256: ' + value.slice(0, 8) + '…' + value.slice(-4);
  }

  function versionLabel(tag) {
    return /^v/i.test(tag) ? tag : 'v' + tag;
  }

  var versionNodes = queryAll('.js-version');
  var sizeNodes = queryAll('.js-size');
  var updatedNodes = queryAll('.js-updated');
  var badgeNodes = queryAll('.js-version-badge');
  var downloadNodes = queryAll('.js-download');
  var assetNameNodes = queryAll('.js-asset-name');
  var assetHashNodes = queryAll('.js-asset-hash');
  var dot = document.getElementById('release-dot');
  var statusLabel = document.getElementById('release-status-label');

  function setState(state, data) {
    if (dot) {
      dot.classList.remove('is-loading', 'is-ok', 'is-error');
      dot.classList.add('is-' + state);
    }

    if (state === 'ok' && data) {
      var label = versionLabel(data.tag);
      setText(versionNodes, label);
      setText(sizeNodes, formatSize(data.size));
      setText(updatedNodes, formatDate(data.publishedAt));
      setText(assetNameNodes, data.assetName);
      setText(assetHashNodes, formatDigest(data.digest));
      if (statusLabel) {
        statusLabel.textContent = 'Obtenido desde GitHub Releases';
      }
      badgeNodes.forEach(function (badge) {
        badge.textContent = label;
        badge.hidden = false;
      });
      downloadNodes.forEach(function (link) {
        link.setAttribute('href', data.downloadUrl);
      });
      return;
    }

    if (state === 'error') {
      setText(versionNodes, 'última versión');
      setText(sizeNodes, '—');
      setText(updatedNodes, '—');
      setText(assetHashNodes, 'SHA256: —');
      if (statusLabel) {
        statusLabel.textContent = 'GitHub no disponible';
      }
      badgeNodes.forEach(function (badge) {
        badge.hidden = true;
      });
      downloadNodes.forEach(function (link) {
        link.setAttribute('href', FALLBACK_URL);
      });
      return;
    }

    setText(versionNodes, 'Cargando…');
    setText(sizeNodes, '…');
    setText(updatedNodes, '…');
    setText(assetHashNodes, 'SHA256: —');
    if (statusLabel) {
      statusLabel.textContent = 'Consultando GitHub Releases…';
    }
    badgeNodes.forEach(function (badge) {
      badge.hidden = true;
    });
  }

  function loadRelease() {
    setState('loading');

    var cached = readCache();
    if (cached) {
      setState('ok', cached);
      return;
    }

    var controller = typeof AbortController === 'function' ? new AbortController() : null;
    var timer = null;
    if (controller) {
      timer = setTimeout(function () {
        controller.abort();
      }, 8000);
    }

    fetch(API_URL, {
      headers: { Accept: 'application/vnd.github+json' },
      cache: 'no-store',
      signal: controller ? controller.signal : undefined
    })
      .then(function (response) {
        if (!response.ok) {
          throw new Error('HTTP ' + response.status);
        }
        return response.json();
      })
      .then(function (payload) {
        var data = normaliseRelease(payload);
        if (!data) {
          throw new Error('Respuesta inesperada');
        }
        writeCache(data);
        setState('ok', data);
      })
      .catch(function () {
        setState('error');
      })
      .then(function () {
        if (timer) {
          clearTimeout(timer);
        }
      });
  }

  function boot() {
    initTheme();
    var year = document.getElementById('year');
    if (year) {
      year.textContent = new Date().getFullYear();
    }
    loadRelease();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
