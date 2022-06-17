package com.ancun.chain_storage.node;

import com.ancun.chain_storage.node.contracts.ChainStorage;
import org.fisco.bcos.sdk.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanChainStorage {

  @Value("${app.ChainStorageAddress}")
  private String chainStorageAddress;

  @Autowired private Client client;

  @Bean
  public ChainStorage getChainStorage() {
    return ChainStorage.load(
        chainStorageAddress, client, client.getCryptoSuite().getCryptoKeyPair());
  }
}
