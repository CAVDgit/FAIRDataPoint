/**
 * The MIT License
 * Copyright © 2016-2024 FAIR Data Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fairdatapoint.service.schema;

import org.fairdatapoint.util.RdfIOUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.Set;
import java.util.stream.Collectors;

import static org.fairdatapoint.util.ValueFactoryHelper.i;

public class MetadataSchemaShaclUtils {

    public static Set<String> extractTargetClasses(String definition) {
        final Model model = RdfIOUtil.read(definition, "");
        return model
                .filter(null, SHACL.TARGET_CLASS, null)
                .objects()
                .stream()
                .map(Value::stringValue)
                .filter(iri -> isRootNodeOfTargetClass(model, iri))
                .collect(Collectors.toSet());
    }

    private static boolean isRootNodeOfTargetClass(Model model, String iri) {
        final IRI resource = i(iri);
        for (Resource subject : model.filter(null, SHACL.TARGET_CLASS, resource).subjects()) {
            if (model.contains(null, null, subject)) {
                return false;
            }
        }
        return true;
    }
}
