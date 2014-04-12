/*
 * 03/08/2004
 *
 * SyntaxConstants.java - Constants used by RSyntaxTextArea and friends.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;


/**
 * Constants that define the different programming languages understood by
 * <code>RSyntaxTextArea</code>.  These constants are the values you can pass
 * to {@link RSyntaxTextArea#setSyntaxEditingStyle(String)} to get syntax
 * highlighting.<p>
 *
 * By default, all <code>RSyntaxTextArea</code>s can render all of these
 * languages, but this can be changed (the list can be augmented or completely
 * overwritten) on a per-text area basis.  What languages can be rendered is
 * actually managed by the {@link TokenMakerFactory} installed on the text
 * area's {@link RSyntaxDocument}.  By default, all
 * <code>RSyntaxDocumenet</code>s have a factory installed capable of handling
 * all of these languages.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public interface SyntaxConstants {

	/**
	 * Style meaning don't syntax highlight anything.
	 */
	public static final String SYNTAX_STYLE_NONE			= "text/plain";
	public static final String SYNTAX_STYLE_JAVASCRIPT		= "text/javascript";

}