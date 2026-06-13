package org.example.springboot.entity.es;


import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import lombok.Data;


import org.example.springboot.config.DateRangeDeserializer;
import org.example.springboot.config.IntegerRangeDeserializer;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.domain.Range;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoJsonMultiPoint;
import org.springframework.data.elasticsearch.core.geo.GeoJsonPoint;
import org.springframework.data.elasticsearch.utils.geohash.Point;
import org.springframework.data.relational.core.sql.In;

import java.util.Date;

@Document(indexName = "job_index_test", writeTypeHint = WriteTypeHint.FALSE)
@TypeAlias("job")
@Data
public class JobTest {

    @Id
    private String id;

    @Field(type = FieldType.Integer_Range)
    @JsonDeserialize(using = IntegerRangeDeserializer.class)
    private Range<Integer> ageRange;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Date, pattern = "uuuu-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8", shape = JsonFormat.Shape.STRING) //此注解将date类型数据转成字符串响应出去
    @JsonDeserialize(using = DateDeserializers.DateDeserializer.class)		// 反序列化
    @JsonSerialize(using = DateSerializer.class)		// 序列化
    private Date createTime;

    @Field(type = FieldType.Date_Range,pattern = "uuuu-MM-dd HH:mm:ss")
    @JsonDeserialize(using = DateRangeDeserializer.class)
    private Range<Date>  recruitmentTime;


    @Field(type = FieldType.Keyword)
    private Integer baseSalary;

    @Field(type = FieldType.Keyword)
    private Integer moreSalary;

    @IndexedIndexName
    private String indexName;
    @Field(type = FieldType.Keyword)
    private Integer status;



    private Point location;

    @Field(type = FieldType.Integer)
    @WriteOnlyProperty
    @AccessType(AccessType.Type.PROPERTY)
    public Integer getAllSalary(){ //必须是public
        if (baseSalary == null || moreSalary == null) {
            return 0;
        }
        return baseSalary + moreSalary;
    }



}
