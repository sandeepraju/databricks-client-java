package com.level11data.databricks.client;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import javax.ws.rs.core.Response;

public class DatabricksClient {
    public DatabricksSession Session;

    public DatabricksClient(DatabricksSession session) {
        Session = session;
    }

    protected ClientConfig ClientConfig() {
        return new ClientConfig().register(new JacksonFeature());
    }

    protected void checkResponse(Response response) throws HttpException {
        // check response status code
        if (response.getStatus() == 401) {
            throw new HttpException("HTTP 401 Unauthorized: Not Authenticated");
        } else if(response.getStatus() == 403) {
            throw new HttpException("HTTP 403 Forbidden: Not Authorized");
        } else if (response.getStatus() != 200) {
            throw new HttpException("HTTP "+ response.getStatus() + ":");
        }
        //This will print the entire response body; useful for debugging code
        //String body = response.readEntity(String.class);
        //System.out.println(body);
    }

}