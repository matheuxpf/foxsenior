import javax.print.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

public class MotorEtiquetaZebra {

    // Ponto de partida do teste (a "chave de ignição")
    public static void main(String[] args) {
        System.out.println("Iniciando FoxSenior - Teste de Impressão Raw...");

        Map<String, String> produtoMock = new HashMap<>();
        produtoMock.put("#C", "BASE SALGADINHO");
        produtoMock.put("#E", "SABOR QUEIJO");
        produtoMock.put("#M", "PCT");
        produtoMock.put("#S", "1.000");
        produtoMock.put("#B", "MARCA ELBIS");
        produtoMock.put("#D", "74444");

        String arquivoTemplate = "Etiqueta Martins.out"; 
        
        imprimirEtiqueta(arquivoTemplate, produtoMock, "LOTE-TESTE-01", "1-A", "12/2027");
    }

    // A "engrenagem" que processa as variáveis do produto
    public static void imprimirEtiqueta(String caminhoTemplateOut, Map<String, String> variaveisProduto, String lote, String turno, String validade) {
        try {
            String layoutZpl = new String(Files.readAllBytes(Paths.get(caminhoTemplateOut)));

            Map<String, String> dadosImpressao = new HashMap<>(variaveisProduto);
            dadosImpressao.put("#R", lote);
            dadosImpressao.put("#I", turno);
            dadosImpressao.put("#Q", validade);
            dadosImpressao.put("#H", "04/05/2026");

            for (Map.Entry<String, String> entry : dadosImpressao.entrySet()) {
                layoutZpl = layoutZpl.replace(entry.getKey(), entry.getValue());
            }

            enviarParaImpressora(layoutZpl);
            System.out.println("Etiqueta enviada com sucesso!");

        } catch (Exception e) {
            System.err.println("Erro ao processar etiqueta: " + e.getMessage());
        }
    }

    // O envio direto para a USB
    private static void enviarParaImpressora(String zplFinal) throws PrintException {
        System.out.println("\n--- ZPL GERADO PARA TESTE ---");
        System.out.println(zplFinal);
        System.out.println("-----------------------------\n");


        PrintService impressora = PrintServiceLookup.lookupDefaultPrintService();
        if (impressora == null) {
            throw new RuntimeException("Nenhuma impressora padrão encontrada no Windows.");
        }
        DocPrintJob job = impressora.createPrintJob();
        byte[] bytesZpl = zplFinal.getBytes();
        Doc doc = new SimpleDoc(bytesZpl, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        job.print(doc, null);
    }
}