package tukano.impl.rest.utils;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.message.internal.ReaderWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class CustomLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger Log = Logger.getLogger(CustomLoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String sb = " - queryParams: " + requestContext.getUriInfo().getQueryParameters() +
                " - Path: " + requestContext.getUriInfo().getPath() +
                " - Header: " + requestContext.getHeaders() +
                " - Entity: " + getEntityBody(requestContext);
        Log.info("HTTP REQUEST : " + sb);
    }

    private String getEntityBody(ContainerRequestContext requestContext) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = requestContext.getEntityStream();

        final StringBuilder b = new StringBuilder();
        try {
            ReaderWriter.writeTo(in, out);

            byte[] requestEntity = out.toByteArray();
            if (requestEntity.length == 0) {
                b.append("\n");
            } else {
                b.append(new String(requestEntity)).append("\n");
            }
            requestContext.setEntityStream(new ByteArrayInputStream(requestEntity));

        } catch (IOException ex) {
            // Handle logging error
        }
        return b.toString();
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {

        String sb = "Header: " + responseContext.getHeaders() +
                " - Entity (JSON): " + Entity.entity(responseContext.getEntity(), MediaType.APPLICATION_JSON).getEntity();
        Log.info("HTTP RESPONSE : " + sb);
    }

}