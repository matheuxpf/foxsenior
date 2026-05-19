import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FoxSeniorApp extends JFrame {

    private JComboBox<String> comboImpressoras;
    private JComboBox<String> comboTemplates;
    private JTextField txtQtd;
    private JPanel panelCamposDinamicos;
    private JTextArea areaTextoLegado;
    private JScrollPane scrollCamposDinamicos;
    private JScrollPane scrollTxtLegado;
    
    private Map<String, JTextField> mapaInputs = new HashMap<>();
    private List<SlotLegado> slotsAtivos = new ArrayList<>();
    private JButton btnImprimir;
    private final Color corRaposa = new Color(235, 110, 0);
    private final Map<String, String> DICIONARIO = new HashMap<>();

    public FoxSeniorApp() {
        // Dicionário para mapeamento de tags de arquivos .out
        DICIONARIO.put("#C", "Produto"); DICIONARIO.put("#E", "Sabor"); DICIONARIO.put("#D", "EAN/Barras");
        DICIONARIO.put("#R", "Lote");    DICIONARIO.put("#I", "Turno"); DICIONARIO.put("#Q", "Validade");
        DICIONARIO.put("#B", "Marca");

        setTitle("FoxSenior - Painel de Etiquetas");
        setSize(650, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // PAINEL SUPERIOR
        JPanel panelNorte = new JPanel(new GridLayout(6, 1, 2, 2));
        panelNorte.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        panelNorte.add(new JLabel("1. Selecione a Impressora:"));
        comboImpressoras = new JComboBox<>(listarImpressoras());
        selecionarImpressoraPadrao();
        panelNorte.add(comboImpressoras);

        panelNorte.add(new JLabel("2. Selecione o modelo de etiqueta (.out ou .txt):"));
        comboTemplates = new JComboBox<>(listarTemplates());
        panelNorte.add(comboTemplates);
        
        panelNorte.add(new JLabel("3. Quantidade de etiquetas:"));
        txtQtd = new JTextField("1");
        panelNorte.add(txtQtd);
        add(panelNorte, BorderLayout.NORTH);

        // PAINEL CENTRAL COMPARTILHADO
        JPanel panelCentro = new JPanel(new CardLayout());
        panelCentro.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(corRaposa, 2), 
                "Dados do Conteúdo", TitledBorder.LEFT, TitledBorder.TOP, null, corRaposa));

        // Sub-painel A: Campos Dinâmicos (Para arquivos .out)
        panelCamposDinamicos = new JPanel();
        panelCamposDinamicos.setLayout(new BoxLayout(panelCamposDinamicos, BoxLayout.Y_AXIS));
        scrollCamposDinamicos = new JScrollPane(panelCamposDinamicos);
        panelCentro.add(scrollCamposDinamicos, "MODO_OUT");

        // Sub-painel B: Caixa de Texto Livre (Para arquivos .txt legados)
        areaTextoLegado = new JTextArea();
        areaTextoLegado.setFont(new Font("Monospaced", Font.PLAIN, 14));
        scrollTxtLegado = new JScrollPane(areaTextoLegado);
        panelCentro.add(scrollTxtLegado, "MODO_TXT");

        add(panelCentro, BorderLayout.CENTER);

        // BOTÃO
        btnImprimir = new JButton("GERAR E IMPRIMIR");
        btnImprimir.setBackground(corRaposa);
        btnImprimir.setForeground(Color.WHITE);
        btnImprimir.setFont(new Font("Arial", Font.BOLD, 16));
        btnImprimir.setPreferredSize(new Dimension(0, 60));
        add(btnImprimir, BorderLayout.SOUTH);

        // EVENTOS
        comboTemplates.addActionListener(e -> alternarEGerenciarModo());
        btnImprimir.addActionListener(e -> executarImpressao());

        alternarEGerenciarModo();
    }

    private String[] listarTemplates() {
        File pasta = new File("templates");
        File[] arquivos = pasta.listFiles((dir, nome) -> {
            String low = nome.toLowerCase();
            return low.endsWith(".out") || low.endsWith(".txt");
        });
        if (arquivos == null || arquivos.length == 0) return new String[0];
        return Arrays.stream(arquivos).map(File::getName).sorted().toArray(String[]::new);
    }

    private void alternarEGerenciarModo() {
        String arquivo = (String) comboTemplates.getSelectedItem();
        if (arquivo == null) return;

        CardLayout cl = (CardLayout) scrollCamposDinamicos.getParent().getLayout();

        if (arquivo.toLowerCase().endsWith(".txt")) {
            cl.show(scrollCamposDinamicos.getParent(), "MODO_TXT");
            carregarInterfaceTxtLegado(arquivo);
        } else {
            cl.show(scrollCamposDinamicos.getParent(), "MODO_OUT");
            scanTemplateOut(arquivo);
        }
    }

    private void carregarInterfaceTxtLegado(String arquivo) {
        slotsAtivos.clear();
        areaTextoLegado.setText("");
        StringBuilder sb = new StringBuilder();

        try {
            List<String> linhas = Files.readAllLines(Paths.get("templates", arquivo));
            for (int i = 0; i < linhas.size(); i++) {
                String linha = linhas.get(i);
                if (linha.contains("|")) {
                    String[] partes = linha.split("\\|");
                    String meta = partes[0].trim();
                    String texto = partes.length > 1 ? partes[1].trim() : "";

                    // Ignora linhas vazias padrão do sistema antigo para limpar a tela
                    if (texto.isEmpty() && meta.startsWith("10")) continue;

                    int fonte = 22;
                    try { fonte = Integer.parseInt(meta.split("-")[0].trim()); } catch (Exception e) {}

                    SlotLegado slot = new SlotLegado();
                    slot.linhaIndex = i;
                    slot.tamanhoFonte = fonte;
                    slotsAtivos.add(slot);

                    sb.append(texto).append("\n");
                }
            }
            areaTextoLegado.setText(sb.toString());
        } catch (Exception e) {
            areaTextoLegado.setText("Erro ao ler TXT legado: " + e.getMessage());
        }
    }

    private void scanTemplateOut(String arquivo) {
        panelCamposDinamicos.removeAll();
        mapaInputs.clear();

        try {
            String conteudo = new String(Files.readAllBytes(Paths.get("templates", arquivo)));
            Matcher m = Pattern.compile("#[A-Z0-9]+").matcher(conteudo);
            Set<String> tags = new TreeSet<>();
            while (m.find()) { tags.add(m.group()); }

            for (String tag : tags) {
                JPanel row = new JPanel(new BorderLayout(5, 5));
                row.setMaximumSize(new Dimension(1000, 40));
                String labelTexto = DICIONARIO.getOrDefault(tag, "Campo " + tag);
                JLabel label = new JLabel(labelTexto + " (" + tag + "):");
                label.setPreferredSize(new Dimension(180, 25));
                JTextField input = new JTextField();
                mapaInputs.put(tag, input);
                row.add(label, BorderLayout.WEST);
                row.add(input, BorderLayout.CENTER);
                panelCamposDinamicos.add(row);
                panelCamposDinamicos.add(Box.createVerticalStrut(5));
            }
        } catch (Exception e) {
            panelCamposDinamicos.add(new JLabel("Erro ao ler .out: " + e.getMessage()));
        }
        panelCamposDinamicos.revalidate();
        panelCamposDinamicos.repaint();
    }

    private void executarImpressao() {
        String arquivo = (String) comboTemplates.getSelectedItem();
        String impressora = (String) comboImpressoras.getSelectedItem();
        int qtd = Integer.parseInt(txtQtd.getText());

        try {
            if (arquivo.toLowerCase().endsWith(".txt")) {
                // --- MOTOR DE COMPILAÇÃO TXT LEGADO -> ZPL ---
                String[] linhasDigitadas = areaTextoLegado.getText().split("\n");
                StringBuilder zpl = new StringBuilder("^XA\n");

                int fatorY = 5; // Fator multiplicador de linhas para pontos Zebra
                int larguraColuna = 260; // Deslocamento para impressão em 3 colunas horizontais

                for (int i = 0; i < slotsAtivos.size(); i++) {
                    if (i >= linhasDigitadas.length) break;

                    SlotLegado slot = slotsAtivos.get(i);
                    String texto = linhasDigitadas[i].trim();
                    if (texto.isEmpty()) continue;

                    int y = (slot.linhaIndex * fatorY) + 30; // Converte índice da linha em coordenada Y
                    int h = slot.tamanhoFonte;

                    // Replica o texto nas 3 colunas horizontais do rolo de etiquetas
                    for (int c = 0; c < 3; c++) {
                        int x = 20 + (c * larguraColuna);
                        zpl.append(String.format("^FO%d,%d^A0N,%d,%d^CI13^FD%s^FS\n", x, y, h, h, texto));
                    }
                }
                zpl.append("^PQ").append(qtd).append("\n^XZ");
                MotorEtiquetaZebra.enviarParaImpressora(zpl.toString(), impressora);

            } else {
                // --- PROCESSAMENTO PADRÃO DOS ARQUIVOS .OUT ---
                String zpl = new String(Files.readAllBytes(Paths.get("templates", arquivo)));
                for (Map.Entry<String, JTextField> entry : mapaInputs.entrySet()) {
                    zpl = zpl.replace(entry.getKey(), entry.getValue().getText());
                }
                zpl = zpl.replaceAll("\\^PQ\\d+", "^PQ" + qtd);
                MotorEtiquetaZebra.enviarParaImpressora(zpl, impressora);
            }
            JOptionPane.showMessageDialog(this, "Impressão enviada!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
        }
    }

    private String[] listarImpressoras() {
        PrintService[] servicos = PrintServiceLookup.lookupPrintServices(null, null);
        if (servicos.length == 0) return new String[]{"Nenhuma impressora instalada"};
        return Arrays.stream(servicos).map(PrintService::getName).toArray(String[]::new);
    }

    private void selecionarImpressoraPadrao() {
        PrintService padrao = PrintServiceLookup.lookupDefaultPrintService();
        if (padrao != null) comboImpressoras.setSelectedItem(padrao.getName());
    }

    static class SlotLegado {
        int linhaIndex;
        int tamanhoFonte;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FoxSeniorApp().setVisible(true));
    }
}