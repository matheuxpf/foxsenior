import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

public class MotorEtiquetaZebra {

    public static boolean imprimirEtiqueta(String caminhoTemplateOut, Map<String, String> variaveisProduto, String lote, String turno, String validade) throws Exception {
        String layoutZpl = new String(Files.readAllBytes(Paths.get(caminhoTemplateOut)));

        Map<String, String> dadosImpressao = new HashMap<>(variaveisProduto);
        dadosImpressao.put("#R", lote);
        dadosImpressao.put("#I", turno);
        dadosImpressao.put("#Q", validade);
        dadosImpressao.put("#H", "05/05/2026"); // Data de Hoje mockada para o MVP

        for (Map.Entry<String, String> entry : dadosImpressao.entrySet()) {
            layoutZpl = layoutZpl.replace(entry.getKey(), entry.getValue());
        }

        return enviarParaImpressora(layoutZpl);
    }

    private static boolean enviarParaImpressora(String zplFinal) throws Exception {
        // Busca todas as impressoras instaladas no Windows
        PrintService[] servicos = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService impressoraPadrao = PrintServiceLookup.lookupDefaultPrintService();

        if (servicos.length == 0) {
            throw new RuntimeException("Nenhuma impressora encontrada no sistema.");
        }

        // Abre a janela nativa do Windows para seleção de impressora
        PrintRequestAttributeSet atributos = new HashPrintRequestAttributeSet();
        PrintService servicoEscolhido = ServiceUI.printDialog(
                null, 200, 200, servicos, impressoraPadrao, null, atributos);

        // Se o usuário clicou em OK e escolheu uma impressora
        if (servicoEscolhido != null) {
            DocPrintJob job = servicoEscolhido.createPrintJob();
            byte[] bytesZpl = zplFinal.getBytes();
            Doc doc = new SimpleDoc(bytesZpl, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            job.print(doc, atributos);
            return true; 
        } else {
            // Usuário cancelou a impressão na tela do Windows
            return false; 
        }
    }
}