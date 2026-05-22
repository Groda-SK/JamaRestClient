package com.jamasoftware.services.restclient.jamaclient;

import com.jamasoftware.services.restclient.httpconnection.FileResponse;
import com.jamasoftware.services.restclient.jamadomain.core.JamaDomainObject;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.httpconnection.Response;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.httpconnection.HttpClient;
import com.jamasoftware.services.restclient.jamadomain.core.LazyResource;
import com.jamasoftware.services.restclient.jamadomain.fields.JamaField;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.JamaAttachment;
import com.jamasoftware.services.restclient.jamadomain.values.JamaFieldValue;
import com.jamasoftware.services.restclient.json.JsonHandler;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JamaClient {
    private HttpClient httpClient;
    private JsonHandler json;
    private String username;
    private String password;
    private String baseUrl;
    private String linkUrl;
    private String apiKey = null;
    private boolean oauth;

    public JamaClient(HttpClient httpClient, JsonHandler json, String baseUrl, String username, String password,
            boolean oauth) {
        this.httpClient = httpClient;
        this.json = json;
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.oauth = oauth;
    }

    public JamaClient(HttpClient httpClient, JsonHandler json, String baseUrl, String username, String password,
            String linkUrl, String apiKey, boolean oauth) {
        this.httpClient = httpClient;
        this.json = json;
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.linkUrl = linkUrl;
        this.apiKey = apiKey;
        this.oauth = oauth;
    }

    public JamaDomainObject getResource(String resource, JamaInstance jamaInstance) throws RestClientException {
        Response response = httpClient.get(baseUrl + resource, username, password, apiKey, oauth);
        return json.deserialize(response.getResponse(), jamaInstance);
    }

    // public JamaPage getPage(String url, JamaInstance jamaInstance) throws
    // RestClientException {
    // return getPage(url, jamaInstance);
    // }

    public JamaPage getPage(String url, JamaInstance jamaInstance) throws RestClientException {
        return getPage(url, "", jamaInstance);
    }

    public JamaPage getPage(String url, String startAt, JamaInstance jamaInstance) throws RestClientException {
        Response response = httpClient.get(url + startAt, username, password, apiKey, oauth);
        JamaPage page = json.getPage(response.getResponse(), jamaInstance);
        page.setJamaClient(this);
        page.setUrl(url);
        return page;
    }

    public List<JamaDomainObject> getAll(String url, JamaInstance jamaInstance) throws RestClientException {
        List<JamaDomainObject> results = new ArrayList<>();
        JamaPage page = getPage(url, jamaInstance);
        results.addAll(page.getResults());
        while (page.hasNext()) {
            page = page.getNext(jamaInstance);
            results.addAll(page.getResults());
        }
        return results;
    }

    public void ping() throws RestClientException {
        httpClient.get(baseUrl, username, password, apiKey, oauth);
    }

    public void putRaw(String url, String payload) throws RestClientException {
        httpClient.put(url, username, password, apiKey, payload, oauth);
    }

    public void deleteRaw(String url) throws RestClientException {
        httpClient.delete(url, username, password, apiKey, oauth);
    }

    public void delete(String resource) throws RestClientException {
        deleteRaw(baseUrl + resource);
    }

    public void put(String resource, LazyResource payload) throws RestClientException {
        // System.out.println(json.serialize(payload));
        putRaw(baseUrl + resource, json.serializeEdited(payload));
    }

    public Response postRaw(String url, String payload) throws RestClientException {
        return httpClient.post(url, username, password, apiKey, payload, oauth);
        // System.out.println(response.getResponse());
    }

    public Integer post(String resource, LazyResource payload) throws RestClientException {
        Response response = postRaw(baseUrl + resource, json.serializeCreated(payload));
        return json.deserializeLocation(response.getResponse());
    }

    public byte[] getItemTypeImage(String url) throws RestClientException {
        String domain = url.substring(0, url.indexOf("/img/"));
        if (!baseUrl.contains(domain)) {
            throw new RestClientException("Not a valid Item Type image URL: \"" + url + "\"");
        }
        FileResponse response = httpClient.getFile(url, username, password, apiKey, oauth);
        return response.getFileData();
    }

    public Response putAttachment(String url, File file) throws RestClientException {
        return httpClient.putFile(url, username, password, apiKey, file, oauth);
    }

    public List<JamaAttachment> getAttachment(String url, JamaInstance jamaInstance, int itemId)
            throws RestClientException, JSONException {
        Response response = httpClient.get(url, username, password, apiKey, oauth);
        JSONObject object = new JSONObject(response.getResponse());
        int totalresults = ((JSONObject) ((JSONObject) object.get("meta")).get("pageInfo")).getInt("totalResults");

        List<JamaAttachment> results = new ArrayList<>();

        if (totalresults==0)
            return results;

        for (int startAt = 0; startAt < totalresults;) {
            List<JamaAttachment> pagedresults = pagedResults(url, jamaInstance, itemId, startAt);
            results.addAll(pagedresults);
            startAt = results.size();
        }
        return results;
    }

    private List<JamaAttachment> pagedResults(String url, JamaInstance jamaInstance, int itemId, int startAt)
            throws RestClientException, JSONException {
        List<JamaAttachment> results = new ArrayList<>();
        String startaturl;
        if(url.contains("?"))
            startaturl=url + "&startAt=" + startAt;
        else
            startaturl=url + "?startAt=" + startAt;
        Response response = httpClient.get(startaturl, username, password, apiKey, oauth);
        JSONObject object = new JSONObject(response.getResponse());

        JSONArray array = (JSONArray) object.get("data");

        for (int count = 0; count < array.length(); count++) {
            JamaAttachment attachment = new JamaAttachment(jamaInstance);
            JSONObject attdata = (JSONObject) array.get(count);
            attachment.setAttachmentId(attdata.getInt("id"));
            attachment.setName(attdata.getString("fileName"));
            attachment.setSize(attdata.getInt("fileSize"));
            results.add(attachment);
        }
        return results;
    }

    public JSONArray getAvailableWorkflowTransitions(String url, JamaInstance jamaInstance) throws RestClientException, JSONException {
        Response response = httpClient.get(url, username, password, apiKey, oauth);
        JSONObject object = new JSONObject(response.getResponse());
        return (JSONArray) object.get("data");
    }

    public int patchItem(String url, String payload) throws RestClientException {
        Response response = httpClient.patch(url, username, password, apiKey, payload, oauth);
        return response.getStatusCode();
    }
}
