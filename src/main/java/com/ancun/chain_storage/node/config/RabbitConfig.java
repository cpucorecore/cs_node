package com.ancun.chain_storage.node.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
  @Autowired private String selfAddress;

  @Bean
  public Queue tryRequestAddFileQueue() {
    String queueName = "TryRequestAddFile-" + selfAddress;
    return new Queue(queueName, true, false, false);
  }

  @Bean
  public Queue requestAddFileQueue() {
    String queueName = "RequestAddFile-" + selfAddress;
    return new Queue(queueName, true, false, false);
  }

  @Bean
  public Queue tryRequestDeleteFileQueue() {
    String queueName = "TryRequestDeleteFile-" + selfAddress;
    return new Queue(queueName, true, false, false);
  }

  @Bean
  public Queue requestDeleteFileQueue() {
    String queueName = "RequestDeleteFile-" + selfAddress;
    return new Queue(queueName, true, false, false);
  }

  @Bean
  public TopicExchange topicExchange() {
    String exchangeName = "NodeExchange-" + selfAddress;
    return new TopicExchange(exchangeName, true, false, null);
  }

  @Bean
  public Binding bindingTryRequestAddFileQueue() {
    return BindingBuilder.bind(tryRequestAddFileQueue())
        .to(topicExchange())
        .with("TryRequestAddFile");
  }

  @Bean
  public Binding bindingRequestAddFile() {
    return BindingBuilder.bind(requestAddFileQueue()).to(topicExchange()).with("RequestAddFile");
  }

  @Bean
  public Binding bindingTryRequestDeleteFileQueue() {
    return BindingBuilder.bind(tryRequestDeleteFileQueue())
        .to(topicExchange())
        .with("TryRequestDeleteFile");
  }

  @Bean
  public Binding bindingRequestDeleteFile() {
    return BindingBuilder.bind(requestDeleteFileQueue())
        .to(topicExchange())
        .with("RequestDeleteFile");
  }
}
