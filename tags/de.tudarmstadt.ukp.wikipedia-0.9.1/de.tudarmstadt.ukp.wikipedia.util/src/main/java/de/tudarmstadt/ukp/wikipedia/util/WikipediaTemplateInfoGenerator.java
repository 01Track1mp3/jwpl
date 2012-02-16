/*******************************************************************************
 * Copyright (c) 2011 Ubiquitous Knowledge Processing (UKP) Lab
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Oliver Ferschke
 ******************************************************************************/
package de.tudarmstadt.ukp.wikipedia.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tudarmstadt.ukp.wikipedia.api.DatabaseConfiguration;
import de.tudarmstadt.ukp.wikipedia.api.Page;
import de.tudarmstadt.ukp.wikipedia.api.PageIterator;
import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.api.Wikipedia;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiApiException;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.Template;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.ShowTemplateNamesAndParameters;


/**
 * This class determines which page in a JWPL database contains which templates.
 * It produces an SQL file that will add this data to the extisting database.
 * It can then be accessed by the WikipediaTemplateInfo class.
 *
 * @author Oliver Ferschke
 *
 */
public class WikipediaTemplateInfoGenerator
{
    private MediaWikiParser parser=null;
	private final Log logger = LogFactory.getLog(getClass());
    private final Wikipedia wiki;
    private final PageIterator pageIter;
    private int pageCounter;

    private final Map<String,Set<Integer>> TPLNAME_TO_PAGEIDS = new HashMap<String,Set<Integer>>();
    private final Map<String,Integer> TPLNAME_TO_TPLID = new HashMap<String,Integer>();

    private final String charset;
    private final long maxAllowedPacket;
	private final String outputPath;

    protected final static String TABLE_TPLID_REVISIONID="templateId_revisionId";
    protected final static String TABLE_TPLID_PAGEID="templateId_pageId";
    protected final static String TABLE_TPLID_TPLNAME="templates";
    private final int VERBOSITY = 500;
    
    private HashSet<String> whiteList = new HashSet<String>();
    private HashSet<String> whitePrefixList = new HashSet<String>();
    private HashSet<String> blackList = new HashSet<String>();
    private HashSet<String> blackPrefixList = new HashSet<String>();
    

    
    

    public HashSet<String> getWhiteList()
	{
		return whiteList;
	}


	public void setWhiteList(HashSet<String> whiteList)
	{
		this.whiteList = whiteList;
	}


	public HashSet<String> getWhitePrefixList()
	{
		return whitePrefixList;
	}


	public void setWhitePrefixList(HashSet<String> whitePrefixList)
	{
		this.whitePrefixList = whitePrefixList;
	}


	public HashSet<String> getBlackList()
	{
		return blackList;
	}


	public void setBlackList(HashSet<String> blackList)
	{
		this.blackList = blackList;
	}


	public HashSet<String> getBlackPrefixList()
	{
		return blackPrefixList;
	}


	public void setBlackPrefixList(HashSet<String> blackPrefixList)
	{
		this.blackPrefixList = blackPrefixList;
	}


	public WikipediaTemplateInfoGenerator(Wikipedia pWiki, int pageBuffer, String charset, String outputPath, long maxAllowedPacket) throws WikiApiException{
        this.wiki = pWiki;
        this.pageIter = new PageIterator(wiki, true, pageBuffer);

        MediaWikiParserFactory pf = new MediaWikiParserFactory(wiki.getLanguage());
        pf.setTemplateParserClass(ShowTemplateNamesAndParameters.class);
        parser = pf.createParser();

        this.maxAllowedPacket=maxAllowedPacket;
        this.charset=charset;
        this.outputPath=outputPath;

        pageCounter=0;
    }


    public void process(){
		//TODO Check if template tables (tpl-page and tpl-revision) already
    	//exist and display a warning in that case - however, go on processing

    	logger.info("Processing pages, extracting template information ...");
    	while(pageIter.hasNext()){
    		pageCounter++;

    		if(pageCounter%VERBOSITY==0){
    			logger.info(pageCounter+" pages processed ...");
    		}

    		//TODO Add alternative for processing Revisions instead of Pages
    		Page curPage = pageIter.next();
    		int curPageId = curPage.getPageId();

    		Set<String> names = getTemplateNames(curPage.getText());

    		//Update the map with template values for current page
    		for(String name:names){

    			//filter templates - only use templates from a provided whitelist
        		if(acceptTemplate(name)){
        			//Create records for TEMPLATE->PAGES map
        			if(TPLNAME_TO_PAGEIDS.containsKey(name)){
            			//add the page id to the set for the current template
            			Set<Integer> pIdList = TPLNAME_TO_PAGEIDS.remove(name);
            			pIdList.add(curPageId);
            			TPLNAME_TO_PAGEIDS.put(name, pIdList);
            		}else{
            			//add new list with page id of current page
            			Set<Integer> newIdList = new HashSet<Integer>();
            			newIdList.add(curPageId);
            			TPLNAME_TO_PAGEIDS.put(name,newIdList);
            		}
        		}
    		}
    	}
		logger.info("Generating template indices ...");
		generateTemplateIndices();

    	logger.info("Writing SQL dump ...");
    	try{
    		writeSQL();
    	}catch(Exception e){
    		logger.error("Error while writing SQL dump.");
    		logger.error(e);
    	}
    }

    /**
     * Checks whether to include the template with the given name in the
     * database or not.
     *
     * @param tpl the template name
     * @return true, if the template should be included in the db
     */
    private boolean acceptTemplate(String tpl){
 
    	if(isInWhiteList(tpl) && !isInBlackList(tpl)) {
    		if(containsAllowedPrefix(tpl) && !containsRestrictedPrefix(tpl)) {
    			return true;
    		}else {
    			return false;
    		}
    	}else {
    		return false;
    	}

    }
    
    /**
     * Checks if the input string is in white list
     * @param tpl string to check
     * @return
     */
    private boolean isInWhiteList(String tpl) {
    	if ((!whiteList.isEmpty() && whiteList.contains(tpl)) || (whiteList.isEmpty())) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * Checks if the input string is in black list
     * @param tpl string to check
     * @return
     */
    private boolean isInBlackList(String tpl) {
    	if (blackList.contains(tpl)) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * Checks if the input string contains prefixes from white list
     * @param tpl string to check
     * @return
     */
    private boolean containsAllowedPrefix(String tpl) {
    	if(whitePrefixList.isEmpty())
    		return true;
    	
    	for(String i :whitePrefixList) {
    		if(tpl.startsWith(i))
    			return true;
    	}
    	return false;
    }
    
    /**
     * Checks if the input string contains prefixes from black list
     * @param tpl string to check
     * @return
     */
    private boolean containsRestrictedPrefix(String tpl) {
    	for(String i :blackPrefixList) {
    		if(tpl.startsWith(i))
    			return true;
    	}
    	return false;
    }
    
    private boolean tableExists;

    /**
     * Fills a map with the template names and gives them a unique int-key,
     * which is later on used as a key in the db.
     */
    private void generateTemplateIndices(){
    	try {
			WikipediaTemplateInfo info = new WikipediaTemplateInfo(this.wiki);
			tableExists = true;
			for(String name:TPLNAME_TO_PAGEIDS.keySet()){
				int id = info.checkTemplateId(name);
				if (id != -1) {
					TPLNAME_TO_TPLID.put(name, id);
				}
			}
			
		}
		catch (WikiApiException e) {

		}
    }
    
    private void writeSQL() throws Exception{
    	Writer writer = null;
    	try{
	    	writer=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath), charset));

	    	writer.write("CREATE TABLE IF NOT EXISTS "+TABLE_TPLID_PAGEID+" ("
					+ "templateId INTEGER UNSIGNED NOT NULL,"
					+ "pageId INTEGER UNSIGNED NOT NULL, UNIQUE(templateId, pageId));");
			writer.write("\r\n");
	    	writer.write("CREATE TABLE IF NOT EXISTS "+TABLE_TPLID_TPLNAME+" ("
					+ "templateId INTEGER NOT NULL AUTO_INCREMENT,"
					+ "templateName MEDIUMTEXT NOT NULL, "
					+ "PRIMARY KEY(templateId));");
			writer.write("\r\n");

			/*
			 * Generate insert statements and values
			 */


			for(Entry<String,Set<Integer>> e:TPLNAME_TO_PAGEIDS.entrySet()){

				String curTemplateName = e.getKey();
					
	    		Set<Integer> curPageIds = e.getValue();
	    		
	    		if(!curTemplateName.isEmpty()&&!curPageIds.isEmpty()){
	    			String id = "LAST_INSERT_ID()";
	    			if(!TPLNAME_TO_TPLID.containsKey(curTemplateName)) {
	    				writer.write("INSERT INTO "+TABLE_TPLID_TPLNAME+" (templateName) VALUES ('"+curTemplateName+"');");
		    			writer.write("\r\n");
	    			}else {
	    				id = TPLNAME_TO_TPLID.get(curTemplateName).toString();
	    			}
	    				
	    			StringBuilder curValues = new StringBuilder();
		    		for(Integer pId:curPageIds){
		    			if(curValues.length()>0){
		    				curValues.append(",");
		    			}
		    			curValues.append("("+id+", ");
		    			curValues.append(pId);
		    			curValues.append(")");
		    		}
		    		writer.write("REPLACE INTO " + TABLE_TPLID_PAGEID + " VALUES "+ curValues+";");
		    		writer.write("\r\n");
		    		
	    		}
	    	}
			if(!tableExists) {
				writer.write("CREATE INDEX pageIdx ON "+TABLE_TPLID_PAGEID+"(pageId);");
		    	writer.write("\r\n");
		    	writer.write("CREATE INDEX tplIdx ON "+TABLE_TPLID_PAGEID+"(templateID);");
		    	writer.write("\r\n");
			}
	    	

    	}catch(IOException e){
    		logger.error("Error writing SQL file: "+e.getMessage());
    	}finally{
    		try{
        		if(writer!=null){
        			writer.close();
        		}
    		}catch(IOException e){
    			logger.error("Error closing stream: "+e.getMessage());
    		}
    	}
    }


	/**
	 * Returns the set of names of all templates that are contained in the given
	 * article (without duplicates).<br/>
	 * The names are SQL escaped using StringUtils.sqlEscape
	 *
	 * @param p
	 *            the page to get the templates from
	 * @return a set of template names (without duplicates)
	 */
    private Set<String> getTemplateNames(String pageText){
        Set<String> names = new HashSet<String>();
    	if(!pageText.isEmpty()){
            try{
    	    	ParsedPage pp = parser.parse(pageText);
            	List<Template> templates = pp.getTemplates();
    	        for(Template t: templates){
    	        	names.add(StringUtils.sqlEscape(t.getName().toLowerCase()));
    	        }
            }catch(Exception e){
            	//Most likely parsing problems
            	e.printStackTrace();
            	logger.warn("Problems parsing page");
            }
    	}
        return names;
    }

	/**
	 * Starts index generation using the database credentials in the properties
	 * file specified in args[0].<br/>
	 * The properties file should have the following structure:
	 * <ul>
	 * <li>host=dbhost</li>
	 * <li>db=revisiondb</li>
	 * <li>user=username</li>
	 * <li>password=pwd</li>
	 * <li>language=english</li>
	 * <li>output=outputFile</li>
	 * <li>charset=UTF8 (or others) (optional)</li>
	 * <li>pagebuffer=5000 (optional)</li>
	 * <li>maxAllowedPackets=16760832 (optional)</li>
	 * </ul>
	 * <br/>
	 *
	 * @param args
	 *            allows only one entry that contains the path to the config
	 *            file
	 */
	public static void main(String[] args)
	{

		if (args == null || args.length != 1) {
			System.out
					.println(("You need to specify the database configuration file. \n"
							+ "It should contain the access credentials to you revision database in the following format: \n"
							+ "  host=dbhost \n"
							+ "  db=revisiondb \n"
							+ "  user=username \n"
							+ "  password=pwd \n"
							+ "  language=english \n"
							+ "  output=outputFile \n"
							+ "  charset=UTF8 (optional)\n"
							+ "  pagebuffer=5000 (optional)\n"
							+ "  maxAllowedPackets=16760832 (optional)"));
			throw new IllegalArgumentException();
		}
		else {
			Properties props = load(args[0]);

			DatabaseConfiguration config = new DatabaseConfiguration();
			
			config.setHost(props.getProperty("host"));
			config.setDatabase(props.getProperty("db"));
			config.setUser(props.getProperty("user"));
			config.setPassword(props.getProperty("password"));
			config.setLanguage(Language.valueOf(props.getProperty("language")));

			String charset = props.getProperty("charset");

			String pagebufferString = props.getProperty("pagebuffer");
			int pageBuffer;

			String maxAllowedPacketsString = props.getProperty("maxAllowedPackets");
			long maxAllowedPackets;
			
			try {
				if (charset == null) {
					charset="UTF-8";
				}

				if (pagebufferString != null) {
					pageBuffer=Integer.parseInt(pagebufferString);
				}
				else {
					pageBuffer=5000;
				}

				if (maxAllowedPacketsString != null) {
					maxAllowedPackets= Long.parseLong(maxAllowedPacketsString);
				}
				else {
					maxAllowedPackets=(16 * 1024 * 1023);
				}

				String output = props.getProperty("output");
				File outfile = new File(output);
				if (outfile.isDirectory()) {
					try {
						output= outfile.getCanonicalPath()
										+ File.separatorChar
										+ "templateInfo.sql";
					}
					catch (IOException e) {
						output = outfile.getPath()
										+ File.separatorChar
										+ "templateInfo.sql";
					}
				}

				WikipediaTemplateInfoGenerator generator = new WikipediaTemplateInfoGenerator(
						new Wikipedia(config),
						pageBuffer,
						charset,
						output,
						maxAllowedPackets);

				generator.setWhiteList(generator.createSetFromProperty(props.getProperty("white_list")));
				generator.setBlackList(generator.createSetFromProperty(props.getProperty("black_list")));
				generator.setWhitePrefixList(generator.createSetFromProperty(props.getProperty("white_prefix_list")));
				generator.setBlackPrefixList(generator.createSetFromProperty(props.getProperty("black_prefix_list")));
				//Start processing now
				generator.process();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Parses property string into HashSet
	 * @param property string to parse
	 * @return
	 */
	public HashSet<String> createSetFromProperty(String property)
	{
		HashSet<String> properties = new HashSet<String>();
		
		if (property != null && !property.equals("null")) {
			// "([\\w]*)=([\\w]*);"
			Pattern params = Pattern.compile("([\\w]+)[;]*");
			Matcher matcher = params.matcher(property.trim());
			while (matcher.find()) {
				properties.add(matcher.group(1));
			}
			
		}
		
		return properties;
	}

	/**
	 * Loads a properties file from disk
	 *
	 * @param propsName
	 *            path to the configuration file
	 * @return Properties the properties object containing the configuration
	 *         data
	 * @throws IOException
	 *             if an error occurs while accessing the configuration file
	 */
	private static Properties load(String configFilePath)
	{
		Properties props = new Properties();
		FileInputStream fis = null;
		try {
			File configFile = new File(configFilePath);
	        fis = new FileInputStream(configFile);
	        props.load(fis);
        }
        catch(IOException e){
        	System.err.println("Could not load configuration file "+configFilePath);
        }
        finally{
			if(fis!=null){
			    try {
					fis.close();
				}
				catch (IOException e) {
		        	System.err.println("Error closing file stream of configuration file "+configFilePath);
				}
			}
        }
        return props;
	}
}
