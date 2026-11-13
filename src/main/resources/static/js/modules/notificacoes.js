import { el } from './utils.js';
import { Toasts } from './toasts.js';

const PRIORIDADES = {
  CRITICO: { icone: 'bi-exclamation-octagon', classe: 'st-erro', rotulo: 'Crítico' },
  ATENCAO: { icone: 'bi-exclamation-triangle', classe: 'st-aviso', rotulo: 'Atenção' },
  INFORMATIVO: { icone: 'bi-info-circle', classe: 'st-info', rotulo: 'Informativo' }
};
const INTERVALO_POLLING_MS = 60000;
const MAX_TENTATIVAS_WS = 6;

let contador;
let lista;
let aberto = false;
let tentativas = 0;
let pollingAtivo = null;

function atualizarContador(total) {
  if (!contador) return;
  contador.textContent = total > 99 ? '99+' : String(total || 0);
  contador.hidden = !total;
}

function itemVazio(texto) {
  lista.replaceChildren(el('div', { class: 'vazio compacto' }, el('h3', { text: texto })));
}

function renderizar(itens) {
  if (!itens) return itemVazio('Não foi possível carregar as notificações.');
  if (!itens.length) return itemVazio('Nenhuma notificação pendente.');
  lista.replaceChildren(...itens.map((n) => {
    const p = PRIORIDADES[n.prioridade] || PRIORIDADES.INFORMATIVO;
    return el('a', { class: 'notif-item', href: '/notificacoes' },
      el('span', { class: 'badge-b ' + p.classe + ' st-sem-icone align-self-start', title: p.rotulo }, el('i', { class: 'bi ' + p.icone }), p.rotulo),
      el('span', {}, el('strong', { text: n.titulo }), n.mensagem ? el('span', { class: 'nota', text: n.mensagem }) : null));
  }));
}

function carregar() {
  return fetch('/notificacoes/resumo', { headers: { Accept: 'application/json' }, credentials: 'same-origin' })
    .then((resposta) => {
      if (!resposta.ok) throw new Error('HTTP ' + resposta.status);
      return resposta.json();
    })
    .then((dados) => {
      atualizarContador(dados.naoLidas);
      if (aberto) renderizar(dados.itens);
    })
    .catch(() => { if (aberto) renderizar(null); });
}

function iniciarPolling() {
  if (pollingAtivo) return;
  pollingAtivo = setInterval(() => { if (!document.hidden) carregar(); }, INTERVALO_POLLING_MS);
}

function conectar() {
  if (!('WebSocket' in window)) return iniciarPolling();
  const protocolo = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  let socket;
  try {
    socket = new WebSocket(protocolo + '//' + window.location.host + '/ws/notificacoes');
  } catch (e) {
    return iniciarPolling();
  }
  socket.onopen = () => {
    tentativas = 0;
    if (pollingAtivo) { clearInterval(pollingAtivo); pollingAtivo = null; }
  };
  socket.onmessage = (evento) => {
    let mensagem;
    try { mensagem = JSON.parse(evento.data); } catch (e) { return; }
    if (mensagem.tipo !== 'notificacao') return;
    const dados = mensagem.dados || {};
    atualizarContador((parseInt(contador.textContent, 10) || 0) + 1);
    if (dados.prioridade === 'CRITICO') Toasts.erro(dados.titulo || 'Notificação crítica', 'Requer atenção imediata.');
    else Toasts.info('Nova notificação', dados.titulo);
    if (aberto) carregar();
  };
  socket.onclose = () => {
    tentativas += 1;
    iniciarPolling();
    if (tentativas > MAX_TENTATIVAS_WS) return;
    setTimeout(conectar, Math.min(30000, 2000 * tentativas));
  };
}

export function iniciarNotificacoes() {
  contador = document.getElementById('contador-notificacoes');
  lista = document.getElementById('lista-notificacoes');
  const gatilho = document.getElementById('btn-notificacoes');
  if (!gatilho || !lista) return;
  gatilho.addEventListener('show.bs.dropdown', () => { aberto = true; carregar(); });
  gatilho.addEventListener('hidden.bs.dropdown', () => { aberto = false; });
  carregar();
  conectar();
}
