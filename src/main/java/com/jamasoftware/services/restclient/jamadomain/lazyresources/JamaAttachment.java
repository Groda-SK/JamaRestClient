package com.jamasoftware.services.restclient.jamadomain.lazyresources;

import com.jamasoftware.services.restclient.jamadomain.core.JamaDomainObject;
import com.jamasoftware.services.restclient.jamadomain.core.JamaInstance;
import com.jamasoftware.services.restclient.jamadomain.core.LazyResource;

public class JamaAttachment extends LazyResource {

    protected JamaItem item;
    protected String name;
    protected int size;
    protected int attachmentId;
    protected int projectid;
    protected int itemType;
    protected JamaInstance jamaInstance;

    public JamaAttachment () {
        this.itemType=22;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(int attachmentId) {
        this.attachmentId = attachmentId;
    }

    public void setJamaInstance(JamaInstance jamaInstance) {
        this.jamaInstance = jamaInstance;
    }

    public JamaAttachment(JamaInstance jamaInstance) {
        this.jamaInstance=jamaInstance;
    }

    @Override
    protected String getResourceUrl() {
        return "attachments/" + getId();
    }

    @Override
    protected void copyContentFrom(JamaDomainObject jamaDomainObject) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copyContentFrom'");
    }

    @Override
    protected void writeContentTo(JamaDomainObject jamaDomainObject) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'writeContentTo'");
    }
}
