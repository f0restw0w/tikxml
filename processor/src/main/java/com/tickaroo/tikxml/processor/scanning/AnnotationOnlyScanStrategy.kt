/*
 * Copyright (C) 2015 Hannes Dorfmann
 * Copyright (C) 2015 Tickaroo, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.tickaroo.tikxml.processor.scanning

import com.tickaroo.tikxml.annotation.*
import com.tickaroo.tikxml.processor.ProcessingException
import com.tickaroo.tikxml.processor.converter.AttributeConverterChecker
import com.tickaroo.tikxml.processor.converter.PropertyElementConverterChecker
import com.tickaroo.tikxml.processor.model.AttributeField
import com.tickaroo.tikxml.processor.model.Field
import com.tickaroo.tikxml.processor.model.PropertyField
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.collections.isEmpty
import kotlin.text.isEmpty

/**
 * A [ScanStrategy] that scans the element only for annotations
 * @author Hannes Dorfmann
 */
open class AnnotationOnlyScanStrategy(elementUtils: Elements, typeUtils: Types, requiredDetector: RequiredDetector) : ScanStrategy(elementUtils, typeUtils, requiredDetector) {

    val listTypeMirror: TypeMirror

    init {
        listTypeMirror = typeUtils.erasure(elementUtils.getTypeElement("java.util.List").asType())
    }

    override fun isXmlField(element: VariableElement): Field? {

        var annotationFound = 0;


        val ignoreAnnotation = element.getAnnotation(IgnoreXml::class.java)

        // Ignore xml
        if (ignoreAnnotation != null)
            return null


        // MAIN ANNOTATIONS
        val attributeAnnotation = element.getAnnotation(Attribute::class.java)
        val propertyAnnotation = element.getAnnotation(PropertyElement::class.java)
        val elementAnnotation = element.getAnnotation(Element::class.java)

        if (attributeAnnotation != null) {
            annotationFound++;
        }

        if (propertyAnnotation != null) {
            annotationFound++
        }

        if (elementAnnotation != null) {
            annotationFound++
        }

        // No annotations
        if (annotationFound == 0) {
            return null
        }

        if (annotationFound > 1) {
            // More than one annotation is not allowed
            throw ProcessingException(element, "Fields can ONLY be annotated with one of the "
                    + "following annotations @${Attribute::class.simpleName}, "
                    + "@${PropertyElement::class.simpleName} or @${Element::class.simpleName} "
                    + "and not multiple of them! The field ${element.simpleName.toString()} in class "
                    + "${(element.enclosingElement as TypeElement).qualifiedName} is annotated with more than one of these annotations. You must annotate a field with exactly one of these annotations (not multiple)!")
        }


        if (attributeAnnotation != null) {

            val converterChecker = AttributeConverterChecker()
            return AttributeField(element,
                    nameFromAnnotationOrFieldName(attributeAnnotation.name, element),
                    requiredDetector.isRequired(element),
                    converterChecker.getQualifiedConverterName(element, attributeAnnotation))
        }

        if (propertyAnnotation != null) {
            val converterChecker = PropertyElementConverterChecker()
            return PropertyField(element,
                    nameFromAnnotationOrFieldName(propertyAnnotation.name, element),
                    requiredDetector.isRequired(element),
                    converterChecker.getQualifiedConverterName(element, propertyAnnotation))
        }


        if (elementAnnotation != null) {

            val nameMatchers = elementAnnotation.typesByElement
            val inlineListAnnotation = element.getAnnotation(InlineList::class.java)

            if (nameMatchers.isEmpty()) {
                // No polymorphism
                if (isList(element)) {

                } else {

                    if (inlineListAnnotation != null) {
                        throw ProcessingException(element, "The annotation @${InlineList::class.simpleName} is only allowed on java.util.List types, but the field '${element.simpleName}' in class ${(element.enclosingElement as TypeElement).qualifiedName} is of type ${element.asType()}")
                    }

                }
            } else {
                // polymorphism
                if (isList(element)) {

                } else {

                    if (inlineListAnnotation != null) {
                        throw ProcessingException(element, "The annotation @${InlineList::class.simpleName} is only allowed on java.util.List types, but the field '${element.simpleName}' in class ${(element.enclosingElement as TypeElement).qualifiedName} is of type ${element.asType()}")
                    }

                }
            }

        }



        // TODO thow exception
        //   throw ProcessingException(element, "Unknown annotation!")
        return null
    }

    /**
     * Checks whether or not thy element is of type (or subtype) java.util.List
     */
    protected fun isList(element: VariableElement): Boolean {
        return typeUtils.isAssignable(element.asType(), listTypeMirror)
    }

    /**
     *
     */
    protected fun nameFromAnnotationOrFieldName(name: String, element: VariableElement) =
            if (name.isEmpty()) {
                element.simpleName.toString()
            } else name

}