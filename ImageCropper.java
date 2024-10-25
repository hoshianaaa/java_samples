import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class ImageCropper extends JFrame {
    private ImagePanel imagePanel;
    private CroppedImagePanel croppedImagePanel;

    public ImageCropper() {
        setTitle("画像クロッパー");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 画像表示パネル
        imagePanel = new ImagePanel();
        // クロップ画像表示パネル
        croppedImagePanel = new CroppedImagePanel();

        // クロップイベントを設定
        imagePanel.setCropListener(croppedImage -> {
            croppedImagePanel.setCroppedImage(croppedImage);
        });

        // スプリットペインで左3/4と右1/4に分割
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imagePanel, croppedImagePanel);
        splitPane.setDividerLocation((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.75));
        splitPane.setResizeWeight(0.75);
        add(splitPane, BorderLayout.CENTER);

        // メニューバーに「画像を開く」オプションを追加
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("ファイル");
        JMenuItem openItem = new JMenuItem("画像を開く");
        openItem.addActionListener(e -> openImage());
        menu.add(openItem);
        menuBar.add(menu);
        setJMenuBar(menuBar);

        setSize(1200, 800);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void openImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(".")); // カレントディレクトリを初期ディレクトリに設定
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(selectedFile);
                imagePanel.setImage(img);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "画像の読み込みに失敗しました。", "エラー", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // メインメソッド
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageCropper());
    }
}

class ImagePanel extends JPanel {
    private BufferedImage originalImage;
    private BufferedImage displayedImage;
    private double scale = 1.0;

    private Point startPoint;
    private Point endPoint;
    private Rectangle selectionRect;

    private CropListener cropListener;

    public ImagePanel() {
        setBackground(Color.GRAY);
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (displayedImage != null) {
                    startPoint = e.getPoint();
                    endPoint = startPoint;
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (startPoint != null) {
                    endPoint = e.getPoint();
                    selectionRect = createRectangle(startPoint, endPoint);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (startPoint != null && endPoint != null && selectionRect != null) {
                    // 座標をオリジナル画像にスケールバック
                    int x = (int) ((selectionRect.x - getImageX()) / scale);
                    int y = (int) ((selectionRect.y - getImageY()) / scale);
                    int w = (int) (selectionRect.width / scale);
                    int h = (int) (selectionRect.height / scale);

                    // 画像の範囲内に収める
                    x = Math.max(0, x);
                    y = Math.max(0, y);
                    w = Math.min(originalImage.getWidth() - x, w);
                    h = Math.min(originalImage.getHeight() - y, h);

                    if (w > 0 && h > 0) {
                        BufferedImage cropped = originalImage.getSubimage(x, y, w, h);
                        if (cropListener != null) {
                            cropListener.onCrop(cropped);
                        }
                    }
                }
                startPoint = null;
                endPoint = null;
                selectionRect = null;
                repaint();
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private Rectangle createRectangle(Point p1, Point p2) {
        int x = Math.min(p1.x, p2.x);
        int y = Math.min(p1.y, p2.y);
        int w = Math.abs(p1.x - p2.x);
        int h = Math.abs(p1.y - p2.y);
        return new Rectangle(x, y, w, h);
    }

    public void setImage(BufferedImage img) {
        this.originalImage = img;
        this.displayedImage = getScaledImage(img);
        repaint();
    }

    private BufferedImage getScaledImage(BufferedImage img) {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        if (panelWidth == 0 || panelHeight == 0) {
            // 初期サイズがまだ設定されていない場合、フレームサイズを基に計算
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            panelWidth = (int) (screenSize.width * 0.75);
            panelHeight = screenSize.height;
        }
        double imgWidth = img.getWidth();
        double imgHeight = img.getHeight();

        double scaleX = (double) panelWidth / imgWidth;
        double scaleY = (double) panelHeight / imgHeight;
        scale = Math.min(scaleX, scaleY);

        int newWidth = (int) (imgWidth * scale);
        int newHeight = (int) (imgHeight * scale);

        Image scaled = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage buffered = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = buffered.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();
        return buffered;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (originalImage != null && displayedImage != null) {
            // 画像を中央に配置
            int x = getImageX();
            int y = getImageY();
            g.drawImage(displayedImage, x, y, this);
            // 矩形選択を描画
            if (selectionRect != null) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(2));
                g2d.draw(selectionRect);
            }
        }
    }

    private int getImageX() {
        return (getWidth() - displayedImage.getWidth()) / 2;
    }

    private int getImageY() {
        return (getHeight() - displayedImage.getHeight()) / 2;
    }

    // リサイズ時に画像を再スケール
    @Override
    public void invalidate() {
        super.invalidate();
        if (originalImage != null) {
            displayedImage = getScaledImage(originalImage);
        }
    }

    public void setCropListener(CropListener listener) {
        this.cropListener = listener;
    }

    // クロップイベントリスナーインターフェース
    interface CropListener {
        void onCrop(BufferedImage croppedImage);
    }
}

class CroppedImagePanel extends JPanel {
    private BufferedImage croppedImage;
    private BufferedImage displayedCroppedImage;
    private double scale = 1.0;

    public CroppedImagePanel() {
        setBackground(Color.LIGHT_GRAY);
    }

    public void setCroppedImage(BufferedImage img) {
        this.croppedImage = img;
        this.displayedCroppedImage = getScaledImage(img);
        repaint();
    }

    private BufferedImage getScaledImage(BufferedImage img) {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        if (panelWidth == 0 || panelHeight == 0) {
            // 初期サイズがまだ設定されていない場合、フレームサイズを基に計算
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            panelWidth = (int) (screenSize.width * 0.25);
            panelHeight = screenSize.height;
        }
        double imgWidth = img.getWidth();
        double imgHeight = img.getHeight();

        double scaleX = (double) panelWidth / imgWidth;
        double scaleY = (double) panelHeight / imgHeight;
        scale = Math.min(scaleX, scaleY);

        int newWidth = (int) (imgWidth * scale);
        int newHeight = (int) (imgHeight * scale);

        Image scaled = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage buffered = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = buffered.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();
        return buffered;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (displayedCroppedImage != null) {
            // 画像を中央に配置
            int x = (getWidth() - displayedCroppedImage.getWidth()) / 2;
            int y = (getHeight() - displayedCroppedImage.getHeight()) / 2;
            g.drawImage(displayedCroppedImage, x, y, this);
        }
    }

    // リサイズ時に画像を再スケール
    @Override
    public void invalidate() {
        super.invalidate();
        if (croppedImage != null) {
            displayedCroppedImage = getScaledImage(croppedImage);
        }
    }
}

