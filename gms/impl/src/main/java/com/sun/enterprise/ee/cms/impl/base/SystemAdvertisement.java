/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package com.sun.enterprise.ee.cms.impl.base;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.net.URI;

/**
 * This interface provides system characteristics
 *
 * i.g. HW/SW configuration, CPU load, etc...
 * 
 * @author Bongjae Chang
 */
public interface SystemAdvertisement extends Comparable<SystemAdvertisement>, Cloneable, Serializable {

    static final long serialVersionUID = 4520670615616793233L;

    public static final String OSNameTag = "OSName";
    public static final String OSVersionTag = "OSVer";
    public static final String OSarchTag = "osarch";
    public static final String hwarchTag = "hwarch";
    public static final String hwvendorTag = "hwvendor";
    public static final String idTag = "ID";
    public static final String ipTag = "ip";
    public static final String nameTag = "name";

    /**
     * Sets the hWArch attribute of the SystemAdvertisement object
     *
     * @param hwarch The new hWArch value
     */
    public void setHWArch( final String hwarch );

    /**
     * Sets the OSArch attribute of the SystemAdvertisement object
     *
     * @param osarch The new hWArch value
     */
    public void setOSArch( final String osarch );

    /**
     * Sets the hWVendor attribute of the SystemAdvertisement object
     *
     * @param hwvendor The new hWVendor value
     */
    public void setHWVendor( final String hwvendor );

    /**
     * sets the unique id
     *
     * @param id The id
     */
    public void setID( final PeerID id );

    /**
     * Sets the network interface's address in the form of a URI
     *
     * @param value new uri (tcp://host:port)
     */
    public void addEndpointAddress( final String value );

    /**
     * API for setting the IP addresses for all the network interfaces
     *
     * @param endpoints endpoint addresses
     */
    public void setEndpointAddresses( final List<String> endpoints );

    /**
     * Sets the name attribute of the DeviceAdvertisement object
     *
     * @param name The new name value
     */
    public void setName( final String name );

    /**
     * Sets the oSName attribute of the SystemAdvertisement object
     *
     * @param osname The new oSName value
     */
    public void setOSName( final String osname );

    /**
     * Sets the oSVersion attribute of the SystemAdvertisement object
     *
     * @param osversion The new oSVersion value
     */
    public void setOSVersion( final String osversion );

    public void setCustomTag( final String tag, final String value );

    public void setCustomTags( final Map<String, String> tags );

    /**
     * Gets the hWArch attribute of the SystemAdvertisement object
     *
     * @return The hWArch value
     */
    public String getHWArch();

    /**
     * Gets the OSArch attribute of the SystemAdvertisement object
     *
     * @return The OSArch value
     */
    public String getOSArch();

    /**
     * Gets the hWVendor attribute of the SystemAdvertisement object
     *
     * @return The hWVendor value
     */
    public String getHWVendor();

    /**
     * returns the id of the device
     *
     * @return ID the device id
     */
    public PeerID getID();

    /**
     * Gets the address of the network interface in the form of URI
     *
     * @return the list of URIs for all the network interfaces
     */
    public List<String> getEndpointAddresses();

    public List<URI> getURIs();

    /**
     * Gets the name attribute of the SystemAdvertisement object
     *
     * @return The name value
     */
    public String getName();

    /**
     * Gets the OSName attribute of the SystemAdvertisement object
     *
     * @return The OSName value
     */
    public String getOSName();

    /**
     * Gets the OSVersion attribute of the SystemAdvertisement object
     *
     * @return The OSVersion value
     */
    public String getOSVersion();

    public String getCustomTagValue( final String tagName ) throws NoSuchFieldException;

    public Map<String, String> getCustomTags();
}
