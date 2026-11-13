import { el, debounce } from '../modules/utils.js';

const tipo = document.getElementById('tipoMaterial');
const grupos = document.querySelectorAll('.campos-tipo');
const camposAutores = document.getElementById('camposAutores');
const listaAutores = document.getElementById('listaAutores');
const entrada = document.getElementById('novoAutor');
const SUGESTAO_MINIMA = 2;
const ATRASO_SUGESTAO_MS = 250;

function mostrarCamposDoTipo() {
  grupos.forEach((grupo) => {
    const ativo = grupo.dataset.tipo === tipo.value;
    grupo.hidden = !ativo;
    grupo.querySelectorAll('input').forEach((campo) => { campo.disabled = !ativo; });
  });
}

const nomesAtuais = () => Array.from(camposAutores.querySelectorAll('input')).map((campo) => campo.value);

function desenharAutores() {
  listaAutores.replaceChildren(...nomesAtuais().map((nome, indice) => {
    const remover = el('button', { type: 'button', class: 'remover', 'aria-label': 'Remover ' + nome }, el('i', { class: 'bi bi-x' }));
    remover.addEventListener('click', () => {
      camposAutores.querySelectorAll('input')[indice].remove();
      desenharAutores();
    });
    return el('span', { class: 'chip chip-filtro' }, nome, remover);
  }));
  if (!listaAutores.children.length) listaAutores.replaceChildren(el('span', { class: 'texto-fraco small', text: 'Nenhum autor informado.' }));
}

function adicionarAutor() {
  const nome = entrada.value.trim();
  if (!nome || nomesAtuais().includes(nome)) { entrada.value = ''; return; }
  camposAutores.append(el('input', { type: 'hidden', name: 'novosAutores', value: nome }));
  entrada.value = '';
  desenharAutores();
}

document.getElementById('btnAddAutor').addEventListener('click', adicionarAutor);
entrada.addEventListener('keydown', (e) => { if (e.key === 'Enter') { e.preventDefault(); adicionarAutor(); } });
entrada.addEventListener('input', debounce(() => {
  if (entrada.value.trim().length < SUGESTAO_MINIMA) return;
  fetch('/acervo/sugestoes-autores?termo=' + encodeURIComponent(entrada.value.trim()), { credentials: 'same-origin' })
    .then((r) => (r.ok ? r.json() : []))
    .then((nomes) => {
      document.getElementById('sugestoesAutores').replaceChildren(...nomes.map((nome) => el('option', { value: nome })));
    })
    .catch(() => {});
}, ATRASO_SUGESTAO_MS));

tipo.addEventListener('change', mostrarCamposDoTipo);
mostrarCamposDoTipo();
desenharAutores();
