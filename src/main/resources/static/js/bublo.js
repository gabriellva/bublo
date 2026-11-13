/* =========================================================================
   Bublo - JavaScript Modular
   Tema claro/escuro, toasts, notificações WebSocket, utilitários de UI.
   Sem dependência de CDN.
   ========================================================================= */

const Bublo = (function () {
  "use strict";

  const TEMA_KEY = "bublo:tema";

  // ---------- Tema ----------
  const Tema = {
    obterPreferencia: function () {
      try {
        return localStorage.getItem(TEMA_KEY);
      } catch (e) { return null; }
    },
    salvarPreferencia: function (tema) {
      try { localStorage.setItem(TEMA_KEY, tema); } catch (e) {}
    },
    aplicar: function (tema) {
      const html = document.documentElement;
      if (!tema) {
        tema = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches
          ? "escuro" : "claro";
      }
      html.setAttribute("data-tema", tema);
      this.salvarPreferencia(tema);
      const btn = document.getElementById("btn-tema");
      if (btn) {
        const icone = btn.querySelector("i");
        if (icone) icone.className = tema === "escuro" ? "bi bi-sun-fill" : "bi bi-moon-stars-fill";
      }
      window.dispatchEvent(new CustomEvent("bublo:tema:alterado", { detail: { tema } }));
    },
    alternar: function () {
      const atual = document.documentElement.getAttribute("data-tema") || this.obterPreferencia() || "claro";
      this.aplicar(atual === "claro" ? "escuro" : "claro");
    },
    init: function () {
      const btn = document.getElementById("btn-tema");
      if (btn) btn.addEventListener("click", () => this.alternar());
      const tema = this.obterPreferencia();
      this.aplicar(tema);
    }
  };

  // ---------- Toasts ----------
  const Toasts = {
    container: null,
    init: function () {
      let c = document.getElementById("toast-container");
      if (!c) {
        c = document.createElement("div");
        c.id = "toast-container";
        c.className = "toast-container";
        document.body.appendChild(c);
      }
      this.container = c;
    },
    mostrar: function (tipo, titulo, descricao, duracao = 5000) {
      if (!this.container) this.init();
      const el = document.createElement("div");
      el.className = `toast ${tipo}`;
      const icones = { sucesso: "bi-check-circle-fill", erro: "bi-exclamation-circle-fill", atencao: "bi-exclamation-triangle-fill", info: "bi-info-circle-fill" };
      el.innerHTML = `
        <i class="bi ${icones[tipo] || 'bi-info-circle-fill'} icone"></i>
        <div class="conteudo">
          <div class="titulo">${this.escapeHtml(titulo)}</div>
          ${descricao ? `<div class="descricao">${this.escapeHtml(descricao)}</div>` : ""}
        </div>
      `;
      this.container.appendChild(el);
      setTimeout(() => this.remover(el), duracao);
      return el;
    },
    remover: function (el) {
      if (el && el.parentNode) {
        el.style.opacity = "0";
        el.style.transform = "translateX(20px)";
        setTimeout(() => el.remove(), 250);
      }
    },
    sucesso: function (t, d) { this.mostrar("sucesso", t, d); },
    erro: function (t, d) { this.mostrar("erro", t, d); },
    atencao: function (t, d) { this.mostrar("atencao", t, d); },
    info: function (t, d) { this.mostrar("info", t, d); },
    escapeHtml: function (s) {
      const div = document.createElement("div");
      div.textContent = s;
      return div.innerHTML;
    }
  };

  // ---------- Notificações (dropdown + WebSocket) ----------
  const Notificacoes = {
    contadorEl: null,
    dropdownEl: null,
    socket: null,
    wsHandler: null,
    init: function (wsEndpoint) {
      this.contadorEl = document.getElementById("contador-notificacoes");
      this.dropdownEl = document.getElementById("dropdown-notificacoes");
      const btn = document.getElementById("btn-notificacoes");
      if (btn) {
        btn.addEventListener("click", (e) => {
          e.stopPropagation();
          this.alternarDropdown();
        });
      }
      document.addEventListener("click", (e) => {
        if (this.dropdownEl && !this.dropdownEl.contains(e.target)) {
          this.dropdownEl.classList.remove("visivel");
        }
      });
      if (wsEndpoint) this.conectarWS(wsEndpoint);
    },
    conectarWS: function (endpoint) {
      try {
        this.socket = new WebSocket(endpoint);
        this.socket.onopen = () => {};
        this.socket.onmessage = (evt) => {
          try {
            const msg = JSON.parse(evt.data);
            if (msg.tipo === "notificacao") {
              this.incrementarContador();
              Toasts.info("Nova notificação", msg.dados && msg.dados.titulo ? msg.dados.titulo : "Você tem uma nova notificação");
            }
          } catch (e) {}
        };
        this.socket.onclose = () => {
          // reconexão automática após 3s
          setTimeout(() => this.conectarWS(endpoint), 3000);
        };
        this.socket.onerror = () => {};
      } catch (e) {}
    },
    incrementarContador: function () {
      if (!this.contadorEl) return;
      let atual = parseInt(this.contadorEl.textContent || "0", 10) || 0;
      this.contadorEl.textContent = atual + 1;
      this.contadorEl.style.display = "inline";
    },
    atualizarContador: function (n) {
      if (!this.contadorEl) return;
      this.contadorEl.textContent = n || "0";
      this.contadorEl.style.display = n > 0 ? "inline" : "none";
    },
    alternarDropdown: function () {
      if (this.dropdownEl) this.dropdownEl.classList.toggle("visivel");
    }
  };

  // ---------- Utilitários ----------
  const Utils = {
    formatoMoeda: function (v) {
      return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v != null ? v : 0);
    },
    formatoData: function (iso) {
      if (!iso) return "-";
      try {
        const d = new Date(iso);
        return d.toLocaleDateString("pt-BR");
      } catch (e) { return iso; }
    },
    formatoDataHora: function (iso) {
      if (!iso) return "-";
      try {
        const d = new Date(iso);
        return d.toLocaleString("pt-BR");
      } catch (e) { return iso; }
    },
    debounce: function (fn, ms) {
      let t;
      return function (...args) {
        clearTimeout(t);
        t = setTimeout(() => fn.apply(this, args), ms);
      };
    },
    csrfToken: function () {
      const meta = document.querySelector('meta[name="_csrf"]');
      const header = document.querySelector('meta[name="_csrf_header"]');
      if (meta && header) return { nome: header.content, valor: meta.content };
      return null;
    }
  };

  // ---------- Confirmações ----------
  const Confirmacoes = {
    perguntar: function (mensagem) {
      return confirm(mensagem);
    }
  };

  // Inicialização automática
  document.addEventListener("DOMContentLoaded", function () {
    Tema.init();
    Toasts.init();
  });

  return {
    Tema, Toasts, Notificacoes, Utils, Confirmacoes
  };
})();
