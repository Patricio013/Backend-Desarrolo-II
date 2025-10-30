package com.example.demo.rabbit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class RabbitMqIntegrationTest {

    // Mockeamos el template de AMQP que usaría cualquier servicio publisher
    @MockBean
    private AmqpTemplate amqpTemplate;

    @Autowired
    private AmqpTemplate injectedTemplate;

    @Test
    void contextLoads_y_puedoPublicarConTemplateMockeado() {
        // sanity check: el bean está en contexto
        assertNotNull(injectedTemplate);

        // Simulamos un publish básico (no importa el exchange/routingKey reales)
        String exchange = "test.exchange";
        String routingKey = "test.key";
        String payload = "{\"ok\":true}";

        // when/then: no hace falta stubbing; verificamos que se invoque
        injectedTemplate.convertAndSend(exchange, routingKey, payload);

        verify(amqpTemplate, times(1)).convertAndSend(exchange, routingKey, payload);
        verifyNoMoreInteractions(amqpTemplate);
    }

    /**
     * Config extra si quisieras exponer un bean alternativo.
     * No es estrictamente necesario porque @MockBean ya inyecta el mock.
     */
    @TestConfiguration
    static class NoopConfig {
        @Bean
        AmqpTemplate noopAmqpTemplate() {
            // Este bean sería “overrideado” por @MockBean, pero sirve si
            // ejecutás el test sin @MockBean.
            return mock(AmqpTemplate.class);
        }
    }
}
