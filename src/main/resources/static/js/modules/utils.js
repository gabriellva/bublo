export function debounce(fn, ms) {
  let temporizador;
  return function (...args) {
    clearTimeout(temporizador);
    temporizador = setTimeout(() => fn.apply(this, args), ms);
  };
}

export function csrf() {
  const token = document.querySelector('meta[name="_csrf"]');
  const cabecalho = document.querySelector('meta[name="_csrf_header"]');
  return token && cabecalho ? { cabecalho: cabecalho.content, valor: token.content } : null;
}

export function moeda(valor) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor || 0);
}

export function el(tag, atributos = {}, ...filhos) {
  const no = document.createElement(tag);
  Object.entries(atributos).forEach(([chave, valor]) => {
    if (valor === null || valor === undefined || valor === false) return;
    if (chave === 'class') no.className = valor;
    else if (chave === 'text') no.textContent = valor;
    else no.setAttribute(chave, valor === true ? '' : valor);
  });
  filhos.flat().forEach((filho) => {
    if (filho === null || filho === undefined) return;
    no.append(filho.nodeType ? filho : document.createTextNode(String(filho)));
  });
  return no;
}

export function reduzirMovimento() {
  return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}
