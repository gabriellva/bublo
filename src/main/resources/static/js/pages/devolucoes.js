import { moeda } from '../modules/utils.js';

const tabela = document.getElementById('tabela-devolucoes');
const MS_POR_DIA = 86400000;

function diasDeAtraso(prevista) {
  const [ano, mes, dia] = prevista.split('-').map(Number);
  const hoje = new Date();
  const base = Date.UTC(hoje.getFullYear(), hoje.getMonth(), hoje.getDate());
  return Math.round((base - Date.UTC(ano, mes - 1, dia)) / MS_POR_DIA);
}

if (tabela) {
  const taxa = parseFloat(tabela.dataset.taxa);
  document.querySelectorAll('form[data-devolucao]').forEach((form) => {
    const dias = diasDeAtraso(form.dataset.prevista);
    if (dias <= 0 || Number.isNaN(taxa)) return;
    form.dataset.confirmar = 'Este exemplar está ' + dias + ' dia(s) em atraso. Será gerada uma multa estimada de ' + moeda(taxa * dias) + ' (' + moeda(taxa) + ' por dia). Registrar a devolução mesmo assim?';
    form.dataset.confirmarRotulo = 'Devolver e gerar multa';
  });
}
