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
package com.googlecode.jmapper.api;

import com.googlecode.jmapper.xml.beans.XmlTargetClass;

/**
 * Permits to define an xml Class.
 * 
 * @author Alessandro Vurro
 *
 */
public class TargetClass implements Convertible<XmlTargetClass>{

	/** xml target class*/
	private XmlTargetClass xmlTargetClass;
	
	/**
	 * Class to convert.
	 * @param clazz Class to convert
	 */
	public TargetClass(Class<?> clazz) {
		xmlTargetClass = new XmlTargetClass(clazz.getName());
	}
	
	public XmlTargetClass toXStream() {
		return xmlTargetClass;
	}

}