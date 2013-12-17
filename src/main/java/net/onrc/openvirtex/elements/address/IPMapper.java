/*******************************************************************************
 * Copyright (c) 2013 Open Networking Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package net.onrc.openvirtex.elements.address;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;

import net.onrc.openvirtex.elements.Mappable;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.exceptions.IndexOutOfBoundException;
import net.onrc.openvirtex.exceptions.AddressMappingException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerDestination;
import net.onrc.openvirtex.messages.actions.OVXActionNetworkLayerSource;

public class IPMapper {
    static Logger log = LogManager.getLogger(IPMapper.class.getName());

    public static Integer getPhysicalIp(Integer tenantId, Integer virtualIP) {
    	final Mappable map = OVXMap.getInstance();
    	final OVXIPAddress vip = new OVXIPAddress(tenantId, virtualIP);
    	try {
    		PhysicalIPAddress pip;
    		if (map.hasPhysicalIP(vip, tenantId)) {
    			pip = map.getPhysicalIP(vip, tenantId);
    		} else {
    			pip = new PhysicalIPAddress(map.getVirtualNetwork(tenantId).nextIP());
    			log.debug("Adding IP mapping {} -> {} for tenant {}", vip, pip, tenantId);
    			map.addIP(pip, vip);
    		}
    		return pip.getIp();
    	} catch (IndexOutOfBoundException e) {
    		log.error("No available physical IPs for virtual ip {} in tenant {}",vip, tenantId);
    	} catch (NetworkMappingException e) {
    		log.error(e);
    	} catch (AddressMappingException e) {
    		log.error("Inconsistency in Physical-Virtual mapping : {}", e);
    	}
    	return 0;
    }

    public static void rewriteMatch(final Integer tenantId, final OFMatch match) {
    	match.setNetworkSource(getPhysicalIp(tenantId, match.getNetworkSource()));
    	match.setNetworkDestination(getPhysicalIp(tenantId, match.getNetworkDestination()));
    }

    public static List<OFAction> prependRewriteActions(final Integer tenantId, final OFMatch match) {
    	final List<OFAction> actions = new LinkedList<OFAction>();
    	if (!match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
    		final OVXActionNetworkLayerSource srcAct = new OVXActionNetworkLayerSource();
    		srcAct.setNetworkAddress(getPhysicalIp(tenantId, match.getNetworkSource()));
    		actions.add(srcAct);
    	}
    	if (!match.getWildcardObj().isWildcarded(Flag.NW_DST)) {
    		final OVXActionNetworkLayerDestination dstAct = new OVXActionNetworkLayerDestination();
    		dstAct.setNetworkAddress(getPhysicalIp(tenantId, match.getNetworkDestination()));
    		actions.add(dstAct);
    	}
    	return actions;
    }
    
    public static List<OFAction> prependUnRewriteActions(final OFMatch match) {
    	final List<OFAction> actions = new LinkedList<OFAction>();
    	if (!match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
    		final OVXActionNetworkLayerSource srcAct = new OVXActionNetworkLayerSource();
    		srcAct.setNetworkAddress(match.getNetworkSource());
    		actions.add(srcAct);
    	}
    	if (!match.getWildcardObj().isWildcarded(Flag.NW_DST)) {
    		final OVXActionNetworkLayerDestination dstAct = new OVXActionNetworkLayerDestination();
    		dstAct.setNetworkAddress(match.getNetworkDestination());
    		actions.add(dstAct);
    	}
    	return actions;
    }
}
