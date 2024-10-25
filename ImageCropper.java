import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class ImageCropper extends JFrame {
    private BufferedImage originalImage;
    private BufferedImage croppedImage;
    private JPanel mainPanel;
    private JPanel previewPanel;
    private Point startPoint;
    private Rectangle cropRect;
    private boolean drawing = false;

    public ImageCropper() {
        setTitle("Image Cropper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initializeUI();
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void initializeUI() {
        // メインパネルの設定（左側3/4）
        mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (originalImage != null) {
                    // 画像をパネルサイズに合わせて表示
                    drawFitImage(g);
                    
                    // 選択範囲を描画
                    if (cropRect != null) {
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setColor(new Color(255, 255, 255, 100));
                        g2d.fill(cropRect);
                        g2d.setColor(Color.WHITE);
                        g2d.draw(cropRect);
                    }
                }
            }
        };

        // プレビューパネルの設定（右側1/4）
        previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (croppedImage != null) {
                    // クロップされた画像をプレビューパネルに表示
                    Graphics2D g2d = (Graphics2D) g;
                    int w = getWidth();
                    int h = getHeight();
                    double scale = Math.min((double) w / croppedImage.getWidth(),
                                         (double) h / croppedImage.getHeight());
                    int scaledW = (int) (croppedImage.getWidth() * scale);
                    int scaledH = (int) (croppedImage.getHeight() * scale);
                    int x = (w - scaledW) / 2;
                    int y = (h - scaledH) / 2;
                    g2d.drawImage(croppedImage, x, y, scaledW, scaledH, null);
                }
            }
        };

        // マウスリスナーの追加
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                cropRect = null;
                drawing = true;
                mainPanel.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (drawing) {
                    int x = Math.min(startPoint.x, e.getX());
                    int y = Math.min(startPoint.y, e.getY());
                    int width = Math.abs(e.getX() - startPoint.x);
                    int height = Math.abs(e.getY() - startPoint.y);
                    cropRect = new Rectangle(x, y, width, height);
                    mainPanel.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                drawing = false;
                if (cropRect != null && originalImage != null) {
                    // 選択範囲を元画像の座標系に変換
                    double scaleX = (double) originalImage.getWidth() / mainPanel.getWidth();
                    double scaleY = (double) originalImage.getHeight() / mainPanel.getHeight();
                    
                    int x = (int) (cropRect.x * scaleX);
                    int y = (int) (cropRect.y * scaleY);
                    int width = (int) (cropRect.width * scaleX);
                    int height = (int) (cropRect.height * scaleY);
                    
                    // 範囲チェック
                    x = Math.max(0, Math.min(x, originalImage.getWidth()));
                    y = Math.max(0, Math.min(y, originalImage.getHeight()));
                    width = Math.min(width, originalImage.getWidth() - x);
                    height = Math.min(height, originalImage.getHeight() - y);
                    
                    // 画像をクロップ
                    croppedImage = originalImage.getSubimage(x, y, width, height);
                    previewPanel.repaint();
                }
            }
        };

        mainPanel.addMouseListener(mouseAdapter);
        mainPanel.addMouseMotionListener(mouseAdapter);

        // メニューバーの作成
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save Cropped");

        openItem.addActionListener(e -> loadImage());
        saveItem.addActionListener(e -> saveCroppedImage());

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // レイアウトの設定
        setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainPanel, previewPanel);
        splitPane.setResizeWeight(0.75); // 左側のパネルが3/4の幅を占める
        add(splitPane, BorderLayout.CENTER);
    }

    private void drawFitImage(Graphics g) {
        if (originalImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            int panelWidth = mainPanel.getWidth();
            int panelHeight = mainPanel.getHeight();
            
            double scale = Math.min((double) panelWidth / originalImage.getWidth(),
                                 (double) panelHeight / originalImage.getHeight());
            
            int scaledWidth = (int) (originalImage.getWidth() * scale);
            int scaledHeight = (int) (originalImage.getHeight() * scale);
            
            int x = (panelWidth - scaledWidth) / 2;
            int y = (panelHeight - scaledHeight) / 2;
            
            g2d.drawImage(originalImage, x, y, scaledWidth, scaledHeight, null);
        }
    }

    private void loadImage() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                originalImage = ImageIO.read(file);
                cropRect = null;
                croppedImage = null;
                mainPanel.repaint();
                previewPanel.repaint();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveCroppedImage() {
        if (croppedImage != null) {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = fileChooser.getSelectedFile();
                    String name = file.getName();
                    String extension = "png";
                    if (name.lastIndexOf(".") != -1) {
                        extension = name.substring(name.lastIndexOf(".") + 1);
                    } else {
                        file = new File(file.getAbsolutePath() + ".png");
                    }
                    ImageIO.write(croppedImage, extension, file);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving image: " + ex.getMessage(),
                                              "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ImageCropper().setVisible(true);
        });
    }
}
