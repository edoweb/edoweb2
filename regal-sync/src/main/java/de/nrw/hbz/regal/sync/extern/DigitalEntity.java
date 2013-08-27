/*
 * Copyright 2012 hbz NRW (http://www.hbz-nrw.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.nrw.hbz.regal.sync.extern;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

/**
 * @author Jan Schnasse, schnasse@hbz-nrw.de
 * 
 */
public class DigitalEntity {

    private boolean isParent = true;
    private String pid = null;
    private String usageType = null;
    private String location = null;

    private HashMap<StreamType, Stream> streams = null;
    private Vector<RelatedDigitalEntity> related = null;

    private String parentPid;
    private String label = null;
    private String type;

    private File xml = null;

    /**
     * @param location
     *            the directory of the entity
     */
    public DigitalEntity(String location) {
	this.location = location;

	related = new Vector<RelatedDigitalEntity>();
	streams = new HashMap<StreamType, Stream>();
    }

    /**
     * @param location
     *            the directory of the entity
     * @param pid
     *            The pid of the entity
     */
    public DigitalEntity(String location, String pid) {
	this.location = location;
	this.pid = pid;
	related = new Vector<RelatedDigitalEntity>();
	streams = new HashMap<StreamType, Stream>();
    }

    private String fileToString(File file, String streamId) throws Exception {
	// String str = "";
	if (pid == null)
	    throw new Exception("Can't set Attribute please set PID first.");
	if (file == null || !file.exists()) {
	    // System.out.println("NO MARC METADATA");
	    return "";
	}
	byte[] buffer = new byte[(int) file.length()];
	BufferedInputStream f = null;
	try {
	    f = new BufferedInputStream(new FileInputStream(file));
	    f.read(buffer);
	} finally {
	    if (f != null)
		try {
		    f.close();
		} catch (IOException ignored) {
		}
	}

	return new String(buffer);

    }

    /**
     * @return a pid for the entity
     */
    public String getPid() {
	return pid;
    }

    /**
     * @param pid
     *            a pid for the entity
     */
    public void setPid(String pid) {
	this.pid = pid;
    }

    /**
     * @param type
     *            stream type
     * @return the stream
     */
    public Stream getStream(StreamType type) {
	return streams.get(type);
    }

    /**
     * @param xml
     *            a xml file
     */
    public void setXml(File xml) {
	this.xml = xml;
    }

    /**
     * @return xml file
     */
    public File getXml() {
	return xml;
    }

    /**
     * TODO: WTF?
     * 
     * @return a xml representation of myself
     */
    public File getMe() {
	return new File(location + File.separator + pid + ".xml");
    }

    /**
     * @param label
     *            a label for the entity
     */
    public void setLabel(String label) {
	this.label = label;
    }

    /**
     * @return a label for the entity
     */
    public String getLabel() {
	return label;
    }

    /**
     * @return do you have children?
     */
    public boolean isParent() {
	return isParent;
    }

    /**
     * @param isParent
     *            do you have children?
     */
    public void setIsParent(boolean isParent) {
	this.isParent = isParent;
    }

    /**
     * @param relPid
     *            set a parent
     */
    public void setParentPid(String relPid) {
	parentPid = relPid;
    }

    /**
     * @return if there is a hierarchy, return your parent
     */
    public String getParentPid() {
	return parentPid;
    }

    /**
     * @return the entities type
     */
    public String getType() {
	return type;
    }

    /**
     * @param type
     *            a user defined type for the entity
     */
    public void setType(String type) {
	this.type = type;
    }

    /**
     * @return all related entities
     */
    public Vector<RelatedDigitalEntity> getRelated() {
	return related;
    }

    /**
     * @param related
     *            alle related entities
     */
    public void setRelated(Vector<RelatedDigitalEntity> related) {
	this.related = related;
    }

    /**
     * @param entity
     *            a related entity
     * @param relation
     *            a user defined relation
     */
    public void addRelated(DigitalEntity entity, String relation) {
	addRelated(new RelatedDigitalEntity(entity, relation));
    }

    /**
     * @param relation
     *            a related entity
     */
    public void addRelated(RelatedDigitalEntity relation) {
	related.add(relation);
    }

    /**
     * @param file
     *            a file
     * @param mime
     *            a mimetype, e.g. application/pdf
     * @param type
     *            a stream type
     */
    public void addStream(File file, String mime, StreamType type) {
	streams.put(type, new Stream(file, mime, type));

    }

    /**
     * @param file
     *            a file
     * @param mime
     *            a mimetype, e.g. application/pdf
     * @param type
     *            a stream type
     * @param fileId
     *            a id for the file
     */
    public void addStream(File file, String mime, StreamType type, String fileId) {
	streams.put(type, new Stream(file, mime, type, fileId));

    }

    /**
     * @return the data stream
     */
    public File getStream() {
	return streams.get(StreamType.DATA).getFile();
    }

    /**
     * @return the base location of the streams in filesystem
     */
    public String getLocation() {
	return location;
    }

    /**
     * TODO: Remove:Digitool Specific
     * 
     * @return dc string
     * @throws Exception
     *             if io fails
     */
    public String getDc() throws Exception {
	return fileToString(streams.get(StreamType.DC).getFile(),
		StreamType.DC.toString());
    }

    /**
     * TODO: Remove:Digitool Specific
     * 
     * @return control section of a digitool object
     * @throws Exception
     *             if something goes wrong
     */
    public String getControl() throws Exception {
	return fileToString(streams.get(StreamType.CONTROL).getFile(),
		StreamType.CONTROL.toString());

    }

    /**
     * TODO: Remove:Digitool Specific
     * 
     * @return all related Objects with part_of relation
     */
    public Vector<DigitalEntity> getParts() {
	Vector<DigitalEntity> links = new Vector<DigitalEntity>();
	for (RelatedDigitalEntity rel : related) {
	    if (rel.relation == DigitalEntityRelation.part_of.toString())
		links.add(rel.entity);
	}
	return links;
    }

    /**
     * @return the usage type is one more type
     */
    public String getUsageType() {
	return usageType;
    }

    /**
     * @param usageType
     *            set the usage type as you wish
     */
    public void setUsageType(String usageType) {
	this.usageType = usageType;
    }

    @Override
    public String toString() {
	return toString(this, 12, 0);
    }

    /**
     * @param digitalEntity
     *            the entity you want to convert to string
     * @param depth
     *            numbers of hierarchy
     * @param indent
     *            usually start with 0
     * @return a string representation of the digitalEntity
     */
    public String toString(DigitalEntity digitalEntity, int depth, int indent) {
	StringBuffer buffer = new StringBuffer();
	buffer.append(indent(indent));
	buffer.append(digitalEntity.getPid() + " (" + digitalEntity.getLabel()
		+ ") usage_type:" + digitalEntity.getUsageType() + "  parent:"
		+ digitalEntity.getParentPid());
	if (depth != 0) {
	    for (RelatedDigitalEntity related : digitalEntity.getRelated()) {
		buffer.append(toString(related.entity, depth - 1, indent + 1));
	    }
	}
	return buffer.toString();
    }

    private String indent(int indent) {
	StringBuffer buffer = new StringBuffer();
	buffer.append("\n");
	for (int i = 0; i < indent; i++)
	    buffer.append("\t");
	return buffer.toString();
    }

}
