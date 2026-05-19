import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FoxSeniorApp - Versão com Prévia Visual em Tempo Real e Texto Multilinha
 */
public class FoxSeniorApp extends JFrame {

    // Cores e Estilos
    private final Color corRaposa = new Color(235, 110, 0);
    private final Color corFundo = new Color(248, 249, 250);
    private final Color corTexto = new Color(51, 51, 51);
    private final Font fontPadrao = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font fontNegrito = new Font("Segoe UI", Font.BOLD, 14);
    private final Font fontTitulo = new Font("Segoe UI", Font.BOLD, 18);

    private JComboBox<String> comboImpressoras;
    private JComboBox<String> comboTemplates;
    private JTextField txtQtd;
    private JPanel panelCamposDinamicos;
    private Map<String, JTextComponent> mapaInputs = new HashMap<>(); // Alterado para suportar JTextArea
    private JButton btnImprimir;
    
    // Componentes de Prévia
    private LabelPreviewPanel previewPanel;
    private Map<String, Point> mapaCoordenadas = new HashMap<>();
    private List<Rectangle> mapaBarras = new ArrayList<>();
    private List<Rectangle> mapaLinhas = new ArrayList<>();

    private final Map<String, String> DICIONARIO = new HashMap<>();

    public FoxSeniorApp() {
        inicializarDicionario();
        configurarJanela();
        construirInterface();
        scanTemplate();
    }

    private void inicializarDicionario() {
        DICIONARIO.put("#C", "Produto");
        DICIONARIO.put("#E", "Sabor");
        DICIONARIO.put("#D", "EAN/Barras");
        DICIONARIO.put("#R", "Lote");
        DICIONARIO.put("#I", "Turno");
        DICIONARIO.put("#Q", "Validade");
        DICIONARIO.put("#B", "Marca");
        DICIONARIO.put("#TEXTO", "Texto Livre"); // Novo campo multilinha
        DICIONARIO.put("#L1", "Linha Livre 1"); // Mantido por compatibilidade
        DICIONARIO.put("#L2", "Linha Livre 2");
        DICIONARIO.put("#L3", "Linha Livre 3");
        DICIONARIO.put("#L4", "Linha Livre 4");
        DICIONARIO.put("#L5", "Linha Livre 5");
    }

    private void configurarJanela() {
        setTitle("FoxSenior - Etiquetas");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(corFundo);
        setLayout(new BorderLayout());
    }

    private void construirInterface() {
        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(corRaposa);
        header.setPreferredSize(new Dimension(0, 70));
        header.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        JLabel lblTitulo = new JLabel("FOX SENIOR");
        lblTitulo.setFont(fontTitulo);
        lblTitulo.setForeground(Color.WHITE);
        header.add(lblTitulo, BorderLayout.WEST);

        JLabel lblSubtitulo = new JLabel("Gestão de Etiquetas com Prévia Visual");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblSubtitulo.setForeground(new Color(255, 255, 255, 200));
        header.add(lblSubtitulo, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);

        // --- DIVISÃO PRINCIPAL (Esquerda: Inputs | Direita: Prévia) ---
        JPanel panelPrincipal = new JPanel(new GridLayout(1, 2, 20, 0));
        panelPrincipal.setOpaque(false);
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // COLUNA ESQUERDA (Formulário)
        JPanel colEsquerda = new JPanel(new BorderLayout(15, 15));
        colEsquerda.setOpaque(false);

        JPanel panelConfig = new JPanel(new GridBagLayout());
        panelConfig.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.weightx = 1.0;

        gbc.gridy = 0;
        panelConfig.add(criarLabelEstilizada("Selecione a Impressora:"), gbc);
        gbc.gridy = 1;
        comboImpressoras = new JComboBox<>(listarImpressoras());
        estilizarComponente(comboImpressoras);
        selecionarImpressoraPadrao();
        panelConfig.add(comboImpressoras, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(15, 0, 5, 0);
        panelConfig.add(criarLabelEstilizada("Modelo de Etiqueta (.out):"), gbc);
        gbc.gridy = 3;
        gbc.insets = new Insets(5, 0, 5, 0);
        comboTemplates = new JComboBox<>(listarArquivosOut());
        estilizarComponente(comboTemplates);
        comboTemplates.addActionListener(e -> scanTemplate());
        panelConfig.add(comboTemplates, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(15, 0, 5, 0);
        panelConfig.add(criarLabelEstilizada("Quantidade de Cópias:"), gbc);
        gbc.gridy = 5;
        gbc.insets = new Insets(5, 0, 5, 0);
        txtQtd = new JTextField("1");
        estilizarComponente(txtQtd);
        panelConfig.add(txtQtd, gbc);

        colEsquerda.add(panelConfig, BorderLayout.NORTH);

        panelCamposDinamicos = new JPanel();
        panelCamposDinamicos.setBackground(Color.WHITE);
        panelCamposDinamicos.setLayout(new BoxLayout(panelCamposDinamicos, BoxLayout.Y_AXIS));
        panelCamposDinamicos.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(panelCamposDinamicos);
        scroll.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(200, 200, 200), 1), 
                " CAMPOS DO TEMPLATE ", TitledBorder.LEFT, TitledBorder.TOP, fontNegrito, corRaposa));
        scroll.getViewport().setBackground(Color.WHITE);
        colEsquerda.add(scroll, BorderLayout.CENTER);

        panelPrincipal.add(colEsquerda);

        // COLUNA DIREITA (Prévia)
        JPanel colDireita = new JPanel(new BorderLayout());
        colDireita.setOpaque(false);
        
        JLabel lblPreview = criarLabelEstilizada("PRÉVIA DA ETIQUETA:");
        lblPreview.setHorizontalAlignment(SwingConstants.CENTER);
        colDireita.add(lblPreview, BorderLayout.NORTH);

        previewPanel = new LabelPreviewPanel();
        colDireita.add(previewPanel, BorderLayout.CENTER);

        panelPrincipal.add(colDireita);

        add(panelPrincipal, BorderLayout.CENTER);

        // --- BOTÃO SUL ---
        JPanel panelSul = new JPanel(new BorderLayout());
        panelSul.setOpaque(false);
        panelSul.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));

        btnImprimir = new JButton("IMPRIMIR ETIQUETAS");
        btnImprimir.setBackground(corRaposa);
        btnImprimir.setForeground(Color.BLACK);
        btnImprimir.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnImprimir.setFocusPainted(false);
        btnImprimir.setOpaque(true);
        btnImprimir.setBorderPainted(false);
        btnImprimir.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        btnImprimir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnImprimir.addActionListener(e -> executarImpressao());

        btnImprimir.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnImprimir.setBackground(corRaposa.darker()); }
            public void mouseExited(MouseEvent e) { btnImprimir.setBackground(corRaposa); }
        });

        panelSul.add(btnImprimir, BorderLayout.CENTER);
        add(panelSul, BorderLayout.SOUTH);
    }

    private JLabel criarLabelEstilizada(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(fontNegrito);
        label.setForeground(corTexto);
        return label;
    }

    private void estilizarComponente(JComponent c) {
        c.setFont(fontPadrao);
        if (c instanceof JTextField || c instanceof JComboBox) {
            c.setPreferredSize(new Dimension(c.getPreferredSize().width, 35));
        }
        if (c instanceof JTextComponent) {
            c.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
        }
    }

    private String[] listarImpressoras() {
        PrintService[] servicos = PrintServiceLookup.lookupPrintServices(null, null);
        if (servicos.length == 0) return new String[]{"Nenhuma impressora instalada"};
        return Arrays.stream(servicos).map(PrintService::getName).toArray(String[]::new);
    }

    private void selecionarImpressoraPadrao() {
        PrintService padrao = PrintServiceLookup.lookupDefaultPrintService();
        if (padrao != null) {
            comboImpressoras.setSelectedItem(padrao.getName());
        }
    }

    private void scanTemplate() {
        panelCamposDinamicos.removeAll();
        mapaInputs.clear();
        mapaCoordenadas.clear();
        mapaBarras.clear();
        mapaLinhas.clear();
        
        String arquivo = (String) comboTemplates.getSelectedItem();
        if (arquivo == null) return;

        try {
            String conteudo = new String(Files.readAllBytes(Paths.get("templates", arquivo)));
            
            // Lógica de Parsing ZPL Lite para Coordenadas
            String[] comandos = conteudo.split("\\^");
            int lastX = 30, lastY = 30;
            
            for (String cmd : comandos) {
                if (cmd.startsWith("FO")) { // Field Origin
                    try {
                        String[] coords = cmd.substring(2).split(",");
                        lastX = Integer.parseInt(coords[0].replaceAll("[^0-9]", ""));
                        lastY = Integer.parseInt(coords[1].replaceAll("[^0-9]", ""));
                    } catch (Exception ignored) {}
                }
                
                if (cmd.startsWith("FD")) { // Field Data
                    String data = cmd.substring(2).split("\\^")[0].split("\\$")[0].split("\\n")[0].trim();
                    if (data.startsWith("#")) {
                        mapaCoordenadas.put(data, new Point(lastX, lastY));
                    }
                }
                
                if (cmd.startsWith("B") || cmd.contains("BC") || cmd.contains("B3") || cmd.contains("BE")) { // Barcodes
                    mapaBarras.add(new Rectangle(lastX, lastY, 200, 60)); // Estimativa
                }
                
                if (cmd.startsWith("GB")) { // Graphic Box
                    try {
                        String[] dims = cmd.substring(2).split(",");
                        int w = dims.length > 0 ? Integer.parseInt(dims[0]) : 100;
                        int h = dims.length > 1 ? Integer.parseInt(dims[1]) : 2;
                        mapaLinhas.add(new Rectangle(lastX, lastY, w, h));
                    } catch (Exception ignored) {}
                }
            }

            // Gerar inputs baseados nas tags encontradas
            Set<String> tags = new TreeSet<>(mapaCoordenadas.keySet());
            for (String tag : tags) {
                JPanel row = new JPanel(new BorderLayout(10, 0));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                
                String labelTexto = DICIONARIO.getOrDefault(tag, "Campo " + tag);
                JLabel label = new JLabel(labelTexto + ":");
                label.setFont(fontPadrao);
                label.setPreferredSize(new Dimension(140, 30));
                
                row.add(label, BorderLayout.WEST);
                
                JTextComponent input;
                if (tag.equals("#TEXTO")) {
                    // Área de texto multilinha
                    JTextArea textArea = new JTextArea();
                    textArea.setLineWrap(true);
                    textArea.setWrapStyleWord(true);
                    textArea.setRows(4); // Quantidade inicial de linhas
                    estilizarComponente(textArea);
                    
                    JScrollPane scrollText = new JScrollPane(textArea);
                    scrollText.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                    row.add(scrollText, BorderLayout.CENTER);
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100)); // Mais alto
                    input = textArea;
                } else {
                    // Campo de texto simples
                    input = new JTextField();
                    estilizarComponente(input);
                    row.add(input, BorderLayout.CENTER);
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45)); // Padrão
                }
                
                mapaInputs.put(tag, input);
                
                // Listener para atualizar a prévia enquanto digita
                input.getDocument().addDocumentListener(new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) { previewPanel.repaint(); }
                    public void removeUpdate(DocumentEvent e) { previewPanel.repaint(); }
                    public void changedUpdate(DocumentEvent e) { previewPanel.repaint(); }
                });
                
                panelCamposDinamicos.add(row);
            }
        } catch (Exception e) {
            panelCamposDinamicos.add(new JLabel("Erro: " + e.getMessage()));
        }
        panelCamposDinamicos.revalidate();
        panelCamposDinamicos.repaint();
        previewPanel.repaint();
    }

    private void executarImpressao() {
        try {
            String arquivo = (String) comboTemplates.getSelectedItem();
            if (arquivo == null) throw new Exception("Selecione um template.");
            
            String impressoraSelecionada = (String) comboImpressoras.getSelectedItem();
            String zpl = new String(Files.readAllBytes(Paths.get("templates", arquivo)));

            for (Map.Entry<String, JTextComponent> entry : mapaInputs.entrySet()) {
                String tag = entry.getKey();
                String valor = entry.getValue().getText(); // Mantém espaços para verificação
                
                if (valor.trim().isEmpty()) {
                    // Remove todo o bloco ZPL associado a esta tag vazia (ex: ^FO...^BEN...^FD#D^FS)
                    // Isso evita que a impressora tente gerar um código de barras sem dados ou sujeira na tela
                    String regex = "(?s)\\^FO(?:(?!\\^FO).)*?\\^FD" + Pattern.quote(tag) + "\\^FS";
                    zpl = zpl.replaceAll(regex, "");
                    
                    // Tratamento específico: Se a tag for EAN (#D) e estiver vazia, removemos também o texto fixo "Codigo EAN:"
                    if (tag.equals("#D")) {
                        String regexLabelEan = "(?s)\\^FO(?:(?!\\^FO).)*?\\^FDCodigo EAN:\\^FS[\\r\\n]*";
                        zpl = zpl.replaceAll(regexLabelEan, "");
                    }
                    
                    // Fallback para limpar a tag caso ela esteja fora de um bloco padrão
                    zpl = zpl.replace(tag, "");
                } else {
                    // Se for campo de texto múltiplo, substitui as quebras de linha do Java pelo caractere ZPL correspondente (\&)
                    if (tag.equals("#TEXTO")) {
                         valor = valor.replace("\n", "\\&");
                    }
                    zpl = zpl.replace(tag, valor);
                }
            }

            int qtd = 1;
            try { qtd = Integer.parseInt(txtQtd.getText()); } catch (Exception e) {}
            zpl = zpl.replaceAll("\\^PQ\\d+", "^PQ" + qtd);

            MotorEtiquetaZebra.enviarParaImpressora(zpl, impressoraSelecionada);
            
            btnImprimir.setText("ENVIADO COM SUCESSO!");
            btnImprimir.setBackground(new Color(40, 167, 69));
            new javax.swing.Timer(2000, e -> {
                btnImprimir.setText("IMPRIMIR ETIQUETAS");
                btnImprimir.setBackground(corRaposa);
            }).start();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro na Impressão", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String[] listarArquivosOut() {
    File pasta = new File("templates"); // <-- Alterado de "." para "templates"
    File[] arquivos = pasta.listFiles((dir, nome) -> nome.toLowerCase().endsWith(".out"));
    if (arquivos == null || arquivos.length == 0) return new String[0];
    return Arrays.stream(arquivos).map(File::getName).toArray(String[]::new);
    }

    /**
     * Componente Interno para Renderizar a Etiqueta
     */
    private class LabelPreviewPanel extends JPanel {
        private final double ESCALA = 0.5; // Escala de dots para pixels

        public LabelPreviewPanel() {
            setBackground(Color.WHITE);
            setBorder(new LineBorder(new Color(200, 200, 200), 2));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Desenhar Linhas/Boxes do Template
            g2.setColor(new Color(220, 220, 220));
            for (Rectangle r : mapaLinhas) {
                g2.fillRect((int)(r.x * ESCALA), (int)(r.y * ESCALA), (int)(r.width * ESCALA), Math.max(1, (int)(r.height * ESCALA)));
            }

            // Desenhar Placeholders de Código de Barras (Visual Simulado)
            for (Rectangle r : mapaBarras) {
                int rx = (int)(r.x * ESCALA);
                int ry = (int)(r.y * ESCALA);
                int rw = (int)(r.width * ESCALA);
                int rh = (int)(r.height * ESCALA);
                
                // Fundo branco do código de barras
                g2.setColor(Color.WHITE);
                g2.fillRect(rx, ry, rw, rh);
                
                // Desenhar linhas para simular o código de barras
                g2.setColor(Color.BLACK);
                Random random = new Random(r.x + r.y); // Seed fixa para manter o desenho estático
                int currentX = rx + 5;
                while (currentX < rx + rw - 5) {
                    int barWidth = random.nextInt(4) + 1; // Largura da barra preta
                    int spaceWidth = random.nextInt(3) + 1; // Largura do espaço branco
                    
                    if (currentX + barWidth > rx + rw - 5) break;
                    
                    g2.fillRect(currentX, ry + 5, barWidth, rh - 10);
                    currentX += barWidth + spaceWidth;
                }
            }

            // Desenhar Textos Dinâmicos
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            
            for (Map.Entry<String, Point> entry : mapaCoordenadas.entrySet()) {
                String tag = entry.getKey();
                Point p = entry.getValue();
                String texto = mapaInputs.containsKey(tag) ? mapaInputs.get(tag).getText() : tag;
                if (texto.isEmpty()) texto = tag;
                
                // Lógica para desenho de texto com múltiplas linhas
                if (texto.contains("\n")) {
                    String[] linhas = texto.split("\n");
                    int yOffset = 0;
                    for (String linha : linhas) {
                        g2.drawString(linha, (int)(p.x * ESCALA), (int)(p.y * ESCALA) + yOffset);
                        yOffset += g2.getFontMetrics().getHeight(); // Incrementa o Y com base na altura da fonte
                    }
                } else {
                    g2.drawString(texto, (int)(p.x * ESCALA), (int)(p.y * ESCALA));
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        SwingUtilities.invokeLater(() -> new FoxSeniorApp().setVisible(true));
    }
}