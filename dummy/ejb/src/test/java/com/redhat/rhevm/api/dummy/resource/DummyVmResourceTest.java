/*
 * Copyright © 2010 Red Hat, Inc.
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
package com.redhat.rhevm.api.dummy.resource;

import java.util.List;
import org.junit.Test;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.client.ClientResponse;
import com.redhat.rhevm.api.model.Link;
import com.redhat.rhevm.api.model.VM;

public class DummyVmResourceTest extends DummyTestBase
{
	private VmResource getService() {
		return createVmResource(getEntryPoint("vms").getHref());
	}

	private void checkVM(VM vm) {
		// FIXME: assertNotNull(vm.getName());
		assertNotNull(vm.getId());
		assertNotNull(vm.getLink());
		assertNotNull(vm.getLink().getRel());
		assertNotNull(vm.getLink().getHref());
		assertTrue(vm.getLink().getHref().endsWith(vm.getId()));
		assertNotNull(vm.getActions());
		assertTrue(vm.getActions().getLinks().size() > 0);
	}

	@Test
	public void testGetVmsList() throws Exception {
		VmResource service = getService();
		assertNotNull(service);

		List<VM> vms = service.list();
		assertNotNull(vms);
		assertTrue(vms.size() > 0);

		for (VM vm : vms) {
			checkVM(vm);

			VM t = service.get(vm.getId());
			checkVM(t);
			assertEquals(vm.getId(), t.getId());
		}
	}
}
