package com.ancun.chain_storage.node.config;

import com.ancun.chain_storage.node.contracts.ChainStorage;
import com.ancun.chain_storage.node.contracts.FileStorage;
import com.ancun.chain_storage.node.contracts.NodeManager;
import com.ancun.chain_storage.node.contracts.NodeStorage;
import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ContractConfig {
  @Value("${app.ChainStorageAddress}")
  private String chainStorageAddress;

  @Value("${app.NodeStorageAddress}")
  private String nodeStorageAddress;

  @Value("${app.NodeManagerAddress}")
  private String nodeManagerAddress;

  @Value("${app.FileStorageAddress}")
  private String fileStorageAddress;

  @Value("${app.GroupId}")
  private Integer groupId;

  @Autowired private BcosSDK bcosSDK;

  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  @Bean
  String selfAddress() {
    return bcosSDK.getClient(groupId).getCryptoSuite().getCryptoKeyPair().getAddress();
  }

  @Bean
  public Client client() {
    return bcosSDK.getClient(groupId);
  }

  @Bean
  public ChainStorage chainStorage() {
    Client client = bcosSDK.getClient(groupId);
    return ChainStorage.load(
        chainStorageAddress, client, client.getCryptoSuite().getCryptoKeyPair());
  }

  @Bean
  public FileStorage fileStorage() {
    Client client = bcosSDK.getClient(groupId);
    return FileStorage.load(fileStorageAddress, client, client.getCryptoSuite().getCryptoKeyPair());
  }

  @Bean
  public NodeManager nodeManager() {
    Client client = bcosSDK.getClient(groupId);
    return NodeManager.load(nodeManagerAddress, client, client.getCryptoSuite().getCryptoKeyPair());
  }

  @Bean
  public NodeStorage nodeStorage() {
    Client client = bcosSDK.getClient(groupId);
    return NodeStorage.load(nodeStorageAddress, client, client.getCryptoSuite().getCryptoKeyPair());
  }
}
