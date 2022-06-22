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

public class TryRequestAddFileEventCallback implements EventCallback {
  private static Logger logger = LoggerFactory.getLogger(TryRequestAddFileEventCallback.class);

  @Autowired private String selfAddress;

  @Autowired private StringRedisTemplate redis;

  @Autowired private RabbitTemplate rabbitTemplate;

  private ABICodec abiCodec = new ABICodec(new CryptoSuite(SM_TYPE));

  @Override
  public void onReceiveLog(int status, List<EventLog> logs) {
    if (0 == status && null != logs) {
      for (EventLog log : logs) {
        try {
          String logKey = generateLogKey(selfAddress, log);
          if (redis.hasKey(logKey)) {
            logger.warn("duplicated TryRequestAddFileEvent log: {}", logKey);
            continue;
          }

          List<Object> list = abiCodec.decodeEvent(NodeManager.ABI, "TryRequestAddFile", log);
          if (1 != list.size()) {
            logger.error("wrong log:{}", log);
            continue;
          }

          String cid = list.get(0).toString();
          logger.debug("cid: {}", cid);
          redis.opsForValue().set(logKey, "");

          rabbitTemplate.convertAndSend("NodeExchange-" + selfAddress, "TryRequestAddFile", cid);
        } catch (ABICodecException e) {
          logger.error("ABICodecException: {}", e.toString());
        }
      }
    }
  }
}
