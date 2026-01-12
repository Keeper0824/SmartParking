package com.SerialPort.ui;

import com.SerialPort.exception.*;
import com.SerialPort.utils.ByteUtils;
import com.SerialPort.utils.ShowUtils;

import com.SerialPort.manage.SerialPortManager;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

/**
 * 主界面 The main interface
 */
public class MainFrame extends JFrame {

    public static final int WIDTH = 500;
    /**
     * 程序界面高度
     */
    public static final int HEIGHT = 450;
    private JTextArea dataView = new JTextArea();
    private JScrollPane scrollDataView = new JScrollPane(dataView);
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
    private JButton serialPortOperate1 = new JButton("绿亮红灭");
    private JButton serialPortOperate2 = new JButton("红亮绿灭");
    private JButton serialPortOperate3 = new JButton("打开两灯");
    private JButton serialPortOperate4 = new JButton("关灯");
    private JButton serialPortOperate5 = new JButton("频闪");
    private JButton sendData = new JButton("发送数据");
    private List<String> commList = null;
    private SerialPort serialport;


    public MainFrame() {

        initView();
        initComponents();
        actionListener();
        initData();
    }

    /**
     * main
     * 主函数 main
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }

    //initView()
    private void initView() {
        // 关闭程序
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        // 禁止窗口最大化
        setResizable(false);
        // 设置程序窗口居中显示
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getCenterPoint();
        setBounds(p.x - WIDTH / 2, p.y - HEIGHT / 2, WIDTH, HEIGHT);
        this.setLayout(null);
        setTitle("串口通讯");
    }

    private void initComponents() {
        // 数据显示
        dataView.setFocusable(false);
        scrollDataView.setBounds(10, 10, 475, 200);
        add(scrollDataView);
        // 串口设置
        serialPortPanel.setBorder(BorderFactory.createTitledBorder("串口设置"));
        serialPortPanel.setBounds(10, 220, 170, 100);
        serialPortPanel.setLayout(null);
        add(serialPortPanel);
        serialPortLabel.setForeground(Color.gray);
        serialPortLabel.setBounds(10, 25, 40, 20);
        serialPortPanel.add(serialPortLabel);
        commChoice.setFocusable(false);
        commChoice.setBounds(60, 25, 100, 20);
        serialPortPanel.add(commChoice);
        baudrateLabel.setForeground(Color.gray);
        baudrateLabel.setBounds(10, 60, 40, 20);
        serialPortPanel.add(baudrateLabel);
        baudrateChoice.setFocusable(false);
        baudrateChoice.setBounds(60, 60, 100, 20);
        serialPortPanel.add(baudrateChoice);
        // 操作
        operatePanel.setBorder(BorderFactory.createTitledBorder("操作"));
        operatePanel.setBounds(200, 220, 285, 90);
        operatePanel.setLayout(null);
        add(operatePanel);
        dataInput.setBounds(25, 25, 235, 25);
        operatePanel.add(dataInput);
        serialPortOperate.setFocusable(false);
        serialPortOperate.setBounds(45, 60, 90, 25);
        operatePanel.add(serialPortOperate);

        serialPortOperate1.setFocusable(false);
        serialPortOperate1.setBounds(45, 90, 90, 25);
        operatePanel.add(serialPortOperate1);
        serialPortOperate2.setFocusable(false);
        serialPortOperate2.setBounds(155, 90, 90, 25);
        operatePanel.add(serialPortOperate2);
        serialPortOperate3.setFocusable(false);
        serialPortOperate3.setBounds(155, 120, 90, 25);
        operatePanel.add(serialPortOperate3);
        serialPortOperate4.setFocusable(false);
        serialPortOperate4.setBounds(45, 120, 90, 25);
        operatePanel.add(serialPortOperate4);
        serialPortOperate5.setFocusable(false);
        serialPortOperate5.setBounds(45, 150, 90, 25);
        operatePanel.add(serialPortOperate5);
        sendData.setFocusable(false);
        sendData.setBounds(155, 60, 90, 25);
        operatePanel.add(sendData);
    }

    //initComponents()
    private void initData() {
        commList = SerialPortManager.findPort();
        // 检查是否有可用串口，有则加入选项中
        if (commList == null || commList.size() < 1) {
            ShowUtils.warningMessage("没有搜索到有效串口！");
        } else {
            for (String s : commList) {
                commChoice.addItem(s);
            }
        }
        baudrateChoice.addItem("9600");
        baudrateChoice.addItem("19200");
        baudrateChoice.addItem("38400");
        baudrateChoice.addItem("57600");
        baudrateChoice.addItem("115200");
    }

    //actionListener()
    private void actionListener() {
        serialPortOperate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ("打开串口".equals(serialPortOperate.getText())
                        && serialport == null) {
                    openSerialPort(e);
                } else {
                    closeSerialPort(e);
                }
            }
        });
        sendData.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendData(e);
            }
        });
        serialPortOperate1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openGreen(e);
            }
        });
        serialPortOperate2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openRed(e);
            }
        });
        serialPortOperate3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openAll(e);
            }
        });
        serialPortOperate4.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeAll(e);
            }
        });

        serialPortOperate5.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Flash(e);
            }
        });
    }

    //initData()
    private void openSerialPort(java.awt.event.ActionEvent evt) {
        // 获取串口名称
        String commName = (String) commChoice.getSelectedItem();
        // 获取波特率
        int baudrate = 9600;
        String bps = (String) baudrateChoice.getSelectedItem();
        baudrate = Integer.parseInt(bps);
        // 检查串口名称是否获取正确
        if (commName == null || commName.equals("")) {
            ShowUtils.warningMessage("没有搜索到有效串口！");
        } else {
            try {
                serialport = SerialPortManager.openPort(commName, baudrate);
                if (serialport != null) {
                    dataView.setText("串口已打开" + "\r\n");
                    serialPortOperate.setText("关闭串口");
                }
            } catch (SerialPortParameterFailure e) {
                e.printStackTrace();
            } catch (NotASerialPort e) {
                e.printStackTrace();
            } catch (NoSuchPort e) {
                e.printStackTrace();
            } catch (PortInUse e) {
                e.printStackTrace();
                ShowUtils.warningMessage("串口已被占用！");
            }
        }
        try {
            SerialPortManager.addListener(serialport, new SerialListener());
        } catch (TooManyListeners e) {
            e.printStackTrace();
        }
    }

    /**
     * openSerialPort
     * 打开串口  open the serial port
     *
     * @param evt 点击事件 Click event
     */


    private void closeSerialPort(java.awt.event.ActionEvent evt) {
        SerialPortManager.closePort(serialport);
        dataView.setText("串口已关闭" + "\r\n");
        serialPortOperate.setText("打开串口");
        serialport = null;
    }

    /**
     * closeSerialPort
     * 关闭串口 close the serial port
     *
     * @param evt 点击事件 Click event
     */

    private void sendData(java.awt.event.ActionEvent evt) {
        String data = dataInput.getText().toString();
        data = "FFFF" + data + "FEFE"; // 添加一个无需再额外加表头表尾处理的
        try {
            SerialPortManager.sendToPort(serialport,
                    ByteUtils.hexStr2Byte(data));
        } catch (SendDataToSerialPortFailure e) {
            e.printStackTrace();
        } catch (SerialPortOutputStreamCloseFailure e) {
            e.printStackTrace();
        }
    }

    private void openGreen(java.awt.event.ActionEvent evt) {
        String data = "FFFFB6230001FEFE";
        try {
            SerialPortManager.sendToPort(serialport,
                    ByteUtils.hexStr2Byte(data));
        } catch (SendDataToSerialPortFailure e) {
            e.printStackTrace();
        } catch (SerialPortOutputStreamCloseFailure e) {
            e.printStackTrace();
        }
        dataView.setText("绿灯已打开" + "\r\n");
    }

    private void openRed(java.awt.event.ActionEvent evt) {
        String data = "FFFFB6230010FEFE";
        try {
            SerialPortManager.sendToPort(serialport,
                    ByteUtils.hexStr2Byte(data));
        } catch (SendDataToSerialPortFailure e) {
            e.printStackTrace();
        } catch (SerialPortOutputStreamCloseFailure e) {
            e.printStackTrace();
        }
        dataView.setText("红灯已打开" + "\r\n");
    }

    private void openAll(java.awt.event.ActionEvent evt) {
        String data = "FFFFB6230011FEFE";
        try {
            SerialPortManager.sendToPort(serialport,
                    ByteUtils.hexStr2Byte(data));
        } catch (SendDataToSerialPortFailure e) {
            e.printStackTrace();
        } catch (SerialPortOutputStreamCloseFailure e) {
            e.printStackTrace();
        }
        dataView.setText("红灯和绿灯都已打开" + "\r\n");
    }

    private void closeAll(java.awt.event.ActionEvent evt) {
        String data = "FFFFB6230000FEFE";
        try {
            SerialPortManager.sendToPort(serialport,
                    ByteUtils.hexStr2Byte(data));
        } catch (SendDataToSerialPortFailure e) {
            e.printStackTrace();
        } catch (SerialPortOutputStreamCloseFailure e) {
            e.printStackTrace();
        }
        dataView.setText("红灯和绿灯都已关闭" + "\r\n");
    }

    private void Flash(java.awt.event.ActionEvent evt) {
        String data;

        for (int i = 0; i < 5; i++) {
            data = "FFFFB6230001FEFE";

            try {
                SerialPortManager.sendToPort(serialport,
                        ByteUtils.hexStr2Byte(data));
            } catch (SendDataToSerialPortFailure e) {
                e.printStackTrace();
            } catch (SerialPortOutputStreamCloseFailure e) {
                e.printStackTrace();
            }

            dataView.setText("绿灯" + "\r\n");

            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            data = "FFFFB6230010FEFE";
            try {
                SerialPortManager.sendToPort(serialport,
                        ByteUtils.hexStr2Byte(data));
            } catch (SendDataToSerialPortFailure e) {
                e.printStackTrace();
            } catch (SerialPortOutputStreamCloseFailure e) {
                e.printStackTrace();
            }
            dataView.setText("红灯" + "\r\n");

            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * sendData
     * 发送数据 send data
     */

    private class SerialListener implements SerialPortEventListener {
        /**
         * 处理监控到的串口事件
         */
        public void serialEvent(SerialPortEvent serialPortEvent) {
            switch (serialPortEvent.getEventType()) {
                case SerialPortEvent.BI: // 10 通讯中断
                    ShowUtils.errorMessage("与串口设备通讯中断");
                    break;
                case SerialPortEvent.OE: // 7 溢位（溢出）错误
                case SerialPortEvent.FE: // 9 帧错误
                case SerialPortEvent.PE: // 8 奇偶校验错误
                case SerialPortEvent.CD: // 6 载波检测
                case SerialPortEvent.CTS: // 3 清除待发送数据
                case SerialPortEvent.DSR: // 4 待发送数据准备好了
                case SerialPortEvent.RI: // 5 振铃指示
                case SerialPortEvent.OUTPUT_BUFFER_EMPTY: // 2 输出缓冲区已清空
                    break;
                case SerialPortEvent.DATA_AVAILABLE: // 1 串口存在可用数据
                    byte[] data = null;
                    try {
                        if (serialport == null) {
                            ShowUtils.errorMessage("串口对象为空！监听失败！");
                        } else {
                            // 读取串口数据
                            data = SerialPortManager.readFromPort(serialport);
                            dataView.append(ByteUtils.byteArrayToHexString(data,
                                    true) + "\r\n");
                            System.out.println(Arrays.toString(parseEnvData(data)));
                        }
                    } catch (Exception e) {
                        ShowUtils.errorMessage(e.toString());
                        // 发生读取错误时显示错误信息后退出系统
                        System.exit(0);
                    }
                    break;
            }
        }

        public double[] parseEnvData(byte[] data) {
            // 末尾定位：优先找最后一个 FE FE，温度/湿度/光照依次位于它前面的 6 个字节（T高T低 H高H低 L低L高）
            int end = -1;
            for (int i = 1; i < data.length; i++) {
                if ((data[i - 1] & 0xFF) == 0xFE && (data[i] & 0xFF) == 0xFE) end = i - 1;
            }
            if (end == -1) end = data.length;               // 没有 FEFE 就以数组末尾为准
            if (end < 6) throw new IllegalArgumentException("帧长度不足，无法解析");

            int tHigh = data[end - 6] & 0xFF, tLow = data[end - 5] & 0xFF;   // 温度 1D00
            int hHigh = data[end - 4] & 0xFF, hLow = data[end - 3] & 0xFF;   // 湿度 03B4
            int lLow  = data[end - 2] & 0xFF, lHigh= data[end - 1] & 0xFF;   // 光照 4205（低在前）

            // 温度：TMP175 12-bit 补码，分辨率 1/16℃
            int rawT = (tHigh << 4) | (tLow >> 4);
            double temperature;
            if ((tHigh & 0x80) != 0) {
                rawT = ((~rawT) & 0x7FF) + 1;
                temperature = -(rawT / 16.0);
            } else {
                temperature = rawT / 16.0;
            }

            // 湿度：12-bit ADC -> 电压 -> 三段线性（参考 3.3V）
            int rawH = ((hHigh << 8) | hLow) & 0x0FFF;
            double volt = (rawH / 4096.0) * 3.3;
            double denom = (temperature < 5) ? 3.27 : (temperature > 50 ? 2.70 : 3.10);
            double humidity = ((volt - 0.8) * 100.0) / denom;
            if (humidity < 0) humidity = 0;
            if (humidity > 100) humidity = 100;

            // 光照：16-bit 计数（低在前）→ 0~16000 lux
            int rawL = (lHigh << 8) | lLow;
            double lux = (rawL / 65536.0) * 16000.0;

            return new double[]{temperature, humidity, lux};
        }
    }
}