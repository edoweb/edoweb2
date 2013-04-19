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
package de.nrw.hbz.ellinet.sync.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nrw.hbz.edoweb2.api.ObjectType;
import de.nrw.hbz.edoweb2.datatypes.ContentModel;
import de.nrw.hbz.edoweb2.sync.extern.DigitalEntity;
import de.nrw.hbz.edoweb2.sync.ingest.ControlBean;
import de.nrw.hbz.edoweb2.sync.ingest.IngestInterface;
import de.nrw.hbz.edoweb2.sync.ingest.Webclient;

/**
 * Class FedoraIngester
 * 
 * <p>
 * <em>Title: </em>
 * </p>
 * <p>
 * Description:
 * </p>
 * 
 * @author Jan Schnasse, schnasse@hbz-nrw.de
 * 
 */
public class EllinetIngester implements IngestInterface
{
	final static Logger logger = LoggerFactory.getLogger(EllinetIngester.class);

	final static String namespace = "ellinet";
	String host = null;
	Webclient webclient = null;

	public EllinetIngester(String usr, String pwd, String host)
	{
		this.host = host;
		webclient = new Webclient(namespace, usr, pwd, host);
	}

	@Override
	public ContentModel createContentModel()
	{
		return null;
	}

	@Override
	public void ingest(DigitalEntity dtlBean)
	{
		logger.info("Start ingest: " + namespace + ":" + dtlBean.getPid());

		String partitionC = null;
		String pid = null;
		pid = dtlBean.getPid();
		try
		{
			ControlBean control = new ControlBean(dtlBean);
			partitionC = control.getPartitionC().firstElement();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		try
		{

			if (partitionC.compareTo("HSS00DZM") == 0)
			{
				logger.info(pid + ": start ingesting ellinetObject");
				updateMonographs(dtlBean);
				logger.info(pid + ": end ingesting eJournal");
			}
			else
			{
				logger.warn("Unknown type: " + partitionC
						+ ". No further actions performed.");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			logger.info(e.getMessage());
		}

	}

	@Override
	public void update(DigitalEntity dtlBean)
	{
		ingest(dtlBean);
	}

	private void updateMonographs(DigitalEntity dtlBean)
	{
		String pid = namespace + ":" + dtlBean.getPid();
		logger.info(pid + " Found monograph.");
		String resource = host + ":8080/edoweb2-api/monograph/" + pid;
		webclient.createObject(resource, dtlBean, "application/pdf",
				ObjectType.monograph);
		webclient.metadata(resource, dtlBean, ObjectType.monograph);
		logger.info(pid + " " + "updated.\n");
	}

	@Override
	public void delete(String pid)
	{
		webclient.delete(pid);
	}
}
