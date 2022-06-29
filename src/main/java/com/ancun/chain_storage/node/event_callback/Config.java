package com.ancun.chain_storage.node.event_callback;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
  @Bean
  public RequestAddFileEventCallback requestAddFileEventCallback() {
    return new RequestAddFileEventCallback();
  }

  @Bean
  public TryRequestAddFileEventCallback tryRequestAddFileEventCallback() {
    return new TryRequestAddFileEventCallback();
  }

  @Bean
  public RequestDeleteFileEventCallback requestDeleteFileEventCallback() {
    return new RequestDeleteFileEventCallback();
  }

  @Bean
  public TryRequestDeleteFileEventCallback tryRequestDeleteFileEventCallback() {
    return new TryRequestDeleteFileEventCallback();
  }
}
