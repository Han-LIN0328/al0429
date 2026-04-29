import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ChamferDistanceCat {

    public static void main(String[] args) {
        try {
            // 1. 直接指定讀取你的貓咪圖片
            String[] possibleFiles = {
                "cat.png",
                "c:\\Users\\user\\Desktop\\aa\\cat.png"
            };
            
            BufferedImage inputImg = null;
            String successFile = null;
            
            for (String filename : possibleFiles) {
                File inputFile = new File(filename);
                if (inputFile.exists()) {
                    inputImg = ImageIO.read(inputFile);
                    if (inputImg != null) {
                        successFile = filename;
                        break;
                    }
                }
            }
            
            if (inputImg == null) {
                System.out.println("✗ 錯誤：找不到 cat.png，請確認圖片是否與程式放在同一個資料夾，或路徑是否正確。");
                return;
            }
            System.out.println("✓ 成功讀取圖片：" + successFile);
            System.out.println("✓ 圖片大小：" + inputImg.getWidth() + "x" + inputImg.getHeight());

            int cols = inputImg.getWidth();
            int rows = inputImg.getHeight();
            int[][] imageArray = new int[rows][cols];

            // 2. 影像二值化 (抓取貓咪輪廓)
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    Color c = new Color(inputImg.getRGB(x, y));
                    int gray = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                    // 假設深色為特徵點 (灰階值 < 128)
                    imageArray[y][x] = (gray < 128) ? 1 : 0;
                }
            }

            // 3. 執行 Chamfer Distance 演算法
            System.out.println("♦ 正在計算 Chamfer Distance...");
            int[][] result = calculateChamfer(imageArray);

            // 4. 儲存結果影像 (存成報告指定的檔名)
            String outputFileName = "alllresult0310.png";
            saveAsImage(result, outputFileName);

        } catch (Exception e) {
            System.out.println("發生錯誤：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Chamfer Distance 核心演算法 (Two-pass) ---
    public static int[][] calculateChamfer(int[][] img) {
        int rows = img.length;
        int cols = img[0].length;
        int[][] dist = new int[rows][cols];
        final int MAX_DISTANCE = 99999;

        // 初始化
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                dist[i][j] = (img[i][j] == 1) ? 0 : MAX_DISTANCE;
            }
        }

        // 正向掃描
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (dist[i][j] == 0) continue;
                int top = (i > 0) ? dist[i - 1][j] + 1 : MAX_DISTANCE;
                int left = (j > 0) ? dist[i][j - 1] + 1 : MAX_DISTANCE;
                dist[i][j] = Math.min(dist[i][j], Math.min(top, left));
            }
        }

        // 反向掃描
        for (int i = rows - 1; i >= 0; i--) {
            for (int j = cols - 1; j >= 0; j--) {
                int bottom = (i < rows - 1) ? dist[i + 1][j] + 1 : MAX_DISTANCE;
                int right = (j < cols - 1) ? dist[i][j + 1] + 1 : MAX_DISTANCE;
                dist[i][j] = Math.min(dist[i][j], Math.min(bottom, right));
            }
        }
        return dist;
    }

    // --- 將距離陣列轉回圖片並存檔 (高對比清晰版) ---
    public static void saveAsImage(int[][] dist, String filename) {
        int height = dist.length;
        int width = dist[0].length;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final int MAX_DISTANCE = 99999;

        int maxDist = 0;
        for (int[] row : dist) {
            for (int val : row) {
                if (val != MAX_DISTANCE) {
                    maxDist = Math.max(maxDist, val);
                }
            }
        }

        // 壓低亮度天花板，讓漸層提早展開，增加立體感
        double displayMaxDist = maxDist * 0.6; 
        if (displayMaxDist < 1) displayMaxDist = 1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int val = dist[y][x];
                int gray;
                
                if (val == MAX_DISTANCE) {
                    gray = 255; 
                } else {
                    // --- 一般的高對比漸層 ---
                    double normalized = val / displayMaxDist;
                    normalized = Math.pow(normalized, 0.7); // 溫和的 Gamma 校正
                    gray = (int)(normalized * 255);
                    
                    // 💡 如果你想要超級明顯的「等高線/水波紋」特效，
                    // 請把上面三行加上雙斜線註解，並把下面這行的註解拿掉：
                    // gray = (val * 15) % 255; 
                }
                
                gray = Math.max(0, Math.min(255, gray));
                int rgb = (gray << 16) | (gray << 8) | gray;
                img.setRGB(x, y, rgb);
            }
        }

        try {
            ImageIO.write(img, "png", new File(filename));
            System.out.println("✓ 圖檔已成功生成：" + filename + " (高對比清晰版)");
        } catch (Exception e) {
            System.out.println("✗ 圖片存檔失敗：" + e.getMessage());
        }
    }
}