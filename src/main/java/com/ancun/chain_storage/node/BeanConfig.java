package com.ancun.chain_storage.node;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {
  @Bean
  public FileAdder fileAdder() {
    return new FileAdder();
  }

  @Bean
  public TxCallback txCallback() {
    return new TxCallback();
  }
}
