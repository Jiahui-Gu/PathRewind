import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenPreview {
    public static void main(String[] args) throws Exception {
        int w = 512, h = 512;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background gradient: dark blue to black
        GradientPaint bg = new GradientPaint(0, 0, new Color(20, 20, 60), 0, h, new Color(5, 5, 20));
        g.setPaint(bg);
        g.fillRect(0, 0, w, h);

        // Draw a rewind arrow icon (two triangles pointing left)
        g.setColor(new Color(100, 150, 255));
        int cx = w / 2, cy = h / 2 - 30;
        int[] x1 = {cx - 10, cx + 50, cx + 50};
        int[] y1 = {cy, cy - 45, cy + 45};
        g.fillPolygon(x1, y1, 3);
        int[] x2 = {cx - 70, cx - 10, cx - 10};
        int[] y2 = {cy, cy - 45, cy + 45};
        g.fillPolygon(x2, y2, 3);

        // Title
        g.setFont(new Font("Arial", Font.BOLD, 52));
        FontMetrics fm = g.getFontMetrics();
        String title = "Path Rewind";
        int tw = fm.stringWidth(title);
        g.setColor(new Color(200, 220, 255));
        g.drawString(title, (w - tw) / 2, cy + 110);

        // Subtitle
        g.setFont(new Font("Arial", Font.PLAIN, 22));
        fm = g.getFontMetrics();
        String sub = "Click visited nodes to rewind";
        int sw = fm.stringWidth(sub);
        g.setColor(new Color(150, 170, 220));
        g.drawString(sub, (w - sw) / 2, cy + 145);

        g.dispose();
        ImageIO.write(img, "png", new File(args[0]));
        System.out.println("Preview saved to " + args[0]);
    }
}
