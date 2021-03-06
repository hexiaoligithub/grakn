/*
 * GRAKN.AI - THE KNOWLEDGE GRAPH
 * Copyright (C) 2018 Grakn Labs Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ai.grakn.graql.internal.template;

import ai.grakn.exception.GraqlSyntaxException;
import ai.grakn.graql.internal.antlr.GraqlTemplateLexer;
import ai.grakn.graql.internal.antlr.GraqlTemplateParser;
import ai.grakn.graql.internal.parser.GraqlErrorListener;
import ai.grakn.graql.internal.template.macro.BooleanMacro;
import ai.grakn.graql.internal.template.macro.ConcatMacro;
import ai.grakn.graql.internal.template.macro.DateMacro;
import ai.grakn.graql.internal.template.macro.DoubleMacro;
import ai.grakn.graql.internal.template.macro.EqualsMacro;
import ai.grakn.graql.internal.template.macro.IntMacro;
import ai.grakn.graql.internal.template.macro.LongMacro;
import ai.grakn.graql.internal.template.macro.LowerMacro;
import ai.grakn.graql.internal.template.macro.NoescpMacro;
import ai.grakn.graql.internal.template.macro.SplitMacro;
import ai.grakn.graql.internal.template.macro.StringMacro;
import ai.grakn.graql.internal.template.macro.UpperMacro;
import ai.grakn.graql.macro.Macro;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for parsing Graql templates and associated data into Graql statements.
 *
 * @author alexandraorth
 */
public class TemplateParser {

    private final Map<String, Macro<?>> macros = new HashMap<>();

    /**
     * Create a template parser.
     */
    private TemplateParser(){}

    /**
     * Create a template parser.
     * @return the created template parser
     */
    public static TemplateParser create(){
        TemplateParser parser = new TemplateParser();
        parser.registerDefaultMacros();
        return parser;
    }

    /**
     * Register a macro that can be used in any template parsed by this class.
     * @param macro macro that can be called in templates
     */
    public void registerMacro(Macro macro){
        macros.put(macro.name(), macro);
    }

    /**
     * Parse and resolve a graql template.
     * @param templateString a string representing a graql template
     * @param data data to use in template
     * @return resolved graql query string
     */
    public String parseTemplate(String templateString, Map<String, Object> data){
        GraqlErrorListener errorListener = GraqlErrorListener.of(templateString);

        CommonTokenStream tokens = lexGraqlTemplate(templateString, errorListener);
        ParseTree tree = parseGraqlTemplate(tokens, errorListener);

        TemplateVisitor visitor = new TemplateVisitor(tokens, data, macros);
        return visitor.visit(tree).toString();
    }


    private CommonTokenStream lexGraqlTemplate(String templateString, GraqlErrorListener errorListener){
        ANTLRInputStream inputStream = new ANTLRInputStream(templateString);
        GraqlTemplateLexer lexer = new GraqlTemplateLexer(inputStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        return new CommonTokenStream(lexer);
    }

    private ParseTree parseGraqlTemplate(CommonTokenStream tokens, GraqlErrorListener errorListener){
        GraqlTemplateParser parser = new GraqlTemplateParser(tokens);
        parser.setBuildParseTree(true);

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        ParseTree tree = parser.template();

        if(errorListener.hasErrors()){
            throw GraqlSyntaxException.create(errorListener.toString());
        }

        return tree;
    }

    /**
     * Register the default macros that can be used by the visitor
     */
    private void registerDefaultMacros(){
        registerMacro(new NoescpMacro());
        registerMacro(new IntMacro());
        registerMacro(new DoubleMacro());
        registerMacro(new EqualsMacro());
        registerMacro(new StringMacro());
        registerMacro(new LongMacro());
        registerMacro(new DateMacro());
        registerMacro(new LowerMacro());
        registerMacro(new UpperMacro());
        registerMacro(new BooleanMacro());
        registerMacro(new SplitMacro());
        registerMacro(new ConcatMacro());
    }
}
