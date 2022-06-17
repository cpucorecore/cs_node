package com.ancun.chain_storage.node;

import com.ancun.chain_storage.node.contracts.NodeStorage;
import org.fisco.bcos.sdk.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanNodeStorage {

  @Value("${app.NodeStorageAddress}")
  private String nodeStorageAddress;

  @Autowired private Client client;

  @Bean
  public NodeStorage getNodeStorage() {
    return NodeStorage.load(nodeStorageAddress, client, client.getCryptoSuite().getCryptoKeyPair());
  }
}
