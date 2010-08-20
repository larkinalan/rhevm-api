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

import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;

import com.redhat.rhevm.api.model.Action;
import com.redhat.rhevm.api.model.Actionable;
import com.redhat.rhevm.api.model.Host;


@Produces(MediaType.APPLICATION_XML)
public interface HostResource extends UpdatableResource<Host> {

    @Path("{action: (approve|fence|resume)}/{oid}")
    public ActionResource getActionSubresource(@PathParam("action")String action, @PathParam("oid")String oid);

    @POST
    @Formatted
    @Actionable
    @Path("approve")
    public Response approve(@Context UriInfo uriInfo, Action action);

    @POST
    @Formatted
    @Actionable
    @Path("install")
    public Response install(@Context UriInfo uriInfo, Action action);

    @POST
    @Formatted
    @Actionable
    @Path("activate")
    public Response activate(@Context UriInfo uriInfo, Action action);

    @POST
    @Formatted
    @Actionable
    @Path("deactivate")
    public Response deactivate(@Context UriInfo uriInfo, Action action);

    @Path("nics")
    public HostNicsResource getHostNicsResource();
}
