import jdk.nashorn.internal.scripts.JO;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;

public class Main {
    public static int options = 0;
    public static String name = "";
    public static File imgPath = null;
    public static File chasePath = null;
    public static File jumpPath = null;
    public static File killPath = null;
    public static File appDir = null;
    public static File gmodLocation = null;
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        if (!System.getProperty("os.name").contains("Windows")) {
            JOptionPane.showMessageDialog(null, "This is a Windows only program, as it depends on many\nother Windows only programs. Sorry for the inconvenience.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        appDir = new File(System.getProperty("user.home") + "/AppData/Roaming/.nextbotcreator");
        appDir.mkdir();
        copy("gmad.exe", appDir);
        copy("gmpublish.exe", appDir);
        copy("VTFCmd.exe", appDir);
        copy("VTFLib.dll", appDir);
        copy("DevIL.dll", appDir);
        copy("steam_api.dll", appDir);
        File gmodLocationFile = new File(appDir, "gmodlocation.txt");
        if (gmodLocationFile.exists()) {
            gmodLocation = new File(new String(readInputStream(new FileInputStream(gmodLocationFile))));
        }
        JDialog frame = new JDialog((Dialog)null);
        frame.setTitle("GMod Nextbot Creator");
        frame.setLocation(100, 100);
        frame.getContentPane().setPreferredSize(new Dimension(271, 187));
        frame.pack();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        frame.setIconImage(new ImageIcon(Main.class.getResource("/files/icon.png")).getImage());
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setLayout(null);
        JTextField nameField = addOption(frame, "Name", new JTextField());
        JButton imageButton = addOption(frame, "Image", new JButton("<no image>"));
        JButton chaseButton = addOption(frame, "Chase Sound", new JButton("<no sound>"));
        JButton jumpButton = addOption(frame, "Jump Sound", new JButton("<no sound>"));
        JButton killButton = addOption(frame, "Kill Sound", new JButton("<no sound>"));
        JButton publishButton = new JButton("Publish");
        JButton addToGmodButton = new JButton("Add to GMod");
        publishButton.setBounds(5, 5 + options * 29, 128, 32);
        addToGmodButton.setBounds(138, 5 + options * 29, 128, 32);
        frame.add(publishButton);
        frame.add(addToGmodButton);
        nameField.addCaretListener((e) -> {
            name = nameField.getText();
        });
        imageButton.addActionListener((e) -> {
            File file = selectFile(frame, false, imageButton, "<no image>");
            if (file == null) return;
            imgPath = file;
        });
        chaseButton.addActionListener((e) -> {
            chasePath = selectFile(frame, true, chaseButton, "<no sound>");
        });
        jumpButton.addActionListener((e) -> {
            jumpPath = selectFile(frame, true, jumpButton, "<no sound>");
        });
        killButton.addActionListener((e) -> {
            killPath = selectFile(frame, true, killButton, "<no sound>");
        });
        publishButton.addActionListener((e) -> {
            if (!buildAddon()) return;
            createThumbnail();
            try {
                if (new ProcessBuilder().directory(appDir).command(new File(appDir, "gmad.exe").getAbsolutePath(), "create", "-folder", "addon", "-out", "addon.gma").start().waitFor() != 0) throw new Exception();
                if (new ProcessBuilder().directory(appDir).command(new File(appDir, "gmpublish.exe").getAbsolutePath(), "create", "-icon", "thumbnail.jpg", "-addon", "addon.gma").start().waitFor() != 0) throw new Exception();
                JOptionPane.showMessageDialog(null, "Done! Check your uploaded workshop addons.\nMake sure you edit the name & description before you make it public.", "GMod Nextbot Creator", JOptionPane.INFORMATION_MESSAGE);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occured trying to publish the nextbot", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        addToGmodButton.addActionListener((e) -> {
            if (gmodLocation == null) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Select GMod Directory");
                chooser.showOpenDialog(frame);
                if (chooser.getSelectedFile() == null) return;
                gmodLocation = new File(chooser.getSelectedFile(), "garrysmod/addons");
                if (!gmodLocation.exists() || gmodLocation.isFile()) {
                    JOptionPane.showMessageDialog(null, gmodLocation.getAbsolutePath() + "\nDirectory doesn't exist, or is a file", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    writeFile(gmodLocationFile, gmodLocation.getAbsolutePath());
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (!buildAddon()) return;
            try {
                copyDirectory(new File(appDir, "addon"), new File(gmodLocation, name + "_nextbot"));
                JOptionPane.showMessageDialog(null, "Done! Restart GMod if you have it running.", "GMod Nextbot Creator", JOptionPane.INFORMATION_MESSAGE);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occured trying to copy the addon files", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        frame.setVisible(true);
    }
    public static <T extends JComponent> T addOption(JDialog frame, String name, T component) {
        JLabel label = new JLabel(name);
        label.setBounds(5, 5 + options * 29 + 4, 128, 16);
        component.setBounds(138, 5 + options * 29, 128, 24);
        frame.add(label);
        frame.add(component);
        options++;
        return component;
    }
    public static void copy(String file, File dir) throws IOException {
        File dest = new File(dir, file);
        if (dest.exists()) return;
        System.out.println("Extracting " + file + "...");
        InputStream in = Main.class.getResourceAsStream("/" + file);
        OutputStream out = new FileOutputStream(dest);
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        while ((bytesRead = in.read(buffer)) > 0) {
            out.write(buffer, 0, bytesRead);
        }
        in.close();
        out.close();
    }
    public static File selectFile(JDialog parent, boolean erasable, JButton button, String defaultMsg) {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.showOpenDialog(parent);
        if (chooser.getSelectedFile() == null) {
            if (erasable) button.setText(defaultMsg);
            return null;
        }
        button.setText(chooser.getSelectedFile().getName());
        return chooser.getSelectedFile();
    }
    public static boolean checkValid() {
        String msg = null;
        if (imgPath == null) msg = "No image specified";
        if (name.isEmpty()) msg = "Name is empty";
        if (!checkInvalidCharacters(name, "ABCDEFHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_".toCharArray())) msg = "Name must contain only letters, numbers and underscores.";
        if (msg != null) {
            JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    public static boolean checkInvalidCharacters(String string, char... valid) {
        for (char character : string.toCharArray()) {
            boolean isValid = false;
            for (char validChar : valid) {
                if (character == validChar) isValid = true;
            }
            if (!isValid) return false;
        }
        return true;
    }
    public static boolean buildAddon() {
        try {
            if (!checkValid()) return false;
            File addonDir = new File(appDir, "addon");
            delete(addonDir);
            File matEntDir = new File(addonDir, "materials/entities/");
            File matNpcDir = new File(addonDir, "materials/npc_" + name + "/");
            File luaEntDir = new File(addonDir, "lua/entities/");
            File sndNpcDir = new File(addonDir, "sound/npc_" + name + "/");
            matEntDir.mkdirs();
            matNpcDir.mkdirs();
            luaEntDir.mkdirs();
            sndNpcDir.mkdirs();
            Placeholder[] placeholders = {
                new Placeholder("NEXTBOT-NAME", name, Placeholder.REPLACEMENT),
                chasePath == null ? null : new Placeholder("CHASE-SOUND-FILE", chasePath.getName(), Placeholder.REPLACEMENT),
                jumpPath == null ? null : new Placeholder("JUMP-SOUND-FILE", jumpPath.getName(), Placeholder.REPLACEMENT),
                killPath == null ? null : new Placeholder("KILL-SOUND-FILE", killPath.getName(), Placeholder.REPLACEMENT),
                new Placeholder("REMOVE-IF-NO-CHASE", chasePath == null ? Placeholder.REMOVE : Placeholder.INCLUDE, Placeholder.LINE_REMOVE),
                new Placeholder("REMOVE-IF-NO-JUMP", jumpPath == null ? Placeholder.REMOVE : Placeholder.INCLUDE, Placeholder.LINE_REMOVE),
                new Placeholder("REMOVE-IF-NO-KILL", killPath == null ? Placeholder.REMOVE : Placeholder.INCLUDE, Placeholder.LINE_REMOVE),
                new Placeholder("ADD-IF-NO-CHASE", chasePath == null ? Placeholder.INCLUDE : Placeholder.REMOVE, Placeholder.LINE_REMOVE),
            };
            writeFile(new File(luaEntDir, "npc_" + name + ".lua"), getAddonFile("lua-template.lua", placeholders));
            writeFile(new File(matNpcDir, "killicon.vmt"), getAddonFile("killicon-template.vmt", placeholders));
            writeFile(new File(matNpcDir, name + ".vmt"), getAddonFile("material-template.vmt", placeholders));
            writeFile(new File(addonDir, "addon.json"), getAddonFile("addon-template.json", placeholders));
            if (chasePath != null) Files.copy(chasePath.toPath(), new File(sndNpcDir, chasePath.getName()).toPath());
            if (jumpPath != null) Files.copy(jumpPath.toPath(), new File(sndNpcDir, jumpPath.getName()).toPath());
            if (killPath != null) Files.copy(killPath.toPath(), new File(sndNpcDir, killPath.getName()).toPath());
            File imagePath = new File(matEntDir, "npc_" + name + ".png");
            ImageIO.write(ImageIO.read(imgPath), "png", imagePath);
            if (new ProcessBuilder().directory(appDir).command(new File(appDir, "VTFCmd.exe").getAbsolutePath(), "-file", imagePath.getAbsolutePath(), "-output", matNpcDir.getAbsolutePath(), "-format", "rgb888").start().waitFor() != 0) throw new Exception();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occured trying to build the addon", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    public static String getAddonFile(String templateFile, Placeholder... placeholders) throws IOException {
        String str = new String(readInputStream(Main.class.getResourceAsStream("/files/" + templateFile)));
        for (Placeholder placeholder : placeholders) {
            if (placeholder == null) continue;
            if (placeholder.mode == Placeholder.REPLACEMENT) str = str.replaceAll("\\{\\{" + placeholder.placeholder + "}}", placeholder.value);
            if (placeholder.mode == Placeholder.LINE_REMOVE) {
                if (placeholder.value.equals(Placeholder.INCLUDE)) str = str.replaceAll("\\{\\{" + placeholder.placeholder + "}}", "");
                if (placeholder.value.equals(Placeholder.REMOVE)) {
                    // this regex: ^.*(\{\{PLACEHOLDER}}).*$ doesn't work for some reason
                    String[] lines = str.split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        if (lines[i].contains(placeholder.placeholder)) lines[i] = "";
                    }
                    str = String.join("\n", lines);
                }
            }
        }
        return str;
    }
    public static void createThumbnail() {
        try {
            BufferedImage image = ImageIO.read(imgPath);
            BufferedImage resized = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
            Graphics g = resized.getGraphics();
            g.drawImage(image, 0, 0, 512, 512, null);
            exportAsJpg(resized, new File(appDir, "thumbnail.jpg"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void writeFile(File file, String str) throws IOException {
        OutputStream out = new FileOutputStream(file);
        out.write(str.getBytes());
        out.close();
    }
    public static byte[] readInputStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        while ((bytesRead = in.read(buffer)) > 0) {
            out.write(buffer, 0, bytesRead);
        }
        return out.toByteArray();
    }
    public static void delete(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                delete(f);
            }
        }
        file.delete();
    }
    public static void copyDirectory(File dir, File dest) throws IOException {
        dest.mkdirs();
        for (File file : dir.listFiles()) {
            File destFile = new File(dest, file.getName());
            if (file.isDirectory()) copyDirectory(file, destFile);
            else {
                delete(destFile);
                Files.copy(file.toPath(), destFile.toPath());
            }
        }
    }
    public static void exportAsJpg(BufferedImage image, File file) throws IOException {
        BufferedImage img = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        int[] px = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), px, 0, image.getWidth());
        img.setRGB(0, 0, image.getWidth(), image.getHeight(), px, 0, image.getWidth());
        ImageIO.write(img, "jpg", file);
    }
    public static class Placeholder {
        public static final int REPLACEMENT = 0;
        public static final int LINE_REMOVE = 1;
        public static final String REMOVE = "rem";
        public static final String INCLUDE = "inc";
        public String placeholder;
        public String value;
        public int mode;
        public Placeholder(String placeholder, String value, int mode) {
            this.placeholder = placeholder;
            this.value = value;
            this.mode = mode;
        }
    }
}
