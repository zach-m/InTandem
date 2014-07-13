package tectonica.intandem.impl.jdbc;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSON
{
	private static final ObjectMapper fieldsMapper = createFieldsMapper();

	public static ObjectMapper createFieldsMapper()
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
		mapper.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
		mapper.setSerializationInclusion(Include.NON_NULL);
		return mapper;
	}

	public static ObjectMapper getJaxbMapper()
	{
		return fieldsMapper;
	}

	public static String toJson(Object o)
	{
		try
		{
			if (fieldsMapper.canSerialize(o.getClass()))
				return (fieldsMapper.writeValueAsString(o));
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException(e);
		}
		throw new RuntimeException("Unserializable object: " + o.toString());
	}

	public static <T> T fromJson(String jsonStr, Class<T> clz)
	{
		try
		{
			return fieldsMapper.readValue(jsonStr, clz);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
