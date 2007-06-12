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


package com.sun.enterprise.jxtamgmt;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Attributable;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import static net.jxta.document.StructuredDocumentFactory.newStructuredDocument;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.TextElement;
import net.jxta.id.ID;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A SystemAdvertisement is described as follows <p/>
 * <p/>
 * <pre>
 * &lt;?xml version="1.0"?>
 * &lt;!DOCTYPE jxta:System>
 * &lt;jxta:System xmlns:jxta="http://jxta.org">
 *   &lt;id>id&lt;/id>
 *   &lt;name>Device Name&lt;/name>
 *   &lt;ip>ip address&lt;/ip>
 *   &lt;hwarch>x86&lt;/hwarch>
 *   &lt;hwvendor>Sun MicroSystems&lt;/hwvendor>
 *   &lt;OSName>&lt;/OSName>
 *   &lt;OSVer>&lt;/OSVer>
 *   &lt;osarch>&lt;/osarch>
 *   &lt;sw>&lt;/sw>
 * &lt;/jxta:System>
 * </pre>
 */
public class SystemAdvertisement extends Advertisement
        implements Comparable, Cloneable, Serializable {
    private String hwarch;
    private String hwvendor;
    private ID id = ID.nullID;
    private String ip;
    private String name;
    private String osname;
    private String osversion;
    private String osarch;
    private HashMap<String, String> customTags = null;

    private static final String OSNameTag = "OSName";
    private static final String OSVersionTag = "OSVer";
    private static final String OSarchTag = "osarch";
    private static final String hwarchTag = "hwarch";
    private static final String hwvendorTag = "hwvendor";
    private static final String idTag = "ID";
    private static final String ipTag = "ip";
    private static final String nameTag = "name";
    //Indexable fields
    private static final String[] fields = {idTag, nameTag, hwarchTag};

    /**
     * Default Constructor
     */
    public SystemAdvertisement() {
    }

    /**
     * Construct from a StructuredDocument
     *
     * @param root Root element
     */
    public SystemAdvertisement(final Element root) {
        final TextElement doc = (TextElement) root;

        if (!getAdvertisementType().equals(doc.getName())) {
            throw new IllegalArgumentException("Could not construct : " +
                    getClass().getName() + "from doc containing a " + doc.getName());
        }
        initialize(doc);
    }

    /**
     * Construct a doc from InputStream
     *
     * @param stream the underlying input stream.
     * @throws IOException if an I/O error occurs.
     */

    public SystemAdvertisement(final InputStream stream) throws IOException {
        final StructuredTextDocument doc = (StructuredTextDocument)
                newStructuredDocument(MimeMediaType.XMLUTF8, stream);
        initialize(doc);
    }


    /**
     * Sets the hWArch attribute of the SystemAdvertisement object
     *
     * @param hwarch The new hWArch value
     */
    public void setHWArch(final String hwarch) {
        this.hwarch = hwarch;
    }

    /**
     * Sets the OSArch attribute of the SystemAdvertisement object
     *
     * @param osarch The new hWArch value
     */
    public void setOSArch(final String osarch) {
        this.osarch = osarch;
    }


    /**
     * Sets the hWVendor attribute of the SystemAdvertisement object
     *
     * @param hwvendor The new hWVendor value
     */
    public void setHWVendor(final String hwvendor) {
        this.hwvendor = hwvendor;
    }

    /**
     * sets the unique id
     *
     * @param id The id
     */
    public void setID(final ID id) {
        this.id = (id == null ? null : id);
    }

    /**
     * Sets the iP attribute of the SystemAdvertisement object
     *
     * @param ip The new iP value
     */
    public void setIP(final String ip) {
        this.ip = ip;
    }

    /**
     * Sets the name attribute of the DeviceAdvertisement object
     *
     * @param name The new name value
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Sets the oSName attribute of the SystemAdvertisement object
     *
     * @param osname The new oSName value
     */
    public void setOSName(final String osname) {
        this.osname = osname;
    }

    /**
     * Sets the oSVersion attribute of the SystemAdvertisement object
     *
     * @param osversion The new oSVersion value
     */
    public void setOSVersion(final String osversion) {
        this.osversion = osversion;
    }

    public void setCustomTags(final Map<String, String> tags) {
        if (customTags == null) {
            customTags = new HashMap<String, String>();
        }
        customTags.putAll(tags);
    }

    public Map<String, String> getCustomTags() {
        return (HashMap<String, String>) customTags.clone();
    }

    public void setCustomTag(final String tag, final String value) {
        if (customTags == null) {
            customTags = new HashMap<String, String>();
        }
        customTags.put(tag, value);
    }

    /**
     * {@inheritDoc}
     *
     * @param asMimeType Document encoding
     * @return The document value
     */
    public Document getDocument(final MimeMediaType asMimeType) {
        final StructuredDocument adv = newStructuredDocument(asMimeType,
                getAdvertisementType());
        if (adv instanceof Attributable) {
            ((Attributable) adv).addAttribute("xmlns:jxta", "http://jxta.org");
        }
        Element e;
        e = adv.createElement(idTag, getID().toString());
        adv.appendChild(e);
        e = adv.createElement(nameTag, getName().trim());
        adv.appendChild(e);
        e = adv.createElement(OSNameTag, getOSName().trim());
        adv.appendChild(e);
        e = adv.createElement(OSVersionTag, getOSVersion().trim());
        adv.appendChild(e);
        e = adv.createElement(OSarchTag, getOSArch().trim());
        adv.appendChild(e);
        e = adv.createElement(ipTag, getIP().trim());
        adv.appendChild(e);
        e = adv.createElement(hwarchTag, getHWArch().trim());
        adv.appendChild(e);
        e = adv.createElement(hwvendorTag, getHWVendor().trim());
        adv.appendChild(e);
        if (customTags != null && !customTags.isEmpty()) {
            for (final String key : customTags.keySet()) {
                e = adv.createElement(key, customTags.get(key));
                adv.appendChild(e);
            }
        }
        return adv;
    }

    /**
     * Gets the hWArch attribute of the SystemAdvertisement object
     *
     * @return The hWArch value
     */
    public String getHWArch() {
        return hwarch;
    }

    /**
     * Gets the OSArch attribute of the SystemAdvertisement object
     *
     * @return The OSArch value
     */
    public String getOSArch() {
        return osarch;
    }

    /**
     * Gets the hWVendor attribute of the SystemAdvertisement object
     *
     * @return The hWVendor value
     */
    public String getHWVendor() {
        return hwvendor;
    }


    /**
     * returns the id of the device
     *
     * @return ID the device id
     */
    public ID getID() {
        return (id == null ? null : id);
    }

    /**
     * Gets the IP attribute of the SystemAdvertisement object
     *
     * @return The IP value
     */
    public String getIP() {
        return ip;
    }

    /**
     * Gets the name attribute of the SystemAdvertisement object
     *
     * @return The name value
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the OSName attribute of the SystemAdvertisement object
     *
     * @return The OSName value
     */
    public String getOSName() {
        return osname;
    }

    /**
     * Gets the OSVersion attribute of the SystemAdvertisement object
     *
     * @return The OSVersion value
     */
    public String getOSVersion() {
        return osversion;
    }

    public String getCustomTagValue(final String tagName)
            throws NoSuchFieldException {
        if (customTags != null) {
            return customTags.get(tagName);
        } else {
            throw new NoSuchFieldException(tagName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SystemAdvertisement clone() throws CloneNotSupportedException {
        try {
            SystemAdvertisement likeMe = (SystemAdvertisement) super.clone();

            likeMe.setID(getID());
            likeMe.setName(getName());
            likeMe.setOSName(getName());
            likeMe.setOSVersion(getOSVersion());
            likeMe.setOSArch(getOSArch());
            likeMe.setIP(getIP());
            likeMe.setHWArch(getHWArch());
            likeMe.setHWVendor(getHWVendor());
            if (customTags != null && !customTags.isEmpty()) {
                likeMe.setCustomTags(getCustomTags());

            }
            return likeMe;
        } catch (CloneNotSupportedException impossible) {
            throw new Error("Object.clone() threw CloneNotSupportedException", impossible);
        }
    }

    /**
     * Process an individual element from the document.
     *
     * @param elem the element to be processed.
     */
    protected void handleElement(final TextElement elem) {
        if (elem.getName().equals(idTag)) {
            setID(ID.create(URI.create(elem.getTextValue())));
        } else if (elem.getName().equals(nameTag)) {
            setName(elem.getTextValue());
        } else if (elem.getName().equals(OSNameTag)) {
            setOSName(elem.getTextValue());
        } else if (elem.getName().equals(OSVersionTag)) {
            setOSVersion(elem.getTextValue());
        } else if (elem.getName().equals(OSarchTag)) {
            setOSArch(elem.getTextValue());
        } else if (elem.getName().equals(ipTag)) {
            setIP(elem.getTextValue());
        } else if (elem.getName().equals(hwarchTag)) {
            setHWArch(elem.getTextValue());
        } else if (elem.getName().equals(hwvendorTag)) {
            setHWVendor(elem.getTextValue());
        } else {
            setCustomTag(elem.getName(), elem.getTextValue());
        }
    }

    /**
     * Intialize a System advertisement from a portion of a structured document.
     *
     * @param root documet to initialize object from
     */
    protected void initialize(final Element root) {
        if (!TextElement.class.isInstance(root)) {
            throw new IllegalArgumentException(getClass().getName() +
                    " only supports TextElement");
        }
        final TextElement doc = (TextElement) root;
        if (!doc.getName().equals(getAdvertisementType())) {
            throw new IllegalArgumentException(
                    new StringBuffer().append("Could not construct : ")
                            .append(getClass().getName())
                            .append("from doc containing a ").append(doc.getName())
                            .toString());
        }
        final Enumeration elements = doc.getChildren();
        while (elements.hasMoreElements()) {
            final TextElement elem = (TextElement) elements.nextElement();
            handleElement(elem);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final String[] getIndexFields() {
        return fields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        boolean retVal = false;
        if (this == obj) {
            retVal = true;
        } else if (obj instanceof SystemAdvertisement) {
            final SystemAdvertisement adv = (SystemAdvertisement) obj;
            retVal = getID().equals(adv.getID());
        }

        return retVal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {

        int result = 17;

        result = 37 * result + getID().hashCode();
        String name = getName();

        if (name != null) {
            result = 37 * result + name.hashCode();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(final Object other) {
        return getID().toString().compareTo(other.toString());
    }

    /**
     * All messages have a type (in xml this is &#0033;doctype) which
     * identifies the message
     *
     * @return String "jxta:System"
     */
    public static String getAdvertisementType() {
        return "jxta:System";
    }

    /**
     * Instantiator
     */
    public static class Instantiator implements AdvertisementFactory.Instantiator {

        /**
         * Returns the identifying type of this Advertisement.
         *
         * @return String the type of advertisement
         */
        public String getAdvertisementType() {
            return SystemAdvertisement.getAdvertisementType();
        }

        /**
         * Constructs an instance of <CODE>Advertisement</CODE> matching the
         * type specified by the <CODE>advertisementType</CODE> parameter.
         *
         * @return The instance of <CODE>Advertisement</CODE> or null if it
         *         could not be created.
         */
        public Advertisement newInstance() {
            return new SystemAdvertisement();
        }

        /**
         * Constructs an instance of <CODE>Advertisement</CODE> matching the
         * type specified by the <CODE>advertisementType</CODE> parameter.
         *
         * @param root Specifies a portion of a StructuredDocument which will
         *             be converted into an Advertisement.
         * @return The instance of <CODE>Advertisement</CODE> or null if it
         *         could not be created.
         */
        public Advertisement newInstance(final net.jxta.document.Element root) {
            return new SystemAdvertisement(root);
        }
    }
}
