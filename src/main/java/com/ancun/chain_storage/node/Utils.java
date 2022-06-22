package com.ancun.chain_storage.node;

import org.fisco.bcos.sdk.model.EventLog;

public class Utils {
  public static String bytes2hex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    String tmp = null;
    for (byte b : bytes) {
      tmp = Integer.toHexString(0xFF & b);
      if (tmp.length() == 1) {
        tmp = "0" + tmp;
      }
      sb.append(tmp);
    }
    return sb.toString();
  }

  public static String generateLogKey(String selfAddress, EventLog log) {
    return selfAddress
        + ":"
        + log.getBlockNumber().toString()
        + ":"
        + log.getTransactionIndex().toString()
        + ":"
        + log.getLogIndex().toString();
  }
}
