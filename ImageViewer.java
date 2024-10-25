import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageViewer extends JFrame {
    private JLabel imageLabel;
    private JButton selectButton;
    private JButton cropButton;
    private Point startPoint;
    private Point endPoint;
    private boolean isDragging = false;
    private BufferedImage originalImage;
    private JPanel croppedImagesPanel;
    private List<BufferedImage> croppedImages = new ArrayList<>();
    
    public ImageViewer() {
        setTitle("画像ビューア（クロップ機能付き）");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // メインパネル（左側：元画像、右側：クロップ画像）
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // 左側パネル（元画像）
        JPanel leftPanel = new JPanel(new BorderLayout());
        
        // 画像表示用のカスタムラベル
        imageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (isDragging && startPoint != null && endPoint != null) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(new Color(0, 0, 255, 50));
                    Rectangle rect = createRect(startPoint, endPoint);
                    g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
                    g2d.setColor(Color.BLUE);
                    g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
                }
            }
        };
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        
        // マウスリスナーの追加
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                isDragging = true;
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                endPoint = e.getPoint();
                imageLabel.repaint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
                endPoint = e.getPoint();
                imageLabel.repaint();
            }
        };
        
        imageLabel.addMouseListener(mouseAdapter);
        imageLabel.addMouseMotionListener(mouseAdapter);
        
        // スクロールペイン
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        leftPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 右側パネル（クロップ画像）
        croppedImagesPanel = new JPanel();
        croppedImagesPanel.setLayout(new BoxLayout(croppedImagesPanel, BoxLayout.Y_AXIS));
        JScrollPane rightScrollPane = new JScrollPane(croppedImagesPanel);
        rightScrollPane.setPreferredSize(new Dimension(200, 0));
        
        // ボタンパネル
        JPanel buttonPanel = new JPanel();
        selectButton = new JButton("画像を選択");
        cropButton = new JButton("選択範囲をクロップ");
        cropButton.setEnabled(false);
        
        selectButton.addActionListener(e -> selectAndDisplayImage());
        cropButton.addActionListener(e -> cropSelectedArea());
        
        buttonPanel.add(selectButton);
        buttonPanel.add(cropButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // スプリットペインに追加
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightScrollPane);
        
        add(splitPane, BorderLayout.CENTER);
        
        setSize(1000, 600);
        setLocationRelativeTo(null);
    }
    
    private Rectangle createRect(Point p1, Point p2) {
        return new Rectangle(
            Math.min(p1.x, p2.x),
            Math.min(p1.y, p2.y),
            Math.abs(p1.x - p2.x),
            Math.abs(p1.y - p2.y)
        );
    }
    
    private void selectAndDisplayImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                       name.endsWith(".png") || name.endsWith(".gif");
            }
            
            public String getDescription() {
                return "画像ファイル (*.jpg, *.jpeg, *.png, *.gif)";
            }
        });
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // 画像をBufferedImageとして読み込み
                ImageIcon icon = new ImageIcon(selectedFile.getPath());
                Image image = icon.getImage();
                originalImage = new BufferedImage(
                    image.getWidth(null),
                    image.getHeight(null),
                    BufferedImage.TYPE_INT_RGB
                );
                
                Graphics g = originalImage.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
                
                // 画像のリサイズ処理
                Image resizedImage = image.getScaledInstance(-1, 500, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(resizedImage));
                cropButton.setEnabled(true);
                pack();
                setLocationRelativeTo(null);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "画像の読み込みに失敗しました: " + ex.getMessage(),
                    "エラー",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void cropSelectedArea() {
        if (startPoint != null && endPoint != null && originalImage != null) {
            try {
                // 選択範囲の座標を取得
                Rectangle rect = createRect(startPoint, endPoint);
                
                // 画像の実際のサイズと表示サイズの比率を計算
                double scaleX = (double) originalImage.getWidth() / imageLabel.getIcon().getIconWidth();
                double scaleY = (double) originalImage.getHeight() / imageLabel.getIcon().getIconHeight();
                
                // 選択範囲を実際の画像サイズに変換
                int x = (int) (rect.x * scaleX);
                int y = (int) (rect.y * scaleY);
                int width = (int) (rect.width * scaleX);
                int height = (int) (rect.height * scaleY);
                
                // 範囲チェック
                x = Math.max(0, Math.min(x, originalImage.getWidth()));
                y = Math.max(0, Math.min(y, originalImage.getHeight()));
                width = Math.min(width, originalImage.getWidth() - x);
                height = Math.min(height, originalImage.getHeight() - y);
                
                // クロップ実行
                BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);
                croppedImages.add(croppedImage);
                
                // クロップ画像を右パネルに追加
                JLabel croppedLabel = new JLabel(new ImageIcon(
                    croppedImage.getScaledInstance(180, -1, Image.SCALE_SMOOTH)
                ));
                croppedLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                croppedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                
                // 保存ボタン
                JButton saveButton = new JButton("保存");
                saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                saveButton.addActionListener(e -> saveImage(croppedImage));
                
                JPanel croppedPanel = new JPanel();
                croppedPanel.setLayout(new BoxLayout(croppedPanel, BoxLayout.Y_AXIS));
                croppedPanel.add(croppedLabel);
                croppedPanel.add(saveButton);
                croppedPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                
                croppedImagesPanel.add(croppedPanel);
                croppedImagesPanel.revalidate();
                croppedImagesPanel.repaint();
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "クロップ処理に失敗しました: " + ex.getMessage(),
                    "エラー",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void saveImage(BufferedImage image) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                return f.getName().toLowerCase().endsWith(".png");
            }
            
            public String getDescription() {
                return "PNG画像 (*.png)";
            }
        });
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".png")) {
                    file = new File(file.getPath() + ".png");
                }
                javax.imageio.ImageIO.write(image, "png", file);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "画像の保存に失敗しました: " + ex.getMessage(),
                    "エラー",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageViewer().setVisible(true));
    }
}
