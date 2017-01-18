///////////////////////////////////////////////////////////////////////////////
//  Filename: $RCSfile: XMLChars.java,v $
//  Purpose:  Atom representation.
//  Language: Java
//  Compiler: JDK 1.4
//  Authors:  Joerg K. Wegner, Morten O. Alver
//  Version:  $Revision: 1.2 $
//            $Date: 2004/11/28 23:25:27 $
//            $Author: mortenalver $
//
//  Copyright (c) Dept. Computer Architecture, University of Tuebingen, Germany
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation version 2 of the License.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
///////////////////////////////////////////////////////////////////////////////

package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import java.util.regex.*;
import java.util.Iterator;
import net.sf.jabref.Util;
import net.sf.jabref.Globals;

/**
 * Changes {\^o} or {\^{o}} to ?
 *
 * @author $author$
 * @version $Revision: 1.2 $
 */
public class XMLChars implements LayoutFormatter
{
    //~ Methods ////////////////////////////////////////////////////////////////
    //Pattern pattern = Pattern.compile(".*\\{..[a-zA-Z].\\}.*");
    Pattern pattern = Pattern.compile(".*\\{\\\\.*[a-zA-Z]\\}.*");

    public String format(String fieldText)
    {
 
	fieldText = firstFormat(fieldText);

	//if (!pattern.matcher(fieldText).matches())
	//    return restFormat(fieldText);
        
	for (Iterator i=Globals.HTML_CHARS.keySet().iterator(); i.hasNext();) {
	    String s = (String)i.next();         
            String repl = (String)Globals.XML_CHARS.get(s);
            if (repl != null)
                fieldText = fieldText.replaceAll(s, repl);
	}
	//RemoveBrackets rb = new RemoveBrackets();
	return restFormat(fieldText);
    }

    private String firstFormat(String s) {
	return s.replaceAll("&|\\\\&","&#x0026;").replaceAll("--", "&#x2013;");
    }

    private String restFormat(String s) {
	return s.replaceAll("\\}","").replaceAll("\\{","").replaceAll("<", "&#x3c;");
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
