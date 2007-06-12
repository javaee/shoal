/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
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
