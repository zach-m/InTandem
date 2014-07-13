package tectonica.intandem.impl.jdbc;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

public class JSON
{
	private static final ObjectMapper jaxbJson = createJaxbMapper();

	public static ObjectMapper createJaxbMapper()
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JaxbAnnotationModule());
		mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		mapper.setSerializationInclusion(Include.NON_NULL);
//		mapper.registerModule(new JodaModule());
		return mapper;
	}

	public static ObjectMapper getJaxbMapper()
	{
		return jaxbJson;
	}

	public static String toJson(Object o)
	{
		try
		{
			if (jaxbJson.canSerialize(o.getClass()))
				return (jaxbJson.writeValueAsString(o));
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
			return jaxbJson.readValue(jsonStr, clz);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
