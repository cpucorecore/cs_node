package com.ancun.chain_storage.node;

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
}
