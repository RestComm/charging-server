/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2014, TeleStax and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for
 * a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.charging.server.ratingengine.local;

import org.mobicents.charging.server.BaseSbb;
import org.mobicents.charging.server.ratingengine.RatingEngineClient;
import org.mobicents.charging.server.ratingengine.RatingInfo;
import org.mobicents.slee.SbbContextExt;

import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * SBB for Rating Engine Client implementation sample. This always returns success.
 *
 * @author ammendonca
 */
public class LocalRatingEngineSbb extends BaseSbb implements Sbb, RatingEngineClient {

	private Tracer tracer;

	private SbbContextExt sbbContext;

	public void setSbbContext(SbbContext context) {
		this.sbbContext = (SbbContextExt) context;
		this.tracer = sbbContext.getTracer("CS-RF-SMPL");
	}

	private HashMap<Integer, Integer> serviceIdUnits;

	@Override
	public void init() {
		// Load service id units (Unit Determination)
		// TODO: Need to port this to database.
		try {
			Properties props = new Properties();
			props.load(this.getClass().getClassLoader().getResourceAsStream("serviceid-units.properties"));
			serviceIdUnits = new HashMap<Integer, Integer>();
			for (Object key : props.keySet()) {
				String serviceId = ((String) key).trim();
				String unitTypeID = props.getProperty(serviceId).trim();
				serviceIdUnits.put(Integer.valueOf(serviceId),Integer.valueOf(unitTypeID));
			}
			if (tracer.isInfoEnabled()) {
				tracer.info("[--] Loaded service id units from properties file. Dumping info.");
			}
			// dump info...
			for (Object o : serviceIdUnits.entrySet()) {
				Map.Entry entry = (Map.Entry) o;
				String key = null;
				String val = null;
				if (entry.getKey() != null) {
					key = entry.getKey().toString();
				}
				if (entry.getValue() != null) {
					val = entry.getValue().toString();
				}
				if (tracer.isInfoEnabled()) {
					tracer.info("[--] Service-ID:" + key + " => Unit-Type-ID:" + val);
				}
			}
		}
		catch (Exception e) {
			tracer.warning("[!!] Unable to load service id units from properties file. Allowing everything!", e);
			// FIXME: Need something to bypass or use default unit determination
		}
	}

	@Override
	public RatingInfo getRateForService(HashMap params) {
		String sessionId = (String) params.get("SessionId");
		if (tracer.isInfoEnabled()) {
			tracer.info("[$$] SID<" + sessionId + "> Performing rating with the SIMPLE Rating Module.");
		}
		RatingInfo ri = new RatingInfo(0, sessionId);
		ri.setRate(1.0);

		return ri;
	}

}
