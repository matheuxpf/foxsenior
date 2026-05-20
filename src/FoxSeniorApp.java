import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class FoxSeniorApp extends JFrame {

    private JList<String> listArquivos;
    private DefaultListModel<String> modelLista;
    private JTextArea areaTextoEtiqueta;
    private JComboBox<String> comboModelo;
    private JTextField txtQtd;
    private JComboBox<String> comboImpressoras;
    private JSpinner spinnerFonte;
    
    private PainelPreview previewPanel;
    private Map<Integer, Integer> mapFontes = new HashMap<>();
    private boolean isUpdatingSpinner = false;

    private final String PASTA_TEMPLATES = "templates";
    private final Color corRaposa = new Color(235, 110, 0); 
    private final Color corGrafite = new Color(50, 50, 50);

    // Fontes Globais Padronizadas e Maiores
    private final Font fonteTituloJanela = new Font("Arial", Font.BOLD, 24);
    private final Font fonteTitulosPaineis = new Font("Arial", Font.BOLD, 15);
    private final Font fonteLabels = new Font("Arial", Font.BOLD, 14);
    private final Font fonteInputs = new Font("Arial", Font.PLAIN, 15);

    public FoxSeniorApp() {
        setTitle("FoxSenior - Emissor de Etiquetas");
        setSize(1200, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        try { setIconImage(Toolkit.getDefaultToolkit().getImage("src/assets/logofox.png")); } catch (Exception e) {}
        
        // Aplica o visual nativo do Windows
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        
        inicializarInterface();
        carregarArquivos();
    }

    private void inicializarInterface() {
        JPanel painelPrincipal = new JPanel(new BorderLayout(15, 15));
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        painelPrincipal.setBackground(Color.WHITE);

        // --- 1. BARRA SUPERIOR (BRANDING) ---
        JPanel painelTopo = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        painelTopo.setBackground(Color.WHITE);
        painelTopo.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        try {
            ImageIcon iconOriginal = new ImageIcon("src/assets/logofox.png");
            Image imgRedimensionada = iconOriginal.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
            painelTopo.add(new JLabel(new ImageIcon(imgRedimensionada)));
        } catch (Exception e) {}
        
        JLabel lblTitulo = new JLabel("FoxSenior");
        lblTitulo.setFont(fonteTituloJanela);
        lblTitulo.setForeground(corGrafite);
        painelTopo.add(lblTitulo);
        painelPrincipal.add(painelTopo, BorderLayout.NORTH);

        // Bordas Robustas para Botões
        Border bordaBotaoImprimir = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(corRaposa.darker(), 3), 
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        );
        Border bordaBotaoSalvar = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 3), 
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        );

        // --- 2. MENU LATERAL ESQUERDO (ARQUIVOS + CONFIGURAÇÕES) ---
        JPanel painelEsquerdo = new JPanel(new BorderLayout(0, 15));
        painelEsquerdo.setBackground(Color.WHITE);
        painelEsquerdo.setPreferredSize(new Dimension(320, 0));

        JPanel panelArquivos = new JPanel(new BorderLayout());
        panelArquivos.setBackground(Color.WHITE);
        panelArquivos.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(corRaposa, 2), " ARQUIVOS DISPONÍVEIS ", 
            TitledBorder.LEFT, TitledBorder.TOP, fonteTitulosPaineis, corRaposa));
        
        modelLista = new DefaultListModel<>();
        listArquivos = new JList<>(modelLista);
        listArquivos.setFont(fonteInputs);
        listArquivos.setSelectionBackground(corRaposa);
        listArquivos.setSelectionForeground(Color.WHITE);
        listArquivos.addListSelectionListener(this::selecionarArquivo);
        
        JScrollPane scrollArquivos = new JScrollPane(listArquivos);
        panelArquivos.add(scrollArquivos, BorderLayout.CENTER);
        painelEsquerdo.add(panelArquivos, BorderLayout.CENTER); 

        JPanel panelConfiguracoes = new JPanel(new GridBagLayout());
        panelConfiguracoes.setBackground(new Color(248, 248, 248)); 
        panelConfiguracoes.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(corGrafite, 2), " OPÇÕES DE IMPRESSÃO ", 
            TitledBorder.LEFT, TitledBorder.TOP, fonteTitulosPaineis, corGrafite));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        gbc.gridy = 0; 
        JLabel lblImp = new JLabel("Impressora de Destino:");
        lblImp.setFont(fonteLabels);
        panelConfiguracoes.add(lblImp, gbc);
        
        gbc.gridy = 1; 
        comboImpressoras = new JComboBox<>(listarImpressoras());
        comboImpressoras.setFont(fonteInputs);
        selecionarImpressoraPadrao();
        panelConfiguracoes.add(comboImpressoras, gbc);

        gbc.gridy = 2; 
        JLabel lblMod = new JLabel("Modelo de Layout:");
        lblMod.setFont(fonteLabels);
        panelConfiguracoes.add(lblMod, gbc);
        
        gbc.gridy = 3; 
        comboModelo = new JComboBox<>(new String[]{"1 Coluna", "Multicolunas (Lado a Lado)"});
        comboModelo.setFont(fonteInputs);
        comboModelo.addActionListener(e -> previewPanel.repaint());
        panelConfiguracoes.add(comboModelo, gbc);

        gbc.gridy = 4; 
        JLabel lblQtd = new JLabel("Quantidade de Cópias:");
        lblQtd.setFont(fonteLabels);
        panelConfiguracoes.add(lblQtd, gbc);
        
        gbc.gridy = 5; 
        txtQtd = new JTextField("1");
        txtQtd.setFont(new Font("Arial", Font.BOLD, 16));
        txtQtd.setHorizontalAlignment(JTextField.CENTER);
        panelConfiguracoes.add(txtQtd, gbc);
        
        painelEsquerdo.add(panelConfiguracoes, BorderLayout.SOUTH);
        painelPrincipal.add(painelEsquerdo, BorderLayout.WEST);

        // --- 3. PAINEL DIREITO (EDITOR + PREVIEW) ---
        JPanel painelDireito = new JPanel(new BorderLayout(10, 10));
        painelDireito.setBackground(Color.WHITE);
        
        JPanel panelFerramentasFonte = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panelFerramentasFonte.setBackground(Color.WHITE);
        JLabel lblFonte = new JLabel("TAMANHO DA FONTE (LINHA ATUAL):");
        lblFonte.setFont(fonteLabels);
        panelFerramentasFonte.add(lblFonte);
        
        spinnerFonte = new JSpinner(new SpinnerNumberModel(25, 10, 100, 1));
        spinnerFonte.setFont(fonteInputs);
        spinnerFonte.setPreferredSize(new Dimension(80, 30));
        spinnerFonte.addChangeListener(e -> alterarFonteLinhaAtual());
        panelFerramentasFonte.add(spinnerFonte);
        
        areaTextoEtiqueta = new JTextArea();
        areaTextoEtiqueta.setFont(new Font("Monospaced", Font.BOLD, 16)); 
        areaTextoEtiqueta.setBackground(new Color(250, 250, 250));
        JScrollPane scrollEdicao = new JScrollPane(areaTextoEtiqueta);
        scrollEdicao.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1), " EDITOR DE TEXTO ", 
            TitledBorder.LEFT, TitledBorder.TOP, fonteTitulosPaineis, Color.DARK_GRAY));
        configurarWYSIWYG();

        JPanel painelEditor = new JPanel(new BorderLayout());
        painelEditor.setBackground(Color.WHITE);
        painelEditor.add(panelFerramentasFonte, BorderLayout.NORTH);
        painelEditor.add(scrollEdicao, BorderLayout.CENTER);
        
        previewPanel = new PainelPreview();
        previewPanel.setPreferredSize(new Dimension(0, 340));
        previewPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 2), " VISUALIZAÇÃO DE IMPRESSÃO ",
            TitledBorder.LEFT, TitledBorder.TOP, fonteTitulosPaineis, Color.BLACK));

        JSplitPane splitDireito = new JSplitPane(JSplitPane.VERTICAL_SPLIT, painelEditor, previewPanel);
        splitDireito.setResizeWeight(0.5); 
        splitDireito.setBorder(null);
        painelDireito.add(splitDireito, BorderLayout.CENTER);

        // --- 4. BOTÕES INFERIORES COM PINTURA CUSTOMIZADA ---
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        painelBotoes.setBackground(Color.WHITE);
        
        // SOBREPOSIÇÃO: Botão Salvar
        JButton btnSalvar = new JButton("SALVAR ALTERAÇÕES") {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        btnSalvar.setBackground(corGrafite);
        btnSalvar.setForeground(Color.WHITE);
        btnSalvar.setFont(fonteLabels);
        btnSalvar.setBorder(bordaBotaoSalvar);
        btnSalvar.setFocusPainted(false);
        btnSalvar.setContentAreaFilled(false); // Anula a interferência do Windows
        btnSalvar.setOpaque(true);
        btnSalvar.addActionListener(e -> executarSalvamento());

        // SOBREPOSIÇÃO: Botão Imprimir
        JButton btnOk = new JButton("IMPRIMIR ETIQUETA") {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        btnOk.setBackground(corRaposa);
        btnOk.setForeground(Color.WHITE);
        btnOk.setFont(fonteLabels);
        btnOk.setBorder(bordaBotaoImprimir);
        btnOk.setFocusPainted(false);
        btnOk.setContentAreaFilled(false); // Anula a interferência do Windows
        btnOk.setOpaque(true);
        btnOk.addActionListener(e -> executarImpressao());

        painelBotoes.add(btnSalvar);
        painelBotoes.add(btnOk);
        painelDireito.add(painelBotoes, BorderLayout.SOUTH);

        painelPrincipal.add(painelDireito, BorderLayout.CENTER);
        add(painelPrincipal);
    }

    private void configurarWYSIWYG() {
        areaTextoEtiqueta.addCaretListener(e -> {
            try {
                int linhaAtual = areaTextoEtiqueta.getLineOfOffset(areaTextoEtiqueta.getCaretPosition());
                isUpdatingSpinner = true;
                spinnerFonte.setValue(mapFontes.getOrDefault(linhaAtual, 25));
                isUpdatingSpinner = false;
            } catch (Exception ex) {}
        });

        areaTextoEtiqueta.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { previewPanel.repaint(); }
            public void removeUpdate(DocumentEvent e) { previewPanel.repaint(); }
            public void changedUpdate(DocumentEvent e) { previewPanel.repaint(); }
        });
    }

    private void alterarFonteLinhaAtual() {
        if (isUpdatingSpinner) return; 
        try {
            int linhaAtual = areaTextoEtiqueta.getLineOfOffset(areaTextoEtiqueta.getCaretPosition());
            mapFontes.put(linhaAtual, (Integer) spinnerFonte.getValue());
            previewPanel.repaint(); 
        } catch (Exception ex) {}
    }

    class PainelPreview extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int larguraCanvas = getWidth();
            int alturaCanvas = getHeight();
            
            g2d.setColor(new Color(230, 230, 230));
            g2d.fillRect(0, 0, larguraCanvas, alturaCanvas);
            
            int larguraEtiqueta = 350;
            int alturaEtiqueta = 350; 
            int xEtiqueta = (larguraCanvas - larguraEtiqueta) / 2; 
            int yEtiqueta = 15;

            g2d.setColor(Color.WHITE);
            g2d.fillRect(xEtiqueta, yEtiqueta, larguraEtiqueta, alturaEtiqueta);
            g2d.setColor(Color.GRAY);
            g2d.drawRect(xEtiqueta, yEtiqueta, larguraEtiqueta, alturaEtiqueta);
            
            Stroke pincelOriginal = g2d.getStroke();
            Stroke tracejado = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f);
            g2d.setStroke(tracejado);
            g2d.setColor(new Color(220, 80, 80, 180));
            int yLimite = yEtiqueta + alturaEtiqueta - 2;
            g2d.drawLine(xEtiqueta + 2, yLimite, xEtiqueta + larguraEtiqueta - 2, yLimite);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2d.drawString("FIM DA ETIQUETA", xEtiqueta + (larguraEtiqueta / 2) - 45, yLimite - 6);
            g2d.setStroke(pincelOriginal);
            
            g2d.setColor(Color.BLACK);
            String[] linhas = areaTextoEtiqueta.getText().split("\n");
            int fatorY = 24; 
            
            for (int i = 0; i < linhas.length; i++) {
                String texto = linhas[i].trim();
                if (texto.isEmpty()) continue;
                
                int tamanhoFonte = mapFontes.getOrDefault(i, 25);
                int fonteRender = tamanhoFonte - 7;
                
                g2d.setFont(new Font("SansSerif", Font.BOLD, fonteRender));
                int yRender = yEtiqueta + 25 + (i * fatorY); 
                
                if (yRender < yEtiqueta + alturaEtiqueta) {
                    g2d.drawString(texto, xEtiqueta + 15, yRender);
                }
            }
        }
    }

    private void carregarArquivos() {
        File pasta = new File(PASTA_TEMPLATES);
        if (!pasta.exists()) pasta.mkdir();
        
        File[] arquivos = pasta.listFiles((dir, nome) -> {
            String n = nome.toLowerCase();
            return n.endsWith(".txt") || n.endsWith(".out");
        });
        if (arquivos != null) {
            Arrays.sort(arquivos);
            modelLista.clear();
            for (File f : arquivos) modelLista.addElement(f.getName());
        }
    }

    private void selecionarArquivo(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        String nomeArquivo = listArquivos.getSelectedValue();
        if (nomeArquivo == null) return;

        mapFontes.clear(); 
        areaTextoEtiqueta.setText("");
        StringBuilder sbTexto = new StringBuilder();

        try {
            List<String> linhas = Files.readAllLines(Paths.get(PASTA_TEMPLATES, nomeArquivo));
            int indiceLinha = 0;

            for (String linha : linhas) {
                if (linha.contains("|")) {
                    String[] partes = linha.split("\\|");
                    String meta = partes[0].trim();
                    String textoLimpo = partes.length > 1 ? partes[1].trim() : "";
                    if (textoLimpo.isEmpty() && meta.startsWith("10")) continue;
                    
                    int fonte = 25;
                    try { fonte = Integer.parseInt(meta.split("-")[0].trim()); } catch (Exception ex) {}

                    mapFontes.put(indiceLinha, fonte);
                    sbTexto.append(textoLimpo).append("\n");
                    indiceLinha++;
                } else {
                    mapFontes.put(indiceLinha, 25);
                    sbTexto.append(linha).append("\n");
                    indiceLinha++;
                }
            }
            areaTextoEtiqueta.setText(sbTexto.toString());
            areaTextoEtiqueta.setCaretPosition(0);
            previewPanel.repaint(); 
        } catch (Exception ex) {
            areaTextoEtiqueta.setText("Erro: " + ex.getMessage());
        }
    }

    private void executarSalvamento() {
        String arquivo = listArquivos.getSelectedValue();
        if (arquivo == null) return;

        try {
            String[] linhasText = areaTextoEtiqueta.getText().split("\n");
            StringBuilder sbSalvar = new StringBuilder();
            
            for (int i = 0; i < linhasText.length; i++) {
                int fonte = mapFontes.getOrDefault(i, 25);
                sbSalvar.append(fonte).append(" - 0|").append(linhasText[i]).append("\n");
            }
            
            Files.write(Paths.get(PASTA_TEMPLATES, arquivo), sbSalvar.toString().getBytes());
            JOptionPane.showMessageDialog(this, "Alterações gravadas com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {}
    }

    private void executarImpressao() {
        String arquivo = listArquivos.getSelectedValue();
        String impressora = (String) comboImpressoras.getSelectedItem();
        if (arquivo == null) return;

        try {
            int qtd = Integer.parseInt(txtQtd.getText());
            String zplFinal = "";

            if (arquivo.toLowerCase().endsWith(".txt")) {
                String[] linhasDigitadas = areaTextoEtiqueta.getText().split("\n");
                StringBuilder zpl = new StringBuilder("^XA\n");
                
                int numColunas = comboModelo.getSelectedIndex() == 1 ? 2 : 1;
                int larguraColuna = 265; 

                for (int i = 0; i < linhasDigitadas.length; i++) {
                    String texto = linhasDigitadas[i].trim();
                    if (texto.isEmpty()) continue;
                    
                    int h = mapFontes.getOrDefault(i, 25);
                    int y = (i * 40) + 40; 

                    for (int c = 0; c < numColunas; c++) {
                        int x = 20 + (c * larguraColuna);
                        zpl.append(String.format("^FO%d,%d^A0N,%d,%d^CI13^FD%s^FS\n", x, y, h, h, texto));
                    }
                }
                zpl.append("^PQ").append(qtd).append("\n^XZ");
                zplFinal = zpl.toString();
            } else {
                zplFinal = new String(Files.readAllBytes(Paths.get(PASTA_TEMPLATES, arquivo)));
                zplFinal = zplFinal.replaceAll("\\^PQ\\d+", "^PQ" + qtd);
            }

            MotorEtiquetaZebra.enviarParaImpressora(zplFinal, impressora);
            JOptionPane.showMessageDialog(this, "Trabalho enviado com sucesso para a Zebra!");

        } catch (Exception ex) {}
    }

    private String[] listarImpressoras() {
        PrintService[] servicos = PrintServiceLookup.lookupPrintServices(null, null);
        return Arrays.stream(servicos).map(PrintService::getName).toArray(String[]::new);
    }

    private void selecionarImpressoraPadrao() {
        PrintService padrao = PrintServiceLookup.lookupDefaultPrintService();
        if (padrao != null) comboImpressoras.setSelectedItem(padrao.getName());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FoxSeniorApp().setVisible(true));
    }
}