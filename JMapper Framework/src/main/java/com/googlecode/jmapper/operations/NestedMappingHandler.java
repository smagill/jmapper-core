/**
 * Copyright (C) 2012 - 2016 Alessandro Vurro.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.jmapper.operations;

import static com.googlecode.jmapper.config.Constants.NESTED_MAPPING_ENDS_SYMBOL;
import static com.googlecode.jmapper.config.Constants.NESTED_MAPPING_SPLIT_SYMBOL;
import static com.googlecode.jmapper.config.Constants.NESTED_MAPPING_STARTS_SYMBOL;
import static com.googlecode.jmapper.util.ClassesManager.retrieveField;
import static com.googlecode.jmapper.util.ClassesManager.verifyGetterMethods;
import static com.googlecode.jmapper.util.ClassesManager.verifySetterMethods;
import static com.googlecode.jmapper.util.GeneralUtility.isNull;

import java.lang.reflect.Field;

import com.googlecode.jmapper.annotations.Annotation;
import com.googlecode.jmapper.config.Error;
import com.googlecode.jmapper.exceptions.InvalidNestedMappingException;
import com.googlecode.jmapper.exceptions.MappingException;
import com.googlecode.jmapper.operations.beans.MappedField;
import com.googlecode.jmapper.operations.info.NestedMappedField;
import com.googlecode.jmapper.operations.info.NestedMappingInfo;
import com.googlecode.jmapper.xml.XML;
import static com.googlecode.jmapper.config.Constants.SAFE_NAVIGATION_OPERATOR;

/**
 * Nested mapping Handler.
 * @author Alessandro Vurro
 *
 */
public class NestedMappingHandler {

	/**
	 * Returns true if this regex represents a nested mapping, false other cases.
	 * @param regex string that represents the mapping
	 * @return true if this is a nested mapping, false other cases
	 */
	public static boolean isNestedMapping(String regex){
		return   regex.startsWith(NESTED_MAPPING_STARTS_SYMBOL)
			  && regex.endsWith  (NESTED_MAPPING_ENDS_SYMBOL);
	}
	
	/**
	 * @param nestedMappingPath nested mapping path
	 * @return a list with splitted fields
	 */
	public static String[] nestedFields(String nestedMappingPath){
		return nestedMappingPath
				 .substring(NESTED_MAPPING_STARTS_SYMBOL.length(), nestedMappingPath.length() - NESTED_MAPPING_ENDS_SYMBOL.length())
		         .split(NESTED_MAPPING_SPLIT_SYMBOL);
	}
	
	/**
	 * It checks the presence of the elvis operator and how it is used.
	 * @param nestedFieldName field name to check
	 * @return true if is used an elvis operator, false otherwise, for all other cases a MappingException will be thrown 
	 */
	public static boolean safeNavigationOperatorDefined(String nestedFieldName){
		if(nestedFieldName.contains(SAFE_NAVIGATION_OPERATOR))
			if(!nestedFieldName.startsWith(SAFE_NAVIGATION_OPERATOR))
				//TODO standardizzare exception
				throw new MappingException("Safe navigation operator must be the first symbol after dot notation");
			else
				return true;
		
		return false;
	}
	
	/**
	 * @param nestedFieldName nested field filtered
	 * @return the filtered name of this field
	 */
	public static String safeNavigationOperatorFilter(String nestedFieldName){
			return nestedFieldName.replaceAll("\\"+SAFE_NAVIGATION_OPERATOR,"");
	}
	
	/**
	 * Checks only get accessor of this field.
	 * @param xml used to check xml configuration
	 * @param aClass class where is the field with the nested mapping
	 * @param nestedField field with nested mapping
	 */
	private static MappedField checkGetAccessor(XML xml, Class<?> aClass, Field nestedField){
		
		MappedField field = new MappedField(nestedField);
		xml.fillMappedField(aClass, field);
		Annotation.fillMappedField(aClass,field);
		verifyGetterMethods(aClass,field);
		return field;
	}
	
	/**
	 * Checks get and set accessors of this field.
	 * @param xml used to check xml configuration
	 * @param aClass class where is the field with the nested mapping
	 * @param nestedField field with nested mapping
	 * @return mapped field
	 */
	private static MappedField checkAccessors(XML xml, Class<?> aClass, Field nestedField){

		MappedField field = checkGetAccessor(xml, aClass, nestedField);
		verifySetterMethods(aClass,field);
		return field;
	}

	/**
	 * This method returns a bean with nested mapping information.
	 * @param xml xml configuration
	 * @param targetClass class to check
	 * @param nestedMappingPath path that define nested mapping
	 * @param sourceClass sourceClass used to indentify nested mapping
	 * @param destinationClass destinationClass used to indentify nested mapping
	 * @param configuredField used only for the explanation of the errors
	 * @return NestedMappingInfo
	 */
	public static NestedMappingInfo loadNestedMappingInformation(XML xml, Class<?> targetClass,String nestedMappingPath, Class<?> sourceClass, Class<?> destinationClass, Field configuredField){
		
		boolean isSourceClass = targetClass == sourceClass;
		
		NestedMappingInfo info = new NestedMappingInfo(isSourceClass);
		info.setConfiguredClass(isSourceClass?destinationClass:sourceClass);
		info.setConfiguredField(configuredField);
		try{
		
			Class<?> nestedClass = targetClass;
			String[] nestedFields = nestedFields(nestedMappingPath);
			Field field = null;
			
			for(int i = 0; i< nestedFields.length;i++){
				
				String nestedFieldName = nestedFields[i];
				
				boolean elvisOperatorDefined = false;
				
				// the safe navigation operator is not valid for the last field
				if(i < nestedFields.length - 1){
					String nextNestedFieldName = nestedFields[i+1];
					elvisOperatorDefined = safeNavigationOperatorDefined(nextNestedFieldName);
					
				}
				
				// name filtering
				nestedFieldName = safeNavigationOperatorFilter(nestedFieldName);

				field = retrieveField(nestedClass, nestedFieldName);
				
				if(isNull(field))
					Error.inexistentField(nestedFieldName, nestedClass.getSimpleName());
				
				// verifies if is exists a get method for this nested field
				// in case of nested mapping relative to source, only get methods will be checked
				// in case of nested mapping relative to destination, get and set methods will be checked
				MappedField nestedField = isSourceClass ? checkGetAccessor(xml, nestedClass, field) 
						//TODO Nested Mapping -> in caso di avoidSet qui potremmo avere un problema
						                                : checkAccessors(xml, nestedClass, field);
				
				// storage information relating to the piece of path
				info.addNestedField(new NestedMappedField(nestedField, nestedClass, elvisOperatorDefined));
				
				nestedClass = field.getType();
			}
			
		}catch(MappingException e){
			
			InvalidNestedMappingException exception = new InvalidNestedMappingException(nestedMappingPath);
			exception.getMessages().put(InvalidNestedMappingException.FIELD, e.getMessage());
			throw exception;
		}
		
		return info;
	}
	
}
