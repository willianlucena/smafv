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

import javax.media.Time;
import javax.media.control.TrackControl;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.*;

/**Classe CutterBufferDataSource consiste intercalar entre a entrada ea saída do
 * processador. Classes CutterBufferStream opera um resort.
 */
public class CutterBufferDataSource extends PushBufferDataSource {
	private Time[] start;
	private Time[] end;
	private Processor proc;
	private PushBufferDataSource pbds;
	private CutterBufferStream[] streams;
	private MediaLocator out;

	public CutterBufferDataSource(Time[] start, Time end[], Processor proc, MediaLocator out) {
		super();
		this.proc = proc;
		this.pbds = (PushBufferDataSource)proc.getDataOutput();
		this.start = start;
		this.end = end;
		TrackControl tcs[] = proc.getTrackControls();
	    PushBufferStream pbs[] = pbds.getStreams();
	    this.out = out;
	    streams = new CutterBufferStream[pbs.length];
	    for (int i = 0; i < pbs.length; i++) {
	    	streams[i] = new CutterBufferStream(start,end, tcs[i],pbs[i]);
	    }
	}

	// pøekrytí metod, nìkteré není nutné pro naše úèely ani vyplòovat kódem :-)
	public PushBufferStream[] getStreams() {
		return this.streams;
	}


	public String getContentType() {
		return this.pbds.getContentType();
	}


	public void connect() throws IOException {
		// TODO Auto-generated method stub

	}


	public void disconnect() {
		// TODO Auto-generated method stub

	}


	public void start() throws IOException {
		proc.start();
		pbds.start();

	}


	public void stop() throws IOException {
		// TODO Auto-generated method stub

	}

	public Object getControl(String arg0) {
		return pbds.getControl(arg0);
	}


	public Object[] getControls() {
		return pbds.getControls();
	}


	public Time getDuration() {
		return pbds.getDuration();
	}

}
