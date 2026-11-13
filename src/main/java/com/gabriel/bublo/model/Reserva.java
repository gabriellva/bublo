package com.gabriel.bublo.model;

import java.time.LocalDateTime;

/**
 * Reserva de um item do acervo (por título, não por exemplar).
 * Fila FIFO: quem reserva primeiro, é atendido primeiro.
 */
public class Reserva {

    private Long id;
    private Leitor leitor;
    private ItemAcervo itemAcervo;
    private LocalDateTime dataReserva;
    private Integer posicao;
    private LocalDateTime prazoRetirada;
    private String status; // AGUARDANDO, ATENDIDA, CANCELADA, EXPIRADA

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Leitor getLeitor() {
        return leitor;
    }

    public void setLeitor(Leitor leitor) {
        this.leitor = leitor;
    }

    public ItemAcervo getItemAcervo() {
        return itemAcervo;
    }

    public void setItemAcervo(ItemAcervo itemAcervo) {
        this.itemAcervo = itemAcervo;
    }

    public LocalDateTime getDataReserva() {
        return dataReserva;
    }

    public void setDataReserva(LocalDateTime dataReserva) {
        this.dataReserva = dataReserva;
    }

    public Integer getPosicao() {
        return posicao;
    }

    public void setPosicao(Integer posicao) {
        this.posicao = posicao;
    }

    public LocalDateTime getPrazoRetirada() {
        return prazoRetirada;
    }

    public void setPrazoRetirada(LocalDateTime prazoRetirada) {
        this.prazoRetirada = prazoRetirada;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
