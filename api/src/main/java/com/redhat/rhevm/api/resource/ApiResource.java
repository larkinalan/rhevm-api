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
package com.redhat.rhevm.api.resource;

import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/* FIXME:
 * different backends shouldn't need to specialize this,
 * we should make this a concrete implementation instead
 * of an interface
 */

@Path("/")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_X_YAML, MediaType.APPLICATION_JSON})
public interface ApiResource {
    /* FIXME: use @Context on a private field instead of
     *        having it passed as a parameter
     */

    @HEAD
    public Response head(@Context UriInfo uriInfo);

    @Path("datacenters")
    public DataCentersResource getDataCentersSubResource(@Context UriInfo uriInfo);

    @Path("hosts")
    public HostsResource getHostsSubResource(@Context UriInfo uriInfo);

    @Path("storagedomains")
    public StorageDomainsResource getStorageDomainsSubResource(@Context UriInfo uriInfo);

    @Path("vms")
    public VmsResource getVmsSubResource(@Context UriInfo uriInfo);
}
