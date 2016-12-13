package org.cytosm.cypher2sql.utils;

import java.util.Arrays;
import java.util.List;

import org.cytosm.cypher2sql.cypher.ast.Statement;

/**
 * Converts an AST into a string with tab spacing for each depth of nesting.
 *
 */
public final class ASTPrinter {

    private ASTPrinter() {}

    // TODO better way to define this? Instead of hard-coding the strings.
    private static final List<String> NO_SPLIT = Arrays.asList("LabelName", "Variable", "PropertyKeyName",
            "SignedDecimalIntegerLiteral", "RelTypeName", "StringLiteral", "UnsignedDecimalIntegerLiteral",
            "FunctionName", "count", "CountStar");

    /***
     * AST Print States.
     */
    private enum States {
        STARTING, TOKEN, BETWEEN
    }

    /**
     * Returns a string representation of the Statement with the nesting set for the depth.
     *
     * @param statement Statement to print
     * @return string representation with depth via tabs.
     */
    public static String printAST(final Statement statement) {
        String theString = statement.toString();
        States state = States.STARTING;
        StringBuilder theStrings = new StringBuilder();
        StringBuilder temp = new StringBuilder("");
        int tab = 0;
        int noSplit = 0;

        for (int i = 0; i < theString.length(); i++) {
            char cTemp = theString.charAt(i);
            switch (cTemp) {
                case '(':
                    if (state == States.STARTING) {
                        state = States.BETWEEN;
                    } else if (state == States.TOKEN) {
                        String word = temp.toString().replaceAll("\\s", "");
                        state = States.BETWEEN;
                        if (NO_SPLIT.contains(word.replace(",", ""))) {
                            if (noSplit == 0) {
                                if (word.startsWith(",")) {
                                    if ((i + 1) < theString.length() && theString.charAt(i + 1) == ')') {
                                        theStrings.append(getNTabs(tab) + word + cTemp);
                                    } else {
                                        theStrings.append("\n" + getNTabs(tab) + word + cTemp);
                                    }
                                } else {
                                    theStrings.append(getNTabs(tab) + word + cTemp);
                                }
                            } else {
                                theStrings.append(word + cTemp);
                            }
                            noSplit++;
                        } else {
                            if (noSplit == 0) {
                                if (word.startsWith(",")) {
                                    if ((i + 1) < theString.length() && theString.charAt(i + 1) == ')') {
                                        theStrings.append("\n" + getNTabs(tab) + word + cTemp);
                                    } else {
                                        theStrings.append("\n" + getNTabs(tab) + word + cTemp + "\n");
                                    }
                                } else {
                                    if ((i + 1) < theString.length() && theString.charAt(i + 1) == ')') {
                                        theStrings.append(getNTabs(tab) + word + cTemp);
                                    } else {
                                        theStrings.append(getNTabs(tab) + word + cTemp + "\n");
                                    }
                                }
                            } else {
                                theStrings.append(word + cTemp);
                            }

                        }
                        temp.delete(0, temp.length());
                    }
                    tab++;
                    break;
                case ')':
                    tab--;
                    String word = temp.toString().replaceAll("\\s", "");
                    if (state == States.TOKEN) {
                        if (noSplit == 0) {
                            theStrings.append("\n" + getNTabs(tab) + word + cTemp);
                        } else {
                            theStrings.append(word + cTemp);
                            noSplit--;
                        }
                        temp.delete(0, temp.length());
                        state = States.BETWEEN;
                    } else if (state == States.BETWEEN) {
                        if (noSplit == 0) {
                            theStrings.append("\n" + getNTabs(tab) + cTemp);
                        } else {
                            theStrings.append(cTemp);
                            noSplit--;
                        }
                    }
                    break;
                default:
                    state = States.TOKEN;
                    temp.append(cTemp);
            }
        }

        return theStrings.toString();
    }

    private static String getNTabs(final int tab) {
        StringBuilder tabs = new StringBuilder();
        for (int i = 0; i < tab; i++) {
            tabs.append("\t");
        }
        return tabs.toString();
    }
}
