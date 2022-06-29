package com.ancun.chain_storage.node.event_callback;

import static com.ancun.chain_storage.node.Utils.generateLogKey;
import static org.fisco.bcos.sdk.model.CryptoType.SM_TYPE;

import com.ancun.chain_storage.node.contracts.NodeManager;
import java.util.List;
import javax.annotation.Resource;
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
  @Resource private StringRedisTemplate redis;
  @Resource private RabbitTemplate rabbitTemplate;
  private ABICodec abiCodec = new ABICodec(new CryptoSuite(SM_TYPE));

  private long latestBlockNumberProcessed = 0;

  @Override
  public void onReceiveLog(int status, List<EventLog> logs) {
    if (0 == status && null != logs) {
      for (EventLog log : logs) {
        try {
          logger.debug("log: {}", log);
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

          rabbitTemplate.convertAndSend("NodeExchange-" + selfAddress, "TryRequestAddFile", cid);
          redis.opsForValue().set(logKey, "");

          if (log.getBlockNumber().longValue() > latestBlockNumberProcessed) {
            latestBlockNumberProcessed = log.getBlockNumber().longValue();
            redis.opsForValue().set(selfAddress, String.valueOf(latestBlockNumberProcessed));
          }
        } catch (ABICodecException e) {
          logger.error("ABICodecException: {}", e.toString());
        }
      }
    }
  }
}
