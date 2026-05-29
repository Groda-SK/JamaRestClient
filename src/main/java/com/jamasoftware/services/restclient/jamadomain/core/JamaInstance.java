package com.jamasoftware.services.restclient.jamadomain.core;

import com.jamasoftware.services.restclient.JamaConfig;
import com.jamasoftware.services.restclient.JamaParent;
import com.jamasoftware.services.restclient.jamadomain.fields.JamaField;
import com.jamasoftware.services.restclient.jamadomain.lazyresources.*;
import com.jamasoftware.services.restclient.exception.RestClientException;
import com.jamasoftware.services.restclient.httpconnection.Response;
import com.jamasoftware.services.restclient.jamaclient.JamaClient;
import com.jamasoftware.services.restclient.jamadomain.stagingresources.StagingItem;
import com.jamasoftware.services.restclient.jamadomain.stagingresources.StagingRelationship;
import com.jamasoftware.services.restclient.jamadomain.stagingresources.StagingResource;
import com.jamasoftware.services.restclient.jamadomain.values.JamaFieldValue;
import com.jamasoftware.services.restclient.util.CompareUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

public class JamaInstance implements JamaDomainObject {
    private JamaClient jamaClient;
    private JamaConfig jamaConfig;
    private Integer resourceTimeOut;
    private JamaUser currentUser;
    private ItemTypeList itemTypeList;
    private RelationshipTypeList relationshipTypeList;

    private Map<String, WeakReference<JamaDomainObject>> resourcePool = new HashMap<>();

    public JamaInstance(JamaConfig jamaConfig) {
        this.jamaConfig = jamaConfig;
        this.resourceTimeOut = jamaConfig.getResourceTimeOut();
        this.jamaClient = new JamaClient(
                jamaConfig.getHttpClient(),
                jamaConfig.getJson(),
                jamaConfig.getBaseUrl(),
                jamaConfig.getUsername(),
                jamaConfig.getPassword(),
                jamaConfig.getOpenUrlBase(),
                jamaConfig.getApiKey(),
                jamaConfig.isOauth());
    }

    private JamaDomainObject getPoolOrNull(String key) {
        WeakReference<JamaDomainObject> wr = resourcePool.get(key);
        return wr == null ? null : wr.get();
    }

    public JamaDomainObject checkPool(Class clazz, int id) {
        return getPoolOrNull(clazz.getName() + id);
    }

    public void addToPool(Class clazz, int id, JamaDomainObject jamaDomainObject) {
        resourcePool.put(clazz.getName() + id, new WeakReference<>(jamaDomainObject));
    }

    private JamaDomainObject checkPool(JamaDomainObject fresh) {
        if(fresh instanceof LazyResource) {
            String key = fresh.getClass().getName() + ((LazyResource) fresh).getId();
            LazyResource existingResource = (LazyResource) getPoolOrNull(key);
            if(existingResource != null) {
                existingResource.copyContentFrom(fresh);
                return existingResource;
            }
            resourcePool.put(key, new WeakReference<>(fresh));
        }
        return fresh;
    }

    public JamaDomainObject getResource(String resource) throws RestClientException {
        JamaDomainObject retrieved = jamaClient.getResource(resource, this);
        return checkPool(retrieved);
    }

    public List<JamaDomainObject> getResourceCollection(String resource) throws RestClientException {
        return getAll(resource);
    }

    public List<JamaDomainObject> getAll(String resource) throws RestClientException {
        List<JamaDomainObject> objects = jamaClient.getAll(jamaConfig.getBaseUrl() + resource, this);
        for(int i = 0; i < objects.size(); ++i) {
            objects.set(i, checkPool(objects.get(i)));
        }
        return objects;
    }


    public JamaProject getProject(int id) throws RestClientException{
        String key = JamaProject.class.getName() + id;
        JamaProject project = (JamaProject) getPoolOrNull(key);
        if(project != null) {
            project.fetch();
        } else {
            project = new JamaProject();
            project.associate(id, this);
            resourcePool.put(key, new WeakReference<>((JamaDomainObject)project));
        }
        return project;
    }

    public List<JamaProject> getProjects() throws RestClientException {
        List<JamaProject> projects = new ArrayList<>();
        List<JamaDomainObject> jamaDomainObjects = getAll("projects");
        for(JamaDomainObject jamaDomainObject : jamaDomainObjects) {
            JamaProject project = (JamaProject) jamaDomainObject;
            projects.add(project);
        }
        return projects;
    }

    public List<JamaItemType> getItemTypes() throws RestClientException {
        if(itemTypeList == null) {
            itemTypeList = new ItemTypeList(this);
        }
        return itemTypeList.getItemTypes();
    }

    public JamaItemType getItemType(int id) throws RestClientException{
        JamaItemType itemType = new JamaItemType();
        itemType.associate(id, this);
        return itemType;
    }

    public JamaItemType getItemType(String name) throws RestClientException {
        List<JamaItemType> itemTypes = getItemTypes();
        JamaItemType found = null;
        for(JamaItemType itemType : itemTypes) {
            if(CompareUtil.closeEnough(name, itemType.getDisplay())) {
                if(found != null) {
                    throw new RestClientException("Multiple ItemTypes with the display: " + name);
                }
                found = itemType;
            }
        }
        return found;
    }

    public JamaItem getItem(int id) throws RestClientException {
        String key = JamaItem.class.getName() + id;
        JamaItem item = (JamaItem) getPoolOrNull(key);
        if(item != null) {
            item.fetch();
        } else {
            item = new JamaItem();
            item.associate(id, this);
            resourcePool.put(key, new WeakReference<>((JamaDomainObject)item));
        }
        return item;
    }
    
    public List<JamaItem> findItem(int projectId, String searchstring) throws RestClientException {
    	List<JamaItem> items = new ArrayList<>();
    	StringBuilder builder = new StringBuilder("abstractitems");
    	if(projectId>0) {
    		builder.append("?");
    		builder.append("project=" + projectId);
    	}
    	if (searchstring!=null) {
    		if (builder.toString().contains("?")) {
    			builder.append("&");
    		} else {
    			builder.append("?");
    		}
    		builder.append("contains=" + searchstring);
    	}
    	String resource = builder.toString();
    	
    	List<JamaDomainObject> jamaDomainObjects =  jamaClient.getAll(jamaConfig.getBaseUrl() + resource, this);
    	for(JamaDomainObject jamaDomainObject : jamaDomainObjects) {
    		if (jamaDomainObject instanceof JamaItem)
    			items.add((JamaItem) jamaDomainObject);
    	}
    	return items;
    }

    public int postProjectAttachment(int projectID, String name, String description) throws RestClientException, JSONException {
        String url=jamaConfig.getBaseUrl() + "projects/" + projectID + "/attachments";
        String payload="{\"fields\": {\"name\": \"" + name + "\", \"description\": \"" + description + "\"}}";
        Response response=jamaClient.postRaw(url, payload);
        JSONObject responsejson = new JSONObject(response.getResponse());
        return (int) ((JSONObject) responsejson.get("meta")).get("id");
    }

    public void putAttachmentFile(int attachmentId, String filepath) throws RestClientException {
        String url = jamaConfig.getBaseUrl() + "attachments/" + attachmentId + "/file";
        File attachmentFile = new File(filepath);
        Response response=jamaClient.putAttachment(url, attachmentFile);
        if (response.getStatusCode()!=200)
            throw new RestClientException("Couldn't put file to Jama. Status code " + response.getStatusCode());
    }

    public void postItemAttachment(int itemId, int attachmentId) throws JSONException, RestClientException {
        String url = jamaConfig.getBaseUrl() + "items/" + itemId + "/attachments";
        JSONObject object = new JSONObject();
        object.put("attachment", attachmentId);
        String payload = object.toString();
        Response response=jamaClient.postRaw(url, payload);
        if(response.getStatusCode()>=400)
            throw new RestClientException("Couldn't post attachment to item " + itemId);
    }

    public List<JamaAttachment> getItemAttachment(int itemId) throws RestClientException, JSONException {
        String url = jamaConfig.getBaseUrl() + "items/" + itemId + "/attachments";
        return jamaClient.getAttachment(url, this, itemId);
    }

    public void deleteAttachment(int itemId, int attachmentId) throws RestClientException {
        String url = jamaConfig.getBaseUrl() + "items/" + itemId + "/attachments/" + attachmentId;
        jamaClient.deleteRaw(url);
    }

    public List<JamaAttachment> getAttachmentByID(int attachmentid) throws RestClientException, JSONException {
        String url = jamaConfig.getBaseUrl() + "abstractitems?itemType=22&contains=" + attachmentid;
        return jamaClient.getAttachment(url, this, attachmentid);
    }

    public JamaRelationship getRelationship(int id) throws RestClientException {
        String key = JamaRelationship.class.getName() + id;
        JamaRelationship relationship = (JamaRelationship) getPoolOrNull(key);
        if(relationship != null) {
            relationship.fetch();
        } else {
            relationship = new JamaRelationship();
            relationship.associate(id, this);
            resourcePool.put(key, new WeakReference<>((JamaDomainObject)relationship));
        }
        return relationship;
    }

    public void deleteItem(int id) throws RestClientException {
        deleteRawData("items/" + id);
    }

    public void deleteRelationship(int id) throws RestClientException {
        deleteRawData("relationships/" + id);
    }

    void deleteRawData(String resource) throws RestClientException {
        jamaClient.deleteRaw(jamaConfig.getBaseUrl() + resource);
    }

    public void ping() throws RestClientException {
        jamaClient.ping();
    }

    public Integer getResourceTimeOut() {
        return resourceTimeOut;
    }

    public JamaUser getCurrentUser() throws RestClientException{
        if(currentUser == null) {
            currentUser = (JamaUser) getResource("users/current");
        }
        return currentUser;
    }

    public void putRawData(String resource, String payload) throws RestClientException {
        jamaClient.putRaw(jamaConfig.getBaseUrl() + resource, payload);
    }

    public void postRawData(String resource, String payload) throws RestClientException {
        jamaClient.postRaw(jamaConfig.getBaseUrl() + resource, payload);
    }

    public byte[] retrieveItemTypeImage(String url) throws RestClientException {
        return jamaClient.getItemTypeImage(url);
    }

    public String getOpenUrl(JamaItem item) {
        return this.jamaConfig.getOpenUrlBase() + item.getId() + "?project=" + item.getProject().getId();
    }

    public void setBaseOpenUrl(String baseOpenUrl) {
        this.jamaConfig.setOpenUrlBase(baseOpenUrl);
    }

    public void setResourceTimeOut(Integer resourceTimeOut) {
        this.resourceTimeOut = resourceTimeOut;
    }

    public StagingItem createItem(String name, JamaParent parent, JamaItemType itemType) throws RestClientException{
        return (new StagingDispenser()).createStagingItem(this, name, parent, itemType);
    }

    protected void put(LazyResource lazyResource) throws RestClientException {
        jamaClient.put(lazyResource.getEditUrl(), lazyResource);
    }

    protected void delete(LazyResource lazyResource) throws RestClientException {
        jamaClient.delete(lazyResource.getDeleteUrl());
    }

    protected LazyResource post(LazyResource lazyResource, Class clazz) throws RestClientException {
        Integer resourceId = jamaClient.post(lazyResource.getCreateUrl(), lazyResource);
        if(resourceId == null) {
            return null;
        }
        LazyResource createdResource;
        try {
            createdResource = (LazyResource)clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RestClientException(e);
        }
        createdResource.associate(resourceId, this);
        return createdResource;
    }

    public StagingItem editItem(JamaItem jamaItem) throws RestClientException {
        jamaItem.fetch();
        return (new StagingDispenser()).createStagingItem(jamaItem);
    }

    public StagingRelationship editRelationship(JamaRelationship jamaRelationship) throws RestClientException {
        jamaRelationship.fetch();
        return (new StagingDispenser()).createStagingRelationship(jamaRelationship);
    }


    public List<JamaRelationshipType> getRelationshipTypes() throws RestClientException {
        if(relationshipTypeList == null) {
            relationshipTypeList = new RelationshipTypeList(this);
        }
        return relationshipTypeList.getRelationshipTypes();
    }

    public String executeWorkflowTransition(int itemId, String stateId) throws RestClientException, JSONException {
        String url=jamaConfig.getBaseUrl() + "items/" + itemId + "/workflowtransitions";
        String payload="{\"transitionId\": \"" + stateId + "\",\"comment\": \"\"}";
        Response response=jamaClient.postRaw(url, payload);
        JSONObject responsejson = new JSONObject(response.getResponse());
        return (String) ((JSONObject) responsejson.get("meta")).get("status");
    }

    public JSONArray listWorkflowTransitions(int itemId) throws RestClientException, JSONException {
        String url=jamaConfig.getBaseUrl() + "items/" + itemId + "/workflowtransitionoptions";
        return jamaClient.getAvailableWorkflowTransitions(url, this);
    }

    public int patchItem(int itemId, List<Map<String,Object>> listoffields) throws JSONException, RestClientException {
        String url=jamaConfig.getBaseUrl() + "items/" + itemId;
        List<JSONObject> list = new ArrayList<>();

        for (Map<String,Object> field:listoffields) {
            JSONObject entry = new JSONObject();
            entry.put("op", field.get("action"));
            Map<String, Object> fielddata = (Map<String, Object>) field.get("field");
            entry.put("path", "/fields/" + fielddata.get("name"));
            if(field.get("action").equals("add"))
                entry.put("value", fielddata.get("value"));
            list.add(entry);
        }
        JSONArray payload = new JSONArray(list);
        return jamaClient.patchItem(url, payload.toString());
    }
    
    public List<Map<String, Object>> getRelationshipRules(int projectid) throws RestClientException, JSONException {
    	List<Map<String, Object>> listofrules = new ArrayList<Map<String,Object>>();
    	String url=jamaConfig.getBaseUrl() + "relationshiprulesets";
    	JSONObject ruleset = jamaClient.getRelationshipRules(url, projectid);
    	JSONArray rules = (JSONArray) ruleset.get("rules");
    	for (int i=0; i<rules.length(); i++) {
    		Map<String, Object> map = new HashMap<String, Object>();
    		JSONObject rule = (JSONObject) rules.get(i);
    		map.put("id", rule.get("id"));
    		map.put("fromItemTypeId", rule.get("fromItemTypeId"));
    		map.put("toItemTypeId", rule.get("toItemTypeId"));
    		map.put("forCoverage", rule.get("forCoverage"));
    		map.put("relationshipTypeId", rule.get("relationshipTypeId"));
    		listofrules.add(map);
    	}
		return listofrules;
    	
    }
}
