import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class RefinedChamferDistance {

    public static void main(String[] args) {
        try {
            // 1. 讀取圖片 (確保 cat.png 與此程式在同一資料夾)
            File inputFile = new File("cat.png");
            if (!inputFile.exists()) {
                System.out.println("✗ 錯誤：找不到 cat.png，請確認檔案路徑。");
                return;
            }
            BufferedImage inputImg = ImageIO.read(inputFile);
            
            System.out.println("✓ 成功讀取 cat.png: 大小 " + inputImg.getWidth() + "x" + inputImg.getHeight());

            int cols = inputImg.getWidth();
            int rows = inputImg.getHeight();
            int[][] imageArray = new int[rows][cols];

            // 2. 影像二值化 (抓取貓咪輪廓)
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    Color c = new Color(inputImg.getRGB(x, y));
                    int gray = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                    imageArray[y][x] = (gray < 128) ? 1 : 0;
                }
            }

            // 3. 執行 Chamfer Distance 演算法
            System.out.println("♦ 正在計算 Chamfer Distance...");
            int[][] result = calculateChamfer(imageArray);

            // 4. 儲存高細節影像
            String outputFileName = "alllresult0310-2.png";
            saveAsHighDetailImage(result, outputFileName);

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

    // --- 將距離陣列轉回圖片 (【超高細節強化版】) ---
    public static void saveAsHighDetailImage(int[][] dist, String filename) {
        int height = dist.length;
        int width = dist[0].length;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final int MAX_DISTANCE = 99999;

        // 找出實際的最大距離
        int realMaxDist = 0;
        for (int[] row : dist) {
            for (int val : row) {
                if (val < MAX_DISTANCE && val > realMaxDist) {
                    realMaxDist = val;
                }
            }
        }

        // 【細節強化 1】：壓低天花板。
        // 不考慮最邊角的極遠距離，只取最大距離的 30% 作為漸層區間，讓漸層集中在貓咪周圍
        double ceiling = realMaxDist * 0.3; 
        if (ceiling < 1) ceiling = 1.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int val = dist[y][x];
                int gray;
                
                if (val >= MAX_DISTANCE) {
                    gray = 255;
                } else {
                    // 標準化距離 (大於天花板的會被截斷為 1.0)
                    double norm = val / ceiling;
                    if (norm > 1.0) norm = 1.0;
                    
                    // 【細節強化 2】：開根號映射 (Square Root Mapping)
                    // 這會把數值小的部分（也就是靠近貓咪輪廓的暗部）大幅提亮
                    // 讓原本看不見的內部骨架和星芒細節瞬間變得超級清晰
                    gray = (int)(Math.sqrt(norm) * 255.0);
                    
                    // 💡 如果你想要更誇張的「等高線地圖」效果（保證細節最豐富），
                    // 把上面那行加雙斜線註解，並把下面這行註解拿掉：
                    // gray = (val * 20) % 256;
                }
                
                gray = Math.max(0, Math.min(255, gray));
                int rgb = (gray << 16) | (gray << 8) | gray;
                img.setRGB(x, y, rgb);
            }
        }

        try {
            ImageIO.write(img, "png", new File(filename));
            System.out.println("✓ 超高細節黑白影像已成功生成: " + filename);
        } catch (Exception e) {
            System.out.println("✗ 圖片存檔失敗：" + e.getMessage());
        }
    }
}