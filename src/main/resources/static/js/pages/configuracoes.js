const form = document.getElementById('form-config');
const justificativa = document.getElementById('justificativa');
const erro = document.getElementById('erro-justificativa');

function abaDoCampo(campo) {
  const painel = campo.closest('.tab-pane');
  return painel ? document.querySelector('[data-bs-target="#' + painel.id + '"]') : null;
}

form.addEventListener('submit', (e) => {
  const invalidos = Array.from(form.querySelectorAll('input[required], textarea[required]')).filter((c) => !c.value.trim());
  form.querySelectorAll('.is-invalid').forEach((c) => c.classList.remove('is-invalid'));
  erro.hidden = true;
  if (!invalidos.length) return;
  e.preventDefault();
  invalidos.forEach((c) => c.classList.add('is-invalid'));
  if (invalidos.includes(justificativa)) erro.hidden = false;
  const primeiro = invalidos[0];
  const aba = abaDoCampo(primeiro);
  if (aba) window.bootstrap.Tab.getOrCreateInstance(aba).show();
  setTimeout(() => primeiro.focus(), 60);
});
form.addEventListener('input', (e) => e.target.classList.remove('is-invalid'));
