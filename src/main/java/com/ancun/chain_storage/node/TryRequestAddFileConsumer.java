package com.ancun.chain_storage.node;

import com.ancun.chain_storage.node.contracts.ChainStorage;
import com.ancun.chain_storage.node.contracts.FileStorage;
import java.math.BigInteger;
import org.fisco.bcos.sdk.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TryRequestAddFileConsumer {
  Logger logger = LoggerFactory.getLogger(TryRequestAddFileConsumer.class);

  @Autowired private ChainStorage chainStorage;

  @Autowired private FileStorage fileStorage;

  private TxCallback txCallback = new TxCallback();

  @RabbitHandler
  @RabbitListener(queuesToDeclare = @Queue("TryRequestAddFile"))
  public void handleMsg(String cid) {
    logger.debug("consume TryRequestAddFile cid: {}", cid);

    BigInteger fileStatus = null;
    try {
      fileStatus = fileStorage.getStatus(cid);
    } catch (ContractException e) {
      logger.error("fileStorage.getStatus exception: {}", e);
    }

    logger.debug("fileStatus {}:{}", cid, fileStatus.longValue());
    if (1 != fileStatus.longValue()) {
      logger.warn("file no in TryAddFile status, escape the cid: {}", cid);
      return;
    }

    logger.info("new log, receive try request add file: {}", cid);
    byte[] txHash = chainStorage.nodeCanAddFile(cid, BigInteger.valueOf(1000), txCallback);
    logger.info("new log, finish node can add file: {}, txHash: {}", cid, Utils.bytes2hex(txHash));
  }
}
