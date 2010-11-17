/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package editordevideo;

/**
 *
 * @author Maxwell
 */
import java.io.IOException;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.*;
/** Esta é a maior classe do crédito para støihu. Ao trabalhar com uma banda sonora Calcula o tempo e cai nechtìná data.
 */
public class CutterBufferStream implements PushBufferStream, BufferTransferHandler {
	private Time[] start;
	private Time[] end;
	private TrackControl tc;
	private PushBufferStream pbs;
	private Buffer buffer;
	private int bufferFilled = 0;
	private boolean eos = false;
	private Format format;
	private BufferTransferHandler bth;
	private long timeStamp = 0;
	private long lastTS = 0;
	private int audioLen = 0;
	private int audioElapsed = 0;
	//long start[], end[];
	private boolean startReached[], endReached[];
	private int idx = 0;
	public CutterBufferStream(Time[] start, Time[] end, TrackControl tc, PushBufferStream pbs) {
		super();
		this.end = end;
		this.start = start;
		this.tc = tc;
		this.pbs = pbs;
		startReached = new boolean[start.length];
	    endReached = new boolean[end.length];
	    for (int i = 0; i < start.length; i++) {
	    	startReached[i] = endReached[i] = false;
	    }
		buffer = new Buffer();
	    pbs.setTransferHandler(this);
	}

	public Format getFormat() {
		return pbs.getFormat();
	}

	public void read(Buffer rdBuf) throws IOException {

	    synchronized (buffer) {
			while (bufferFilled == 0) {
			    try {
				buffer.wait();
			    } catch (Exception e) {}
			}
		    }

		    // Copiar dados da Fila
		    Object oldData = rdBuf.getData();

		    rdBuf.copy(buffer);
		    buffer.setData(oldData);

		    // pøepoèet èasových znaèek.

		    if (isRawAudio(rdBuf.getFormat())) {
			// O áudio não comprimido é suficiente para calcular um tempo exato.
					rdBuf.setTimeStamp(computeDuration(audioElapsed, rdBuf
							.getFormat()));
					audioElapsed += buffer.getLength();
				} else if (rdBuf.getTimeStamp() != Buffer.TIME_UNKNOWN) {
					long diff = rdBuf.getTimeStamp() - lastTS;
					lastTS = rdBuf.getTimeStamp();
					if (diff > 0)
						timeStamp += diff;
					rdBuf.setTimeStamp(timeStamp);
		    }

		    synchronized (buffer) {
			bufferFilled = 0;
			buffer.notifyAll();
		    }


	}

	public void setTransferHandler(BufferTransferHandler bth) {
		//jako transferhandler nastavime sama sebe
		this.bth = bth;

	}

	public ContentDescriptor getContentDescriptor() {
		// o formato de saída é o melhor PushbufferStream tempo
		return pbs.getContentDescriptor();
	}

	public long getContentLength() {
		// Infelizmente, o tempo nunca vai saber com antecedência pelo
		return LENGTH_UNKNOWN;
	}

	public boolean endOfStream() {
		// pøíznak final da transferência
		return this.eos;
	}

	public Object[] getControls() {
		return pbs.getControls();
	}

	public Object getControl(String arg0) {
		return pbs.getControl(arg0);
	}
	void processData() {

	    // comprimento da fila sincronizada de um
	    synchronized (buffer) {
		while (bufferFilled == 1) {
		    try {
			buffer.wait();
		    } catch (Exception e) {}
		}
	    }

	    // Leitura da FOnte
	    try {
		pbs.read(buffer);
	    } catch (IOException e) {}

	    format = buffer.getFormat();

	    if (idx >= end.length) {
		// Se você tiver quaisquer seções interessantes vystøiženy
                // O suficiente para definir o fim da transferência pøíznak
			buffer.setOffset(0);
			buffer.setLength(0);
			buffer.setEOM(true);
	    }

	    if (buffer.isEOM())
		eos = true;

	    int len = buffer.getLength();

	    // Se nós não nos importamos com o conteúdo do buffer, ignorá-lo
	    if (checkTimeToSkip(buffer)) {
		// Claro que não se deve esquecer de atualizar o tempo se foi apenas o som.
		if (isRawAudio(buffer.getFormat()))
		    audioLen += len;
		return;
	    }

	    // Tempo de atualização poèítadla
	    if (isRawAudio(buffer.getFormat()))
		audioLen += len;

	    // Ativar processor ler a partir da fonte
	    synchronized (buffer) {
		bufferFilled = 1;
		buffer.notifyAll();
	    }

	    // Dloubnutí do processoru.
	    if (bth != null)
		bth.transferData(this);
	}
	/**
	 * O poèáteèních pøedaného buffer detectada a tempo e final de
         * seções selecionadas, se necessário, em tal data ou se ela é descartada.
	 */
	boolean checkTimeToSkip(Buffer buf) {

	    if (idx >= startReached.length)
		return false;

	    if (!eos && !startReached[idx]) {
		if (!(startReached[idx] = checkStartTime(buf, start[idx].getNanoseconds()))) {
		    return true;
		}
	    }

	    if (!eos && !endReached[idx]) {
		if (endReached[idx] = checkEndTime(buf, end[idx].getNanoseconds())) {
		    idx++;	// mover para o próximo fim-de-linha dois poèátek
		    return true;
		}
	    } else if (endReached[idx]) {
		if (!eos) {
		    return true;
		} else {
		    buf.setOffset(0);
		    buf.setLength(0);
		}
	    }

	    return false;
	}

	 boolean isRawAudio(Format fmt) {
			return (fmt instanceof AudioFormat) &&
				fmt.getEncoding().equalsIgnoreCase(AudioFormat.LINEAR);
		    }
	/**
	 * Verifique buffer seção poèátkùm vùèi interessante
	 */
	boolean checkStartTime(Buffer buf, long startTS) {
	    if (isRawAudio(buf.getFormat())) {
		long ts = computeDuration(audioLen+buf.getLength(),
					buf.getFormat());
		if (ts > startTS) {
		    int len = computeLength(ts - startTS, buf.getFormat());
		    buf.setOffset(buf.getOffset() + buf.getLength() - len);
		    buf.setLength(len);
		    lastTS = buf.getTimeStamp();
		    return true;
		}
	    } else if (buf.getTimeStamp() >= startTS) {
		if (buf.getFormat() instanceof VideoFormat) {
		    // Necessário quadro poèáteèní zjískat, que deve estar faltando-chave
		    if ((buf.getFlags() & Buffer.FLAG_KEY_FRAME) != 0) {
			lastTS = buf.getTimeStamp();
			return true;
		    }
		} else {
		    lastTS = buf.getTimeStamp();
		    return true;
		}
	    }
	    return false;
        }


	/**
	 * Verifique buffer termina vùèi do segmento interessante
	 */
	boolean checkEndTime(Buffer buf, long endTS) {
	    if (isRawAudio(buf.getFormat())) {
		if (computeDuration(audioLen, buf.getFormat()) >= endTS)
		    return true;
		else {
		    long ts = computeDuration(audioLen+buf.getLength(),
					buf.getFormat());
		    if (ts >= endTS) {
			int len = computeLength(ts - endTS, buf.getFormat());
			buf.setLength(buf.getLength() - len);
			// O último buffer deve ser preparado
		    }
		}
	    } else if (buf.getTimeStamp() > endTS) {
		return true;
	    }

	    return false;
	}


	/**
	 * Calcule o comprimento
	 */
	public long computeDuration(int len, Format fmt) {
	    if (!(fmt instanceof AudioFormat))
		return -1;
	    return ((AudioFormat)fmt).computeDuration(len);
	}


	/**
	 * Calcule o comprimento do áudio
	 */
	public int computeLength(long duration, Format fmt) {
	    if (!(fmt instanceof AudioFormat))
		return -1;
	    AudioFormat af = (AudioFormat)fmt;
	    // Pozor na pøeteèení
	    return (int) ((((duration /1000) * (af.getChannels() * af.getSampleSizeInBits()))/1000) * af.getSampleRate() / 8000);
	}


	public synchronized void transferData(PushBufferStream pbs) {
		   processData();
	}

}
