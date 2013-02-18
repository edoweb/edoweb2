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
package de.nrw.hbz.edoweb2.archive;

import static de.nrw.hbz.edoweb2.datatypes.Vocabulary.REL_BELONGS_TO_OBJECT;
import static de.nrw.hbz.edoweb2.datatypes.Vocabulary.REL_IS_NODE_TYPE;
import static de.nrw.hbz.edoweb2.datatypes.Vocabulary.REL_IS_RELATED;
import static de.nrw.hbz.edoweb2.datatypes.Vocabulary.TYPE_OBJECT;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;

import de.nrw.hbz.edoweb2.datatypes.ComplexObject;
import de.nrw.hbz.edoweb2.datatypes.ComplexObjectNode;
import de.nrw.hbz.edoweb2.datatypes.Link;
import de.nrw.hbz.edoweb2.datatypes.Node;
import de.nrw.hbz.edoweb2.fedora.FedoraFacade;
import de.nrw.hbz.edoweb2.fedora.FedoraInterface;
import de.nrw.hbz.edoweb2.sesame.SesameFacade;

/**
 * Class Archive
 * 
 * <p>
 * <em>Title: </em>
 * </p>
 * <p>
 * 
 * </p>
 * 
 * @author Jan Schnasse, schnasse@hbz-nrw.de
 * 
 */
class Archive implements ArchiveInterface
{
	// final static Logger logger = LoggerFactory.
	// .getLogger(HBZFedoraIngester.class);

	private FedoraInterface fedoraInterface = null;
	private SesameFacade sesame = null;
	private static Archive me = null;

	public static Archive getInstance(String host, String user,
			String password, String sesameNativeStore)
	{
		if (me == null)
		{
			me = new Archive(host, user, password, sesameNativeStore);
		}
		return me;
	}

	private Archive(String host, String user, String password,
			String sesameNativeStore)
	{

		fedoraInterface = new FedoraFacade(host, user, password);
		sesame = new SesameFacade(user, password, sesameNativeStore);
	}

	public FedoraInterface getFedoraInterface()
	{
		return fedoraInterface;
	}

	public void setFedoraInterface(FedoraInterface my_fedoraInterface)
	{
		this.fedoraInterface = my_fedoraInterface;
	}

	@Override
	public Node createRootObject(String namespace)
	{
		Node rootObject = null;
		try
		{
			String pid = fedoraInterface.getPid(namespace);
			rootObject = new Node();
			rootObject.setPID(pid);
			rootObject.setLabel("Default Object");// (pid,
													// "Ein wunderschönes Objekt");
			rootObject.setNamespace(namespace);

			rootObject.setNodeType(TYPE_OBJECT);
			Link link = new Link();
			link.setPredicate(REL_IS_NODE_TYPE);
			link.setObject(TYPE_OBJECT, false);
			rootObject.addRelation(link);

			fedoraInterface.createNode(rootObject);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return rootObject;
	}

	private Node createObject(Node object)
	{
		Node rootObject = null;
		try
		{
			String pid = object.getPID();
			String namespace = object.getNamespace();
			if (namespace == null)
			{
				// TODO Do anything
				try
				{
					throw new Exception("Object has no namespace");
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (pid == null)
			{
				pid = fedoraInterface.getPid(namespace);
				object.setPID(pid);
			}

			fedoraInterface.createNode(object);

		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return rootObject;
	}

	@Override
	public Node createNode(String parentPid)
	{
		Node node = null;
		Node parent = null;
		try
		{
			parent = fedoraInterface.readNode(parentPid);
			String namespace = parent.getNamespace();
			String pid = fedoraInterface.getPid(namespace);
			node = new Node();
			node.setPID(pid);
			node.setLabel("Blank Node");// (pid,

			node.setNamespace(namespace);

		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}

		return createNode(parent, node);
	}

	@Override
	public Node createNode(Node parent, Node node)
	{

		try
		{
			String pid = node.getPID();
			String namespace = parent.getNamespace();// FedoraFacade.pred2pid(parent.getNamespace());
			if (pid == null)
			{
				pid = fedoraInterface.getPid(namespace);// String pid =
														// fedoraInterface.getPid(namespace);
				node.setPID(pid);
				node.setNamespace(namespace);
			}

			if (!fedoraInterface.nodeExists(pid))
			{
				parent.addRelation(node);
				node.setNamespace(namespace);
				fedoraInterface.createNode(node);
			}
			else
			{
				System.out.println("Das Objekt " + pid + " existiert bereits!");
				node = fedoraInterface.readNode(node.getPID());
				// Parent to node
				Link meToNode = new Link();
				meToNode.setPredicate(node.getHasNodeType());
				meToNode.setObject(addUriPrefix(node.getPID()), false);
				parent.addRelation(meToNode);

				// node to parent
				Link nodeToMe = new Link();
				nodeToMe.setPredicate(node.getIsNodeTypeOf());
				nodeToMe.setObject(addUriPrefix(parent.getPID()), false);
				node.addRelation(nodeToMe);

				fedoraInterface.updateNode(node);
				sesame.updateNode(node);
			}

			fedoraInterface.updateNode(parent);
			sesame.updateNode(node);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return node;
	}

	@Override
	public Node createComplexObject(ComplexObject tree)
	{
		Node object = tree.getRoot();

		createObject(object);

		for (int i = 0; i < tree.sizeOfChildren(); i++)
		{

			ComplexObjectNode node = tree.getChild(i);
			iterateCreate(node, object);
		}

		try
		{
			return readObject(object.getPID());
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	private void iterateCreate(ComplexObjectNode tnode, Node parent)
	{
		Node node = tnode.getMe();
		node = createNode(parent, node);
		for (int i = 0; i < tnode.sizeOfChildren(); i++)
		{
			ComplexObjectNode n1 = tnode.getChild(i);
			iterateCreate(n1, node);
		}
	}

	@Override
	public Node readObject(String rootPID) throws RemoteException
	{

		return fedoraInterface.readNode(rootPID);

	}

	@Override
	public ComplexObject readComplexObject(String rootPID)
			throws RemoteException
	{
		// RootObject object = new RootObject();
		Node object = fedoraInterface.readNode(rootPID);
		// object.update(node);

		ComplexObject complexObject = new ComplexObject(object);
		Vector<Link> rels = object.getRelsExt();

		for (Link rel : rels)
		{
			if (rel.getPredicate().compareTo(REL_IS_RELATED) == 0)
			{
				String pid = removeUriPrefix(rel.getObject());

				if (pid.compareTo(rootPID) == 0)
					continue;

				Node child = readNode(pid);
				ComplexObjectNode cn = new ComplexObjectNode(child);
				complexObject.addChild(cn);

				add(rootPID, cn, child.getRelsExt());
			}
		}

		return complexObject;
	}

	private void add(String rootPID, ComplexObjectNode cn, Vector<Link> rels)
			throws RemoteException
	{
		for (Link rel : rels)
		{
			if (rel.getPredicate().compareTo(REL_IS_RELATED) == 0)
			{
				String pid = removeUriPrefix(rel.getObject());
				if (pid.compareTo(rootPID) == 0)
					continue;
				Node child = readNode(pid);
				ComplexObjectNode cn2 = new ComplexObjectNode(child);
				cn.addChild(cn2);

				add(rootPID, cn2, child.getRelsExt());
			}
		}
	}

	@Override
	public Node readNode(String rootPID) throws RemoteException
	{
		Node node = fedoraInterface.readNode(rootPID);

		return node;
	}

	@Override
	public void updateNode(String nodePid, Node node)
	{
		try
		{
			node.setPID(nodePid);
			fedoraInterface.updateNode(node);
			// sesame.updateNode(node);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public String deleteComplexObject(String rootPID)
	{
		// logger.info("deleteObject");
		fedoraInterface.deleteNode(rootPID);

		// Find all children
		List<String> pids = null;
		try
		{
			pids = fedoraInterface.findPids("* <" + REL_BELONGS_TO_OBJECT
					+ "> <" + addUriPrefix(rootPID) + ">",
					FedoraFacade.TYPE_SPO);

			// Delete all children
			if (pids != null)
				for (String pid : pids)
				{

					// Remove relation
					Node node = readNode(pid);
					Vector<String> objects = node.getObjects();
					// If no object relation remains: delete
					if (objects == null || objects.size() == 1)
						fedoraInterface.deleteNode(node.getPID());
					else
					{
						System.out.println("Can not delete " + pid
								+ " node is shared by other objects.");
						node.removeRelation(REL_BELONGS_TO_OBJECT, rootPID);
						fedoraInterface.updateNode(node);
						sesame.updateNode(node);
					}
				}
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return rootPID;
	}

	@Override
	public List<String> findNodes(String searchTerm)
	{
		return fedoraInterface.findPids(searchTerm, FedoraFacade.TYPE_SIMPLE);
	}

	@Override
	public String[] getPids(String namespace, int number)
			throws RemoteException
	{
		return fedoraInterface.getPids(namespace, number);
	}

	@Override
	public String deleteNode(String pid)
	{
		fedoraInterface.deleteNode(pid);
		return pid;
	}

	@Override
	public void updateObject(String nodePid, Node object)
			throws RemoteException
	{
		updateNode(nodePid, object);
		sesame.updateNode(object);

	}

	@Override
	public void updateComplexObject(ComplexObject tree) throws RemoteException
	{
		Node object = tree.getRoot();

		for (int i = 0; i < tree.sizeOfChildren(); i++)
		{

			ComplexObjectNode node = tree.getChild(i);
			iterateUpdate(node, object);
		}

		try
		{
			updateObject(object.getPID(), object);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}

	}

	private void iterateUpdate(ComplexObjectNode tnode, Node parent)
	{
		Node node = tnode.getMe();
		updateNode(node.getPID(), node);
		sesame.updateNode(node);
		for (int i = 0; i < tnode.sizeOfChildren(); i++)
		{
			ComplexObjectNode n1 = tnode.getChild(i);
			iterateUpdate(n1, node);
		}
	}

	@Override
	public boolean nodeExists(String pid)
	{
		return fedoraInterface.nodeExists(pid);
	}

	@Override
	public InputStream findTriples(String rdfQuery, String queryType,
			String outputFormat)
	{
		return fedoraInterface.findTriples(rdfQuery, queryType, outputFormat);
	}

	@Override
	public String addUriPrefix(String pid)
	{

		return fedoraInterface.addUriPrefix(pid);
	}

	@Override
	public String removeUriPrefix(String pred)
	{

		return fedoraInterface.removeUriPrefix(pred);
	}
	/*
	 * #How is the #earth so #small. #elliptic #rider, #spaceegg, #transport of
	 * my #soul. #ahouuuuu
	 */
}
