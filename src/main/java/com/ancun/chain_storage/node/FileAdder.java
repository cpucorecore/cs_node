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
      String cid = null;
      for (byte[] cidHash : nodeCanAddFileCidHashes) {
        try {
          cid = fileStorage.getCid(cidHash);
        } catch (ContractException e) {
          logger.error("fileStorage.getCid failed, exception: {}", e);
          continue;
        }

        long status = 0;
        try {
          status = fileStorage.getStatus(cid).longValue();
        } catch (ContractException e) {
          throw new RuntimeException(e);
        }

        logger.debug("status: {}", status);
        if (4 == status) {
          logger.warn("file {} in FileAdded status, do nodeCancelCanAddFile", cid);
          chainStorage.nodeCancelCanAddFile(cid, txCallback);
          continue;
        } else if (2 == status || 3 == status) { // FileAdding/FilePartialAdded
          // TODO: do ipfs get/pin file, after success then to do 'chainStorage.nodeAddFile(cid)'
          chainStorage.nodeAddFile(cid, txCallback);
          logger.debug("finish add file: {}", cid);
        }
      }
    }
  }
}
