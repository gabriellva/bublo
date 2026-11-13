import { el } from './utils.js';

let modal = null;
let elemento = null;
let aoConfirmar = null;

function construir() {
  elemento = el('div', { class: 'modal fade', tabindex: '-1', 'aria-hidden': 'true', 'aria-labelledby': 'confirmacao-titulo' },
    el('div', { class: 'modal-dialog modal-dialog-centered' },
      el('div', { class: 'modal-content' },
        el('div', { class: 'modal-header' },
          el('h2', { class: 'modal-title', id: 'confirmacao-titulo' }),
          el('button', { type: 'button', class: 'btn-close', 'data-bs-dismiss': 'modal', 'aria-label': 'Fechar' })),
        el('div', { class: 'modal-body' }, el('p', { class: 'mb-0', 'data-campo': 'mensagem' }), el('div', { class: 'confirma-registro', 'data-campo': 'registro', hidden: true })),
        el('div', { class: 'modal-footer' },
          el('button', { type: 'button', class: 'btn btn-outline-secondary', 'data-bs-dismiss': 'modal' }, 'Voltar'),
          el('button', { type: 'button', class: 'btn btn-primary', 'data-campo': 'confirmar' }, 'Confirmar')))));
  document.body.append(elemento);
  elemento.querySelector('[data-campo="confirmar"]').addEventListener('click', () => {
    const acao = aoConfirmar;
    aoConfirmar = null;
    modal.hide();
    if (acao) acao();
  });
  modal = new window.bootstrap.Modal(elemento);
}

export function perguntar({ mensagem, titulo, registro, rotulo, perigo }, confirmar) {
  if (!window.bootstrap) {
    if (window.confirm(mensagem)) confirmar();
    return;
  }
  if (!modal) construir();
  elemento.querySelector('.modal-title').textContent = titulo || 'Confirmar ação';
  elemento.querySelector('[data-campo="mensagem"]').textContent = mensagem;
  const blocoRegistro = elemento.querySelector('[data-campo="registro"]');
  blocoRegistro.hidden = !registro;
  blocoRegistro.textContent = registro || '';
  const botao = elemento.querySelector('[data-campo="confirmar"]');
  botao.textContent = rotulo || 'Confirmar';
  botao.className = 'btn ' + (perigo ? 'btn-danger' : 'btn-primary');
  aoConfirmar = confirmar;
  modal.show();
}

export function iniciarConfirmacoes() {
  document.addEventListener('submit', (evento) => {
    const form = evento.target;
    if (!(form instanceof HTMLFormElement) || !form.dataset.confirmar || form.dataset.confirmado) return;
    evento.preventDefault();
    perguntar({
      mensagem: form.dataset.confirmar,
      titulo: form.dataset.confirmarTitulo,
      registro: form.dataset.confirmarRegistro,
      rotulo: form.dataset.confirmarRotulo,
      perigo: form.dataset.confirmarPerigo !== undefined
    }, () => {
      form.dataset.confirmado = '1';
      form.requestSubmit();
    });
  });
}
