package com.gabriel.bublo.config;

import com.gabriel.bublo.service.ReservaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Expira reservas vencidas automaticamente: na partida e a cada hora.
 */
@Configuration
@EnableScheduling
public class ReservaScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ReservaScheduler.class);

    private final ReservaService reservaService;

    public ReservaScheduler(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() {
        expirar();
    }

    @Scheduled(cron = "0 5 * * * *")
    public void expirar() {
        try {
            int total = reservaService.expirarVencidas();
            if (total > 0) LOG.info("{} reserva(s) expirada(s) por prazo de retirada vencido.", total);
        } catch (RuntimeException e) {
            LOG.warn("Falha ao expirar reservas vencidas: {}", e.getMessage());
        }
    }
}
