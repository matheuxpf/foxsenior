import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

public class MotorEtiquetaZebra {
    public static boolean imprimirEtiqueta(String caminhoTemplate, Map<String, String> vars, String lote, String turno, String validade, int qtd) throws Exception {
        String layoutZpl = new String(Files.readAllBytes(Paths.get(caminhoTemplate)));

        // Substituições básicas
        layoutZpl = layoutZpl.replace("#R", lote);
        layoutZpl = layoutZpl.replace("#I", turno);
        layoutZpl = layoutZpl.replace("#Q", validade);
        
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            layoutZpl = layoutZpl.replace(entry.getKey(), entry.getValue());
        }

        // CORREÇÃO DA QUANTIDADE: Busca o comando ^PQ e substitui
        // O regex garante que pegamos ^PQ seguido de qualquer número e trocamos pela nossa qtd
        layoutZpl = layoutZpl.replaceAll("\\^PQ\\d+", "^PQ" + qtd);

        return enviarParaImpressora(layoutZpl);
    }

    public static boolean enviarParaImpressora(String zplFinal) throws Exception {
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