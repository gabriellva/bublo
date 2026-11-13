import { Toasts } from './toasts.js';

function iniciarSeletorTamanho() {
  document.querySelectorAll('[data-tamanho]').forEach((seletor) => {
    seletor.addEventListener('change', () => {
      const url = new URL(window.location.href);
      url.searchParams.set('tamanho', seletor.value);
      url.searchParams.set('pagina', '0');
      window.location.href = url.toString();
    });
  });
}

function iniciarProtecaoDeAlteracoes() {
  const formularios = document.querySelectorAll('form[data-proteger-alteracoes]');
  if (!formularios.length) return;
  let sujo = false;
  let enviando = false;
  formularios.forEach((form) => {
    form.addEventListener('input', () => { sujo = true; });
    form.addEventListener('change', () => { sujo = true; });
    form.addEventListener('submit', () => { enviando = true; });
  });
  window.addEventListener('beforeunload', (e) => {
    if (!sujo || enviando) return;
    e.preventDefault();
    e.returnValue = '';
  });
}

function iniciarEnvioComEstado() {
  document.addEventListener('submit', (evento) => {
    const form = evento.target;
    if (!(form instanceof HTMLFormElement) || form.method.toLowerCase() !== 'post' || evento.defaultPrevented || form.dataset.confirmar && !form.dataset.confirmado) return;
    const botao = evento.submitter || form.querySelector('button[type="submit"]');
    if (!botao || botao.dataset.semEstado !== undefined) return;
    setTimeout(() => {
      botao.disabled = true;
      botao.dataset.rotuloOriginal = botao.innerHTML;
      botao.innerHTML = '<span class="spinner-border spinner-border-sm" aria-hidden="true"></span><span>Salvando…</span>';
    }, 0);
  });
  window.addEventListener('pageshow', (e) => {
    if (!e.persisted) return;
    document.querySelectorAll('button[data-rotulo-original]').forEach((b) => { b.disabled = false; b.innerHTML = b.dataset.rotuloOriginal; });
  });
}

function iniciarAlternadorDeSenha() {
  const botao = document.getElementById('ver-senha');
  const campo = document.getElementById('password');
  if (!botao || !campo) return;
  botao.addEventListener('click', () => {
    const visivel = campo.type === 'text';
    campo.type = visivel ? 'password' : 'text';
    botao.setAttribute('aria-pressed', String(!visivel));
    botao.setAttribute('aria-label', visivel ? 'Mostrar senha' : 'Ocultar senha');
    botao.firstElementChild.className = visivel ? 'bi bi-eye' : 'bi bi-eye-slash';
  });
}

function iniciarTooltips() {
  if (!window.bootstrap) return;
  document.querySelectorAll('.sidebar-recolhida .nav-item-b[title]').forEach((no) => new window.bootstrap.Tooltip(no, { placement: 'right', trigger: 'hover' }));
}

function iniciarErroDeRede() {
  window.addEventListener('offline', () => Toasts.atencao('Sem conexão', 'Verifique sua rede antes de continuar.'));
}

export function iniciarComuns() {
  iniciarSeletorTamanho();
  iniciarProtecaoDeAlteracoes();
  iniciarEnvioComEstado();
  iniciarAlternadorDeSenha();
  iniciarTooltips();
  iniciarErroDeRede();
}
