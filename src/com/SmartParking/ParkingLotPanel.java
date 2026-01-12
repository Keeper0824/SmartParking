package com.SmartParking;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ParkingLotPanel extends JPanel {
    private JPanel[][][] parkingSpots = new JPanel[2][5][2];
    private SpotClickListener listener;

    // ================= 1. 配色配置 (修改这里) =================
    // 空闲：绿色
    private static final Color COLOR_SPOT_FREE = new Color(46, 204, 113);
    // 占用：黄色 (选用了橙黄色，保证白字能看清，纯黄太刺眼)
    private static final Color COLOR_SPOT_OCCUPIED = new Color(243, 156, 18);
    // 离线：灰色
    private static final Color COLOR_SPOT_OFFLINE = new Color(149, 165, 166);

    private static final Color COLOR_SPOT_BORDER = Color.GRAY;
    private static final Color COLOR_ROAD = new Color(127, 140, 141);
    private static final Color COLOR_FRAME_BORDER = new Color(52, 152, 219);
    private static final Color COLOR_FRAME_BG = Color.WHITE;
    private static final Color COLOR_BACKGROUND = Color.WHITE;
    private static final Color COLOR_TEXT_WHITE = Color.WHITE;

    // ================= 尺寸配置 =================
    private static final int FRAME_WIDTH = 1140;
    private static final int FRAME_HEIGHT = 650;
    private static final int WELCOME_TITLE_HEIGHT = 50;

    // 状态常量
    public static final int STATUS_FREE = 0;
    public static final int STATUS_OCCUPIED = 1;
    public static final int STATUS_OFFLINE = 2;

    public interface SpotClickListener {
        void onSpotClick(int block, int row, int col);
    }

    public void setSpotClickListener(SpotClickListener listener) {
        this.listener = listener;
    }

    public ParkingLotPanel() {
        setLayout(new BorderLayout());
        setBackground(COLOR_BACKGROUND);
        setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT + WELCOME_TITLE_HEIGHT + 10));

        // 标题
        JPanel welcomePanel = new JPanel();
        welcomePanel.setBackground(COLOR_BACKGROUND);
        welcomePanel.setPreferredSize(new Dimension(FRAME_WIDTH, WELCOME_TITLE_HEIGHT));
        JLabel welcomeLabel = new JLabel("智慧停车场监控大屏");
        // 字体加大
        welcomeLabel.setFont(new Font("微软雅黑", Font.BOLD, 36));
        welcomeLabel.setForeground(Color.DARK_GRAY);
        welcomePanel.add(welcomeLabel);
        add(welcomePanel, BorderLayout.NORTH);

        // 中间区域
        JPanel centerContainer = new JPanel();
        centerContainer.setBackground(COLOR_BACKGROUND);
        centerContainer.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        add(centerContainer, BorderLayout.CENTER);

        JPanel framePanel = new JPanel();
        framePanel.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        framePanel.setBorder(BorderFactory.createLineBorder(COLOR_FRAME_BORDER, 1));
        framePanel.setBackground(COLOR_FRAME_BG);
        framePanel.setLayout(null);
        centerContainer.add(framePanel);

        drawFullFrameContent(framePanel);
    }

    /**
     * 更新状态的核心方法
     * @param statusType 0=Free, 1=Occupied, 2=Offline
     */
    public void updateSpotStatus(int block, int row, int col, int statusType) {
        if (block < 2 && row < 5 && col < 2) {
            SwingUtilities.invokeLater(() -> {
                if(parkingSpots[block][row][col] instanceof ParkingSpotPanel) {
                    ((ParkingSpotPanel) parkingSpots[block][row][col]).setStatus(statusType);
                }
            });
        }
    }

    private void drawFullFrameContent(JPanel framePanel) {
        int mainRoadWidth = 40;
        int centerRoadWidth = 40;
        int availableW = FRAME_WIDTH - (mainRoadWidth * 2) - centerRoadWidth - 80;
        int spotWidth = availableW / 4;
        int spotHeight = spotWidth * 2 / 5;
        int spotGapH = 10;
        int spotGapV = 10;

        drawRoad(framePanel, 0, 0, FRAME_WIDTH, mainRoadWidth);
        drawRoad(framePanel, 0, FRAME_HEIGHT - mainRoadWidth, FRAME_WIDTH, mainRoadWidth);
        drawRoad(framePanel, 0, 0, mainRoadWidth, FRAME_HEIGHT);
        drawRoad(framePanel, FRAME_WIDTH - mainRoadWidth, 0, mainRoadWidth, FRAME_HEIGHT);
        drawRoad(framePanel, FRAME_WIDTH/2 - centerRoadWidth/2, 0, centerRoadWidth, FRAME_HEIGHT);

        JLabel entryLabel = new JLabel("入口 IN ->");
        entryLabel.setForeground(Color.WHITE);
        entryLabel.setFont(new Font("Arial", Font.BOLD, 18)); // 字体加大
        entryLabel.setBounds(10, 10, 100, 20);
        framePanel.add(entryLabel);
        framePanel.setComponentZOrder(entryLabel, 0);

        int leftBlockX = mainRoadWidth + 20;
        int startY = mainRoadWidth + 20;
        drawBlock(framePanel, leftBlockX, startY, spotWidth, spotHeight, spotGapH, spotGapV, 0);

        int rightBlockX = (FRAME_WIDTH/2 + centerRoadWidth/2) + 20;
        drawBlock(framePanel, rightBlockX, startY, spotWidth, spotHeight, spotGapH, spotGapV, 1);
    }

    private void drawRoad(JPanel parent, int x, int y, int w, int h) {
        JPanel road = new JPanel();
        road.setBackground(COLOR_ROAD);
        road.setBounds(x, y, w, h);
        parent.add(road);
    }

    private void drawBlock(JPanel parent, int startX, int startY, int spotW, int spotH, int gapH, int gapV, int blockIndex) {
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 2; col++) {
                int spotX = startX + col * (spotW + gapH);
                int spotY = startY + row * (spotH + gapV);
                int spotNum = blockIndex * 10 + row * 2 + col + 1;

                ParkingSpotPanel spot = new ParkingSpotPanel("车位" + spotNum, spotW, spotH, blockIndex, row, col);
                spot.setBounds(spotX, spotY, spotW, spotH);
                parkingSpots[blockIndex][row][col] = spot;
                parent.add(spot);
            }
        }
    }

    // 内部类：单个车位面板
    private class ParkingSpotPanel extends JPanel {
        private String spotName;
        private JLabel statusLabel;

        public ParkingSpotPanel(String name, int w, int h, int b, int r, int c) {
            this.spotName = name;
            setLayout(new BorderLayout());
            setBackground(COLOR_SPOT_OFFLINE); // 默认离线灰色
            setBorder(BorderFactory.createLineBorder(COLOR_SPOT_BORDER, 2));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel nameLabel = new JLabel(spotName);
            nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
            nameLabel.setForeground(COLOR_TEXT_WHITE);
            // ================= 2. 字体大小固定 (修改这里) =================
            nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 22));
            add(nameLabel, BorderLayout.CENTER);

            statusLabel = new JLabel("离线");
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            statusLabel.setForeground(COLOR_TEXT_WHITE);
            // ================= 2. 字体大小固定 (修改这里) =================
            statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
            add(statusLabel, BorderLayout.SOUTH);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (listener != null) {
                        listener.onSpotClick(b, r, c);
                    }
                }
            });
        }

        // ================= 3. 支持三种状态的逻辑 (修改这里) =================
        public void setStatus(int statusType) {
            switch (statusType) {
                case STATUS_FREE:
                    setBackground(COLOR_SPOT_FREE); // 绿色
                    statusLabel.setText("空闲");
                    break;
                case STATUS_OCCUPIED:
                    setBackground(COLOR_SPOT_OCCUPIED); // 黄色
                    statusLabel.setText("占用");
                    break;
                case STATUS_OFFLINE:
                default:
                    setBackground(COLOR_SPOT_OFFLINE); // 灰色
                    statusLabel.setText("离线");
                    break;
            }
            repaint();
        }
    }
}