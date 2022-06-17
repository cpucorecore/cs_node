package com.ancun.chain_storage.node;

import com.ancun.chain_storage.node.contracts.FileStorage;
import org.fisco.bcos.sdk.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanFileStorage {
  @Value("${app.FileStorageAddress}")
  private String fileStorageAddress;

  @Autowired private Client client;

  @Bean
  public FileStorage getFileStorage() {
    return FileStorage.load(fileStorageAddress, client, client.getCryptoSuite().getCryptoKeyPair());
  }
}
