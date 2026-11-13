const CHAVE_VISAO = 'bublo:acervo-visao';
const BREAKPOINT_DESKTOP = '(min-width: 768px)';

function lerVisao() {
  try { return sessionStorage.getItem(CHAVE_VISAO) === 'tabela' ? 'tabela' : 'cartoes'; } catch (e) { return 'cartoes'; }
}

function iniciarVisao() {
  const area = document.getElementById('acervo-wrap');
  const botoes = document.querySelectorAll('[data-visao]');
  if (!area) return;
  const definir = (visao) => {
    area.dataset.visao = visao;
    botoes.forEach((b) => {
      const ativo = b.dataset.visao === visao;
      b.classList.toggle('ativo', ativo);
      b.setAttribute('aria-pressed', String(ativo));
    });
    try { sessionStorage.setItem(CHAVE_VISAO, visao); } catch (e) { /* sem persistência */ }
  };
  botoes.forEach((b) => b.addEventListener('click', () => definir(b.dataset.visao)));
  definir(lerVisao());
}

function iniciarFiltros() {
  const botao = document.getElementById('btn-filtros');
  const painel = document.getElementById('painel-filtros');
  if (!botao || !painel) return;
  botao.addEventListener('click', () => {
    if (window.matchMedia(BREAKPOINT_DESKTOP).matches) {
      const recolhido = painel.classList.toggle('recolhido');
      botao.setAttribute('aria-expanded', String(!recolhido));
      return;
    }
    window.bootstrap.Offcanvas.getOrCreateInstance(painel).toggle();
  });
}

iniciarVisao();
iniciarFiltros();
