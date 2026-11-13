import { el } from './utils.js';

const ICONES = { sucesso: 'bi-check-circle', erro: 'bi-x-octagon', atencao: 'bi-exclamation-triangle', info: 'bi-info-circle' };
let area = null;

function garantirArea() {
  if (area) return area;
  area = document.getElementById('toast-area');
  if (!area) {
    area = el('div', { id: 'toast-area', class: 'toast-area', 'aria-live': 'polite' });
    document.body.append(area);
  }
  return area;
}

function remover(toast) {
  if (!toast.isConnected) return;
  toast.classList.remove('entrou');
  toast.classList.add('saindo');
  setTimeout(() => toast.remove(), 180);
}

export function mostrar(tipo, titulo, descricao, duracao) {
  const fechar = el('button', { type: 'button', class: 't-fechar', 'aria-label': 'Fechar aviso' }, el('i', { class: 'bi bi-x-lg' }));
  const toast = el('div', { class: 'toast-b ' + tipo, role: tipo === 'erro' ? 'alert' : 'status' },
    el('i', { class: 'bi ' + (ICONES[tipo] || ICONES.info) + ' t-icone', 'aria-hidden': 'true' }),
    el('div', {}, el('div', { class: 't-titulo', text: titulo }), descricao ? el('div', { class: 't-texto', text: descricao }) : null),
    fechar);
  fechar.addEventListener('click', () => remover(toast));
  garantirArea().append(toast);
  requestAnimationFrame(() => toast.classList.add('entrou'));
  const tempo = duracao || (tipo === 'erro' ? 8000 : 5000);
  let temporizador = setTimeout(() => remover(toast), tempo);
  toast.addEventListener('mouseenter', () => clearTimeout(temporizador));
  toast.addEventListener('mouseleave', () => { temporizador = setTimeout(() => remover(toast), 2000); });
  return toast;
}

export const Toasts = {
  mostrar,
  sucesso: (t, d) => mostrar('sucesso', t, d),
  erro: (t, d) => mostrar('erro', t, d),
  atencao: (t, d) => mostrar('atencao', t, d),
  info: (t, d) => mostrar('info', t, d)
};

export function exibirMensagensDoServidor() {
  const dados = document.getElementById('flash-data');
  if (!dados) return;
  const login = document.querySelector('.alerta-login');
  if (dados.dataset.sucesso) Toasts.sucesso(dados.dataset.sucesso);
  if (dados.dataset.erro && !login) Toasts.erro(dados.dataset.erro);
  if (dados.dataset.aviso) Toasts.atencao(dados.dataset.aviso);
}
