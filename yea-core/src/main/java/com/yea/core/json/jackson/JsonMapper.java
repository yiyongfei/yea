/**
 * Copyright 2017 伊永飞
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yea.core.json.jackson;

import java.text.DateFormat;
import java.util.HashSet;
import java.util.Set;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * 
 * @author yiyongfei
 *
 */
public class JsonMapper {
	private ObjectMapper jsonMapper;
	private SimpleFilterProvider filterProvider;
	private Set<String> filternames;
	
	public JsonMapper() {
		jsonMapper = new ObjectMapper();
		// 允许属性名称没有引号
		jsonMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		// 对于空的对象转json的时候不抛出错误
		jsonMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		// 禁用遇到未知属性抛出异常
		jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		filternames = new HashSet<String>();
		filterProvider = new SimpleFilterProvider();
	}
	
	public ObjectMapper registerModules(Module... modules) {
		return jsonMapper.registerModules(modules);
	}
	
	public ObjectMapper setDateFormat(DateFormat dateFormat) {
		return jsonMapper.setDateFormat(dateFormat);
	}
	
	/**
	 * 设置转换成Json串时的过滤字段
	 */
	public void filter(String filterId, SimpleBeanPropertyFilter filter) {
		if (StringUtils.isEmpty(filterId) || filter == null)
			return;
		filterProvider.addFilter(filterId, filter);
		filternames.add(filterId);
	}

	/**
	 * 将对象转换成Json串
	 * 
	 * @param object
	 * @return
	 * @throws Exception
	 */
	public <T> String generatorJson(T object) throws JsonProcessingException {
		if(filternames.size() > 0) {
			jsonMapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
				private static final long serialVersionUID = 1L;

				@Override
			    public Object findFilterId(Annotated a) {
					Object filterId = super.findFilterId(a);
					if (filterId == null) {
						if(filternames.contains(a.getName())) {
							return a.getName();
						} else {
							return null;
						}
					} else {
						return filterId;
					}
			    }
			});
			return jsonMapper.setFilterProvider(filterProvider).writeValueAsString(object);
		} else {
			return jsonMapper.writeValueAsString(object);
		}
		
	}
	
	/**
	 * 将Json串转换成对象
	 * 
	 * @param jsonString
	 * @param objClass
	 * @return
	 * @throws Exception
	 */
	public <T> T parserJson(String jsonString, Class<T> objClass) throws Exception{
		JsonParser parser = jsonMapper.getFactory().createParser(jsonString);
		try{
			return parser.readValueAs(objClass);
		} finally {
			parser.close();
		}
	}
}