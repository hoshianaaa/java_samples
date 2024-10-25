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
    private Rectangle imageRect; // 実際の画像表示領域を保持

    public ImageCropper() {
        setTitle("Image Cropper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initializeUI();
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private void initializeUI() {
        mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (originalImage != null) {
                    // 画像を描画し、表示領域を保存
                    imageRect = drawFitImage(g);
                    
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

        previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (croppedImage != null) {
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

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 画像の表示領域内でのみ処理を行う
                if (imageRect != null && imageRect.contains(e.getPoint())) {
                    startPoint = e.getPoint();
                    cropRect = null;
                    drawing = true;
                    mainPanel.repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (drawing && imageRect != null) {
                    // マウス座標を画像表示領域内に制限
                    Point p = new Point(
                        Math.max(imageRect.x, Math.min(e.getX(), imageRect.x + imageRect.width)),
                        Math.max(imageRect.y, Math.min(e.getY(), imageRect.y + imageRect.height))
                    );
                    
                    int x = Math.min(startPoint.x, p.x);
                    int y = Math.min(startPoint.y, p.y);
                    int width = Math.abs(p.x - startPoint.x);
                    int height = Math.abs(p.y - startPoint.y);
                    
                    cropRect = new Rectangle(x, y, width, height);
                    mainPanel.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (drawing && cropRect != null && imageRect != null) {
                    // 画像表示領域内での相対座標に変換
                    double scaleX = (double) originalImage.getWidth() / imageRect.width;
                    double scaleY = (double) originalImage.getHeight() / imageRect.height;
                    
                    // 選択範囲の座標を画像表示領域からの相対位置に変換
                    int relativeX = cropRect.x - imageRect.x;
                    int relativeY = cropRect.y - imageRect.y;
                    
                    // 元画像での座標に変換
                    int x = (int) (relativeX * scaleX);
                    int y = (int) (relativeY * scaleY);
                    int width = (int) (cropRect.width * scaleX);
                    int height = (int) (cropRect.height * scaleY);
                    
                    // 範囲チェック
                    x = Math.max(0, Math.min(x, originalImage.getWidth() - 1));
                    y = Math.max(0, Math.min(y, originalImage.getHeight() - 1));
                    width = Math.min(width, originalImage.getWidth() - x);
                    height = Math.min(height, originalImage.getHeight() - y);
                    
                    // 最小サイズチェック
                    if (width > 0 && height > 0) {
                        croppedImage = originalImage.getSubimage(x, y, width, height);
                        previewPanel.repaint();
                    }
                }
                drawing = false;
            }
        };

        mainPanel.addMouseListener(mouseAdapter);
        mainPanel.addMouseMotionListener(mouseAdapter);

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

        setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainPanel, previewPanel);
        splitPane.setResizeWeight(0.75);
        add(splitPane, BorderLayout.CENTER);
    }

    private Rectangle drawFitImage(Graphics g) {
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
            
            // 実際の画像表示領域を返す
            return new Rectangle(x, y, scaledWidth, scaledHeight);
        }
        return null;
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
