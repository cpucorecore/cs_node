package com.ancun.chain_storage.node;

import com.ancun.chain_storage.node.contracts.FileStorage;
import com.ancun.chain_storage.node.contracts.NodeStorage;
import java.math.BigInteger;
import java.util.List;
import javax.annotation.Resource;
import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

public class Monitor extends Thread {
  private Logger logger = LoggerFactory.getLogger(Monitor.class);

  @Value("${app.CheckAll}")
  private Boolean checkAll;

  @Value("${app.CheckInterval}")
  private long checkInterval;

  @Resource private StringRedisTemplate redis;

  @Autowired private String selfAddress;
  @Autowired private NodeStorage nodeStorage;
  @Autowired private FileStorage fileStorage;

  private static final long PageSize = 50;
  private static final BigInteger pageSize = BigInteger.valueOf(PageSize);

  public void monit() {
    logger.info("begin monitor");
    String redisKey = "PageNumber-" + selfAddress;

    BigInteger pageNumber = BigInteger.valueOf(1);
    if (!checkAll) {
      if (redis.hasKey(redisKey)) {
        String redisPageNumberString = redis.opsForValue().get(redisKey);
        long redisPageNumber = Integer.valueOf(redisPageNumberString);
        if (redisPageNumber > 1) {
          pageNumber = BigInteger.valueOf(redisPageNumber);
        }
      }
    }

    try {
      Tuple2<List<byte[]>, Boolean> result = null;
      do {
        result = nodeStorage.getCidHashes(selfAddress, pageSize, pageNumber);
        for (byte[] cidHash : result.getValue1()) {
          String cid = fileStorage.getCid(cidHash);
          if (!isPinned(cid)) {
            pin(cid);
          }
        }

        if (result.getValue1().size() == PageSize) {
          redis.opsForValue().set(redisKey, pageNumber.toString());
        }

        pageNumber = pageNumber.add(BigInteger.valueOf(1));
      } while (false == result.getValue2() && result.getValue1().size() > 0);
    } catch (ContractException e) {
      logger.error("ContractException:{}", e);
    }

    logger.info("finish monitor");
  }

  @Override
  public void run() {
    while (true) {
      try {
        sleep(checkInterval * 1000);
      } catch (InterruptedException e) {
        logger.error("sleep failed:{}", e);
        break;
      }

      monit();
    }
  }

  private Boolean isPinned(String cid) {
    // TODO: use ipfs 'pin/ls' api to check pin status

    /*
    pin:
    [ipfs@localhost ~]$ curl -X POST  "http://127.0.0.1:6001/api/v0/pin/ls?arg=/ipfs/QmYurnA17imKSoKk2K3gYVwAxW9LrhHbbMFSYHMPr5EoNc&type=all&quiet=true"
    {"Keys":{"QmYurnA17imKSoKk2K3gYVwAxW9LrhHbbMFSYHMPr5EoNc":{"Type":"recursive"}}}

    unpin
    ipfs@ac:~$ curl -X POST  "http://127.0.0.1:6001/api/v0/pin/ls?arg=/ipfs/QmYurnA17imKSoKk2K3gYVwAxW9LrhHbbMFSYHMPr5EoNc&type=all&quiet=true"
    {"Message":"path '/ipfs/QmYurnA17imKSoKk2K3gYVwAxW9LrhHbbMFSYHMPr5EoNc' is not pinned","Code":0,"Type":"error"}
    * */
    return false;
  }

  private void pin(String cid) {
    // TODO: use ipfs 'pin/add' api to add pin
    logger.info("pin:{}", cid);
  }
}
