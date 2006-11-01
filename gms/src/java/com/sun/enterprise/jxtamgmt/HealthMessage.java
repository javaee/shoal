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

package com.sun.enterprise.jxtamgmt;

import net.jxta.document.*;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * A HealthAdvertisement is described as follows
 * <p><pre>
 * &lt;?xml version="1.0"?>
 * &lt;!DOCTYPE jxta:Health>
 * &lt;jxta:System xmlns:jxta="http://jxta.org">
 *   &lt;src> src
 *   &lt;id state=up|shutdown|hibernate|sleep|failed>id&lt;/id>
 * &lt;/jxta:Health>
 * </pre>
 */
public class HealthMessage {
    private List<Entry> entries;
    private PeerID srcID;

    private static final String entryTag = "Entry";
    private static final String srcTag = "src";
    private static final String stateTag = "state";
    private static final String timestampTag = "timestamp";
    /*private static final Logger LOG = JxtaUtil.getLogger(HealthMessage.class.getName());*/

    /**
     * Default Constructor
     */
    public HealthMessage() {
    }

    public HealthMessage(final InputStream stream) throws IOException {
        final StructuredTextDocument doc = (StructuredTextDocument)
                StructuredDocumentFactory.newStructuredDocument(
                        MimeMediaType.XMLUTF8, stream);
        initialize(doc);
    }

    /**
     * Constructor for the HealthMessage object
     *
     * @param srcID   Source PeerID
     * @param entries List of entries
     */
    public HealthMessage(final PeerID srcID, final List<Entry> entries) {
        this.srcID = srcID;
        this.entries = entries;
    }


    /**
     * Construct from a StructuredDocument
     *
     * @param root root element
     */
    public HealthMessage(final Element root) {
        final TextElement doc = (TextElement) root;

        if (!getAdvertisementType().equals(doc.getName())) {
            throw new IllegalArgumentException("Could not construct : " +
                    getClass().getName() + "from doc containing a " + doc.getName());
        }
        initialize(doc);
    }

    /**
     * Returns the XML representation of this message
     *
     * @param asMimeType mime type encoding
     * @return The document
     */
    public Document getDocument(final MimeMediaType asMimeType) {
        final StructuredDocument adv = StructuredDocumentFactory
                .newStructuredDocument(asMimeType, getAdvertisementType());
        if (adv instanceof Attributable) {
            ((Attributable) adv).addAttribute("xmlns:jxta", "http://jxta.org");
        }
        Element e;
        e = adv.createElement(srcTag, getSrcID().toString());
        adv.appendChild(e);

        Entry entry;
        for (Entry entry1 : entries) {
            entry = entry1;
            if (entry.id == null && entry.state == null) {
                //skip bad entries
                continue;
            }
            e = adv.createElement(entryTag);
            adv.appendChild(e);
            ((Attributable) e).addAttribute(stateTag, entry.state);
            ((Attributable) e).addAttribute(timestampTag, entry.timestamp);
            StructuredTextDocument doc = (StructuredTextDocument) entry.adv.getDocument(asMimeType);
            StructuredDocumentUtils.copyElements(adv, e, doc);
        }
        return adv;
    }

    /**
     * gets the entries list
     *
     * @return List The List containing Entries
     */
    public List<Entry> getEntries() {
        return entries;
    }

    /**
     * gets the src id
     *
     * @return Peerid The sender's peer id
     */
    public PeerID getSrcID() {
        return srcID;
    }

    /**
     * Process an individual element from the document.
     *
     * @param doc TextElement to iitialize object from
     */
    private void initialize(final TextElement doc) {

        final Enumeration elements = doc.getChildren();
        TextElement elem;
        URI id;
        while (elements.hasMoreElements()) {
            elem = (TextElement) elements.nextElement();
            if (elem.getName().equals(srcTag)) {
                try {
                    id = new URI(elem.getTextValue());
                    setSrcID((PeerID) IDFactory.fromURI(id));
                } catch (URISyntaxException badID) {
                    throw new IllegalArgumentException(
                            "unknown ID format in advertisement: "
                                    + badID.getLocalizedMessage()
                                    + ' ' + elem.getTextValue());
                } catch (ClassCastException badID) {
                    throw new IllegalArgumentException(
                            "Id is not a known id type: " +
                                    badID.getLocalizedMessage()
                                    + ' ' + elem.getTextValue());
                }
                continue;
            }

            if (elem.getName().equals(entryTag)) {
                String state = "NA";
                final Attribute stateAttr = ((Attributable) elem).getAttribute(stateTag);
                if (stateAttr != null) {
                    state = stateAttr.getValue();
                }

                for (Enumeration each = elem.getChildren(); each.hasMoreElements();) {
                    SystemAdvertisement adv = new
                            SystemAdvertisement((TextElement) each.nextElement());
                    final Entry entry = new Entry(adv, state);
                    add(entry);
                }


            }
        }
        setEntries(entries);
    }

    /**
     * sets the entries list
     *
     * @param list The new entries value
     */
    public void setEntries(final List<Entry> list) {
        this.entries = list;
    }

    public void add(final Entry entry) {
        if (entries == null) {
            entries = new ArrayList<Entry>();
        }
        if (!entries.contains(entry)) {
            entries.add(entry);
        }
    }

    public void remove(final Entry entry) {
        if (entries.contains(entry)) {
            entries.remove(entries.indexOf(entry));
        }
    }

    /**
     * sets the unique id
     *
     * @param id The id
     */
    public void setSrcID(final PeerID id) {
        this.srcID = (id == null ? null : id);
    }

    /**
     * returns the document string representation of this object
     *
     * @return String representation of the of this message type
     */
    public String toString() {
        final StructuredTextDocument doc =
                (StructuredTextDocument) getDocument(MimeMediaType.XMLUTF8);
        return doc.toString();
    }

    /**
     * All messages have a type (in xml this is &#0033;doctype) which
     * identifies the message
     *
     * @return String "jxta:Health"
     */
    public static String getAdvertisementType() {
        return "jxta:Health";
    }


    /**
     * Entries class
     */
    public static final class Entry {
        /**
         * Entry ID entry id
         */
        PeerID id;
        /**
         * Entry adv SystemAdvertisement
         */
        SystemAdvertisement adv;
        /**
         * Entry state
         */
        String state;

        /**
         * Entry timestamp
         */
        String timestamp;

        /**
         * Creates a Entry with id and state
         *
         * @param adv   SystemAdvertisement
         * @param state state value
         */
        public Entry(final SystemAdvertisement adv, final String state) {
            this(adv, state, System.currentTimeMillis());
        }

        public Entry(final SystemAdvertisement adv, final String state, final long timestamp) {
            this.state = state;
            this.adv = adv;
            this.id = (PeerID) adv.getID();
            this.timestamp = Long.toString(timestamp);
        }

        @Override
        public boolean equals(final Object obj) {
            final boolean retval;
            if (this == obj) {
                retval = true;
            } else if (obj == null) {
                retval = false;
            } else {
                retval = id.equals(obj);
            }
            return retval;
        }

        public String toString() {
            return "HealthMessage.Entry: Id = " + id.toString() + "; State = " + state + "; LastTimeStamp = " +
                    timestamp;
        }
    }
}

