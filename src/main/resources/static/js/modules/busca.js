import { el, debounce } from './utils.js';

const ATRASO_MS = 250;
const GRUPOS = [
  { chave: 'acervo', titulo: 'Acervo', icone: 'bi-book' },
  { chave: 'leitores', titulo: 'Leitores', icone: 'bi-people' },
  { chave: 'emprestimos', titulo: 'Empréstimos', icone: 'bi-arrow-up-right-square' }
];

export function iniciarBusca() {
  const form = document.getElementById('busca-global');
  const campo = document.getElementById('busca-termo');
  const painel = document.getElementById('busca-resultados');
  const limpar = document.getElementById('busca-limpar');
  if (!form || !campo || !painel) return;
  let requisicao = 0;

  const marcarPreenchida = () => form.classList.toggle('preenchida', campo.value.length > 0);
  const fechar = () => { painel.classList.remove('visivel'); campo.setAttribute('aria-expanded', 'false'); };
  const abrir = () => { painel.classList.add('visivel'); campo.setAttribute('aria-expanded', 'true'); };

  function renderizar(termo, dados) {
    const grupos = GRUPOS.filter((g) => (dados[g.chave] || []).length);
    if (!grupos.length) {
      painel.replaceChildren(el('div', { class: 'busca-vazio', text: 'Nada encontrado para “' + termo + '”.' }));
      return abrir();
    }
    const nos = [];
    grupos.forEach((g) => {
      nos.push(el('div', { class: 'busca-grupo-titulo' }, el('i', { class: 'bi ' + g.icone }), g.titulo));
      dados[g.chave].forEach((item) => {
        nos.push(el('a', { class: 'busca-item', href: item.url, role: 'option' },
          el('span', { class: 't', text: item.titulo }), el('span', { class: 'd', text: item.detalhe })));
      });
    });
    nos.push(el('a', { class: 'busca-todos', href: '/busca?termo=' + encodeURIComponent(termo) }, 'Ver todos os resultados', el('i', { class: 'bi bi-arrow-right' })));
    painel.replaceChildren(...nos);
    abrir();
  }

  const buscar = debounce(() => {
    const termo = campo.value.trim();
    if (termo.length < 2) return fechar();
    const minha = ++requisicao;
    fetch('/busca/sugestoes?termo=' + encodeURIComponent(termo), { credentials: 'same-origin', headers: { Accept: 'application/json' } })
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error('HTTP ' + r.status))))
      .then((dados) => { if (minha === requisicao) renderizar(termo, dados); })
      .catch(() => {
        if (minha !== requisicao) return;
        painel.replaceChildren(el('div', { class: 'busca-vazio', text: 'A busca não está disponível agora. Tente novamente.' }));
        abrir();
      });
  }, ATRASO_MS);

  campo.addEventListener('input', () => { marcarPreenchida(); buscar(); });
  campo.addEventListener('focus', () => { if (painel.children.length && campo.value.trim().length >= 2) abrir(); });
  campo.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') { fechar(); campo.blur(); }
    if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return;
    const itens = Array.from(painel.querySelectorAll('.busca-item'));
    if (!itens.length) return;
    e.preventDefault();
    const indice = itens.findIndex((i) => i.classList.contains('ativo'));
    itens.forEach((i) => i.classList.remove('ativo'));
    const proximo = e.key === 'ArrowDown' ? (indice + 1) % itens.length : (indice - 1 + itens.length) % itens.length;
    itens[proximo].classList.add('ativo');
    itens[proximo].scrollIntoView({ block: 'nearest' });
  });
  form.addEventListener('submit', (e) => {
    const ativo = painel.querySelector('.busca-item.ativo');
    if (ativo) { e.preventDefault(); window.location.href = ativo.getAttribute('href'); return; }
    if (campo.value.trim().length < 2) e.preventDefault();
  });
  limpar.addEventListener('click', () => { campo.value = ''; marcarPreenchida(); fechar(); campo.focus(); });
  document.addEventListener('click', (e) => { if (!form.contains(e.target)) fechar(); });
  marcarPreenchida();
}
