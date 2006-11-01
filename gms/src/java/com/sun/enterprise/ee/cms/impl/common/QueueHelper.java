/*
 * Copyright 2004-2005 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */
 /*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://shoal.dev.java.net/public/CDDLv1.0.html
 *
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */
package com.sun.enterprise.ee.cms.impl.common;

import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Bounded Buffer class inspired by Array based Bounded Buffer
 * as sampled in the book Concurrent Programming in Java: Design Principles
 * and Patterns by Doug Lea
 * @author Shreedhar Ganapathy
 * Date: Jan 22, 2004
 * @version $Revision$
 */
public class QueueHelper {
	private SignalPacket[] signals;
	private int putPtr = 0;
	private int takePtr = 0;
	private int usedSlots = 0;
    private Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private static final int BUFSIZE = 100;

    public QueueHelper() {
	    setBufferSize(BUFSIZE);
    }

	final void setBufferSize(final int bufsize){
		final int bsize;
		if(bufsize >=0){
			bsize=bufsize;
		}
		else{
			bsize = 5;
		}
		signals = new SignalPacket[bsize];
	}

	synchronized void put(final SignalPacket signalPacket){
		while(usedSlots == signals.length){
			try {
				wait();
			}
			catch (InterruptedException e) {
                logger.log(Level.FINEST, e.getLocalizedMessage());
			}
		}
		signals[putPtr] = signalPacket;
		putPtr = (putPtr + 1) % signals.length;
		if(usedSlots++ == 0){
			notifyAll();
		}
	}

	synchronized SignalPacket take(){
		final SignalPacket signal;
		while (usedSlots == 0) {
			try {
				wait();
			}
			catch (InterruptedException e) {
                logger.log(Level.FINEST, e.getLocalizedMessage());
			}
		}

		signal = signals[takePtr];
		signals[takePtr] = null;
		takePtr = (takePtr+1) % signals.length;

		if(usedSlots-- == signals.length){
			notifyAll();
		}
		return signal;
	}
}
