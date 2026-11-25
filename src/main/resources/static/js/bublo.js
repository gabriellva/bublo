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
      const raiz = document.documentElement;
      raiz.classList.add("tema-animado");
      this.aplicar(this.atual() === "claro" ? "escuro" : "claro", true);
      // A transição de cores vale só durante a troca; depois não pode interferir nas demais animações.
      clearTimeout(this.fimTransicao);
      this.fimTransicao = setTimeout(() => raiz.classList.remove("tema-animado"), 400);
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
      const btnTodas = document.getElementById("btn-ler-todas");
      if (btnTodas) {
        btnTodas.addEventListener("click", (e) => {
          e.stopPropagation();
          this.marcar("/notificacoes/ler-todas", () => this.renderizar([]));
        });
      }
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
      const btnTodas = document.getElementById("btn-ler-todas");
      if (btnTodas) btnTodas.hidden = !(itens && itens.length);
      const vazio = (texto) => {
        const el = document.createElement("div");
        el.className = "item item-vazio";
        el.textContent = texto;
        this.listaEl.appendChild(el);
      };
      if (itens === null) return vazio("Não foi possível carregar as notificações.");
      if (!itens.length) return vazio("Nenhuma notificação nova.");
      itens.forEach((n) => {
        const el = document.createElement("div");
        el.className = "item item-notificacao";
        const corpo = document.createElement("div");
        corpo.className = "corpo-notificacao";
        const titulo = document.createElement("strong");
        titulo.textContent = n.titulo;
        corpo.appendChild(titulo);
        if (n.mensagem) {
          const msg = document.createElement("div");
          msg.className = "texto-ajuda";
          msg.textContent = n.mensagem;
          corpo.appendChild(msg);
        }
        const check = document.createElement("button");
        check.type = "button";
        check.className = "btn-check-notificacao";
        check.title = "Marcar como lida";
        check.setAttribute("aria-label", "Marcar como lida: " + n.titulo);
        check.innerHTML = '<i class="bi bi-check-lg" aria-hidden="true"></i>';
        check.addEventListener("click", (e) => {
          e.stopPropagation();
          this.marcar("/notificacoes/" + encodeURIComponent(n.id) + "/ler", () => {
            el.remove();
            if (!this.listaEl.querySelector(".item-notificacao")) this.renderizar([]);
          });
        });
        el.appendChild(corpo);
        el.appendChild(check);
        this.listaEl.appendChild(el);
      });
    },
    marcar: function (url, aoConcluir) {
      const csrf = Utils.csrf();
      const headers = { Accept: "application/json" };
      if (csrf) headers[csrf.cabecalho] = csrf.valor;
      fetch(url, { method: "POST", headers: headers, credentials: "same-origin" })
        .then((r) => {
          if (!r.ok) throw new Error("HTTP " + r.status);
          return r.json();
        })
        .then((dados) => {
          this.atualizarContador(dados.naoLidas);
          aoConcluir();
        })
        .catch(() => Toasts.atencao("Não foi possível atualizar as notificações."));
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
    const fundo = document.createElement("div");
    fundo.className = "sidebar-fundo";
    document.body.appendChild(fundo);
    btn.setAttribute("aria-controls", "sidebar");
    btn.setAttribute("aria-expanded", "false");

    function definir(aberto) {
      sidebar.classList.toggle("aberta", aberto);
      fundo.classList.toggle("visivel", aberto);
      document.body.classList.toggle("menu-aberto", aberto);
      btn.setAttribute("aria-expanded", aberto ? "true" : "false");
      btn.setAttribute("aria-label", aberto ? "Fechar menu" : "Abrir menu");
      if (aberto) {
        const primeiro = sidebar.querySelector("a");
        if (primeiro) primeiro.focus();
      }
    }

    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      definir(!sidebar.classList.contains("aberta"));
    });
    fundo.addEventListener("click", () => definir(false));
    sidebar.addEventListener("click", (e) => { if (e.target.closest("a")) definir(false); });
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && sidebar.classList.contains("aberta")) {
        definir(false);
        btn.focus();
      }
    });
    // Ao voltar para o layout largo, o estado do menu móvel não pode ficar preso.
    window.matchMedia("(min-width: 993px)").addEventListener("change", (e) => { if (e.matches) definir(false); });
  }

  // ---------- Painéis recolhíveis: no celular a busca/importação some atrás de um botão ----------
  function iniciarPaineisRecolhiveis() {
    const paineis = Array.from(document.querySelectorAll(".painel-recolhivel"));
    if (!paineis.length) return;
    const barra = document.createElement("div");
    barra.className = "barra-paineis";
    paineis[0].before(barra);
    const parametros = new URLSearchParams(window.location.search);
    // Campos que não são filtro de verdade não contam como "filtro aplicado".
    const ignorados = ["aba", "pagina", "tamanho", "ordenacao"];

    paineis.forEach((painel, i) => {
      if (!painel.id) painel.id = "painel-recolhivel-" + i;
      const form = painel.querySelector("form");
      const filtrado = !!form && Array.from(form.elements).some((el) =>
        el.name && !ignorados.includes(el.name) && parametros.get(el.name));

      const botao = document.createElement("button");
      botao.type = "button";
      botao.className = "btn-bublo btn-contorno btn-painel";
      botao.setAttribute("aria-expanded", "false");
      botao.setAttribute("aria-controls", painel.id);
      const icone = document.createElement("i");
      icone.className = "bi " + (painel.dataset.icone || "bi-search");
      icone.setAttribute("aria-hidden", "true");
      const rotulo = document.createElement("span");
      rotulo.textContent = painel.dataset.rotulo || "Pesquisar";
      botao.append(icone, rotulo);
      if (filtrado) {
        const ponto = document.createElement("span");
        ponto.className = "ponto-filtro";
        ponto.title = "Filtros aplicados";
        botao.appendChild(ponto);
      }
      const seta = document.createElement("i");
      seta.className = "bi bi-chevron-down seta";
      seta.setAttribute("aria-hidden", "true");
      botao.appendChild(seta);

      painel.classList.add("recolhido");
      botao.addEventListener("click", () => {
        const aberto = painel.classList.toggle("recolhido") === false;
        botao.setAttribute("aria-expanded", aberto ? "true" : "false");
      });
      barra.appendChild(botao);
    });
  }

  // ---------- Envio de formulários: feedback e proteção contra duplo clique ----------
  function iniciarEnvios() {
    document.addEventListener("submit", (e) => {
      const form = e.target;
      // Confirmações cancelam o envio e reenviam depois; só reage ao envio que realmente segue.
      if (e.defaultPrevented || !(form instanceof HTMLFormElement)) return;
      if ((form.method || "get").toLowerCase() !== "post") return;
      if (form.dataset.enviando) {
        e.preventDefault();
        return;
      }
      form.dataset.enviando = "1";
      const botao = e.submitter || form.querySelector('button[type="submit"]');
      if (botao) {
        botao.classList.add("carregando");
        botao.setAttribute("aria-busy", "true");
      }
      // Desabilita depois do envio começar (desabilitar antes descartaria o valor do botão).
      setTimeout(() => {
        form.querySelectorAll('button[type="submit"]').forEach((b) => { b.disabled = true; });
      }, 0);
    });
    // Voltar pelo histórico (bfcache) não pode deixar o formulário travado.
    window.addEventListener("pageshow", (e) => {
      if (!e.persisted) return;
      document.querySelectorAll("form[data-enviando]").forEach((form) => {
        delete form.dataset.enviando;
        form.querySelectorAll('button[type="submit"]').forEach((b) => {
          b.disabled = false;
          b.classList.remove("carregando");
          b.removeAttribute("aria-busy");
        });
      });
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    Tema.init();
    Toasts.init();
    Confirmacoes.init();
    Notificacoes.init();
    iniciarMenuMobile();
    iniciarPaineisRecolhiveis();
    iniciarEnvios();
    Toasts.exibirFlash();
  });

  return { Tema, Toasts, Notificacoes, Utils, Confirmacoes };
})();
