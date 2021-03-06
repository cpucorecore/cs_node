package com.ancun.chain_storage.node.consumer;

import com.ancun.chain_storage.node.TxCallback;
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
public class RequestAddFileConsumer {
  Logger logger = LoggerFactory.getLogger(RequestAddFileConsumer.class);

  @Autowired private FileStorage fileStorage;
  @Autowired private ChainStorage chainStorage;
  @Autowired private String selfAddress;

  private TxCallback txCallback = new TxCallback();

  @RabbitHandler
  @RabbitListener(queues = "#{queueName.requestAddFile(selfAddress)}")
  public void handleMsg(String cid) {
    logger.debug("consume RequestAddFile cid: {}", cid);

    try {
      BigInteger fileStatus = fileStorage.getStatus(cid);
      logger.debug("fileStatus {}:{}", cid, fileStatus.longValue());
      if (2 != fileStatus.longValue() && 3 != fileStatus.longValue()) {
        logger.warn("file no in FileAdding/FilePartialAdded status, escape the cid: {}", cid);
        return;
      }

      Boolean nodeExist = fileStorage.nodeExist(cid, selfAddress);
      if (nodeExist) {
        logger.warn("nodeExist, escape the cid: {}", cid);
        return;
      }
    } catch (ContractException e) {
      logger.error("exception: {}", e);
    }

    logger.info("new log, receive request add file: {}", cid);
    // TODO: ipfs add/pin file
    chainStorage.nodeAddFile(cid, txCallback);
    logger.info("new log, node finish add this file: {}", cid);
  }
}
