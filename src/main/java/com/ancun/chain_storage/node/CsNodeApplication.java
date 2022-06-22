package com.ancun.chain_storage.node;

import com.ancun.chain_storage.node.contracts.ChainStorage;
import com.ancun.chain_storage.node.contracts.NodeManager;
import com.ancun.chain_storage.node.contracts.NodeStorage;
import com.ancun.chain_storage.node.event_callback.RequestAddFileEventCallback;
import com.ancun.chain_storage.node.event_callback.TryRequestAddFileEventCallback;
import java.math.BigInteger;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootApplication
public class CsNodeApplication implements CommandLineRunner {
  private static Logger logger = LoggerFactory.getLogger(CsNodeApplication.class);

  @Value("${app.Ext}")
  private String ext;

  @Value("${app.TotalStorageBytes}")
  private BigInteger totalStorageBytes;

  @Autowired private String selfAddress;
  @Autowired private FileAdder fileAdder;
  @Resource private StringRedisTemplate redis;
  @Autowired private NodeStorage nodeStorage;
  @Autowired private NodeManager nodeManager;
  @Autowired private ChainStorage chainStorage;
  @Autowired private TryRequestAddFileEventCallback tryRequestAddFileEventCallback;
  @Autowired private RequestAddFileEventCallback requestAddFileEventCallback;

  public static void main(String[] args) {
    SpringApplication.run(CsNodeApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    logger.info("node address: {}", selfAddress);

    if (!nodeStorage.exist(selfAddress)) {
      chainStorage.nodeRegister(totalStorageBytes, ext);
    }

    fileAdder.addFiles();

    String block = redis.opsForValue().get(selfAddress);
    logger.debug("log processed at block: {}", block);

    if (block == null) {
      block = "latest";
    }
    logger.debug("subscribe log from block: {}", block);

    nodeManager.subscribeRequestAddFileEvent(block, "latest", null, requestAddFileEventCallback);

    nodeManager.subscribeTryRequestAddFileEvent(
        block, "latest", null, tryRequestAddFileEventCallback);
  }
}
