package com.ancun.chain_storage.node;

import com.ancun.chain_storage.node.contracts.ChainStorage;
import com.ancun.chain_storage.node.contracts.FileStorage;
import com.ancun.chain_storage.node.contracts.NodeStorage;
import java.util.List;
import org.fisco.bcos.sdk.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class FileAdder {
  private static Logger logger = LoggerFactory.getLogger(FileAdder.class);
  @Autowired private String selfAddress;
  @Autowired private NodeStorage nodeStorage;
  @Autowired private ChainStorage chainStorage;
  @Autowired private FileStorage fileStorage;
  @Autowired private TxCallback txCallback;

  public void addFiles() {
    List<byte[]> nodeCanAddFileCidHashes = null;
    try {
      nodeCanAddFileCidHashes = nodeStorage.getNodeCanAddFileCidHashes(selfAddress);
    } catch (ContractException e) {
      logger.error("nodeStorage.getNodeCanAddFileCidHashes failed, exception: {}", e);
    }
    if (!nodeCanAddFileCidHashes.isEmpty()) {
      for (byte[] cidHash : nodeCanAddFileCidHashes) {
        String cid = null;
        try {
          cid = fileStorage.getCid(cidHash);
        } catch (ContractException e) {
          logger.error("fileStorage.getCid failed, exception: {}", e);
          continue;
        }

        // TODO: do ipfs get/pin file, after success then to do 'chainStorage.nodeAddFile(cid)'
        logger.info("node duty: should add file cid: {}", cid);
        chainStorage.nodeAddFile(cid, txCallback);
        logger.debug("finish add file: {}", cid);
      }
    }
  }
}
