import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class ImageViewer extends JFrame {
    private JLabel imageLabel;
    private JButton selectButton;
    
    public ImageViewer() {
        setTitle("画像ビューア");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // 画像表示用のラベル
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        
        // スクロールペイン
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        add(scrollPane, BorderLayout.CENTER);
        
        // ファイル選択ボタン
        selectButton = new JButton("画像を選択");
        selectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectAndDisplayImage();
            }
        });
        
        // ボタンパネル
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(selectButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // ウィンドウサイズ設定
        setSize(800, 600);
        setLocationRelativeTo(null);
    }
    
    private void selectAndDisplayImage() {
        JFileChooser fileChooser = new JFileChooser();
        // 画像ファイルのフィルター設定
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
                // 画像の読み込みと表示
                ImageIcon icon = new ImageIcon(selectedFile.getPath());
                // 画像のリサイズ処理
                Image image = icon.getImage();
                Image resizedImage = image.getScaledInstance(-1, 500, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(resizedImage));
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
    
    public static void main(String[] args) {
        // UIスレッドで実行
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ImageViewer().setVisible(true);
            }
        });
    }
}
