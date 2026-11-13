const form = document.getElementById('form-pagamento');
if (form) {
  const saldo = form.dataset.saldo;
  const valor = document.getElementById('valor');
  const ajuda = document.getElementById('ajuda-valor');
  form.querySelectorAll('[data-modo]').forEach((botao) => {
    botao.addEventListener('click', () => {
      const integral = botao.dataset.modo === 'integral';
      form.querySelectorAll('[data-modo]').forEach((b) => {
        b.classList.toggle('ativo', b === botao);
        b.setAttribute('aria-pressed', String(b === botao));
      });
      valor.readOnly = integral;
      if (integral) valor.value = saldo; else { valor.value = ''; valor.focus(); }
      ajuda.textContent = integral ? 'O valor é o saldo pendente completo.' : 'Informe um valor de até ' + Number(saldo).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) + '.';
    });
  });
}
