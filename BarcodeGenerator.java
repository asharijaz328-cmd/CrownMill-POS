import java.awt.Image;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class BarcodeGenerator {

    // Ye function internet se barcode utha kar JLabel par set karega
    public static void setBarcode(JLabel label, String text) {
        if (label == null || text == null || text.isEmpty()) return;

        // Threading use kar rahe hain taake software "hang" na ho load hote waqt
        new Thread(() -> {
            try {
                // Barcode API URL
            String urlString = "https://bwipjs-api.metafloor.com/?bcid=code128&barcolor=ffffff&text=" + text.replace(" ", "%20") + "&scale=1&height=10";
                URL url = new URL(urlString);
                
                // Image read karna
                Image image = ImageIO.read(url);
                
                if (image != null) {
                    label.setIcon(new ImageIcon(image));
                    label.setText(""); // Placeholder text hata dein
                }
            } catch (Exception e) {
                label.setText("No Internet/Error");
                System.out.println("Barcode Error: " + e.getMessage());
            }
        }).start();
    }
}