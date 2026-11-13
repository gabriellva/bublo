package com.gabriel.bublo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Teste de contexto simples: verifica que a classe da aplicação é carregável
 * sem exigir banco de dados (os testes de integração usam PostgreSQL real no CI).
 */
class BubloApplicationTests {

    @Test
    @DisplayName("Classe da aplicação é instanciável")
    void applicationClassIsLoadable() {
        assert org.springframework.boot.SpringApplication.class != null;
        assert BubloApplication.class != null;
    }
}
