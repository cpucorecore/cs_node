package com.ancun.chain_storage.cs_node;

import com.ancun.chain_storage.cs_node.contracts.ChainStorage;
import com.ancun.chain_storage.cs_node.contracts.NodeManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import org.fisco.bcos.sdk.abi.ABICodec;
import org.fisco.bcos.sdk.abi.ABICodecException;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.eventsub.EventCallback;
import org.fisco.bcos.sdk.model.EventLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestAddFileCallback implements EventCallback {
  private static Logger logger = LoggerFactory.getLogger(RequestAddFileCallback.class);
  private Semaphore semaphore = new Semaphore(1, true);
  private ABICodec abiCodec = new ABICodec(new CryptoSuite(1));
  private Set<String> logDedup = new HashSet<>();

  private ChainStorage chainStorage = null;

  private String selfAddress = null;

  public RequestAddFileCallback(ChainStorage _chainStorage, String _selfAddress) {
    chainStorage = _chainStorage;
    selfAddress = _selfAddress;
  }

  @Override
  public void onReceiveLog(int status, List<EventLog> logs) {
    try {
      semaphore.acquire(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    if (0 == status && null != logs) {
      for (int i = 0; i < logs.size(); i++) {
        try {
          EventLog log = logs.get(i);
          log.getBlockNumber();
          log.getTransactionIndex();
          log.getLogIndex();

          // log可能重复，根据区块号+交易号+日志号去重
          String key =
              log.getBlockNumber().toString()
                  + "-"
                  + log.getTransactionIndex().toString()
                  + "-"
                  + log.getLogIndex().toString();
          if (logDedup.contains(key)) {
            logger.warn("duplicated log:{}", key);
            continue;
          }
          logDedup.add(key);

          List<Object> list = abiCodec.decodeEvent(NodeManager.ABI, "RequestAddFile", log);
          if (2 != list.size()) {
            logger.error("wrong log:{}", log);
            continue;
          }

          String cid = list.get(0).toString();
          String nodeAddresses = list.get(1).toString();
          logger.info("cid:{}, nodeAddresses:{}", cid, nodeAddresses);

          if (nodeAddresses.contains(selfAddress)) {
            // TODO: do ipfs add/pin file, after success then to call 'nodeAddFile' below:
            // ipfs add QmQPeNsJPyVWPFDVHb77w8G42Fvo15z4bG2X8D2GhfbSXc
            chainStorage.nodeAddFile(cid);
            logger.info("finish add cid: {}", cid);
          }
        } catch (ABICodecException e) {
          logger.error(e.toString());
        }
      }
    }

    semaphore.release();
  }
}
