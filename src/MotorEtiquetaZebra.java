import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;

public class MotorEtiquetaZebra {

    public static boolean enviarParaImpressora(String zplFinal, String nomeImpressora) throws Exception {
        // Busca todas as impressoras do sistema
        PrintService[] servicos = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService servicoEscolhido = null;

        // Varre a lista até achar a impressora com o nome que o usuário selecionou na tela
        for (PrintService servico : servicos) {
            if (servico.getName().equalsIgnoreCase(nomeImpressora)) {
                servicoEscolhido = servico;
                break;
            }
        }

        if (servicoEscolhido == null) {
            throw new RuntimeException("A impressora '" + nomeImpressora + "' não está mais disponível ou offline.");
        }

        // Envio Silencioso (Direto para o hardware)
        DocPrintJob job = servicoEscolhido.createPrintJob();
        byte[] bytesZpl = zplFinal.getBytes();
        Doc doc = new SimpleDoc(bytesZpl, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        job.print(doc, new HashPrintRequestAttributeSet());
        
        return true; 
    }
}