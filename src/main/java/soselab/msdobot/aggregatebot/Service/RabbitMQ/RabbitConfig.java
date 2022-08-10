package soselab.msdobot.aggregatebot.Service.RabbitMQ;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * this class controls connection with rabbitMQ server<br>
 *
 * to add new consumer connection, you need to add create a new exchange and queue, you can also use existing ones<br>
 * 1. create queue<br>
 * 2. create exchange<br>
 * 3. bind exchange and queue, note that you can set routing key in this step<br>
 * 4. define which class and which function will handle incoming message from your queue<br>
 * 5. create message listener container<br>
 *
 * use existing code below as template
 */
@Configuration
public class RabbitConfig {

    private final RabbitMessageHandler rabbitMessageHandler;

    // todo: maybe exchange, queue and routing key could be read from application properties
    public RabbitConfig(RabbitMessageHandler rabbitMessageHandler, Environment env){
        this.rabbitMessageHandler = rabbitMessageHandler;
//        this.JENKINS_EXCHANGE = "jenkins";
//        this.JENKINS_QUEUE = "jChannel";
        this.JENKINS_EXCHANGE = env.getProperty("discord.rabbitmq.exchange");
        this.JENKINS_QUEUE = env.getProperty("discord.rabbitmq.queue");
    }

    public final String JENKINS_EXCHANGE;
    public final String JENKINS_QUEUE;


    @Bean
    public MessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    // bind: myExchange - <"dog.#"> --> (myQueue)
    //         exchange   routingKey    queue(channel)
//    @Bean
//    Binding binding(Queue q, TopicExchange topicExchange){
//        return BindingBuilder.bind(q).to(topicExchange).with("dog.#");
//    }

    /**
     * create jenkins queue
     * @return
     */
    @Bean
    Queue createJenkinsQueue(){
        return new Queue(JENKINS_QUEUE, true);
    }

    /**
     * create jenkins exchange
     * @return
     */
    @Bean
    TopicExchange exchangeJenkins(){
        return new TopicExchange(JENKINS_EXCHANGE);
    }

    /**
     * bind jenkins queue to exchange
     * @return
     */
    @Bean
    Binding bindJenkins(){
        return BindingBuilder.bind(createJenkinsQueue()).to(exchangeJenkins()).with("jenkins.*");
    }

    /* consumer message function settings */

    /**
     * bind rabbitmq consumer with assigned class and method
     * let assigned method to handle incoming message
     * note that received message type will be byte array
     * @param handler message handler
     * @return instance of message listener adapter
     */
    @Bean
    MessageListenerAdapter jenkinsListener(RabbitMessageHandler handler){
        return new MessageListenerAdapter(handler, "handleJenkinsMessage");
    }

    /* consumer settings */
    /**
     * bind exchange, queue, message handler together
     * @param connectionFactory
     * @return consumer container
     */
    @Bean
    SimpleMessageListenerContainer jenkinsContainer(ConnectionFactory connectionFactory){
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(JENKINS_QUEUE);
        container.setMessageListener(jenkinsListener(rabbitMessageHandler));

        return container;
    }
}
