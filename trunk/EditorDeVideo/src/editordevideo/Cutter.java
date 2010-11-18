package editordevideo;

/**
 *
 * @author Maxwell
 */
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.*;

import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.datasink.*;

import java.io.*;

public class Cutter implements DataSinkListener {

    Processor proc;
    MediaLocator inML;
    DataSource inDS;
    ProcessorWaiter pw;
    DataSink filewriter;

    /*probìhne construtor que cria o processador inicial.
     * Kit de primeiros socorros para todas as exceções possíveis, elegeu apenas
     * um acesso à rede, especialmente para esta finalidade.
     */
    public Cutter(String inputfile) throws CutterException {
        File finput = new File(inputfile);
        if (!finput.canRead()) {
            if (finput.exists()) {
                throw new CutterException("Não é possivel Ler o Arquivo :" + inputfile);
            } else {
                throw new CutterException("Arquivo " + inputfile + " Negado!");
            }
        }
        inML = new MediaLocator("file:///C:/exemplo.mpg");
        //inML = new MediaLocator(inputfile);'
        //inDS = new DataSource(inML);
        //System.out.println(inML);
        try {
            //proc = Manager.createRealizedProcessor(new ProcessorModel)
            proc = Manager.createProcessor(this.inML);
        } catch (IOException e) {
            throw new CutterException("Arquivo " + inML.toString() + " encontrado", e);
        } catch (NoProcessorException e) {
            throw new CutterException("Não foi possivel criar um Processor do " + inML.toString(), e);
        }

        ProcessorWaiter pwIN = new ProcessorWaiter(proc);
        if (!pwIN.waitForState(Processor.Configured)) {
            throw new CutterException("Não foi possível configurar o processor " + inML);
        }
        //Definir o formato de exibição de saída
        ContentDescriptor cd = new FileTypeDescriptor(FileTypeDescriptor.RAW);
        TrackControl tc[] = proc.getTrackControls();
        int tamTC = tc.length;
        System.out.println(tamTC);
        System.out.println(tc[0].getFormat().getEncoding());

        if (!this.setTrackFormat(new VideoFormat(VideoFormat.JPEG), tc[0])) {
            System.err.println("NÃO É POSSÍVEL CONVERTER O VÍDEO PARA JPEG");
        }
        try {
            if (!this.setTrackFormat(new AudioFormat(AudioFormat.LINEAR), tc[0])) {
                System.err.println("NÃO É POSSÍVEL CONVERTER O AUDIO PARA LINEAR");
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        if (!pwIN.waitForState(Processor.Realized)) {
            throw new CutterException("Processor não mudou de estado para " + inML);
        }
        //Qualidade das faixas na configuração de vídeo
        this.setJPEGQuality(proc, 0.3f);
    }

    /**
     * O método tenta definir o formato de saída é selecionada por parar
     * parâmetro f, que é dado pelo TC parâmetro. O formato escolhido é promovida
     * entre os formatos suportados.
     *
     */
    private boolean setTrackFormat(Format f, TrackControl tc) {
        //Formatos
        Format formats[] = tc.getSupportedFormats();
        for (int i = 0; i < formats.length; i++) {
            if (formats[i].matches(f)) {
                tc.setFormat(f);
                return true;
            }
        }
        return false;
    }

    public synchronized void dataSinkUpdate(DataSinkEvent event) {
        //Se não houver mais dados para fechar o arquivo de saída
        if (event instanceof EndOfStreamEvent) {
            filewriter.close();
        }

    }

    protected void setJPEGQuality(Player p, float val) {
        Control cs[] = p.getControls();
        QualityControl qc = null;
        VideoFormat jpegFmt = new VideoFormat(VideoFormat.JPEG);

//    	Através de todos os drivers (controles) para encontrar controlo da qualidade
//o codificador JPEG.
        for (int i = 0; i < cs.length; i++) {

            if (cs[i] instanceof QualityControl
                    && cs[i] instanceof Owned) {
                Object owner = ((Owned) cs[i]).getOwner();

                // Verifique se o proprietário é um Codec.
                // Em seguida, verifique o formato de saída.
                if (owner instanceof Codec) {
                    Format fmts[] = ((Codec) owner).getSupportedOutputFormats(null);
                    for (int j = 0; j < fmts.length; j++) {
                        if (fmts[j].matches(jpegFmt)) {
                            qc = (QualityControl) cs[i];
                            qc.setQuality(val);
                            System.err.println("- Defina a qualidade de "
                                    + val + " em " + qc);
                            break;
                        }
                    }
                }
                if (qc != null) {
                    break;

                }
            }
        }
    }

    public void dataSourceInfo(DataSource ds) {
        System.out.println(" DATASOURCE INFO: contentType>" + ds.getContentType());
        System.out.println(" DATASOURCE INFO: locator>" + ds.getLocator());
        System.out.println(" DATASOURCE INFO: class>" + ds.getClass());
        if (ds instanceof PushBufferDataSource) {
            PushBufferDataSource pbds = (PushBufferDataSource) ds;
            System.out.println(" DATASOURCE INFO PBDS: pocet Streamu>" + pbds.getStreams().length);
            PushBufferStream streams[] = pbds.getStreams();
            for (int i = 0; i < streams.length; i++) {
                System.out.println(" DATASOURCE INFO: PBDS Stream " + i + "> " + streams[i].getContentDescriptor());
                System.out.println(" DATASOURCE INFO: PBDS Stream " + i + "> " + streams[i].getClass());
                System.out.println(" DATASOURCE INFO: PBDS Stream " + i + "> " + streams[i].getFormat());
            }
        }
    }
    /*Este método é realizado søih. Como os parâmetros são passados em dois
    campos, de início e de fim a fim e contém poèáteèní campo Fácil escolhido.
    Fácil de ser seøazeny trás como eles vão e eles não devem pøekrývat.
    Determina o destino do arquivo de saída.
     */

    public void cut(Time[] start, Time[] end, MediaLocator destination) {
        Processor outp = null;
        //institui intercalar vnoøené, que se preocupam com queda de dados nepotøebných
        DataSource ds = new CutterBufferDataSource(start, end, proc, destination);
        this.dataSourceInfo(ds);
        try {
            outp = Manager.createProcessor(ds);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (NoProcessorException e) {
            e.printStackTrace();
            return;
        }

        ProcessorWaiter pwout = new ProcessorWaiter(outp);
        if (!pwout.waitForState(Processor.Configured)) {
            System.err.println("Falha ao configurar o PipeLine de envio");
        }
        // Define o formato do arquivo de saída
        ContentDescriptor cdout = new FileTypeDescriptor(FileTypeDescriptor.WAVE);
        outp.setContentDescriptor(cdout);

        TrackControl tc[] = outp.getTrackControls();
        // Define o formato das trilhas de vídeo
        if (!this.setTrackFormat(new VideoFormat(VideoFormat.MJPG), tc[0])) {
            System.err.println("NÃO É POSSIVEL COVERTER O VÍDEO PARA MJPEG - outro");
        } else {
            System.out.println("Deu certo outro");
        }
        try {
            if (!this.setTrackFormat(new AudioFormat(AudioFormat.ULAW, 8000, 8, 1), tc[0])) {
                System.err.println("NÃO É POSSÍVEL CONVERTER O AUDIO PARA ULAW");
            } else {
                System.out.println("Deu Certo Outro");
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("asdada");
        }

        if (!pwout.waitForState(Processor.Realized)) {
            System.err.println("Falha ao executar o pipeline de envio");
            return;
        }

        // Saída do Arquivo
        try {
            filewriter = Manager.createDataSink(outp.getDataOutput(), destination);
        } catch (NoDataSinkException e) {
            System.err.println("Não é possível criar o DataSink para " + inML + " e " + destination);
            //e.printStackTrace();
            return;
        }

        //Ouvintes Registrados
        proc.addControllerListener(new EOMListener(proc));
        outp.addControllerListener(new EOMListener(outp));
        filewriter.addDataSinkListener(this);

        if (!pwout.waitForState(Processor.Prefetched)) {
            System.err.println("saída Prefeteched falhou");
        }

        pw = new ProcessorWaiter(proc);
        if (!pw.waitForState(Processor.Started)) {
            System.err.println("Decodificador Prefeteched não terá êxito");
        }

        // odstartování støihu
        try {
            filewriter.open();
            filewriter.start();
            outp.start();

        } catch (IOException e) {
            System.err.println("Não é possivel escrever o arquivo: " + destination);
            return;
        }
    }

    /**Destino do Processor
     */
    class EOMListener implements ControllerListener {

        Processor p;

        public EOMListener(Processor p) {
            this.p = p;
        }

        public void controllerUpdate(ControllerEvent ce) {

            if (ce instanceof StopEvent) {
                p.close();
            }
            System.out.println(ce.getClass().toString());
        }
    }

    public void controllerUpdate(ControllerEvent ce) {
    }

    /** A takto se støíhá :-)
     */
    public static void main(String[] args) {
        try {
            Cutter cut = new Cutter("C:\\exemplo.mpg");
            Time[] start = {new Time(0.0d)};
            Time[] end = {new Time(10.0d)};
//            FileOutputStream saida;
//            File file = new File("E:/oot6.txt");
//            try {
//                saida = new FileOutputStream(file);
//                String a = "atsaftsafatsfdtasda";
//                saida.write(a.getBytes());
//                saida.close();
//            } catch (FileNotFoundException ex) {
//                Logger.getLogger(Cutter.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (IOException e) {
//                Logger.getLogger(Cutter.class.getName()).log(Level.SEVERE, null, e);
//            }

            cut.cut(start, end, new MediaLocator("file:///C:/oot6.wav"));
            System.out.println("done");
        } catch (CutterException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
