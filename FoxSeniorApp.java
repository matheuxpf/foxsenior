import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FoxSeniorApp extends JFrame {

    private JComboBox<String> comboTemplates;
    private JTextField txtQtd;
    private JPanel panelCamposDinamicos;
    private Map<String, JTextField> mapaInputs = new HashMap<>();
    private JButton btnImprimir;
    private final Color corRaposa = new Color(235, 110, 0);

    // Dicionário opcional para nomes amigáveis
    // Dicionário de traduções para linguagem natural
    private final Map<String, String> DICIONARIO = new HashMap<>();

    public FoxSeniorApp() {
        // --- INICIALIZANDO O DICIONARIO ---
        DICIONARIO.put("#C", "Produto");
        DICIONARIO.put("#E", "Sabor");
        DICIONARIO.put("#D", "EAN/Barras");
        DICIONARIO.put("#R", "Lote");
        DICIONARIO.put("#I", "Turno");
        DICIONARIO.put("#Q", "Validade");
        DICIONARIO.put("#B", "Marca");
        DICIONARIO.put("#L1", "Linha Livre 1");
        DICIONARIO.put("#L2", "Linha Livre 2");
        DICIONARIO.put("#L3", "Linha Livre 3");
        DICIONARIO.put("#L4", "Linha Livre 4");
        DICIONARIO.put("#L5", "Linha Livre 5");

        setTitle("FoxSenior - Etiquetas Livres");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // PAINEL SUPERIOR: Escolha do Arquivo
        JPanel panelNorte = new JPanel(new GridLayout(4, 1, 5, 5));
        panelNorte.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelNorte.add(new JLabel("1. Selecione o arquivo de modelo (.out):"));
        comboTemplates = new JComboBox<>(listarArquivosOut());
        panelNorte.add(comboTemplates);
        panelNorte.add(new JLabel("2. Quantidade de etiquetas:"));
        txtQtd = new JTextField("1");
        panelNorte.add(txtQtd);
        add(panelNorte, BorderLayout.NORTH);

        // PAINEL CENTRAL: Campos detectados no arquivo
        panelCamposDinamicos = new JPanel();
        panelCamposDinamicos.setLayout(new BoxLayout(panelCamposDinamicos, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(panelCamposDinamicos);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(corRaposa, 2), 
                "Campos Detectados no Template", TitledBorder.LEFT, TitledBorder.TOP, null, corRaposa));
        add(scroll, BorderLayout.CENTER);

        // BOTÃO
        btnImprimir = new JButton("GERAR E IMPRIMIR");
        btnImprimir.setBackground(corRaposa);
        btnImprimir.setForeground(Color.WHITE);
        btnImprimir.setFont(new Font("Arial", Font.BOLD, 16));
        btnImprimir.setPreferredSize(new Dimension(0, 60));
        add(btnImprimir, BorderLayout.SOUTH);

        // EVENTOS
        comboTemplates.addActionListener(e -> scanTemplate());
        btnImprimir.addActionListener(e -> executarImpressao());

        scanTemplate(); // Inicializa o primeiro
    }

    private void scanTemplate() {
        panelCamposDinamicos.removeAll();
        mapaInputs.clear();
        String arquivo = (String) comboTemplates.getSelectedItem();
        if (arquivo == null) return;

        try {
            String conteudo = new String(Files.readAllBytes(Paths.get(arquivo)));
            // REGEX Ninja: Busca qualquer # seguido de uma letra ou número
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
            panelCamposDinamicos.add(new JLabel("Erro ao ler arquivo: " + e.getMessage()));
        }
        panelCamposDinamicos.revalidate();
        panelCamposDinamicos.repaint();
    }

    private void executarImpressao() {
        try {
            String arquivo = (String) comboTemplates.getSelectedItem();
            String zpl = new String(Files.readAllBytes(Paths.get(arquivo)));

            // Substitui TUDO que foi mapeado
            for (Map.Entry<String, JTextField> entry : mapaInputs.entrySet()) {
                zpl = zpl.replace(entry.getKey(), entry.getValue().getText());
            }

            // Fix Quantidade
            int qtd = Integer.parseInt(txtQtd.getText());
            zpl = zpl.replaceAll("\\^PQ\\d+", "^PQ" + qtd);

            // Chama o motor (agora como public)
            MotorEtiquetaZebra.enviarParaImpressora(zpl);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
        }
    }

    private String[] listarArquivosOut() {
        File pasta = new File(".");
        File[] arquivos = pasta.listFiles((dir, nome) -> nome.toLowerCase().endsWith(".out"));
        if (arquivos == null || arquivos.length == 0) return new String[0];
        return Arrays.stream(arquivos).map(File::getName).toArray(String[]::new);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FoxSeniorApp().setVisible(true));
    }
}