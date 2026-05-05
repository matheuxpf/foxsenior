import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Type;

// Importações da biblioteca Gson
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class FoxSeniorApp extends JFrame {

    private JComboBox<Produto> comboProduto;
    private JTextField txtLote, txtTurno, txtValidade;
    private JButton btnImprimir;
    private List<Produto> listaProdutos;

    public FoxSeniorApp() {
        // 1. Carrega o JSON antes de desenhar a tela
        carregarProdutosDoJson();

        // 2. Configurações da Janela
        setTitle("FoxSenior - Impressão de Etiquetas");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(6, 2, 10, 10));

        // 3. Inicializando Componentes com os dados do JSON
        add(new JLabel(" Produto:"));
        // Transforma a lista de produtos num Array que o ComboBox entende
        comboProduto = new JComboBox<>(listaProdutos.toArray(new Produto[0]));
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

        add(new JLabel("")); 
        btnImprimir = new JButton("IMPRIMIR ETIQUETA");
        btnImprimir.setBackground(new Color(34, 139, 34)); 
        btnImprimir.setForeground(Color.WHITE);
        btnImprimir.setFont(new Font("Arial", Font.BOLD, 14));
        add(btnImprimir);

        // Ação do Botão
        btnImprimir.addActionListener(e -> dispararImpressao());
    }

    private void carregarProdutosDoJson() {
        try {
            // Lê o arquivo de texto
            String json = new String(Files.readAllBytes(Paths.get("etiq.json")));
            
            // O Gson faz a mágica de converter o Texto na nossa Lista de Produtos
            Gson gson = new Gson();
            
            // CORREÇÃO: Usando o nome completo (java.lang.reflect.Type) para evitar confusão com o JFrame
            java.lang.reflect.Type tipoLista = new TypeToken<List<Produto>>(){}.getType();
            
            listaProdutos = gson.fromJson(json, tipoLista);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erro ao ler o etiq.json. O arquivo existe e está bem formatado?\n" + e.getMessage());
            System.exit(1); // Fecha o app se não achar o banco
        }
    }

    private void dispararImpressao() {
        // Pega o Objeto Produto inteiro que o usuário selecionou na tela
        Produto selecionado = (Produto) comboProduto.getSelectedItem();
        
        String lote = txtLote.getText();
        String turno = txtTurno.getText();
        String validade = txtValidade.getText();

        // Passa as variáveis fixas daquele produto específico para o motor
        Map<String, String> produtoMock = new HashMap<>(selecionado.variaveisFixas);

        try {
            // Usa o template amarrado no JSON dinamicamente
            MotorEtiquetaZebra.imprimirEtiqueta(selecionado.template, produtoMock, lote, turno, validade);
            JOptionPane.showMessageDialog(this, "Comando enviado para a impressora com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new FoxSeniorApp().setVisible(true);
        });
    }

    // --- A nossa "Fôrma" do JSON ---
    // O Gson lê o JSON e preenche esta classe automaticamente se os nomes baterem.
    static class Produto {
        String id;
        String nomeProduto;
        String template;
        Map<String, String> variaveisFixas;

        // O toString diz ao ComboBox da tela o que ele deve mostrar como texto
        @Override
        public String toString() {
            return nomeProduto;
        }
    }
}