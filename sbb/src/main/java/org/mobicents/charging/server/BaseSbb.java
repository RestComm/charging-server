/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2012, TeleStax and individual contributors as indicated
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

package org.mobicents.charging.server;

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;

/**
 * Base SBB Class for Charging Server SBBs.
 * 
 * @author ammendonca
 * @author baranowb
 */
public abstract class BaseSbb implements Sbb/*Ext*/ {

	@Override
	public void sbbActivate() {
	}

	@Override
	public void sbbCreate() throws CreateException {
	}

	@Override
	public void sbbExceptionThrown(Exception exception, Object event, ActivityContextInterface activity) {
	}

	@Override
	public void sbbLoad() {
	}

	@Override
	public void sbbPassivate() {
	}

	@Override
	public void sbbPostCreate() throws CreateException {
	}

	@Override
	public void sbbRemove() {
	}

	@Override
	public void sbbRolledBack(RolledBackContext context) {
	}

	@Override
	public void sbbStore() {
	}

	@Override
	public void setSbbContext(SbbContext context) {
	}

	@Override
	public void unsetSbbContext() {
	}

}
