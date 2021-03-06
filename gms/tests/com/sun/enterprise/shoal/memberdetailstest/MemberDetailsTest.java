/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package com.sun.enterprise.shoal.memberdetailstest;

import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.GMSFactory;
import com.sun.enterprise.ee.cms.core.GroupManagementService;
import com.sun.enterprise.ee.cms.core.GroupManagementService.MemberType;
import com.sun.enterprise.ee.cms.impl.common.GroupManagementServiceImpl;

/**
 * Simple test for MemberDetails
 *
 * @author leehui
 */

public class MemberDetailsTest {

	/**
	 * main
	 * 
	 * every node's member details contains a key named memberToken.
	 * 
	 * start MemberDetailsTest's serveral instance,see the console's infomation. 
	 * 
	 * @param args command line args
	 */
	public static void main(String[] args) {
		
		String serverToken = UUID.randomUUID().toString();
		GroupManagementService gms = (GroupManagementService) GMSFactory.startGMSModule(serverToken, "DemoGroup", MemberType.CORE, null);
		try {
			gms.join();
		} catch (GMSException e) {
			e.printStackTrace();
		}
						
		Map<String, Object> memberDetails = new Hashtable<String, Object>();			
		memberDetails.put("memberToken", serverToken); 	
		
		try {
			((GroupManagementServiceImpl)gms).setMemberDetails(serverToken, memberDetails);
		} catch (GMSException e) {
			e.printStackTrace();
		}
		
		while(true){
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				System.out.println("error");
				break;
			}
			System.out.println("********************************************************");
			Map<Serializable,Serializable> members = gms.getAllMemberDetails("memberToken");
			Collection<Serializable> memberValues = members.values();
			for(Serializable member : memberValues){
				System.out.println(member);
			}
			System.out.println("---The above print result should be the same as below---");
			List<String> allMembers = gms.getGroupHandle().getAllCurrentMembers();
			for(String member : allMembers){
				System.out.println(gms.getMemberDetails(member).get("memberToken"));
			}
			System.out.println();
			
		}
	}

}
