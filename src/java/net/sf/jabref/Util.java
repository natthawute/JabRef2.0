/*
 Copyright (C) 2003 Morten O. Alver

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */

package net.sf.jabref;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import javax.swing.*;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.builder.DefaultFormBuilder;

import net.sf.jabref.export.LatexFieldFormatter;
import net.sf.jabref.groups.*;
import net.sf.jabref.imports.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

/**
 * Describe class <code>Util</code> here.
 *
 * @author <a href="mailto:"> </a>
 * @version 1.0
 */
public class Util {

    // Colors are defined here.
    public static Color fieldsCol = new Color(180, 180, 200);

    // Integer values for indicating result of duplicate check (for entries):
    final static int TYPE_MISMATCH = -1
    ,
    NOT_EQUAL = 0
    ,
    EQUAL = 1
    ,
    EMPTY_IN_ONE = 2
    ,
    EMPTY_IN_TWO = 3;

    public static void bool(boolean b) {
        if (b) System.out.println("true");
        else System.out.println("false");
    }

    public static void pr(String s) {
        System.out.println(s);
    }

    public static void pr_(String s) {
        System.out.print(s);
    }

    public static String nCase(String s) {
        // Make first character of String uppercase, and the
        // rest lowercase.
        if (s.length() > 1) return s.substring(0, 1).toUpperCase()
                + s.substring(1, s.length()).toLowerCase();
        else return s.toUpperCase();

    }

    public static String checkName(String s) {
        // Append '.bib' to the string unless it ends with that.
        String extension = s.substring(s.length() - 4);
        if (!extension.equalsIgnoreCase(".bib")) return s + ".bib";
        else return s;
    }

    private static int idCounter = 0;

    public synchronized static String createNeutralId() {
        return String.valueOf(idCounter++);
    }

    /**
     * This method sets the location of a Dialog such that it is centered with
     * regard to another window, but not outside the screen on the left and the
     * top.
     */
    public static void placeDialog(java.awt.Dialog diag,
                                   java.awt.Container win) {
        Dimension ds = diag.getSize(), df = win.getSize();
        Point pf = win.getLocation();
        diag.setLocation(new Point(Math
                .max(0, pf.x + (df.width - ds.width) / 2), Math.max(0, pf.y
                + (df.height - ds.height) / 2)));

    }

    /**
     * This method translates a field or string from Bibtex notation, with
     * possibly text contained in " " or { }, and string references,
     * concatenated by '#' characters, into Bibkeeper notation, where string
     * references are enclosed in a pair of '#' characters.
     */
    public static String parseField(String content) {
        if (content.length() == 0) return "";
        String toSet = "";
        boolean string;
        // Keeps track of whether the next item is
        // a reference to a string, or normal content. First we must
        // check which we begin with. We simply check if we can find
        // a '#' before either '"' or '{'.
        int hash = content.indexOf('#'), wr1 = content.indexOf('"'), wr2 = content
                .indexOf('{'), end = content.length();
        if (hash == -1) hash = end;
        if (wr1 == -1) wr1 = end;
        if (wr2 == -1) wr2 = end;
        string = ((wr1 == end) && (wr2 == end)) || (hash < Math.min(wr1, wr2));

        //System.out.println("FileLoader: "+content+" "+string+" "+hash+"
        // "+wr1+" "+wr2);
        StringTokenizer tok = new StringTokenizer(content, "#", true);
        // 'tok' splits at the '#' sign, and keeps delimiters

        while (tok.hasMoreTokens()) {
            String str = tok.nextToken();
            if (str.equals("#")) string = !string;
            else {
                if (string) {
                    // This part should normally be a string, but if it's
                    // a pure number, it is not.
                    String s = shaveString(str);
                    try {
                        Integer.parseInt(s);
                        // If there's no exception, it's a number.
                        toSet = toSet + s;
                    } catch (NumberFormatException ex) {
                        toSet = toSet + "#" + shaveString(str) + "#";
                    }

                } else toSet = toSet + shaveString(str);
            }
        }
        return toSet;
    }

    public static String shaveString(String s) {
        // returns the string, after shaving off whitespace at the beginning
        // and end, and removing (at most) one pair of braces or " surrounding
        // it.
        if (s == null) return null;
        char ch, ch2;
        int beg = 0, end = s.length();
        // We start out assuming nothing will be removed.
        boolean begok = false, endok = false;
        while (!begok) {
            if (beg < s.length()) {
                ch = s.charAt(beg);
                if (Character.isWhitespace(ch)) beg++;
                else begok = true;
            } else begok = true;

        }
        while (!endok) {
            if (end > beg + 1) {
                ch = s.charAt(end - 1);
                if (Character.isWhitespace(ch)) end--;
                else endok = true;
            } else endok = true;
        }

        if (end > beg + 1) {
            ch = s.charAt(beg);
            ch2 = s.charAt(end - 1);
            if (((ch == '{') && (ch2 == '}')) || ((ch == '"') && (ch2 == '"'))) {
                beg++;
                end--;
            }
        }
        s = s.substring(beg, end);
        //Util.pr(s);
        return s;
    }

    /**
     * This method returns a String similar to the one passed in, except that it is
     * molded into a form that is acceptable for bibtex.
     */
    public static String checkLegalKey(String key) {
        if (key == null) return null;
        StringBuffer newKey = new StringBuffer();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!Character.isWhitespace(c) && (c != '#') && (c != '{')  && (c != '\\')
                    && (c != '"')
                    && (c != '}') && (c != '~') && (c != ',') && (c != '^')) newKey
                    .append(c);
        }

        // Replace non-english characters like umlauts etc. with a sensible
        // letter or letter combination that bibtex can accept.
        String newKeyS = replaceSpecialCharacters(newKey.toString());

        return newKeyS;
    }

    /**
     * Replace non-english characters like umlauts etc. with a sensible
     * letter or letter combination that bibtex can accept. The basis
     * for replacement is the HashMap GLobals.UNICODE_CHARS.
     */
    public static String replaceSpecialCharacters(String s) {
        for (Iterator i = Globals.UNICODE_CHARS.keySet().iterator(); i.hasNext();) {
            String chr = (String) i.next(),
                    replacer = (String) Globals.UNICODE_CHARS.get(chr);
            //pr(chr+" "+replacer);
            s = s.replaceAll(chr, replacer);
        }
        return s;
    }

    static public String _wrap2(String in, int wrapAmount) {
        // The following line cuts out all whitespace and replaces them with single
        // spaces:
        //in = in.replaceAll("[ ]+"," ").replaceAll("[\\t]+"," ");
        //StringBuffer out = new StringBuffer(in);
        StringBuffer out = new StringBuffer(in.replaceAll("[ \\t\\r]+", " "));


        int p = in.length() - wrapAmount;
        int lastInserted = -1;
        while (p > 0) {
            p = out.lastIndexOf(" ", p);
            if (p <= 0 || p <= 20) break;
            int lbreak = out.indexOf("\n", p);
            System.out.println(lbreak + " " + lastInserted);
            if ((lbreak > p) && ((lastInserted >= 0) && (lbreak < lastInserted))) {
                p = lbreak - wrapAmount;
            } else {
                out.insert(p, "\n\t");
                lastInserted = p;
                p -= wrapAmount;
            }
        }
        return out.toString();
    }

    static public String wrap2(String in, int wrapAmount) {
        return net.sf.jabref.imports.FieldContentParser.wrap(in, wrapAmount);
    }

    static public String __wrap2(String in, int wrapAmount) {
        // The following line cuts out all whitespace except line breaks, and replaces
        // with single spaces. Line breaks are padded with a tab character:
        StringBuffer out = new StringBuffer(in.replaceAll("[ \\t\\r]+", " "));

        int p = 0;
        //int lastInserted = -1;
        while (p < out.length()) {
            int q = out.indexOf(" ", p + wrapAmount);
            if ((q < 0) || (q >= out.length()))
                break;
            int lbreak = out.indexOf("\n", p);
            //System.out.println(lbreak);
            if ((lbreak > p) && (lbreak < q)) {
                p = lbreak + 1;
                int piv = lbreak + 1;
                if ((out.length() > piv) && !(out.charAt(piv) == '\t'))
                    out.insert(piv, "\n\t");

            } else {
                //System.out.println(q+" "+out.length());
                out.deleteCharAt(q);
                out.insert(q, "\n\t");
                p = q + 1;
            }
        }
        return out.toString();//.replaceAll("\n", "\n\t");
    }

    public static HashSet findDeliminatedWordsInField(BibtexDatabase db,
                                                      String field, String deliminator) {
        HashSet res = new HashSet();
        Iterator i = db.getKeySet().iterator();
        while (i.hasNext()) {
            BibtexEntry be = db.getEntryById(i.next().toString());
            Object o = be.getField(field);
            if (o != null) {
                String fieldValue = o.toString().trim();
                StringTokenizer tok = new StringTokenizer(fieldValue, deliminator);
                while (tok.hasMoreTokens()) res.add(tok.nextToken().trim());
            }
        }
        return res;
    }

    /**
     * Returns a HashMap containing all words used in the database in the given
     * field type. Characters in
     *
     * @param remove are not included.
     * @param db     a <code>BibtexDatabase</code> value
     * @param field  a <code>String</code> value
     * @param remove a <code>String</code> value
     * @return a <code>HashSet</code> value
     */
    public static HashSet findAllWordsInField(BibtexDatabase db, String field,
                                              String remove) {
        HashSet res = new HashSet();
        StringTokenizer tok;
        Iterator i = db.getKeySet().iterator();
        while (i.hasNext()) {
            BibtexEntry be = db.getEntryById(i.next().toString());
            Object o = be.getField(field);
            if (o != null) {
                tok = new StringTokenizer(o.toString(), remove, false);
                while (tok.hasMoreTokens()) res.add(tok.nextToken());
            }
        }
        return res;
    }

    /**
     * Takes a String array and returns a string with the array's elements
     * delimited by a certain String.
     *
     * @param strs      String array to convert.
     * @param delimiter String to use as delimiter.
     * @return Delimited String.
     */
    public static String stringArrayToDelimited(String[] strs, String delimiter) {
        if ((strs == null) || (strs.length == 0)) return "";
        if (strs.length == 1) return strs[0];
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < strs.length - 1; i++) {
            sb.append(strs[i]);
            sb.append(delimiter);
        }
        sb.append(strs[strs.length - 1]);
        return sb.toString();
    }

    /**
     * Takes a delimited string, splits it and returns
     *
     * @param names a <code>String</code> value
     * @return a <code>String[]</code> value
     */
    public static String[] delimToStringArray(String names, String delimiter) {
        if (names == null) return null;
        return names.split(delimiter);
    }

    /**
     * Open a http/pdf/ps viewer for the given link string.
     */
    public static void openExternalViewer(String link, String fieldName,
                                          JabRefPreferences prefs) throws IOException {

        if (fieldName.equals("ps") || fieldName.equals("pdf")) {
            //System.out.println(expandFilename(link, prefs.get(fieldName+"Directory")));
            File file = expandFilename(link, prefs.get(fieldName + "Directory"));

            // Check that the file exists:
            if ((file == null) || !file.exists()) {
                throw new IOException(
                        Globals.lang("File not found") + " (" + fieldName + "): '" + link + "'.");
            }
            link = file.getCanonicalPath();

            // Use the correct viewer even if pdf and ps are mixed up:
            String[] split = file.getName().split("\\.");
            if (split.length >= 2) {
                if (split[split.length - 1].equalsIgnoreCase("pdf"))
                    fieldName = "pdf";
                else if (split[split.length - 1].equalsIgnoreCase("ps") ||
                        (split.length >= 3 && split[split.length - 2].equalsIgnoreCase("ps")))
                    fieldName = "ps";
            }
        } else if (fieldName.equals("doi")) {
            fieldName = "url";
            // Check to see if link field already contains a well formated URL
            if (!link.startsWith("http://")) {
                link = Globals.DOI_LOOKUP_PREFIX + link;
            }
        } else if (fieldName.equals("citeseerurl")) {
            fieldName = "url";

            String canonicalLink = CiteSeerFetcher.generateCanonicalURL(link);
            if (canonicalLink != null)
                link = canonicalLink;
        }

        String cmdArray[] = new String[2];
        if (fieldName.equals("url")) { // html
            try {

                // First check if the url is enclosed in \\url{}. If so, remove
                // the wrapper.
                if (link.startsWith("\\url{") && link.endsWith("}")) link = link
                        .substring(5, link.length() - 1);

                //System.err.println("Starting HTML browser: "
                //                   + prefs.get("htmlviewer") + " " + link);
                if (Globals.ON_MAC) {
                    String[] cmd = {"/usr/bin/open", "-a",
                            prefs.get("htmlviewer"), link};
                    Process child = Runtime.getRuntime().exec(cmd);
                } else if (Globals.ON_WIN) {
                    openFileOnWindows(link, false);
                    /*cmdArray[0] = prefs.get("htmlviewer");
                    cmdArray[1] = link;
                    Process child = Runtime.getRuntime().exec(
                    cmdArray[0] + " " + cmdArray[1]);
                    */
                } else {
                    cmdArray[0] = prefs.get("htmlviewer");
                    cmdArray[1] = link;
                    Process child = Runtime.getRuntime().exec(cmdArray);
                }

            } catch (IOException e) {
                System.err.println("An error occured on the command: "
                        + prefs.get("htmlviewer") + " " + link);
            }
        } else if (fieldName.equals("ps")) {
            try {
                if (Globals.ON_MAC) {
                    String[] cmd = {"/usr/bin/open", "-a",
                            prefs.get("psviewer"), link};
                    Process child = Runtime.getRuntime().exec(cmd);
                } else if (Globals.ON_WIN) {
                    openFileOnWindows(link, true);
                    /*
                    cmdArray[0] = prefs.get("psviewer");
                    cmdArray[1] = link;
                    Process child = Runtime.getRuntime().exec(
                            cmdArray[0] + " " + cmdArray[1]);
                    */
                } else {
                    cmdArray[0] = prefs.get("psviewer");
                    cmdArray[1] = link;
                    Process child = Runtime.getRuntime().exec(cmdArray);
                }
            } catch (IOException e) {
                System.err.println("An error occured on the command: "
                        + prefs.get("psviewer") + " " + link);
            }
        } else if (fieldName.equals("pdf")) {
            try {
                if (Globals.ON_MAC) {
                    String[] cmd = {"/usr/bin/open", "-a",
                            prefs.get("pdfviewer"), link};
                    Process child = Runtime.getRuntime().exec(cmd);
                } else if (Globals.ON_WIN) {
                    openFileOnWindows(link, true);
                    /*
                    String[] spl = link.split("\\\\");
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < spl.length; i++) {
                        if (i > 0) sb.append("\\");
                        if (spl[i].indexOf(" ") >= 0) spl[i] = "\"" + spl[i]
                                + "\"";
                        sb.append(spl[i]);

                    }
                    //pr(sb.toString());
                    link = sb.toString();

                    String cmd = "cmd.exe /c start " + link;

                    Process child = Runtime.getRuntime().exec(cmd);
                    */
                } else {
                    cmdArray[0] = prefs.get("pdfviewer");
                    cmdArray[1] = link;
                    //Process child = Runtime.getRuntime().exec(cmdArray[0]+"
                    // "+cmdArray[1]);
                    Process child = Runtime.getRuntime().exec(cmdArray);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("An error occured on the command: "
                        + prefs.get("pdfviewer") + " #" + link);
                System.err.println(e.getMessage());
            }
        } else {
            System.err
                    .println("Message: currently only PDF, PS and HTML files can be opened by double clicking");
            //ignore
        }
    }

    /**
     * Opens a file on a Windows system, using its default viewer.
     * @param link The file name.
     * @param localFile true if it is a local file, not an URL.
     * @throws IOException
     */
    public static void openFileOnWindows(String link, boolean localFile) throws IOException {
        /*if (localFile) {
            String[] spl = link.split("\\\\");
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < spl.length; i++) {
                if (i > 0) sb.append("\\");
                if (spl[i].indexOf(" ") >= 0) spl[i] = "\"" + spl[i]
                        + "\"";
                sb.append(spl[i]);
            }
            link = sb.toString();
	    }*/
	link = link.replaceAll("&", "\"&\"").replaceAll(" ", "\" \"");
        String cmd = "cmd.exe /c start " + link;
        Process child = Runtime.getRuntime().exec(cmd);
    }

    /**
     * Searches the given directory and subdirectories for a pdf file with name
     * as given + ".pdf"
     */
    public static String findPdf(String key, String extension, String directory, OpenFileFilter off) {
        //String filename = key + "."+extension;

        /* Simon Fischer's patch for replacing a regexp in keys before converting to filename:

        String regex = Globals.prefs.get("basenamePatternRegex");
        if ((regex != null) && (regex.trim().length() > 0)) {
            String replacement = Globals.prefs.get("basenamePatternReplacement");
            key = key.replaceAll(regex, replacement);
        }
        */
        if (!directory.endsWith(System.getProperty("file.separator"))) directory += System
                .getProperty("file.separator");
        String found = findInDir(key, directory, off);
        if (found != null) return found.substring(directory.length());
        else return null;
    }


    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * null if the file does not exist.
     */
    public static File expandFilename(String name, String dir) {
        //System.out.println("expandFilename: name="+name+"\t dir="+dir);

        File file = new File(name);
        if (!file.exists() && (dir != null)) {
            if (dir.endsWith(System.getProperty("file.separator"))) name = dir
                    + name;
            else name = dir + System.getProperty("file.separator") + name;

            //System.out.println("expanded to: "+name);
            //if (name.startsWith("ftp"))

            file = new File(name);
            if (file.exists()) return file;
            // Ok, try to fix / and \ problems:
            if (Globals.ON_WIN) name = name.replaceAll("/", "\\");
            else name = name.replaceAll("\\\\", "/");
            //System.out.println("expandFilename: "+name);
            file = new File(name);
            return (file.exists() ? file : null);
        } else return file;
    }

    private static String findInDir(String key, String dir, OpenFileFilter off) {
        File f = new File(dir);
        File[] all = f.listFiles();
        if (all == null)
            return null;  // An error occured. We may not have
        // permission to list the files.

        int numFiles = all.length;

        for (int i = 0; i < numFiles; i++) {
            File curFile = all[i];

            if (curFile.isFile()) {
                String name = curFile.getName();
                if (name.startsWith(key + ".") &&
                        off.accept(name))
                    return curFile.getPath();

            } else if (curFile.isDirectory()) {
                String found = findInDir(key, curFile.getPath(), off);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /**
     * Checks if the two entries represent the same publication.
     *
     * @param one BibtexEntry
     * @param two BibtexEntry
     * @return boolean
     */
    public static boolean isDuplicate(BibtexEntry one, BibtexEntry two,
                                      float threshold) {

        // First check if they are of the same type - a necessary condition:
        if (one.getType() != two.getType()) return false;

        // The check if they have the same required fields:
        String[] fields = one.getType().getRequiredFields();

        if (fields == null) return false;

        float req = compareFieldSet(fields, one, two);
        fields = one.getType().getOptionalFields();

        if (fields != null) {
            float opt = compareFieldSet(fields, one, two);
            return (2 * req + opt) / 3 >= threshold;
        } else {
            return (req >= threshold);
        }
    }

    /**
     * Goes through all entries in the given database, and if at least one of
     * them is a duplicate of the given entry, as per Util.isDuplicate(BibtexEntry, BibtexEntry),
     * the duplicate is returned. The search is terminated when the first duplicate is found.
     *
     * @param database The database to search.
     * @param entry    The entry of which we are looking for duplicates.
     * @return The first duplicate entry found. null if no duplicates are found.
     */
    public static BibtexEntry containsDuplicate(BibtexDatabase database, BibtexEntry entry) {
        Collection entries = database.getEntries();
        for (Iterator i = entries.iterator(); i.hasNext();) {
            BibtexEntry other = (BibtexEntry) i.next();
            if (isDuplicate(entry, other, Globals.duplicateThreshold))
                return other; // Duplicate found.
        }
        return null; // No duplicate found.
    }

    private static float compareFieldSet(String[] fields, BibtexEntry one,
                                         BibtexEntry two) {
        int res = 0;
        for (int i = 0; i < fields.length; i++) {
            //Util.pr(":"+compareSingleField(fields[i], one, two));
            if (compareSingleField(fields[i], one, two) == EQUAL) {
                res++;
                //Util.pr(fields[i]);
            }
        }
        return ((float) res) / ((float) fields.length);
    }

    private static int compareSingleField(String field, BibtexEntry one,
                                          BibtexEntry two) {
        String s1 = (String) one.getField(field), s2 = (String) two
                .getField(field);
        if (s1 == null) {
            if (s2 == null) return EQUAL;
            else return EMPTY_IN_ONE;
        } else if (s2 == null) return EMPTY_IN_TWO;
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        //Util.pr(field+": '"+s1+"' vs '"+s2+"'");
        if (field.equals("author") || field.equals("editor")) {
            // Specific for name fields.
            // Harmonise case:
            String[] aus1 = AuthorList.fixAuthor_lastNameFirst(s1)
                    .split(" and "), aus2 = AuthorList
                    .fixAuthor_lastNameFirst(s2).split(" and "), au1 = aus1[0]
                    .split(","), au2 = aus2[0].split(",");

            // Can check number of authors, all authors or only the first.
            if ((aus1.length > 0) && (aus1.length == aus2.length)
                    && au1[0].trim().equals(au2[0].trim())) return EQUAL;
            else return NOT_EQUAL;
        } else {
            if (s1.trim().equals(s2.trim())) return EQUAL;
            else return NOT_EQUAL;
        }

    }

    public static double compareEntriesStrictly(BibtexEntry one, BibtexEntry two) {
        HashSet allFields = new HashSet();//one.getAllFields());
        Object[] o = one.getAllFields();
        for (int i = 0; i < o.length; i++)
            allFields.add(o[i]);
        o = two.getAllFields();
        for (int i = 0; i < o.length; i++)
            allFields.add(o[i]);
        int score = 0;
        for (Iterator fld = allFields.iterator(); fld.hasNext();) {
            String field = (String) fld.next();
            Object en = one.getField(field), to = two.getField(field);
            if ((en != null) && (to != null) && (en.equals(to))) score++;
            else if ((en == null) && (to == null)) score++;
        }
        if (score == allFields.size()) return 1.01; // Just to make sure we can
            // use score>1 without
            // trouble.
        else return ((double) score) / allFields.size();
    }

    /**
     * This methods assures all words in the given entry are recorded in their
     * respective Completers, if any.
     */
    /*
     * public static void updateCompletersForEntry(Hashtable autoCompleters,
     * BibtexEntry be) {
     * 
     * for (Iterator j=autoCompleters.keySet().iterator(); j.hasNext();) {
     * String field = (String)j.next(); Completer comp =
     * (Completer)autoCompleters.get(field); comp.addAll(be.getField(field)); } }
     */

    /**
     * Sets empty or non-existing owner fields of bibtex entries inside an array
     * to a specified default value. Timestamp field is also set. Preferences are
     * checked to see if these options are enabled.
     *
     * @param bibs List of bibtex entries
     */
    public static void setAutomaticFields(List bibs) {

        String defaultOwner = Globals.prefs.get("defaultOwner");
        String timestamp = easyDateFormat();
        // Iterate through all entries
        for (int i = 0; i < bibs.size(); i++) {
            // Get current entry
            BibtexEntry curEntry = (BibtexEntry) bibs.get(i);

            // Set owner field if this option is enabled:
            if (Globals.prefs.getBoolean("useOwner")) {
                // No or empty owner field?
                if (curEntry.getField(Globals.OWNER) == null
                        || ((String) curEntry.getField(Globals.OWNER)).length() == 0) {
                    // Set owner field to default value
                    curEntry.setField(Globals.OWNER, defaultOwner);
                }
            }

            if (Globals.prefs.getBoolean("useTimeStamp"))
                curEntry.setField(Globals.prefs.get("timeStampField"), timestamp);
        }

    }

    /**
     * Copies a file.
     *
     * @param source         File Source file
     * @param dest           File Destination file
     * @param deleteIfExists boolean Determines whether the copy goes on even if the file
     *                       exists.
     * @throws IOException
     * @return boolean Whether the copy succeeded, or was stopped due to the
     * file already existing.
     */
    public static boolean copyFile(File source, File dest,
                                   boolean deleteIfExists) throws IOException {

        // Check if the file already exists.
        if (dest.exists()) {
            if (!deleteIfExists) return false;
            //else dest.delete();
        }

        BufferedInputStream in = new BufferedInputStream(new FileInputStream(
                source));
        BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(dest));
        int el;
        //int tell = 0;
        while ((el = in.read()) >= 0) {
            out.write(el);
        }
        out.flush(); // Will this make a difference? 2005.09.30
        in.close();
        out.close();
        return true;
    }

    /**
     * This method is called at startup, and makes necessary adaptations to
     * preferences for users from an earlier version of Jabref.
     */
    public static void performCompatibilityUpdate() {

        // Make sure "abstract" is not in General fields, because
        // Jabref 1.55 moves the abstract to its own tab.
        String genFields = Globals.prefs.get("generalFields");
        //pr(genFields+"\t"+genFields.indexOf("abstract"));
        if (genFields.indexOf("abstract") >= 0) {
            //pr(genFields+"\t"+genFields.indexOf("abstract"));
            String newGen;
            if (genFields.equals("abstract")) newGen = "";
            else if (genFields.indexOf(";abstract;") >= 0) {
                newGen = genFields.replaceAll(";abstract;", ";");
            } else if (genFields.indexOf("abstract;") == 0) {
                newGen = genFields.replaceAll("abstract;", "");
            } else if (genFields.indexOf(";abstract") == genFields.length() - 9) {
                newGen = genFields.replaceAll(";abstract", "");
            } else newGen = genFields;
            //pr(newGen);
            Globals.prefs.put("generalFields", newGen);
        }

    }

    // -------------------------------------------------------------------------------

    /**
     * extends the filename with a default Extension, if no Extension '.x' could
     * be found
     */
    public static String getCorrectFileName(String orgName,
                                            String defaultExtension) {
        if (orgName == null) return "";

        String back = orgName;
        int t = orgName.indexOf(".", 1); // hidden files Linux/Unix (?)
        if (t < 1) back = back + "." + defaultExtension;

        return back;
    }
    
    /** Quotes each and every character, e.g. '!' as &#33;. Used for
     *  verbatim display of arbitrary strings that may contain HTML entities. */
    public static String quoteForHTML(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            sb.append("&#"+(int)s.charAt(i)+";");
        }
        return sb.toString();
    }
    
    public static String quote(String s, String specials, char quoteChar) {
        return quote(s, specials, quoteChar, 0);
    }

    /**
     * Quote special characters.
     *
     * @param s         The String which may contain special characters.
     * @param specials  A String containing all special characters except the quoting
     *                  character itself, which is automatically quoted.
     * @param quoteChar The quoting character.
     * @param linewrap  The number of characters after which a linebreak is inserted
     *                  (this linebreak is undone by unquote()). Set to 0 to disable.
     * @return A String with every special character (including the quoting
     *         character itself) quoted.
     */
    public static String quote(String s, String specials, char quoteChar,
                               int linewrap) {
        StringBuffer sb = new StringBuffer();
        char c;
        int linelength = 0;
        boolean isSpecial;
        for (int i = 0; i < s.length(); ++i) {
            c = s.charAt(i);
            isSpecial = specials.indexOf(c) >= 0 || c == quoteChar;
            // linebreak?
            if (linewrap > 0
                    && (++linelength >= linewrap || (isSpecial && linelength >= linewrap - 1))) {
                sb.append(quoteChar);
                sb.append('\n');
                linelength = 0;
            }
            if (isSpecial) {
                sb.append(quoteChar);
                ++linelength;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Unquote special characters.
     *
     * @param s         The String which may contain quoted special characters.
     * @param quoteChar The quoting character.
     * @return A String with all quoted characters unquoted.
     */
    public static String unquote(String s, char quoteChar) {
        StringBuffer sb = new StringBuffer();
        char c;
        boolean quoted = false;
        for (int i = 0; i < s.length(); ++i) {
            c = s.charAt(i);
            if (quoted) { // append literally...
                if (c != '\n') // ...unless newline
                    sb.append(c);
                quoted = false;
            } else if (c != quoteChar) {
                sb.append(c);
            } else { // quote char
                quoted = true;
            }
        }
        return sb.toString();
    }

    /**
     * Quote all regular expression meta characters in s, in order to
     * search for s literally.
     */
    public static String quoteMeta(String s) {
        // work around a bug: trailing backslashes have to be quoted individually
        int i = s.length() - 1;
        StringBuffer bs = new StringBuffer("");
        while ((i >= 0) && (s.charAt(i) == '\\')) {
            --i;
            bs.append("\\\\");
        }
        s = s.substring(0, i + 1);
        return "\\Q" + s.replaceAll("\\\\E", "\\\\E\\\\\\\\E\\\\Q") + "\\E" + bs.toString();
    }

    /*
     * This method "tidies" up e.g. a keyword string, by alphabetizing the words and
     * removing all duplicates.
     */
    public static String sortWordsAndRemoveDuplicates(String text) {

        String[] words = text.split(", ");
        SortedSet set = new TreeSet();
        for (int i = 0; i < words.length; i++)
            set.add(words[i]);
        StringBuffer sb = new StringBuffer();
        for (Iterator i = set.iterator(); i.hasNext();) {
            sb.append(i.next());
            sb.append(", ");
        }
        if (sb.length() > 2)
            sb.delete(sb.length() - 2, sb.length());
        String result = sb.toString();
        return result.length() > 2 ? result : "";
    }

    /**
     * Warns the user of undesired side effects of an explicit
     * assignment/removal of entries to/from this group. Currently there are
     * four types of groups: AllEntriesGroup, SearchGroup - do not support
     * explicit assignment. ExplicitGroup - never modifies entries. KeywordGroup -
     * only this modifies entries upon assignment/removal. Modifications are
     * acceptable unless they affect a standard field (such as "author") besides
     * the "keywords" field.
     *
     * @param parent The Component used as a parent when displaying a
     *               confirmation dialog.
     * @return true if the assignment has no undesired side effects, or the user
     *         chose to perform it anyway. false otherwise (this indicates that
     *         the user has aborted the assignment).
     */
    public static boolean warnAssignmentSideEffects(AbstractGroup[] groups,
                                                    BibtexEntry[] entries, BibtexDatabase db, Component parent) {
        Vector affectedFields = new Vector();
        for (int k = 0; k < groups.length; ++k) {
            if (groups[k] instanceof KeywordGroup) {
                KeywordGroup kg = (KeywordGroup) groups[k];
                String field = kg.getSearchField().toLowerCase();
                if (field.equals("keywords"))
                    continue; // this is not undesired
                for (int i = 0; i < GUIGlobals.ALL_FIELDS.length; ++i) {
                    if (field.equals(GUIGlobals.ALL_FIELDS[i])) {
                        affectedFields.add(field);
                        break;
                    }
                }
            }
        }
        if (affectedFields.size() == 0)
            return true; // no side effects

        // show a warning, then return
        StringBuffer message = // JZTODO lyrics...
                new StringBuffer("This action will modify the following field(s)\n" +
                        "in at least one entry each:\n");
        for (int i = 0; i < affectedFields.size(); ++i)
            message.append(affectedFields.elementAt(i)).append("\n");
        message.append("This could cause undesired changes to "
                + "your entries, so it is\nrecommended that you change the grouping field "
                + "in your group\ndefinition to \"keywords\" or a non-standard name."
                + "\n\nDo you still want to continue?");
        int choice = JOptionPane.showConfirmDialog(parent, message,
                Globals.lang("Warning"), JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return choice != JOptionPane.NO_OPTION;

//        if (groups instanceof KeywordGroup) {
//            KeywordGroup kg = (KeywordGroup) groups;
//            String field = kg.getSearchField().toLowerCase();
//            if (field.equals("keywords")) 
//                return true; // this is not undesired
//            for (int i = 0; i < GUIGlobals.ALL_FIELDS.length; ++i) {
//                if (field.equals(GUIGlobals.ALL_FIELDS[i])) {
//                    // show a warning, then return
//                    String message = Globals // JZTODO lyrics...
//                            .lang(
//                                    "This action will modify the \"%0\" field "
//                                            + "of your entries.\nThis could cause undesired changes to "
//                                            + "your entries, so it is\nrecommended that you change the grouping field "
//                                            + "in your group\ndefinition to \"keywords\" or a non-standard name."
//                                            + "\n\nDo you still want to continue?",
//                                    field);
//                    int choice = JOptionPane.showConfirmDialog(parent, message,
//                            Globals.lang("Warning"), JOptionPane.YES_NO_OPTION,
//                            JOptionPane.WARNING_MESSAGE);
//                    return choice != JOptionPane.NO_OPTION;
//                }
//            }
//        }
//        return true; // found no side effects
    }

    //========================================================
    // lot of abreviations in medline
    // PKC etc convert to {PKC} ...
    //========================================================
    static Pattern titleCapitalPattern = Pattern.compile("[A-Z]+");

    /**
     * Wrap all uppercase letters, or sequences of uppercase letters, in
     * curly braces. Ignore letters within a pair of # character, as these
     * are part of a string label that should not be modified.
     * @param s The string to modify.
     * @return The resulting string after wrapping capitals.
     */
    public static String putBracesAroundCapitals(String s) {

        boolean inString=false, isBracing=false, escaped=false;
        int inBrace = 0;
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<s.length(); i++) {
            // Update variables based on special characters:
            int c = s.charAt(i);
            if (c == '{') inBrace++;
            else if (c == '}') inBrace--;
            else if (!escaped && (c == '#'))
                inString = !inString;

            // See if we should start bracing:
            if ((inBrace == 0) && !isBracing && !inString
                && Character.isLetter((char)c)
                && Character.isUpperCase((char)c)) {

                buf.append('{');
                isBracing = true;
            }

            // See if we should close a brace set:
            if (isBracing &&
                    !(Character.isLetter((char)c)
                        &&Character.isUpperCase((char)c))) {

                buf.append('}');
                isBracing = false;
            }

            // Add the current character:
            buf.append((char)c);

            // Check if we are entering an escape sequence:
            if ((c == '\\') && !escaped)
                escaped = true;
            else
                escaped = false;

        }
        // Check if we have an unclosed brace:
        if (isBracing)
            buf.append('}');

        return buf.toString();

        /*if (s.length() == 0) return s; // Protect against ArrayIndexOutOf....
        StringBuffer buf = new StringBuffer();

        Matcher mcr = titleCapitalPattern.matcher(s.substring(1));
        while (mcr.find()) {
            String replaceStr = mcr.group();
            mcr.appendReplacement(buf, "{" + replaceStr + "}");
        }
        mcr.appendTail(buf);
        return s.substring(0, 1) + buf.toString();*/
    }

    static Pattern bracedTitleCapitalPattern = Pattern.compile("\\{[A-Z]+\\}");

    /**
     * This method looks for occurences of capital letters enclosed in an arbitrary number of
     * pairs of braces, e.g. "{AB}" or "{{T}}". All of these pairs of braces are removed.
     * @param s The String to analyze.
     * @return A new String with braces removed.
     */
    public static String removeBracesAroundCapitals(String s) {
        String previous = s;
        while ((s = removeSingleBracesAroundCapitals(s)).length() < previous.length()) {
            previous = s;
        }
        return s;
    }

    /**
     * This method looks for occurences of capital letters enclosed in one pair of braces, e.g. "{AB}".
     * All these are replaced by only the capitals in between the braces.
     * @param s The String to analyze.
     * @return A new String with braces removed.
     */
    public static String removeSingleBracesAroundCapitals(String s) {
        Matcher mcr = bracedTitleCapitalPattern.matcher(s);
        StringBuffer buf = new StringBuffer();
        while (mcr.find()) {
            String replaceStr = mcr.group();
            mcr.appendReplacement(buf, replaceStr.substring(1, replaceStr.length()-1));
        }
        mcr.appendTail(buf);
        return buf.toString();
    }

    /**
     * This method looks up what kind of external binding is used for the given field,
     * and constructs on OpenFileFilter suitable for browsing for an external file.
     *
     * @param fieldName The BibTeX field in question.
     * @return The file filter.
     */
    public static OpenFileFilter getFileFilterForField(String fieldName) {
        String s = (String) GUIGlobals.FIELD_EXTRAS.get(fieldName);
        final String ext = "." + fieldName.toLowerCase();
        final OpenFileFilter off;
        if (s.equals("browseDocZip"))
            off = new OpenFileFilter(new String[]{ext, ext + ".gz", ext + ".bz2"});
        else
            off = new OpenFileFilter(new String[]{ext});
        return off;
    }

    /**
     * This method can be used to display a "rich" error dialog which offers the entire
     * stack trace for an exception.
     *
     * @param parent
     * @param e
     */
    public static void showQuickErrorDialog(JFrame parent, String title, Exception e) {
        // create and configure a text area - fill it with exception text.
        final JPanel pan = new JPanel(),
                details = new JPanel();
        final CardLayout crd = new CardLayout();
        pan.setLayout(crd);
        final JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Sans-Serif", Font.PLAIN, 10));
        textArea.setEditable(false);
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        textArea.setText(writer.toString());
        JLabel lab = new JLabel(e.getMessage());
        JButton flip = new JButton(Globals.lang("Details"));

        FormLayout layout = new FormLayout("left:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append(lab);
        builder.nextLine();
        builder.append(Box.createVerticalGlue());
        builder.nextLine();
        builder.append(flip);
        final JPanel simple = builder.getPanel();

        // stuff it in a scrollpane with a controlled size.
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(350, 150));
        details.setLayout(new BorderLayout());
        details.add(scrollPane, BorderLayout.CENTER);

        flip.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                crd.show(pan, "details");
            }
        });
        pan.add(simple, "simple");
        pan.add(details, "details");
        // pass the scrollpane to the joptionpane.
        JOptionPane.showMessageDialog(parent, pan, title, JOptionPane.ERROR_MESSAGE);
    }

    public static String wrapHTML(String s, final int lineWidth) {
        StringBuffer sb = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(s);
        int charsLeft = lineWidth;
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken();
            if (charsLeft == lineWidth) { // fresh line
                sb.append(word);
                charsLeft -= word.length();
                if (charsLeft <= 0) {
                    sb.append("<br>\n");
                    charsLeft = lineWidth;
                }
            } else { // continue previous line
                if (charsLeft < word.length() + 1) {
                    sb.append("<br>\n");
                    sb.append(word);
                    if (word.length() >= lineWidth - 1) {
                        sb.append("<br>\n");
                        charsLeft = lineWidth;
                    } else {
                        sb.append(" ");
                        charsLeft = lineWidth - word.length() - 1;
                    }
                } else {
                    sb.append(' ').append(word);
                    charsLeft -= word.length() + 1;
                }
            }
        }
        return sb.toString();
    }

    /**
     * Creates a String containing the current date (and possibly time), formatted
     * according to the format set in preferences under the key "timeStampFormat".
     *
     * @return The date string.
     */
    public static String easyDateFormat() {
        String format = Globals.prefs.get("timeStampFormat");
        Date today = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(today);
    }


    public static void markEntry(BibtexEntry be, NamedCompound ce) {
        Object o = be.getField(Globals.MARKED);
        if ((o != null) && (o.toString().indexOf(Globals.prefs.WRAPPED_USERNAME) >= 0))
            return;
        String newValue;
        if (o == null) {
            newValue = Globals.prefs.WRAPPED_USERNAME;
        } else {
            StringBuffer sb = new StringBuffer(o.toString());
            //sb.append(' ');
            sb.append(Globals.prefs.WRAPPED_USERNAME);
            newValue = sb.toString();
        }
        ce.addEdit(new UndoableFieldChange(be, Globals.MARKED,
                                          be.getField(Globals.MARKED), newValue));
        be.setField(Globals.MARKED, newValue);
    }

    public static void unmarkEntry(BibtexEntry be, BibtexDatabase database, NamedCompound ce) {
        Object o = be.getField(Globals.MARKED);
        if (o != null) {
            String s = o.toString();
            if (s.equals("0")) {
                unmarkOldStyle(be, database, ce);
                return;
            }

            int piv=0, hit;
            StringBuffer sb = new StringBuffer();
            while ((hit = s.indexOf(Globals.prefs.WRAPPED_USERNAME, piv)) >= 0) {
                if (hit > 0)
                    sb.append(s.substring(piv, hit));
                piv = hit+Globals.prefs.WRAPPED_USERNAME.length();
            }
            if (piv < s.length()-1) {
                sb.append(s.substring(piv));
            }
            String newVal = sb.length()>0 ? sb.toString() : null;
            ce.addEdit(new UndoableFieldChange(be, Globals.MARKED,
                be.getField(Globals.MARKED), newVal));
            be.setField(Globals.MARKED, newVal);
        }
    }

    /**
     * An entry is marked with a "0", not in the new style with user names. We want to
     * unmark it as transparently as possible. Since this shouldn't happen too often,
     * we do it by scanning the "owner" fields of the entire database, collecting all
     * user names. We then mark the entry for all users except the current one. Thus
     * only the user who unmarks will see that it is unmarked, and we get rid of the
     * old-style marking.
     * @param be
     * @param ce
     */
    private static void unmarkOldStyle(BibtexEntry be, BibtexDatabase database, NamedCompound ce) {
        TreeSet owners = new TreeSet();
        for (Iterator i=database.getEntries().iterator(); i.hasNext();) {
            BibtexEntry entry = (BibtexEntry)i.next();
            Object o = entry.getField(Globals.OWNER);
            if (o != null)
                owners.add(o);
            //System.out.println("Owner: "+entry.getField(Globals.OWNER));
        }
        owners.remove(Globals.prefs.get("defaultOwner"));
        StringBuffer sb = new StringBuffer();
        for (Iterator i=owners.iterator(); i.hasNext();) {
            sb.append('[');
            sb.append(i.next().toString());
            sb.append(']');
        }
        String newVal = sb.toString();
        if (newVal.length() == 0)
            newVal = null;
        ce.addEdit(new UndoableFieldChange(be, Globals.MARKED, be.getField(Globals.MARKED), newVal));
        be.setField(Globals.MARKED, newVal);

    }

    public static boolean isMarked(BibtexEntry be) {
        Object fieldVal = be.getField(Globals.MARKED);
        if (fieldVal == null)
            return false;
        String s = (String)fieldVal;
        return (s.equals("0") || (s.indexOf(Globals.prefs.WRAPPED_USERNAME) >= 0));
    }

    /**
     * Make a list of supported character encodings that can encode all characters in the given String.
     * @param characters A String of characters that should be supported by the encodings.
     * @return A List of character encodings
     */
    public static List findEncodingsForString(String characters) {
        List encodings = new ArrayList();
        for (int i=0; i<Globals.ENCODINGS.length; i++) {
            CharsetEncoder encoder = Charset.forName(Globals.ENCODINGS[i]).newEncoder();
            if (encoder.canEncode(characters))
                encodings.add(Globals.ENCODINGS[i]);
        }
        return encodings;
    }

}
