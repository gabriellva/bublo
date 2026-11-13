import { moeda } from '../modules/utils.js';
import { Toasts } from '../modules/toasts.js';

const Chart = window.Chart;
const graficos = {};
let periodoAtual = '30d';

const estilo = (nome) => getComputedStyle(document.documentElement).getPropertyValue(nome).trim();
const cores = () => [1, 2, 3, 4, 5, 6].map((n) => estilo('--grafico-' + n));
const formatoData = (iso) => iso.split('-').reverse().join('/');

function aplicarPadroes() {
  Chart.defaults.color = estilo('--texto-suave');
  Chart.defaults.borderColor = estilo('--borda');
  Chart.defaults.font.family = estilo('--fonte-corpo');
  Chart.defaults.font.size = 12;
  Chart.defaults.plugins.legend.labels.usePointStyle = true;
  Chart.defaults.plugins.legend.labels.boxWidth = 8;
  Chart.defaults.plugins.tooltip.backgroundColor = estilo('--vinho-900');
  Chart.defaults.plugins.tooltip.padding = 10;
  Chart.defaults.plugins.tooltip.cornerRadius = 8;
}

function escalas(extra = {}) {
  return {
    x: { grid: { display: false }, ticks: { maxRotation: 0, autoSkipPadding: 12 } },
    y: { beginAtZero: true, border: { display: false }, ticks: { precision: 0 } },
    ...extra
  };
}

function resumo(id, texto) {
  const alvo = document.querySelector('[data-resumo="' + id + '"]');
  if (alvo) alvo.textContent = texto;
}

function desenhar(id, config, vazio, resumoTexto) {
  const canvas = document.getElementById(id);
  const quadro = canvas.closest('.grafico-quadro');
  const aviso = quadro.querySelector('.grafico-vazio');
  quadro.classList.remove('skeleton');
  if (graficos[id]) { graficos[id].destroy(); delete graficos[id]; }
  aviso.classList.toggle('visivel', vazio);
  canvas.style.visibility = vazio ? 'hidden' : 'visible';
  resumo(id, resumoTexto);
  if (vazio) {
    aviso.textContent = '';
    const icone = document.createElement('i');
    icone.className = 'bi bi-bar-chart fs-3';
    const texto = document.createElement('span');
    texto.textContent = 'Sem dados para este período.';
    aviso.append(icone, texto);
    return;
  }
  graficos[id] = new Chart(canvas, config);
}

const soma = (lista) => lista.reduce((a, b) => a + Number(b), 0);
const resumirSerie = (rotulos, valores) => rotulos.map((r, i) => r + ': ' + valores[i]).join('; ');

function render(dados) {
  const [c1, c2, c3, c4, c5, c6] = cores();
  const paleta = cores();
  const tempo = dados.emprestimosNoTempo;
  desenhar('g-tempo', {
    type: 'line',
    data: { labels: tempo.rotulos, datasets: [{ label: 'Empréstimos', data: tempo.valores, borderColor: c1, backgroundColor: c1 + '22', fill: true, tension: 0.3, pointRadius: (ctx) => (ctx.raw > 0 ? 4 : 0), pointHoverRadius: 5, pointBackgroundColor: c1, borderWidth: 2 }] },
    options: { maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: escalas() }
  }, soma(tempo.valores) === 0, resumirSerie(tempo.rotulos, tempo.valores));

  const mensal = dados.comparacaoMensal;
  desenhar('g-mensal', {
    type: 'bar',
    data: { labels: mensal.rotulos, datasets: [
      { label: 'Empréstimos', data: mensal.emprestimos, backgroundColor: c1, borderRadius: 4 },
      { label: 'Devoluções', data: mensal.devolucoes, backgroundColor: c2, borderRadius: 4 }] },
    options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } }, scales: escalas() }
  }, soma(mensal.emprestimos) + soma(mensal.devolucoes) === 0, resumirSerie(mensal.rotulos, mensal.emprestimos));

  const rosca = (id, serie, cor) => desenhar(id, {
    type: 'doughnut',
    data: { labels: serie.rotulos, datasets: [{ data: serie.valores, backgroundColor: cor, borderColor: estilo('--superficie'), borderWidth: 3 }] },
    options: { maintainAspectRatio: false, cutout: '62%', plugins: { legend: { position: 'bottom' } } }
  }, soma(serie.valores) === 0, resumirSerie(serie.rotulos, serie.valores));

  rosca('g-tipo', document.__tipos, paleta);
  rosca('g-disponibilidade', dados.disponibilidade, [c3, c6]);
  rosca('g-leitores', dados.leitoresPorTipo, [c1, c2]);

  const barras = (id, serie, cor) => desenhar(id, {
    type: 'bar',
    data: { labels: serie.rotulos, datasets: [{ label: 'Empréstimos', data: serie.valores, backgroundColor: cor, borderRadius: 4, maxBarThickness: 22 }] },
    options: { indexAxis: 'y', maintainAspectRatio: false, plugins: { legend: { display: false } },
      scales: { x: { beginAtZero: true, border: { display: false }, ticks: { precision: 0 } }, y: { grid: { display: false }, ticks: { callback(valor) { const r = this.getLabelForValue(valor); return r.length > 30 ? r.slice(0, 29) + '…' : r; } } } } }
  }, soma(serie.valores) === 0, resumirSerie(serie.rotulos, serie.valores));
  barras('g-categoria', dados.porCategoria, c4);
  barras('g-ranking', dados.maisEmprestados, c1);

  const multas = dados.multas;
  desenhar('g-multas', {
    type: 'bar',
    data: { labels: multas.rotulos, datasets: [
      { label: 'Geradas', data: multas.geradas, backgroundColor: c5, borderRadius: 4 },
      { label: 'Pagas', data: multas.pagas, backgroundColor: c3, borderRadius: 4 }] },
    options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' }, tooltip: { callbacks: { label: (item) => item.dataset.label + ': ' + moeda(item.parsed.y) } } },
      scales: escalas({ y: { beginAtZero: true, border: { display: false }, ticks: { callback: (v) => moeda(v) } } }) }
  }, soma(multas.geradas) + soma(multas.pagas) === 0, resumirSerie(multas.rotulos, multas.geradas));

  document.getElementById('kpi-arrecadado').textContent = moeda(dados.arrecadado);
  document.getElementById('sub-emprestimos').textContent = dados.emprestimosPeriodo + ' empréstimo(s) no período';
  document.getElementById('periodo-texto').textContent = formatoData(dados.de) + ' a ' + formatoData(dados.ate);
}

let ultimosDados = null;

function carregar(parametros) {
  document.querySelectorAll('.grafico-quadro').forEach((q) => q.classList.add('skeleton'));
  return fetch('/dashboard/dados?' + parametros, { credentials: 'same-origin', headers: { Accept: 'application/json' } })
    .then((r) => (r.ok ? r.json() : Promise.reject(new Error('HTTP ' + r.status))))
    .then((dados) => { ultimosDados = dados; render(dados); })
    .catch(() => {
      document.querySelectorAll('.grafico-quadro').forEach((q) => q.classList.remove('skeleton'));
      Toasts.erro('Não foi possível carregar os gráficos', 'Atualize a página para tentar novamente.');
    });
}

function escolherPeriodo(periodo) {
  periodoAtual = periodo;
  document.querySelectorAll('#filtro-periodo button').forEach((b) => b.classList.toggle('ativo', b.dataset.periodo === periodo));
  const custom = document.getElementById('periodo-custom');
  custom.classList.toggle('visivel', periodo === 'custom');
  if (periodo === 'custom') return;
  carregar('periodo=' + periodo);
}

function iniciar() {
  const tipos = document.getElementById('dados-tipos');
  document.__tipos = tipos ? JSON.parse(tipos.textContent) : { rotulos: [], valores: [] };
  aplicarPadroes();
  document.querySelectorAll('#filtro-periodo button').forEach((b) => b.addEventListener('click', () => escolherPeriodo(b.dataset.periodo)));
  document.getElementById('periodo-aplicar').addEventListener('click', () => {
    const de = document.getElementById('periodo-de').value;
    const ate = document.getElementById('periodo-ate').value;
    if (!de || !ate) { Toasts.atencao('Informe as duas datas', 'Escolha o início e o fim do período.'); return; }
    carregar('periodo=custom&de=' + de + '&ate=' + ate);
  });
  window.addEventListener('bublo:tema:alterado', () => { aplicarPadroes(); if (ultimosDados) render(ultimosDados); });
  escolherPeriodo(periodoAtual);
}

iniciar();
