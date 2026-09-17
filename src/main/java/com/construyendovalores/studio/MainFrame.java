package com.construyendovalores.studio;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dimension;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

public class MainFrame extends javax.swing.JFrame {

    private final Config cfg;
    private final Map<String, JComboBox<String>> voiceBoxes = new HashMap<>();
    private final Map<String, JSpinner> voicePitchSpinners = new HashMap<>();
    private final Map<String, JSpinner> voiceRateSpinners = new HashMap<>();

    private static int parseHz(String s) {
        try {
            return Integer.parseInt(s.replace("Hz", "").replace("+", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static int parseRate(String s) {
        try {
            return Integer.parseInt(s.replace("%", "").replace("+", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static int parseHour(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 9;
        }
    }

    private final JTextField topicField = new JTextField("La torre que no quiso rendirse", 30);
    private final JTextField valueField = new JTextField("Perseverancia", 15);
    private final JSpinner scenesSpinner = new JSpinner(new SpinnerNumberModel(5, 2, 12, 1));
    private final JComboBox<Scenario> scenarioBox = new JComboBox<>(Scenario.values());
    private final JTextArea scriptArea = new JTextArea(16, 60);
    private final JTextArea logArea = new JTextArea(14, 70);
    private final JTextField outField = new JTextField(new File("videos").getAbsolutePath(), 20);
    private final JTextField keyField = new JTextField(cfg().apiKey(), 40);

    // Para config
    private final JTextField modelField = new JTextField(cfg().geminiModel(), 20);
    private final JTextField pythonField = new JTextField(cfg().pythonCmd(), 15);
    private final JTextField ffmpegField = new JTextField(cfg().ffmpeg(), 40);
    private final JTextField musicField = new JTextField(cfg().musicPath(), 30);
    private final JTextField clientIdField = new JTextField(cfg().get("youtube.clientId", ""), 40);
    private final JTextField clientSecretField = new JTextField(cfg().get("youtube.clientSecret", ""), 40);
    private final JSpinner publishHourSpinner = new JSpinner(
            new SpinnerNumberModel(parseHour(cfg().get("youtube.publishHour", "9")), 0, 23, 1));
    private final JComboBox<String> privacyCombo =
            new JComboBox<>(new String[] {"public", "private", "unlisted"});
    { privacyCombo.setSelectedItem(cfg().get("youtube.privacyStatus", "public")); }
    private final JLabel autoStatus = new JLabel("");
    private JComboBox<String> rondaBox = new JComboBox<>();

    public MainFrame(Config cfg) {
        this.cfg = cfg;
        setTitle("Construyendo Valores Studio");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Guion", buildScriptPanel());
        tabs.addTab("Voz", buildVoicePanel());
        tabs.addTab("Render", buildRenderPanel());
        tabs.addTab("Rondas", buildRondasPanel());
        tabs.addTab("Contenido", buildContentPanel());
        tabs.addTab("Canal", buildChannelPanel());
        tabs.addTab("Automatización", buildAutomationPanel());
        tabs.addTab("Config", buildConfigPanel());
        add(tabs, BorderLayout.CENTER);
        add(buildLog(), BorderLayout.SOUTH);

        pack();
        setSize(1000, 720);
        setLocationRelativeTo(null);

        SwingWorker<List<String>, Void> voices = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                return Voices.spanishVoices(cfg);
            }
        };
        voices.execute();
        new Thread(() -> {
            try {
                List<String> vs = voices.get();
                javax.swing.SwingUtilities.invokeLater(() -> fillVoices(vs));
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }).start();
    }

    private Config cfg() {
        if (cfg != null) return cfg;
        return new Config();
    }

    private JPanel buildScriptPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        top.add(new JLabel("Tema / idea:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        top.add(topicField, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        top.add(new JLabel("Valor a enseñar:"), g);
        g.gridx = 1;
        top.add(valueField, g);

        g.gridx = 0; g.gridy = 2;
        top.add(new JLabel("Escenas:"), g);
        g.gridx = 1;
        top.add(scenesSpinner, g);

        g.gridx = 0; g.gridy = 3;
        top.add(new JLabel("Escenario:"), g);
        g.gridx = 1;
        g.fill = GridBagConstraints.NONE;
        scenarioBox.setSelectedItem(Scenario.fromId(cfg.get("video.scenario", "obra")));
        top.add(scenarioBox, g);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton gen = new JButton("Generar guion con IA");
        gen.addActionListener(e -> generateScript());
        JButton example = new JButton("Guion de ejemplo");
        example.addActionListener(e -> scriptArea.setText(Pipeline.exampleJson()));
        row.add(gen);
        row.add(example);
        g.gridx = 0; g.gridy = 4; g.gridwidth = 2;
        top.add(row, g);

        p.add(top, BorderLayout.NORTH);

        scriptArea.setLineWrap(true);
        scriptArea.setWrapStyleWord(true);
        p.add(new JScrollPane(scriptArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton parse = new JButton("Revisar escenas detectadas");
        parse.addActionListener(e -> previewScenes());
        bottom.add(parse);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildVoicePanel() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        grid.add(new JLabel("Personaje"), g);
        g.gridx = 1;
        grid.add(new JLabel("Voz"), g);
        g.gridx = 2;
        grid.add(new JLabel("Tono (Hz)"), g);
        g.gridx = 3;
        grid.add(new JLabel("Velocidad (%)"), g);

        int y = 1;
        for (String character : Config.CHARACTER_VOICES.keySet()) {
            g.gridx = 0; g.gridy = y;
            grid.add(new JLabel(character + ":  "), g);
            g.gridx = 1;
            JComboBox<String> box = new JComboBox<>();
            box.setEditable(true);
            box.setSelectedItem(cfg.voiceFor(character));
            voiceBoxes.put(character, box);
            box.setPreferredSize(new Dimension(300, 26));
            grid.add(box, g);

            g.gridx = 2;
            JSpinner pitch = new JSpinner(new SpinnerNumberModel(parseHz(cfg.voicePitchFor(character)), -100, 120, 2));
            pitch.setPreferredSize(new Dimension(90, 26));
            voicePitchSpinners.put(character, pitch);
            grid.add(pitch, g);

            g.gridx = 3;
            JSpinner rate = new JSpinner(new SpinnerNumberModel(parseRate(cfg.voiceRateFor(character)), -60, 60, 2));
            rate.setPreferredSize(new Dimension(90, 26));
            voiceRateSpinners.put(character, rate);
            grid.add(rate, g);
            y++;
        }
        p.add(grid, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton save = new JButton("Guardar voces elegidas");
        save.addActionListener(e -> {
            for (Map.Entry<String, JComboBox<String>> en : voiceBoxes.entrySet()) {
                Object sel = en.getValue().getSelectedItem();
                if (sel != null) cfg.set("voice." + en.getKey(), sel.toString());
            }
            for (Map.Entry<String, JSpinner> en : voicePitchSpinners.entrySet()) {
                cfg.set("voice.pitch." + en.getKey(), en.getValue().getValue() + "Hz");
            }
            for (Map.Entry<String, JSpinner> en : voiceRateSpinners.entrySet()) {
                cfg.set("voice.rate." + en.getKey(), en.getValue().getValue() + "%");
            }
            cfg.save();
            log("Voces guardadas. Los tonos altos y velocidades + suenan más infantiles.");
        });
        bottom.add(save);
        JButton test = new JButton("Probar voz (Casquito)");
        test.addActionListener(e -> testVoice());
        bottom.add(test);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildRenderPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Carpeta de salida:"));
        top.add(outField);
        JButton browse = new JButton("...");
        browse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(new File(outField.getText()).getParentFile());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                outField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        top.add(browse);
        p.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton run = new JButton("Generar video completo");
        run.addActionListener(e -> new Thread(() -> render()).start());
        JButton demo = new JButton("Video con guion de ejemplo");
        demo.addActionListener(e -> new Thread(() -> renderExample()).start());
        JButton shorts = new JButton("Generar Short (30-45 s) de cada video");
        shorts.addActionListener(e -> runTask(() -> {
            Path d = Paths.get(outField.getText());
            if (!Files.isDirectory(d)) throw new IllegalStateException("Indica primero la carpeta de salida");
            int n = 0;
            try (var stream = Files.list(d)) {
                for (Path v : stream.filter(f -> f.toString().toLowerCase().endsWith(".mp4"))
                        .filter(f -> !f.getFileName().toString().toLowerCase().contains("short"))
                        .sorted().toList()) {
                    Path s = Shorts.make(cfg, v, d, 38, this::log);
                    log("SHORT | " + s);
                    n++;
                }
            }
            log(n == 0 ? "No había videos (sin 'Short' en el nombre) en: " + d : "Shorts listos: " + n);
        }));
        center.add(run);
        center.add(demo);
        center.add(shorts);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRondasPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String info = "<html><h3>Rondas infantiles karaoke</h3>"
                + "Clásicas (dominio público) con <b>melodía sintetizada</b>, personajes bailando "
                + "al ritmo y la letra en pantalla. <b>Sin IA ni coste</b>.<br>"
                + "La rotación diaria alterna automáticamente: ronda un día, historia al siguiente."
                + "<br>Las melodías se editan en <b>RondaLibrary.java</b>.</html>";
        p.add(new JLabel(info), BorderLayout.NORTH);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rondaBox = new JComboBox<>();
        for (Ronda r : RondaLibrary.clasicas()) {
            rondaBox.addItem(r.titulo());
        }
        center.add(new JLabel("Ronda:"));
        center.add(rondaBox);
        JButton gen = new JButton("Generar ronda (video)");
        gen.addActionListener(e -> runTask(() -> {
            Ronda r = RondaLibrary.byIdOrIndex(rondaBox.getSelectedItem().toString());
            Path out = Paths.get(outField.getText());
            RondaVideo.build(cfg, r, out, this::log);
            log("Ronda lista: " + out.resolve(r.titulo() + ".mp4").toAbsolutePath());
            javax.swing.SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Ronda generada correctamente.", "Listo", JOptionPane.INFORMATION_MESSAGE));
        }));
        JButton plan = new JButton("Guardar plan RONDAS (csv/md)");
        plan.addActionListener(e -> runTask(() -> {
            Path csv = RondaLibrary.writePlan(Paths.get(outField.getText()));
            log("Plan de rondas: " + csv.toAbsolutePath());
        }));
        center.add(gen);
        center.add(plan);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildContentPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String info = "<html><h3>Contenido del canal con IA</h3>"
                + "Genera los <b>30 títulos y descripciones</b> del plan del canal "
                + "(10 valores x 3 formatos).<br>Se guarda <b>CONTENIDO_30.md</b> en la carpeta de salida. "
                + "Luego edita y adapta cada idea a tu estilo.</html>";
        JLabel lbl = new JLabel(info);
        p.add(lbl, BorderLayout.NORTH);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton gen = new JButton("Generar 30 títulos + descripciones (IA)");
        gen.addActionListener(e -> {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            new Thread(() -> {
                try {
                    Path out = Paths.get(outField.getText());
                    Path md = ContentGenerator.generate(cfg, out, this::log);
                    log("Contenido generado: " + md.toAbsolutePath());
                    javax.swing.SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Títulos y descripciones listos.\nCarpeta: " + out.toAbsolutePath(),
                            "Contenido", JOptionPane.INFORMATION_MESSAGE));
                } catch (Exception ex) {
                    log("Error generando contenido: " + ex.getMessage());
                } finally {
                    javax.swing.SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
                }
            }).start();
        });
        center.add(gen);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildChannelPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String info = "<html><h3>Identidad del canal</h3>"
                + "Genera el <b>avatar</b> (logo, 800x800) y el <b>banner</b> (2560x1440) "
                + "con el mismo estilo de los videos.<br>Se guardan en la carpeta de salida "
                + "(avatar_canal.png y banner_canal.png).</html>";
        p.add(new JLabel(info), BorderLayout.NORTH);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton avatar = new JButton("Generar avatar");
        avatar.addActionListener(e -> {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            new Thread(() -> {
                try {
                    Path out = Paths.get(outField.getText());
                    ChannelArt.makeAvatar(out);
                    log("Avatar: " + out.resolve("avatar_canal.png").toAbsolutePath());
                } catch (Exception ex) {
                    log("Error avatar: " + ex.getMessage());
                } finally {
                    javax.swing.SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
                }
            }).start();
        });
        JButton banner = new JButton("Generar banner");
        banner.addActionListener(e -> {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            new Thread(() -> {
                try {
                    Path out = Paths.get(outField.getText());
                    ChannelArt.makeBanner(out, null);
                    log("Banner: " + out.resolve("banner_canal.png").toAbsolutePath());
                } catch (Exception ex) {
                    log("Error banner: " + ex.getMessage());
                } finally {
                    javax.swing.SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
                }
            }).start();
        });
        center.add(avatar);
        center.add(banner);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildConfigPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        p.add(new JLabel("API key de Gemini (gratis):"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        p.add(keyField, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        p.add(new JLabel("Modelo Gemini:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        p.add(modelField, g);

        g.gridx = 0; g.gridy = 2; g.weightx = 0;
        p.add(new JLabel("Comando Python:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        p.add(pythonField, g);

        g.gridx = 0; g.gridy = 3; g.weightx = 0;
        p.add(new JLabel("Ruta ffmpeg:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        p.add(ffmpegField, g);

        g.gridx = 0; g.gridy = 4; g.weightx = 0;
        p.add(new JLabel("Música de fondo (opcional):"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        p.add(musicField, g);

        g.gridx = 0; g.gridy = 5; g.weightx = 0;
        p.add(new JLabel("YouTube Client ID (OAuth):"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        p.add(clientIdField, g);

        g.gridx = 0; g.gridy = 6; g.weightx = 0;
        p.add(new JLabel("YouTube Client Secret:"), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        p.add(clientSecretField, g);

        g.gridx = 0; g.gridy = 7; g.gridwidth = 2;
        JButton save = new JButton("Guardar configuración");
        save.addActionListener(e -> {
            cfg.set("gemini.apiKey", keyField.getText().trim());
            cfg.set("gemini.model", modelField.getText().trim());
            cfg.set("python.cmd", pythonField.getText().trim());
            cfg.set("ffmpeg.path", ffmpegField.getText().trim());
            cfg.set("music.path", musicField.getText().trim());
            cfg.set("youtube.clientId", clientIdField.getText().trim());
            cfg.set("youtube.clientSecret", clientSecretField.getText().trim());
            cfg.set("youtube.publishHour", publishHourSpinner.getValue().toString());
            cfg.save();
            log("Configuración guardada.");
        });
        p.add(save, g);

        g.gridx = 0; g.gridy = 8; g.gridwidth = 2;
        JLabel info = new JLabel("<html>Obtén tu key gratis en <b>aistudio.google.com</b> → Get API key.<br>"
                + "Prefiere el modelo <b>gemini-2.5-flash</b> si tu key lo permite.<br>"
                + "Client ID/Secret: paso a paso en <b>GUIA_GOOGLE_CLOUD.md</b>.</html>");
        p.add(info, g);
        return p;
    }

    private JPanel buildAutomationPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String info = "<html><h3>Publica un video al día en tu canal (gratis)</h3>"
                + "Paso 1: sigue <b>GUIA_GOOGLE_CLOUD.md</b> para crear el proyecto y pega los IDs en "
                + "'Configuración'.<br>"
                + "Paso 2: pulsa <b>Conectar YouTube</b> (abre el navegador una sola vez).<br>"
                + "Paso 3: cada día la app toma el siguiente video del plan de 30, lo crea y lo sube "
                + "para publicarse a la <b>hora elegida</b>. La Tarea de Windows la ejecuta aunque la PC "
                + "esté apagada al momento de publicarse.</html>";
        p.add(new JLabel(info), BorderLayout.NORTH);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton connect = new JButton("Conectar YouTube");
        connect.addActionListener(e -> runTask(() -> {
            Youtube.authorize(cfg, this::log);
            log("Canal: " + Youtube.channelName(cfg));
            refreshAutoStatus();
        }));
        JButton test = new JButton("Probar conexión");
        test.addActionListener(e -> runTask(() -> log("Canal conectado: " + Youtube.channelName(cfg))));
        JButton upload = new JButton("Subir el video de hoy ahora");
        upload.addActionListener(e -> runTask(() -> {
            cfg.set("youtube.publishHour", publishHourSpinner.getValue().toString());
            cfg.set("youtube.privacyStatus", (String) privacyCombo.getSelectedItem());
            DailyVideo.Result r = DailyVideo.run(cfg, Paths.get(outField.getText()), this::log);
            log(r.message());
            refreshAutoStatus();
        }));
        JButton publi = new JButton("Hacer público el último video");
        publi.addActionListener(e -> runTask(() -> {
            String id = cfg.get("canal.lastVideoId", "");
            if (id.isBlank()) {
                log("No hay ningún video subido por la app todavía.");
                return;
            }
            String applied = Youtube.setPublic(cfg, id);
            log("PUBLICADO: https://youtu.be/" + id + " -> " + applied);
            refreshAutoStatus();
        }));
        center.add(connect);
        center.add(test);
        center.add(upload);
        center.add(publi);
        p.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JLabel("Hora de publicación:"));
        bottom.add(publishHourSpinner);
        bottom.add(new JLabel("Privacidad:"));
        bottom.add(privacyCombo);
        bottom.add(new JLabel("(el video se crea antes y se publica a esa hora)"));
        p.add(bottom, BorderLayout.SOUTH);

        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT));
        status.add(autoStatus);
        p.add(status, BorderLayout.WEST);

        refreshAutoStatus();
        return p;
    }

    private void refreshAutoStatus() {
        int idx;
        try {
            idx = Integer.parseInt(cfg.get("canal.videoIndex", "1"));
        } catch (NumberFormatException e) {
            idx = 1;
        }
        int vc;
        try {
            vc = Integer.parseInt(cfg.get("canal.videoCount", "1"));
        } catch (NumberFormatException e) {
            vc = 1;
        }
        int rondaN = RondaLibrary.nextRondaIndex(cfg);
        String last = cfg.get("canal.lastUploadDate", "nunca");
        String hoy = java.time.LocalDate.now().toString();
        boolean done = hoy.equals(cfg.get("canal.lastUploadDate", ""));
        String sig = vc % 2 == 1
                ? "RONDA " + rondaN + "/" + RondaLibrary.clasicas().size()
                : "Historia " + ((idx - 1) % 30 + 1) + "/30";
        autoStatus.setText("Publicados: " + vc + " · Siguiente: " + sig
                + " · Última subida: " + last + " · Hoy: " + (done ? "publicado" : "pendiente"));
    }

    private void runTask(ThrowingRunnable task) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            try {
                task.run();
            } catch (Exception ex) {
                log("Error: " + ex.getMessage());
            } finally {
                javax.swing.SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
            }
        }).start();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private JScrollPane buildLog() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setPreferredSize(new Dimension(900, 150));
        return new JScrollPane(logArea);
    }

    private void fillVoices(List<String> voices) {
        for (JComboBox<String> box : voiceBoxes.values()) {
            for (String v : voices) {
                if (!containsItem(box, v)) box.addItem(v);
            }
        }
    }

    private boolean containsItem(JComboBox<String> box, String value) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (value.equals(box.getItemAt(i))) return true;
        }
        return false;
    }

    private void generateScript() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            try {
                String json = Gemini.generateScript(cfg, topicField.getText(),
                        valueField.getText(), (Integer) scenesSpinner.getValue());
                String clean = GeminiParser.extractJson(json);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    scriptArea.setText(clean);
                    log("Guion generado por IA. Revisa el JSON y edítalo si quieres.");
                });
            } catch (Exception ex) {
                log("Error al generar: " + ex.getMessage());
            } finally {
                javax.swing.SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
            }
        }).start();
    }

    private void previewScenes() {
        try {
            ScriptParser.Script s = ScriptParser.parse(scriptArea.getText());
            StringBuilder sb = new StringBuilder("Título: " + s.title() + "\n");
            for (Scene sc : s.scenes()) {
                sb.append(sc.index).append(". [").append(sc.character).append("] ").append(sc.narration).append("\n");
            }
            log(sb.toString());
        } catch (IllegalArgumentException ex) {
            log("No se pudo interpretar el guion: " + ex.getMessage());
        }
    }

    private void testVoice() {
        new Thread(() -> {
            try {
                java.io.File tmp = java.io.File.createTempFile("voz_prueba", ".mp3");
                Tts.synth(cfg, "Casquito", cfg.voiceFor("Casquito"),
                        "Hola, pequeños constructores. ¡Hoy aprenderemos un valor!", Paths.get(tmp.getAbsolutePath()));
                log("Voz generada OK (prueba): " + tmp);
            } catch (Exception ex) {
                log("Error de voz: " + ex.getMessage());
            }
        }).start();
    }

    private void render() {
        saveScenario();
        doRender(() -> ScriptParser.parse(scriptArea.getText()));
    }

    private void renderExample() {
        saveScenario();
        doRender(Pipeline::example);
    }

    private void saveScenario() {
        Object sel = scenarioBox.getSelectedItem();
        if (sel instanceof Scenario sc) {
            cfg.set("video.scenario", sc.id);
        }
    }

    private void doRender(java.util.function.Supplier<ScriptParser.Script> sc) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            try {
                ScriptParser.Script s = sc.get();
                java.nio.file.Path out = Paths.get(outField.getText());
                Pipeline.exec(cfg, s, out, this::log);
                log("¡Video listo! Revísalo en: " + out.toAbsolutePath());
                javax.swing.SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Video generado correctamente.",
                                "Listo", JOptionPane.INFORMATION_MESSAGE));
            } catch (Exception ex) {
                log("Error: " + ex.getMessage());
                String detail = ex instanceof RuntimeException && ex.getCause() != null
                        ? ex.getCause().getMessage() : "";
                javax.swing.SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Error generando video:\n" + ex.getMessage() + "\n" + detail,
                                "Error", JOptionPane.ERROR_MESSAGE));
            } finally {
                javax.swing.SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
            }
        }).start();
    }

    private void log(String txt) {
        javax.swing.SwingUtilities.invokeLater(() ->
                logArea.append("> " + txt + "\n"));
    }
}