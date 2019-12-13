package org.servantscode.commons.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

public abstract class AbstractServiceClient {

    final private Client client;
    final private WebTarget webTarget;

    public abstract String getReferralUrl();
    public abstract String getAuthorization();
    public abstract Map<String, String> getAdditionalHeaders();

    protected AbstractServiceClient(String baseUrl) {
        client = ClientBuilder.newClient(new ClientConfig().register(this.getClass()));
        webTarget = client.target(baseUrl);
    }

    public Response post(Map<String, Object> data, Map<String, Object>... params) {
        return post(null, data, params);
    }

    public Response post(String path, Map<String, Object> data, Map<String, Object>... params) {
        try {
            translateDates(data);
            if(isEmpty(path))
                return buildInvocation(params)
                        .post(Entity.entity(data, MediaType.APPLICATION_JSON));
            else
                return buildInvocation(path, params)
                        .post(Entity.entity(data, MediaType.APPLICATION_JSON));
        } catch (Throwable e) {
            try {
                System.err.println("Call failed: " + new ObjectMapper().writeValueAsString(data));
            } catch (JsonProcessingException e1) {
                System.err.println("Won't happen");
            }
            throw new RuntimeException("Call failed: ", e);
        }
    }

    public Response post(List<Map<String, Object>> data, Map<String, Object>... params) {
        try {
            data.forEach(this::translateDates);
            return buildInvocation(params)
                    .post(Entity.entity(data, MediaType.APPLICATION_JSON));
        } catch (Throwable e) {
            try {
                System.err.println("Call failed: " + new ObjectMapper().writeValueAsString(data));
            } catch (JsonProcessingException e1) {
                System.err.println("Won't happen");
            }
            throw new RuntimeException("Call failed: ", e);
        }
    }

    public Response put(Map<String, Object> data, Map<String, Object>... params) {
        return put(null, data, params);
    }

    public Response put(String path, Map<String, Object> data, Map<String, Object>... params) {
        try {
            translateDates(data);
            if(isEmpty(path))
                return buildInvocation(params)
                        .put(Entity.entity(data, MediaType.APPLICATION_JSON));
            else
                return buildInvocation(path, params)
                        .put(Entity.entity(data, MediaType.APPLICATION_JSON));
        } catch (Throwable e) {
            try {
                System.err.println("Call failed: " + new ObjectMapper().writeValueAsString(data));
            } catch (JsonProcessingException e1) {
                System.err.println("Won't happen");
            }
            throw new RuntimeException("Call failed: ", e);
        }
    }

    public Response put(List<Map<String, Object>> data, Map<String, Object>... params) {
        try {
            data.forEach(this::translateDates);
            return buildInvocation(params)
                    .put(Entity.entity(data, MediaType.APPLICATION_JSON));
        } catch (Throwable e) {
            try {
                System.err.println("Call failed: " + new ObjectMapper().writeValueAsString(data));
            } catch (JsonProcessingException e1) {
                System.err.println("Won't happen");
            }
            throw new RuntimeException("Call failed: ", e);
        }
    }

    public Response get(Map<String, Object>... params) {
        return buildInvocation(params).get();
    }

    public Response get(String path, Map<String, Object>... params) {
        return buildInvocation(path, params).get();
    }

    public Response delete(int id, Map<String, Object>... params) {
        return buildInvocation("/" + id, params).delete();
    }

    // ----- Protected -----
    protected void translateDates(Map<String, Object> data) {
        data.entrySet().forEach( (entry) -> {
            Object obj = entry.getValue();
            if(obj instanceof ZonedDateTime) {
                entry.setValue(((ZonedDateTime) obj).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            } else if(obj instanceof LocalDate) {
                entry.setValue(((LocalDate)obj).format(DateTimeFormatter.ISO_DATE));
            } else if(obj instanceof List) {
                List list = (List)obj;
                if(!list.isEmpty() && list.get(0) instanceof Map)
                    list.forEach((item) -> translateDates((Map<String, Object>)item));
                if(!list.isEmpty() && list.get(0) instanceof LocalDate)
                    entry.setValue(list.stream()
                            .map((item) -> ((LocalDate)item).format(DateTimeFormatter.ISO_DATE)).collect(Collectors.toList())
                    );
            } else if(obj instanceof Map) {
                translateDates((Map<String, Object>)obj);
            }
        });
    }

    protected Invocation.Builder buildInvocation(String path, Map<String, Object>... optionalParams) {
        return buildInvocation(webTarget.path(path), optionalParams);
    }

    protected Invocation.Builder buildInvocation(Map<String, Object>... optionalParams) {
        return buildInvocation(webTarget, optionalParams);
    }

    protected Invocation.Builder buildInvocation(WebTarget target, Map<String, Object>... optionalParams) {
        if(optionalParams.length > 0) {
            Map<String, Object> params = optionalParams[0];
            for(Map.Entry<String, Object> entry: params.entrySet())
                target = target.queryParam(entry.getKey(), entry.getValue());
        }

        String urlPrefix = getReferralUrl();
        String authorization = getAuthorization();

        Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);
        if(isSet(urlPrefix))
            builder = builder.header("referer", urlPrefix);
        if(isSet(authorization))
            builder = builder.header("Authorization", authorization);

        Map<String, String> headers = getAdditionalHeaders();
        if(headers != null) {
            for(Map.Entry<String, String> entry: headers.entrySet())
                builder = builder.header(entry.getKey(), entry.getValue());
        }

        return builder;
    }
}
