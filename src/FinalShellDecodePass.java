import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

public class FinalShellDecodePass extends JFrame {
    private static final int COL_NAME = 0;
    private static final int COL_HOST = 1;
    private static final int COL_PORT = 2;
    private static final int COL_USER = 3;
    private static final int COL_AUTH_MODE = 4;
    private static final int COL_CREDENTIAL = 5;

    private static final String TEXT_TITLE = "FinalShell \u6279\u91cf\u89e3\u6790\u5de5\u5177";
    private static final String TEXT_READY = "\u5c31\u7eea";
    private static final String TEXT_COPIED = "\u5df2\u590d\u5236";
    private static final String TEXT_CLICK_COPY = "\u70b9\u51fb\u590d\u5236";
    private static final String TEXT_PRIVATE_KEY_CLICK_COPY = "\u79c1\u94a5\uff08\u70b9\u51fb\u590d\u5236\uff09";

    private final JTextField pathField = new JTextField();
    private final JTextArea detailArea = new JTextArea();
    private final JLabel statusLabel = new JLabel(TEXT_READY);
    private final CredentialTableModel tableModel = new CredentialTableModel();
    private final JTable table = new JTable(tableModel) {
        @Override
        public String getToolTipText(MouseEvent e) {
            int viewRow = rowAtPoint(e.getPoint());
            int viewCol = columnAtPoint(e.getPoint());
            if (viewRow < 0 || viewCol < 0) {
                return null;
            }
            int modelCol = convertColumnIndexToModel(viewCol);
            if (isCopyableColumn(modelCol)) {
                return TEXT_CLICK_COPY;
            }
            return null;
        }
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FinalShellDecodePass app = new FinalShellDecodePass();
            app.setVisible(true);
        });
    }

    public FinalShellDecodePass() {
        super(TEXT_TITLE);
        initUi();
        autoDetectInstallDir();
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1220, 780);
        setLocationRelativeTo(null);

        JPanel pathPanel = new JPanel(new BorderLayout(8, 8));
        pathPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        pathPanel.add(new JLabel("FinalShell \u8def\u5f84:"), BorderLayout.WEST);
        pathPanel.add(pathField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton detectButton = new JButton("\u81ea\u52a8\u8bc6\u522b");
        JButton browseButton = new JButton("\u624b\u52a8\u9009\u62e9");
        JButton parseButton = new JButton("\u5f00\u59cb\u89e3\u6790");
        buttonPanel.add(detectButton);
        buttonPanel.add(browseButton);
        buttonPanel.add(parseButton);
        pathPanel.add(buttonPanel, BorderLayout.EAST);
        add(pathPanel, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(24);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                    handleTableCopyClick(e.getPoint());
                }
            }
        });
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateTableCursor(e.getPoint());
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);

        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setPreferredSize(new Dimension(1000, 280));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, detailScroll);
        splitPane.setResizeWeight(0.58);
        add(splitPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 8, 8));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        add(statusPanel, BorderLayout.SOUTH);

        detectButton.addActionListener(e -> autoDetectInstallDir());
        browseButton.addActionListener(e -> chooseDir());
        parseButton.addActionListener(e -> parseAll());
    }

    private void handleTableCopyClick(Point point) {
        int viewRow = table.rowAtPoint(point);
        int viewCol = table.columnAtPoint(point);
        if (viewRow < 0 || viewCol < 0) {
            return;
        }

        int modelCol = table.convertColumnIndexToModel(viewCol);
        if (!isCopyableColumn(modelCol)) {
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        ServerCredential row = tableModel.getRow(modelRow);
        if (row == null) {
            return;
        }

        String copyValue = valueForCopy(row, modelCol);
        if (!hasText(copyValue)) {
            statusLabel.setText("\u8be5\u5b57\u6bb5\u6ca1\u6709\u53ef\u590d\u5236\u5185\u5bb9");
            return;
        }

        table.setRowSelectionInterval(viewRow, viewRow);
        copyToClipboard(copyValue);

        String copiedField = tableModel.getColumnName(modelCol);
        if (modelCol == COL_CREDENTIAL && isKeyCredential(row)) {
            statusLabel.setText(TEXT_COPIED + " \u767b\u5f55\u51ed\u636e\uff08\u79c1\u94a5\uff09");
        } else {
            statusLabel.setText(TEXT_COPIED + " " + copiedField + ": " + preview(copyValue));
        }

        showDetailForRow(row, copiedField);
    }

    private void updateTableCursor(Point point) {
        int viewCol = table.columnAtPoint(point);
        if (viewCol < 0) {
            table.setCursor(Cursor.getDefaultCursor());
            return;
        }

        int modelCol = table.convertColumnIndexToModel(viewCol);
        if (isCopyableColumn(modelCol)) {
            table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            table.setCursor(Cursor.getDefaultCursor());
        }
    }

    private static String preview(String value) {
        String flat = value.replace('\r', ' ').replace('\n', ' ').trim();
        if (flat.length() <= 26) {
            return flat;
        }
        return flat.substring(0, 26) + "...";
    }

    private static void copyToClipboard(String value) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
    }

    private boolean isCopyableColumn(int modelCol) {
        return modelCol == COL_NAME
            || modelCol == COL_HOST
            || modelCol == COL_PORT
            || modelCol == COL_USER
            || modelCol == COL_CREDENTIAL;
    }

    private String valueForCopy(ServerCredential row, int modelCol) {
        switch (modelCol) {
            case COL_NAME:
                return hasText(row.name) ? row.name : row.fileName;
            case COL_HOST:
                return safe(row.host);
            case COL_PORT:
                return row.port > 0 ? String.valueOf(row.port) : "";
            case COL_USER:
                return safe(row.userName);
            case COL_CREDENTIAL:
                return buildCredentialCopyValue(row);
            default:
                return "";
        }
    }

    private void showDetailForRow(ServerCredential row, String copiedField) {
        StringBuilder sb = new StringBuilder();
        sb.append("\u672c\u6b21\u590d\u5236\u5b57\u6bb5: ").append(copiedField).append('\n');
        sb.append('\n');

        sb.append("\u6587\u4ef6\u540d: ").append(safe(row.fileName)).append('\n');
        sb.append("\u8fde\u63a5\u540d\u79f0: ").append(safe(row.name)).append('\n');
        sb.append("\u670d\u52a1\u5668ID: ").append(safe(row.id)).append('\n');
        sb.append("\u4e3b\u673a: ").append(safe(row.host)).append(':').append(row.port).append('\n');
        sb.append("\u7528\u6237\u540d: ").append(safe(row.userName)).append('\n');
        sb.append("\u8ba4\u8bc1\u65b9\u5f0f: ").append(safe(row.authMode))
            .append(" (authentication_type=").append(row.authenticationType).append(")\n");
        sb.append("secret_key_id: ").append(safe(row.secretKeyId)).append("\n\n");

        sb.append("\u767b\u5f55\u51ed\u636e\u5c55\u793a\u503c:\n").append(safe(row.credentialDisplay)).append("\n\n");
        sb.append("\u53ef\u590d\u5236\u7684\u767b\u5f55\u51ed\u636e\u5b9e\u9645\u503c:\n").append(safe(buildCredentialCopyValue(row))).append("\n\n");

        sb.append("\u52a0\u5bc6\u5bc6\u7801:\n").append(safe(row.encryptedPassword)).append("\n\n");
        sb.append("\u89e3\u5bc6\u5bc6\u7801:\n").append(safe(row.decodedPassword)).append("\n\n");

        sb.append("\u52a0\u5bc6\u79c1\u94a5(key_data):\n").append(safe(row.encryptedPrivateKey)).append("\n\n");
        sb.append("\u89e3\u7801\u79c1\u94a5:\n").append(safe(row.decodedPrivateKey)).append("\n");

        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    private void autoDetectInstallDir() {
        Path detected = detectFinalShellPath();
        if (detected != null) {
            pathField.setText(detected.toString());
            statusLabel.setText("\u5df2\u81ea\u52a8\u8bc6\u522b\u8def\u5f84: " + detected);
        } else {
            statusLabel.setText("\u672a\u81ea\u52a8\u8bc6\u522b\u5230\u8def\u5f84\uff0c\u8bf7\u624b\u52a8\u9009\u62e9 FinalShell \u5b89\u88c5\u76ee\u5f55");
        }
    }

    private Path detectFinalShellPath() {
        List<Path> candidates = new ArrayList<>();

        String userHome = System.getProperty("user.home");
        if (hasText(userHome)) {
            candidates.add(Paths.get(userHome, "AppData", "Local", "finalshell"));
        }

        String userProfile = System.getenv("USERPROFILE");
        if (hasText(userProfile)) {
            candidates.add(Paths.get(userProfile, "AppData", "Local", "finalshell"));
        }

        Path usersDir = Paths.get("C:", "Users");
        if (Files.isDirectory(usersDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(usersDir)) {
                for (Path userDir : stream) {
                    candidates.add(userDir.resolve(Paths.get("AppData", "Local", "finalshell")));
                }
            } catch (Exception ignored) {
            }
        }

        for (Path candidate : candidates) {
            if (isFinalShellDir(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isFinalShellDir(Path baseDir) {
        if (baseDir == null || !Files.isDirectory(baseDir)) {
            return false;
        }
        return Files.isDirectory(baseDir.resolve("conn"));
    }

    private void chooseDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("\u9009\u62e9 FinalShell \u5b89\u88c5\u76ee\u5f55\uff08\u76ee\u5f55\u4e0b\u9700\u5305\u542b conn\uff09");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        String current = pathField.getText().trim();
        if (!current.isEmpty()) {
            Path path = Paths.get(current);
            if (Files.isDirectory(path)) {
                chooser.setCurrentDirectory(path.toFile());
            }
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void parseAll() {
        String pathText = pathField.getText().trim();
        if (pathText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "\u8bf7\u5148\u9009\u62e9 FinalShell \u5b89\u88c5\u76ee\u5f55", "\u63d0\u793a", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path baseDir = Paths.get(pathText);
        if (!isFinalShellDir(baseDir)) {
            JOptionPane.showMessageDialog(this, "\u76ee\u5f55\u4e0d\u6b63\u786e\uff1a\u672a\u627e\u5230 conn \u6587\u4ef6\u5939", "\u9519\u8bef", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Path connDir = baseDir.resolve("conn");
            Path configPath = baseDir.resolve("config.json");

            Map<String, String> keyDataMap = loadKeyDataMap(configPath);
            List<ServerCredential> credentials = loadCredentials(connDir, keyDataMap);

            tableModel.setRows(credentials);
            detailArea.setText("");
            statusLabel.setText("\u89e3\u6790\u5b8c\u6210\uff1a\u5171 " + credentials.size() + " \u6761\u670d\u52a1\u5668\u8bb0\u5f55");
            if (!Files.exists(configPath)) {
                statusLabel.setText(statusLabel.getText() + "\uff08\u672a\u627e\u5230 config.json\uff0c\u79c1\u94a5\u65e0\u6cd5\u89e3\u6790\uff09");
            }
        } catch (Exception ex) {
            statusLabel.setText("\u89e3\u6790\u5931\u8d25: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "\u89e3\u6790\u5931\u8d25: " + ex.getMessage(), "\u9519\u8bef", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Map<String, String> loadKeyDataMap(Path configPath) {
        if (!Files.exists(configPath) || !Files.isRegularFile(configPath)) {
            return Collections.emptyMap();
        }

        Map<String, String> keyDataMap = new HashMap<>();
        try {
            String content = Files.readString(configPath, StandardCharsets.UTF_8);

            Pattern idThenKey = Pattern.compile(
                "\\{[^\\{\\}]*?\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[^\\{\\}]*?\\\"key_data\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"[^\\{\\}]*?\\}",
                Pattern.DOTALL
            );
            Pattern keyThenId = Pattern.compile(
                "\\{[^\\{\\}]*?\\\"key_data\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"[^\\{\\}]*?\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[^\\{\\}]*?\\}",
                Pattern.DOTALL
            );

            Matcher m1 = idThenKey.matcher(content);
            while (m1.find()) {
                String id = unescapeJson(m1.group(1));
                String keyData = unescapeJson(m1.group(2));
                if (hasText(id) && hasText(keyData)) {
                    keyDataMap.putIfAbsent(id, keyData);
                }
            }

            Matcher m2 = keyThenId.matcher(content);
            while (m2.find()) {
                String keyData = unescapeJson(m2.group(1));
                String id = unescapeJson(m2.group(2));
                if (hasText(id) && hasText(keyData)) {
                    keyDataMap.putIfAbsent(id, keyData);
                }
            }
        } catch (Exception ex) {
            statusLabel.setText("\u8bfb\u53d6 config.json \u5931\u8d25: " + ex.getMessage());
        }

        return keyDataMap;
    }

    private List<ServerCredential> loadCredentials(Path connDir, Map<String, String> keyDataMap) throws Exception {
        List<Path> connFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(connDir, "*_connect_config.json")) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    connFiles.add(file);
                }
            }
        }

        connFiles.sort(Comparator.comparing(path -> path.getFileName().toString()));

        List<ServerCredential> result = new ArrayList<>();
        for (Path file : connFiles) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            result.add(parseConnFile(file.getFileName().toString(), content, keyDataMap));
        }
        return result;
    }

    private ServerCredential parseConnFile(String fileName, String json, Map<String, String> keyDataMap) {
        ServerCredential row = new ServerCredential();
        row.fileName = fileName;
        row.id = jsonString(json, "id");
        row.name = jsonString(json, "name");
        row.host = jsonString(json, "host");
        row.port = jsonInt(json, "port", -1);
        row.userName = jsonString(json, "user_name");
        row.authenticationType = jsonInt(json, "authentication_type", -1);
        row.encryptedPassword = jsonString(json, "password");
        row.secretKeyId = jsonString(json, "secret_key_id");

        if (hasText(row.encryptedPassword)) {
            try {
                row.decodedPassword = decodePass(row.encryptedPassword);
            } catch (Exception ex) {
                row.decodedPassword = "[\u5bc6\u7801\u89e3\u5bc6\u5931\u8d25] " + ex.getMessage();
            }
        }

        if (hasText(row.secretKeyId)) {
            row.encryptedPrivateKey = keyDataMap.get(row.secretKeyId);
            if (hasText(row.encryptedPrivateKey)) {
                try {
                    row.decodedPrivateKey = decodePrivateKey(row.encryptedPrivateKey);
                } catch (Exception ex) {
                    row.decodedPrivateKey = "[\u79c1\u94a5\u89e3\u7801\u5931\u8d25] " + ex.getMessage();
                }
            }
        }

        row.authMode = buildAuthMode(row);
        row.credentialDisplay = buildCredentialDisplay(row);
        return row;
    }

    private static String buildAuthMode(ServerCredential row) {
        boolean hasPassword = hasText(row.encryptedPassword);
        boolean hasKey = hasText(row.secretKeyId);

        if (hasPassword && hasKey) {
            return "\u5bc6\u7801 + \u79c1\u94a5";
        }
        if (hasPassword) {
            return "\u8d26\u53f7\u5bc6\u7801";
        }
        if (hasKey) {
            return "\u516c\u79c1\u94a5";
        }
        return "\u672a\u77e5";
    }

    private static boolean isKeyCredential(ServerCredential row) {
        return !hasText(row.encryptedPassword) && hasText(row.secretKeyId);
    }

    private static String buildCredentialDisplay(ServerCredential row) {
        if (hasText(row.encryptedPassword)) {
            if (hasText(row.decodedPassword)) {
                return row.decodedPassword;
            }
            return "[\u5bc6\u7801\u89e3\u5bc6\u5931\u8d25]";
        }
        if (hasText(row.secretKeyId)) {
            return TEXT_PRIVATE_KEY_CLICK_COPY;
        }
        return "";
    }

    private static String buildCredentialCopyValue(ServerCredential row) {
        if (hasText(row.encryptedPassword)) {
            if (hasText(row.decodedPassword)) {
                return row.decodedPassword;
            }
            return safe(row.encryptedPassword);
        }

        if (hasText(row.secretKeyId)) {
            if (hasText(row.decodedPrivateKey)) {
                return row.decodedPrivateKey;
            }
            return safe(row.encryptedPrivateKey);
        }

        return "";
    }

    private static String decodePrivateKey(String keyData) {
        byte[] bytes = Base64.getDecoder().decode(keyData);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String jsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\\\"])*)\\\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return "";
        }
        return unescapeJson(matcher.group(1));
    }

    private static int jsonInt(String json, String key, int defaultValue) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static String unescapeJson(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException ex) {
                                sb.append("\\u").append(hex);
                                i += 4;
                            }
                        } else {
                            sb.append("\\u");
                        }
                        break;
                    default:
                        sb.append(n);
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static byte[] desDecode(byte[] data, byte[] head) throws Exception {
        SecureRandom sr = new SecureRandom();
        DESKeySpec dks = new DESKeySpec(head);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey securekey = keyFactory.generateSecret(dks);
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.DECRYPT_MODE, securekey, sr);
        return cipher.doFinal(data);
    }

    public static String decodePass(String data) throws Exception {
        if (data == null) {
            return null;
        }

        byte[] buf = Base64.getDecoder().decode(data);
        byte[] head = new byte[8];
        System.arraycopy(buf, 0, head, 0, head.length);
        byte[] d = new byte[buf.length - head.length];
        System.arraycopy(buf, head.length, d, 0, d.length);
        byte[] bt = desDecode(d, ranDomKey(head));
        return new String(bt, StandardCharsets.UTF_8);
    }

    static byte[] ranDomKey(byte[] head) {
        long ks = 3680984568597093857L / (long) (new Random((long) head[5]).nextInt(127));
        Random random = new Random(ks);
        int t = head[0];

        for (int i = 0; i < t; ++i) {
            random.nextLong();
        }

        long n = random.nextLong();
        Random r2 = new Random(n);
        long[] ld = new long[] {
            (long) head[4],
            r2.nextLong(),
            (long) head[7],
            (long) head[3],
            r2.nextLong(),
            (long) head[1],
            random.nextLong(),
            (long) head[2]
        };

        byte[] keyData = new byte[ld.length * Long.BYTES];
        int offset = 0;
        for (long value : ld) {
            for (int i = Long.BYTES - 1; i >= 0; i--) {
                keyData[offset + i] = (byte) (value & 0xFF);
                value >>>= 8;
            }
            offset += Long.BYTES;
        }
        return md5(keyData);
    }

    public static byte[] md5(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    private static class ServerCredential {
        private String fileName;
        private String id;
        private String name;
        private String host;
        private int port;
        private String userName;
        private int authenticationType;

        private String encryptedPassword;
        private String decodedPassword;

        private String secretKeyId;
        private String encryptedPrivateKey;
        private String decodedPrivateKey;

        private String authMode;
        private String credentialDisplay;
    }

    private static class CredentialTableModel extends AbstractTableModel {
        private final String[] columns = {
            "\u540d\u79f0", "\u4e3b\u673a", "\u7aef\u53e3", "\u7528\u6237\u540d", "\u8ba4\u8bc1\u65b9\u5f0f", "\u767b\u5f55\u51ed\u636e"
        };

        private final List<ServerCredential> rows = new ArrayList<>();

        public void setRows(List<ServerCredential> newRows) {
            rows.clear();
            rows.addAll(newRows);
            fireTableDataChanged();
        }

        public ServerCredential getRow(int index) {
            if (index < 0 || index >= rows.size()) {
                return null;
            }
            return rows.get(index);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ServerCredential row = rows.get(rowIndex);
            switch (columnIndex) {
                case COL_NAME:
                    return hasText(row.name) ? row.name : row.fileName;
                case COL_HOST:
                    return row.host;
                case COL_PORT:
                    return row.port > 0 ? row.port : "";
                case COL_USER:
                    return row.userName;
                case COL_AUTH_MODE:
                    return row.authMode;
                case COL_CREDENTIAL:
                    return trimForTable(row.credentialDisplay);
                default:
                    return "";
            }
        }

        private String trimForTable(String text) {
            if (!hasText(text)) {
                return "";
            }
            String line = text.replace('\n', ' ').replace('\r', ' ');
            return line.length() > 52 ? line.substring(0, 52) + "..." : line;
        }
    }
}
