import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class FoxSeniorApp extends JFrame {

    private JComboBox<String> comboTemplates;
    private JComboBox<Produto> comboProduto;
    private JTextField txtMarca, txtEAN, txtLote, txtTurno, txtValidade, txtQtd;
    private JButton btnImprimir;
    private List<Produto> listaProdutos;

    public FoxSeniorApp() {
        carregarProdutosDoJson();

        setTitle("FoxSenior - Impressão de Etiquetas");
        setSize(550, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // PAINEL SUPERIOR: Seleção de Template e Item
        JPanel panelTopo = new JPanel(new GridLayout(4, 1, 5, 5));
        panelTopo.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        
        panelTopo.add(new JLabel("Modelos de Etiquetas (.OUT):"));
        comboTemplates = new JComboBox<>(listarArquivosOut());
        panelTopo.add(comboTemplates);

        panelTopo.add(new JLabel("Item (Produto):"));
        comboProduto = new JComboBox<>(listaProdutos.toArray(new Produto[0]));
        panelTopo.add(comboProduto);
        
        add(panelTopo, BorderLayout.NORTH);

        // PAINEL CENTRAL: Dados Visuais e Variáveis
        JPanel panelCentro = new JPanel(new GridLayout(4, 2, 10, 10));
        panelCentro.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Dados da Produção", TitledBorder.LEFT, TitledBorder.TOP));

        panelCentro.add(new JLabel("Marca:"));
        txtMarca = new JTextField();
        txtMarca.setEditable(false); // Travado para digitação
        panelCentro.add(txtMarca);

        panelCentro.add(new JLabel("Cod. EAN-13:"));
        txtEAN = new JTextField();
        txtEAN.setEditable(false); // Travado para digitação
        panelCentro.add(txtEAN);

        panelCentro.add(new JLabel("Numero do Lote:"));
        txtLote = new JTextField("LOTE-001");
        panelCentro.add(txtLote);

        panelCentro.add(new JLabel("Turno (1-A, 2-B...):"));
        txtTurno = new JTextField("1-A");
        panelCentro.add(txtTurno);

        panelCentro.add(new JLabel("Data de Validade:"));
        txtValidade = new JTextField("11/2026");
        panelCentro.add(txtValidade);

        panelCentro.add(new JLabel("Quantidade Etiq.:"));
        txtQtd = new JTextField("1");
        panelCentro.add(txtQtd);

        // Container para dar uma margem no centro
        JPanel marginPanel = new JPanel(new BorderLayout());
        marginPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        marginPanel.add(panelCentro, BorderLayout.CENTER);
        add(marginPanel, BorderLayout.CENTER);

        // PAINEL INFERIOR: Botão
        JPanel panelBaixo = new JPanel();
        btnImprimir = new JButton("OK - IMPRIMIR");
        btnImprimir.setPreferredSize(new Dimension(200, 40));
        btnImprimir.setFont(new Font("Arial", Font.BOLD, 14));
        panelBaixo.add(btnImprimir);
        add(panelBaixo, BorderLayout.SOUTH);

        // LÓGICA DE EVENTOS
        btnImprimir.addActionListener(e -> dispararImpressao());
        
        // Listener: Quando mudar o produto, atualiza a tela
        comboProduto.addActionListener(e -> preencherDadosTela());
        
        // Força o preenchimento da tela ao abrir pela primeira vez
        preencherDadosTela(); 
    }

    private String[] listarArquivosOut() {
        File pasta = new File(".");
        File[] arquivos = pasta.listFiles((dir, nome) -> nome.toLowerCase().endsWith(".out"));
        if (arquivos == null || arquivos.length == 0) return new String[]{"Nenhum modelo .out encontrado"};
        
        String[] nomes = new String[arquivos.length];
        for (int i = 0; i < arquivos.length; i++) {
            nomes[i] = arquivos[i].getName();
        }
        return nomes;
    }

    private void carregarProdutosDoJson() {
        try {
            String json = new String(Files.readAllBytes(Paths.get("etiq.json")));
            Gson gson = new Gson();
            java.lang.reflect.Type tipoLista = new TypeToken<List<Produto>>(){}.getType();
            listaProdutos = gson.fromJson(json, tipoLista);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erro ao ler etiq.json\n" + e.getMessage());
            System.exit(1);
        }
    }

    private void preencherDadosTela() {
        Produto p = (Produto) comboProduto.getSelectedItem();
        if (p != null) {
            // Atualiza os campos travados lendo do JSON
            txtMarca.setText(p.variaveisFixas.getOrDefault("#B", "N/A"));
            txtEAN.setText(p.variaveisFixas.getOrDefault("#D", "N/A"));
            
            // Auto-seleciona o template, mas permite que o usuário mude
            comboTemplates.setSelectedItem(p.template);
        }
    }

    private void dispararImpressao() {
        Produto selecionado = (Produto) comboProduto.getSelectedItem();
        String lote = txtLote.getText();
        String turno = txtTurno.getText();
        String validade = txtValidade.getText();
        String templateEscolhido = (String) comboTemplates.getSelectedItem();

        Map<String, String> produtoMock = new HashMap<>(selecionado.variaveisFixas);

        try {
            boolean enviou = MotorEtiquetaZebra.imprimirEtiqueta(templateEscolhido, produtoMock, lote, turno, validade);
            if (enviou) {
                JOptionPane.showMessageDialog(this, "Etiqueta enviada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FoxSeniorApp().setVisible(true));
    }

    static class Produto {
        String id;
        String nomeProduto;
        String template;
        Map<String, String> variaveisFixas;

        @Override
        public String toString() {
            return nomeProduto;
        }
    }
}