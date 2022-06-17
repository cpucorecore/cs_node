package com.ancun.chain_storage.node;

import com.ancun.chain_storage.node.contracts.NodeManager;
import org.fisco.bcos.sdk.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanNodeManager {

  @Value("${app.NodeManagerAddress}")
  private String nodeManagerAddress;

  @Autowired private Client client;

  @Bean
  public NodeManager getNodeManager() {
    return NodeManager.load(nodeManagerAddress, client, client.getCryptoSuite().getCryptoKeyPair());
  }
}
