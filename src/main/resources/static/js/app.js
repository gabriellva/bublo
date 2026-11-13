import { iniciarTema } from './modules/tema.js';
import { Toasts, exibirMensagensDoServidor } from './modules/toasts.js';
import { iniciarConfirmacoes, perguntar } from './modules/confirmacoes.js';
import { iniciarNotificacoes } from './modules/notificacoes.js';
import { iniciarBusca } from './modules/busca.js';
import { iniciarShell } from './modules/shell.js';
import { iniciarComuns } from './modules/comuns.js';
import * as Utils from './modules/utils.js';

window.Bublo = { Toasts, Utils, perguntar };

iniciarTema();
iniciarShell();
iniciarConfirmacoes();
iniciarBusca();
iniciarNotificacoes();
iniciarComuns();
exibirMensagensDoServidor();
