package com.ancun.chain_storage.cs_node;

import static org.fisco.bcos.sdk.model.CryptoType.SM_TYPE;

import com.ancun.chain_storage.cs_node.contracts.ChainStorage;
import com.ancun.chain_storage.cs_node.contracts.NodeManager;
import com.ancun.chain_storage.cs_node.contracts.NodeStorage;
import java.math.BigInteger;
import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.config.Config;
import org.fisco.bcos.sdk.config.ConfigOption;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CsNodeApplication {
  private static Logger logger = LoggerFactory.getLogger(CsNodeApplication.class);

  @Value("${ConfigFilePath}")
  private String configFilePath;

  @Value("${GroupId}")
  private Integer groupId;

  @Value("${NodeManagerAddress}")
  private String nodeManagerAddress;

  @Value("${ChainStorageAddress}")
  private String chainStorageAddress;

  @Value("${NodeStorageAddress}")
  private String nodeStorageAddress;

  @Value("${TotalStorageBytes}")
  private BigInteger totalStorageBytes;

  @Value("${Ext}")
  private String ext;

  public static void main(String[] args) {
    SpringApplication.run(CsNodeApplication.class, args);
  }

  @Bean
  public CommandLineRunner commandLineRunner(ApplicationContext c) {
    return args -> {
      ConfigOption configOption = Config.load(configFilePath, SM_TYPE);
      BcosSDK bcosSDK = new BcosSDK(configOption);
      Client client = bcosSDK.getClient(groupId);

      CryptoKeyPair keyPair = client.getCryptoSuite().getCryptoKeyPair();
      String selfAddress = keyPair.getAddress();
      logger.info("node address: {}", selfAddress);

      NodeStorage nodeStorage = NodeStorage.load(nodeStorageAddress, client, keyPair);
      NodeManager nodeManager = NodeManager.load(nodeManagerAddress, client, keyPair);
      ChainStorage chainStorage = ChainStorage.load(chainStorageAddress, client, keyPair);

      if (!nodeStorage.exist(selfAddress)) {
        chainStorage.nodeRegister(totalStorageBytes, ext);
      }

      nodeManager.subscribeTryRequestAddFileEvent(new TryRequestAddFileCallback(chainStorage));
      nodeManager.subscribeRequestAddFileEvent(
          new RequestAddFileCallback(chainStorage, selfAddress));
    };
  }
}
