package com.ancun.chain_storage.node;

import org.springframework.stereotype.Component;

@Component
public class QueueName {
  public String requestAddFile(String name) {
    return "RequestAddFile-" + name;
  }

  public String tryRequestAddFile(String name) {
    return "TryRequestAddFile-" + name;
  }
}
