const CHAVE_MENU = 'bublo:menu';

export function iniciarShell() {
  const raiz = document.documentElement;
  const botaoMenu = document.getElementById('btn-menu');
  const fundo = document.getElementById('sidebar-fundo');
  const recolher = document.getElementById('btn-recolher');

  const fecharMobile = () => {
    raiz.classList.remove('sidebar-aberta');
    if (botaoMenu) botaoMenu.setAttribute('aria-expanded', 'false');
  };

  if (botaoMenu) {
    botaoMenu.addEventListener('click', () => {
      const aberta = raiz.classList.toggle('sidebar-aberta');
      botaoMenu.setAttribute('aria-expanded', String(aberta));
    });
  }
  if (fundo) fundo.addEventListener('click', fecharMobile);
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') fecharMobile(); });

  if (recolher) {
    recolher.addEventListener('click', () => {
      const recolhida = raiz.classList.toggle('sidebar-recolhida');
      try { localStorage.setItem(CHAVE_MENU, recolhida ? 'recolhido' : 'aberto'); } catch (e) { /* sem persistência */ }
    });
  }
  document.querySelectorAll('.nav-item-b').forEach((link) => link.addEventListener('click', fecharMobile));
}
