package com.gabriel.bublo.service;

import com.gabriel.bublo.dto.Paginacao;
import com.gabriel.bublo.model.Multa;
import com.gabriel.bublo.model.Pagamento;
import com.gabriel.bublo.repository.MultaRepository;
import com.gabriel.bublo.repository.PagamentoRepository;
import com.gabriel.bublo.repository.LeitorRepository;
import com.gabriel.bublo.repository.EmprestimoRepository;
import com.gabriel.bublo.repository.NotificacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de MultaService (regras de negócio de multas e pagamentos).
 */
class MultaServiceTest {

    private MultaRepository multaRepository;
    private PagamentoRepository pagamentoRepository;
    private LeitorRepository leitorRepository;
    private EmprestimoRepository emprestimoRepository;
    private NotificacaoRepository notificacaoRepository;
    private MultaService service;

    @BeforeEach
    void setUp() {
        multaRepository = mock(MultaRepository.class);
        pagamentoRepository = mock(PagamentoRepository.class);
        leitorRepository = mock(LeitorRepository.class);
        emprestimoRepository = mock(EmprestimoRepository.class);
        notificacaoRepository = mock(NotificacaoRepository.class);
        service = new MultaService(multaRepository, pagamentoRepository, leitorRepository,
                emprestimoRepository, notificacaoRepository);
    }

    @Test
    @DisplayName("Pagamento integral quita a multa")
    void pagarIntegralQuitaMulta() {
        Multa m = new Multa();
        m.setId(1L);
        m.setLeitorId(10L);
        m.setValor(new BigDecimal("10.00"));
        m.setValorPago(BigDecimal.ZERO);
        m.setStatus("PENDENTE");
        m.setDataGeracao(LocalDateTime.now());

        when(multaRepository.buscarPorId(1L)).thenReturn(Optional.of(m));
        when(pagamentoRepository.buscarPorId(any())).thenReturn(null);

        service.pagar(1L, new BigDecimal("10.00"), "Dinheiro", "admin");

        verify(multaRepository).salvar(m);
        assertThat(m.getStatus()).isEqualTo("PAGA");
        assertThat(m.getValorPago()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Pagamento parcial não quita a multa")
    void pagarParcialNaoQuita() {
        Multa m = new Multa();
        m.setId(1L);
        m.setValor(new BigDecimal("10.00"));
        m.setValorPago(BigDecimal.ZERO);
        m.setStatus("PENDENTE");

        when(multaRepository.buscarPorId(1L)).thenReturn(Optional.of(m));
        when(pagamentoRepository.buscarPorId(any())).thenReturn(null);

        service.pagar(1L, new BigDecimal("4.00"), "Dinheiro", "admin");

        assertThat(m.getStatus()).isEqualTo("PAGA_PARCIAL");
        assertThat(m.getValorPago()).isEqualByComparingTo("4.00");
    }

    @Test
    @DisplayName("Valor maior que saldo é rejeitado")
    void pagarMaiorQueSaldoRejeitado() {
        Multa m = new Multa();
        m.setId(1L);
        m.setValor(new BigDecimal("10.00"));
        m.setValorPago(BigDecimal.ZERO);
        m.setStatus("PENDENTE");

        when(multaRepository.buscarPorId(1L)).thenReturn(Optional.of(m));

        assertThatIllegalStateException().isThrownBy(() ->
            service.pagar(1L, new BigDecimal("20.00"), "Dinheiro", "admin"));
    }

    @Test
    @DisplayName("Estorno de pagamento integral volta multa para pendente")
    void estornoVoltaParaPendente() {
        Multa m = new Multa();
        m.setId(1L);
        m.setValor(new BigDecimal("10.00"));
        m.setValorPago(new BigDecimal("10.00"));
        m.setStatus("PAGA");

        Pagamento p = new Pagamento();
        p.setId(100L);
        p.setMultaId(1L);
        p.setValor(new BigDecimal("10.00"));

        when(pagamentoRepository.buscarPorId(100L)).thenReturn(p);
        when(multaRepository.buscarPorId(1L)).thenReturn(Optional.of(m));

        service.estornar(100L, "Erro de lançamento", "admin");

        assertThat(m.getStatus()).isEqualTo("PENDENTE");
        assertThat(m.getValorPago()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(notificacaoRepository).salvar(any());
    }

    @Test
    @DisplayName("Saldo pendente do leitor")
    void saldoPendenteLeitor() {
        when(multaRepository.saldoPendentePorLeitor(10L))
            .thenReturn(new BigDecimal("25.50"));

        BigDecimal saldo = service.saldoPendente(10L);
        assertThat(saldo).isEqualByComparingTo("25.50");
    }

    @Test
    @DisplayName("Leitor com saldo zerado não está bloqueado")
    void leitorSemBloqueio() {
        when(multaRepository.saldoPendentePorLeitor(10L)).thenReturn(BigDecimal.ZERO);
        assertThat(service.leitorBloqueado(10L)).isFalse();
    }

    @Test
    @DisplayName("Leitor com saldo positivo está bloqueado")
    void leitorBloqueado() {
        when(multaRepository.saldoPendentePorLeitor(10L)).thenReturn(new BigDecimal("5.00"));
        assertThat(service.leitorBloqueado(10L)).isTrue();
    }

    @Test
    @DisplayName("Estorno exige justificativa")
    void estornoExigeJustificativa() {
        assertThatIllegalStateException().isThrownBy(() ->
            service.estornar(1L, null, "admin"));
    }
}
