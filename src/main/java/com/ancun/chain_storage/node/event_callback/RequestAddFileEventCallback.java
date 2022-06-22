package com.ancun.chain_storage.node.event_callback;

import static com.ancun.chain_storage.node.Utils.generateLogKey;
import static org.fisco.bcos.sdk.model.CryptoType.SM_TYPE;

import com.ancun.chain_storage.node.contracts.NodeManager;
import java.util.List;
import org.fisco.bcos.sdk.abi.ABICodec;
import org.fisco.bcos.sdk.abi.ABICodecException;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.eventsub.EventCallback;
import org.fisco.bcos.sdk.model.EventLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RequestAddFileEventCallback implements EventCallback {
  private static Logger logger = LoggerFactory.getLogger(RequestAddFileEventCallback.class);

  @Autowired private String selfAddress;

  @Autowired private StringRedisTemplate redis;

  @Autowired private RabbitTemplate rabbitTemplate;

  private ABICodec abiCodec = new ABICodec(new CryptoSuite(SM_TYPE));

  private long latestBlockNumberProcessed = 0;

  @Override
  public void onReceiveLog(int status, List<EventLog> logs) {
    if (0 == status && null != logs) {
      for (EventLog log : logs) {
        try {
          String logKey = generateLogKey(selfAddress, log);
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
            rabbitTemplate.convertAndSend("NodeExchange-" + selfAddress, "RequestAddFile", cid);

            redis.opsForValue().set(logKey, "");
            if (log.getBlockNumber().longValue() > latestBlockNumberProcessed) {
              latestBlockNumberProcessed = log.getBlockNumber().longValue();
              redis.opsForValue().set(selfAddress, String.valueOf(latestBlockNumberProcessed));
            }
          }
        } catch (ABICodecException e) {
          logger.error("ABICodecException: {}", e);
        }
      }
    }
  }
}
