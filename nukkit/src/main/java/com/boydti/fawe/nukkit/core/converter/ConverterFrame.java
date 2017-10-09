package com.boydti.fawe.nukkit.core.converter;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweVersion;
import com.boydti.fawe.installer.BrowseButton;
import com.boydti.fawe.installer.CloseButton;
import com.boydti.fawe.installer.ImagePanel;
import com.boydti.fawe.installer.InteractiveButton;
import com.boydti.fawe.installer.InvisiblePanel;
import com.boydti.fawe.installer.MinimizeButton;
import com.boydti.fawe.installer.MovablePanel;
import com.boydti.fawe.installer.TextAreaOutputStream;
import com.boydti.fawe.installer.URLButton;
import com.boydti.fawe.object.clipboard.ClipboardRemapper;
import com.boydti.fawe.object.clipboard.ItemWikiScraper;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.wrappers.FakePlayer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class ConverterFrame extends JFrame {
    private final InvisiblePanel loggerPanel;
    private final JProgressBar progressBar;
    private Color LIGHT_GRAY = new Color(0x66, 0x66, 0x66);
    private Color GRAY = new Color(0x44, 0x44, 0x46);
    private Color DARK_GRAY = new Color(0x33, 0x33, 0x36);
    private Color DARKER_GRAY = new Color(0x26, 0x26, 0x28);
    private Color INVISIBLE = new Color(0, 0, 0, 0);
    private Color OFF_WHITE = new Color(200, 200, 200);

    private JTextArea loggerTextArea;
    private BrowseButton browseLoad;
    private BrowseButton browseSave;

    public ConverterFrame() throws Exception {
        async(() -> downloadDependencies());

        final MovablePanel movable = new MovablePanel(this);
        movable.setBorder(BorderFactory.createLineBorder(new Color(0x28, 0x28, 0x29)));

        Container content = this.getContentPane();
        content.add(movable);
        this.setSize(720, 640);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setUndecorated(true);
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - this.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - this.getHeight()) / 2);
        this.setLocation(x, y);
        this.setVisible(true);

        if (this.transparency(0)) {
            fadeIn();
        }

        movable.setBackground(DARK_GRAY);
        movable.setLayout(new BorderLayout());

        JPanel topBar = new InvisiblePanel(new BorderLayout());
        {
            JPanel topBarLeft = new InvisiblePanel();
            JPanel topBarCenter = new InvisiblePanel();
            JPanel topBarRight = new InvisiblePanel();

            JLabel title = new JLabel();
            title.setHorizontalAlignment(SwingConstants.CENTER);
            title.setAlignmentX(Component.RIGHT_ALIGNMENT);
            title.setForeground(Color.LIGHT_GRAY);
            title.setText("(FAWE) Anvil and LevelDB converter");
            title.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 15));
            setTitle(null);

            MinimizeButton minimize = new MinimizeButton(this);
            CloseButton exit = new CloseButton();

            topBarLeft.setPreferredSize(new Dimension(96, 36));
            try {
                BufferedImage image = ImageIO.read(getClass().getResource("/axe-logo.png"));
                setIconImage(image);
                ImagePanel imgPanel = new ImagePanel(image);
                imgPanel.setPreferredSize(new Dimension(32, 36));
                topBarLeft.add(imgPanel);
            } catch (IOException ignore) {}

            topBarCenter.add(title);
            topBarRight.add(minimize);
            topBarRight.add(exit);

            topBar.add(topBarLeft, BorderLayout.WEST);
            topBar.add(topBarCenter, BorderLayout.CENTER);
            topBar.add(topBarRight, BorderLayout.EAST);
        }
        final JPanel mainContent = new InvisiblePanel(new BorderLayout());
        {
            File world = MainUtil.getWorkingDirectory("minecraft");
            long lastMod = Long.MIN_VALUE;
            if (world != null && world.exists()) {
                File saves = new File(world, "saves");
                if (saves.exists()) {
                    for (File file : saves.listFiles()) {
                        if (file.isDirectory()) {
                            long modified = file.lastModified();
                            if (modified > lastMod) {
                                world = file;
                                lastMod = modified;
                            }
                        }
                    }
                }
            }

            final InteractiveButton browseLoadText = new InteractiveButton(world.getPath(), DARKER_GRAY) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    browseLoad.browse(new File(getText()).getParentFile());
                }
            };
            final InteractiveButton browseSaveText = new InteractiveButton(getDefaultOutput().getPath(), DARKER_GRAY) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    browseSave.browse(new File(getText()));
                }
            };
            for (JButton button : Arrays.asList(browseLoadText, browseSaveText)) {
                button.setForeground(OFF_WHITE);
                button.setBackground(DARKER_GRAY);
                button.setOpaque(true);
                button.setBorder(new EmptyBorder(4, 4, 4, 4));
            }
            browseLoad = new BrowseButton("_FROM") {
                @Override
                public void onSelect(File folder) {
                    browseLoadText.setText(folder.getPath());
                    movable.repaint();
                }
            };
            browseSave = new BrowseButton("_TO") {
                @Override
                public void onSelect(File folder) {
                    browseSaveText.setText(folder.getPath());
                    movable.repaint();
                }
            };

            final JPanel browseContent = new InvisiblePanel(new BorderLayout());
            final JPanel browseLoadContent = new InvisiblePanel(new BorderLayout());
            final JPanel browseSaveContent = new InvisiblePanel(new BorderLayout());
            browseSaveContent.setBorder(new EmptyBorder(10, 0, 0, 0));
            JLabel selectWorld = new JLabel("Select World:");
            selectWorld.setForeground(OFF_WHITE);
            selectWorld.setPreferredSize(new Dimension(120, 0));
            JLabel output = new JLabel("Output:");
            output.setForeground(OFF_WHITE);
            output.setPreferredSize(new Dimension(120, 0));

            browseLoadContent.add(selectWorld, BorderLayout.WEST);
            browseLoadContent.add(browseLoadText, BorderLayout.CENTER);
            browseLoadContent.add(browseLoad, BorderLayout.EAST);
            browseSaveContent.add(output, BorderLayout.WEST);
            browseSaveContent.add(browseSaveText, BorderLayout.CENTER);
            browseSaveContent.add(browseSave, BorderLayout.EAST);
            browseContent.add(browseLoadContent, BorderLayout.NORTH);
            browseContent.add(browseSaveContent, BorderLayout.SOUTH);

            InteractiveButton install = new InteractiveButton(">> Convert World <<", DARKER_GRAY) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        install(browseLoadText.getText(), browseSaveText.getText());
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            };

            final JPanel installContent = new InvisiblePanel(new FlowLayout());
            install.setPreferredSize(new Dimension(Integer.MAX_VALUE, 32));
            installContent.add(install);
            installContent.setBorder(new EmptyBorder(10, 0, 10, 0));
            this.loggerPanel = new InvisiblePanel(new BorderLayout());
            loggerPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 380));
            loggerTextArea = new JTextArea();
            loggerTextArea.setBackground(Color.GRAY);
            loggerTextArea.setForeground(Color.DARK_GRAY);
            loggerTextArea.setFont(new Font(loggerTextArea.getFont().getName(), Font.BOLD, 9));
            loggerTextArea.setBorder(BorderFactory.createCompoundBorder(loggerTextArea.getBorder(), BorderFactory.createEmptyBorder(6, 6, 6, 6)));
            JScrollPane scroll = new JScrollPane(loggerTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBackground(DARK_GRAY);
            scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
            loggerPanel.add(scroll);
            loggerPanel.setVisible(false);

            this.progressBar = new JProgressBar();
            progressBar.setVisible(false);
            progressBar.setStringPainted(true);
            progressBar.setBackground(DARK_GRAY);
            progressBar.setForeground(OFF_WHITE);

            mainContent.setBorder(new EmptyBorder(6, 32, 6, 32));
            mainContent.add(browseContent, BorderLayout.NORTH);
            mainContent.add(installContent, BorderLayout.CENTER);

            final JPanel console = new InvisiblePanel(new BorderLayout());
            console.setBorder(new EmptyBorder(6, 0, 6, 0));
            console.add(progressBar, BorderLayout.SOUTH);
            console.add(loggerPanel, BorderLayout.NORTH);

            mainContent.add(console, BorderLayout.SOUTH);
        }
        JPanel bottomBar = new InvisiblePanel();
        {
            try {
                InputStream stream = getClass().getResourceAsStream("/fawe.properties");
                java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
                String versionString = scanner.next().trim();
                scanner.close();
                FaweVersion version = new FaweVersion(versionString);
                String date = new Date(100 + version.year, version.month, version.day).toGMTString();
                String build = "https://ci.athion.net/job/FastAsyncWorldEdit/" + version.build;
                String commit = "https://github.com/boy0001/FastAsyncWorldedit/commit/" + Integer.toHexString(version.hash);
                String footerMessage = "FAWE v" + version.major + "." + version.minor + "." + version.patch + " by Empire92 (c) 2017 (GPL v3.0)";
                URL licenseUrl = new URL("https://github.com/boy0001/FastAsyncWorldedit/blob/master/LICENSE");
                URLButton licenseButton = new URLButton(licenseUrl, footerMessage);
                bottomBar.add(licenseButton);
            } catch (Throwable ignore) {
                ignore.printStackTrace();
            }
            URL chat = new URL("https://discord.gg/ngZCzbU");
            URLButton chatButton = new URLButton(chat, "Chat");
            bottomBar.add(chatButton);
            URL wiki = new URL("https://github.com/boy0001/FastAsyncWorldedit/wiki");
            URLButton wikiButton = new URLButton(wiki, "Wiki");
            bottomBar.add(wikiButton);
            URL issue = new URL("https://github.com/boy0001/FastAsyncWorldedit/issues/new");
            URLButton issueButton = new URLButton(issue, "Report Issue");
            bottomBar.add(issueButton);
            bottomBar.setBackground(new Color(0x26, 0x26, 0x28));

            bottomBar.add(new InteractiveButton("Debug") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
                    for (Map.Entry<Thread, StackTraceElement[]> entry : stacks.entrySet()) {
                        Thread thread = entry.getKey();
                        Fawe.debug("--------------------------------------------------------------------------------------------");
                        Fawe.debug("Thread: " + thread.getName() + " | Id: " + thread.getId() + " | Alive: " + thread.isAlive());
                        for (StackTraceElement elem : entry.getValue()) {
                            Fawe.debug(elem.toString());
                        }
                    }
                }
            });
        }

        // We want to add these a bit later
        movable.add(topBar, BorderLayout.NORTH);
        this.setVisible(true);
        this.repaint();
        movable.add(mainContent, BorderLayout.CENTER);
        this.setVisible(true);
        this.repaint();
        movable.add(bottomBar, BorderLayout.SOUTH);
        this.setVisible(true);
        this.repaint();
    }

    public void prompt(String message) {
        JOptionPane.showMessageDialog(null, message);
        Fawe.debug(message);
    }

    public void debug(String m) {
        System.out.println(m);
    }

    public void setProgress(String text, int percent) {
        Border border = BorderFactory.createTitledBorder(new EmptyBorder(0, 0, 0, 0), "Time remaining: " + text, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Lucida Sans Unicode",Font.PLAIN,12), OFF_WHITE);
        progressBar.setVisible(true);
        progressBar.setBorder(border);
        progressBar.setValue(percent);
        repaint();
    }

    private void fadeIn() {
        async(() -> {
            for (float i = 0; i <= 1.015; i += 0.016) {
                if (!transparency(Math.min(1, i))) return;
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (UnsupportedOperationException ignore) {
                    return;
                }
            }
        });
    }

    private boolean transparency(float val) {
        try {
            super.setOpacity(val);
            return true;
        } catch (UnsupportedOperationException ignore) {
            return false;
        }
    }

    private File getDefaultOutput() {
        if (MainUtil.getPlatform() == MainUtil.OS.WINDOWS) {
            String applicationData = System.getenv("APPDATA");
            if (applicationData != null && new File(applicationData).exists()) {
                File saves = new File(new File(applicationData).getParentFile(), "Local/Packages/Microsoft.MinecraftUWP_8wekyb3d8bbwe/LocalState/games/com.mojang/minecraftWorlds");
                if (saves.exists()) return saves.getAbsoluteFile();
            }
        }
        return new File(".");
    }

    private void async(Runnable r) {
        new Thread(r).start();
    }

    private AtomicBoolean dependenciesLoaded = new AtomicBoolean(false);
    private void downloadDependencies() {
        synchronized (dependenciesLoaded) {
            if (dependenciesLoaded.get()) return;
            try {
                ExecutorService pool = Executors.newCachedThreadPool();
                ItemWikiScraper scraper = new ItemWikiScraper();

                File lib = new File("lib");
                File leveldb = new File(lib, "leveldb_v1.jar");
                URL levelDbUrl = new URL("https://git.io/vdZ9e");

                pool.submit((Runnable) () -> {
                    try {
                        MainUtil.download(levelDbUrl, leveldb);
                        MainUtil.loadURLClasspath(leveldb.toURL());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                pool.submit((Runnable) () -> {
                    try {
                        scraper.scapeOrCache(ClipboardRemapper.RemapPlatform.PE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                pool.submit((Runnable) () -> {
                    try {
                        scraper.scapeOrCache(ClipboardRemapper.RemapPlatform.PC);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                pool.shutdown();
                try {
                    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                dependenciesLoaded.set(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void install(String input, String output) throws Exception {
        if (!loggerPanel.isVisible()) {
            loggerPanel.setVisible(true);
            this.repaint();
            TextAreaOutputStream logger = new TextAreaOutputStream(loggerTextArea);
            System.setOut(logger);
            System.setErr(logger);
        }
        if (input == null || input.isEmpty()) {
            prompt("No world selected");
            return;
        }
        if (output == null || output.isEmpty()) {
            prompt("No output folder selection");
            return;
        }
        if (new File(output, new File(input).getName()).exists()) {
            prompt("Please select another output directory, or delete it, as there are files already there.");
            return;
        }
        final File dirMc = new File(input);
        if (!dirMc.exists()) {
            prompt("Folder does not exist");
            return;
        }
        if (!dirMc.isDirectory()) {
            prompt("You must select a folder, not a file");
            return;
        }
        Thread installThread = new Thread(new Runnable() {
            @Override
            public void run() {
                FakePlayer console = FakePlayer.getConsole();
                try {
                    debug("Downloading levedb.jar and mappings (~4MB), please wait...");
                    downloadDependencies();
                    debug("Starting converter...");
                    File newWorldFile = new File(output, dirMc.getName());
                    MapConverter converter = MapConverter.get(dirMc, newWorldFile);
                    converter.accept(ConverterFrame.this);
                } catch (Throwable e) {
                    e.printStackTrace();
                    prompt("[ERROR] Conversion failed, you will have to do it manually (Nukkit server + anvil2leveldb command)");
                    return;
                }
                System.gc();
                System.gc();
            }
        });
        installThread.start();
    }

    public static void main(String[] args) throws Exception {
        ConverterFrame window = new ConverterFrame();
    }
}
