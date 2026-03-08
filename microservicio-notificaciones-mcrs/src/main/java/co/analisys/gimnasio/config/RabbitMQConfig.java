package co.analisys.gimnasio.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_INSCRIPCIONES = "gimnasio.inscripciones";

    /** Pub/sub: exchange y routing key para eventos del gimnasio (debe coincidir con el productor). */
    public static final String EXCHANGE_EVENTOS = "gimnasio.eventos";
    public static final String ROUTING_KEY_HORARIO_CAMBIADO = "clase.horario.cambiado";
    /** Cola propia de notificaciones para el evento cambio de horario (varios suscriptores pueden tener su cola). */
    public static final String QUEUE_CAMBIO_HORARIO = "notificaciones.cambio-horario-clase";

    @Bean
    public Queue inscripcionesQueue() {
        return new Queue(QUEUE_INSCRIPCIONES, true);
    }

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public Queue cambioHorarioQueue() {
        return new Queue(QUEUE_CAMBIO_HORARIO, true);
    }

    @Bean
    public Binding bindingCambioHorario(Queue cambioHorarioQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(cambioHorarioQueue).to(eventosExchange).with(ROUTING_KEY_HORARIO_CAMBIADO);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
