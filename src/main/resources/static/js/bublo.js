/* =========================================================================
   Bublo - JavaScript Modular
   Tema claro/escuro, toasts, confirmações, notificações WebSocket.
   Sem dependência de CDN. O tema inicial é aplicado por um script inline no
   <head> (layout/base.html) para evitar piscar antes da primeira pintura.
   ========================================================================= */

const Bublo = (function () {
  "use strict";

  const TEMA_KEY = "bublo:tema";

  // ---------- Tema ----------
  const Tema = {
    atual: function () {
      return document.documentElement.getAttribute("data-tema") === "escuro" ? "escuro" : "claro";
    },
    preferenciaSalva: function () {
      try {
        const salvo = localStorage.getItem(TEMA_KEY);
        return salvo === "claro" || salvo === "escuro" ? salvo : null;
      } catch (e) { return null; }
    },
    aplicar: function (tema, persistir) {
      const raiz = document.documentElement;
      raiz.setAttribute("data-tema", tema);
      raiz.setAttribute("data-bs-theme", tema === "escuro" ? "dark" : "light");
      if (persistir) {
        try { localStorage.setItem(TEMA_KEY, tema); } catch (e) {}
      }
      this.atualizarIcone();
      window.dispatchEvent(new CustomEvent("bublo:tema:alterado", { detail: { tema } }));
    },
    atualizarIcone: function () {
      const icone = document.querySelector("#btn-tema i");
      if (icone) icone.className = this.atual() === "escuro" ? "bi bi-sun-fill" : "bi bi-moon-stars-fill";
    },
    alternar: function () {
      document.documentElement.classList.add("tema-animado");
      this.aplicar(this.atual() === "claro" ? "escuro" : "claro", true);
    },
    init: function () {
      const btn = document.getElementById("btn-tema");
      if (btn) btn.addEventListener("click", () => this.alternar());
      this.atualizarIcone();
      if (window.matchMedia) {
        window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", (e) => {
          if (!this.preferenciaSalva()) this.aplicar(e.matches ? "escuro" : "claro", false);
        });
      }
      window.addEventListener("storage", (e) => {
        if (e.key === TEMA_KEY && (e.newValue === "claro" || e.newValue === "escuro")) this.aplicar(e.newValue, false);
      });
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
        c.setAttribute("aria-live", "polite");
        document.body.appendChild(c);
      }
      this.container = c;
    },
    mostrar: function (tipo, titulo, descricao, duracao) {
      if (!this.container) this.init();
      const icones = { sucesso: "bi-check-circle-fill", erro: "bi-exclamation-circle-fill", atencao: "bi-exclamation-triangle-fill", info: "bi-info-circle-fill" };
      const el = document.createElement("div");
      el.className = "toast show " + tipo;
      el.setAttribute("role", tipo === "erro" ? "alert" : "status");

      const icone = document.createElement("i");
      icone.className = "bi " + (icones[tipo] || icones.info) + " icone";
      const conteudo = document.createElement("div");
      conteudo.className = "conteudo";
      const t = document.createElement("div");
      t.className = "titulo";
      t.textContent = titulo;
      conteudo.appendChild(t);
      if (descricao) {
        const d = document.createElement("div");
        d.className = "descricao";
        d.textContent = descricao;
        conteudo.appendChild(d);
      }
      const fechar = document.createElement("button");
      fechar.type = "button";
      fechar.className = "fechar";
      fechar.setAttribute("aria-label", "Fechar");
      fechar.textContent = "×";
      fechar.addEventListener("click", () => this.remover(el));

      el.append(icone, conteudo, fechar);
      this.container.appendChild(el);
      setTimeout(() => this.remover(el), duracao || (tipo === "erro" ? 8000 : 5000));
      return el;
    },
    remover: function (el) {
      if (!el || !el.parentNode) return;
      el.style.opacity = "0";
      el.style.transform = "translateX(20px)";
      setTimeout(() => el.remove(), 250);
    },
    sucesso: function (t, d) { return this.mostrar("sucesso", t, d); },
    erro: function (t, d) { return this.mostrar("erro", t, d); },
    atencao: function (t, d) { return this.mostrar("atencao", t, d); },
    info: function (t, d) { return this.mostrar("info", t, d); },
    exibirFlash: function () {
      const dados = document.getElementById("flash-data");
      if (!dados) return;
      if (dados.dataset.sucesso) this.sucesso(dados.dataset.sucesso);
      if (dados.dataset.erro) this.erro(dados.dataset.erro);
      if (dados.dataset.aviso) this.atencao(dados.dataset.aviso);
    }
  };

  // ---------- Confirmações (modal Bootstrap) ----------
  const Confirmacoes = {
    modal: null,
    elemento: null,
    aoConfirmar: null,
    construir: function () {
      const el = document.createElement("div");
      el.className = "modal fade modal-confirmacao";
      el.tabIndex = -1;
      el.setAttribute("aria-hidden", "true");
      el.innerHTML =
        '<div class="modal-dialog modal-dialog-centered"><div class="modal-content">' +
        '<div class="modal-header"><h5 class="modal-title"></h5>' +
        '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button></div>' +
        '<div class="modal-body"></div>' +
        '<div class="modal-footer">' +
        '<button type="button" class="btn-bublo btn-sm btn-contorno" data-bs-dismiss="modal">Voltar</button>' +
        '<button type="button" class="btn-bublo btn-sm" data-acao="confirmar">Confirmar</button>' +
        "</div></div></div>";
      document.body.appendChild(el);
      el.querySelector('[data-acao="confirmar"]').addEventListener("click", () => {
        const acao = this.aoConfirmar;
        this.aoConfirmar = null;
        this.modal.hide();
        if (acao) acao();
      });
      this.modal = new bootstrap.Modal(el);
      this.elemento = el;
    },
    perguntar: function (mensagem, titulo, aoConfirmar) {
      if (typeof bootstrap === "undefined") {
        if (window.confirm(mensagem)) aoConfirmar();
        return;
      }
      if (!this.modal) this.construir();
      this.elemento.querySelector(".modal-title").textContent = titulo || "Confirmar ação";
      this.elemento.querySelector(".modal-body").textContent = mensagem;
      this.aoConfirmar = aoConfirmar;
      this.modal.show();
    },
    init: function () {
      document.addEventListener("submit", (e) => {
        const form = e.target;
        if (!(form instanceof HTMLFormElement) || !form.dataset.confirmar || form.dataset.confirmado) return;
        e.preventDefault();
        this.perguntar(form.dataset.confirmar, form.dataset.confirmarTitulo, () => {
          form.dataset.confirmado = "1";
          form.requestSubmit();
        });
      });
    }
  };

  // ---------- Notificações (dropdown + WebSocket) ----------
  const Notificacoes = {
    contadorEl: null,
    dropdownEl: null,
    listaEl: null,
    tentativas: 0,
    init: function () {
      this.contadorEl = document.getElementById("contador-notificacoes");
      this.dropdownEl = document.getElementById("dropdown-notificacoes");
      this.listaEl = document.getElementById("lista-notificacoes");
      const btn = document.getElementById("btn-notificacoes");
      if (!btn || !this.dropdownEl) return;
      btn.addEventListener("click", (e) => {
        e.stopPropagation();
        this.dropdownEl.classList.toggle("visivel");
        if (this.dropdownEl.classList.contains("visivel")) this.carregar();
      });
      document.addEventListener("click", (e) => {
        if (!this.dropdownEl.contains(e.target)) this.dropdownEl.classList.remove("visivel");
      });
      this.carregar();
      this.conectarWS();
    },
    carregar: function () {
      fetch("/notificacoes/resumo", { headers: { Accept: "application/json" }, credentials: "same-origin" })
        .then((r) => {
          if (!r.ok) throw new Error("HTTP " + r.status);
          return r.json();
        })
        .then((dados) => {
          this.atualizarContador(dados.naoLidas);
          this.renderizar(dados.itens);
        })
        .catch(() => {
          this.renderizar(null);
        });
    },
    renderizar: function (itens) {
      if (!this.listaEl) return;
      this.listaEl.textContent = "";
      const vazio = (texto) => {
        const el = document.createElement("div");
        el.className = "item item-vazio";
        el.textContent = texto;
        this.listaEl.appendChild(el);
      };
      if (itens === null) return vazio("Não foi possível carregar as notificações.");
      if (!itens.length) return vazio("Nenhuma notificação.");
      itens.forEach((n) => {
        const a = document.createElement("a");
        a.className = "item d-block";
        a.href = "/notificacoes";
        const titulo = document.createElement("strong");
        titulo.textContent = n.titulo;
        a.appendChild(titulo);
        if (n.mensagem) {
          const msg = document.createElement("div");
          msg.className = "texto-ajuda";
          msg.textContent = n.mensagem;
          a.appendChild(msg);
        }
        this.listaEl.appendChild(a);
      });
    },
    conectarWS: function () {
      if (!("WebSocket" in window)) return;
      const protocolo = window.location.protocol === "https:" ? "wss:" : "ws:";
      let socket;
      try {
        socket = new WebSocket(protocolo + "//" + window.location.host + "/ws/notificacoes");
      } catch (e) { return; }
      socket.onopen = () => { this.tentativas = 0; };
      socket.onmessage = (evt) => {
        let msg;
        try { msg = JSON.parse(evt.data); } catch (e) { return; }
        if (msg.tipo !== "notificacao") return;
        this.incrementarContador();
        Toasts.info("Nova notificação", msg.dados && msg.dados.titulo ? msg.dados.titulo : "Você tem uma nova notificação");
        if (this.dropdownEl.classList.contains("visivel")) this.carregar();
      };
      socket.onclose = () => {
        this.tentativas += 1;
        if (this.tentativas > 5) return;
        setTimeout(() => this.conectarWS(), Math.min(30000, 3000 * this.tentativas));
      };
    },
    incrementarContador: function () {
      if (!this.contadorEl) return;
      this.atualizarContador((parseInt(this.contadorEl.textContent, 10) || 0) + 1);
    },
    atualizarContador: function (n) {
      if (!this.contadorEl) return;
      const total = n || 0;
      this.contadorEl.textContent = total;
      this.contadorEl.style.display = total > 0 ? "" : "none";
    }
  };

  // ---------- Utilitários ----------
  const Utils = {
    formatoMoeda: function (v) {
      return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v != null ? v : 0);
    },
    debounce: function (fn, ms) {
      let t;
      return function (...args) {
        clearTimeout(t);
        t = setTimeout(() => fn.apply(this, args), ms);
      };
    },
    csrf: function () {
      const token = document.querySelector('meta[name="_csrf"]');
      const header = document.querySelector('meta[name="_csrf_header"]');
      return token && header ? { cabecalho: header.content, valor: token.content } : null;
    }
  };

  function iniciarMenuMobile() {
    const btn = document.getElementById("btn-menu");
    const sidebar = document.getElementById("sidebar");
    if (!btn || !sidebar) return;
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      sidebar.classList.toggle("aberta");
    });
    document.addEventListener("click", (e) => {
      if (!sidebar.contains(e.target)) sidebar.classList.remove("aberta");
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    Tema.init();
    Toasts.init();
    Confirmacoes.init();
    Notificacoes.init();
    iniciarMenuMobile();
    Toasts.exibirFlash();
  });

  return { Tema, Toasts, Notificacoes, Utils, Confirmacoes };
})();
