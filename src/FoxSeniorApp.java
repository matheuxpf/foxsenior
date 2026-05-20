import javax.swing.*;
import javax.swing.border.TitledBorder;
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
    
    private List<SlotLegado> slotsAtivos = new ArrayList<>();
    private final String PASTA_TEMPLATES = "templates";
    private final Color corRaposa = new Color(235, 110, 0);

    public FoxSeniorApp() {
        setTitle("FoxSenior - Impressão de Etiquetas Livres");
        setSize(950, 600); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        inicializarInterface();
        carregarArquivos();
    }

    private void inicializarInterface() {
        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- TOPO: IMPRESSORA ---
        JPanel painelTopo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelTopo.add(new JLabel("Impressora de Destino:"));
        comboImpressoras = new JComboBox<>(listarImpressoras());
        selecionarImpressoraPadrao();
        painelTopo.add(comboImpressoras);
        painelPrincipal.add(painelTopo, BorderLayout.NORTH);

        // --- CENTRO: SPLIT PANEL ---
        JPanel painelCentro = new JPanel(new GridLayout(1, 2, 15, 0));

        // COLUNA ESQUERDA (Seleção e Configurações)
        JPanel colEsquerda = new JPanel(new BorderLayout(5, 5));
        colEsquerda.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(corRaposa, 2), "Arquivo(s) de Etiqueta(s)", 
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12), corRaposa));
        
        modelLista = new DefaultListModel<>();
        listArquivos = new JList<>(modelLista);
        listArquivos.setFont(new Font("Arial", Font.PLAIN, 14));
        listArquivos.setSelectionBackground(corRaposa);
        listArquivos.setSelectionForeground(Color.WHITE);
        listArquivos.addListSelectionListener(this::selecionarArquivo);
        colEsquerda.add(new JScrollPane(listArquivos), BorderLayout.CENTER);

        // Painel Inferior Esquerdo (Inputs operacionais)
        JPanel painelInputsLista = new JPanel(new GridBagLayout());
        painelInputsLista.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        painelInputsLista.add(new JLabel("Modelo de Impressão:"), gbc);
        gbc.gridx = 1; 
        comboModelo = new JComboBox<>(new String[]{"1 Coluna", "3 Colunas (Horizontal)"});
        comboModelo.setSelectedIndex(0); 
        painelInputsLista.add(comboModelo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painelInputsLista.add(new JLabel("Quantidade Etiq.:"), gbc);
        gbc.gridx = 1;
        txtQtd = new JTextField("1", 5);
        painelInputsLista.add(txtQtd, gbc);

        colEsquerda.add(painelInputsLista, BorderLayout.SOUTH);
        painelCentro.add(colEsquerda);

        // COLUNA DIREITA (Editor de Texto)
        JPanel colDireita = new JPanel(new BorderLayout(5, 5));
        colDireita.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(corRaposa, 2), "Edição Rápida da Etiqueta", 
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12), corRaposa));
        
        areaTextoEtiqueta = new JTextArea();
        areaTextoEtiqueta.setFont(new Font("Monospaced", Font.BOLD, 16)); 
        areaTextoEtiqueta.setBackground(new Color(250, 250, 250));
        colDireita.add(new JScrollPane(areaTextoEtiqueta), BorderLayout.CENTER);
        
        JLabel lblDica = new JLabel("💡 Altere os dados acima e clique em 'SALVAR ALTERAÇÕES' para gravar.");
        lblDica.setForeground(Color.GRAY);
        colDireita.add(lblDica, BorderLayout.SOUTH);

        painelCentro.add(colDireita);
        painelPrincipal.add(painelCentro, BorderLayout.CENTER);

        // --- RODAPÉ: BOTÕES ---
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        
        JButton btnSalvar = new JButton("SALVAR ALTERAÇÕES");
        btnSalvar.setBackground(Color.DARK_GRAY);
        btnSalvar.setForeground(Color.WHITE);
        btnSalvar.setFont(new Font("Arial", Font.BOLD, 14));
        btnSalvar.setPreferredSize(new Dimension(200, 45));
        btnSalvar.addActionListener(e -> executarSalvamento());

        JButton btnOk = new JButton("IMPRIMIR ETIQUETA");
        btnOk.setBackground(corRaposa);
        btnOk.setForeground(Color.WHITE);
        btnOk.setFont(new Font("Arial", Font.BOLD, 14));
        btnOk.setPreferredSize(new Dimension(200, 45));
        btnOk.addActionListener(e -> executarImpressao());
        
        JButton btnFechar = new JButton("FECHAR");
        btnFechar.setFont(new Font("Arial", Font.BOLD, 14));
        btnFechar.setPreferredSize(new Dimension(120, 45));
        btnFechar.addActionListener(e -> System.exit(0));

        painelBotoes.add(btnSalvar);
        painelBotoes.add(btnOk);
        painelBotoes.add(btnFechar);
        painelPrincipal.add(painelBotoes, BorderLayout.SOUTH);

        add(painelPrincipal);
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

        slotsAtivos.clear();
        areaTextoEtiqueta.setText("");
        StringBuilder sbPreview = new StringBuilder();

        try {
            List<String> linhas = Files.readAllLines(Paths.get(PASTA_TEMPLATES, nomeArquivo));
            for (int i = 0; i < linhas.size(); i++) {
                String MathLinha = linhas.get(i);
                
                if (MathLinha.contains("|")) {
                    String[] partes = MathLinha.split("\\|");
                    String meta = partes[0].trim();
                    String textoLimpo = partes.length > 1 ? partes[1].trim() : "";
                    if (textoLimpo.isEmpty() && meta.startsWith("10")) continue;
                    
                    int fonte = 22;
                    try { fonte = Integer.parseInt(meta.split("-")[0].trim()); } catch (Exception ex) {}

                    SlotLegado slot = new SlotLegado();
                    slot.linhaIndex = i;
                    slot.tamanhoFonte = fonte;
                    slotsAtivos.add(slot);
                    sbPreview.append(textoLimpo).append("\n");
                } else {
                    SlotLegado slot = new SlotLegado();
                    slot.linhaIndex = i;
                    slot.tamanhoFonte = 25; 
                    slotsAtivos.add(slot);
                    sbPreview.append(MathLinha).append("\n");
                }
            }
            areaTextoEtiqueta.setText(sbPreview.toString());
            areaTextoEtiqueta.setCaretPosition(0);
        } catch (Exception ex) {
            areaTextoEtiqueta.setText("Erro ao carregar arquivo: " + ex.getMessage());
        }
    }

    private void executarSalvamento() {
        String arquivo = listArquivos.getSelectedValue();
        if (arquivo == null) {
            JOptionPane.showMessageDialog(this, "Selecione um arquivo de etiqueta na lista para salvar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String textoParaSalvar = areaTextoEtiqueta.getText();
            Files.write(Paths.get(PASTA_TEMPLATES, arquivo), textoParaSalvar.getBytes());
            JOptionPane.showMessageDialog(this, "Alterações gravadas no arquivo com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            
            // Recarrega o arquivo silenciosamente para reestruturar as linhas na memória
            selecionarArquivo(new ListSelectionEvent(listArquivos, listArquivos.getSelectedIndex(), listArquivos.getSelectedIndex(), false));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void executarImpressao() {
        String arquivo = listArquivos.getSelectedValue();
        String impressora = (String) comboImpressoras.getSelectedItem();
        if (arquivo == null) {
            JOptionPane.showMessageDialog(this, "Selecione um arquivo de etiqueta na lista.");
            return;
        }

        try {
            int qtd = Integer.parseInt(txtQtd.getText());
            String zplFinal = "";

            if (arquivo.toLowerCase().endsWith(".txt")) {
                String[] linhasDigitadas = areaTextoEtiqueta.getText().split("\n");
                StringBuilder zpl = new StringBuilder("^XA\n");
                int fatorY = 35; 
                int numColunas = comboModelo.getSelectedIndex() == 1 ? 3 : 1;
                int larguraColuna = 265; 

                for (int i = 0; i < linhasDigitadas.length; i++) {
                    String texto = linhasDigitadas[i].trim();
                    if (texto.isEmpty()) continue;
                    
                    int y = (i * fatorY) + 40;
                    int h = (i < slotsAtivos.size()) ? slotsAtivos.get(i).tamanhoFonte : 25;

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
            JOptionPane.showMessageDialog(this, "Etiqueta(s) enviada(s) para a fila com sucesso!", "FoxSenior", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro na impressão: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String[] listarImpressoras() {
        PrintService[] servicos = PrintServiceLookup.lookupPrintServices(null, null);
        return Arrays.stream(servicos).map(PrintService::getName).toArray(String[]::new);
    }

    private void selecionarImpressoraPadrao() {
        PrintService padrao = PrintServiceLookup.lookupDefaultPrintService();
        if (padrao != null) comboImpressoras.setSelectedItem(padrao.getName());
    }

    static class SlotLegado { int linhaIndex; int tamanhoFonte; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FoxSeniorApp().setVisible(true));
    }
}