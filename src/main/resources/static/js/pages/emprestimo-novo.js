import { el } from '../modules/utils.js';
import { Toasts } from '../modules/toasts.js';

const form = document.getElementById('formEmprestimo');
const leitor = document.getElementById('leitorId');
const filtroLeitor = document.getElementById('filtroLeitor');
const exemplares = document.getElementById('exemplares');
const filtroExemplar = document.getElementById('filtroExemplar');
const lista = document.getElementById('listaExemplares');
const contagem = document.getElementById('contagemExemplares');
const data = document.getElementById('dataPrevista');
const status = document.getElementById('statusBloqueio');
const resumo = document.getElementById('resumo');
const passos = Array.from(document.querySelectorAll('#passos li'));
const paineis = Array.from(document.querySelectorAll('.passo-painel'));
const todosLeitores = Array.from(leitor.options).filter((o) => o.value);
let bloqueado = false;
let passoAtual = 1;

const normalizar = (texto) => texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
const formatarData = (iso) => (iso ? iso.split('-').reverse().join('/') : '—');

function irPara(passo) {
  passoAtual = passo;
  paineis.forEach((p) => p.classList.toggle('atual', Number(p.dataset.passo) === passo));
  passos.forEach((li, indice) => {
    li.classList.toggle('atual', indice + 1 === passo);
    li.classList.toggle('feito', indice + 1 < passo);
  });
  if (passo === 4) montarResumo();
  const alvo = paineis[passo - 1].querySelector('input:not([type=hidden]), select');
  if (alvo) alvo.focus({ preventScroll: true });
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function validar(destino) {
  if (destino <= passoAtual) return true;
  if (!leitor.value) { Toasts.atencao('Escolha o leitor', 'Selecione quem está retirando os exemplares.'); return false; }
  if (bloqueado) { Toasts.erro('Leitor bloqueado', 'Há multa pendente. Registre o pagamento antes de emprestar.'); return false; }
  if (destino > 2 && !exemplares.selectedOptions.length) { Toasts.atencao('Escolha os exemplares', 'Marque ao menos um exemplar disponível.'); return false; }
  if (destino > 3 && !data.value) { Toasts.atencao('Informe a data de devolução', 'Defina até quando o empréstimo vale.'); data.focus(); return false; }
  return true;
}

function sugerirData() {
  if (data.value || !leitor.value) return;
  const tipo = leitor.selectedOptions[0].dataset.tipo;
  const dias = parseInt(tipo === 'PROFESSOR' ? form.dataset.prazoProfessor : form.dataset.prazoAluno, 10);
  const previsao = new Date();
  previsao.setDate(previsao.getDate() + dias);
  data.value = previsao.toISOString().slice(0, 10);
  document.getElementById('ajudaPrazo').textContent = 'Sugestão: ' + dias + ' dias para ' + (tipo === 'PROFESSOR' ? 'professores' : 'alunos') + '. Você pode alterar.';
}

function verificarRestricao() {
  status.className = 'estado-leitor';
  status.textContent = '';
  bloqueado = false;
  if (!leitor.value) return;
  sugerirData();
  fetch('/emprestimos/restricao/' + encodeURIComponent(leitor.value), { credentials: 'same-origin' })
    .then((r) => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
    .then((d) => {
      bloqueado = d.bloqueado;
      status.classList.add(d.bloqueado ? 'bloqueado' : 'ok');
      status.replaceChildren(el('i', { class: 'bi ' + (d.bloqueado ? 'bi-slash-circle' : 'bi-check-circle') }),
        d.bloqueado ? 'Leitor bloqueado: possui multa pendente.' : 'Sem pendências. Empréstimos ativos: ' + d.ativos + '.');
    })
    .catch(() => Toasts.atencao('Não foi possível verificar as pendências do leitor.'));
}

function filtrarLeitores() {
  const termo = normalizar(filtroLeitor.value.trim());
  const atual = leitor.value;
  leitor.replaceChildren(el('option', { value: '' }, 'Selecione…'));
  todosLeitores.filter((o) => !termo || normalizar(o.textContent).includes(termo) || o.value === atual)
    .forEach((o) => { o.selected = o.value === atual; leitor.append(o); });
  if (termo && leitor.options.length === 2 && !atual) { leitor.selectedIndex = 1; verificarRestricao(); }
}

function atualizarContagem() {
  const n = exemplares.selectedOptions.length;
  contagem.textContent = n ? n + ' exemplar(es) selecionado(s).' : 'Nenhum exemplar selecionado.';
}

function desenharExemplares() {
  const termo = normalizar(filtroExemplar.value.trim());
  const opcoes = Array.from(exemplares.options).filter((o) => !termo || normalizar(o.textContent).includes(termo));
  if (!opcoes.length) {
    lista.replaceChildren(el('div', { class: 'vazio compacto' }, el('h3', { text: exemplares.options.length ? 'Nenhum exemplar com esse termo' : 'Não há exemplares disponíveis no momento' })));
    return atualizarContagem();
  }
  lista.replaceChildren(...opcoes.map((o) => {
    const caixa = el('input', { type: 'checkbox', class: 'form-check-input', id: 'ex-' + o.value, 'aria-label': o.textContent });
    caixa.checked = o.selected;
    const linha = el('label', { class: 'opcao-selecao' + (o.selected ? ' marcada' : ''), for: 'ex-' + o.value }, caixa,
      el('span', { class: 'info' }, el('strong', { text: o.dataset.titulo }), el('span', { class: 'tombo', text: o.dataset.tomb })));
    caixa.addEventListener('change', () => {
      o.selected = caixa.checked;
      linha.classList.toggle('marcada', caixa.checked);
      atualizarContagem();
    });
    return linha;
  }));
  atualizarContagem();
}

function montarResumo() {
  const itens = Array.from(exemplares.selectedOptions);
  resumo.replaceChildren(
    el('li', {}, el('span', { class: 'k', text: 'Leitor' }), el('span', { class: 'v', text: leitor.selectedOptions[0] ? leitor.selectedOptions[0].textContent : '—' })),
    el('li', {}, el('span', { class: 'k', text: 'Exemplares' }), el('span', { class: 'v' }, ...itens.map((o) => el('div', { text: o.textContent })))),
    el('li', {}, el('span', { class: 'k', text: 'Devolução prevista' }), el('span', { class: 'v numero', text: formatarData(data.value) })));
}

form.addEventListener('click', (e) => {
  const botao = e.target.closest('[data-ir]');
  if (!botao) return;
  const destino = Number(botao.dataset.ir);
  if (validar(destino)) irPara(destino);
});
form.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && e.target.tagName === 'INPUT' && e.target.type !== 'submit') {
    e.preventDefault();
    const proximo = Math.min(4, passoAtual + 1);
    if (validar(proximo)) irPara(proximo);
  }
});
form.addEventListener('submit', (e) => {
  if (bloqueado || !leitor.value || !exemplares.selectedOptions.length || !data.value) {
    e.preventDefault();
    Toasts.atencao('Revise o empréstimo', 'Leitor, exemplares e data de devolução são obrigatórios.');
  }
});
leitor.addEventListener('change', () => { data.value = ''; verificarRestricao(); });
filtroLeitor.addEventListener('input', filtrarLeitores);
filtroExemplar.addEventListener('input', desenharExemplares);

desenharExemplares();
verificarRestricao();
if (form.dataset.comErro === 'true' && leitor.value && exemplares.selectedOptions.length && data.value) irPara(4);
