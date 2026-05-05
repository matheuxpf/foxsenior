import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FoxSeniorApp extends JFrame {

    private JComboBox<String> comboProduto;
    private JTextField txtLote, txtTurno, txtValidade;
    private JButton btnImprimir;

    public FoxSeniorApp() {
        // Configurações da Janela
        setTitle("FoxSenior - Impressão de Etiquetas");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(6, 2, 10, 10));

        // Inicializando Componentes
        add(new JLabel(" Produto:"));
        comboProduto = new JComboBox<>(new String[]{"BASE SALGADINHO", "REQUEIJAO"});
        add(comboProduto);

        add(new JLabel(" Lote:"));
        txtLote = new JTextField("LOTE-001");
        add(txtLote);

        add(new JLabel(" Turno:"));
        txtTurno = new JTextField("1-A");
        add(txtTurno);

        add(new JLabel(" Validade (MM/AAAA):"));
        txtValidade = new JTextField("12/2026");
        add(txtValidade);

        add(new JLabel("")); // Espaço vazio para alinhar o botão
        btnImprimir = new JButton("IMPRIMIR ETIQUETA");
        btnImprimir.setBackground(new Color(34, 139, 34)); // Verde industrial
        btnImprimir.setForeground(Color.WHITE);
        btnImprimir.setFont(new Font("Arial", Font.BOLD, 14));
        add(btnImprimir);

        // Ação do Botão
        btnImprimir.addActionListener(e -> dispararImpressao());
    }

    private void dispararImpressao() {
        // 1. Pega os dados que o usuário digitou
        String produtoSelecionado = (String) comboProduto.getSelectedItem();
        String lote = txtLote.getText();
        String turno = txtTurno.getText();
        String validade = txtValidade.getText();

        // 2. Mock do produto (em breve substituiremos pela leitura do JSON)
        Map<String, String> produtoMock = new HashMap<>();
        if ("BASE SALGADINHO".equals(produtoSelecionado)) {
            produtoMock.put("#C", "BASE SALGADINHO");
            produtoMock.put("#E", "SABOR QUEIJO");
            produtoMock.put("#M", "PCT");
            produtoMock.put("#S", "1.000");
            produtoMock.put("#B", "MARCA ELBIS");
            produtoMock.put("#D", "74444");
        } else {
            produtoMock.put("#C", "REQUEIJAO TRADICIONAL");
            produtoMock.put("#E", "SABOR REQUEIJAO");
            produtoMock.put("#M", "CXA");
            produtoMock.put("#S", "28.000");
            produtoMock.put("#B", "MARCA ELBIS");
            produtoMock.put("#D", "10103");
        }

        // 3. Chama o nosso motor para imprimir!
        String arquivoTemplate = "Etiqueta Martins.out"; 
        
        try {
            MotorEtiquetaZebra.imprimirEtiqueta(arquivoTemplate, produtoMock, lote, turno, validade);
            JOptionPane.showMessageDialog(this, "Comando enviado para a impressora com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // Garante que a interface inicie suavemente
        SwingUtilities.invokeLater(() -> {
            new FoxSeniorApp().setVisible(true);
        });
    }
}