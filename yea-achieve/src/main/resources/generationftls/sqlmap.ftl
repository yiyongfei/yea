<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >
<mapper namespace="${tableWrapper.daoPackagePath}.${tableWrapper.daoName}" >
<#--Freemark注意点：
    1、boolean值的处理，不要用isXXX
    2、类似#的特殊字符，可以使用${r'#'}来转义
  -->
  <#if tableWrapper.daoGenerateable > 
    <#assign sqlidPrefix=""/>
  <#else>
    <#assign sqlidPrefix=tableWrapper.aggregateVar + "_" />
  </#if>
  <#assign resultMapName=tableWrapper.aggregateVar + "ResultMap"/>
  <#assign entityMapName=tableWrapper.aggregateVar + "EntityMap"/>
  <#assign pkMapName=tableWrapper.aggregateVar + "PKMap"/>
  
  <insert id="${sqlidPrefix}insert" parameterType="${tableWrapper.entityPackagePath}.${tableWrapper.entityName}" >
    INSERT INTO ${tableWrapper.tableInfo.introspectedTableName} 
       (
      <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
          ${column.actualColumnName},
      </#list>
      <#list tableWrapper.tableInfo.nonPrimaryKeyColumns?if_exists as column>
          ${column.actualColumnName}<#if column_has_next>,</#if>
      </#list>
        ) VALUES (
      <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
    	  ${r'#'}{pk.${column.javaProperty}, jdbcType=${column.jdbcTypeName}},
      </#list>
      <#list tableWrapper.tableInfo.nonPrimaryKeyColumns?if_exists as column>
    	  ${r'#'}{${column.javaProperty}, jdbcType=${column.jdbcTypeName}}<#if column_has_next>,</#if>
      </#list>
        )
  </insert>
  
  <insert id="${sqlidPrefix}insertSelective" parameterType="${tableWrapper.entityPackagePath}.${tableWrapper.entityName}" >
    INSERT INTO ${tableWrapper.tableInfo.introspectedTableName}
      <trim prefix="(" suffix=")" suffixOverrides="," >
    	<#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
    	<if test="pk != null and pk.${column.javaProperty} != null" >
          ${column.actualColumnName},
      	</if>
    	</#list>
    	<#list tableWrapper.tableInfo.nonPrimaryKeyColumns?if_exists as column>
    	<if test="${column.javaProperty} != null" >
          ${column.actualColumnName},
      	</if>
    	</#list>
      </trim>
    VALUES
      <trim prefix="(" suffix=")" suffixOverrides="," >
    	<#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
    	<if test="pk != null and pk.${column.javaProperty} != null" >
          ${r'#'}{pk.${column.javaProperty}, jdbcType=${column.jdbcTypeName}},
      	</if>
    	</#list>
    	<#list tableWrapper.tableInfo.nonPrimaryKeyColumns?if_exists as column>
    	<if test="${column.javaProperty} != null" >
          ${r'#'}{${column.javaProperty}, jdbcType=${column.jdbcTypeName}},
      	</if>
    	</#list>
      </trim>
  </insert>
  
  <update id="${sqlidPrefix}update" parameterType="${tableWrapper.entityPackagePath}.${tableWrapper.entityName}" >
    UPDATE ${tableWrapper.tableInfo.introspectedTableName} SET 
    	<#list tableWrapper.tableInfo.nonPrimaryKeyColumns?if_exists as column>
          ${column.actualColumnName} = ${r'#'}{${column.javaProperty}, jdbcType=${column.jdbcTypeName}}<#if column_has_next>,</#if>
    	</#list>
     WHERE 1 = 1
     <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
       AND ${column.actualColumnName} = ${r'#'}{pk.${column.javaProperty}, jdbcType=${column.jdbcTypeName}}
     </#list>
  </update>
  
  <update id="${sqlidPrefix}updateSelective" parameterType="${tableWrapper.entityPackagePath}.${tableWrapper.entityName}" >
    UPDATE ${tableWrapper.tableInfo.introspectedTableName}
      <trim prefix="SET" suffixOverrides="," >
    	<#list tableWrapper.tableInfo.nonPrimaryKeyColumns?if_exists as column>
    	<if test="${column.javaProperty} != null" >
          ${column.actualColumnName} = ${r'#'}{${column.javaProperty}, jdbcType=${column.jdbcTypeName}},
      	</if>
    	</#list>
      </trim>
     WHERE 1 = 1
     <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
       AND ${column.actualColumnName} = ${r'#'}{pk.${column.javaProperty}, jdbcType=${column.jdbcTypeName}}
     </#list>
  </update>
  
  <delete id="${sqlidPrefix}delete" parameterType="${tableWrapper.pkPackagePath}.${tableWrapper.pkName}" >
    DELETE FROM ${tableWrapper.tableInfo.introspectedTableName}
     WHERE 1 = 1
     <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
       AND ${column.actualColumnName} = ${r'#'}{${column.javaProperty}, jdbcType=${column.jdbcTypeName}}
     </#list>
  </delete>
  
  <delete id="${sqlidPrefix}deleteBySelective" parameterType="${tableWrapper.entityPackagePath}.${tableWrapper.entityName}" >
    DELETE FROM ${tableWrapper.tableInfo.introspectedTableName}
     <trim prefix="WHERE" prefixOverrides="AND" >
     <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
    	<#if column.jdbcType == 1 || column.jdbcType == 12 >
        <if test="pk != null and pk.${column.javaProperty} != null and pk.${column.javaProperty} != ''" >
        <#else>
        <if test="pk != null and pk.${column.javaProperty} != null" >
        </#if>
          AND ${column.actualColumnName} = ${r'#'}{pk.${column.javaProperty}, jdbcType=${column.jdbcTypeName}}
        </if>
     </#list>
     <#list tableWrapper.tableInfo.nonPrimaryKeyColumns?if_exists as column>
    	<#if column.jdbcType == 1 || column.jdbcType == 12 >
        <if test="${column.javaProperty} != null and ${column.javaProperty} != ''" >
        <#else>
        <if test="${column.javaProperty} != null" >
        </#if>
          AND ${column.actualColumnName} = ${r'#'}{${column.javaProperty}, jdbcType=${column.jdbcTypeName}}
        </if>
     </#list>
     </trim>
  </delete>
  
  <resultMap id="${resultMapName}" type="${tableWrapper.aggregatePackagePath}.${tableWrapper.aggregateName}" >
    <constructor>
    <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
       <#if column.jdbcType == 4 || column.jdbcType == -5 >
       <idArg column="${column.actualColumnName}" javaType="${column.fullyQualifiedJavaType.shortNameWithoutTypeArguments}" jdbcType="${column.jdbcTypeName}"/>
       <#else>
       <arg column="${column.actualColumnName}" javaType="${column.fullyQualifiedJavaType.shortNameWithoutTypeArguments}" jdbcType="${column.jdbcTypeName}"/>
       </#if>
    </#list>
    </constructor>
    <association property="${tableWrapper.entityVar}" javaType="${tableWrapper.entityPackagePath}.${tableWrapper.entityName}" resultMap="${entityMapName}"/>
  </resultMap>
  <resultMap id="${entityMapName}" type="${tableWrapper.entityPackagePath}.${tableWrapper.entityName}" >
  <#list tableWrapper.tableInfo.nonPrimaryKeyColumns?if_exists as column>
    <result column="${column.actualColumnName}" property="${column.javaProperty}" jdbcType="${column.jdbcTypeName}" />
  </#list>
    <association property="pk" javaType="${tableWrapper.pkPackagePath}.${tableWrapper.pkName}" resultMap="${pkMapName}"/>
  </resultMap>
  <resultMap id="${pkMapName}" type="${tableWrapper.pkPackagePath}.${tableWrapper.pkName}" >
	<constructor>
	 <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
       <#if column.jdbcType == 4 || column.jdbcType == -5 >
       <idArg column="${column.actualColumnName}" javaType="${column.fullyQualifiedJavaType.shortNameWithoutTypeArguments}" jdbcType="${column.jdbcTypeName}"/>
       <#else>
       <arg column="${column.actualColumnName}" javaType="${column.fullyQualifiedJavaType.shortNameWithoutTypeArguments}" jdbcType="${column.jdbcTypeName}"/>
       </#if>
     </#list>
	</constructor>
  </resultMap>
  
  <sql id="${tableWrapper.tableInfo.introspectedTableName}_Column_List" >
    <#list tableWrapper.tableInfo.allColumns?if_exists as column>
    ${r'a.'}${column.actualColumnName}<#if column_has_next>,</#if>
    </#list>
  </sql>
  
  <select id="${sqlidPrefix}load" parameterType="${tableWrapper.pkPackagePath}.${tableWrapper.pkName}" resultMap="${resultMapName}">
    SELECT 
    <include refid="${tableWrapper.tableInfo.introspectedTableName}_Column_List" />
      FROM ${tableWrapper.tableInfo.introspectedTableName} a
     WHERE 1 = 1
     <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
       AND ${r'a.'}${column.actualColumnName} = ${r'#'}{${column.javaProperty}, jdbcType=${column.jdbcTypeName}}
     </#list>
  </select>
  
  <select id="${sqlidPrefix}selectBySelective" parameterType="${tableWrapper.aggregatePackagePath}.${tableWrapper.aggregateName}" resultMap="${resultMapName}">
    SELECT 
    <include refid="${tableWrapper.tableInfo.introspectedTableName}_Column_List" />
      FROM ${tableWrapper.tableInfo.introspectedTableName} a
      <trim prefix="WHERE" prefixOverrides="AND" >
     <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
    	<#if column.jdbcType == 1 || column.jdbcType == 12 >
        <if test="${tableWrapper.pkVar} != null and ${tableWrapper.pkVar}.${column.javaProperty} != null and ${tableWrapper.pkVar}.${column.javaProperty} != ''" >
        <#else>
        <if test="${tableWrapper.pkVar} != null and ${tableWrapper.pkVar}.${column.javaProperty} != null" >
        </#if>
          AND ${r'a.'}${column.actualColumnName} = ${r'#'}{${tableWrapper.pkVar}.${column.javaProperty}, jdbcType=${column.jdbcTypeName}}
        </if>
     </#list>
     <#list tableWrapper.tableInfo.nonPrimaryKeyColumns?if_exists as column>
    	<#if column.jdbcType == 1 || column.jdbcType == 12 >
        <if test="${tableWrapper.entityVar} != null and ${tableWrapper.entityVar}.${column.javaProperty} != null and ${tableWrapper.entityVar}.${column.javaProperty} != ''" >
        <#else>
        <if test="${tableWrapper.entityVar} != null and ${tableWrapper.entityVar}.${column.javaProperty} != null" >
        </#if>
          AND ${r'a.'}${column.actualColumnName} = ${r'#'}{${tableWrapper.entityVar}.${column.javaProperty}, jdbcType=${column.jdbcTypeName}}
        </if>
     </#list>
      </trim>
  </select>
  <select id="${sqlidPrefix}selectBySelectiveCount" parameterType="${tableWrapper.aggregatePackagePath}.${tableWrapper.aggregateName}" resultType="int">
    SELECT count(1) FROM ${tableWrapper.tableInfo.introspectedTableName} a
      <trim prefix="WHERE" prefixOverrides="AND" >
     <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as column>
    	<#if column.jdbcType == 1 || column.jdbcType == 12 >
        <if test="${tableWrapper.pkVar} != null and ${tableWrapper.pkVar}.${column.javaProperty} != null and ${tableWrapper.pkVar}.${column.javaProperty} != ''" >
        <#else>
        <if test="${tableWrapper.pkVar} != null and ${tableWrapper.pkVar}.${column.javaProperty} != null" >
        </#if>
          AND ${r'a.'}${column.actualColumnName} = ${r'#'}{${tableWrapper.pkVar}.${column.javaProperty}, jdbcType=${column.jdbcTypeName}}
        </if>
     </#list>
     <#list tableWrapper.tableInfo.nonPrimaryKeyColumns?if_exists as column>
    	<#if column.jdbcType == 1 || column.jdbcType == 12 >
        <if test="${tableWrapper.entityVar} != null and ${tableWrapper.entityVar}.${column.javaProperty} != null and ${tableWrapper.entityVar}.${column.javaProperty} != ''" >
        <#else>
        <if test="${tableWrapper.entityVar} != null and ${tableWrapper.entityVar}.${column.javaProperty} != null" >
        </#if>
          AND ${r'a.'}${column.actualColumnName} = ${r'#'}{${tableWrapper.entityVar}.${column.javaProperty}, jdbcType=${column.jdbcTypeName}}
        </if>
     </#list>
      </trim>
  </select>
</mapper>