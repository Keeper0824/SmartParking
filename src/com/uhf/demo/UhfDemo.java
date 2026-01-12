package com.uhf.demo;

import com.uhf.detailwith.InventoryDetailWith;
import com.uhf.linkage.Linkage;
import com.uhf.structures.InventoryArea;
import com.uhf.structures.RwData;
import com.uhf.utils.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class UhfDemo {
    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        int i = Linkage.getInstance().initial("COM5");// 初始化连接设备,参数：端口号
        // function：init， parameter：The port number
        if (i == 0) {
            System.out.println("connect success");
            // 盘点区域设置 setInventoryArea
            // 盘点区域获取 getInventoryArea
            getInventoryArea();
            setInventoryArea();
//            // 开始盘点测试 startInventory
//            startInventory();
//            // 停止盘点测试 stopInventory
//            stopInventory();
//            // epc同步读取 epcReadSync
//            epcReadSync();
//            // epc同步写入 epcWriteSync
//            epcWriteSync();
//            // user同步读取 userReadSync
//            userReadSync();
//            // user同步写入 userWriteSync
//            userWriteSync();
//            // user同步读取 userReadSync
//            userReadSync();
//            // tid同步读取 tidReadSync
//            tidReadSync();
//            // 断开连接 deinit
//            Linkage.getInstance().deinit();

            interactiveCli();
        } else {
            System.out.println("connect failed");
        }

    }

    private static void interactiveCli() {
        Scanner sc = new Scanner(System.in);
        System.out.println("起始地址/长度单位为 word（每word=2字节）。");

        byte[] password = StringUtils.stringToByte("00000000");

        while (true) {
            System.out.println("\n选择操作：");
            System.out.println("[1] 读取 (可选 EPC/TID/USER)");
            System.out.println("[2] 写入 (仅 USER 区)");
            System.out.println("[0] 退出");
            System.out.print("输入：");
            String choice = sc.nextLine().trim();

            if ("0".equals(choice)) {
                System.out.println("退出交互。");
                break;
            } else if ("1".equals(choice)) {
                int area = askArea(sc); // 1=EPC, 2=TID, 3=USER
                int start = askInt(sc, "起始word地址(十进制)：");
                int len   = askInt(sc, "读取长度len(单位word)：");

                RwData rw = new RwData();
                int status = -1;
                while (status != 0) {
                    status = Linkage.getInstance().readTagSync(password, area, start, len, 3000, rw);
                }
                if (status == 0 && rw.status == 0) {
                    String data = (rw.rwDataLen > 0) ? StringUtils.byteToHexString(rw.rwData, rw.rwDataLen) : "";
                    String epc  = (rw.epcLen > 0) ? StringUtils.byteToHexString(rw.epc, rw.epcLen) : "";
                    System.out.println("READ OK area=" + areaName(area) + ", start=" + start + ", len=" + len);
                    System.out.println("回读 HEX ：" + data);
                    try {
                        String afterText = hexToTextTrim(data, StandardCharsets.UTF_8);
                        System.out.println("回读文本 ：" + afterText);
                    } catch (Exception ignore) {}
                    System.out.println("data  === " + data);
                    System.out.println("epc   === " + epc);
                } else {
                    System.out.println("READ FAIL");
                }

            } else if ("2".equals(choice)) {
                System.out.println("写入仅支持 USER 区（area=3）。");
                int start = askInt(sc, "请输入起始 word 地址（十进制）：");
//                int len   = askInt(sc, "请输入写入长度 len（单位：word）：");
//                int needHexLen = len * 4; // 1 word = 2 bytes = 4 hex chars

                System.out.println("请选择输入类型：");
                System.out.println("[1] 文本（支持中文，默认 UTF-8）");
                System.out.println("[2] 十六进制 ");
                System.out.print("输入类型编号：");
                String t = sc.nextLine().trim();

                String hex; // 最终要写入的十六进制串
                if ("2".equals(t)) {
                    System.out.println("请输入十六进制串：");
                    String inputHex = sc.nextLine();
                    String cleaned = inputHex.replaceAll("\\s+","").toUpperCase(Locale.ROOT);
                    hex = cleaned.toString();
                } else {
                    System.out.print("请输入要写入的文本（中文可直接输入）：");
                    String text = sc.nextLine();
                    try {
                        hex = textToHexFitWords(text, StandardCharsets.UTF_8);
                    } catch (IllegalArgumentException ex) {
                        System.out.println("写入失败" + ex.getMessage());
                        return;
                    }
                }

                byte[] data = StringUtils.stringToByte(hex);
                int len=data.length;
                RwData rw = new RwData();
                int status = -1;
                while (status != 0) {
                    status = Linkage.getInstance().writeTagSync(password, 3, start, len, data, 500, rw);
                }
                System.out.println(hex);
                if (status == 0 && rw.status == 0) {
                    System.out.println("写入成功 USER 区，起始地址：" + start + "，长度：" + len);
                    // 回读验证
                    RwData back = new RwData();
                    int rs = -1;
                    while (rs != 0) {
                        rs = Linkage.getInstance().readTagSync(password, 3, start, len/2, 3000, back);
                    }
                    String afterHex = (back.rwDataLen > 0) ? StringUtils.byteToHexString(back.rwData, back.rwDataLen) : "";
                    System.out.println("回读 HEX ：" + afterHex.substring(0,len*2));

                    if ((afterHex.substring(0,len*2)).equalsIgnoreCase(hex.substring(0, len * 2))) {
                        System.out.println("写入与回读一致");
                    } else {
                        System.out.println("写入与回读不一致");
                    }
                } else {
                    System.out.println("写入失败");
                }
    }}}

    private static int askArea(Scanner sc) {
        while (true) {
            System.out.print("选择区域 [1=EPC, 2=TID, 3=USER]：");
            String s = sc.nextLine().trim();
            if ("1".equals(s) || "2".equals(s) || "3".equals(s)) return Integer.parseInt(s);
            System.out.println("输入错误，请重试。");
        }
    }

    private static int askInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v >= 0) return v;
            } catch (Exception ignored) {}
            System.out.println("请输入非负整数。");
        }
    }

    // 规范化十六进制：去空格/非hex，转大写，按 len*4 补零或截断
    private static String normalizeHex(String raw, int lenWords) {
        String s = raw.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
        int need = lenWords * 4;
        if (s.length() < need) {
            StringBuilder sb = new StringBuilder(s);
            while (sb.length() < need) sb.append('0');
            return sb.toString();
        } else if (s.length() > need) {
            return s.substring(0, need);
        }
        return s;
    }

    private static String areaName(int area) {
        switch (area) {
            case 1: return "EPC";
            case 2: return "TID";
            case 3: return "USER";
            default: return "UNKNOWN(" + area + ")";
        }
    }



    // epc区的同步读取
    public static void epcReadSync() {
        byte[] password = StringUtils.stringToByte("00000000");
        RwData rwData = new RwData();
        int status = -1;
        //调用linkage中的epc读取函数 注意参数
        // Invoking the epc reading function in linkage and note the arguments
        //添加循环验证，避免读取失败 Add loop validation to avoid read failure
        while (status != 0) {
            status = Linkage.getInstance().readTagSync(password, 1, 2, 1, 3000, rwData);
        }
        if (status == 0) {
            if (rwData.status == 0) {
                String result = "";
                String epc = "";
                if (rwData.rwDataLen > 0) {
                    result = StringUtils.byteToHexString(rwData.rwData,
                            rwData.rwDataLen);
                }
                if (rwData.epcLen > 0) {
                    epc = StringUtils
                            .byteToHexString(rwData.epc, rwData.epcLen);
                }
                System.out.println("result====" + result);// 3200
                System.out.println("epc====" + epc);// 320030007F263000DDD90140
                System.out.println("read success");
                return;
            }
        }
        System.out.println("read failed");
    }

    public static void epcWriteSync() {
        byte[] password = StringUtils.stringToByte("00000000");
        byte[] writeData = StringUtils.stringToByte("007A");
        RwData rwData = new RwData();
        int status = -1; //调用linkage中的epc写入函数 注意参数
        // Invoking the epc writing function in linkage and note the arguments
        //添加循环验证，避免读取失败 Add loop validation to avoid write failure
        while (status != 0) {
            status = Linkage.getInstance().writeTagSync(password, 1, 2, 1, writeData, 500, rwData);
        }
        if (status == 0) {
            if (rwData.status == 0) {
                String epc = "";
                if (rwData.epcLen > 0) {
                    epc = StringUtils
                            .byteToHexString(rwData.epc, rwData.epcLen);
                }
                System.out.println("epc====" + epc);
                System.out.println("epc write success");
                return;
            }
        }
        System.out.println("epc write failed");
    }

    public static void userReadSync() {
        RwData rwData = new RwData();
        byte[] password = StringUtils.stringToByte("00000000");
        int status = -1;
        //调用linkage中的user读取函数 注意参数  Invoking the user reading function in linkage and note the arguments
        //添加循环验证，避免读取失败 Add loop validation to avoid read failure
        while (status != 0) {
            status = Linkage.getInstance().readTagSync(password, 3, 0, 8, 3000, rwData);
        }
        if (status == 0) {
            String result = "";
            String epc = "";
            if (rwData.status == 0) {
                if (rwData.rwDataLen > 0) {
                    result = StringUtils.byteToHexString(rwData.rwData,
                            rwData.rwDataLen);
                }
                if (rwData.epcLen > 0) {
                    epc = StringUtils
                            .byteToHexString(rwData.epc, rwData.epcLen);
                }
                System.out.println("userData====" + result);
                System.out.println("epc====" + epc);
                System.out.println("user read success");
                return;
            }
        }
        System.out.println("user read failed");
    }

    public static void tidReadSync() {
        RwData rwData = new RwData();
        byte[] password = StringUtils.stringToByte("00000000");
        int status = -1; //调用linkage中的tid读取函数 注意参数  Invoking the tid reading function in linkage and note the arguments
        //添加循环验证，避免读取失败 Add loop validation to avoid read failure
        while (status != 0) {
            status = Linkage.getInstance().readTagSync(password, 2, 0, 1, 3000, rwData);
        }
        if (status == 0) {
            String result = "";
            String epc = "";
            if (rwData.status == 0) {
                if (rwData.rwDataLen > 0) {
                    result = StringUtils.byteToHexString(rwData.rwData,
                            rwData.rwDataLen);
                }
                if (rwData.epcLen > 0) {
                    epc = StringUtils
                            .byteToHexString(rwData.epc, rwData.epcLen);
                }
                System.out.println("tidData====" + result);
                System.out.println("epc====" + epc);
                System.out.println("tid read success");
                return;
            }
        }
        System.out.println("tid read failed");
    }

    public static void userWriteSync() {
        byte[] password = StringUtils.stringToByte("00000000");
        byte[] writeData = StringUtils.stringToByte("2022213460A2022213461A2022213462");
        RwData rwData = new RwData();
        int status = -1;
        //调用linkage中的user写入函数 注意参数  Invoking the user writing function in linkage and note the arguments
        //添加循环验证，避免读取失败 Add loop validation to avoid write failure
        while (status != 0) {
            status = Linkage.getInstance().writeTagSync(password, 3, 0, 8, writeData, 500, rwData);
        }
        if (status == 0) {
            if (rwData.status == 0) {
                String epc = "";
                if (rwData.epcLen > 0) {
                    epc = StringUtils
                            .byteToHexString(rwData.epc, rwData.epcLen);
                }
                System.out.println("epc" + epc);
                System.out.println("user write success");
                return;
            }
        }
        System.out.println("user write failed");
    }

    public static void startInventory() {// 开始盘点 startInventory
        InventoryArea inventory = new InventoryArea();
        inventory.setValue(2, 0, 6);
        Linkage.getInstance().setInventoryArea(inventory);
        InventoryDetailWith.tagCount = 0;// 获取个数  Get the number
        Linkage.getInstance().startInventory(2, 0);
        InventoryDetailWith.startTime = System.currentTimeMillis();// 盘点的开始时间 Start time of Inventory

        while (InventoryDetailWith.totalCount < 100) {

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        stopInventory();// 进行停止盘点 stopInventory

        for (Map<String, Object> _map : InventoryDetailWith.list) {
            System.out.println(_map);
            System.out.println("天线号(antennaPort)：" + _map.get("antennaPort"));
            System.out.println("epc码：" + _map.get("epc"));
            System.out.println("TID/USER码：" + _map.get("externalData"));
            System.out.println("次数(count)：" + _map.get("count"));
            System.out.println("Rssi：" + _map.get("rssi"));
        }

        long m_lEndTime = System.currentTimeMillis();// 当前时间 The current time
        double Rate = Math.ceil((InventoryDetailWith.tagCount * 1.0) * 1000
                / (m_lEndTime - InventoryDetailWith.startTime));

        long total_time = m_lEndTime - InventoryDetailWith.startTime;
        String dateStr = StringUtils.getTimeFromMillisecond(total_time);
        int tag = InventoryDetailWith.list.size();
        System.out.println("盘点速率(Inventory rate)：" + Rate);

        if (tag != 0) {
            System.out.println("盘点时间(Inventory time)：" + dateStr);
        } else {
            System.out.println("盘点时间(Inventory time)：" + "0时0分0秒0毫秒");
        }
        System.out.println("标签个数(The number of tag)：" + tag);

    }

    public static void stopInventory() {// 停止盘点 stopInventory
        Linkage.getInstance().stopInventory();
    }

    // 盘点区域获取 getInventoryArea
    public static void getInventoryArea() {
        InventoryArea inventoryArea = new InventoryArea();
        int status = Linkage.getInstance().getInventoryArea(inventoryArea);
        if (status == 0) {
            System.out.println("area:" + inventoryArea.area);
            System.out.println("startAddr:" + inventoryArea.startAddr);
            System.out.println("wordLen:" + inventoryArea.wordLen);
            System.out.println("getInventoryArea success");
            return;
        }
        System.out.println("getInventoryArea failed");
    }

    // 盘点区域设置 setInventoryArea
    public static void setInventoryArea() {
        InventoryArea inventoryArea = new InventoryArea();
        inventoryArea.setValue(2, 0, 6);// 2为epc+user
        int status = Linkage.getInstance().setInventoryArea(inventoryArea);
        if (status == 0) {
            System.out.println("setInventoryArea success");
            return;
        }
        System.out.println("setInventoryArea failed");
    }


    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) sb.append(String.format(Locale.ROOT, "%02X", b));
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s+", "");
        if ((hex.length() & 1) != 0) throw new IllegalArgumentException("HEX 长度必须为偶数");
        byte[] out = new byte[hex.length()/2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(2*i, 2*i+2), 16);
        }
        return out;
    }

    /** 文本→十六进制（按编码），并对齐到 lenWords*2 字节（word=2字节），不足补 00，超长报错 */
    private static String textToHexFitWords(String text, Charset cs) {
        byte[] raw = text.getBytes(cs);                 // 文本编码成字节
        int padded = (raw.length % 2 == 0) ? raw.length : raw.length + 1;
        byte[] fit = new byte[padded];
        System.arraycopy(raw, 0, fit, 0, raw.length);
        return bytesToHex(fit);
    }

    /** 从十六进制尝试按编码解码为文本，并去掉结尾的 0x00 填充 */
    private static String hexToTextTrim(String hex, Charset cs) {
        byte[] b = hexToBytes(hex);
        // 去掉结尾的 0x00
        int end = b.length;
        while (end > 0 && b[end-1] == 0x00) end--;
        return new String(b, 0, end, cs);
    }

}
