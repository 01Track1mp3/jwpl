## JWPL and the Wikipedia Revision Toolkit ##

<table cellspacing='10'>
<tr>
<td valign='top'>
<b>JWPL (Java Wikipedia Library)</b> is a free, Java-based application programming interface that allows to access all information in Wikipedia.<br>
<br>
Core features:<br>
<ul><li>Fast and efficient access to Wikipedia<br>
</li><li>Parser for the MediaWiki syntax<br>
</li><li>Language independent</li></ul>

In addition to the core functionality, JWPL allows access to Wikipedia's edit history with the <b>Wikipedia Revision Toolkit</b>.<br>
<br>
Features of the Wikipedia Revision Toolkit:<br>
<ul><li>Tools for reconstructing past states of Wikipedia (TimeMachine)<br>
</li><li>Efficient access to all article revisions (RevisionMachine)<br>
</li><li>Dedicated revision storage format</li></ul>

More detailed information can be found in the <a href='JWPLDocumentation.md'>documentation</a> .<br>
<br>
Learn about the <a href='HowToGetJWPL.md'>different possibilities to get and use JWPL</a>

Any questions should be posted to the <a href='http://groups.google.com/group/jwpl'>JWPL Mailing List</a>.<br>
</td>
<td valign='top'>
<wiki:gadget url="http://www.ohloh.net/p/485502/widgets/project_factoids_stats.xml" width="400" height="270" border="0"/><br>
<table>
<tr>
<td valign='middle'></td>
<td valign='middle'><wiki:gadget url="http://www.ohloh.net/p/485502/widgets/project_users_logo.xml" height="43" border="0"/><br>
</td>
</tr>
</table>
</td>
</tr>
</table>

## News ##
JWPL 1.0.0 was released on Sept 11, 2014 and is available via [Maven Central](http://search.maven.org/#search|ga|1|tudarmstadt.ukp.wikipedia). If you use Maven as your build tool, you can directly add any JWPL component as a dependency to your _pom.xml_ without having to perform any additional configuration. You can find more information on the DeveloperSetup page. Non-Maven users can download individual fatjars with built-in dependencies from the [Maven Central](http://search.maven.org/#search|ga|1|tudarmstadt.ukp.wikipedia) website as well.

## Cite JWPL ##

If you use the **Wikipedia Revision Toolkit** (RevisionMachine, TimeMachine) in scientific work, please cite the [ACL 2011 demo paper](http://aclweb.org/anthology/P/P11/P11-4017.pdf) ([BibTeX](http://www.aclweb.org/anthology/P/P11/P11-4017.bib))

If you only use **JWPL Core** (API, DataMachine), please cite the [2008 LREC paper](http://www.ukp.tu-darmstadt.de/fileadmin/user_upload/Group_UKP/publikationen/2008/lrec08_camera_ready.pdf) ([BibTeX](http://www.ukp.tu-darmstadt.de/publications/details/?no_cache=1&tx_bibtex_pi1%5Bpub_id%5D=TUD-CS-2008-4))

## Code example ##

```
// configure the database connection parameters
DatabaseConfiguration dbConfig = new DatabaseConfiguration();
dbConfig.setHost("SERVER_URL");
dbConfig.setDatabase("DATABASE");
dbConfig.setUser("USER");
dbConfig.setPassword("PASSWORD");
dbConfig.setLanguage(Language.german);

// Create the Wikipedia object
Wikipedia wiki = new Wikipedia(dbConfig);
        
String title = "Hello world";
Page page = wiki.getPage(title);
        
// the title of the page
System.out.println("Queried string       : " + title);
System.out.println("Title                : " + page.getTitle());

// whether the page is a disambiguation page
System.out.println("IsDisambiguationPage : " + page.isDisambiguation());
        
// whether the page is a redirect
// If a page is a redirect, we can use it like a normal page.
// The other infos in this example are transparently served by the page that the redirect points to. 
System.out.println("redirect page query  : " + page.isRedirect());
        
// the number of links pointing to this page
System.out.println("# of ingoing links   : " + page.getNumberOfInlinks());
        
// the number of links in this page pointing to other pages
System.out.println("# of outgoing links  : " + page.getNumberOfOutlinks());

// the number of categories that are assigned to this page
System.out.println("# of categories      : " + page.getNumberOfCategories());
```

## Credits ##

JWPL was initially developed at [UKP Lab](http://www.ukp.tu-darmstadt.de/).
It is now maintained by
  * [Torsten Zesch](http://www.langtech.inf.uni-due.de/team/personal-profile-torsten-zesch/), Language Technology Lab, University of Duisburg-Essen
  * [Oliver Ferschke](http://www.ferschke.com), Language Technologies Institute, Carnegie Mellon University
  * [Richard Eckart de Castilho](http://www.ukp.tu-darmstadt.de/people/richard-eckart-de-castilho), UKP Lab, Technische Universität Darmstadt