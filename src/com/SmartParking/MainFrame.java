package com.SmartParking;

import com.SerialPort.manage.SerialPortManager;
import com.SerialPort.utils.ByteUtils;
import com.SerialPort.utils.ShowUtils;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * 主界面 MainFrame.java
 */
public class MainFrame extends JFrame {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1200;

    // 业务参数
    private static final double LUX_THRESHOLD = 500;
    private static final int DATA_AREA = 3;
    private static final int DATA_START = 0;
    private static final int DATA_WORDS = 6;
    private static final int MAGIC = 0xCAFE;
    private static final double PRICE_PER_MIN = 0.5;
    private static final int SLOT_COUNT = 20;
    private static final long OFFLINE_MS = 15_000;
    private static final int FRAME_LEN = 26;

    // 请替换为你自己的 Key
    private static final String DEEPSEEK_API_KEY = "xxx";
    private static final String AMAP_KEY = "xxx";
    // 默认城市编码 (北京)，实际可根据需扩展
    private static final String CITY_ADCODE = "110000";
    private final Slot[] slots = new Slot[SLOT_COUNT];
    private final java.util.Map<String, Integer> macToSlot = new java.util.HashMap<>();
    //  核心组件
    private ParkingLotPanel parkingUI;
    private JScrollPane parkingScrollPane;
    // 记录RFID入场的车辆数，用于对比逃票
    private int rfidInCount = 0;
    private JLabel alarmLabel = new JLabel("系统状态：正常");
    // 串口设置面板
    private JPanel serialPortPanel = new JPanel();
    private JLabel serialPortLabel = new JLabel("串口");
    private JLabel baudrateLabel = new JLabel("波特率");
    private JComboBox commChoice = new JComboBox();
    private JComboBox baudrateChoice = new JComboBox();
    // 操作面板
    private JPanel operatePanel = new JPanel();
    private JTextField dataInput = new JTextField();
    private JButton serialPortOperate = new JButton("打开串口");
    private JButton sendData = new JButton("发送数据");
    private List<String> commList = null;
    private SerialPort serialport;

    // RFID
    private JButton btnRfidConnect = new JButton("连接RFID");
    private JButton btnEntry = new JButton("车辆入场(刷卡)");
    private JButton btnExit = new JButton("车辆出场(刷卡)");
    private UhfReaderService uhf = new UhfReaderService();
    private boolean uhfReady = false;
    private JPanel rfidPanel = new JPanel();
    private JTextArea rfidView = new JTextArea();
    private JScrollPane scrollRfidView = new JScrollPane(rfidView);
    private JPanel rfidSettingPanel = new JPanel();
    private JLabel rfidPortLabel = new JLabel("RFID 串口"); // 标签
    private JComboBox rfidCommChoice = new JComboBox();   // 下拉框

    public MainFrame() {
        initSlots();
        initView();
        initComponents();
        actionListener();
        initData();
    }

    private static String normalizeAddr(String s) {
        if (s == null) return "";
        return s.replace("0x", "").replace("0X", "").replaceAll("\\s+", "").toUpperCase();
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new MainFrame().setVisible(true));
    }

    private static int nowMin() {
        return (int) (System.currentTimeMillis() / 60000L);
    }

    private static byte[] packInfo(int inMin, int outMin, int feeCents) {
        byte[] b = new byte[12];
        UhfReaderService.putShortBE(b, 0, MAGIC);
        UhfReaderService.putIntBE(b, 2, inMin);
        UhfReaderService.putIntBE(b, 6, outMin);
        UhfReaderService.putShortBE(b, 10, feeCents);
        return b;
    }

    private static ParkInfo unpackInfo(byte[] b) {
        ParkInfo p = new ParkInfo();
        if (b == null || b.length < 12) return p;
        p.magic = UhfReaderService.getShortBE(b, 0);
        p.inMin = UhfReaderService.getIntBE(b, 2);
        p.outMin = UhfReaderService.getIntBE(b, 6);
        p.feeCents = UhfReaderService.getShortBE(b, 10);
        return p;
    }

    private static int feeCentsByMinutes(int minutes) {
        if (minutes < 1) minutes = 1;
        double fee = minutes * PRICE_PER_MIN;
        return (int) Math.round(fee * 100.0);
    }

    private static int u16LE(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    private static String bytesToHex(byte[] b, int off, int len) {
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i = 0; i < len; i++) sb.append(String.format("%02X", b[off + i]));
        return sb.toString();
    }

    private static WsnFrame parseWsnFrame(byte[] env) {
        if (env == null || env.length < FRAME_LEN) return null;
        if ((env[0] & 0xFF) != 0xFF || (env[1] & 0xFF) != 0xFF) return null;
        if ((env[24] & 0xFF) != 0xFE || (env[25] & 0xFF) != 0xFE) return null;

        WsnFrame f = new WsnFrame();
        f.shortAddr = bytesToHex(env, 2, 2);
        f.mac = bytesToHex(env, 4, 8);
        f.lux = u16LE(env, 22);
        return f;
    }

    private void initSlots() {
        String[] macList = new String[SLOT_COUNT];
        // 记得在这里填入你真实的 MAC 地址
        macList[0] = "58D1E107004B1200";
        for (int i = 1; i < SLOT_COUNT; i++) {
            macList[i] = "MAC_SLOT_" + (i + 1);
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            String mac = normalizeAddr(macList[i]);
            slots[i] = new Slot(i + 1, mac);
            macToSlot.put(mac, i);
        }
    }

    private void initView() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        setBounds(p.x - WIDTH / 2, p.y - HEIGHT / 2, WIDTH, HEIGHT);
        this.setLayout(null);
        setTitle("智慧停车场管理系统 - V2.0");
    }

    // 统一创建大字体的边框
    private javax.swing.border.Border createBigTitleBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("Microsoft YaHei", Font.BOLD, 24),
                Color.DARK_GRAY
        );
    }

    private void initComponents() {
        Font baseFont = new Font("Microsoft YaHei", Font.PLAIN, 22);

        // 1. 可视化面板 (保持不变)
        parkingUI = new ParkingLotPanel();
        parkingUI.setSpotClickListener((block, row, col) -> {
            int spotId = block * 10 + row * 2 + col + 1;
            showSpotDetailDialog(spotId);
        });

        parkingScrollPane = new JScrollPane(parkingUI);
        parkingScrollPane.setBounds(20, 20, 1160, 690);
        parkingScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        parkingScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        parkingScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        add(parkingScrollPane);

        // 2. 串口设置 (保持不变)
        serialPortPanel.setBorder(createBigTitleBorder("串口设置"));
        serialPortPanel.setFont(baseFont);
        serialPortPanel.setBounds(40, 730, 660, 230);
        serialPortPanel.setLayout(null);
        add(serialPortPanel);

        serialPortLabel.setBounds(40, 60, 160, 40);
        serialPortLabel.setFont(baseFont);
        serialPortPanel.add(serialPortLabel);

        commChoice.setBounds(240, 60, 380, 40);
        commChoice.setFont(baseFont);
        serialPortPanel.add(commChoice);

        baudrateLabel.setBounds(40, 130, 160, 40);
        baudrateLabel.setFont(baseFont);
        serialPortPanel.add(baudrateLabel);

        baudrateChoice.setBounds(240, 130, 380, 40);
        baudrateChoice.setFont(baseFont);
        serialPortPanel.add(baudrateChoice);

        // 坐标计算: WSN面板Y(730) + 高度(230) + 间距(10) = 970
        rfidSettingPanel.setBorder(createBigTitleBorder("RFID 设置"));
        rfidSettingPanel.setFont(baseFont);
        rfidSettingPanel.setBounds(40, 970, 660, 150); // 高度给150够用了
        rfidSettingPanel.setLayout(null);
        add(rfidSettingPanel);

        // 标签
        rfidPortLabel.setBounds(40, 50, 160, 40);
        rfidPortLabel.setFont(baseFont);
        rfidSettingPanel.add(rfidPortLabel);

        // 下拉框
        rfidCommChoice.setBounds(240, 50, 380, 40);
        rfidCommChoice.setFont(baseFont);
        rfidSettingPanel.add(rfidCommChoice);

        // 3. 操作面板 (保持不变)
        operatePanel.setBorder(createBigTitleBorder("操作"));
        operatePanel.setFont(baseFont);
        operatePanel.setBounds(750, 730, 1120, 390);
        operatePanel.setLayout(null);
        add(operatePanel);

        dataInput.setBounds(100, 60, 920, 60);
        dataInput.setFont(baseFont);
        operatePanel.add(dataInput);

        serialPortOperate.setBounds(180, 140, 360, 60);
        serialPortOperate.setFont(baseFont);
        operatePanel.add(serialPortOperate);

        sendData.setBounds(180, 300, 360, 60);
        sendData.setFont(baseFont);
        operatePanel.add(sendData);

        btnRfidConnect.setBounds(620, 140, 360, 60);
        btnRfidConnect.setFont(baseFont);
        operatePanel.add(btnRfidConnect);

        btnEntry.setBounds(180, 220, 360, 60);
        btnEntry.setFont(baseFont);
        operatePanel.add(btnEntry);

        btnExit.setBounds(620, 220, 360, 60);
        btnExit.setFont(baseFont);
        operatePanel.add(btnExit);

        // 4. RFID 日志 & 系统状态 (右上角)
        rfidPanel.setBorder(createBigTitleBorder("系统状态 & RFID 日志"));
        rfidPanel.setLayout(null);
        rfidPanel.setBounds(1200, 40, 670, 660); // 坐标移动到右上角，尺寸变大
        add(rfidPanel);

        // 4.1 系统状态标签 (防逃票警告)
        alarmLabel.setBounds(20, 40, 630, 40);
        alarmLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
        alarmLabel.setForeground(new Color(46, 204, 113));
        rfidPanel.add(alarmLabel);

        // 4.2 RFID 日志显示区域
        rfidView.setEditable(false);
        rfidView.setFont(new Font("Monospaced", Font.PLAIN, 18));
        // 调整 ScrollPane 的大小以填满剩余空间
        scrollRfidView.setBounds(20, 90, 630, 550);
        rfidPanel.add(scrollRfidView);

        // 启动定时器
        new javax.swing.Timer(3000, e -> refreshSlotPanelText()).start();
    }

    private void showSpotDetailDialog(int spotId) {
        if (spotId < 1 || spotId > SLOT_COUNT) return;
        Slot s = slots[spotId - 1];

        // 1. 准备数据
        String statusStr = s.status == SlotStatus.OCCUPIED ? "占用" : (s.status == SlotStatus.FREE ? "空闲" : "离线");
        long delay = (s.lastSeenMs == 0) ? -1 : (System.currentTimeMillis() - s.lastSeenMs);
        String delayStr = delay == -1 ? "无数据" : (delay / 1000 + "秒前");
        String colorStyle = "color:gray;";
        if (s.status == SlotStatus.OCCUPIED) colorStyle = "color:#E74C3C;";
        else if (s.status == SlotStatus.FREE) colorStyle = "color:#2ECC71;";

        // 2. 准备 HTML 内容
        String msg = String.format(
                "<html><body style='width: 380px; font-family: Microsoft YaHei; text-align: center;'>" +
                        "<div style='font-size: 28px; font-weight: bold; margin-bottom: 20px; margin-top: 10px;'>车位 %02d 详情</div>" +
                        "<hr style='border: 1px solid #eee;'>" +
                        "<div style='font-size: 24px; line-height: 2.0; margin-top: 15px; text-align: left; margin-left: 50px;'>" +
                        "<b>当前状态：</b> <span style='%s font-weight:bold;'>%s</span><br>" +
                        "<b>光照数值：</b> %.1f Lux<br>" +
                        "<b>设备 MAC：</b> <span style='font-size: 20px;'>%s</span><br>" +
                        "<b>上次通信：</b> %s" +
                        "</div>" +
                        "</body></html>",
                s.id, colorStyle, statusStr, s.lastLux, s.mac, delayStr
        );

        // 3. 创建自定义
        JDialog dialog = new JDialog(this, "车位信息查询", true); // true 表示模态窗口(不可点其他地方)
        dialog.setSize(500, 600); // 设置足够大的窗口尺寸
        dialog.setLocationRelativeTo(this); // 居中显示
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE); // 背景纯白

        // 中间内容区
        JLabel contentLabel = new JLabel(msg);
        contentLabel.setHorizontalAlignment(SwingConstants.CENTER);
        dialog.add(contentLabel, BorderLayout.CENTER);

        // 底部按钮区
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // 手动创建大按钮
        JButton btnOk = new JButton("确 定");
        btnOk.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
        btnOk.setBackground(new Color(52, 152, 219)); // 蓝色
        btnOk.setForeground(Color.WHITE); // 白色文字
        btnOk.setFocusPainted(false); // 去掉点击时的虚线框
        btnOk.setPreferredSize(new Dimension(220, 65));
        btnOk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 点击关闭窗口
        btnOk.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnOk);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        // 显示窗口
        dialog.setVisible(true);
    }

    private void initData() {
        commList = SerialPortManager.findPort();
        if (commList != null && commList.size() > 0) {
            for (String s : commList) {
                commChoice.addItem(s);      // 给 WSN 用
                rfidCommChoice.addItem(s);  // 给 RFID 用
            }
        }
        baudrateChoice.addItem("9600");
        baudrateChoice.addItem("115200");

        // 如果你想默认选中某个非空的端口
        if (rfidCommChoice.getItemCount() > 0) {
            rfidCommChoice.setSelectedIndex(0);
        }
    }

    private void actionListener() {
        serialPortOperate.addActionListener(e -> {
            if ("打开串口".equals(serialPortOperate.getText()) && serialport == null) openSerialPort(e);
            else closeSerialPort(e);
        });
        sendData.addActionListener(this::sendData);
        btnRfidConnect.addActionListener(e -> {
            String selectedPort = (String) rfidCommChoice.getSelectedItem();

            if (selectedPort == null || selectedPort.isEmpty()) {
                logRfid("错误：未选择串口");
                return;
            }

            // 使用选中的端口进行初始化
            boolean ok = uhf.init(selectedPort);
            uhfReady = ok;
            logRfid(ok ? "UHF Reader 连接成功 (" + selectedPort + ")" : "UHF Reader 连接失败");
        });

        // 功能1：入场成功后智能推荐
        btnEntry.addActionListener(e -> {
            if (!uhfReady) {
                logRfid("错误：请先连接 RFID");
                return;
            }

            new javax.swing.SwingWorker<ReadResult, Void>() {
                @Override
                protected ReadResult doInBackground() {
                    // 读卡
                    return uhf.readEpcAndData(new byte[]{0, 0, 0, 0}, DATA_AREA, DATA_START, DATA_WORDS, 3000, 2500);
                }

                @Override
                protected void done() {
                    try {
                        ReadResult rr = get();
                        if (rr == null || !rr.ok() || rr.epcHex.isEmpty()) {
                            logRfid("入场：读卡失败");
                            return;
                        }

                        // 重复入场检测逻辑
                        // 1. 先解析卡片里现有的数据
                        byte[] currentData = UhfReaderService.hexToBytes(rr.dataHex);
                        ParkInfo info = unpackInfo(currentData);

                        // 2. 判断是否已在场内 (入场时间不为0 且 出场时间为0)
                        if (info.inMin != 0 && info.outMin == 0) {
                            logRfid("拦截重复入场：EPC=" + rr.epcHex + " 已在场内");

                            // 弹出“重复入场”警告窗口
                            JDialog dialog = new JDialog(MainFrame.this, "操作受限", true);
                            dialog.setLayout(new BorderLayout());
                            dialog.setSize(450, 600);
                            dialog.setLocationRelativeTo(MainFrame.this);

                            String errorHtml = "<html><body style='text-align:center; font-family:Microsoft YaHei;'>" +
                                    "<div style='font-size:60px; color:#c0392b; margin-top:20px;'>⛔</div>" +
                                    "<div style='font-size:26px; font-weight:bold; color:#333; margin-top:10px;'>禁止重复入场</div>" +
                                    "<div style='font-size:16px; color:#7f8c8d; margin-top:10px;'>该车辆已有入场记录<br>且尚未出场结算</div>" +
                                    "</body></html>";

                            JLabel errorLabel = new JLabel(errorHtml);
                            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                            dialog.add(errorLabel, BorderLayout.CENTER);

                            JPanel btnPanel = new JPanel();
                            btnPanel.setBackground(Color.WHITE);
                            btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

                            JButton btnClose = new JButton("关闭");
                            btnClose.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
                            btnClose.setBackground(new Color(52, 152, 219));
                            btnClose.setForeground(Color.WHITE);
                            btnClose.setFocusPainted(false);
                            btnClose.setPreferredSize(new Dimension(200, 60));
                            btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            btnClose.addActionListener(ev -> dialog.dispose());

                            btnPanel.add(btnClose);
                            dialog.add(btnPanel, BorderLayout.SOUTH);
                            dialog.setVisible(true);

                            return;
                        }

                        // 正常入场
                        int inMin = nowMin();
                        byte[] toWrite = packInfo(inMin, 0, 0); // 写入入场时间

                        // 写入数据
                        if (uhf.writeData(new byte[]{0, 0, 0, 0}, DATA_AREA, DATA_START, toWrite, 3000, 2500)) {
                            logRfid(">>> 入场成功：EPC=" + rr.epcHex);
                            rfidInCount++;

                            // 寻找最佳车位
                            int bestSlot = -1;
                            for (Slot s : slots) {
                                if (s.status == SlotStatus.FREE) {
                                    bestSlot = s.id;
                                    break;
                                }
                            }

                            if (bestSlot != -1) {
                                // 推荐车位弹窗 (保持原样)
                                JDialog dialog = new JDialog(MainFrame.this, "智能引导", true);
                                dialog.setLayout(new BorderLayout());
                                dialog.setSize(450, 600);
                                dialog.setLocationRelativeTo(MainFrame.this);

                                String tipHtml = "<html><body style='text-align:center; font-family:Microsoft YaHei;'>" +
                                        "<div style='font-size:20px; margin-top:20px;'>入场成功！</div>" +
                                        "<div style='font-size:18px; margin-top:10px;'>系统为您推荐最佳车位：</div>" +
                                        "<div style='font-size:40px; color:green; font-weight:bold; margin-top:20px;'>【 " + bestSlot + " 号 】</div>" +
                                        "</body></html>";
                                JLabel tipLabel = new JLabel(tipHtml);
                                tipLabel.setHorizontalAlignment(SwingConstants.CENTER);
                                dialog.add(tipLabel, BorderLayout.CENTER);

                                JPanel btnPanel = new JPanel();
                                btnPanel.setBackground(Color.WHITE);
                                btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

                                JButton btnOk = new JButton("确 定");
                                btnOk.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
                                btnOk.setBackground(new Color(52, 152, 219));
                                btnOk.setForeground(Color.WHITE);
                                btnOk.setFocusPainted(false);
                                btnOk.setPreferredSize(new Dimension(220, 65));
                                btnOk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                                btnOk.addActionListener(ev -> dialog.dispose());

                                btnPanel.add(btnOk);
                                dialog.add(btnPanel, BorderLayout.SOUTH);
                                dialog.setVisible(true);

                            } else {
                                // 车位已满弹窗 (保持原样)
                                JDialog dialog = new JDialog(MainFrame.this, "温馨提示", true);
                                dialog.setLayout(new BorderLayout());
                                dialog.setSize(450, 600);
                                dialog.setLocationRelativeTo(MainFrame.this);

                                String warningHtml = "<html><body style='text-align:center; font-family:Microsoft YaHei;'>" +
                                        "<div style='font-size:60px; color:#f39c12; margin-top:15px;'>⚠</div>" +
                                        "<div style='font-size:26px; font-weight:bold; color:#333; margin-top:10px;'>目前车位已满</div>" +
                                        "<div style='font-size:16px; color:#7f8c8d; margin-top:10px; margin-bottom:10px;'>系统虽已读卡，但无法分配车位<br>请稍候再试或联系管理员</div>" +
                                        "</body></html>";

                                JLabel warningLabel = new JLabel(warningHtml);
                                warningLabel.setHorizontalAlignment(SwingConstants.CENTER);
                                dialog.add(warningLabel, BorderLayout.CENTER);

                                JPanel btnPanel = new JPanel();
                                btnPanel.setBackground(Color.WHITE);
                                btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

                                JButton btnOk = new JButton("我知道了");
                                btnOk.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
                                btnOk.setBackground(new Color(52, 152, 219));
                                btnOk.setForeground(Color.WHITE);
                                btnOk.setFocusPainted(false);
                                btnOk.setPreferredSize(new Dimension(200, 60));
                                btnOk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                                btnOk.addActionListener(ev -> dialog.dispose());

                                btnPanel.add(btnOk);
                                dialog.add(btnPanel, BorderLayout.SOUTH);
                                dialog.setVisible(true);
                            }

                        } else {
                            logRfid("入场错误：写入失败");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.execute();
        });

        btnExit.addActionListener(e -> {
            if (!uhfReady) {
                logRfid("错误：请先连接 RFID");
                return;
            }

            new javax.swing.SwingWorker<ReadResult, Void>() {
                @Override
                protected ReadResult doInBackground() {
                    // 尝试读取标签数据
                    return uhf.readEpcAndData(new byte[]{0, 0, 0, 0}, DATA_AREA, DATA_START, DATA_WORDS, 3000, 2500);
                }

                @Override
                protected void done() {
                    try {
                        ReadResult rr = get();
                        if (rr == null || !rr.ok()) {
                            logRfid("出场：读卡失败");
                            return;
                        }

                        // 有些SDK返回的epcHex可能是null或者空字符串，这里做一个安全处理
                        String currentEpc = (rr.epcHex != null && !rr.epcHex.isEmpty()) ? rr.epcHex : "未知EPC";

                        // 解析数据
                        ParkInfo info = unpackInfo(UhfReaderService.hexToBytes(rr.dataHex));
                        if (info.inMin == 0) {
                            logRfid("出场无效：无入场记录 (EPC=" + currentEpc + ")");
                            return;
                        }
                        if (info.outMin != 0) {
                            logRfid("出场无效：该卡已出场 (EPC=" + currentEpc + ")");
                            return;
                        }

                        int outMin = nowMin();
                        int totalMinutes = Math.max(1, outMin - info.inMin);
                        int fee = feeCentsByMinutes(totalMinutes); // 计算费用（分）
                        double feeYuan = fee / 100.0; // 转换为元

                        // 计算人性化的时长显示 (例如 125分钟 -> 2小时 5分钟)
                        int hours = totalMinutes / 60;
                        int mins = totalMinutes % 60;
                        String durationStr = (hours > 0 ? hours + "小时 " : "") + mins + "分钟";

                        // 写入出场信息 (写入出场时间和费用)
                        byte[] toWrite = packInfo(info.inMin, outMin, fee);

                        if (uhf.writeData(new byte[]{0, 0, 0, 0}, DATA_AREA, DATA_START, toWrite, 3000, 2500)) {

                            // 日志输出EPC
                            logRfid(String.format("<<< 出场成功：EPC=%s, 时长=%s, 费用=%.2f元", currentEpc, durationStr, feeYuan));
                            if (rfidInCount > 0) rfidInCount--;

                            JDialog dialog = new JDialog(MainFrame.this, "出场收费", true);
                            dialog.setLayout(new BorderLayout());
                            dialog.setSize(450, 600); // 稍微高一点，因为内容多
                            dialog.setLocationRelativeTo(MainFrame.this);

                            // 1. 中间信息区
                            String infoHtml = "<html><body style='text-align:center; font-family:Microsoft YaHei;'>" +
                                    "<div style='font-size:20px; margin-top:20px;'>识别成功，允许出场</div>" +
                                    "<div style='font-size:16px; color:#555; margin-top:15px;'>停车时长</div>" +
                                    "<div style='font-size:24px; font-weight:bold; margin-bottom:10px;'>" + durationStr + "</div>" +
                                    "<hr style='width:80%;'>" +
                                    "<div style='font-size:16px; color:#555; margin-top:10px;'>应付金额</div>" +
                                    // 金额用红色大字强调
                                    "<div style='font-size:48px; color:#e74c3c; font-weight:bold;'>¥ " + String.format("%.2f", feeYuan) + "</div>" +
                                    "</body></html>";

                            JLabel infoLabel = new JLabel(infoHtml);
                            infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
                            dialog.add(infoLabel, BorderLayout.CENTER);

                            // 2. 底部按钮区
                            JPanel btnPanel = new JPanel();
                            btnPanel.setBackground(Color.WHITE);
                            btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));


                            JButton btnPay = new JButton("支付并抬杆");
                            btnPay.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
                            btnPay.setBackground(new Color(52, 152, 219));
                            btnPay.setForeground(Color.WHITE);
                            btnPay.setFocusPainted(false);
                            btnPay.setPreferredSize(new Dimension(220, 65));
                            btnPay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                            btnPay.addActionListener(ev -> {
                                // 1. 关闭收费弹窗
                                dialog.dispose();
                                logRfid("支付完成，闸机已开启。");

                                // 2. 显示一个“正在思考中”的过渡窗口
                                JDialog loadingDialog = new JDialog(MainFrame.this, "AI 助理", true);
                                loadingDialog.setUndecorated(true); // 无边框
                                loadingDialog.setSize(300, 100);
                                loadingDialog.setLocationRelativeTo(MainFrame.this);
                                JPanel loadPanel = new JPanel(new BorderLayout());
                                loadPanel.setBorder(BorderFactory.createLineBorder(new Color(52, 152, 219), 2));
                                loadPanel.setBackground(Color.WHITE);
                                JLabel loadLabel = new JLabel("正在分析天气与路况...", SwingConstants.CENTER);
                                loadLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16));
                                loadPanel.add(loadLabel, BorderLayout.CENTER);
                                loadingDialog.add(loadPanel);

                                // 3. 在后台线程请求 API (防止界面卡死)
                                new Thread(() -> {
                                    // A. 获取高德天气
                                    String weatherUrl = "https://restapi.amap.com/v3/weather/weatherInfo?city=" + CITY_ADCODE + "&key=" + AMAP_KEY;
                                    String weatherJson = httpGet(weatherUrl);
                                    String weather = extractJsonValue(weatherJson, "weather");
                                    String temp = extractJsonValue(weatherJson, "temperature");
                                    String city = extractJsonValue(weatherJson, "city");

                                    // B. 调用 DeepSeek 获取建议
                                    String prompt = String.format(
                                            "角色设定：你是一位猫娘。" +
                                                    "情境：用户正准备驾车离开停车场。" +
                                                    "数据：城市【%s】，天气【%s】，气温【%s度】。" +
                                                    "任务：输出现在的天气温度。作为智能行车助手，首先根据天气判断路况风险（如湿滑、视线差、防晒等）给出一条行车安全提示，然后根据天气用一句话问候用户。" +
                                                    "要求：字数控制在50字以内，不要啰嗦，说话多用“喵”~。",
                                            city, weather, temp
                                    );
                                    String deepseekJson = httpPostDeepSeek(prompt);
                                    // 提取 DeepSeek 返回的 content (注意：DeepSeek返回结构较深，这里用简易正则尝试提取content)
                                    // 完整结构是 choices[0].message.content，简单正则可能需要多匹配一次
                                    String aiAdvice = "一路顺风！愿您拥有美好的一天。"; // 默认兜底文案
                                    if (!deepseekJson.isEmpty()) {
                                        int contentIndex = deepseekJson.indexOf("\"content\":");
                                        if (contentIndex != -1) {
                                            int start = deepseekJson.indexOf("\"", contentIndex + 10) + 1;
                                            int end = deepseekJson.indexOf("\"", start);
                                            // 处理转义字符
                                            aiAdvice = deepseekJson.substring(start, end).replace("\\n", "<br>").replace("\\", "");
                                        }
                                    }

                                    // C. 关闭加载条，显示最终结果窗口
                                    String finalWeather = weather;
                                    String finalTemp = temp;
                                    String finalAdvice = aiAdvice;

                                    SwingUtilities.invokeLater(() -> {
                                        loadingDialog.dispose();
                                        showSmartTipDialog(finalWeather, finalTemp, finalAdvice);
                                    });
                                }).start();

                                loadingDialog.setVisible(true); // 显示加载框
                            });

                            btnPanel.add(btnPay);
                            dialog.add(btnPanel, BorderLayout.SOUTH);
                            dialog.setVisible(true);

                            btnPanel.add(btnPay);
                            dialog.add(btnPanel, BorderLayout.SOUTH);

                            dialog.setVisible(true);

                        } else {
                            logRfid("出场错误：写入失败 (EPC=" + currentEpc + ")");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.execute();
        });
    }

    private void openSerialPort(java.awt.event.ActionEvent evt) {
        String commName = (String) commChoice.getSelectedItem();
        int baudrate = Integer.parseInt((String) baudrateChoice.getSelectedItem());
        try {
            serialport = SerialPortManager.openPort(commName, baudrate);
            if (serialport != null) {
                System.out.println("串口已打开");
                serialPortOperate.setText("关闭串口");
                SerialPortManager.addListener(serialport, new SerialListener());
            }
        } catch (Exception e) {
            ShowUtils.warningMessage("打开失败");
        }
    }

    private void closeSerialPort(java.awt.event.ActionEvent evt) {
        SerialPortManager.closePort(serialport);
        serialPortOperate.setText("打开串口");
        serialport = null;
    }

    private void sendData(java.awt.event.ActionEvent evt) {
        String data = "FFFF" + dataInput.getText().toString() + "FEFE";
        System.out.println(data);
        try {
            SerialPortManager.sendToPort(serialport, ByteUtils.hexStr2Byte(data));
        } catch (Exception e) {
        }
    }

    private void updateSlotByMac(String macAddr, double luxVal) {
        String mac = normalizeAddr(macAddr);
        Integer idx = macToSlot.get(mac);
        if (idx != null) {
            Slot s = slots[idx];
            s.lastLux = luxVal;
            s.lastSeenMs = System.currentTimeMillis();
            s.status = (luxVal < LUX_THRESHOLD) ? SlotStatus.OCCUPIED : SlotStatus.FREE;
        }
    }

    private void refreshSlotPanelText() {
        long now = System.currentTimeMillis();
        int occ = 0, free = 0, off = 0;

        // 只需要统计数量和更新图形化界面
        for (int i = 0; i < slots.length; i++) {
            Slot s = slots[i];
            if (s.lastSeenMs == 0 || (now - s.lastSeenMs) > OFFLINE_MS) {
                s.status = SlotStatus.OFFLINE;
            }
            switch (s.status) {
                case OCCUPIED:
                    occ++;
                    break;
                case FREE:
                    free++;
                    break;
                case OFFLINE:
                    off++;
                    break;
            }

            // 更新左侧的可视化车位
            if (parkingUI != null) {
                int block = (i < 10) ? 0 : 1;
                int localIndex = i % 10;
                int uiStatus;
                if (s.status == SlotStatus.FREE) uiStatus = ParkingLotPanel.STATUS_FREE;
                else if (s.status == SlotStatus.OCCUPIED) uiStatus = ParkingLotPanel.STATUS_OCCUPIED;
                else uiStatus = ParkingLotPanel.STATUS_OFFLINE;
                parkingUI.updateSpotStatus(block, localIndex / 2, localIndex % 2, uiStatus);
            }
        }

        // 核心逻辑：防逃票报警
        // 逻辑：如果物理占用的车位(occ) 大于 系统记录的入场数(rfidInCount)，说明有人没刷卡就停进去了
        if (occ > rfidInCount) {
            int diff = occ - rfidInCount;
            alarmLabel.setText("警告：监测到 " + diff + " 辆非法/逃票车辆！");
            alarmLabel.setForeground(Color.RED);
        } else {
            alarmLabel.setText("系统状态：正常 (入场:" + rfidInCount + " 占用:" + occ + ")");
            alarmLabel.setForeground(new Color(46, 204, 113)); // 绿色
        }

    }

    private void logRfid(String msg) {
        SwingUtilities.invokeLater(() -> {
            rfidView.append(String.format("[%tT] %s\n", System.currentTimeMillis(), msg));
            rfidView.setCaretPosition(rfidView.getDocument().getLength());
        });
    }

    /**
     * 简单的 HTTP GET 请求 (用于高德天气)
     */
    private String httpGet(String urlStr) {
        try {
            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            if (conn.getResponseCode() == 200) {
                try (java.util.Scanner s = new java.util.Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A")) {
                    return s.hasNext() ? s.next() : "";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 简单的 HTTP POST 请求 (用于 DeepSeek)
     */
    private String httpPostDeepSeek(String prompt) {
        try {
            java.net.URL url = new java.net.URL("https://api.deepseek.com/chat/completions");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + DEEPSEEK_API_KEY);
            conn.setDoOutput(true);

            // 构建 JSON 字符串 (手动拼接，避免依赖库)
            String jsonBody = "{"
                    + "\"model\": \"deepseek-chat\","
                    + "\"messages\": ["
                    + "  {\"role\": \"user\", \"content\": \"" + prompt + "\"}"
                    + "]"
                    + "}";

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() == 200) {
                try (java.util.Scanner s = new java.util.Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A")) {
                    return s.hasNext() ? s.next() : "";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 简单的正则提取 JSON 值
     */
    private String extractJsonValue(String json, String key) {
        if (json == null) return "";
        try {
            // 匹配 "key":"value" 或 "key":value
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + key + "\":\\s*\"?([^,\"}]+)\"?");
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) return m.group(1);
        } catch (Exception e) {
        }
        return "未知";
    }

    /**
     * 显示 AI 生成的出行建议窗口
     */
    private void showSmartTipDialog(String weather, String temp, String advice) {
        JDialog tipDialog = new JDialog(MainFrame.this, "出行向导", true);
        tipDialog.setSize(450, 600);
        tipDialog.setLocationRelativeTo(MainFrame.this);
        tipDialog.setLayout(new BorderLayout());
        tipDialog.getContentPane().setBackground(Color.WHITE);

        // 1. 顶部天气栏
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        topPanel.setBackground(new Color(240, 248, 255)); // 淡蓝色背景
        JLabel weatherLabel = new JLabel("<html><span style='font-size:18px;'>🌡️ " + temp + "°C</span> &nbsp;&nbsp; <span style='font-size:16px;'>" + weather + "</span></html>");
        weatherLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        weatherLabel.setForeground(new Color(41, 128, 185));
        topPanel.add(weatherLabel);
        tipDialog.add(topPanel, BorderLayout.NORTH);

        // 2. 中间建议区
        String htmlContent = "<html><body style='width: 320px; font-family: Microsoft YaHei; padding: 10px;'>" +
                "<div style='color: #555; font-size: 14px; margin-bottom: 10px;'>根据当前天气，AI 为您推荐：</div>" +
                "<div style='font-size: 18px; font-weight: bold; color: #333; line-height: 1.5;'>" +
                advice + // DeepSeek 生成的内容
                "</div>" +
                "<br><br>" +
                "<div style='font-size: 12px; color: #999; text-align: right;'>—— 祝您旅途愉快</div>" +
                "</body></html>";

        JLabel contentLabel = new JLabel(htmlContent);
        contentLabel.setHorizontalAlignment(SwingConstants.CENTER);
        tipDialog.add(contentLabel, BorderLayout.CENTER);

        // 3. 底部按钮
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JButton btnThanks = new JButton("收下建议");
        btnThanks.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        btnThanks.setBackground(new Color(46, 204, 113));
        btnThanks.setForeground(Color.WHITE);
        btnThanks.setFocusPainted(false);
        btnThanks.setPreferredSize(new Dimension(150, 45));
        btnThanks.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnThanks.addActionListener(e -> tipDialog.dispose());

        btnPanel.add(btnThanks);
        tipDialog.add(btnPanel, BorderLayout.SOUTH);

        tipDialog.setVisible(true);
    }

    private enum SlotStatus {FREE, OCCUPIED, OFFLINE}

    private static class Slot {
        final int id;
        final String mac;
        double lastLux = Double.NaN;
        long lastSeenMs = 0;
        SlotStatus status = SlotStatus.OFFLINE;

        Slot(int id, String mac) {
            this.id = id;
            this.mac = mac;
        }
    }

    private static class ParkInfo {
        int magic;
        int inMin;
        int outMin;
        int feeCents;
    }

    private static class WsnFrame {
        String shortAddr;
        String mac;
        int lux;
    }

    private class SerialListener implements SerialPortEventListener {
        public void serialEvent(SerialPortEvent serialPortEvent) {
            if (serialPortEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                try {
                    byte[] data = SerialPortManager.readFromPort(serialport);
                    System.out.println(ByteUtils.byteArrayToHexString(data,
                            true));
                    WsnFrame f = parseWsnFrame(data);
                    if (f != null) {
                        updateSlotByMac(f.mac, f.lux);
                        SwingUtilities.invokeLater(() -> refreshSlotPanelText());
                        String cmd = (f.lux < LUX_THRESHOLD) ? ("FFFF" + "B626" + "0010FEFE") : ("FFFF" + "B626" + "0001FEFE");
                        try {
                            SerialPortManager.sendToPort(serialport, ByteUtils.hexStr2Byte(cmd));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
    }
}

