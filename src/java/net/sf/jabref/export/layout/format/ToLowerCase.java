///////////////////////////////////////////////////////////////////////////////
//  Filename: $RCSfile: ToLowerCase.java,v $
//  Purpose:  Atom representation.
//  Language: Java
//  Compiler: JDK 1.4
//  Authors:  Egon Willighagen
//  Version:  $Revision: 1.2 $
//            $Date: 2004/05/02 16:25:43 $
//            $Author: mortenalver $
//
//  Copyright (c) Egon Willighagen
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


/**
 * Remove brackets formatter.
 *
 * @author $author$
 * @version $Revision: 1.2 $
 */
public class ToLowerCase implements LayoutFormatter
{
    //~ Methods ////////////////////////////////////////////////////////////////

    public String format(String fieldText)
    {
        String fieldEntry = fieldText.toLowerCase();
       return fieldEntry;
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
