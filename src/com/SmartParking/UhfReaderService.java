package com.SmartParking;

import com.uhf.linkage.Linkage;
import com.uhf.structures.RwData;
import com.uhf.utils.StringUtils;

public class UhfReaderService {

    private boolean inited = false;

    public boolean init(String comPort) {
        int r = Linkage.getInstance().initial(comPort);
        inited = (r == 0);
        return inited;
    }

    public void deinit() {
        if (inited) {
            Linkage.getInstance().deinit();
            inited = false;
        }
    }

    /**
     * 读取标签 EPC + 指定存储区的数据（例如 USER 区）
     *
     * @param password 4字节访问密码，通常全0
     * @param area     读哪个存储区（用你 Demo 里一致的值）
     * @param start    起始 word 地址
     * @param len      读取 word 长度
     * @param perTryTimeoutMs 每次 readTagSync 的超时
     * @param totalTimeoutMs  总超时（防止死循环）
     * @return ReadResult，失败时 ok()=false
     */
    public ReadResult readEpcAndData(byte[] password, int area, int start, int len,
                                     int perTryTimeoutMs, long totalTimeoutMs) {
        if (!inited) return new ReadResult("", "", -1, -1);

        long t0 = System.currentTimeMillis();
        int status = -1;

        while (System.currentTimeMillis() - t0 < totalTimeoutMs) {
            RwData rw = new RwData();
            status = Linkage.getInstance().readTagSync(password, area, start, len, perTryTimeoutMs, rw);

            if (status == 0 && rw.status == 0) {
                String data = (rw.rwDataLen > 0) ? StringUtils.byteToHexString(rw.rwData, rw.rwDataLen) : "";
                String epc  = (rw.epcLen > 0) ? StringUtils.byteToHexString(rw.epc, rw.epcLen) : "";
                return new ReadResult(epc, data, status, rw.status);
            }

            // 小睡一下，避免硬轮询打满 CPU/串口
            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
        }

        return new ReadResult("", "", status, -1);
    }

    // 写 data（用于把停车信息写进 USER 区）
    public boolean writeData(byte[] password, int area, int start, byte[] dataBytes,
                             int perTryTimeoutMs, long totalTimeoutMs) {
        if (!inited) return false;

        // writeTagSync 的 len 参数一般是 word 长度，所以 byte 长度必须是偶数
        if (dataBytes == null || dataBytes.length == 0 || (dataBytes.length % 2 != 0)) return false;

        int lenWords = dataBytes.length / 2;

        long t0 = System.currentTimeMillis();
        int status = -1;
        while (System.currentTimeMillis() - t0 < totalTimeoutMs) {
            RwData rw = new RwData();
            // 这里用你库的 writeTagSync（你的 Demo 里肯定有）
            status = Linkage.getInstance().writeTagSync(password, area, start, lenWords, dataBytes, perTryTimeoutMs, rw);

            if (status == 0 && rw.status == 0) return true;

            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    // ====== 工具：hex <-> bytes ======
    public static byte[] hexToBytes(String hex) {
        if (hex == null) return new byte[0];
        hex = hex.replaceAll("\\s+", "");
        if (hex.length() % 2 != 0) return new byte[0];

        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) return new byte[0];
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    // big-endian 写 int
    public static void putIntBE(byte[] b, int off, int v) {
        b[off]     = (byte) ((v >>> 24) & 0xFF);
        b[off + 1] = (byte) ((v >>> 16) & 0xFF);
        b[off + 2] = (byte) ((v >>>  8) & 0xFF);
        b[off + 3] = (byte) ( v         & 0xFF);
    }

    public static int getIntBE(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24)
                | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8)
                |  (b[off + 3] & 0xFF);
    }

    public static void putShortBE(byte[] b, int off, int v) {
        b[off]     = (byte) ((v >>> 8) & 0xFF);
        b[off + 1] = (byte) ( v        & 0xFF);
    }

    public static int getShortBE(byte[] b, int off) {
        return ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
    }
}




