const CHAVE = 'bublo:tema';

function atual() {
  return document.documentElement.getAttribute('data-tema') === 'escuro' ? 'escuro' : 'claro';
}

function salvo() {
  try {
    const valor = localStorage.getItem(CHAVE);
    return valor === 'claro' || valor === 'escuro' ? valor : null;
  } catch (e) {
    return null;
  }
}

function atualizarIcone() {
  const icone = document.querySelector('#btn-tema i');
  if (icone) icone.className = atual() === 'escuro' ? 'bi bi-sun' : 'bi bi-moon-stars';
}

function aplicar(tema, persistir) {
  const raiz = document.documentElement;
  raiz.setAttribute('data-tema', tema);
  raiz.setAttribute('data-bs-theme', tema === 'escuro' ? 'dark' : 'light');
  if (persistir) {
    try { localStorage.setItem(CHAVE, tema); } catch (e) { /* armazenamento indisponível */ }
  }
  atualizarIcone();
  window.dispatchEvent(new CustomEvent('bublo:tema:alterado', { detail: { tema } }));
}

export function iniciarTema() {
  const botao = document.getElementById('btn-tema');
  atualizarIcone();
  if (botao) {
    botao.addEventListener('click', () => {
      const raiz = document.documentElement;
      raiz.classList.add('tema-animado');
      aplicar(atual() === 'claro' ? 'escuro' : 'claro', true);
      setTimeout(() => raiz.classList.remove('tema-animado'), 300);
    });
  }
  if (window.matchMedia) {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (!salvo()) aplicar(e.matches ? 'escuro' : 'claro', false);
    });
  }
  window.addEventListener('storage', (e) => {
    if (e.key === CHAVE && (e.newValue === 'claro' || e.newValue === 'escuro')) aplicar(e.newValue, false);
  });
}
