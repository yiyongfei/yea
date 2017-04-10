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
package com.yea.web.jsonbody;

import java.lang.annotation.Annotation;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletResponse;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.yea.core.json.jackson.JsonMapper;

public class JsonReturnHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		// 如果有自定义的 JSON 注解 就用这个Handler 来处理
		return (AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), JsonBody.class) ||
				returnType.hasMethodAnnotation(JsonBody.class) || 
				AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), JsonPropFilter.class) ||
				returnType.hasMethodAnnotation(JsonPropFilter.class));
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws Exception {
		// 设置这个就是最终的处理类了，处理完不再去找下一个类进行处理
		mavContainer.setRequestHandled(true);

		// 获得注解并执行filter方法 最后返回
		HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
		Annotation[] annos = returnType.getMethodAnnotations();
		
		JsonMapper jsonMapper = new JsonMapper();
		// 序列换成json时,将所有的long变成string
		SimpleModule simpleModule = new SimpleModule();
		simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
		simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
		jsonMapper.registerModules(simpleModule);
		// 序列换成json时,将所有的日期类型变成string
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		jsonMapper.setDateFormat(formatter);

		/*
		 * @param filterId 需要设置规则的Class
		 * @param include 转换时包含哪些字段
		 * @param filter 转换时过滤哪些字段
		 */
		for (Annotation anno : annos) {
			if (anno instanceof JsonPropFilter) {
				JsonPropFilter json = (JsonPropFilter) anno;
				if (json.type() == null)
					continue;
				if (!StringUtils.isEmpty(json.include())) {
					jsonMapper.filter(json.type().getName(), SimpleBeanPropertyFilter.filterOutAllExcept(json.include().replaceAll(" ", "").split(",")));
				} else if (!StringUtils.isEmpty(json.filter())) {
					jsonMapper.filter(json.type().getName(), SimpleBeanPropertyFilter.serializeAllExcept(json.filter().replaceAll(" ", "").split(",")));
				}
			}
			if (anno instanceof JsonBody) {
				JsonBody jsonbody = (JsonBody) anno;
				for(JsonPropFilter json : jsonbody.filters()) {
					if (json.type() == null)
						continue;
					if (!StringUtils.isEmpty(json.include())) {
						jsonMapper.filter(json.type().getName(), SimpleBeanPropertyFilter.filterOutAllExcept(json.include().replaceAll(" ", "").split(",")));
					} else if (!StringUtils.isEmpty(json.filter())) {
						jsonMapper.filter(json.type().getName(), SimpleBeanPropertyFilter.serializeAllExcept(json.filter().replaceAll(" ", "").split(",")));
					}
				}
			}
		}

		response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
		String json = jsonMapper.generatorJson(returnValue);
		response.getWriter().write(json);
	}
}