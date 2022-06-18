package com.ancun.chain_storage.node;

import static org.fisco.bcos.sdk.model.CryptoType.SM_TYPE;

import com.ancun.chain_storage.node.contracts.ChainStorage;
import com.ancun.chain_storage.node.contracts.FileStorage;
import com.ancun.chain_storage.node.contracts.NodeManager;
import com.ancun.chain_storage.node.contracts.NodeStorage;
import java.math.BigInteger;
import java.util.List;
import javax.annotation.Resource;
import org.fisco.bcos.sdk.abi.ABICodec;
import org.fisco.bcos.sdk.abi.ABICodecException;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.eventsub.EventCallback;
import org.fisco.bcos.sdk.model.EventLog;
import org.fisco.bcos.sdk.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootApplication
public class CsNodeApplication implements CommandLineRunner {
  private static Logger logger = LoggerFactory.getLogger(CsNodeApplication.class);

  @Value("${app.TotalStorageBytes}")
  private BigInteger totalStorageBytes;

  @Value("${app.Ext}")
  private String ext;

  @Resource private StringRedisTemplate redis;

  @Resource private RabbitTemplate rabbitTemplate;

  private ABICodec abiCodec = new ABICodec(new CryptoSuite(SM_TYPE));

  @Autowired private Client client;
  @Autowired private NodeStorage nodeStorage;
  @Autowired private NodeManager nodeManager;
  @Autowired private ChainStorage chainStorage;

  @Autowired private FileStorage fileStorage;

  private String selfAddress;
  private String RequestAddFileEventCountKey;
  private String TryRequestAddFileEventCountKey;

  private TxCallback txCallback = new TxCallback();

  public static void main(String[] args) {
    SpringApplication.run(CsNodeApplication.class, args);
  }

  private String generateLogKey(EventLog log) {
    return selfAddress
        + ":"
        + log.getBlockNumber().toString()
        + ":"
        + log.getTransactionIndex().toString()
        + ":"
        + log.getLogIndex().toString();
  }

  @Override
  public void run(String... args) throws Exception {
    CryptoKeyPair keyPair = client.getCryptoSuite().getCryptoKeyPair();
    selfAddress = keyPair.getAddress();
    logger.info("node address: {}", selfAddress);

    RequestAddFileEventCountKey = "RequestAddFileEventCount:" + selfAddress;
    TryRequestAddFileEventCountKey = "TryRequestAddFileEventCount:" + selfAddress;

    if (!nodeStorage.exist(selfAddress)) {
      chainStorage.nodeRegister(totalStorageBytes, ext);
    }

    doAddFiles();

    String block = redis.opsForValue().get(selfAddress);
    logger.debug("log processed at block: {}", block);

    if (block == null) {
      block = "latest";
    }

    logger.debug("subscribe log from block: {}", block);

    nodeManager.subscribeRequestAddFileEvent(
        block,
        "latest",
        null,
        new EventCallback() {
          long RequestAddFileEventCount = 0;
          long latestBlockNumberProcessed = 0;

          @Override
          public void onReceiveLog(int status, List<EventLog> logs) {
            if (0 == status && null != logs) {
              for (int i = 0; i < logs.size(); i++) {
                try {
                  EventLog log = logs.get(i);

                  String logKey = generateLogKey(log);
                  if (redis.hasKey(logKey)) {
                    logger.warn("duplicated RequestAddFileEvent log: {}", logKey);
                    continue;
                  }

                  List<Object> event = abiCodec.decodeEvent(NodeManager.ABI, "RequestAddFile", log);
                  if (2 != event.size()) {
                    logger.error("wrong log: {}", log);
                    continue;
                  }

                  String cid = event.get(0).toString();
                  String nodeAddresses = event.get(1).toString();

                  if (nodeAddresses.contains(selfAddress)) {
                    rabbitTemplate.convertAndSend("cs_direct_exchange", "RequestAddFile", cid);

                    redis.opsForValue().set(logKey, "");
                    RequestAddFileEventCount += 1;
                    redis
                        .opsForValue()
                        .set(RequestAddFileEventCountKey, String.valueOf(RequestAddFileEventCount));
                    redis.opsForValue().set(logKey, "");

                    if (log.getBlockNumber().longValue() > latestBlockNumberProcessed) {
                      latestBlockNumberProcessed = log.getBlockNumber().longValue();
                      redis
                          .opsForValue()
                          .set(selfAddress, String.valueOf(latestBlockNumberProcessed));
                    }
                  }
                } catch (ABICodecException e) {
                  logger.error("ABICodecException: {}", e);
                }
              }
            }
          }
        });

    nodeManager.subscribeTryRequestAddFileEvent(
        block,
        "latest",
        null,
        new EventCallback() {
          int TryRequestAddFileEventCount = 0;

          @Override
          public void onReceiveLog(int status, List<EventLog> logs) {
            if (0 == status && null != logs) {
              for (EventLog log : logs) {
                try {
                  String logKey = generateLogKey(log);
                  if (redis.hasKey(logKey)) {
                    logger.warn("duplicated TryRequestAddFileEvent log: {}", logKey);
                    continue;
                  }

                  List<Object> list =
                      abiCodec.decodeEvent(NodeManager.ABI, "TryRequestAddFile", log);
                  if (1 != list.size()) {
                    logger.error("wrong log:{}", log);
                    continue;
                  }

                  String cid = list.get(0).toString();
                  logger.debug("cid: {}", cid);

                  rabbitTemplate.convertAndSend("cs_direct_exchange", "TryRequestAddFile", cid);

                  redis.opsForValue().set(logKey, "");
                  TryRequestAddFileEventCount += 1;
                  redis
                      .opsForValue()
                      .set(
                          TryRequestAddFileEventCountKey,
                          String.valueOf(TryRequestAddFileEventCount));
                  redis.opsForValue().set(logKey, "");
                } catch (ABICodecException e) {
                  logger.error("ABICodecException: {}", e.toString());
                }
              }
            }
          }
        });
  }

  public void doAddFiles() {
    List<byte[]> nodeCanAddFileCidHashes = null;
    try {
      nodeCanAddFileCidHashes = nodeStorage.getNodeCanAddFileCidHashes(selfAddress);
    } catch (ContractException e) {
      logger.error("nodeStorage.getNodeCanAddFileCidHashes failed, exception: {}", e);
    }
    if (!nodeCanAddFileCidHashes.isEmpty()) {
      for (byte[] cidHash : nodeCanAddFileCidHashes) {
        String cid = null;
        try {
          cid = fileStorage.getCid(cidHash);
        } catch (ContractException e) {
          logger.error("fileStorage.getCid failed, exception: {}", e);
        }
        // TODO: do ipfs get/pin file, after success then to do 'chainStorage.nodeAddFile(cid)'
        logger.info("node duty: should add file cid: {}", cid);
        chainStorage.nodeAddFile(cid, txCallback);
        logger.debug("node duty: finish add file: {}", cid);
      }
    }
  }
}
