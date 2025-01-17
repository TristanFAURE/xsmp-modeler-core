/*******************************************************************************
 * Copyright (C) 2020-2022 THALES ALENIA SPACE FRANCE.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.xsmp.generator.cpp.member

import org.eclipse.xsmp.util.ElementUtil.ArgKind
import org.eclipse.xsmp.xcatalogue.Interface
import org.eclipse.xsmp.xcatalogue.NamedElementWithMembers
import org.eclipse.xsmp.xcatalogue.Operation
import org.eclipse.xsmp.xcatalogue.Parameter
import org.eclipse.xsmp.xcatalogue.ParameterDirectionKind
import org.eclipse.xsmp.xcatalogue.ReferenceType
import org.eclipse.xsmp.xcatalogue.Structure
import org.eclipse.xsmp.xcatalogue.Type
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.naming.QualifiedName
import org.eclipse.xsmp.generator.cpp.IncludeAcceptor

class OperationGenerator extends AbstractMemberGenerator<Operation> {

	def String name(Operation o) {

		if (o.isConstructor)
			return EcoreUtil2.getContainerOfType(o, NamedElementWithMembers).name

		var operator = o.attributeValue(QualifiedName.create("Attributes", "Operator"))
		if (operator !== null) {
			val value = operator.getEnum(null)

			if (value !== null) {
				switch (value.name) {
					case "Positive",
					case "Sum":
						return "operator+"
					case "Add":
						return "operator+="
					case "Negative",
					case "Difference":
						return "operator-"
					case "Subtract":
						return "operator-="
					case "Assign":
						return "operator="
					case "Product":
						return "operator*"
					case "Multiply":
						return "operator*="
					case "Quotient":
						return "operator/"
					case "Divide":
						return "operator/="
					case "Remainder":
						return "operator%="
					case "Greater":
						return "operator>"
					case "Less":
						return "operator<"
					case "Equal":
						return "operator=="
					case "NotGreater":
						return "operator<="
					case "NotLess":
						return "operator>="
					case "NotEqual":
						return "operator!="
					case "Indexer":
						return "operator[]"
					case "Module":
						return "operator%"
				}
			}
		}
		return o.name
	}

	def returnType(Operation op) {
		if (op.isConstructor)
			null
		else if (op.returnParameter === null)
			'''void '''
		else
			'''«op.returnParameter .type()» '''
	}

	override declare(NamedElementWithMembers type, Operation element) {
		var abstract = element.abstract
		if (!abstract) {
			var virtual = element.virtual
			'''
				«element.comment»
				«IF virtual »virtual «ENDIF»«element.returnType»«element.name()» («FOR p : element.parameter SEPARATOR '\n, '»«p.generateParameter(true)»«ENDFOR» ) override «IF abstract»=0«ENDIF»;
			'''
		}
	}

	override define(NamedElementWithMembers type, Operation element) {

		if (!element.abstract)
			'''
				«element.returnType»«type.name»::«element.name()» («FOR p : element.parameter SEPARATOR '\n, '»«p.generateParameter(false)»«ENDFOR» ) 
				{
				    «IF element.returnParameter!==null»
				    	«element.returnParameter.type()» ret {};
				    	return ret;
				    «ENDIF» 
				}
			'''
	}

	override declareGen(NamedElementWithMembers type, Operation element, boolean useGenPattern) {
		var virtual = element.virtual || useGenPattern
		var abstract = element.abstract || useGenPattern
		if (element.eContainer instanceof Interface) {
			virtual = true
			abstract = true
		}
		'''
			«element.comment»
			«IF virtual»virtual «ENDIF»«element.returnType»«element.name()»(«FOR p : element.parameter SEPARATOR ','»«p.generateParameter(true)»«ENDFOR»)«IF abstract»=0«ENDIF»;
		'''
	}

	override defineGen(NamedElementWithMembers type, Operation element, boolean useGenPattern) {
		var abstract = element.abstract || useGenPattern
		if (!abstract)
			'''
				«element.returnType»«type.name»::«element.name()»(«FOR p : element.parameter SEPARATOR ', '»«p.generateParameter(false)»«ENDFOR»)
				{
				    «IF element.returnParameter!==null»
				    	«element.returnParameter.type()» ret {};
				    	return ret;
				    «ENDIF» 
				}
			'''
	}

	override collectIncludes(Operation element, IncludeAcceptor acceptor) {
		super.collectIncludes(element, acceptor)
		// if a parameter is passed by value, add it to includes
		// In case of pointer or reference, use a forward declaration
		for (m : element.parameter) {
			if (m.kind === ArgKind.BY_VALUE || !m.type.isForwardable)
				acceptor.include(m.type)
			else
				acceptor.forward(m.type)
			m.^default?.include(acceptor)
		}
		if (element.returnParameter !== null) {
			if (element.returnParameter.kind === ArgKind.BY_VALUE || !element.returnParameter.type.isForwardable)
				acceptor.include(element.returnParameter.type)
			else
				acceptor.forward(element.returnParameter.type)
		}
	}

	override initialize(NamedElementWithMembers container, Operation member, boolean useGenPattern) {
	}

	override finalize(Operation element) {
	}

	override Publish(Operation element) {
		val r = element.returnParameter
		'''
			{
			    // Publish operation «element.name»
			    «IF !element.parameter.empty || r !== null »
			    	::Smp::Publication::IPublishOperation* operation = 
			    «ENDIF»
			    receiver->PublishOperation("«element.name»", «element.description()», «element.viewKind»);
			    «FOR p : element.parameter»
			    	operation->PublishParameter("«p.name»", «p.description()», «p.type.uuidQfn», «p.direction.generatePDK»);
			    «ENDFOR»
			    «IF r !== null »
			    	operation->PublishParameter("«r.name»", «r.description()», «r.type.uuidQfn», «r.direction.generatePDK»);
			    «ENDIF»
			}
		'''
	}

	protected def CharSequence build(ArgKind kind) {
		switch (kind) {
			case BY_VALUE: ''' '''
			case BY_PTR: '''*'''
			case BY_REF: '''&'''
		}
	}

	protected def CharSequence type(Parameter t) {
		'''«IF t.isConst»const «ENDIF»::«t.type.fqn.toString("::")»«t.kind.build»'''

	}

	protected def CharSequence generateParameter(Parameter t, boolean withDefault) {
//«t.comment»
		'''
			«t.type()» «t.name»«IF t.^default !==null && withDefault» = «t.^default.doGenerateExpression(t.type, t)»«ENDIF» 
		'''
	}

	/**
	 * By default types are not forwardable
	 */
	private def dispatch boolean isForwardable(Type t) {
		false
	}

	/**
	 * Struct, class and exception are forwardable
	 */
	private def dispatch boolean isForwardable(Structure t) {
		true
	}

	/**
	 * Interface, model and services are forwardable
	 */
	private def dispatch boolean isForwardable(ReferenceType t) {
		true
	}

	def CharSequence generatePDK(ParameterDirectionKind t) {
		switch (t) {
			case ParameterDirectionKind.IN: '''Smp::Publication::ParameterDirectionKind::PDK_In'''
			case ParameterDirectionKind.OUT: '''Smp::Publication::ParameterDirectionKind::PDK_Out'''
			case ParameterDirectionKind.INOUT: '''Smp::Publication::ParameterDirectionKind::PDK_InOut'''
			case ParameterDirectionKind.RETURN: '''Smp::Publication::ParameterDirectionKind::PDK_Return'''
		}
	}
}
