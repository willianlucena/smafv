/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package editordevideo;

/**
 *
 * @author Maxwell
 */
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Processor;
/**Classe usada para esperar o Processor até atingirem o estado desejado.
 */
public class ProcessorWaiter implements ControllerListener {
	Processor p;
	boolean error = false;

	ProcessorWaiter(Processor p) {
	    this.p = p;
	    p.addControllerListener(this);
	}

	public synchronized boolean waitForState(int state) {

	    switch (state) {
	    case Processor.Configured:
		p.configure(); break;
	    case Processor.Realized:
		p.realize(); break;
	    case Processor.Prefetched:
		p.prefetch(); break;
	    case Processor.Started:
		p.start(); break;
	    }

	    while (p.getState() < state && !error) {
		try {
		    wait(1000);
		} catch (Exception e) {
		}
	    }

	    return !(error);
	}

	public void controllerUpdate(ControllerEvent ce) {
	    if (ce instanceof ControllerErrorEvent) {

	    	error = true;
	    }
	    synchronized (this) {
	    	notifyAll();
	    }
	}

}
