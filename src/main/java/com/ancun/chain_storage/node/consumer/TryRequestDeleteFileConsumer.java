package com.ancun.chain_storage.node.consumer;

import com.ancun.chain_storage.node.TxCallback;
import com.ancun.chain_storage.node.Utils;
import com.ancun.chain_storage.node.contracts.ChainStorage;
import com.ancun.chain_storage.node.contracts.FileStorage;
import java.math.BigInteger;
import org.fisco.bcos.sdk.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TryRequestDeleteFileConsumer {
  Logger logger = LoggerFactory.getLogger(TryRequestDeleteFileConsumer.class);
  @Autowired private String selfAddress;
  @Autowired private ChainStorage chainStorage;
  @Autowired private FileStorage fileStorage;
  @Autowired private TxCallback txCallback;

  @RabbitHandler
  @RabbitListener(queues = "#{queueName.tryRequestDeleteFile(selfAddress)}")
  public void handleMsg(String cid) {
    logger.debug("consume TryRequestDeleteFile cid: {}", cid);

    BigInteger fileStatus = null;
    try {
      fileStatus = fileStorage.getStatus(cid);
    } catch (ContractException e) {
      logger.error("fileStorage.getStatus exception: {}", e);
    }

    logger.debug("fileStatus {}:{}", cid, fileStatus.longValue());
    if (5 != fileStatus.longValue()) {
      logger.warn("file no in FileTryDelete status, escape the cid: {}", cid);
      return;
    }

    logger.info("new log, receive try request add file: {}", cid);
    byte[] txHash = chainStorage.nodeCanDeleteFile(cid, txCallback);
    logger.info("new log, finish node can add file: {}, txHash: {}", cid, Utils.bytes2hex(txHash));
  }
}
