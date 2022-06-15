package com.ancun.chain_storage.cs_node;

import com.ancun.chain_storage.cs_node.contracts.ChainStorage;
import com.ancun.chain_storage.cs_node.contracts.NodeManager;
import java.math.BigInteger;
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

public class TryRequestAddFileCallback implements EventCallback {
  private static Logger logger = LoggerFactory.getLogger(TryRequestAddFileCallback.class);
  private Semaphore semaphore = new Semaphore(1, true);
  private ABICodec abiCodec = new ABICodec(new CryptoSuite(1));
  private Set<String> logDedup = new HashSet<>();

  private ChainStorage chainStorage = null;

  public TryRequestAddFileCallback(ChainStorage _chainStorage) {
    chainStorage = _chainStorage;
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

          List<Object> list = abiCodec.decodeEvent(NodeManager.ABI, "TryRequestAddFile", log);
          if (1 != list.size()) {
            logger.error("wrong log:{}", log);
            continue;
          }

          String cid = list.get(0).toString();
          logger.info("cid: {}", cid);

          /*
          ipfs files stat运行的时候如果文件存在返回速度非常快(自行搭建的网络)，如果文件不存在会等很长时间，需要超时控制
          返回的信息里，有文件的类型(directory,file)，文件大小用CumulativeSize

          ipfs@localhost ~]$ ipfs files stat /ipfs/QmQPeNsJPyVWPFDVHb77w8G42Fvo15z4bG2X8D2GhfbSXc
          QmQPeNsJPyVWPFDVHb77w8G42Fvo15z4bG2X8D2GhfbSXc
          Size: 0
          CumulativeSize: 6548
          ChildBlocks: 7
          Type: directory

          [ipfs@localhost ~]$ ipfs files stat /ipfs/QmYoKiB2GNxhuPKMz4BdMNNxUVvo7Gt1qiFUFLhmUCjvYf
          QmYoKiB2GNxhuPKMz4BdMNNxUVvo7Gt1qiFUFLhmUCjvYf
          Size: 5
          CumulativeSize: 13
          ChildBlocks: 0
          Type: file
          */

          chainStorage.nodeCanAddFile(
              cid, BigInteger.valueOf(1000)); // QmQPeNsJPyVWPFDVHb77w8G42Fvo15z4bG2X8D2GhfbSXc
        } catch (ABICodecException e) {
          logger.error(e.toString());
        }
      }
    }

    semaphore.release();
  }
}
