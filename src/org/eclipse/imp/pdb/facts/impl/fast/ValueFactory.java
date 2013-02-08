/*******************************************************************************
* Copyright (c) 2009, 2012-2013 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju - interface and implementation
*    Arnold Lankamp - implementation
*    Anya Helene Bagge - rational support, labeled maps and tuples
*    Davy Landman - added PI & E constants
*    Michael Steindorfer - extracted factory for numeric data   
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.util.Map;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListRelation;
import org.eclipse.imp.pdb.facts.IListRelationWriter;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedElementTypeException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;

/**
 * Implementation of IValueFactory.
 */
public class ValueFactory extends FastBaseValueFactory {
	private final static TypeFactory tf = TypeFactory.getInstance();
	
	private final static Type EMPTY_TUPLE_TYPE = TypeFactory.getInstance().tupleEmpty();
	
	protected ValueFactory(){
		super();
	}

	private static class InstanceKeeper{
		public final static ValueFactory instance = new ValueFactory();
	}
	
	public static ValueFactory getInstance(){
		return InstanceKeeper.instance;
	}
		
	public IListWriter listWriter(Type elementType){
		return new ListWriter(elementType);
	}
	
	public IListWriter listWriter(){
		return new ListWriter();
	}
	
	public IMapWriter mapWriter(Type keyType, Type valueType){
		return new MapWriter(TypeFactory.getInstance().mapType(keyType, valueType));
	}

	public IMapWriter mapWriter(Type mapType){
		return new MapWriter(mapType);
	}
	
	public IMapWriter mapWriter(){
		return new MapWriter();
	}
	
	public ISetWriter setWriter(Type elementType){
		if(elementType.isTupleType()) return relationWriter(elementType);
		
		return new SetWriter(elementType);
	}
	
	public ISetWriter setWriter(){
		return new SetWriter();
	}
	
	public IRelationWriter relationWriter(Type tupleType){
		return new RelationWriter(tupleType);
	}
	
	public IRelationWriter relationWriter(){
		return new RelationWriter();
	}
	
	public IListRelationWriter listRelationWriter(Type tupleType) {
		return new ListRelationWriter(tupleType);
	}

	public IListRelationWriter listRelationWriter() {
		return new ListRelationWriter();
	}
	
	public IList list(Type elementType){
		return listWriter(elementType).done();
	}
	
	public IList list(IValue... elements){
		IListWriter listWriter = listWriter(lub(elements));
		listWriter.append(elements);
		
		return listWriter.done();
	}
	
	public IMap map(Type mapType){
		return mapWriter(mapType).done();
	}

	public IMap map(Type keyType, Type valueType){
		return mapWriter(keyType, valueType).done();
	}
	
	public ISet set(Type elementType){
		return setWriter(TypeFactory.getInstance().voidType()).done();
	}
	
	public ISet set(IValue... elements){
		Type elementType = lub(elements);
		
		ISetWriter setWriter = setWriter(elementType);
		setWriter.insert(elements);
		return setWriter.done();
	}
	
	public IRelation relation(Type tupleType){
		return relationWriter(tupleType).done();
	}
	
	public IRelation relation(IValue... elements){
		Type elementType = lub(elements);
		
		if (!elementType.isTupleType()) throw new UnexpectedElementTypeException(tf.tupleType(tf.voidType()), elementType);
		
		IRelationWriter relationWriter = relationWriter(elementType);
		relationWriter.insert(elements);
		return relationWriter.done();
	}
	
	public IListRelation listRelation(Type tupleType) {
		return listRelationWriter(tupleType).done();
	}

	public IListRelation listRelation(IValue... elements) {
		Type elementType = lub(elements);
		
		if (!elementType.isTupleType()) throw new UnexpectedElementTypeException(tf.tupleType(tf.voidType()), elementType);
		
		IListRelationWriter listRelationWriter = listRelationWriter(elementType);
		listRelationWriter.append(elements);
		return listRelationWriter.done();
	}
	
	public INode node(String name){
		return new Node(name, new IValue[0]);
	}
	
	public INode node(String name, Map<String,IValue> annos, IValue... children){
		return new AnnotatedNode(name, children.clone(), annos);
	}
	
	public INode node(String name, IValue... children){
		return new Node(name, children.clone());
	}
	
	@Override
	public INode node(String name, IValue[] children, Map<String, IValue> keyArgValues)
			throws FactTypeUseException {
		return new Node(name, children.clone(), keyArgValues);
	}
	
	public IConstructor constructor(Type constructorType) {
		Type params = constructorType.getAbstractDataType().getTypeParameters();
		if (!params.isVoidType()) {
			ShareableHashMap<Type, Type> bindings = new ShareableHashMap<Type,Type>();
			for (Type p : params) {
				if (p.isParameterType()) {
					bindings.put(p, TypeFactory.getInstance().voidType());
				}
			}
			constructorType = constructorType.instantiate(bindings);
		}
		return new Constructor(constructorType, new IValue[0]);
	}
	
	public IConstructor constructor(Type constructorType, IValue... children){
		Type instantiatedType;
		if(!constructorType.getAbstractDataType().isParameterized()){
			instantiatedType = constructorType;
		}else{
			ShareableHashMap<Type, Type> bindings = new ShareableHashMap<Type,Type>();
			TypeFactory tf = TypeFactory.getInstance();
			Type params = constructorType.getAbstractDataType().getTypeParameters();
			for (Type p : params) {
				if (p.isParameterType()) {
					bindings.put(p, tf.voidType());
				}
			}
			constructorType.getFieldTypes().match(tf.tupleType(children), bindings);
			instantiatedType = constructorType.instantiate(bindings);
		}
		
		return new Constructor(instantiatedType, children.clone());
	}
	
	public IConstructor constructor(Type constructorType,
			Map<String, IValue> annotations, IValue... children)
			throws FactTypeUseException {
		Type instantiatedType;
		if(!constructorType.getAbstractDataType().isParameterized()){
			instantiatedType = constructorType;
		}else{
			ShareableHashMap<Type, Type> bindings = new ShareableHashMap<Type,Type>();
			TypeFactory tf = TypeFactory.getInstance();
			Type params = constructorType.getAbstractDataType().getTypeParameters();
			for (Type p : params) {
				if (p.isParameterType()) {
					bindings.put(p, tf.voidType());
				}
			}
			constructorType.getFieldTypes().match(tf.tupleType(children), bindings);
			instantiatedType = constructorType.instantiate(bindings);
		}
		
		ShareableHashMap<String, IValue> sAnnotations = new ShareableHashMap<String, IValue>();
		sAnnotations.putAll(annotations);
		
		return AnnotatedConstructor.createAnnotatedConstructor(instantiatedType, children.clone(), sAnnotations);
	}
	
	public ITuple tuple(){
		return new Tuple(EMPTY_TUPLE_TYPE, new IValue[0]);
	}
	
	public ITuple tuple(IValue... args){
		return new Tuple(args.clone());
	}
	
	public ITuple tuple(Type type, IValue... args) {
    return new Tuple(type, args.clone());
  }
	
	private static Type lub(IValue... elements) {
		Type elementType = TypeFactory.getInstance().voidType();
		
		for(int i = elements.length - 1; i >= 0; i--){
			elementType = elementType.lub(elements[i].getType());
		}
		
		return elementType;
	}

	
	
}
